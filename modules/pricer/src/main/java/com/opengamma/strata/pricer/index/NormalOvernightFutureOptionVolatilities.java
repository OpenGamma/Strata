/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * Volatility for overnight future options in the normal or Bachelier model.
 */
public interface NormalOvernightFutureOptionVolatilities
    extends OvernightFutureOptionVolatilities {

  @Override
  public default ValueType getVolatilityType() {
    return ValueType.NORMAL_VOLATILITY;
  }

  @Override
  public abstract NormalOvernightFutureOptionVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract NormalOvernightFutureOptionVolatilities withPerturbation(ParameterPerturbation perturbation);

}
