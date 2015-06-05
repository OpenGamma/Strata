/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.fx.FxPayment;
import com.opengamma.strata.finance.rate.FixedRateObservation;
import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.finance.rate.OvernightCompoundedRateObservation;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RateAccrualPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.fx.DiscountingFxProductPricerBeta;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculator to compute the cash flow equivalent of some interest rate derivative. The "equivalence" is based on
 * a single curve simplifying assumption.
 * <p>
 * The calculator also provides methods to compute present value and present value sensitivity of the objects 
 * describing the cash-flows.
 */
public class CashflowEquivalentTheoreticalCalculator {

  /**
   * Default implementation.
   */
  public static final CashflowEquivalentTheoreticalCalculator DEFAULT = new CashflowEquivalentTheoreticalCalculator();
  
  /** The pricer to compute the present value and present value sensitivity of the cash flows. */
  DiscountingFxProductPricerBeta PRICER_CASH_FLOW = DiscountingFxProductPricerBeta.DEFAULT;
  
  /**
   * Computes the cash flow equivalent of a swap.
   * @param swap  the swap
   * @param provider the provider. It is used only for the valuation date and the fixing time series, not for the curves.
   * @return the cash flow equivalent as a list of payments
   */
  public List<FxPayment> cashFlowEquivalent(Swap swap, RatesProvider provider) {
    ExpandedSwap expanded = swap.expand();
    List<FxPayment> list = new ArrayList<>();
    for(ExpandedSwapLeg leg: expanded.getLegs()) {
      list.addAll(cashFlowEquivalent(leg, provider));
    }
    return list;
  }
  
  /**
   * Computes the cash flow equivalent of a swap.
   * @param leg  the leg
   * @param provider the provider. It is used only for the valuation date and the fixing time series, not for the curves.
   * @return the cash flow equivalent as a list of payments
   */
  public List<FxPayment> cashFlowEquivalent(ExpandedSwapLeg leg, RatesProvider provider) {
    List<FxPayment> list = new ArrayList<>();
    for(PaymentPeriod period: leg.getPaymentPeriods()) {
      list.addAll(cashFlowEquivalent(period, provider));
    }
    return list;
  }
  
  /**
   * Computes the cash flow equivalent of a payment period.
   * RateObservation covered are FixedRateObservation and IborRateObservation.
   * @param paymentPeriod  the payment period
   * @param provider The rate provider. Required for valuation date and fixing time series.
   * @return the cash flow equivalent as a list of payments
   */
  public List<FxPayment> cashFlowEquivalent(PaymentPeriod paymentPeriod, RatesProvider provider) {
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "payment period should be of type RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    ArgChecker.isTrue(!ratePaymentPeriod.getFxReset().isPresent(), "FX reset is not supported");
    ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "Compounding is not supported");
    RateAccrualPeriod accrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
    ArgChecker.isTrue(
        (accrualPeriod.getRateObservation() instanceof FixedRateObservation) ||
            (accrualPeriod.getRateObservation() instanceof IborRateObservation) ||
                (accrualPeriod.getRateObservation() instanceof OvernightCompoundedRateObservation),
        "RateObservation must be instance of FixedRateObservation or IborRateObservation");
    LocalDate valuationDate = provider.getValuationDate();
    List<FxPayment> list = new ArrayList<>();
    if(paymentPeriod.getPaymentDate().isBefore(valuationDate)) { // CF before valuation date not taken into account
      return list;
    }
    if (accrualPeriod.getRateObservation() instanceof FixedRateObservation) {
      FixedRateObservation fixed = (FixedRateObservation) accrualPeriod.getRateObservation();
      FxPayment payment = FxPayment.of(paymentPeriod.getPaymentDate(),
          CurrencyAmount.of(paymentPeriod.getCurrency(),
              (accrualPeriod.getGearing() * fixed.getRate() + accrualPeriod.getSpread()) 
              * accrualPeriod.getYearFraction() * ratePaymentPeriod.getNotional()));
      list.add(payment);
      return list;
    }
    if (accrualPeriod.getRateObservation() instanceof IborRateObservation) {
      IborRateObservation ibor = (IborRateObservation) accrualPeriod.getRateObservation();
      if (ibor.getFixingDate().isBefore(valuationDate)) { // fixing already took place
        double fixing = provider.iborIndexRates(ibor.getIndex()).rate(ibor.getFixingDate());
        FxPayment payment = FxPayment.of(paymentPeriod.getPaymentDate(),
            CurrencyAmount.of(paymentPeriod.getCurrency(),
                (accrualPeriod.getGearing() * fixing + accrualPeriod.getSpread())
                    * accrualPeriod.getYearFraction() * ratePaymentPeriod.getNotional()));
        list.add(payment);
        return list;
      }
      // fixing in the future: start and end date notional equivalent
      FxPayment paymentStart = FxPayment.of(accrualPeriod.getStartDate(),
          CurrencyAmount.of(paymentPeriod.getCurrency(), accrualPeriod.getGearing() * ratePaymentPeriod.getNotional()));
      FxPayment paymentEnd = FxPayment.of(accrualPeriod.getEndDate(),
          CurrencyAmount.of(paymentPeriod.getCurrency(), -accrualPeriod.getGearing() * ratePaymentPeriod.getNotional()
              +  accrualPeriod.getSpread() * accrualPeriod.getYearFraction() * ratePaymentPeriod.getNotional()));
      list.add(paymentStart);
      list.add(paymentEnd);
      return list;
    }
    OvernightCompoundedRateObservation on = (OvernightCompoundedRateObservation) accrualPeriod.getRateObservation();
    if (!on.getStartDate().isBefore(valuationDate)) { // First fixing not taken place yet
      // TODO: spread, gearing
      FxPayment paymentStart = FxPayment.of(accrualPeriod.getStartDate(),
          CurrencyAmount.of(paymentPeriod.getCurrency(), ratePaymentPeriod.getNotional()));
      FxPayment paymentEnd = FxPayment.of(accrualPeriod.getEndDate(),
          CurrencyAmount.of(paymentPeriod.getCurrency(), -ratePaymentPeriod.getNotional()));
      list.add(paymentStart);
      list.add(paymentEnd);
      return list;
    }
    OvernightIndex index = on.getIndex();
    double compositionFactor = 1.0d;
    LocalDate currentFixing = on.getStartDate();
    while (currentFixing.isBefore(on.getEndDate()) && // fixing in the non-cutoff period
        provider.getValuationDate().isAfter(currentFixing)) { // publication before valuation
      double rate = provider.overnightIndexRates(index).rate(currentFixing);
      LocalDate effectiveDate = index.calculateEffectiveFromFixing(currentFixing);
      LocalDate maturityDate = index.calculateMaturityFromEffective(effectiveDate);
      double accrualFactor = index.getDayCount().yearFraction(effectiveDate, maturityDate);
      compositionFactor *= 1.0d + accrualFactor * rate;
      currentFixing = index.getFixingCalendar().next(currentFixing);
    }
    FxPayment paymentEnd = FxPayment.of(accrualPeriod.getEndDate(),
        CurrencyAmount.of(paymentPeriod.getCurrency(), -ratePaymentPeriod.getNotional() * compositionFactor));
    list.add(paymentEnd);
    return list;
  }
  
  /**
   * Computes the present value of a list of cash flows.
   * @param payments  the list of payments
   * @param provider  the provider
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(List<FxPayment> payments, RatesProvider provider){
    MultiCurrencyAmount pv = MultiCurrencyAmount.empty();
    for(FxPayment payment: payments) {
      pv = pv.plus(PRICER_CASH_FLOW.presentValue(payment, provider));
    }
    return pv;
  }
  
  /**
   * Computes the present value sensitivity of a list of cash flows.
   * @param payments  the list of payments
   * @param provider  the provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivity(List<FxPayment> payments, RatesProvider provider){
    PointSensitivityBuilder pv = PointSensitivityBuilder.none();
    for(FxPayment payment: payments) {
      pv = pv.combinedWith(PRICER_CASH_FLOW.presentValueSensitivity(payment, provider));
    }
    return pv;
  }

}
