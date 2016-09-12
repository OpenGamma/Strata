/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * Volatility for FX option in the log-normal or Black model.
 */
public interface BlackFxOptionVolatilities
    extends FxOptionVolatilities {

  @Override
  public default ValueType getVolatilityType() {
    return ValueType.BLACK_VOLATILITY;
  }

  @Override
  public abstract BlackFxOptionVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract BlackFxOptionVolatilities withPerturbation(ParameterPerturbation perturbation);

}
