/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.curve;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.RatesCurveGroup;

/**
 * Market data function that locates a curve by name.
 * <p>
 * This function finds an instance of {@link Curve} using the name held in {@link CurveId}.
 * <p>
 * The curve is not actually built in this class, it is extracted from an existing {@link RatesCurveGroup}.
 * The curve group must be available in the {@code MarketDataLookup} passed to the {@link #build} method.
 */
public class CurveMarketDataFunction
    implements MarketDataFunction<Curve, CurveId> {

  @Override
  public MarketDataRequirements requirements(CurveId id, MarketDataConfig config) {
    CurveGroupDefinition groupDefn = config.get(CurveGroupDefinition.class, id.getCurveGroupName());
    MarketDataId<? extends CurveGroup> groupId = groupDefn.createGroupId(id.getObservableSource());
    return MarketDataRequirements.of(groupId);
  }

  @Override
  public MarketDataBox<Curve> build(
      CurveId id,
      MarketDataConfig config,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    // find curve
    CurveGroupDefinition groupDefn = config.get(CurveGroupDefinition.class, id.getCurveGroupName());
    MarketDataId<? extends CurveGroup> groupId = groupDefn.createGroupId(id.getObservableSource());
    MarketDataBox<? extends CurveGroup> curveGroupBox = marketData.getValue(groupId);
    return curveGroupBox.map(curveGroup -> findCurve(id, curveGroup));
  }

  // finds the curve
  private Curve findCurve(CurveId id, CurveGroup curveGroup) {
    return curveGroup.findCurve(id.getCurveName())
        .orElseThrow(() -> new IllegalArgumentException(Messages.format("No curve found: {}", id.getCurveName())));
  }

  @Override
  public Class<CurveId> getMarketDataIdType() {
    return CurveId.class;
  }

}
