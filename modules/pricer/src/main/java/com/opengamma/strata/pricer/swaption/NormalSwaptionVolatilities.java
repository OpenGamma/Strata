/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * Volatility for swaptions in the normal or Bachelier model.
 */
public interface NormalSwaptionVolatilities
    extends SwaptionVolatilities {

  @Override
  public abstract NormalSwaptionVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract NormalSwaptionVolatilities withPerturbation(ParameterPerturbation perturbation);

}
