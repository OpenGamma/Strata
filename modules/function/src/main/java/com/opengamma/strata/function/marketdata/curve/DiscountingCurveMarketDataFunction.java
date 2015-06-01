/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.util.Optional;

import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.DiscountCurveId;

/**
 * Market data function that builds a {@link Curve} representing the discounting curve for a currency.
 * <p>
 * The curve is not actually built in this class, it is extracted from an existing {@link CurveGroup}.
 * The curve group must be available in the {@code MarketDataLookup} passed to the
 * {@link MarketDataFunction#build} method.
 */
public class DiscountingCurveMarketDataFunction implements MarketDataFunction<Curve, DiscountCurveId> {

  @Override
  public MarketDataRequirements requirements(DiscountCurveId id, MarketDataConfig marketDataConfig) {
    CurveGroupId curveGroupId = CurveGroupId.of(id.getCurveGroupName(), id.getMarketDataFeed());
    return MarketDataRequirements.builder()
        .addValues(curveGroupId)
        .build();
  }

  @Override
  public Result<Curve> build(DiscountCurveId id, MarketDataLookup marketData, MarketDataConfig marketDataConfig) {
    CurveGroupId curveGroupId = CurveGroupId.of(id.getCurveGroupName(), id.getMarketDataFeed());

    if (!marketData.containsValue(curveGroupId)) {
      return Result.failure(FailureReason.MISSING_DATA, "No curve group found with name {}", curveGroupId.getName());
    }
    CurveGroup curveGroup = marketData.getValue(curveGroupId);
    Optional<Curve> optionalDiscountCurve = curveGroup.getDiscountCurve(id.getCurrency());

    if (optionalDiscountCurve.isPresent()) {
      return Result.success(optionalDiscountCurve.get());
    } else {
      return Result.failure(
          FailureReason.MISSING_DATA,
          "No discount curve available for {} in curve group {}",
          id.getCurrency(),
          id.getCurveGroupName());
    }
  }

  @Override
  public Class<DiscountCurveId> getMarketDataIdType() {
    return DiscountCurveId.class;
  }
}
