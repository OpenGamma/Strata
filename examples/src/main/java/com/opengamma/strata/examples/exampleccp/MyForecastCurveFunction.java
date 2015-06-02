package com.opengamma.strata.examples.exampleccp;

import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.id.RateIndexCurveId;

public class MyForecastCurveFunction {

  public static MarketDataFunction<?, ?> create() {
    return new MarketDataFunction<Curve, RateIndexCurveId>() {
      @Override
      public MarketDataRequirements requirements(RateIndexCurveId id, MarketDataConfig marketDataConfig) {
        return MarketDataRequirements.empty();
      }

      @Override
      public Result<Curve> build(RateIndexCurveId id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
        return Result.success(MyCurves.oisCurve());
      }

      @Override
      public Class<RateIndexCurveId> getMarketDataIdType() {
        return RateIndexCurveId.class;
      }
    };
  }
}
