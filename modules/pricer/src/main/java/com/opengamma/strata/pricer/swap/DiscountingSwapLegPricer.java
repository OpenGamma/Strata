/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.BiFunction;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.KnownAmountSwapPaymentPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapPaymentEvent;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;

/**
 * Pricer for for rate swap legs.
 * <p>
 * This function provides the ability to price a {@link ResolvedSwapLeg}.
 * The product is priced by pricing each period and event.
 */
public class DiscountingSwapLegPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingSwapLegPricer DEFAULT = new DiscountingSwapLegPricer(
      SwapPaymentPeriodPricer.standard(),
      SwapPaymentEventPricer.standard());

  /**
   * Pricer for {@link SwapPaymentPeriod}.
   */
  private final SwapPaymentPeriodPricer<SwapPaymentPeriod> paymentPeriodPricer;
  /**
   * Pricer for {@link SwapPaymentEvent}.
   */
  private final SwapPaymentEventPricer<SwapPaymentEvent> paymentEventPricer;

  /* Small parameter below which the cash annuity formula is modified. */
  private static final double MIN_YIELD = 1.0E-4;

  /**
   * Creates an instance.
   * 
   * @param paymentPeriodPricer  the pricer for {@link SwapPaymentPeriod}
   * @param paymentEventPricer  the pricer for {@link SwapPaymentEvent}
   */
  public DiscountingSwapLegPricer(
      SwapPaymentPeriodPricer<SwapPaymentPeriod> paymentPeriodPricer,
      SwapPaymentEventPricer<SwapPaymentEvent> paymentEventPricer) {
    this.paymentPeriodPricer = ArgChecker.notNull(paymentPeriodPricer, "paymentPeriodPricer");
    this.paymentEventPricer = ArgChecker.notNull(paymentEventPricer, "paymentEventPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying leg pricer.
   * 
   * @return the leg pricer
   */
  public SwapPaymentPeriodPricer<SwapPaymentPeriod> getPeriodPricer() {
    return paymentPeriodPricer;
  }

  /**
   * Gets the underlying leg pricer.
   * 
   * @return the leg pricer
   */
  public SwapPaymentEventPricer<SwapPaymentEvent> getEventPricer() {
    return paymentEventPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swap leg, converted to the specified currency.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * This is the discounted forecast value.
   * The result is converted to the specified currency.
   * 
   * @param leg  the leg
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value of the swap leg in the specified currency
   */
  public CurrencyAmount presentValue(ResolvedSwapLeg leg, Currency currency, RatesProvider provider) {
    double pv = presentValueInternal(leg, provider);
    return CurrencyAmount.of(currency, (pv * provider.fxRate(leg.getCurrency(), currency)));
  }

  /**
   * Calculates the present value of the swap leg.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * This is the discounted forecast value.
   * The result is returned using the payment currency of the leg.
   * 
   * @param leg  the leg
   * @param provider  the rates provider
   * @return the present value of the swap leg
   */
  public CurrencyAmount presentValue(ResolvedSwapLeg leg, RatesProvider provider) {
    return CurrencyAmount.of(leg.getCurrency(), presentValueInternal(leg, provider));
  }

  // calculates the present value in the currency of the swap leg
  protected double presentValueInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    return presentValuePeriodsInternal(leg, provider) + presentValueEventsInternal(leg, provider);
  }

  /**
   * Calculates the forecast value of the swap leg.
   * <p>
   * The forecast value of the leg is the value on the valuation date without present value discounting.
   * The result is returned using the payment currency of the leg.
   * 
   * @param leg  the leg
   * @param provider  the rates provider
   * @return the forecast value of the swap leg
   */
  public CurrencyAmount forecastValue(ResolvedSwapLeg leg, RatesProvider provider) {
    return CurrencyAmount.of(leg.getCurrency(), forecastValueInternal(leg, provider));
  }

  // calculates the present value in the currency of the swap leg
  double forecastValueInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    return forecastValuePeriodsInternal(leg, provider) + forecastValueEventsInternal(leg, provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the accrued interest since the last payment.
   * <p>
   * This determines the payment period applicable at the valuation date and calculates
   * the accrued interest since the last payment.
   * The result is returned using the payment currency of the leg.
   * 
   * @param leg  the leg
   * @param provider  the rates provider
   * @return the accrued interest of the swap leg
   */
  public CurrencyAmount accruedInterest(ResolvedSwapLeg leg, RatesProvider provider) {
    Optional<SwapPaymentPeriod> period = leg.findPaymentPeriod(provider.getValuationDate());
    if (period.isPresent()) {
      double accruedInterest = paymentPeriodPricer.accruedInterest(period.get(), provider);
      return CurrencyAmount.of(leg.getCurrency(), accruedInterest);
    }
    return CurrencyAmount.zero(leg.getCurrency());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the Present Value of a Basis Point for a swap leg.
   * <p>
   * The Present Value of a Basis Point is the value of the leg when the rate is equal to 1.
   * A better name would be "Present Value of 1".
   * The quantity is also known as "physical annuity" or "level".
   * <p>
   * Each period must not have FX reset or compounding.
   * They must not be of type {@link KnownAmountSwapPaymentPeriod}.
   * 
   * @param leg  the swap leg
   * @param provider  the rates provider
   * @return the Present Value of a Basis Point
   */
  public double pvbp(ResolvedSwapLeg leg, RatesProvider provider) {
    double pvbpLeg = 0d;
    for (SwapPaymentPeriod period : leg.getPaymentPeriods()) {
      pvbpLeg += paymentPeriodPricer.pvbp(period, provider);
    }
    return pvbpLeg;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the coupon equivalent of a swap leg.
   * <p>
   * The coupon equivalent is the common fixed coupon for all the periods which would
   * result in the same present value of the leg.
   * <p>
   * This is used in particular for swaption pricing with a model on the swap rate.
   * 
   * @param leg  the swap leg
   * @param provider  the rates provider
   * @param pvbp  the present value of a basis point
   * @return the fixed coupon equivalent
   */
  public double couponEquivalent(ResolvedSwapLeg leg, RatesProvider provider, double pvbp) {
    return presentValuePeriodsInternal(leg, provider) / pvbp;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swap leg.
   * <p>
   * The present value sensitivity of the leg is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param leg  the leg
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap leg
   */
  public PointSensitivityBuilder presentValueSensitivity(ResolvedSwapLeg leg, RatesProvider provider) {
    return legValueSensitivity(
        leg,
        provider,
        paymentPeriodPricer::presentValueSensitivity,
        paymentEventPricer::presentValueSensitivity);
  }

  /**
   * Calculates the forecast value sensitivity of the swap leg.
   * <p>
   * The forecast value sensitivity of the leg is the sensitivity of the forecast value to
   * the underlying curves.
   * 
   * @param leg  the leg
   * @param provider  the rates provider
   * @return the forecast value curve sensitivity of the swap leg
   */
  public PointSensitivityBuilder forecastValueSensitivity(ResolvedSwapLeg leg, RatesProvider provider) {
    return legValueSensitivity(
        leg,
        provider,
        paymentPeriodPricer::forecastValueSensitivity,
        paymentEventPricer::forecastValueSensitivity);
  }

  // calculate present or forecast value sensitivity for a leg
  private PointSensitivityBuilder legValueSensitivity(
      ResolvedSwapLeg leg,
      RatesProvider provider,
      BiFunction<SwapPaymentPeriod, RatesProvider, PointSensitivityBuilder> periodFn,
      BiFunction<SwapPaymentEvent, RatesProvider, PointSensitivityBuilder> eventFn) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (SwapPaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        builder = builder.combinedWith(periodFn.apply(period, provider));
      }
    }
    for (SwapPaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        builder = builder.combinedWith(eventFn.apply(event, provider));
      }
    }
    return builder;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the Present Value of a Basis Point curve sensitivity for a fixed swap leg.
   * <p>
   * The Present Value of a Basis Point is the value of the leg when the rate is equal to 1.
   * A better name would be "Present Value of 1".
   * The quantity is also known as "physical annuity" or "level".
   * <p>
   * Each period must not have FX reset or compounding.
   * They must not be of type {@link KnownAmountSwapPaymentPeriod}.
   * 
   * @param fixedLeg  the swap fixed leg
   * @param provider  the rates provider
   * @return the Present Value of a Basis Point sensitivity to the curves
   */
  public PointSensitivityBuilder pvbpSensitivity(ResolvedSwapLeg fixedLeg, RatesProvider provider) {
    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (SwapPaymentPeriod period : fixedLeg.getPaymentPeriods()) {
      builder = builder.combinedWith(paymentPeriodPricer.pvbpSensitivity(period, provider));
    }
    return builder;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the conventional cash annuity from a swap leg.
   * <p>
   * The computation is relevant only for standard swaps with constant notional and regular payments.
   * The swap leg must be a fixed leg. However, this is not checked internally.
   * 
   * @param fixedLeg  the fixed leg of the swap
   * @param yield  the yield
   * @return the cash annuity
   */
  public double annuityCash(ResolvedSwapLeg fixedLeg, double yield) {
    int nbFixedPeriod = fixedLeg.getPaymentPeriods().size();
    SwapPaymentPeriod paymentPeriod = fixedLeg.getPaymentPeriods().get(0);
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "payment period should be RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    int nbFixedPaymentYear = (int) Math.round(1d /
        ratePaymentPeriod.getDayCount().yearFraction(ratePaymentPeriod.getStartDate(), ratePaymentPeriod.getEndDate()));
    double notional = Math.abs(ratePaymentPeriod.getNotional());
    double annuityCash = notional * annuityCash(nbFixedPaymentYear, nbFixedPeriod, yield);
    return annuityCash;
  }

  /**
   * Computes the conventional cash annuity for a given yield.
   * 
   * @param nbPaymentsPerYear  the number of payment per year
   * @param nbPeriods  the total number of periods
   * @param yield  the yield
   * @return the cash annuity
   */
  public double annuityCash(int nbPaymentsPerYear, int nbPeriods, double yield) {
    double tau = 1d / nbPaymentsPerYear;
    if (Math.abs(yield) > MIN_YIELD) {
      return (1d - Math.pow(1d + yield * tau, -nbPeriods)) / yield;
    }
    double annuity = 0.0d;
    double periodFactor = 1.0d / (1.0d + yield * tau);
    double multiPeriodFactor = periodFactor;
    for (int i = 0; i < nbPeriods; i++) {
      annuity += multiPeriodFactor;
      multiPeriodFactor *= periodFactor;
    }
    annuity *= tau;
    return annuity;
  }

  /**
   * Computes the conventional cash annuity for a given yield and its first derivative with respect to the yield.
   * 
   * @param nbPaymentsPerYear  the number of payment per year
   * @param nbPeriods  the total number of periods
   * @param yield  the yield
   * @return the cash annuity and its first derivative
   */
  public ValueDerivatives annuityCash1(int nbPaymentsPerYear, int nbPeriods, double yield) {
    double tau = 1d / nbPaymentsPerYear;
    if (Math.abs(yield) > MIN_YIELD) {
      double yieldPerPeriod = yield * tau;
      double dfEnd = Math.pow(1d + yieldPerPeriod, -nbPeriods);
      double annuity = (1d - dfEnd) / yield;
      double derivative = -annuity / yield;
      derivative += tau * nbPeriods * dfEnd / ((1d + yieldPerPeriod) * yield);
      return ValueDerivatives.of(annuity, DoubleArray.of(derivative));
    }
    double annuity = 0.0d;
    double derivative = 0.0d;
    double periodFactor = 1.0d / (1.0d + yield * tau);
    double multiPeriodFactor = periodFactor;
    for (int i = 0; i < nbPeriods; i++) {
      annuity += multiPeriodFactor;
      multiPeriodFactor *= periodFactor;
      derivative += -(i + 1) * multiPeriodFactor;
    }
    annuity *= tau;
    derivative *= tau * tau;
    return ValueDerivatives.of(annuity, DoubleArray.of(derivative));
  }

  /**
   * Computes the conventional cash annuity for a given yield and its first two derivatives with respect to the yield.
   * 
   * @param nbPaymentsPerYear  the number of payment per year
   * @param nbPeriods  the total number of periods
   * @param yield  the yield
   * @return the cash annuity and its first two derivatives
   */
  public ValueDerivatives annuityCash2(int nbPaymentsPerYear, int nbPeriods, double yield) {
    double tau = 1d / nbPaymentsPerYear;
    if (Math.abs(yield) > MIN_YIELD) {
      double yieldPerPeriod = yield * tau;
      double dfEnd = Math.pow(1d + yieldPerPeriod, -nbPeriods);
      double annuity = (1d - dfEnd) / yield;
      double derivative1 = -annuity / yield;
      derivative1 += tau * nbPeriods * dfEnd / ((1d + yieldPerPeriod) * yield);
      double derivative2 = -2 * derivative1 / yield;
      derivative2 -= tau * tau * nbPeriods * (nbPeriods + 1) * dfEnd / ((1d + yieldPerPeriod) * (1d + yieldPerPeriod) * yield);
      return ValueDerivatives.of(annuity, DoubleArray.of(derivative1, derivative2));
    }
    double annuity = 0.0d;
    double derivative1 = 0.0d;
    double derivative2 = 0.0d;
    double periodFactor = 1.0d / (1.0d + yield * tau);
    double multiPeriodFactor = periodFactor;
    for (int i = 0; i < nbPeriods; i++) {
      annuity += multiPeriodFactor;
      multiPeriodFactor *= periodFactor;
      derivative1 += -(i + 1) * multiPeriodFactor;
      derivative2 += (i + 1) * (i + 2) * multiPeriodFactor * periodFactor;
    }
    annuity *= tau;
    derivative1 *= tau * tau;
    derivative2 *= tau * tau * tau;
    return ValueDerivatives.of(annuity, DoubleArray.of(derivative1, derivative2));
  }

  /**
   * Computes the conventional cash annuity for a given yield and its first three derivatives with respect to the yield.
   * 
   * @param nbPaymentsPerYear  the number of payment per year
   * @param nbPeriods  the total number of periods
   * @param yield  the yield
   * @return the cash annuity and its first three derivatives
   */
  public ValueDerivatives annuityCash3(int nbPaymentsPerYear, int nbPeriods, double yield) {
    double tau = 1d / nbPaymentsPerYear;
    if (Math.abs(yield) > MIN_YIELD) {
      double yieldPerPeriod = yield * tau;
      double dfEnd = Math.pow(1d + yieldPerPeriod, -nbPeriods);
      double annuity = (1d - dfEnd) / yield;
      double derivative1 = -annuity / yield;
      derivative1 += tau * nbPeriods * dfEnd / ((1d + yieldPerPeriod) * yield);
      double derivative2 = -2 * derivative1 / yield;
      derivative2 -= tau * tau * nbPeriods * (nbPeriods + 1) * dfEnd / ((1d + yieldPerPeriod) * (1d + yieldPerPeriod) * yield);
      double derivative3 = -6.0d * annuity / (yield * yield * yield);
      derivative3 += 6.0d * tau * nbPeriods / (yield * yield * yield) * dfEnd / (1d + yieldPerPeriod);
      derivative3 += 3.0d * tau * tau * nbPeriods * (nbPeriods + 1) * dfEnd /
          ((1d + yieldPerPeriod) * (1d + yieldPerPeriod) * yield * yield);
      derivative3 += tau * tau * tau * nbPeriods * (nbPeriods + 1) * (nbPeriods + 2) * dfEnd /
          ((1d + yieldPerPeriod) * (1d + yieldPerPeriod) * (1d + yieldPerPeriod) * yield);
      return ValueDerivatives.of(annuity, DoubleArray.of(derivative1, derivative2, derivative3));
    }
    double annuity = 0.0d;
    double derivative1 = 0.0d;
    double derivative2 = 0.0d;
    double derivative3 = 0.0d;
    double periodFactor = 1.0d / (1.0d + yield * tau);
    double multiPeriodFactor = periodFactor;
    for (int i = 0; i < nbPeriods; i++) {
      annuity += multiPeriodFactor;
      multiPeriodFactor *= periodFactor;
      derivative1 += -(i + 1) * multiPeriodFactor;
      derivative2 += (i + 1) * (i + 2) * multiPeriodFactor * periodFactor;
      derivative3 += -(i + 1) * (i + 2) * (i + 3) * multiPeriodFactor * periodFactor * periodFactor;
    }
    annuity *= tau;
    derivative1 *= tau * tau;
    derivative2 *= tau * tau * tau;
    derivative3 *= tau * tau * tau * tau;
    return ValueDerivatives.of(annuity, DoubleArray.of(derivative1, derivative2, derivative3));
  }

  /**
   * Computes the derivative of the conventional cash annuity with respect to the yield from a swap leg.
   * <p>
   * The computation is relevant only for standard swaps with constant notional and regular payments.
   * The swap leg must be a fixed leg. However, this is not checked internally.
   * 
   * @param fixedLeg  the fixed leg of the swap
   * @param yield  the yield
   * @return the cash annuity
   */
  public ValueDerivatives annuityCashDerivative(ResolvedSwapLeg fixedLeg, double yield) {
    int nbFixedPeriod = fixedLeg.getPaymentPeriods().size();
    SwapPaymentPeriod paymentPeriod = fixedLeg.getPaymentPeriods().get(0);
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "payment period should be RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    int nbFixedPaymentYear = (int) Math.round(1d /
        ratePaymentPeriod.getDayCount().yearFraction(ratePaymentPeriod.getStartDate(), ratePaymentPeriod.getEndDate()));
    double notional = Math.abs(ratePaymentPeriod.getNotional());
    ValueDerivatives annuityUnit = annuityCash1(nbFixedPaymentYear, nbFixedPeriod, yield);
    return ValueDerivatives.of(annuityUnit.getValue() * notional, annuityUnit.getDerivatives().multipliedBy(notional));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flows of the swap leg.
   * <p>
   * Each expected cash flow is added to the result.
   * This is based on {@link #forecastValue(ResolvedSwapLeg, RatesProvider)}.
   * 
   * @param leg  the swap leg for which the cash flows should be computed
   * @param provider  the rates provider
   * @return the cash flows
   */
  public CashFlows cashFlows(ResolvedSwapLeg leg, RatesProvider provider) {
    CashFlows cashFlowPeriods = cashFlowPeriodsInternal(leg, provider);
    CashFlows cashFlowEvents = cashFlowEventsInternal(leg, provider);
    return cashFlowPeriods.combinedWith(cashFlowEvents);
  }

  //-------------------------------------------------------------------------
  // calculates the forecast value of the events composing the leg in the currency of the swap leg
  double forecastValueEventsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (SwapPaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentEventPricer.forecastValue(event, provider);
      }
    }
    return total;
  }

  // calculates the forecast value of the periods composing the leg in the currency of the swap leg
  double forecastValuePeriodsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (SwapPaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentPeriodPricer.forecastValue(period, provider);
      }
    }
    return total;
  }

  // calculates the present value of the events composing the leg in the currency of the swap leg
  protected double presentValueEventsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (SwapPaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentEventPricer.presentValue(event, provider);
      }
    }
    return total;
  }

  // calculates the present value of the periods composing the leg in the currency of the swap leg
  protected double presentValuePeriodsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (SwapPaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentPeriodPricer.presentValue(period, provider);
      }
    }
    return total;
  }

  // calculates the present value curve sensitivity of the events composing the leg in the currency of the swap leg
  PointSensitivityBuilder presentValueSensitivityEventsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (SwapPaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        builder = builder.combinedWith(paymentEventPricer.presentValueSensitivity(event, provider));
      }
    }
    return builder;
  }

  // calculates the present value curve sensitivity of the periods composing the leg in the currency of the swap leg
  PointSensitivityBuilder presentValueSensitivityPeriodsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (SwapPaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        builder = builder.combinedWith(paymentPeriodPricer.presentValueSensitivity(period, provider));
      }
    }
    return builder;
  }

  //-------------------------------------------------------------------------
  // calculates the cash flow of the periods composing the leg in the currency of the swap leg
  CashFlows cashFlowPeriodsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    ImmutableList.Builder<CashFlow> builder = ImmutableList.builder();
    for (SwapPaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        double forecastValue = paymentPeriodPricer.forecastValue(period, provider);
        if (forecastValue != 0d) {
          Currency currency = period.getCurrency();
          LocalDate paymentDate = period.getPaymentDate();
          double discountFactor = provider.discountFactor(currency, paymentDate);
          CashFlow singleCashFlow = CashFlow.ofForecastValue(paymentDate, currency, forecastValue, discountFactor);
          builder.add(singleCashFlow);
        }
      }
    }
    return CashFlows.of(builder.build());
  }

  // calculates the cash flow of the events composing the leg in the currency of the swap leg
  CashFlows cashFlowEventsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    ImmutableList.Builder<CashFlow> builder = ImmutableList.builder();
    for (SwapPaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        double forecastValue = paymentEventPricer.forecastValue(event, provider);
        if (forecastValue != 0d) {
          Currency currency = event.getCurrency();
          LocalDate paymentDate = event.getPaymentDate();
          double discountFactor = provider.discountFactor(currency, paymentDate);
          CashFlow singleCashFlow = CashFlow.ofForecastValue(paymentDate, currency, forecastValue, discountFactor);
          builder.add(singleCashFlow);
        }
      }
    }
    return CashFlows.of(builder.build());
  }

  //-------------------------------------------------------------------------
  /**
   * Explain present value builder used to build large explain map from the individual legs.
   * 
   * @param leg  the swap log 
   * @param provider  the rates provider
   * @param builder  the explain map builder which will be populated but the leg 
   */
  void explainPresentValueInternal(ResolvedSwapLeg leg, RatesProvider provider, ExplainMapBuilder builder) {
    builder.put(ExplainKey.ENTRY_TYPE, "Leg");
    builder.put(ExplainKey.PAY_RECEIVE, leg.getPayReceive());
    builder.put(ExplainKey.LEG_TYPE, leg.getType().toString());
    for (SwapPaymentPeriod period : leg.getPaymentPeriods()) {
      builder.addListEntry(
          ExplainKey.PAYMENT_PERIODS, child -> paymentPeriodPricer.explainPresentValue(period, provider, child));
    }
    for (SwapPaymentEvent event : leg.getPaymentEvents()) {
      builder.addListEntry(
          ExplainKey.PAYMENT_EVENTS, child -> paymentEventPricer.explainPresentValue(event, provider, child));
    }
    builder.put(ExplainKey.FORECAST_VALUE, forecastValue(leg, provider));
    builder.put(ExplainKey.PRESENT_VALUE, presentValue(leg, provider));
  }

  /**
   * Explain present value for a swap leg.
   * 
   * @param leg  the swap log 
   * @param provider  the rates provider
   * @return the explain PV map
   */
  public ExplainMap explainPresentValue(ResolvedSwapLeg leg, RatesProvider provider) {
    ExplainMapBuilder builder = ExplainMap.builder();
    explainPresentValueInternal(leg, provider, builder);
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the swap leg.
   * 
   * @param leg  the leg
   * @param provider  the rates provider
   * @return the currency exposure of the swap leg
   */
  public MultiCurrencyAmount currencyExposure(ResolvedSwapLeg leg, RatesProvider provider) {
    return currencyExposurePeriodsInternal(leg, provider).plus(currencyExposureEventsInternal(leg, provider));
  }

  private MultiCurrencyAmount currencyExposurePeriodsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    MultiCurrencyAmount total = MultiCurrencyAmount.empty();
    for (SwapPaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        total = total.plus(paymentPeriodPricer.currencyExposure(period, provider));
      }
    }
    return total;
  }

  private MultiCurrencyAmount currencyExposureEventsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    MultiCurrencyAmount total = MultiCurrencyAmount.empty();
    for (SwapPaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        total = total.plus(paymentEventPricer.currencyExposure(event, provider));
      }
    }
    return total;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the current cash of the swap leg.
   * 
   * @param leg  the leg
   * @param provider  the rates provider
   * @return the current cash of the swap leg
   */
  public CurrencyAmount currentCash(ResolvedSwapLeg leg, RatesProvider provider) {
    return CurrencyAmount.of(leg.getCurrency(),
        currentCashPeriodsInternal(leg, provider) + (currentCashEventsInternal(leg, provider)));
  }

  private double currentCashPeriodsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (SwapPaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentPeriodPricer.currentCash(period, provider);
      }
    }
    return total;
  }

  private double currentCashEventsInternal(ResolvedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (SwapPaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentEventPricer.currentCash(event, provider);
      }
    }
    return total;
  }
}
