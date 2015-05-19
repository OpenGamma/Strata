/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.RateIndexCurveId;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Market data function that builds a {@link YieldCurve} representing the forward curve of an index.
 * <p>
 * The curve is not actually built in this class, it is extracted from an existing {@link CurveGroup}
 * The curve group must be available in the {@code BaseMarketData} passed to the {@link MarketDataFunction#build} method.
 * <p>
 * This builder assumes all discounting curves are of type {@link YieldCurve}, although the signature of
 * {@code CurveGroup} allows them to be {@link YieldAndDiscountCurve}. This assumption is safe. In future
 * it is possible that it will change, but there are many other changes needed before other curve types
 * can be used.
 */
public final class RateIndexCurveMarketDataFunction implements MarketDataFunction<YieldCurve, RateIndexCurveId> {

  @Override
  public MarketDataRequirements requirements(RateIndexCurveId id) {
    CurveGroupId curveGroupId = CurveGroupId.of(id.getCurveGroupName());
    return MarketDataRequirements.builder()
        .addValues(curveGroupId)
        .build();
  }

  @Override
  public Result<YieldCurve> build(RateIndexCurveId id, MarketDataLookup builtData, MarketDataConfig marketDataConfig) {
    String curveGroupName = id.getCurveGroupName();
    CurveGroupId curveGroupId = CurveGroupId.of(curveGroupName);

    if (!builtData.containsValue(curveGroupId)) {
      return Result.failure(FailureReason.MISSING_DATA, "No curve group found with name {}", curveGroupId.getName());
    }
    CurveGroup curveGroup = builtData.getValue(curveGroupId);
    Result<YieldAndDiscountCurve> result = getCurve(curveGroup, curveGroupName, id.getIndex());
    return result.flatMap(curve -> castCurve(curve, id));
  }

  @Override
  public Class<RateIndexCurveId> getMarketDataIdType() {
    return RateIndexCurveId.class;
  }

  /**
   * Returns a curve for the specified index from the curve group, or null if there is no curve
   * for the index or the index is of an unexpected type.
   *
   * @param curveGroup  a curve group
   * @param curveGroupName  the name of the curve group
   * @param index  an index
   * @return a curve for the index from the curve group, or empty if there isn't one or the index is
   *   of an unexpected type
   */
  private static Result<YieldAndDiscountCurve> getCurve(CurveGroup curveGroup, Object curveGroupName, Index index) {
    MulticurveProviderDiscount multicurve = curveGroup.getMulticurveProvider();

    if (index instanceof IborIndex) {
      return Result.ofNullable(
          multicurve.getCurve(Legacy.iborIndex((IborIndex) index)),
          FailureReason.MISSING_DATA,
          "No curve available for index {} in curve group {}",
          index.getName(),
          curveGroupName);
    } else if (index instanceof OvernightIndex) {
      return Result.ofNullable(
          multicurve.getCurve(Legacy.overnightIndex((OvernightIndex) index)),
          FailureReason.MISSING_DATA,
          "No curve available for index {} in curve group {}",
          index.getName(),
          curveGroupName);
    } else {
      return Result.failure(FailureReason.MISSING_DATA, "Unexpected index type {}", index.getClass().getName());
    }
  }

  /**
   * Returns a success result containing the curve if it is a {@link YieldCurve}, else returns a failure.
   *
   * @param curve  a curve
   * @param id  the curve ID
   * @return a success result containing the curve if it is a {@link YieldCurve}, else a failure
   */
  private Result<YieldCurve> castCurve(YieldAndDiscountCurve curve, RateIndexCurveId id) {
    return (curve instanceof YieldCurve) ?
        Result.success(((YieldCurve) curve)) :
        Result.failure(
            FailureReason.OTHER,
            "Curve with ID {} should be a YieldCurve but type is {}",
            id,
            curve.getClass().getName());
  }
}
