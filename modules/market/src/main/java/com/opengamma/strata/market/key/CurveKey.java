/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.market.curve.Curve;

/**
 * A market data key identifying a curve.
 * <p>
 * This is implemented by keys that return a {@link Curve}.
 */
public interface CurveKey extends MarketDataKey<Curve> {

  @Override
  public default Class<Curve> getMarketDataType() {
    return Curve.class;
  }

}
