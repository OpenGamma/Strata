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
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.id.RateIndexCurveId;
import com.opengamma.strata.pricer.impl.Legacy;

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
    implements MarketDataFunction<Curve, RateIndexCurveId> {

  @Override
  public MarketDataRequirements requirements(RateIndexCurveId id, MarketDataConfig marketDataConfig) {
    return MarketDataRequirements.empty();
  }

  @Override
  public Result<Curve> build(RateIndexCurveId id, MarketDataLookup marketData, MarketDataConfig marketDataConfig) {
    YieldCurve curve = ExampleMarketData.loadYieldCurve(marketData.getValuationDate(), id.getIndex().getName());
    return Result.success(Legacy.curve(curve));
  }

  @Override
  public Class<RateIndexCurveId> getMarketDataIdType() {
    return RateIndexCurveId.class;
  }

}
