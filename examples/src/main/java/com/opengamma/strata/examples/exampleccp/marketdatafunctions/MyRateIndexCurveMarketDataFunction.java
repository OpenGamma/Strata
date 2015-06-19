/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.exampleccp.marketdatafunctions;

import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.examples.exampleccp.curves.MyCurves;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.RateIndexCurveId;

/**
 * Market data function that builds a {@link Curve} representing the forward curve of an index.
 * <p>
 * The curve is hardcoded to be build in this function.
 */
public final class MyRateIndexCurveMarketDataFunction implements MarketDataFunction<Curve, RateIndexCurveId> {

  @Override
  public MarketDataRequirements requirements(RateIndexCurveId id, MarketDataConfig marketDataConfig) {
    CurveGroupId curveGroupId = CurveGroupId.of(id.getCurveGroupName(), id.getMarketDataFeed());
    return MarketDataRequirements.builder()
        .addValues(curveGroupId)
        .build();
  }

  @Override
  public Result<Curve> build(RateIndexCurveId id, MarketDataLookup marketData, MarketDataConfig marketDataConfig) {
    return Result.success(MyCurves.oisCurve());
  }

  @Override
  public Class<RateIndexCurveId> getMarketDataIdType() {
    return RateIndexCurveId.class;
  }

  public static MarketDataFunction<?, ?> create() {
    return new MyRateIndexCurveMarketDataFunction();
  }

}
