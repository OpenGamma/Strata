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
 * Market data function that locates a discount factors curve.
 * <p>
 * This function finds an instance of {@link Curve} that can be used to determine discount factors
 * in the currency held in {@link DiscountCurveId}.
 * <p>
 * The curve is not actually built in this class, it is extracted from an existing {@link CurveGroup}.
 * The curve group must be available in the {@code MarketDataLookup} passed to the {@link #build} method.
 */
public class DiscountCurveMarketDataFunction
    implements MarketDataFunction<Curve, DiscountCurveId> {

  /**
   * Public instance of this stateless function.
   */
  public static final DiscountCurveMarketDataFunction INSTANCE = new DiscountCurveMarketDataFunction();

  @Override
  public MarketDataRequirements requirements(DiscountCurveId id, MarketDataConfig config) {
    return MarketDataRequirements.builder()
        .addValues(id.toCurveGroupId())
        .build();
  }

  @Override
  public Result<Curve> build(DiscountCurveId id, MarketDataLookup marketData, MarketDataConfig config) {

    // find curve
    CurveGroupId curveGroupId = id.toCurveGroupId();
    if (!marketData.containsValue(curveGroupId)) {
      return Result.failure(
          FailureReason.MISSING_DATA,
          "No curve group found: Group: {}, Feed: {}",
          id.getCurveGroupName(),
          id.getMarketDataFeed());
    }
    CurveGroup curveGroup = marketData.getValue(curveGroupId);
    Optional<Curve> optionalDiscountCurve = curveGroup.getDiscountCurve(id.getCurrency());
    if (optionalDiscountCurve.isPresent()) {
      return Result.success(optionalDiscountCurve.get());
    } else {
      return Result.failure(
          FailureReason.MISSING_DATA,
          "No discount curve found: Currency: {}, Group: {}, Feed: {}",
          id.getCurrency(),
          id.getCurveGroupName(),
          id.getMarketDataFeed());
    }
  }

  @Override
  public Class<DiscountCurveId> getMarketDataIdType() {
    return DiscountCurveId.class;
  }

}
