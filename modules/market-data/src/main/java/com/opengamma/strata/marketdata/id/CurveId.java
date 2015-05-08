/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.id;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;

/**
 * Market data ID identifying a curve.
 */
public interface CurveId extends MarketDataId<YieldCurve> {

  @Override
  public default Class<YieldCurve> getMarketDataType() {
    return YieldCurve.class;
  }
}
