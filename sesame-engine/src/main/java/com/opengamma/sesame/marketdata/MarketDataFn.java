/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Function providing market data to clients.
 * TODO use Result [SSM-162]
 */
public interface MarketDataFn {

  // TODO this should return an object with the rate and pair (FxRate?)
  Result<Double> getFxRate(Environment env, CurrencyPair currencyPair);

  // TODO would it be better to pass the whole curve spec/def/whatever for easier scenarios?
  Result<Double> getCurveNodeValue(Environment env, CurveNodeWithIdentifier node);

  Result<Double> getCurveNodeUnderlyingValue(Environment env, PointsCurveNodeWithIdentifier node);

  Result<Double> getMarketValue(Environment env, ExternalIdBundle id);

  Result<?> getValue(Environment env, ExternalIdBundle id, FieldName fieldName);
}
