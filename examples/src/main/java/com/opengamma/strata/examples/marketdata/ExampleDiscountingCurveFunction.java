/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import java.time.LocalDate;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.market.id.DiscountingCurveId;

/**
 * Market data function that satisfies requests for discounting curves by loading the
 * calibrated curves from JSON resources.
 * <p>
 * Curves must be available as resources with a name of the form
 * <code>/yieldcurve/[currency]-discounting_[yyyy-mm-dd].json</code>, where
 * <code>[currency]</code> is the curve currency and <code>[yyyy-mm-dd]</code> is
 * the valuation date.
 */
public class ExampleDiscountingCurveFunction
    implements MarketDataFunction<YieldCurve, DiscountingCurveId> {

  @Override
  public MarketDataRequirements requirements(DiscountingCurveId id) {
    return MarketDataRequirements.EMPTY;
  }

  @Override
  public Result<YieldCurve> build(DiscountingCurveId id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
    LocalDate valuationDate = builtData.getValuationDate();
    YieldCurve curve = ExampleMarketData.loadYieldCurve(valuationDate, id.getCurrency() + "-discounting");
    return Result.success(curve);
  }

  @Override
  public Class<DiscountingCurveId> getMarketDataIdType() {
    return DiscountingCurveId.class;
  }

}
