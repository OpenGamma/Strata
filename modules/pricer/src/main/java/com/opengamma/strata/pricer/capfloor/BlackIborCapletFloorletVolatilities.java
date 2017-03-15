/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * Volatility for Ibor caplet/floorlet in the log-normal or Black model.
 */
public interface BlackIborCapletFloorletVolatilities
    extends IborCapletFloorletVolatilities {

  @Override
  public default ValueType getVolatilityType() {
    return ValueType.BLACK_VOLATILITY;
  }

  @Override
  public abstract BlackIborCapletFloorletVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract BlackIborCapletFloorletVolatilities withPerturbation(ParameterPerturbation perturbation);

}
