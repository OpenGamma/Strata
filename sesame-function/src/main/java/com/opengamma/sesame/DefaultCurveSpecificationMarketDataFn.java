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
    Map<ExternalIdBundle, Double> results = new HashMap<>();

    // TODO don't bail out on the first failure, collect all the results
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
