/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * Volatility for swaptions in SABR model.
 * <p>
 * The volatility is represented in terms of SABR model parameters.
 * <p>
 * The prices are calculated using the SABR implied volatility with respect to the Black formula.
 */
public interface SabrSwaptionVolatilities
    extends SwaptionVolatilities {

  @Override
  public default ValueType getVolatilityType() {
    return ValueType.BLACK_VOLATILITY; // SABR implemented with Black implied volatility
  }

  @Override
  public abstract SabrSwaptionVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract SabrSwaptionVolatilities withPerturbation(ParameterPerturbation perturbation);

  //-------------------------------------------------------------------------
  /**
   * Calculates the alpha parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the alpha parameter
   */
  public abstract double alpha(double expiry, double tenor);

  /**
   * Calculates the beta parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the beta parameter
   */
  public abstract double beta(double expiry, double tenor);

  /**
   * Calculates the rho parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the rho parameter
   */
  public abstract double rho(double expiry, double tenor);

  /**
   * Calculates the nu parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the nu parameter
   */
  public abstract double nu(double expiry, double tenor);

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
