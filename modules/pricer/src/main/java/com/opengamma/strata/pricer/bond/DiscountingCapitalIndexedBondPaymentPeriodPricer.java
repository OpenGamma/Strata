/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.bond.CapitalIndexedBondPaymentPeriod;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * Pricer implementation for bond payment periods based on a capital indexed coupon.
 * <p>
 * This pricer performs discounting of {@link CapitalIndexedBondPaymentPeriod}.
 */
public class DiscountingCapitalIndexedBondPaymentPeriodPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingCapitalIndexedBondPaymentPeriodPricer DEFAULT =
      new DiscountingCapitalIndexedBondPaymentPeriodPricer(RateComputationFn.standard());
  /**
   * Rate observation.
   */
  private final RateComputationFn<RateComputation> rateComputationFn;

  /**
   * Creates an instance.
   * 
   * @param rateComputationFn  the rate computation function
   */
  public DiscountingCapitalIndexedBondPaymentPeriodPricer(RateComputationFn<RateComputation> rateComputationFn) {
    this.rateComputationFn = ArgChecker.notNull(rateComputationFn, "rateComputationFn");
  }

  /**
   * Obtains the rate computation function.
   * 
   * @return the rate computation function
   */
  public RateComputationFn<RateComputation> getRateComputationFn() {
    return rateComputationFn;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of a single payment period.
   * <p>
   * This returns the value of the period with discounting.
   * If the payment date of the period is in the past, zero is returned.
   * 
   * @param period  the period to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactors  the discount factor provider
   * @return the present value of the period
   */
  public double presentValue(
      CapitalIndexedBondPaymentPeriod period,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors issuerDiscountFactors) {

    double df = issuerDiscountFactors.discountFactor(period.getPaymentDate());
    return df * forecastValue(period, ratesProvider);
  }

  /**
   * Calculates the present value of a single payment period with z-spread.
   * <p>
   * This returns the value of the period with discounting.
   * If the payment date of the period is in the past, zero is returned.
   * 
   * @param period  the period to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactors  the discount factor provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the period
   */
  public double presentValueWithZSpread(
      CapitalIndexedBondPaymentPeriod period,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors issuerDiscountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    double df = issuerDiscountFactors.getDiscountFactors()
        .discountFactorWithSpread(period.getPaymentDate(), zSpread, compoundedRateType, periodsPerYear);
    return df * forecastValue(period, ratesProvider);
  }

  /**
   * Calculates the forecast value of a single payment period.
   * 
   * @param period  the period to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @return the forecast value of the period 
   */
  public double forecastValue(CapitalIndexedBondPaymentPeriod period, RatesProvider ratesProvider) {
    if (period.getPaymentDate().isBefore(ratesProvider.getValuationDate())) {
      return 0d;
    }
    double rate = rateComputationFn.rate(
        period.getRateComputation(), period.getStartDate(), period.getEndDate(), ratesProvider);
    return period.getNotional() * period.getRealCoupon() * (rate + 1d);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of a single payment period.
   * <p>
   * The present value sensitivity of the period is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param period  the period to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactors  the discount factor provider
   * @return the present value curve sensitivity of the period
   */
  public PointSensitivityBuilder presentValueSensitivity(
      CapitalIndexedBondPaymentPeriod period,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors issuerDiscountFactors) {

    if (period.getPaymentDate().isBefore(ratesProvider.getValuationDate())) {
      return PointSensitivityBuilder.none();
    }
    double rate = rateComputationFn.rate(
        period.getRateComputation(), period.getStartDate(), period.getEndDate(), ratesProvider);
    PointSensitivityBuilder rateSensi = rateComputationFn.rateSensitivity(
        period.getRateComputation(), period.getStartDate(), period.getEndDate(), ratesProvider);
    double df = issuerDiscountFactors.discountFactor(period.getPaymentDate());
    PointSensitivityBuilder dfSensi = issuerDiscountFactors.zeroRatePointSensitivity(period.getPaymentDate());
    double factor = period.getNotional() * period.getRealCoupon();
    return rateSensi.multipliedBy(df * factor).combinedWith(dfSensi.multipliedBy((rate + 1d) * factor));
  }

  /**
   * Calculates the present value sensitivity of a single payment period with z-spread.
   * <p>
   * The present value sensitivity of the period is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param period  the period to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactors  the discount factor provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value curve sensitivity of the period
   */
  public PointSensitivityBuilder presentValueSensitivityWithZSpread(
      CapitalIndexedBondPaymentPeriod period,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors issuerDiscountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (period.getPaymentDate().isBefore(ratesProvider.getValuationDate())) {
      return PointSensitivityBuilder.none();
    }
    double rate = rateComputationFn.rate(
        period.getRateComputation(), period.getStartDate(), period.getEndDate(), ratesProvider);
    PointSensitivityBuilder rateSensi = rateComputationFn.rateSensitivity(
        period.getRateComputation(), period.getStartDate(), period.getEndDate(), ratesProvider);
    double df = issuerDiscountFactors.getDiscountFactors()
        .discountFactorWithSpread(period.getPaymentDate(), zSpread, compoundedRateType, periodsPerYear);
    ZeroRateSensitivity zeroSensi = issuerDiscountFactors.getDiscountFactors()
        .zeroRatePointSensitivityWithSpread(period.getPaymentDate(), zSpread, compoundedRateType, periodsPerYear);
    IssuerCurveZeroRateSensitivity dfSensi =
        IssuerCurveZeroRateSensitivity.of(zeroSensi, issuerDiscountFactors.getLegalEntityGroup());
    double factor = period.getNotional() * period.getRealCoupon();
    return rateSensi.multipliedBy(df * factor).combinedWith(dfSensi.multipliedBy((rate + 1d) * factor));
  }

  /**
   * Calculates the forecast value sensitivity of a single payment period.
   * <p>
   * The forecast value sensitivity of the period is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param period  the period to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @return the forecast value sensitivity of the period 
   */
  public PointSensitivityBuilder forecastValueSensitivity(
      CapitalIndexedBondPaymentPeriod period,
      RatesProvider ratesProvider) {

    if (period.getPaymentDate().isBefore(ratesProvider.getValuationDate())) {
      return PointSensitivityBuilder.none();
    }
    PointSensitivityBuilder rateSensi = rateComputationFn.rateSensitivity(
        period.getRateComputation(), period.getStartDate(), period.getEndDate(), ratesProvider);
    return rateSensi.multipliedBy(period.getNotional() * period.getRealCoupon());
  }

  //-------------------------------------------------------------------------
  /**
   * Explains the present value of a single payment period.
   * <p>
   * This adds information to the {@link ExplainMapBuilder} to aid understanding of the calculation.
   * 
   * @param period  the period to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactors  the discount factor provider
   * @param builder  the builder to populate
   */
  public void explainPresentValue(
      CapitalIndexedBondPaymentPeriod period,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors issuerDiscountFactors,
      ExplainMapBuilder builder) {

    Currency currency = period.getCurrency();
    LocalDate paymentDate = period.getPaymentDate();
    builder.put(ExplainKey.ENTRY_TYPE, "CapitalIndexedBondPaymentPeriod");
    builder.put(ExplainKey.PAYMENT_DATE, paymentDate);
    builder.put(ExplainKey.PAYMENT_CURRENCY, currency);
    builder.put(ExplainKey.START_DATE, period.getStartDate());
    builder.put(ExplainKey.UNADJUSTED_START_DATE, period.getUnadjustedStartDate());
    builder.put(ExplainKey.END_DATE, period.getEndDate());
    builder.put(ExplainKey.UNADJUSTED_END_DATE, period.getUnadjustedEndDate());
    builder.put(ExplainKey.DAYS, (int) DAYS.between(period.getUnadjustedStartDate(), period.getUnadjustedEndDate()));
    if (paymentDate.isBefore(ratesProvider.getValuationDate())) {
      builder.put(ExplainKey.COMPLETED, Boolean.TRUE);
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.zero(currency));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.zero(currency));
    } else {
      builder.put(ExplainKey.DISCOUNT_FACTOR, issuerDiscountFactors.discountFactor(paymentDate));
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.of(currency, forecastValue(period, ratesProvider)));
      builder.put(ExplainKey.PRESENT_VALUE,
          CurrencyAmount.of(currency, presentValue(period, ratesProvider, issuerDiscountFactors)));
    }
  }

  /**
   * Explains the present value of a single payment period with z-spread.
   * <p>
   * This adds information to the {@link ExplainMapBuilder} to aid understanding of the calculation.
   * 
   * @param period  the period to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactors  the discount factor provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @param builder  the builder to populate
   */
  public void explainPresentValueWithZSpread(
      CapitalIndexedBondPaymentPeriod period,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors issuerDiscountFactors,
      ExplainMapBuilder builder,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    Currency currency = period.getCurrency();
    LocalDate paymentDate = period.getPaymentDate();
    builder.put(ExplainKey.ENTRY_TYPE, "CapitalIndexedBondPaymentPeriod");
    builder.put(ExplainKey.PAYMENT_DATE, paymentDate);
    builder.put(ExplainKey.PAYMENT_CURRENCY, currency);
    builder.put(ExplainKey.START_DATE, period.getStartDate());
    builder.put(ExplainKey.UNADJUSTED_START_DATE, period.getUnadjustedStartDate());
    builder.put(ExplainKey.END_DATE, period.getEndDate());
    builder.put(ExplainKey.UNADJUSTED_END_DATE, period.getUnadjustedEndDate());
    builder.put(ExplainKey.DAYS, (int) DAYS.between(period.getUnadjustedStartDate(), period.getUnadjustedEndDate()));
    if (paymentDate.isBefore(ratesProvider.getValuationDate())) {
      builder.put(ExplainKey.COMPLETED, Boolean.TRUE);
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.zero(currency));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.zero(currency));
    } else {
      builder.put(ExplainKey.DISCOUNT_FACTOR, issuerDiscountFactors.discountFactor(paymentDate));
      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.of(currency, forecastValue(period, ratesProvider)));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.of(currency, presentValueWithZSpread(
          period, ratesProvider, issuerDiscountFactors, zSpread, compoundedRateType, periodsPerYear)));
    }
  }

}
