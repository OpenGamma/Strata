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
import com.opengamma.basics.date.Tenor;
import com.opengamma.basics.index.RateIndex;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.rate.FixedRate;
import com.opengamma.platform.finance.rate.IborRate;
import com.opengamma.platform.finance.swap.AccrualPeriod;
import com.opengamma.platform.finance.swap.CompoundingMethod;
import com.opengamma.platform.finance.swap.RateAccrualPeriod;
import com.opengamma.platform.finance.swap.RatePaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.AccrualPeriodPricerFn;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;
import com.opengamma.platform.pricerfn.rate.StandardRateProviderFn;

/**
 * Pricer implementation for swap payment periods based on a rate.
 * <p>
 * The value of a payment period is calculated by combining the value of each accrual period.
 * Where necessary, the accrual periods are compounded.
 */
public class StandardRatePaymentPeriodPricerFn
    implements PaymentPeriodPricerFn<RatePaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final StandardRatePaymentPeriodPricerFn DEFAULT = new StandardRatePaymentPeriodPricerFn(
      StandardRateAccrualPeriodPricerFn.DEFAULT);

  /**
   * Accrual period pricer.
   */
  private final AccrualPeriodPricerFn<RateAccrualPeriod> accrualPeriodPricerFn;

  /**
   * Present value calculator.
   */
  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();

  /**
   * Creates an instance.
   * 
   * @param accrualPeriodPricerFn  the pricer for {@link AccrualPeriod}
   */
  public StandardRatePaymentPeriodPricerFn(
      AccrualPeriodPricerFn<RateAccrualPeriod> accrualPeriodPricerFn) {
    this.accrualPeriodPricerFn = ArgChecker.notNull(accrualPeriodPricerFn, "accrualPeriodPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      RatePaymentPeriod period) {
    // historic payments have zero pv
    if (period.getPaymentDate().isBefore(valuationDate)) {
      return 0;
    }
    // handle compounding
    if (period.isCompounding()) {
      return presentValueCompounded(env, valuationDate, period);
    } else {
      return presentValueNoCompounding(env, valuationDate, period);
    }
  }

  // no compounding needed
  private double presentValueNoCompounding(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    return period.getAccrualPeriods().stream()
        .mapToDouble(accrualPeriod -> accrualPeriodPricerFn.presentValue(
            env, valuationDate, accrualPeriod, period.getPaymentDate()))
        .sum();
  }

  // apply compounding
  private double presentValueCompounded(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    
    double notional = period.getAccrualPeriod(0).getNotional();
    double notionalAccrued = notional;
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
//      double rate = StandardRateProviderFn.DEFAULT.rate(
//          env, valuationDate, accrualPeriod.getRate(), period.getStartDate(), period.getEndDate());
//      double treatedRate = rate * accrualPeriod.getGearing() + accrualPeriod.getSpread();
//      double investFactor = 1 + (treatedRate * accrualPeriod.getYearFraction());
      double unitAccrual = accrualPeriodPricerFn.futureValue(env, valuationDate, accrualPeriod) / accrualPeriod.getNotional();  // TODO FX reset
      double investFactor = 1 + unitAccrual;
      notionalAccrued *= investFactor;
      System.out.println(notionalAccrued);
    }
    double accrued = (notionalAccrued - notional);
    double df = env.discountFactor(period.getCurrency(), valuationDate, period.getPaymentDate());
    System.out.println(accrued * df);
    return accrued * df;
    
    

//    int size = period.getAccrualPeriods().size();
//    double[] futureValues = new double[size];
//    double[] unitAccrual = new double[size];
//    for (int i = 0; i < size; i++) {
//      RateAccrualPeriod accrualPeriod = period.getAccrualPeriod(i);
//      futureValues[i] = accrualPeriodPricerFn.futureValue(env, valuationDate, accrualPeriod);
//      unitAccrual[i] = futureValues[i] / accrualPeriod.getNotional();  // TODO FX reset
//    }
//    double[] compound = new double[size];
//    for (int i = 0; i < size - 1; i++) {
//      for (int j = i + 1; j < size; j++) {
//        compound[j] += futureValues[i] * unitAccrual[j];
//      }
//    }
//    double fv = 0d;
//    for (int i = 0; i < futureValues.length; i++) {
//      fv += futureValues[i] + compound[i];
//    }
//    
////    double fv = DoubleStream.of(Doubles.concat(futureValues, compound)).sum();
//    // futureValue * discountFactor
//    double df = env.discountFactor(period.getCurrency(), valuationDate, period.getPaymentDate());
//    return df * fv;


    
    
//    
//    double total = 0;
//    // fixed
//    List<RateAccrualPeriod> periods1 = period.getAccrualPeriods().stream()
//        .filter(p -> p instanceof RateAccrualPeriod)
//        .map(p-> (RateAccrualPeriod) p)
//        .filter(p -> p.getRate() instanceof FixedRate)
//        .collect(Collectors.toList());
//    if (periods1.size() > 0) {
//      double paymentYearFraction1 = periods1.stream()
//          .mapToDouble(RateAccrualPeriod::getYearFraction)
//          .sum();
//      double[] accrualPeriodYearFractions1 = periods1.stream()
//          .mapToDouble(RateAccrualPeriod::getYearFraction)
//          .toArray();
//      CouponFixedCompounding coupon1 = new CouponFixedCompounding(
//          currency(period.getCurrency()),
//          env.relativeTime(valuationDate, period.getPaymentDate()),
//          paymentYearFraction1,
//          periods1.get(0).getNotional(),  // TODO only one notional
//          accrualPeriodYearFractions1,
//          ((FixedRate) periods1.get(0).getRate()).getRate());  // TODO only one rate
//      total += PVDC.visitCouponFixedCompounding(coupon1, env.getMulticurve())
//          .getAmount(currency(period.getCurrency()));
//    }
//    // floating
//    List<RateAccrualPeriod> periods2 = period.getAccrualPeriods().stream()
//        .filter(p -> p instanceof RateAccrualPeriod)
//        .map(p-> (RateAccrualPeriod) p)
//        .filter(p -> p.getRate() instanceof IborRate)
//        .collect(Collectors.toList());
//    if (periods2.size() > 0) {
//      double paymentYearFraction2 = periods2.stream()
//          .mapToDouble(RateAccrualPeriod::getYearFraction)
//          .sum();
//      double[] accrualPeriodYearFractions2 = periods2.stream()
//          .mapToDouble(RateAccrualPeriod::getYearFraction)
//          .toArray();
//      double[] fixingRelativeTimes = new double[periods2.size()];
//      double[] fixingStartTimes = new double[periods2.size()];
//      double[] fixingEndTimes = new double[periods2.size()];
//      double[] fixingYearFractions = new double[periods2.size()];
//      for (int i = 0; i < periods2.size(); i++) {
//        IborRate rate = (IborRate) periods2.get(i).getRate();
//        LocalDate fixingDate = rate.getFixingDate();
//        LocalDate fixingStartDate = rate.getIndex().calculateEffectiveFromFixing(fixingDate);
//        LocalDate fixingEndDate = rate.getIndex().calculateMaturityFromEffective(fixingStartDate);
//        fixingRelativeTimes[i] = env.relativeTime(valuationDate, fixingDate);
//        fixingStartTimes[i] = env.relativeTime(valuationDate, fixingStartDate);
//        fixingEndTimes[i] = env.relativeTime(valuationDate, fixingEndDate);
//        fixingYearFractions[i] = rate.getIndex().getDayCount().yearFraction(fixingStartDate, fixingEndDate);
//      }
//      IborRate rate = (IborRate) periods2.get(0).getRate();  // TODO only one index here
//      if (period.getCompoundingMethod() == CompoundingMethod.FLAT) {
//        CouponIborCompoundingFlatSpread coupon2 = new CouponIborCompoundingFlatSpread(
//            currency(period.getCurrency()),
//            env.relativeTime(valuationDate, period.getPaymentDate()),
//            paymentYearFraction2,
//            periods2.get(0).getNotional(),
//            0,  // TODO historic accrual
//            index(rate.getIndex()),
//            accrualPeriodYearFractions2,
//            fixingRelativeTimes,
//            fixingStartTimes,
//            fixingEndTimes,
//            fixingYearFractions,
//            periods2.get(0).getSpread());  // TODO only one spread here
//        total += PVDC.visitCouponIborCompoundingFlatSpread(coupon2, env.getMulticurve())
//        .getAmount(currency(period.getCurrency()));
//      } else {
//        CouponIborCompoundingSpread coupon2 = new CouponIborCompoundingSpread(
//            currency(period.getCurrency()),
//            env.relativeTime(valuationDate, period.getPaymentDate()),
//            paymentYearFraction2,
//            periods2.get(0).getNotional(),
//            periods2.get(0).getNotional(),  // TODO historic accrual
//            index(rate.getIndex()),
//            accrualPeriodYearFractions2,
//            fixingRelativeTimes,
//            fixingStartTimes,
//            fixingEndTimes,
//            fixingYearFractions,
//            periods2.get(0).getSpread());  // TODO only one spread here
//        total += PVDC.visitCouponIborCompoundingSpread(coupon2, env.getMulticurve())
//            .getAmount(currency(period.getCurrency()));
//      }
//    }
//    return total;
  }

  //-------------------------------------------------------------------------
  @Override
  public double futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      RatePaymentPeriod period) {
    return 0d;
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
