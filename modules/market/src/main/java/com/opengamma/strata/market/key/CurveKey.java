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
 */
public interface CurveKey extends MarketDataKey<YieldCurve> {

  @Override
  public default Class<YieldCurve> getMarketDataType() {
    return YieldCurve.class;
  }
}
