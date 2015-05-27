package com.opengamma.strata.examples.exampleccp;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.market.id.DiscountingCurveId;

public class MyDiscountCurveFunction {

  public static MarketDataFunction<?, ?> create() {
    return new MarketDataFunction<YieldCurve, DiscountingCurveId>() {
      @Override
      public MarketDataRequirements requirements(DiscountingCurveId id, MarketDataConfig marketDataConfig) {
        return MarketDataRequirements.empty();
      }

      @Override
      public Result<YieldCurve> build(DiscountingCurveId id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
        return Result.success(MyCurves.oisCurveDiscount());
      }

      @Override
      public Class<DiscountingCurveId> getMarketDataIdType() {
        return DiscountingCurveId.class;
      }
    };
  }
}
