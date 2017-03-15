/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.model;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrHaganVolatilityFunctionProvider;

/**
 * Provides volatility and sensitivity in the SABR model.
 */
public interface SabrVolatilityFormula {

  /**
   * The Hagan SABR volatility formula.
   * <p>
   * This provides the functions of volatility and its sensitivity to the SABR model
   * parameters based on the original Hagan SABR formula.
   * <p>
   * Reference: Hagan, P.; Kumar, D.; Lesniewski, A. & Woodward, D. "Managing smile risk", Wilmott Magazine, 2002, September, 84-108
   * <p>
   * OpenGamma documentation: SABR Implementation, OpenGamma documentation n. 33, April 2016.
   * 
   * @return the SABR Hagan formula
   */
  public static SabrVolatilityFormula hagan() {
    return SabrHaganVolatilityFunctionProvider.DEFAULT;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the volatility.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param alpha  the SABR alpha value
   * @param beta  the SABR beta value
   * @param rho  the SABR rho value
   * @param nu  the SABR nu value
   * @return the volatility
   */
  public abstract double volatility(
      double forward,
      double strike,
      double timeToExpiry,
      double alpha,
      double beta,
      double rho,
      double nu);

  /**
   * Calculates volatility and the adjoint (volatility sensitivity to forward, strike and model parameters). 
   * <p>
   * By default the derivatives are computed by central finite difference approximation.
   * This should be overridden in each subclass.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param alpha  the SABR alpha value
   * @param beta  the SABR beta value
   * @param rho  the SABR rho value
   * @param nu  the SABR nu value
   * @return the volatility and associated derivatives
   */
  public ValueDerivatives volatilityAdjoint(
      double forward,
      double strike,
      double timeToExpiry,
      double alpha,
      double beta,
      double rho,
      double nu);

}
