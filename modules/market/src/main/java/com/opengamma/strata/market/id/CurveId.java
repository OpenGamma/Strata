/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.market.curve.Curve;

/**
 * Market data ID identifying a curve.
 */
public interface CurveId extends MarketDataId<Curve> {

  @Override
  public default Class<Curve> getMarketDataType() {
    return Curve.class;
  }
}
