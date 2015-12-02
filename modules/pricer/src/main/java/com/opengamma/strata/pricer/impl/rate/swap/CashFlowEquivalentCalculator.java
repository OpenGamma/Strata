/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.PaymentPeriodPricer;
import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.rate.IborRateObservation;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.FxReset;
import com.opengamma.strata.product.swap.KnownAmountPaymentPeriod;
import com.opengamma.strata.product.swap.PaymentPeriod;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapProduct;

/**
 * Computes cash flow equivalent of products.
 * <p>
 * Reference: Henrard, M. The Irony in the derivatives discounting Part II: the crisis. Wilmott Journal, 2010, 2, 301-316. 
 */
public final class CashFlowEquivalentCalculator {

  /**
   * Computes cash flow equivalent of swap product. 
   * <p>
   * The swap should be a fix-for-Ibor swap without compounding, and its swap legs should not involve {@code PaymentEvent}.
   * <p>
   * The return type is {@code ExpandedSwapLeg} where individual payments are represented in terms of {@code RatePaymentPeriod} 
   * or {@code KnownAmountPaymentPeriod}. The present value of these payment should be computed by the method
   * {@linkplain  PaymentPeriodPricer#presentValueCashFlowEquivalent(PaymentPeriod, RatesProvider) presentValueCashFlowEquivalent}.
   * 
   * @param swap  the swap product
   * @return the cash flow equivalent
   */
  public static ExpandedSwapLeg cashFlowEquivalent(SwapProduct swap) {
    ExpandedSwap expanded = swap.expand();
    ExpandedSwapLeg fixedLeg = expanded.getLegs(SwapLegType.FIXED).get(0);
    ExpandedSwapLeg iborLeg = expanded.getLegs(SwapLegType.IBOR).get(0);
    List<PaymentPeriod> paymentPeriods = new ArrayList<PaymentPeriod>();
    ArgChecker.isTrue(fixedLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    ArgChecker.isTrue(iborLeg.getPaymentEvents().isEmpty(), "PaymentEvent should be empty");
    // ibor leg
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
      FxReset fxReset = ratePaymentPeriod.getFxReset().orElse(null);
      RatePaymentPeriod payStart = RatePaymentPeriod.builder()
          .accrualPeriods(ratePaymentPeriod.getAccrualPeriods())
          .currency(ratePaymentPeriod.getCurrency())
          .fxReset(fxReset)
          .dayCount(ratePaymentPeriod.getDayCount())
          .notional(ratePaymentPeriod.getNotional() * rateAccrualPeriod.getYearFraction() / fixingYearFraction)
          .paymentDate(paymentDate).build();
      KnownAmountPaymentPeriod payEnd = KnownAmountPaymentPeriod.of(
          Payment.of(notional.multipliedBy(-rateAccrualPeriod.getYearFraction() / fixingYearFraction), paymentDate),
          SchedulePeriod.of(ratePaymentPeriod.getStartDate(), ratePaymentPeriod.getEndDate()));
      paymentPeriods.add(payStart);
      paymentPeriods.add(payEnd);
    }
    // fixed leg
    for (PaymentPeriod paymentPeriod : fixedLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "rate payment should be RatePaymentPeriod");
      RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
      ArgChecker.isTrue(ratePaymentPeriod.getAccrualPeriods().size() == 1, "rate payment should not be compounding");
      RateAccrualPeriod rateAccrualPeriod = ratePaymentPeriod.getAccrualPeriods().get(0);
      double factor = rateAccrualPeriod.getYearFraction() *
          ((FixedRateObservation) rateAccrualPeriod.getRateObservation()).getRate();
      CurrencyAmount notional = ratePaymentPeriod.getNotionalAmount().multipliedBy(factor);
      LocalDate paymentDate = ratePaymentPeriod.getPaymentDate();
      KnownAmountPaymentPeriod pay = KnownAmountPaymentPeriod.of(Payment.of(notional, paymentDate),
          SchedulePeriod.of(ratePaymentPeriod.getStartDate(), ratePaymentPeriod.getEndDate()));
      paymentPeriods.add(pay);
    }
    // cash flow equivalent
    ExpandedSwapLeg leg = ExpandedSwapLeg.builder()
        .paymentPeriods(paymentPeriods)
        .payReceive(PayReceive.RECEIVE)
        .type(SwapLegType.OTHER)
        .build();
    return leg;
  }

}
