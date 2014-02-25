/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.success;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.Maps;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.sesame.marketdata.CurveNodeMarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Function implementation that provides market data for a curve specification.
 */
public class DefaultCurveSpecificationMarketDataFn implements CurveSpecificationMarketDataFn {

  /**
   * The market data function.
   */
  private final MarketDataFn _marketDataFn;

  /**
   * Valuation time provider
   */
  private final ValuationTimeFn _valuationTimeProvider;

  public DefaultCurveSpecificationMarketDataFn(MarketDataFn marketDataFn, ValuationTimeFn valuationTimeProvider) {
    _marketDataFn = marketDataFn;
    _valuationTimeProvider = valuationTimeProvider;
  }

  @Override
  public Result<MarketDataValues> requestData(CurveSpecification curveSpecification) {
    ZonedDateTime time = _valuationTimeProvider.getTime();
    return requestData(curveSpecification, time);
  }

  //-------------------------------------------------------------------------

  @Override
  public Result<MarketDataValues> requestData(CurveSpecification curveSpecification, ZonedDateTime valuationTime) {
    // TODO we're looping over the set of nodes twice, do it in one go? not sure that's possible
    Set<MarketDataRequirement> requirements = new HashSet<>();
    for (CurveNodeWithIdentifier id : curveSpecification.getNodes()) {
      MarketDataRequirement fwdReq = MarketDataRequirementFactory.of(id);
      requirements.add(fwdReq);
      if (id instanceof PointsCurveNodeWithIdentifier) {
        PointsCurveNodeWithIdentifier node = (PointsCurveNodeWithIdentifier) id;
        requirements.add(new CurveNodeMarketDataRequirement(node.getUnderlyingIdentifier(), node.getUnderlyingDataField()));
      }
    }
    Result<MarketDataValues> result = _marketDataFn.requestData(requirements, valuationTime);
    if (!result.isValueAvailable()) {
      return propagateFailure(result);
    }
    Map<MarketDataRequirement, MarketDataItem> items = Maps.newHashMap();
    for (CurveNodeWithIdentifier id : curveSpecification.getNodes()) {
      MarketDataRequirement fwdReq = MarketDataRequirementFactory.of(id);
      MarketDataValues value = result.getValue();
      if (value.getStatus(fwdReq) != MarketDataStatus.AVAILABLE) {
        //TODO return a set of failures (see SSM-115)
        return failure(FailureStatus.MISSING_DATA, "Unavailable market data: {}", fwdReq);
      }
      Double fwd = (Double) value.getValue(fwdReq);
      if (id instanceof PointsCurveNodeWithIdentifier) {
        PointsCurveNodeWithIdentifier node = (PointsCurveNodeWithIdentifier) id;
        CurveNodeMarketDataRequirement spotReq = new CurveNodeMarketDataRequirement(node.getUnderlyingIdentifier(),
            node.getUnderlyingDataField());
        // TODO check result is available
        Double spot = (Double) result.getValue().getValue(spotReq);
        items.put(fwdReq, MarketDataItem.available(fwd + spot));
      } else {
        items.put(fwdReq, MarketDataItem.available(fwd));
      }
    }
    return success(new MarketDataValues(items, Collections.<MarketDataRequirement>emptySet()));
  }
}
