/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import com.opengamma.strata.market.ValueType;

/**
 * Volatility for swaptions in SABR model.
 * <p>
 * The volatility is represented in terms of SABR model parameters.
 * <p>
 * The prices are calculated using the SABR implied volatility with respect to the normal formula.
 */
public interface NormalSabrSwaptionVolatilities 
    extends SabrSwaptionVolatilities {

  @Override
  public default ValueType getVolatilityType() {
    return ValueType.NORMAL_VOLATILITY; // SABR implemented with Normal implied volatility
  }

}
