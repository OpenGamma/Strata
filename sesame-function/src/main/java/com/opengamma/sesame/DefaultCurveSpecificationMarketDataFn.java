/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultGenerator;

/**
 * Function implementation that provides market data for a curve specification.
 */
public class DefaultCurveSpecificationMarketDataFn implements CurveSpecificationMarketDataFn {

  /**
   * The market data function.
   */
  private final MarketDataFn _marketDataFn;

  public DefaultCurveSpecificationMarketDataFn(MarketDataFn marketDataFn) {
    _marketDataFn = ArgumentChecker.notNull(marketDataFn, "marketDataFn");
  }

  @Override
  public Result<Map<ExternalIdBundle, Double>> requestData(Environment env, CurveSpecification curveSpecification) {
    // TODO we're looping over the set of nodes twice, do it in one go? not sure that's possible
    /*
    Set<MarketDataRequirement> requirements = new HashSet<>();
    for (CurveNodeWithIdentifier id : curveSpecification.getNodes()) {
      MarketDataRequirement fwdReq = MarketDataRequirementFactory.of(id);
      requirements.add(fwdReq);
      if (id instanceof PointsCurveNodeWithIdentifier) {
        PointsCurveNodeWithIdentifier node = (PointsCurveNodeWithIdentifier) id;
        requirements.add(new CurveNodeMarketDataRequirement(node.getUnderlyingIdentifier(), node.getUnderlyingDataField()));
      }
    }
    Result<MarketDataValues> result = marketDataFn.requestData(requirements);
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
    return success(new MarketDataValues(items, Collections.<MarketDataRequirement>emptySet()));*/

    Map<ExternalIdBundle, Double> results = new HashMap<>();

    for (CurveNodeWithIdentifier node : curveSpecification.getNodes()) {
      if (node instanceof PointsCurveNodeWithIdentifier) {
        PointsCurveNodeWithIdentifier pointsNode = (PointsCurveNodeWithIdentifier) node;
        MarketDataItem<Double> fwdItem = _marketDataFn.getCurveNodeValue(env, node);
        MarketDataItem<Double> spotItem = _marketDataFn.getCurveNodeUnderlyingValue(env, pointsNode);

        if (!fwdItem.isAvailable()) {
          return ResultGenerator.failure(FailureStatus.MISSING_DATA, "No data for {}", node);
        }
        if (!spotItem.isAvailable()) {
          return ResultGenerator.failure(FailureStatus.MISSING_DATA, "No data for {}", pointsNode);
        }
        results.put(node.getIdentifier().toBundle(), fwdItem.getValue() + spotItem.getValue());
      } else {
        MarketDataItem<Double> fwdItem = _marketDataFn.getCurveNodeValue(env, node);

        if (!fwdItem.isAvailable()) {
          return ResultGenerator.failure(FailureStatus.MISSING_DATA, "No data for {}", node);
        } else {
          results.put(node.getIdentifier().toBundle(), fwdItem.getValue());
        }
      }
    }
    return ResultGenerator.success(results);
  }
}
