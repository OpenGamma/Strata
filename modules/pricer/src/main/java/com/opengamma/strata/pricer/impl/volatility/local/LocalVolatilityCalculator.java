/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.local;

import java.util.function.Function;

import com.opengamma.strata.market.surface.Surface;

/**
 * Local volatility calculation.
 */
public interface LocalVolatilityCalculator {

  /**
   * Computes local volatility surface from call price surface.
   * <p>
   * The interest rate and dividend rate must be zero-coupon continuously compounded rates based on respective day 
   * count convention.
   * Thus {@code interestRate} and {@code dividendRate} are functions from year fraction to zero rate.
   * 
   * @param callPriceSurface  the price surface
   * @param spot  the spot
   * @param interestRate  the interest rate
   * @param dividendRate  the dividend rate
   * @return the local volatility surface
   */
  public abstract Surface localVolatilityFromPrice(
      Surface callPriceSurface,
      double spot,
      Function<Double, Double> interestRate,
      Function<Double, Double> dividendRate);

  /**
   * Computes local volatility surface from implied volatility surface.
   * <p>
   * The implied volatility surface must be spanned by time to expiry and strike.
   * <p>
   * The interest rate and dividend rate must be zero-coupon continuously compounded rates based on 
   * respective day count convention.
   * Thus {@code interestRate} and {@code dividendRate} are functions from year fraction to zero rate.
   * 
   * @param impliedVolatilitySurface  the implied volatility surface
   * @param spot  the spot
   * @param interestRate  the interest rate
   * @param dividendRate  the dividend
   * @return the local volatility surface
   */
  public abstract Surface localVolatilityFromImpliedVolatility(
      Surface impliedVolatilitySurface,
      double spot,
      Function<Double, Double> interestRate,
      Function<Double, Double> dividendRate);

}
