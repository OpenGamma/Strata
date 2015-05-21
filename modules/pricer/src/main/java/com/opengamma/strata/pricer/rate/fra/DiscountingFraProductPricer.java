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
import com.opengamma.strata.market.curve.DiscountFactors;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
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
    double pv = futureValue(fra, provider) * df;
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
    PointSensitivityBuilder discSens = discountFactors.pointSensitivity(fra.getPaymentDate())
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
    double fv = futureValue(fra, provider);
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

  //-------------------------------------------------------------------------
  // calculates the future value
  private double futureValue(ExpandedFra fra, RatesProvider provider) {
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
