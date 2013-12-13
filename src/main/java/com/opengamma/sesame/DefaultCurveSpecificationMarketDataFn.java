/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FunctionResultGenerator.propagateFailure;
import static com.opengamma.util.result.FunctionResultGenerator.success;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.sesame.marketdata.CurveNodeMarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.result.FunctionResult;

public class DefaultCurveSpecificationMarketDataFn implements CurveSpecificationMarketDataFn {

  private final MarketDataFn _marketDataFn;

  public DefaultCurveSpecificationMarketDataFn(MarketDataFn marketDataFn) {
    _marketDataFn = marketDataFn;
  }

  // TODO we're looping over the set of nodes twice, do it in one go?
  @Override
  public FunctionResult<MarketDataValues> requestData(CurveSpecification curveSpecification) {
    final Set<MarketDataRequirement> requirements = new HashSet<>();
    for (final CurveNodeWithIdentifier id : curveSpecification.getNodes()) {
      MarketDataRequirement fwdReq = MarketDataRequirementFactory.of(id);
      requirements.add(fwdReq);
      if (id instanceof PointsCurveNodeWithIdentifier) {
        final PointsCurveNodeWithIdentifier node = (PointsCurveNodeWithIdentifier) id;
        requirements.add(new CurveNodeMarketDataRequirement(node.getUnderlyingIdentifier(), node.getUnderlyingDataField()));
      }
    }
    FunctionResult<MarketDataValues> result = _marketDataFn.requestData(requirements);
    if (!result.isResultAvailable()) {
      return propagateFailure(result);
    }
    Map<MarketDataRequirement, MarketDataItem> items = Maps.newHashMap();
    for (final CurveNodeWithIdentifier id : curveSpecification.getNodes()) {
      MarketDataRequirement fwdReq = MarketDataRequirementFactory.of(id);
      // TODO check result is available
      Double fwd = (Double) result.getResult().getValue(fwdReq);
      if (id instanceof PointsCurveNodeWithIdentifier) {
        final PointsCurveNodeWithIdentifier node = (PointsCurveNodeWithIdentifier) id;
        CurveNodeMarketDataRequirement spotReq = new CurveNodeMarketDataRequirement(node.getUnderlyingIdentifier(),
                                                                                    node.getUnderlyingDataField());
        // TODO check result is available
        Double spot = (Double) result.getResult().getValue(spotReq);
        items.put(fwdReq, MarketDataItem.available(fwd + spot));
      } else {
        items.put(fwdReq, MarketDataItem.available(fwd));
      }
    }
    return success(new MarketDataValues(items, Collections.<MarketDataRequirement>emptySet()));
  }
}
