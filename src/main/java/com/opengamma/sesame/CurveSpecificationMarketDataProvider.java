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
import com.opengamma.sesame.marketdata.MarketDataFunctionResult;
import com.opengamma.sesame.marketdata.MarketDataProviderFunction;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;

public class CurveSpecificationMarketDataProvider implements CurveSpecificationMarketDataProviderFunction {

  private final MarketDataProviderFunction _marketDataProviderFunction;

  public CurveSpecificationMarketDataProvider(MarketDataProviderFunction marketDataProviderFunction) {
    _marketDataProviderFunction = marketDataProviderFunction;
  }

  @Override
  public MarketDataFunctionResult requestData(CurveSpecification curveSpecification) {

    final Set<MarketDataRequirement> requirements = new HashSet<>();
    for (final CurveNodeWithIdentifier id : curveSpecification.getNodes()) {
      requirements.addAll(MarketDataRequirementFactory.of(id));
    }
    return _marketDataProviderFunction.requestData(requirements);
  }
}
