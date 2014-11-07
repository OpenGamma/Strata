/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricerfn.swap;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.opengamma.analytics.financial.instrument.index.IndexIborMaster;
import com.opengamma.analytics.financial.interestrate.payments.derivative.CouponFixedCompounding;
import com.opengamma.analytics.financial.interestrate.payments.derivative.CouponIborCompoundingFlatSpread;
import com.opengamma.analytics.financial.interestrate.payments.derivative.CouponIborCompoundingSpread;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.date.Tenor;
import com.opengamma.basics.index.RateIndex;
import com.opengamma.platform.finance.rate.IborRate;
import com.opengamma.platform.finance.swap.AccrualPeriod;
import com.opengamma.platform.finance.swap.CompoundingMethod;
import com.opengamma.platform.finance.swap.FixedRateAccrualPeriod;
import com.opengamma.platform.finance.swap.FloatingRateAccrualPeriod;
import com.opengamma.platform.finance.swap.SwapPaymentPeriod;
import com.opengamma.platform.finance.swap.SwapTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.FixedRateAccrualPeriodPricerFn;
import com.opengamma.platform.pricer.swap.FloatingRateAccrualPeriodPricerFn;
import com.opengamma.platform.pricer.swap.SwapPricerFn;

/**
 * Pricer for swaps.
 */
public class StandardSwapPricerFn implements SwapPricerFn {

  /**
   * Present value calculator.
   */
  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();

  /**
   * Fixed accrual pricer.
   */
  private final FixedRateAccrualPeriodPricerFn fixedPricerFn;
  /**
   * Floating accrual pricer.
   */
  private final FloatingRateAccrualPeriodPricerFn floatingPricerFn;

  /**
   * Creates an instance.
   * 
   * @param fixedPricerFn  the pricer for {@link FixedRateAccrualPeriod}
   * @param floatingPricerFn  the pricer for {@link FloatingRateAccrualPeriod}
   */
  public StandardSwapPricerFn(
      FixedRateAccrualPeriodPricerFn fixedPricerFn,
      FloatingRateAccrualPeriodPricerFn floatingPricerFn) {
    this.fixedPricerFn = fixedPricerFn;
    this.floatingPricerFn = floatingPricerFn;
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, LocalDate valuationDate, SwapTrade trade) {
    if (trade.getSwap().isCrossCurrency()) {
      throw new UnsupportedOperationException();
    }
    Currency currency = trade.getSwap().getLegs().get(0).getCurrency();
    double pv = trade.getSwap().getLegs().stream()
      .flatMap(leg -> leg.toExpanded().getPaymentPeriods().stream())
      .mapToDouble(p -> presentValuePaymentPeriod(env, valuationDate, p))
      .sum();
    return CurrencyAmount.of(currency, pv);
  }

  // present value
  double presentValuePaymentPeriod(PricingEnvironment env, LocalDate valuationDate, SwapPaymentPeriod paymentPeriod) {
    // historic payments have zero pv
    if (paymentPeriod.getPaymentDate().isBefore(valuationDate)) {
      return 0;
    }
    // compounding
    if (paymentPeriod.isCompounding()) {
      double total = 0;
      // fixed
      List<FixedRateAccrualPeriod> periods1 = paymentPeriod.getAccrualPeriods().stream()
          .filter(p -> p instanceof FixedRateAccrualPeriod)
          .map(p-> (FixedRateAccrualPeriod) p)
          .collect(Collectors.toList());
      if (periods1.size() > 0) {
        double paymentYearFraction1 = periods1.stream()
            .mapToDouble(FixedRateAccrualPeriod::getYearFraction)
            .sum();
        double[] accrualPeriodYearFractions1 = periods1.stream()
            .mapToDouble(FixedRateAccrualPeriod::getYearFraction)
            .toArray();
        CouponFixedCompounding coupon1 = new CouponFixedCompounding(
            currency(paymentPeriod.getCurrency()),
            env.relativeTime(valuationDate, paymentPeriod.getPaymentDate()),
            paymentYearFraction1,
            periods1.get(0).getNotional(),  // TODO only one notional
            accrualPeriodYearFractions1,
            periods1.get(0).getRate());  // TODO only one rate
        total += PVDC.visitCouponFixedCompounding(coupon1, env.getMulticurve())
            .getAmount(currency(paymentPeriod.getCurrency()));
      }
      // floating
      List<FloatingRateAccrualPeriod> periods2 = paymentPeriod.getAccrualPeriods().stream()
          .filter(p -> p instanceof FloatingRateAccrualPeriod)
          .map(p-> (FloatingRateAccrualPeriod) p)
          .collect(Collectors.toList());
      if (periods2.size() > 0) {
        double paymentYearFraction2 = periods2.stream()
            .mapToDouble(FloatingRateAccrualPeriod::getYearFraction)
            .sum();
        double[] accrualPeriodYearFractions2 = periods2.stream()
            .mapToDouble(FloatingRateAccrualPeriod::getYearFraction)
            .toArray();
        double[] fixingRelativeTimes = new double[periods2.size()];
        double[] fixingStartTimes = new double[periods2.size()];
        double[] fixingEndTimes = new double[periods2.size()];
        double[] fixingYearFractions = new double[periods2.size()];
        for (int i = 0; i < periods2.size(); i++) {
          IborRate rate = (IborRate) periods2.get(i).getRate();  // TODO assumes IBOR
          LocalDate fixingDate = rate.getFixingDate();
          LocalDate fixingStartDate = rate.getIndex().calculateEffectiveFromFixing(fixingDate);
          LocalDate fixingEndDate = rate.getIndex().calculateMaturityFromEffective(fixingStartDate);
          fixingRelativeTimes[i] = env.relativeTime(valuationDate, fixingDate);
          fixingStartTimes[i] = env.relativeTime(valuationDate, fixingStartDate);
          fixingEndTimes[i] = env.relativeTime(valuationDate, fixingEndDate);
          fixingYearFractions[i] = rate.getIndex().getDayCount().getDayCountFraction(fixingStartDate, fixingEndDate);
        }
        IborRate rate = (IborRate) periods2.get(0).getRate();  // TODO only one index here
        if (paymentPeriod.getCompoundingMethod() == CompoundingMethod.FLAT) {
          CouponIborCompoundingFlatSpread coupon2 = new CouponIborCompoundingFlatSpread(
              currency(paymentPeriod.getCurrency()),
              env.relativeTime(valuationDate, paymentPeriod.getPaymentDate()),
              paymentYearFraction2,
              periods2.get(0).getNotional(),
              0,  // TODO historic accrual
              index(rate.getIndex()),
              accrualPeriodYearFractions2,
              fixingRelativeTimes,
              fixingStartTimes,
              fixingEndTimes,
              fixingYearFractions,
              periods2.get(0).getSpread());  // TODO only one spread here
          total += PVDC.visitCouponIborCompoundingFlatSpread(coupon2, env.getMulticurve())
          .getAmount(currency(paymentPeriod.getCurrency()));
        } else {
          CouponIborCompoundingSpread coupon2 = new CouponIborCompoundingSpread(
              currency(paymentPeriod.getCurrency()),
              env.relativeTime(valuationDate, paymentPeriod.getPaymentDate()),
              paymentYearFraction2,
              periods2.get(0).getNotional(),
              periods2.get(0).getNotional(),  // TODO historic accrual
              index(rate.getIndex()),
              accrualPeriodYearFractions2,
              fixingRelativeTimes,
              fixingStartTimes,
              fixingEndTimes,
              fixingYearFractions,
              periods2.get(0).getSpread());  // TODO only one spread here
          total += PVDC.visitCouponIborCompoundingSpread(coupon2, env.getMulticurve())
              .getAmount(currency(paymentPeriod.getCurrency()));
        }
      }
      return total;
    }
    // no compounding
    return paymentPeriod.getAccrualPeriods().stream()
        .mapToDouble(p -> presentValueAccrualPeriod(env, valuationDate, paymentPeriod, p))
        .sum();
  }

  // present value
  private double presentValueAccrualPeriod(
      PricingEnvironment env,
      LocalDate valuationDate,
      SwapPaymentPeriod paymentPeriod,
      AccrualPeriod accrualPeriod) {    
    // fixed
    if (accrualPeriod instanceof FixedRateAccrualPeriod) {
      FixedRateAccrualPeriod fixedPeriod = (FixedRateAccrualPeriod) accrualPeriod;
      return fixedPricerFn.presentValue(env, valuationDate, fixedPeriod, paymentPeriod.getPaymentDate());
    } else if (accrualPeriod instanceof FloatingRateAccrualPeriod) {
      FloatingRateAccrualPeriod floatingPeriod = (FloatingRateAccrualPeriod) accrualPeriod;
      return floatingPricerFn.presentValue(env, valuationDate, floatingPeriod, paymentPeriod.getPaymentDate());
    } else {
      throw new IllegalArgumentException("Unknown AccrualPeriod type");
    }
  }

  //-------------------------------------------------------------------------
  private static com.opengamma.util.money.Currency currency(Currency currency) {
    return com.opengamma.util.money.Currency.of(currency.getCode());
  }

  private static com.opengamma.analytics.financial.instrument.index.IborIndex index(RateIndex index) {
    com.opengamma.analytics.financial.instrument.index.IborIndex idx =
        IndexIborMaster.getInstance().getIndex(IndexIborMaster.USDLIBOR3M);
    if (index.getTenor().equals(Tenor.TENOR_6M)) {
      idx = IndexIborMaster.getInstance().getIndex(IndexIborMaster.USDLIBOR6M);
    } else if (index.getTenor().equals(Tenor.TENOR_1M)) {
      idx = IndexIborMaster.getInstance().getIndex(IndexIborMaster.USDLIBOR1M);
    }
    return idx;
  }

}
