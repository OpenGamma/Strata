/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricerfn.swap;

import java.time.LocalDate;

import com.opengamma.analytics.financial.instrument.index.IndexIborMaster;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyPair;
import com.opengamma.basics.date.Tenor;
import com.opengamma.basics.index.RateIndex;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.rate.Rate;
import com.opengamma.platform.finance.swap.RateAccrualPeriod;
import com.opengamma.platform.finance.swap.RatePaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.RateProviderFn;
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
      StandardRateProviderFn.DEFAULT);

  /**
   * Rate provider.
   */
  private final RateProviderFn<Rate> rateProviderFn;

  /**
   * Creates an instance.
   * 
   * @param rateProviderFn  the rate provider
   */
  public StandardRatePaymentPeriodPricerFn(
      RateProviderFn<Rate> rateProviderFn) {
    this.rateProviderFn = ArgChecker.notNull(rateProviderFn, "rateProviderFn");
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
    // find FX rate, using 1 if no FX reset occurs
    double fxRate = 1d;
    if (period.getFxReset() != null) {
      CurrencyPair pair = CurrencyPair.of(period.getFxReset().getReferenceCurrency(), period.getCurrency());
      fxRate = env.fxRate(period.getFxReset().getIndex(), pair, valuationDate, period.getFxReset().getFixingDate());
    }
    double notional = period.getNotional() * fxRate;
    // handle compounding
    double unitAccrual; 
    if (period.isCompounding()) {
      unitAccrual = unitNotionalCompounded(env, valuationDate, period);
    } else {
      unitAccrual = unitNotionalNoCompounding(env, valuationDate, period);
    }
    double df = env.discountFactor(period.getCurrency(), valuationDate, period.getPaymentDate());
    return notional * unitAccrual * df;
  }

  // no compounding needed
  private double unitNotionalNoCompounding(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    return period.getAccrualPeriods().stream()
        .mapToDouble(accrualPeriod -> unitNotionalAccrual(env, valuationDate, accrualPeriod))
        .sum();
  }

  // apply compounding
  private double unitNotionalCompounded(PricingEnvironment env, LocalDate valuationDate, RatePaymentPeriod period) {
    double notional = 1d;
    double notionalAccrued = notional;
    for (RateAccrualPeriod accrualPeriod : period.getAccrualPeriods()) {
//      double rate = StandardRateProviderFn.DEFAULT.rate(
//          env, valuationDate, accrualPeriod.getRate(), period.getStartDate(), period.getEndDate());
//      double treatedRate = rate * accrualPeriod.getGearing() + accrualPeriod.getSpread();
//      double investFactor = 1 + (treatedRate * accrualPeriod.getYearFraction());
      double unitAccrual = unitNotionalAccrual(env, valuationDate, accrualPeriod);
      double investFactor = 1 + unitAccrual;
      notionalAccrued *= investFactor;
      System.out.println(notionalAccrued);
    }
    return (notionalAccrued - notional);
//    double df = env.discountFactor(period.getCurrency(), valuationDate, period.getPaymentDate());
////    System.out.println(accrued * df);
//    return accrued * df;
    
    

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
  // calculate the accrual for a unit notional
  private double unitNotionalAccrual(
      PricingEnvironment env,
      LocalDate valuationDate,
      RateAccrualPeriod period) {
    double rate = rateProviderFn.rate(env, valuationDate, period.getRate(), period.getStartDate(), period.getEndDate());
    double treatedRate = rate * period.getGearing() + period.getSpread();
    return treatedRate * period.getYearFraction();
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
