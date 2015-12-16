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
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.rate.IborRateObservation;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.NotionalExchange;
import com.opengamma.strata.product.swap.PaymentPeriod;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 * Computes cash flow equivalent of products.
 * <p>
 * Reference: Henrard, M. The Irony in the derivatives discounting Part II: the crisis. Wilmott Journal, 2010, 2, 301-316. 
 */
public final class CashFlowEquivalentCalculator {

  /**
   * Computes cash flow equivalent of swap. 
   * <p>
   * The swap should be a fix-for-Ibor swap without compounding, and its swap legs should not involve {@code PaymentEvent}.
   * <p>
   * The return type is {@code ExpandedSwapLeg} in which individual payments are represented in terms of {@code NotionalExchange}.
   * 
   * @param swap  the swap product
   * @param ratesProvider  the rates provider
   * @return the cash flow equivalent
   */
  public static ExpandedSwapLeg cashFlowEquivalentSwap(ExpandedSwap swap, RatesProvider ratesProvider) {
    validateSwap(swap);
    ExpandedSwapLeg cfFixed = cashFlowEquivalentFixedLeg(swap.getLegs(SwapLegType.FIXED).get(0), ratesProvider);
    ExpandedSwapLeg cfIbor = cashFlowEquivalentIborLeg(swap.getLegs(SwapLegType.IBOR).get(0), ratesProvider);
    ExpandedSwapLeg leg = ExpandedSwapLeg.builder()
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
   * The return type is {@code ExpandedSwapLeg} in which individual payments are represented in terms of {@code NotionalExchange}.
   * 
   * @param iborLeg  the Ibor leg
   * @param ratesProvider  the rates provider
   * @return the cash flow equivalent
   */
  public static ExpandedSwapLeg cashFlowEquivalentIborLeg(ExpandedSwapLeg iborLeg, RatesProvider ratesProvider) {
    ArgChecker.isTrue(iborLeg.getType().equals(SwapLegType.IBOR), "Leg type should be IBOR");
    ArgChecker.isTrue(iborLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    List<NotionalExchange> paymentEvents = new ArrayList<NotionalExchange>();
    for (PaymentPeriod paymentPeriod : iborLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "rate payment should be RatePaymentPeriod");
      RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
      ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "rate payment should not be compounding");
      RateAccrualPeriod rateAccrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
      CurrencyAmount notional = ratePaymentPeriod.getNotionalAmount();
      LocalDate paymentDate = ratePaymentPeriod.getPaymentDate();
      IborRateObservation obs = ((IborRateObservation) rateAccrualPeriod.getRateObservation());
      IborIndex index = obs.getIndex();
      LocalDate fixingStartDate = index.calculateEffectiveFromFixing(obs.getFixingDate());
      LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
      double fixingYearFraction = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
      double beta = (1d + fixingYearFraction * ratesProvider.iborIndexRates(index).rate(obs.getFixingDate()))
          * ratesProvider.discountFactor(paymentPeriod.getCurrency(), paymentPeriod.getPaymentDate())
          / ratesProvider.discountFactor(paymentPeriod.getCurrency(), fixingStartDate);
      double ycRatio = rateAccrualPeriod.getYearFraction() / fixingYearFraction;
      NotionalExchange payStart = NotionalExchange.of(fixingStartDate, notional.multipliedBy(beta * ycRatio));
      NotionalExchange payEnd = NotionalExchange.of(paymentDate, notional.multipliedBy(-ycRatio));
      paymentEvents.add(payStart);
      paymentEvents.add(payEnd);
    }
    ExpandedSwapLeg leg = ExpandedSwapLeg.builder()
        .paymentEvents(paymentEvents)
        .payReceive(PayReceive.RECEIVE)
        .type(SwapLegType.OTHER)
        .build();
    return leg;
  }

  /**
   * Computes cash flow equivalent of fixed leg. 
   * <p>
   * The return type is {@code ExpandedSwapLeg} in which individual payments are represented in terms of {@code NotionalExchange}.
   * 
   * @param fixedLeg  the fixed leg
   * @param ratesProvider  the rates provider
   * @return the cash flow equivalent
   */
  public static ExpandedSwapLeg cashFlowEquivalentFixedLeg(ExpandedSwapLeg fixedLeg, RatesProvider ratesProvider) {
    ArgChecker.isTrue(fixedLeg.getType().equals(SwapLegType.FIXED), "Leg type should be FIXED");
    ArgChecker.isTrue(fixedLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    List<NotionalExchange> paymentEvents = new ArrayList<NotionalExchange>();
    for (PaymentPeriod paymentPeriod : fixedLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "rate payment should be RatePaymentPeriod");
      RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
      ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "rate payment should not be compounding");
      RateAccrualPeriod rateAccrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
      double factor = rateAccrualPeriod.getYearFraction() *
          ((FixedRateObservation) rateAccrualPeriod.getRateObservation()).getRate();
      CurrencyAmount notional = ratePaymentPeriod.getNotionalAmount().multipliedBy(factor);
      LocalDate paymentDate = ratePaymentPeriod.getPaymentDate();
      NotionalExchange pay = NotionalExchange.of(paymentDate, notional);
      paymentEvents.add(pay);
    }
    ExpandedSwapLeg leg = ExpandedSwapLeg.builder()
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
  public static ImmutableMap<NotionalExchange, PointSensitivityBuilder> cashFlowEquivalentAndSensitivitySwap(
      ExpandedSwap swap,
      RatesProvider ratesProvider) {
    validateSwap(swap);
    ImmutableMap<NotionalExchange, PointSensitivityBuilder> mapFixed =
        cashFlowEquivalentAndSensitivityFixedLeg(swap.getLegs(SwapLegType.FIXED).get(0), ratesProvider);
    ImmutableMap<NotionalExchange, PointSensitivityBuilder> mapIbor =
        cashFlowEquivalentAndSensitivityIborLeg(swap.getLegs(SwapLegType.IBOR).get(0), ratesProvider);
    return ImmutableMap.<NotionalExchange, PointSensitivityBuilder>builder().putAll(mapFixed).putAll(mapIbor).build();
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
  public static ImmutableMap<NotionalExchange, PointSensitivityBuilder> cashFlowEquivalentAndSensitivityIborLeg(
      ExpandedSwapLeg iborLeg,
      RatesProvider ratesProvider) {
    ArgChecker.isTrue(iborLeg.getType().equals(SwapLegType.IBOR), "Leg type should be IBOR");
    ArgChecker.isTrue(iborLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    Map<NotionalExchange, PointSensitivityBuilder> res = new HashMap<NotionalExchange, PointSensitivityBuilder>();
    for (PaymentPeriod paymentPeriod : iborLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "rate payment should be RatePaymentPeriod");
      RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
      ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "rate payment should not be compounding");
      RateAccrualPeriod rateAccrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
      CurrencyAmount notional = ratePaymentPeriod.getNotionalAmount();
      LocalDate paymentDate = ratePaymentPeriod.getPaymentDate();
      IborRateObservation obs = ((IborRateObservation) rateAccrualPeriod.getRateObservation());
      IborIndex index = obs.getIndex();
      LocalDate fixingStartDate = index.calculateEffectiveFromFixing(obs.getFixingDate());
      LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
      double fixingYearFraction = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);

      double factorIndex = (1d + fixingYearFraction * ratesProvider.iborIndexRates(index).rate(obs.getFixingDate()));
      double dfPayment = ratesProvider.discountFactor(paymentPeriod.getCurrency(), paymentPeriod.getPaymentDate());
      double dfStart = ratesProvider.discountFactor(paymentPeriod.getCurrency(), fixingStartDate);
      double beta = factorIndex * dfPayment / dfStart;
      double ycRatio = rateAccrualPeriod.getYearFraction() / fixingYearFraction;
      NotionalExchange payStart = NotionalExchange.of(fixingStartDate, notional.multipliedBy(beta * ycRatio));
      NotionalExchange payEnd = NotionalExchange.of(paymentDate, notional.multipliedBy(-ycRatio));
      double factor = ycRatio * notional.getAmount() / dfStart;

      PointSensitivityBuilder factorIndexSensi = ratesProvider.iborIndexRates(index)
          .ratePointSensitivity(obs.getFixingDate()).multipliedBy(fixingYearFraction * dfPayment * factor);
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
  public static ImmutableMap<NotionalExchange, PointSensitivityBuilder> cashFlowEquivalentAndSensitivityFixedLeg(
      ExpandedSwapLeg fixedLeg,
      RatesProvider ratesProvider) {
    ArgChecker.isTrue(fixedLeg.getType().equals(SwapLegType.FIXED), "Leg type should be FIXED");
    ArgChecker.isTrue(fixedLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    Map<NotionalExchange, PointSensitivityBuilder> res = new HashMap<NotionalExchange, PointSensitivityBuilder>();
    for (PaymentPeriod paymentPeriod : fixedLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "rate payment should be RatePaymentPeriod");
      RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
      ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "rate payment should not be compounding");
      RateAccrualPeriod rateAccrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
      double factor = rateAccrualPeriod.getYearFraction() *
          ((FixedRateObservation) rateAccrualPeriod.getRateObservation()).getRate();
      CurrencyAmount notional = ratePaymentPeriod.getNotionalAmount().multipliedBy(factor);
      LocalDate paymentDate = ratePaymentPeriod.getPaymentDate();
      NotionalExchange pay = NotionalExchange.of(paymentDate, notional);
      res.put(pay, PointSensitivityBuilder.none());
    }
    return ImmutableMap.copyOf(res);
  }

  //-------------------------------------------------------------------------
  private static void validateSwap(ExpandedSwap swap) {
    ArgChecker.isTrue(swap.getLegs().size() == 2, "swap should have 2 legs");
    ArgChecker.isTrue(swap.getLegs(SwapLegType.FIXED).size() == 1, "swap should have unique fixed leg");
    ArgChecker.isTrue(swap.getLegs(SwapLegType.IBOR).size() == 1, "swap should have unique Ibor leg");
  }
}
