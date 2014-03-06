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

/**
 * Function providing market data to clients.
 * TODO use Result [SSM-162]
 */
public interface MarketDataFn {

  // TODO this should return an object with the rate and pair (FxRate?)
  MarketDataItem<Double> getFxRate(Environment env, CurrencyPair currencyPair);

  // TODO would it be better to pass the whole curve spec/def/whatever for easier scenarios?
  MarketDataItem<Double> getCurveNodeValue(Environment env, CurveNodeWithIdentifier node);

  MarketDataItem<Double> getCurveNodeUnderlyingValue(Environment env, PointsCurveNodeWithIdentifier node);

  MarketDataItem<Double> getMarketValue(Environment env, ExternalIdBundle id);

  MarketDataItem<?> getValue(Environment env, ExternalIdBundle id, FieldName fieldName);
}
