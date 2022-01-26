/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.market.ValueType;

/**
 * Volatility for Ibor/Overnight caplet/floorlet in SABR model.
 * <p>
 * The volatility is represented in terms of SABR model parameters.
 * <p>
 * The prices are calculated using the SABR implied volatility with respect to the normal formula.
 */
public interface NormalSabrIborCapletFloorletVolatilities
    extends SabrIborCapletFloorletVolatilities {

  @Override
  public default ValueType getVolatilityType() {
    return ValueType.NORMAL_VOLATILITY; // SABR implemented with Normal implied volatility
  }

}
