package com.opengamma.strata.examples.exampleccp.marketdatafunctions;

import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.examples.exampleccp.curves.MyCurves;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.id.DiscountCurveId;

public class MyDiscountCurveFunction {

  public static MarketDataFunction<?, ?> create() {
    return new MarketDataFunction<Curve, DiscountCurveId>() {
      @Override
      public MarketDataRequirements requirements(DiscountCurveId id, MarketDataConfig marketDataConfig) {
        return MarketDataRequirements.empty();
      }

      @Override
      public Result<Curve> build(DiscountCurveId id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
        return Result.success(MyCurves.oisCurveDiscount());
      }

      @Override
      public Class<DiscountCurveId> getMarketDataIdType() {
        return DiscountCurveId.class;
      }
    };
  }
}
