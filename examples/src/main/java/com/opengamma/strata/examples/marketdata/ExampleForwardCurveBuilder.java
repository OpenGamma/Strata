/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.builders.MarketDataBuilder;
import com.opengamma.strata.marketdata.id.IndexCurveId;

/**
 * Market data builder that satisfies requests for forward curves by loading the
 * calibrated curves from JSON resources.
 * <p>
 * Curves must be available as resources with a name of the form
 * <code>/yieldcurve/[index]_[yyyy-mm-dd].json</code>, where
 * <code>[index]</code> is name of the curve index and <code>[yyyy-mm-dd]</code> is
 * the valuation date.
 */
public class ExampleForwardCurveBuilder
    implements MarketDataBuilder<YieldCurve, IndexCurveId> {

  @Override
  public MarketDataRequirements requirements(IndexCurveId id) {
    return MarketDataRequirements.EMPTY;
  }

  @Override
  public Result<YieldCurve> build(IndexCurveId requirement, BaseMarketData builtData) {
    YieldCurve curve = ExampleMarketData.loadYieldCurve(builtData.getValuationDate(), requirement.getIndex().getName());
    return Result.success(curve);
  }

  @Override
  public Class<IndexCurveId> getMarketDataIdType() {
    return IndexCurveId.class;
  }

}
