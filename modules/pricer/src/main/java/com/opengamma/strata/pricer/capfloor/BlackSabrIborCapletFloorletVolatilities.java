/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.market.ValueType;

/**
 * Volatility for Ibor caplet/floorlet in SABR model.
 * <p>
 * The volatility is represented in terms of SABR model parameters.
 * <p>
 * The prices are calculated using the SABR implied volatility with respect to Black formula.
 */
public interface BlackSabrIborCapletFloorletVolatilities
    extends SabrIborCapletFloorletVolatilities {

  @Override
  public default ValueType getVolatilityType() {
    return ValueType.BLACK_VOLATILITY; // SABR implemented with Black implied volatility
  }

}
