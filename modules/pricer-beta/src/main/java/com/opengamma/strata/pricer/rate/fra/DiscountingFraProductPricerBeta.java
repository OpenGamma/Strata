/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.fra;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.RateObservation;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.finance.rate.fra.FraProduct;
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
public class DiscountingFraProductPricerBeta {
  // TODO: tests and better documentation of each measure
  // code copied/modified from ForwardRateAgreementDiscountingMethod
  // TODO: correct formulas when valuation after payment date

  /**
   * Default implementation.
   */
  public static final DiscountingFraProductPricerBeta DEFAULT = new DiscountingFraProductPricerBeta(
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
  public DiscountingFraProductPricerBeta(
      RateObservationFn<RateObservation> rateObservationFn) {
    this.rateObservationFn = ArgChecker.notNull(rateObservationFn, "rateObservationFn");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par rate of the FRA product.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par rate of the product
   */
  public double parRate(FraProduct product, RatesProvider provider) {
    ExpandedFra fra = product.expand();
    return forwardRate(fra, provider);
  }

  /**
   * Calculates the par spread.
   * <p>
   * This is spread to be added to the fixed rate to have a present value of 0.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(FraProduct product, RatesProvider provider) {
    ExpandedFra fra = product.expand();
    double forward = forwardRate(fra, provider);
    return forward - fra.getFixedRate();
  }

  /**
   * Calculates the par spread curve sensitivity.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par spread sensitivity
   */
  public PointSensitivities parSpreadCurveSensitivity(FraProduct product, RatesProvider provider) {
    ExpandedFra fra = product.expand();
    return forwardRateSensitivity(fra, provider).build();
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
