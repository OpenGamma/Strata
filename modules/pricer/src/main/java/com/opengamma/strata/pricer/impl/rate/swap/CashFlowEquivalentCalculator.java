/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.swap.NotionalExchange;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;

/**
 * Computes cash flow equivalent of products.
 * <p>
 * Reference: Henrard, M. The Irony in the derivatives discounting Part II: the crisis. Wilmott Journal, 2010, 2, 301-316.
 */
public final class CashFlowEquivalentCalculator {

  /**
   * Computes cash flow equivalent of swap.
   * <p>
   * The swap should be a fix-for-Ibor swap without compounding, and its swap legs
   * should not involve {@code PaymentEvent}.
   * <p>
   * The return type is {@code ResolvedSwapLeg} in which individual payments are
   * represented in terms of {@code NotionalExchange}.
   * 
   * @param swap  the swap product
   * @param ratesProvider  the rates provider
   * @return the cash flow equivalent
   */
  public static ResolvedSwapLeg cashFlowEquivalentSwap(ResolvedSwap swap, RatesProvider ratesProvider) {
    validateSwap(swap);
    ResolvedSwapLeg cfFixed = cashFlowEquivalentFixedLeg(swap.getLegs(SwapLegType.FIXED).get(0), ratesProvider);
    ResolvedSwapLeg cfIbor = cashFlowEquivalentIborLeg(swap.getLegs(SwapLegType.IBOR).get(0), ratesProvider);
    ResolvedSwapLeg leg = ResolvedSwapLeg.builder()
        .paymentEvents(
            Stream.concat(cfFixed.getPaymentEvents().stream(), cfIbor.getPaymentEvents().stream()).collect(Collectors.toList()))
        .payReceive(PayReceive.RECEIVE)
        .type(SwapLegType.OTHER)
        .build();
    return leg;
  }

  /**
   * Computes cash flow equivalent of Ibor leg.
   * <p>
   * The return type is {@code ResolvedSwapLeg} in which individual payments are
   * represented in terms of {@code NotionalExchange}.
   * 
   * @param iborLeg  the Ibor leg
   * @param ratesProvider  the rates provider
   * @return the cash flow equivalent
   */
  public static ResolvedSwapLeg cashFlowEquivalentIborLeg(ResolvedSwapLeg iborLeg, RatesProvider ratesProvider) {
    ArgChecker.isTrue(iborLeg.getType().equals(SwapLegType.IBOR), "Leg type should be IBOR");
    ArgChecker.isTrue(iborLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    List<NotionalExchange> paymentEvents = new ArrayList<NotionalExchange>();
    for (SwapPaymentPeriod paymentPeriod : iborLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "rate payment should be RatePaymentPeriod");
      RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
      ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "rate payment should not be compounding");
      RateAccrualPeriod rateAccrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
      CurrencyAmount notional = ratePaymentPeriod.getNotionalAmount();
      LocalDate paymentDate = ratePaymentPeriod.getPaymentDate();
      IborIndexObservation obs = ((IborRateComputation) rateAccrualPeriod.getRateComputation()).getObservation();
      IborIndex index = obs.getIndex();
      LocalDate fixingStartDate = obs.getEffectiveDate();
      double fixingYearFraction = obs.getYearFraction();
      double beta = (1d + fixingYearFraction * ratesProvider.iborIndexRates(index).rate(obs)) *
          ratesProvider.discountFactor(paymentPeriod.getCurrency(), paymentPeriod.getPaymentDate()) /
          ratesProvider.discountFactor(paymentPeriod.getCurrency(), fixingStartDate);
      double ycRatio = rateAccrualPeriod.getYearFraction() / fixingYearFraction;
      NotionalExchange payStart = NotionalExchange.of(notional.multipliedBy(beta * ycRatio), fixingStartDate);
      NotionalExchange payEnd = NotionalExchange.of(notional.multipliedBy(-ycRatio), paymentDate);
      paymentEvents.add(payStart);
      paymentEvents.add(payEnd);
    }
    ResolvedSwapLeg leg = ResolvedSwapLeg.builder()
        .paymentEvents(paymentEvents)
        .payReceive(PayReceive.RECEIVE)
        .type(SwapLegType.OTHER)
        .build();
    return leg;
  }

  /**
   * Computes cash flow equivalent of fixed leg.
   * <p>
   * The return type is {@code ResolvedSwapLeg} in which individual payments are
   * represented in terms of {@code NotionalExchange}.
   * 
   * @param fixedLeg  the fixed leg
   * @param ratesProvider  the rates provider
   * @return the cash flow equivalent
   */
  public static ResolvedSwapLeg cashFlowEquivalentFixedLeg(ResolvedSwapLeg fixedLeg, RatesProvider ratesProvider) {
    ArgChecker.isTrue(fixedLeg.getType().equals(SwapLegType.FIXED), "Leg type should be FIXED");
    ArgChecker.isTrue(fixedLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    List<NotionalExchange> paymentEvents = new ArrayList<NotionalExchange>();
    for (SwapPaymentPeriod paymentPeriod : fixedLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "rate payment should be RatePaymentPeriod");
      RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
      ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "rate payment should not be compounding");
      RateAccrualPeriod rateAccrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
      double factor = rateAccrualPeriod.getYearFraction() *
          ((FixedRateComputation) rateAccrualPeriod.getRateComputation()).getRate();
      CurrencyAmount notional = ratePaymentPeriod.getNotionalAmount().multipliedBy(factor);
      LocalDate paymentDate = ratePaymentPeriod.getPaymentDate();
      NotionalExchange pay = NotionalExchange.of(notional, paymentDate);
      paymentEvents.add(pay);
    }
    ResolvedSwapLeg leg = ResolvedSwapLeg.builder()
        .paymentEvents(paymentEvents)
        .payReceive(PayReceive.RECEIVE)
        .type(SwapLegType.OTHER)
        .build();
    return leg;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes cash flow equivalent and sensitivity of swap.
   * <p>
   * The swap should be a fix-for-Ibor swap without compounding, and its swap legs should not involve {@code PaymentEvent}.
   * <p>
   * The return type is a map of {@code NotionalExchange} and {@code PointSensitivityBuilder}.
   * 
   * @param swap  the swap product
   * @param ratesProvider  the rates provider
   * @return the cash flow equivalent and sensitivity
   */
  public static ImmutableMap<Payment, PointSensitivityBuilder> cashFlowEquivalentAndSensitivitySwap(
      ResolvedSwap swap,
      RatesProvider ratesProvider) {

    validateSwap(swap);
    ImmutableMap<Payment, PointSensitivityBuilder> mapFixed =
        cashFlowEquivalentAndSensitivityFixedLeg(swap.getLegs(SwapLegType.FIXED).get(0), ratesProvider);
    ImmutableMap<Payment, PointSensitivityBuilder> mapIbor =
        cashFlowEquivalentAndSensitivityIborLeg(swap.getLegs(SwapLegType.IBOR).get(0), ratesProvider);
    return ImmutableMap.<Payment, PointSensitivityBuilder>builder().putAll(mapFixed).putAll(mapIbor).build();
  }

  /**
   * Computes cash flow equivalent and sensitivity of Ibor leg.
   * <p>
   * The return type is a map of {@code NotionalExchange} and {@code PointSensitivityBuilder}.
   * 
   * @param iborLeg  the Ibor leg
   * @param ratesProvider  the rates provider
   * @return the cash flow equivalent and sensitivity
   */
  public static ImmutableMap<Payment, PointSensitivityBuilder> cashFlowEquivalentAndSensitivityIborLeg(
      ResolvedSwapLeg iborLeg,
      RatesProvider ratesProvider) {

    ArgChecker.isTrue(iborLeg.getType().equals(SwapLegType.IBOR), "Leg type should be IBOR");
    ArgChecker.isTrue(iborLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    Map<Payment, PointSensitivityBuilder> res = new HashMap<Payment, PointSensitivityBuilder>();
    for (SwapPaymentPeriod paymentPeriod : iborLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "rate payment should be RatePaymentPeriod");
      RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
      ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "rate payment should not be compounding");
      RateAccrualPeriod rateAccrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
      CurrencyAmount notional = ratePaymentPeriod.getNotionalAmount();
      LocalDate paymentDate = ratePaymentPeriod.getPaymentDate();
      IborIndexObservation obs = ((IborRateComputation) rateAccrualPeriod.getRateComputation()).getObservation();
      IborIndex index = obs.getIndex();
      LocalDate fixingStartDate = obs.getEffectiveDate();
      double fixingYearFraction = obs.getYearFraction();

      double factorIndex = (1d + fixingYearFraction * ratesProvider.iborIndexRates(index).rate(obs));
      double dfPayment = ratesProvider.discountFactor(paymentPeriod.getCurrency(), paymentPeriod.getPaymentDate());
      double dfStart = ratesProvider.discountFactor(paymentPeriod.getCurrency(), fixingStartDate);
      double beta = factorIndex * dfPayment / dfStart;
      double ycRatio = rateAccrualPeriod.getYearFraction() / fixingYearFraction;
      Payment payStart = Payment.of(notional.multipliedBy(beta * ycRatio), fixingStartDate);
      Payment payEnd = Payment.of(notional.multipliedBy(-ycRatio), paymentDate);
      double factor = ycRatio * notional.getAmount() / dfStart;

      PointSensitivityBuilder factorIndexSensi = ratesProvider.iborIndexRates(index)
          .ratePointSensitivity(obs).multipliedBy(fixingYearFraction * dfPayment * factor);
      PointSensitivityBuilder dfPaymentSensitivity = ratesProvider.discountFactors(paymentPeriod.getCurrency())
          .zeroRatePointSensitivity(paymentPeriod.getPaymentDate()).multipliedBy(factorIndex * factor);
      PointSensitivityBuilder dfStartSensitivity = ratesProvider.discountFactors(paymentPeriod.getCurrency())
          .zeroRatePointSensitivity(fixingStartDate).multipliedBy(-factorIndex * dfPayment * factor / dfStart);
      res.put(payStart, factorIndexSensi.combinedWith(dfPaymentSensitivity).combinedWith(dfStartSensitivity));
      res.put(payEnd, PointSensitivityBuilder.none());
    }
    return ImmutableMap.copyOf(res);
  }

  /**
   * Computes cash flow equivalent and sensitivity of fixed leg.
   * <p>
   * The return type is a map of {@code NotionalExchange} and {@code PointSensitivityBuilder}.
   * 
   * @param fixedLeg  the fixed leg
   * @param ratesProvider  the rates provider
   * @return the cash flow equivalent and sensitivity
   */
  public static ImmutableMap<Payment, PointSensitivityBuilder> cashFlowEquivalentAndSensitivityFixedLeg(
      ResolvedSwapLeg fixedLeg,
      RatesProvider ratesProvider) {

    ArgChecker.isTrue(fixedLeg.getType().equals(SwapLegType.FIXED), "Leg type should be FIXED");
    ArgChecker.isTrue(fixedLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    Map<Payment, PointSensitivityBuilder> res = new HashMap<Payment, PointSensitivityBuilder>();
    for (SwapPaymentPeriod paymentPeriod : fixedLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "rate payment should be RatePaymentPeriod");
      RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
      ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "rate payment should not be compounding");
      RateAccrualPeriod rateAccrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
      double factor = rateAccrualPeriod.getYearFraction() *
          ((FixedRateComputation) rateAccrualPeriod.getRateComputation()).getRate();
      CurrencyAmount notional = ratePaymentPeriod.getNotionalAmount().multipliedBy(factor);
      LocalDate paymentDate = ratePaymentPeriod.getPaymentDate();
      Payment pay = Payment.of(notional, paymentDate);
      res.put(pay, PointSensitivityBuilder.none());
    }
    return ImmutableMap.copyOf(res);
  }

  //-------------------------------------------------------------------------
  private static void validateSwap(ResolvedSwap swap) {
    ArgChecker.isTrue(swap.getLegs().size() == 2, "swap should have 2 legs");
    ArgChecker.isTrue(swap.getLegs(SwapLegType.FIXED).size() == 1, "swap should have unique fixed leg");
    ArgChecker.isTrue(swap.getLegs(SwapLegType.IBOR).size() == 1, "swap should have unique Ibor leg");
  }

}
