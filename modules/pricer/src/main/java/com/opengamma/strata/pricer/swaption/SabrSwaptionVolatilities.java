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
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the shift parameter
   */
  public abstract double shift(double expiry, double tenor);

  /**
   * Calculates the volatility and associated sensitivities.
   * <p>
   * The derivatives are stored in an array with:
   * <ul>
   * <li>[0] derivative with respect to the forward
   * <li>[1] derivative with respect to the forward strike
   * <li>[2] derivative with respect to the alpha
   * <li>[3] derivative with respect to the beta
   * <li>[4] derivative with respect to the rho
   * <li>[5] derivative with respect to the nu
   * </ul>
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility and associated sensitivities
   */
  public abstract ValueDerivatives volatilityAdjoint(double expiry, double tenor, double strike, double forward);

}
