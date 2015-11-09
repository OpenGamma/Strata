/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFuture;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RateAccrualPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapLegType;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.HullWhiteOneFactorPiecewiseConstantProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.PaymentEventPricer;
import com.opengamma.strata.pricer.rate.swap.PaymentPeriodPricer;

/**
 * Pricer for for deliverable swap futures.
 * <p>
 * This function provides the ability to price a {@link DeliverableSwapFuture}.
 */
public class HullWhiteDeliverableSwapFutureProductPricer {

  public static final HullWhiteDeliverableSwapFutureProductPricer DEFAULT =
      new HullWhiteDeliverableSwapFutureProductPricer(
          PaymentPeriodPricer.instance(),
          PaymentEventPricer.instance());

  /**
   * Pricer for {@link PaymentPeriod}.
   */
  private final PaymentPeriodPricer<PaymentPeriod> paymentPeriodPricer;
  /**
   * Pricer for {@link PaymentEvent}.
   */
  private final PaymentEventPricer<PaymentEvent> paymentEventPricer;

  /**
   * Creates an instance.
   * 
   * @param paymentPeriodPricer  the pricer for {@link PaymentPeriod}
   * @param paymentEventPricer  the pricer for {@link PaymentEvent}
   */
  public HullWhiteDeliverableSwapFutureProductPricer(
      PaymentPeriodPricer<PaymentPeriod> paymentPeriodPricer,
      PaymentEventPricer<PaymentEvent> paymentEventPricer) {
    this.paymentPeriodPricer = ArgChecker.notNull(paymentPeriodPricer, "paymentPeriodPricer");
    this.paymentEventPricer = ArgChecker.notNull(paymentEventPricer, "paymentEventPricer");
  }

  //-------------------------------------------------------------------------
  public double price(DeliverableSwapFuture futures, RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantProvider hullWhiteProvider) {
    // TODO check valuation date equal
    Swap swap = futures.getUnderlyingSwap();
    SwapLeg fixedLeg = swap.getLegs(SwapLegType.FIXED).get(0);
    SwapLeg iborLeg = swap.getLegs(SwapLegType.IBOR).get(0);
    double pv = presentValuePeriodsFixedLeg(fixedLeg.expand(), ratesProvider, hullWhiteProvider, futures)
        + presentValuePeriodsIborLeg(iborLeg.expand(), ratesProvider, hullWhiteProvider, futures);
    double df0 = ratesProvider.discountFactor(futures.getCurrency(), futures.getDeliveryDate());
    return 1d + pv / df0;
  }

  public PointSensitivities priceSensitivity(DeliverableSwapFuture futures, RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantProvider hullWhiteProvider) {
    // TODO check valuation date equal
    Swap swap = futures.getUnderlyingSwap();
    SwapLeg fixedLeg = swap.getLegs(SwapLegType.FIXED).get(0);
    SwapLeg iborLeg = swap.getLegs(SwapLegType.IBOR).get(0);
    double pv = presentValuePeriodsFixedLeg(fixedLeg.expand(), ratesProvider, hullWhiteProvider, futures)
        + presentValuePeriodsIborLeg(iborLeg.expand(), ratesProvider, hullWhiteProvider, futures);
    double df0 = ratesProvider.discountFactor(futures.getCurrency(), futures.getDeliveryDate());
    PointSensitivityBuilder pvSensi = presentValueSensitivityPeriodsFixedLeg(fixedLeg.expand(), ratesProvider,
        hullWhiteProvider, futures).combinedWith(
        presentValueSensitivityPeriodsIborLeg(iborLeg.expand(), ratesProvider, hullWhiteProvider, futures))
        .multipliedBy(1d / df0);
    PointSensitivityBuilder df0Sensi = ratesProvider.discountFactors(futures.getCurrency())
        .zeroRatePointSensitivity(futures.getDeliveryDate()).multipliedBy(-pv / df0 / df0);
    return pvSensi.combinedWith(df0Sensi).build();
  }

  //-------------------------------------------------------------------------
  private double presentValuePeriodsFixedLeg(ExpandedSwapLeg leg, RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantProvider hullWhiteProvider, DeliverableSwapFuture futures) {
    double total = 0d;
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(ratesProvider.getValuationDate())) {
        double adjustment = hullWhiteProvider.futuresConvexityFactor(futures.getLastTradeDate(),
            period.getPaymentDate(), futures.getDeliveryDate());
        total += paymentPeriodPricer.presentValue(period, ratesProvider) * adjustment;
      }
    }
    return total;
  }

  private double presentValuePeriodsIborLeg(ExpandedSwapLeg leg, RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantProvider hullWhiteProvider, DeliverableSwapFuture futures) {
    double total = 0d;
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(ratesProvider.getValuationDate())) {
        RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) period;
        total += presentValuePaymentPeriod(
            ratePaymentPeriod, ratesProvider, hullWhiteProvider, futures.getLastTradeDate(), futures.getDeliveryDate());
      }
    }
    // TODO no notional exchange?
    return total;
  }

  private double presentValuePaymentPeriod(RatePaymentPeriod period, RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantProvider hullWhiteProvider, LocalDate lastTradeDate, LocalDate deliveryDate) {
    RateAccrualPeriod accrualPeriod = period.getAccrualPeriods().get(0);
    double paymentYearFraction = accrualPeriod.getYearFraction();
    IborRateObservation obs = ((IborRateObservation) accrualPeriod.getRateObservation());
    IborIndex index = obs.getIndex();
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(obs.getFixingDate());
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    double fixingYearFraction = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
    double betaDf1 = (1d + fixingYearFraction * ratesProvider.iborIndexRates(index).rate(obs.getFixingDate()))
        * ratesProvider.discountFactor(period.getCurrency(), period.getPaymentDate());

    double df2 = ratesProvider.discountFactor(period.getCurrency(), period.getPaymentDate());
    double adjustment1 = hullWhiteProvider.futuresConvexityFactor(lastTradeDate, fixingStartDate, deliveryDate);
    double adjustment2 = hullWhiteProvider.futuresConvexityFactor(lastTradeDate, period.getPaymentDate(), deliveryDate);

    // TODO no notional exchange?
    return (betaDf1 * adjustment1 - adjustment2 * df2) * paymentYearFraction * period.getNotional() /
        fixingYearFraction;
  }

  //-------------------------------------------------------------------------
  private PointSensitivityBuilder presentValueSensitivityPeriodsFixedLeg(ExpandedSwapLeg leg,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantProvider hullWhiteProvider, DeliverableSwapFuture futures) {
    PointSensitivityBuilder total = PointSensitivityBuilder.none();
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(ratesProvider.getValuationDate())) {
        double adjustment = hullWhiteProvider.futuresConvexityFactor(futures.getLastTradeDate(),
            period.getPaymentDate(), futures.getDeliveryDate());
        total = total.combinedWith(paymentPeriodPricer.presentValueSensitivity(period, ratesProvider).multipliedBy(
            adjustment));
      }
    }
    return total;
  }

  private PointSensitivityBuilder presentValueSensitivityPeriodsIborLeg(ExpandedSwapLeg leg,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantProvider hullWhiteProvider, DeliverableSwapFuture futures) {
    PointSensitivityBuilder total = PointSensitivityBuilder.none();
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(ratesProvider.getValuationDate())) {
        RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) period;
        total = total
            .combinedWith(presentValueSensitivityPaymentPeriod(
                ratePaymentPeriod, ratesProvider, hullWhiteProvider, futures.getLastTradeDate(),
                futures.getDeliveryDate()));
      }
    }
    // TODO no notional exchange?
    return total;
  }

  private PointSensitivityBuilder presentValueSensitivityPaymentPeriod(RatePaymentPeriod period,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantProvider hullWhiteProvider, LocalDate lastTradeDate, LocalDate deliveryDate) {
    RateAccrualPeriod accrualPeriod = period.getAccrualPeriods().get(0);
    double paymentYearFraction = accrualPeriod.getYearFraction();
    IborRateObservation obs = ((IborRateObservation) accrualPeriod.getRateObservation());
    IborIndex index = obs.getIndex();
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(obs.getFixingDate());
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    LocalDate paymentDate = period.getPaymentDate();
    double fixingYearFraction = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
    Currency currency = period.getCurrency();

    double adjustment1 = hullWhiteProvider.futuresConvexityFactor(lastTradeDate, fixingStartDate, deliveryDate);
    double adjustment2 = hullWhiteProvider.futuresConvexityFactor(lastTradeDate, paymentDate, deliveryDate);

    PointSensitivityBuilder betaDf1Sensi1 = ratesProvider.iborIndexRates(index).ratePointSensitivity(obs.getFixingDate())
        .multipliedBy(fixingYearFraction * ratesProvider.discountFactor(currency, paymentDate) * adjustment1);
    PointSensitivityBuilder betaDf1Sensi2 = ratesProvider.discountFactors(currency).zeroRatePointSensitivity(paymentDate)
        .multipliedBy(
            (1d + fixingYearFraction * ratesProvider.iborIndexRates(index).rate(obs.getFixingDate())) * adjustment1);
    PointSensitivityBuilder df2Sensi = ratesProvider.discountFactors(currency).zeroRatePointSensitivity(paymentDate)
        .multipliedBy(-adjustment2);

    // TODO no notional exchange?
    return betaDf1Sensi1.combinedWith(betaDf1Sensi2).combinedWith(df2Sensi)
        .multipliedBy(paymentYearFraction * period.getNotional() / fixingYearFraction);
  }
}
