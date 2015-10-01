/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.fra;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.RateObservation;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.finance.rate.fra.FraProduct;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for for forward rate agreement (FRA) products.
 * <p>
 * This function provides the ability to price a {@link FraProduct}.
 * The product is priced using a forward curve for the index.
 */
public class DiscountingFraProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFraProductPricer DEFAULT = new DiscountingFraProductPricer(
      RateObservationFn.instance());

  /**
   * Rate observation.
   */
  private final RateObservationFn<RateObservation> rateObservationFn;

  /**
   * Creates an instance.
   * 
   * @param rateObservationFn  the rate observation function
   */
  public DiscountingFraProductPricer(
      RateObservationFn<RateObservation> rateObservationFn) {
    this.rateObservationFn = ArgChecker.notNull(rateObservationFn, "rateObservationFn");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FRA product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is the discounted future value.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(FraProduct product, RatesProvider provider) {
    // futureValue * discountFactor
    ExpandedFra fra = product.expand();
    double df = provider.discountFactor(fra.getCurrency(), fra.getPaymentDate());
    double pv = futureValue0(fra, provider) * df;
    return CurrencyAmount.of(fra.getCurrency(), pv);
  }

  /**
   * Calculates the present value sensitivity of the FRA product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(FraProduct product, RatesProvider provider) {
    ExpandedFra fra = product.expand();
    DiscountFactors discountFactors = provider.discountFactors(fra.getCurrency());
    double df = discountFactors.discountFactor(fra.getPaymentDate());
    double notional = fra.getNotional();
    double unitAmount = unitAmount(fra, provider);
    double derivative = derivative(fra, provider);
    PointSensitivityBuilder iborSens = forwardRateSensitivity(fra, provider)
        .multipliedBy(derivative * df * notional);
    PointSensitivityBuilder discSens = discountFactors.zeroRatePointSensitivity(fra.getPaymentDate())
        .multipliedBy(unitAmount * notional);
    return iborSens.withCurrency(fra.getCurrency()).combinedWith(discSens).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future value of the FRA product.
   * <p>
   * The future value of the product is the value on the valuation date without present value discounting.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the future value of the product
   */
  public CurrencyAmount futureValue(FraProduct product, RatesProvider provider) {
    ExpandedFra fra = product.expand();
    double fv = futureValue0(fra, provider);
    return CurrencyAmount.of(fra.getCurrency(), fv);
  }

  /**
   * Calculates the future value sensitivity of the FRA product.
   * <p>
   * The future value sensitivity of the product is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the point sensitivity of the future value
   */
  public PointSensitivities futureValueSensitivity(FraProduct product, RatesProvider provider) {
    ExpandedFra fra = product.expand();
    double notional = fra.getNotional();
    double derivative = derivative(fra, provider);
    PointSensitivityBuilder iborSens = forwardRateSensitivity(fra, provider)
        .multipliedBy(derivative * notional);
    return iborSens.withCurrency(fra.getCurrency()).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par rate of the FRA product.
   * <p>
   * The par rate is the rate for which the FRA present value is 0.
   * 
   * @param product  the FRA product for which the par rate should be computed
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(FraProduct product, RatesProvider provider) {
    return forwardRate(product.expand(), provider);
  }

  /**
   * Calculates the par spread of the FRA product.
   * <p>
   * This is spread to be added to the fixed rate to have a present value of 0.
   * 
   * @param product  the FRA product for which the par spread should be computed
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(FraProduct product, RatesProvider provider) {
    ExpandedFra fra = product.expand();
    double forward = forwardRate(fra, provider);
    return forward - fra.getFixedRate();
  }

  /**
   * Calculates the par spread curve sensitivity of the FRA product.
   * <p>
   * The par spread curve sensitivity of the product is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param product  the FRA product for which the curve sensitivity of the par spread should be computed
   * @param provider  the rates provider
   * @return the par spread sensitivity
   */
  public PointSensitivities parSpreadSensitivity(FraProduct product, RatesProvider provider) {
    return forwardRateSensitivity(product.expand(), provider).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flow of the FRA product.
   * <p>
   * There is only one cash flow on the payment date for the FRA product.
   * The expected currency amount of the cash flow is the same as {@link #futureValue(FraProduct, RatesProvider)}.
   * 
   * @param product  the FRA product for which the cash flow should be computed
   * @param provider  the rates provider
   * @return the cash flows
   */
  public CashFlows cashFlows(FraProduct product, RatesProvider provider) {
    ExpandedFra fra = product.expand();
    LocalDate paymentDate = fra.getPaymentDate();
    double futureValue = futureValue0(fra, provider);
    double df = provider.discountFactor(fra.getCurrency(), paymentDate);
    CashFlow cashFlow = CashFlow.ofFutureValue(paymentDate, fra.getCurrency(), futureValue, df);
    return CashFlows.of(cashFlow);
  }

  //-------------------------------------------------------------------------
  /**
   * Explains the present value of the FRA product.
   * <p>
   * This returns explanatory information about the calculation.
   * 
   * @param product  the FRA product for which present value should be computed
   * @param provider  the rates provider
   * @return the explanatory information
   */
  public ExplainMap explainPresentValue(FraProduct product, RatesProvider provider) {
    ExpandedFra fra = product.expand();

    ExplainMapBuilder builder = ExplainMap.builder();
    Currency currency = fra.getCurrency();
    builder.put(ExplainKey.ENTRY_TYPE, "FRA");
    builder.put(ExplainKey.PAYMENT_DATE, fra.getPaymentDate());
    builder.put(ExplainKey.START_DATE, fra.getStartDate());
    builder.put(ExplainKey.END_DATE, fra.getEndDate());
    builder.put(ExplainKey.ACCRUAL_YEAR_FRACTION, fra.getYearFraction());
    builder.put(ExplainKey.ACCRUAL_DAYS, (int) DAYS.between(fra.getStartDate(), fra.getEndDate()));
    builder.put(ExplainKey.PAYMENT_CURRENCY, currency);
    builder.put(ExplainKey.NOTIONAL, CurrencyAmount.of(currency, fra.getNotional()));
    builder.put(ExplainKey.TRADE_NOTIONAL, CurrencyAmount.of(currency, fra.getNotional()));
    if (fra.getPaymentDate().isBefore(provider.getValuationDate())) {
      builder.put(ExplainKey.FUTURE_VALUE, CurrencyAmount.zero(currency));
      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.zero(currency));
    } else {
      double rate = rateObservationFn.explainRate(
          fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provider, builder);
      builder.put(ExplainKey.FIXED_RATE, fra.getFixedRate());
      builder.put(ExplainKey.DISCOUNT_FACTOR, provider.discountFactor(currency, fra.getPaymentDate()));
      builder.put(ExplainKey.PAY_OFF_RATE, rate);
      builder.put(ExplainKey.UNIT_AMOUNT, unitAmount(fra, provider));
      builder.put(ExplainKey.FUTURE_VALUE, futureValue(fra, provider));
      builder.put(ExplainKey.PRESENT_VALUE, presentValue(fra, provider));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // calculates the future value
  private double futureValue0(ExpandedFra fra, RatesProvider provider) {
    if (fra.getPaymentDate().isBefore(provider.getValuationDate())) {
      return 0d;
    }
    // notional * unitAmount
    return fra.getNotional() * unitAmount(fra, provider);
  }

  // unit amount in various discounting methods
  private double unitAmount(ExpandedFra fra, RatesProvider provider) {
    switch (fra.getDiscounting()) {
      case NONE:
        return unitAmountNone(fra, provider);
      case ISDA:
        return unitAmountIsda(fra, provider);
      case AFMA:
        return unitAmountAfma(fra, provider);
      default:
        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
    }
  }

  // NONE discounting method
  private double unitAmountNone(ExpandedFra fra, RatesProvider provider) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(fra, provider);
    double yearFraction = fra.getYearFraction();
    return (forwardRate - fixedRate) * yearFraction;
  }

  // ISDA discounting method
  private double unitAmountIsda(ExpandedFra fra, RatesProvider provider) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(fra, provider);
    double yearFraction = fra.getYearFraction();
    return ((forwardRate - fixedRate) / (1.0 + forwardRate * yearFraction)) * yearFraction;
  }

  // AFMA discounting method
  private double unitAmountAfma(ExpandedFra fra, RatesProvider provider) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(fra, provider);
    double yearFraction = fra.getYearFraction();
    return (1.0 / (1.0 + fixedRate * yearFraction)) - (1.0 / (1.0 + forwardRate * yearFraction));
  }

  //-------------------------------------------------------------------------
  // determine the derivative
  private double derivative(ExpandedFra fra, RatesProvider provider) {
    switch (fra.getDiscounting()) {
      case NONE:
        return derivativeNone(fra, provider);
      case ISDA:
        return derivativeIsda(fra, provider);
      case AFMA:
        return derivativeAfma(fra, provider);
      default:
        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
    }
  }

  // NONE discounting method
  private double derivativeNone(ExpandedFra fra, RatesProvider provider) {
    return fra.getYearFraction();
  }

  // ISDA discounting method
  private double derivativeIsda(ExpandedFra fra, RatesProvider provider) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(fra, provider);
    double yearFraction = fra.getYearFraction();
    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
    return (1.0 + fixedRate * yearFraction) * yearFraction * dsc * dsc;
  }

  // AFMA discounting method
  private double derivativeAfma(ExpandedFra fra, RatesProvider provider) {
    double forwardRate = forwardRate(fra, provider);
    double yearFraction = fra.getYearFraction();
    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
    return yearFraction * dsc * dsc;
  }

  //-------------------------------------------------------------------------
  // query the forward rate
  private double forwardRate(ExpandedFra fra, RatesProvider provider) {
    return rateObservationFn.rate(fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provider);
  }

  // query the sensitivity
  private PointSensitivityBuilder forwardRateSensitivity(ExpandedFra fra, RatesProvider provider) {
    return rateObservationFn.rateSensitivity(fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provider);
  }

}
