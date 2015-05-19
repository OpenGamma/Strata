/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.market.id.RateIndexCurveId;

/**
 * Market data function that satisfies requests for forward curves by loading the
 * calibrated curves from JSON resources.
 * <p>
 * Curves must be available as resources with a name of the form
 * <code>/yieldcurve/[index]_[yyyy-mm-dd].json</code>, where
 * <code>[index]</code> is name of the curve index and <code>[yyyy-mm-dd]</code> is
 * the valuation date.
 */
public class ExampleForwardCurveFunction
    implements MarketDataFunction<YieldCurve, RateIndexCurveId> {

  @Override
  public MarketDataRequirements requirements(RateIndexCurveId id) {
    return MarketDataRequirements.EMPTY;
  }

  @Override
  public Result<YieldCurve> build(RateIndexCurveId id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
    YieldCurve curve = ExampleMarketData.loadYieldCurve(builtData.getValuationDate(), id.getIndex().getName());
    return Result.success(curve);
  }

  @Override
  public Class<RateIndexCurveId> getMarketDataIdType() {
    return RateIndexCurveId.class;
  }

}
