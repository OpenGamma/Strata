/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.market.MarketDataKey;

/**
 * A market data key identifying a yield curve.
 * <p>
 * This is implemented by keys that return a {@link YieldCurve}.
 */
public interface YieldCurveKey extends MarketDataKey<YieldCurve> {

  @Override
  public default Class<YieldCurve> getMarketDataType() {
    return YieldCurve.class;
  }

}
