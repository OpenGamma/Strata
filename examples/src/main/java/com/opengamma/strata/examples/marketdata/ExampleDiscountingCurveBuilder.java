/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.builders.MarketDataBuilder;
import com.opengamma.strata.marketdata.id.DiscountingCurveId;

/**
 * Market data builder that satisifes requests for discounting curves by loading the
 * calibrated curves from JSON resources.
 * <p>
 * Curves must be available as resources with a name of the form
 * <code>/yieldcurve/[currency]-discounting_[yyyy-mm-dd].json</code>, where
 * <code>[currency]</code> is the curve currency and <code>[yyyy-mm-dd]</code> is
 * the valuation date.
 */
public class ExampleDiscountingCurveBuilder
    implements MarketDataBuilder<YieldCurve, DiscountingCurveId> {

  @Override
  public MarketDataRequirements requirements(DiscountingCurveId id) {
    return MarketDataRequirements.EMPTY;
  }

  @Override
  public Map<DiscountingCurveId, Result<YieldCurve>> build(Set<DiscountingCurveId> requirements, BaseMarketData builtData) {
    LocalDate valuationDate = builtData.getValuationDate();
    ImmutableMap.Builder<DiscountingCurveId, Result<YieldCurve>> resultBuilder = ImmutableMap.builder();
    for (DiscountingCurveId curveId : requirements) {
      YieldCurve curve = ExampleMarketData.loadYieldCurve(valuationDate, curveId.getCurrency() + "-discounting");
      resultBuilder.put(curveId, Result.success(curve));
    }
    return resultBuilder.build();
  }

  @Override
  public Class<DiscountingCurveId> getMarketDataIdType() {
    return DiscountingCurveId.class;
  }

}
