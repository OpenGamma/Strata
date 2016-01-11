/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.market.view.SwaptionVolatilities;

/**
 * Volatility for swaptions in SABR model. 
 * <p>
 * The volatility is represented in terms of SABR model parameters.
 */
public interface SabrSwaptionVolatilities
    extends SwaptionVolatilities {

  /**
   * Calculates the shift parameter for the specified time to expiry and instrument tenor.
   * 
   * @param expiry  the expiry
   * @param tenor  the tenor
   * @return the shift parameter
   */
  public abstract double shift(double expiry, double tenor);

  /**
   * Calculates the volatility and associated sensitivities.
   * <p>
   * The derivatives are stored in an array with:
   * <ul>
   * <li>[0] Derivative w.r.t the forward
   * <li>[1] the derivative w.r.t the strike
   * <li>[2] the derivative w.r.t. to alpha
   * <li>[3] the derivative w.r.t. to beta
   * <li>[4] the derivative w.r.t. to rho
   * <li>[5] the derivative w.r.t. to nu
   * </ul>
   * 
   * @param expiry  time to expiry
   * @param tenor  tenor of the instrument
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility and associated sensitivities
   */
  public abstract ValueDerivatives volatilityAdjoint(double expiry, double tenor, double strike, double forward);

}
