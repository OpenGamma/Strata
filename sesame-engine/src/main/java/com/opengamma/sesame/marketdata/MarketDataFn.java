/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Function providing market data for structured objects.
 * The underlying raw market data comes from the environment argument passed to each method
 */
public interface MarketDataFn {

  /**
   * Returns an FX spot rate for a currency pair.
   *
   * @param env  the function execution environment, not null
   * @param currencyPair  the currency pair, not null
   * @return the rate for the currency pair, not null
   */
  // TODO this should return an object with the rate and pair (FxRate?)
  Result<Double> getFxRate(Environment env, CurrencyPair currencyPair);

  /**
   * Returns the rate for a node on a curve.
   *
   * @param env  the function execution environment, not null
   * @param node  the curve node, not null
   * @return the rate for the node, not null
   */
  // TODO would it be better to pass the whole curve spec/def/whatever for easier scenarios?
  Result<Double> getCurveNodeValue(Environment env, CurveNodeWithIdentifier node);

  /**
   * Returns the rate for the underlying of a node on a curve.
   *
   * @param env  the function execution environment, not null
   * @param node  the curve node, not null
   * @return the rate for the node's underlying, not null
   */
  // TODO would it be better to pass the whole curve spec/def/whatever for easier scenarios?
  Result<Double> getCurveNodeUnderlyingValue(Environment env, PointsCurveNodeWithIdentifier node);

  /**
   * Returns the value of the {@link MarketDataRequirementNames#MARKET_VALUE} field for an ID.
   *
   * @param env  the function execution environment, not null
   * @param id  the external identifier for the values required, not null
   * @return the value of {@link MarketDataRequirementNames#MARKET_VALUE} for the ID, not null
   */
  Result<Double> getMarketValue(Environment env, ExternalIdBundle id);

  /**
   * Returns the value of an arbitrary field of market data for an ID.
   *
   * @param env  the function execution environment, not null
   * @param id  the external identifier for the values required, not null
   * @param fieldName  the name of the field in the market data record, not null
   * @return the value of the field for the ID, not null
   */
  Result<?> getValue(Environment env, ExternalIdBundle id, FieldName fieldName);

}
