/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.fra;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.RateObservation;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.finance.rate.fra.FraProduct;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

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
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(PricingEnvironment env, FraProduct product) {
    // futureValue * discountFactor
    ExpandedFra fra = product.expand();
    double df = env.discountFactor(fra.getCurrency(), fra.getPaymentDate());
    double pv = futureValue(env, fra) * df;
    return CurrencyAmount.of(fra.getCurrency(), pv);
  }

  /**
   * Calculates the present value sensitivity of the FRA product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(PricingEnvironment env, FraProduct product) {
    ExpandedFra fra = product.expand();
    double df = env.discountFactor(fra.getCurrency(), fra.getPaymentDate());
    double notional = fra.getNotional();
    double unitAmount = unitAmount(env, fra);
    double derivative = derivative(env, fra);
    PointSensitivityBuilder iborSens = forwardRateSensitivity(env, fra)
        .multipliedBy(derivative * df * notional);
    PointSensitivityBuilder discSens =
        env.discountFactorZeroRateSensitivity(fra.getCurrency(), fra.getPaymentDate())
            .multipliedBy(unitAmount * notional);
    return iborSens.withCurrency(fra.getCurrency()).combinedWith(discSens).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future value of the FRA product.
   * <p>
   * The future value of the product is the value on the valuation date without present value discounting.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the future value of the product
   */
  public CurrencyAmount futureValue(PricingEnvironment env, FraProduct product) {
    ExpandedFra fra = product.expand();
    double fv = futureValue(env, fra);
    return CurrencyAmount.of(fra.getCurrency(), fv);
  }

  /**
   * Calculates the future value sensitivity of the FRA product.
   * <p>
   * The future value sensitivity of the product is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the point sensitivity of the future value
   */
  public PointSensitivities futureValueSensitivity(PricingEnvironment env, FraProduct product) {
    ExpandedFra fra = product.expand();
    double notional = fra.getNotional();
    double derivative = derivative(env, fra);
    PointSensitivityBuilder iborSens = forwardRateSensitivity(env, fra)
        .multipliedBy(derivative * notional);
    return iborSens.withCurrency(fra.getCurrency()).build();
  }

  //-------------------------------------------------------------------------
  // calculates the future value
  private double futureValue(PricingEnvironment env, ExpandedFra fra) {
    if (fra.getPaymentDate().isBefore(env.getValuationDate())) {
      return 0d;
    }
    // notional * unitAmount
    return fra.getNotional() * unitAmount(env, fra);
  }

  // unit amount in various discounting methods
  private double unitAmount(PricingEnvironment env, ExpandedFra fra) {
    switch (fra.getDiscounting()) {
      case NONE:
        return unitAmountNone(env, fra);
      case ISDA:
        return unitAmountIsda(env, fra);
      case AFMA:
        return unitAmountAfma(env, fra);
      default:
        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
    }
  }

  // NONE discounting method
  private double unitAmountNone(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return (forwardRate - fixedRate) * yearFraction;
  }

  // ISDA discounting method
  private double unitAmountIsda(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return ((forwardRate - fixedRate) / (1.0 + forwardRate * yearFraction)) * yearFraction;
  }

  // AFMA discounting method
  private double unitAmountAfma(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    return (1.0 / (1.0 + fixedRate * yearFraction)) - (1.0 / (1.0 + forwardRate * yearFraction));
  }

  //-------------------------------------------------------------------------
  // determine the derivative
  private double derivative(PricingEnvironment env, ExpandedFra fra) {
    switch (fra.getDiscounting()) {
      case NONE:
        return derivativeNone(env, fra);
      case ISDA:
        return derivativeIsda(env, fra);
      case AFMA:
        return derivativeAfma(env, fra);
      default:
        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
    }
  }

  // NONE discounting method
  private double derivativeNone(PricingEnvironment env, ExpandedFra fra) {
    return fra.getYearFraction();
  }

  // ISDA discounting method
  private double derivativeIsda(PricingEnvironment env, ExpandedFra fra) {
    double fixedRate = fra.getFixedRate();
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
    return (1.0 + fixedRate * yearFraction) * yearFraction * dsc * dsc;
  }

  // AFMA discounting method
  private double derivativeAfma(PricingEnvironment env, ExpandedFra fra) {
    double forwardRate = forwardRate(env, fra);
    double yearFraction = fra.getYearFraction();
    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
    return yearFraction * dsc * dsc;
  }

  //-------------------------------------------------------------------------
  // query the forward rate
  private double forwardRate(PricingEnvironment env, ExpandedFra fra) {
    return rateObservationFn.rate(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
  }

  // query the sensitivity
  private PointSensitivityBuilder forwardRateSensitivity(PricingEnvironment env, ExpandedFra fra) {
    return rateObservationFn.rateSensitivity(env, fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
  }

}
