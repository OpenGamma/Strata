/**
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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.KnownAmountPaymentPeriod;
import com.opengamma.strata.product.swap.PaymentEvent;
import com.opengamma.strata.product.swap.PaymentPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Pricer for for rate swap legs.
 * <p>
 * This function provides the ability to price a {@link SwapLeg}.
 * The product is priced by pricing each period and event.
 */
public class DiscountingSwapLegPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingSwapLegPricer DEFAULT = new DiscountingSwapLegPricer(
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
  public DiscountingSwapLegPricer(
      PaymentPeriodPricer<PaymentPeriod> paymentPeriodPricer,
      PaymentEventPricer<PaymentEvent> paymentEventPricer) {
    this.paymentPeriodPricer = ArgChecker.notNull(paymentPeriodPricer, "paymentPeriodPricer");
    this.paymentEventPricer = ArgChecker.notNull(paymentEventPricer, "paymentEventPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swap leg, converted to the specified currency.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * This is the discounted forecast value.
   * The result is converted to the specified currency.
   * 
   * @param leg  the leg to price
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value of the swap leg in the specified currency
   */
  public CurrencyAmount presentValue(SwapLeg leg, Currency currency, RatesProvider provider) {
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
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the present value of the swap leg
   */
  public CurrencyAmount presentValue(SwapLeg leg, RatesProvider provider) {
    return CurrencyAmount.of(leg.getCurrency(), presentValueInternal(leg, provider));
  }

  // calculates the present value in the currency of the swap leg
  double presentValueInternal(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    return presentValuePeriodsInternal(expanded, provider) + presentValueEventsInternal(expanded, provider);
  }

  /**
   * Calculates the forecast value of the swap leg.
   * <p>
   * The forecast value of the leg is the value on the valuation date without present value discounting.
   * The result is returned using the payment currency of the leg.
   * 
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the forecast value of the swap leg
   */
  public CurrencyAmount forecastValue(SwapLeg leg, RatesProvider provider) {
    return CurrencyAmount.of(leg.getCurrency(), forecastValueInternal(leg, provider));
  }

  // calculates the present value in the currency of the swap leg
  double forecastValueInternal(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    return forecastValuePeriodsInternal(expanded, provider) + forecastValueEventsInternal(expanded, provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the accrued interest since the last payment.
   * <p>
   * This determines the payment period applicable at the valuation date and calculates
   * the accrued interest since the last payment.
   * The result is returned using the payment currency of the leg.
   * 
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the accrued interest of the swap leg
   */
  public CurrencyAmount accruedInterest(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    Optional<PaymentPeriod> period = expanded.findPaymentPeriod(provider.getValuationDate());
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
   * They must not be of type {@link KnownAmountPaymentPeriod}.
   * 
   * @param leg  the swap leg
   * @param provider  the rates provider
   * @return the Present Value of a Basis Point
   */
  public double pvbp(SwapLeg leg, RatesProvider provider) {
    double pvbpLeg = 0d;
    for (PaymentPeriod period : leg.expand().getPaymentPeriods()) {
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
  public double couponEquivalent(SwapLeg leg, RatesProvider provider, double pvbp) {
    ExpandedSwapLeg legExpanded = leg.expand();
    return presentValuePeriodsInternal(legExpanded, provider) / pvbp;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swap leg.
   * <p>
   * The present value sensitivity of the leg is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap leg
   */
  public PointSensitivityBuilder presentValueSensitivity(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    return legValueSensitivity(
        expanded,
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
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the forecast value curve sensitivity of the swap leg
   */
  public PointSensitivityBuilder forecastValueSensitivity(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    return legValueSensitivity(
        expanded,
        provider,
        paymentPeriodPricer::forecastValueSensitivity,
        paymentEventPricer::forecastValueSensitivity);
  }

  // calculate present or forecast value sensitivity for a leg
  private PointSensitivityBuilder legValueSensitivity(
      ExpandedSwapLeg leg,
      RatesProvider provider,
      BiFunction<PaymentPeriod, RatesProvider, PointSensitivityBuilder> periodFn,
      BiFunction<PaymentEvent, RatesProvider, PointSensitivityBuilder> eventFn) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        builder = builder.combinedWith(periodFn.apply(period, provider));
      }
    }
    for (PaymentEvent event : leg.getPaymentEvents()) {
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
   * They must not be of type {@link KnownAmountPaymentPeriod}.
   * 
   * @param fixedLeg  the swap fixed leg
   * @param provider  the rates provider
   * @return the Present Value of a Basis Point sensitivity to the curves
   */
  public PointSensitivityBuilder pvbpSensitivity(SwapLeg fixedLeg, RatesProvider provider) {
    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (PaymentPeriod period : fixedLeg.expand().getPaymentPeriods()) {
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
   * @param fixedLeg  the fixed leg of the swap.
   * @param yield  the yield.
   * @return the cash annuity.
   */
  public double annuityCash(SwapLeg fixedLeg, double yield) {
    ExpandedSwapLeg expanded = fixedLeg.expand();
    int nbFixedPeriod = expanded.getPaymentPeriods().size();
    PaymentPeriod paymentPeriod = expanded.getPaymentPeriods().get(0);
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "payment period should be RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    int nbFixedPaymentYear = (int) Math.round(1d /
        ratePaymentPeriod.getDayCount().yearFraction(ratePaymentPeriod.getStartDate(), ratePaymentPeriod.getEndDate()));
    double notional = Math.abs(ratePaymentPeriod.getNotional());
    double annuityCash = notional * (1d - Math.pow(1d + yield / nbFixedPaymentYear, -nbFixedPeriod)) / yield;
    return annuityCash;
  }

  /**
   * Computes the derivative of the conventional cash annuity with respect to the yield from a swap leg. 
   * <p>
   * The computation is relevant only for standard swaps with constant notional and regular payments.
   * The swap leg must be a fixed leg. However, this is not checked internally. 
   * 
   * @param fixedLeg  the fixed leg of the swap.
   * @param yield  the yield.
   * @return the cash annuity.
   */
  public double annuityCashDerivative(SwapLeg fixedLeg, double yield) {
    ExpandedSwapLeg expanded = fixedLeg.expand();
    int nbFixedPeriod = expanded.getPaymentPeriods().size();
    PaymentPeriod paymentPeriod = expanded.getPaymentPeriods().get(0);
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "payment period should be RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    int nbFixedPaymentYear = (int) Math.round(1d /
        ratePaymentPeriod.getDayCount().yearFraction(ratePaymentPeriod.getStartDate(), ratePaymentPeriod.getEndDate()));
    double notional = Math.abs(ratePaymentPeriod.getNotional());
    double fwdOverPeriods = yield / nbFixedPaymentYear;
    int nbFixedPeriodPlus = 1 + nbFixedPeriod;
    double annuityCashDerivative = notional * Math.pow(yield, -2)
        * ((1d + nbFixedPeriodPlus * fwdOverPeriods) * Math.pow(1d + fwdOverPeriods, -nbFixedPeriodPlus) - 1d);
    return annuityCashDerivative;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flows of the swap leg.
   * <p>
   * Each expected cash flow is added to the result.
   * This is based on {@link #forecastValue(SwapLeg, RatesProvider)}.
   * 
   * @param leg  the swap leg for which the cash flows should be computed
   * @param provider  the rates provider
   * @return the cash flows
   */
  public CashFlows cashFlows(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    CashFlows cashFlowPeriods = cashFlowPeriodsInternal(expanded, provider);
    CashFlows cashFlowEvents = cashFlowEventsInternal(expanded, provider);
    return cashFlowPeriods.combinedWith(cashFlowEvents);
  }

  //-------------------------------------------------------------------------
  // calculates the forecast value of the events composing the leg in the currency of the swap leg
  double forecastValueEventsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (PaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentEventPricer.forecastValue(event, provider);
      }
    }
    return total;
  }

  // calculates the forecast value of the periods composing the leg in the currency of the swap leg
  double forecastValuePeriodsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentPeriodPricer.forecastValue(period, provider);
      }
    }
    return total;
  }

  // calculates the present value of the events composing the leg in the currency of the swap leg
  double presentValueEventsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (PaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentEventPricer.presentValue(event, provider);
      }
    }
    return total;
  }

  // calculates the present value of the periods composing the leg in the currency of the swap leg
  double presentValuePeriodsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentPeriodPricer.presentValue(period, provider);
      }
    }
    return total;
  }

  // calculates the present value curve sensitivity of the events composing the leg in the currency of the swap leg
  PointSensitivityBuilder presentValueSensitivityEventsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (PaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        builder = builder.combinedWith(paymentEventPricer.presentValueSensitivity(event, provider));
      }
    }
    return builder;
  }

  // calculates the present value curve sensitivity of the periods composing the leg in the currency of the swap leg
  PointSensitivityBuilder presentValueSensitivityPeriodsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        builder = builder.combinedWith(paymentPeriodPricer.presentValueSensitivity(period, provider));
      }
    }
    return builder;
  }

  //-------------------------------------------------------------------------
  // calculates the cash flow of the periods composing the leg in the currency of the swap leg
  CashFlows cashFlowPeriodsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    ImmutableList.Builder<CashFlow> builder = ImmutableList.builder();
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
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
  CashFlows cashFlowEventsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    ImmutableList.Builder<CashFlow> builder = ImmutableList.builder();
    for (PaymentEvent event : leg.getPaymentEvents()) {
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
  // explains the present value of the leg
  void explainPresentValueInternal(ExpandedSwapLeg leg, RatesProvider provider, ExplainMapBuilder builder) {
    builder.put(ExplainKey.ENTRY_TYPE, "Leg");
    builder.put(ExplainKey.PAY_RECEIVE, leg.getPayReceive());
    builder.put(ExplainKey.LEG_TYPE, leg.getType().toString());
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      builder.addListEntry(
          ExplainKey.PAYMENT_PERIODS, child -> paymentPeriodPricer.explainPresentValue(period, provider, child));
    }
    for (PaymentEvent event : leg.getPaymentEvents()) {
      builder.addListEntry(
          ExplainKey.PAYMENT_EVENTS, child -> paymentEventPricer.explainPresentValue(event, provider, child));
    }
    builder.put(ExplainKey.FORECAST_VALUE, forecastValue(leg, provider));
    builder.put(ExplainKey.PRESENT_VALUE, presentValue(leg, provider));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the swap leg.
   * 
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the currency exposure of the swap leg
   */
  public MultiCurrencyAmount currencyExposure(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    return currencyExposurePeriodsInternal(expanded, provider).plus(currencyExposureEventsInternal(expanded, provider));
  }

  private MultiCurrencyAmount currencyExposurePeriodsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    MultiCurrencyAmount total = MultiCurrencyAmount.empty();
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        total = total.plus(paymentPeriodPricer.currencyExposure(period, provider));
      }
    }
    return total;
  }

  private MultiCurrencyAmount currencyExposureEventsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    MultiCurrencyAmount total = MultiCurrencyAmount.empty();
    for (PaymentEvent event : leg.getPaymentEvents()) {
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
   * @param leg  the leg to price
   * @param provider  the rates provider
   * @return the current cash of the swap leg
   */
  public CurrencyAmount currentCash(SwapLeg leg, RatesProvider provider) {
    ExpandedSwapLeg expanded = leg.expand();
    return CurrencyAmount.of(leg.getCurrency(),
        currentCashPeriodsInternal(expanded, provider) + (currentCashEventsInternal(expanded, provider)));
  }

  private double currentCashPeriodsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (PaymentPeriod period : leg.getPaymentPeriods()) {
      if (!period.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentPeriodPricer.currentCash(period, provider);
      }
    }
    return total;
  }

  private double currentCashEventsInternal(ExpandedSwapLeg leg, RatesProvider provider) {
    double total = 0d;
    for (PaymentEvent event : leg.getPaymentEvents()) {
      if (!event.getPaymentDate().isBefore(provider.getValuationDate())) {
        total += paymentEventPricer.currentCash(event, provider);
      }
    }
    return total;
  }
}
