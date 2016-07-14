/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * Volatility for pricing bond futures and their options in the log-normal or Black model.
 */
public interface BlackBondFutureVolatilities extends BondFutureVolatilities {

  @Override
  public default ValueType getVolatilityType() {
    return ValueType.BLACK_VOLATILITY;
  }

  @Override
  public abstract BlackBondFutureVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract BlackBondFutureVolatilities withPerturbation(ParameterPerturbation perturbation);

}
