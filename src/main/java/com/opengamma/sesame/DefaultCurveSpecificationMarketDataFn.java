/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.HashSet;
import java.util.Set;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.result.FunctionResult;

public class DefaultCurveSpecificationMarketDataFn implements CurveSpecificationMarketDataFn {

  private final MarketDataFn _marketDataFn;

  public DefaultCurveSpecificationMarketDataFn(MarketDataFn marketDataFn) {
    _marketDataFn = marketDataFn;
  }

  @Override
  public FunctionResult<MarketDataValues> requestData(CurveSpecification curveSpecification) {

    final Set<MarketDataRequirement> requirements = new HashSet<>();
    for (final CurveNodeWithIdentifier id : curveSpecification.getNodes()) {
      requirements.addAll(MarketDataRequirementFactory.of(id));
    }
    return _marketDataFn.requestData(requirements);
  }
}
