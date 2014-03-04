/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;

/**
 * Function providing market data to clients.
 */
public interface MarketDataFn {

  // TODO this should return an object with the rate and pair (FxRate?)
  MarketDataItem<Double> getFxRate(Environment env, CurrencyPair currencyPair);

  MarketDataItem<Double> getCurveNodeValue(Environment env, CurveNodeWithIdentifier node);

  MarketDataItem<Double> getMarketValue(Environment env, ExternalIdBundle id);
}
