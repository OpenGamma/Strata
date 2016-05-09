/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.util.Optional;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.OvernightIndexCurveId;

/**
 * Market data function that builds a {@link Curve} representing the forward curve of an Overnight index.
 * <p>
 * The curve is not actually built in this class, it is extracted from an existing {@link CurveGroup}.
 * The curve group must be available in the {@code BaseMarketData} passed to the
 * {@link MarketDataFunction#build} method.
 */
public final class OvernightIndexCurveMarketDataFunction implements MarketDataFunction<Curve, OvernightIndexCurveId> {

  @Override
  public MarketDataRequirements requirements(OvernightIndexCurveId id, MarketDataConfig marketDataConfig) {
    CurveGroupId curveGroupId = CurveGroupId.of(id.getCurveGroupName(), id.getMarketDataFeed());
    return MarketDataRequirements.builder()
        .addValues(curveGroupId)
        .build();
  }

  @Override
  public MarketDataBox<Curve> build(
      OvernightIndexCurveId id,
      MarketDataConfig marketDataConfig,
      CalculationEnvironment marketData,
      ReferenceData refData) {

    CurveGroupId curveGroupId = CurveGroupId.of(id.getCurveGroupName(), id.getMarketDataFeed());
    MarketDataBox<CurveGroup> curveGroupBox = marketData.getValue(curveGroupId);
    return curveGroupBox.map(curveGroup -> buildCurve(id, curveGroup));
  }

  private Curve buildCurve(OvernightIndexCurveId id, CurveGroup curveGroup) {
    Optional<Curve> optionalForwardCurve = curveGroup.findForwardCurve(id.getIndex());

    if (optionalForwardCurve.isPresent()) {
      return optionalForwardCurve.get();
    } else {
      throw new IllegalArgumentException(
          Messages.format(
              "No forward curve available for Overnight index {} in curve group {}",
              id.getIndex().getName(),
              id.getCurveGroupName()));
    }
  }

  @Override
  public Class<OvernightIndexCurveId> getMarketDataIdType() {
    return OvernightIndexCurveId.class;
  }

}
