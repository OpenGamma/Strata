/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.impl.rate.swap.CashFlowEquivalentCalculator;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParametersProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.common.SettlementType;
import com.opengamma.strata.product.swap.NotionalExchange;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swaption.ResolvedSwaption;

/**
 * Pricer for swaption with physical settlement in Hull-White one factor model with piecewise constant volatility.
 * <p>
 * Reference: Henrard, M. "The Irony in the derivatives discounting Part II: the crisis", Wilmott Journal, 2010, 2, 301-316
 */
public class HullWhiteSwaptionPhysicalProductPricer {

  /**
   * Normal distribution function.
   */
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);

  /**
   * The small parameter.
   */
  private static final double SMALL = 1.0e-9;

  /**
   * Default implementation.
   */
  public static final HullWhiteSwaptionPhysicalProductPricer DEFAULT =
      new HullWhiteSwaptionPhysicalProductPricer(DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public HullWhiteSwaptionPhysicalProductPricer(DiscountingPaymentPricer paymentPricer) {
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param swaption  the product
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    validate(swaption, ratesProvider, hwProvider);
    ResolvedSwap swap = swaption.getUnderlying();
    LocalDate expiryDate = swaption.getExpiryDate();
    if (expiryDate.isBefore(ratesProvider.getValuationDate())) { // Option has expired already
      return CurrencyAmount.of(swap.getLegs().get(0).getCurrency(), 0d);
    }
    ResolvedSwapLeg cashFlowEquiv = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, ratesProvider);
    int nPayments = cashFlowEquiv.getPaymentEvents().size();
    double[] alpha = new double[nPayments];
    double[] discountedCashFlow = new double[nPayments];
    for (int loopcf = 0; loopcf < nPayments; loopcf++) {
      NotionalExchange payment = (NotionalExchange) cashFlowEquiv.getPaymentEvents().get(loopcf);
      LocalDate maturityDate = payment.getPaymentDate();
      alpha[loopcf] = hwProvider.alpha(ratesProvider.getValuationDate(), expiryDate, expiryDate, maturityDate);
      discountedCashFlow[loopcf] = paymentPricer.presentValueAmount(payment.getPayment(), ratesProvider);
    }
    double omega = (swap.getLegs(SwapLegType.FIXED).get(0).getPayReceive().isPay() ? -1d : 1d);
    double kappa = computeKappa(hwProvider, discountedCashFlow, alpha, omega);
    double pv = 0.0;
    for (int loopcf = 0; loopcf < nPayments; loopcf++) {
      pv += discountedCashFlow[loopcf] * NORMAL.getCDF(omega * (kappa + alpha[loopcf]));
    }
    return CurrencyAmount.of(cashFlowEquiv.getCurrency(), pv * (swaption.getLongShort().isLong() ? 1d : -1d));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the swaption product.
   * 
   * @param swaption  the product
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    return MultiCurrencyAmount.of(presentValue(swaption, ratesProvider, hwProvider));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param swaption  the product
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the point sensitivity to the rate curves
   */
  public PointSensitivityBuilder presentValueSensitivityRates(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    validate(swaption, ratesProvider, hwProvider);
    ResolvedSwap swap = swaption.getUnderlying();
    LocalDate expiryDate = swaption.getExpiryDate();
    if (expiryDate.isBefore(ratesProvider.getValuationDate())) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    ImmutableMap<Payment, PointSensitivityBuilder> cashFlowEquivSensi =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap, ratesProvider);
    ImmutableList<Payment> list = cashFlowEquivSensi.keySet().asList();
    ImmutableList<PointSensitivityBuilder> listSensi = cashFlowEquivSensi.values().asList();
    int nPayments = list.size();
    double[] alpha = new double[nPayments];
    double[] discountedCashFlow = new double[nPayments];
    for (int loopcf = 0; loopcf < nPayments; loopcf++) {
      Payment payment = list.get(loopcf);
      alpha[loopcf] = hwProvider.alpha(ratesProvider.getValuationDate(), expiryDate, expiryDate, payment.getDate());
      discountedCashFlow[loopcf] = paymentPricer.presentValueAmount(payment, ratesProvider);
    }
    double omega = (swap.getLegs(SwapLegType.FIXED).get(0).getPayReceive().isPay() ? -1d : 1d);
    double kappa = computeKappa(hwProvider, discountedCashFlow, alpha, omega);
    PointSensitivityBuilder point = PointSensitivityBuilder.none();
    for (int loopcf = 0; loopcf < nPayments; loopcf++) {
      Payment payment = list.get(loopcf);
      double cdf = NORMAL.getCDF(omega * (kappa + alpha[loopcf]));
      point = point.combinedWith(paymentPricer.presentValueSensitivity(payment, ratesProvider).multipliedBy(cdf));
      if (!listSensi.get(loopcf).equals(PointSensitivityBuilder.none())) {
        point = point.combinedWith(listSensi.get(loopcf)
            .multipliedBy(cdf * ratesProvider.discountFactor(payment.getCurrency(), payment.getDate())));
      }
    }
    return swaption.getLongShort().isLong() ? point : point.multipliedBy(-1d);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to piecewise constant volatility parameters of the Hull-White model.
   * 
   * @param swaption  the product
   * @param ratesProvider  the rates provider
   * @param hwProvider  the Hull-White model parameter provider
   * @return the present value Hull-White model parameter sensitivity of the swaption product
   */
  public DoubleArray presentValueSensitivityModelParamsHullWhite(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    validate(swaption, ratesProvider, hwProvider);
    ResolvedSwap swap = swaption.getUnderlying();
    LocalDate expiryDate = swaption.getExpiryDate();
    if (expiryDate.isBefore(ratesProvider.getValuationDate())) { // Option has expired already
      return DoubleArray.EMPTY;
    }
    ResolvedSwapLeg cashFlowEquiv = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, ratesProvider);
    int nPayments = cashFlowEquiv.getPaymentEvents().size();
    double[] alpha = new double[nPayments];
    double[][] alphaAdjoint = new double[nPayments][];
    double[] discountedCashFlow = new double[nPayments];
    for (int loopcf = 0; loopcf < nPayments; loopcf++) {
      NotionalExchange payment = (NotionalExchange) cashFlowEquiv.getPaymentEvents().get(loopcf);
      ValueDerivatives valueDeriv = hwProvider.alphaAdjoint(
          ratesProvider.getValuationDate(), expiryDate, expiryDate, payment.getPaymentDate());
      alpha[loopcf] = valueDeriv.getValue();
      alphaAdjoint[loopcf] = valueDeriv.getDerivatives().toArray();
      discountedCashFlow[loopcf] = paymentPricer.presentValueAmount(payment.getPayment(), ratesProvider);
    }
    double omega = (swap.getLegs(SwapLegType.FIXED).get(0).getPayReceive().isPay() ? -1d : 1d);
    double kappa = computeKappa(hwProvider, discountedCashFlow, alpha, omega);
    int nParams = alphaAdjoint[0].length;
    if (Math.abs(kappa) > 1d / SMALL) { // decays exponentially
      return DoubleArray.filled(nParams);
    }
    double[] pvSensi = new double[nParams];
    double sign = (swaption.getLongShort().isLong() ? 1d : -1d);
    for (int i = 0; i < nParams; ++i) {
      for (int loopcf = 0; loopcf < nPayments; loopcf++) {
        pvSensi[i] += sign * discountedCashFlow[loopcf] * NORMAL.getPDF(omega * (kappa + alpha[loopcf])) *
            omega * alphaAdjoint[loopcf][i];
      }
    }
    return DoubleArray.ofUnsafe(pvSensi);
  }

  //-------------------------------------------------------------------------
  // validate that the rates and volatilities providers are coherent
  private void validate(ResolvedSwaption swaption, RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {
    ArgChecker.isTrue(hwProvider.getValuationDateTime().toLocalDate().equals(ratesProvider.getValuationDate()),
        "Hull-White model data and rate data should be for the same date");
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "underlying swap should be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.PHYSICAL),
        "swaption should be physical settlement");
  }

  // handling short time to expiry
  private double computeKappa(HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider,
      double[] discountedCashFlow, double[] alpha, double omega) {
    double kappa = 0d;
    if (DoubleArrayMath.fuzzyEqualsZero(alpha, SMALL)) { // threshold coherent to rootfinder in kappa computation
      double totalPv = DoubleArrayMath.sum(discountedCashFlow);
      kappa = totalPv * omega > 0d ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
    } else {
      kappa = hwProvider.getModel().kappa(DoubleArray.ofUnsafe(discountedCashFlow), DoubleArray.ofUnsafe(alpha));
    }
    return kappa;
  }

}
