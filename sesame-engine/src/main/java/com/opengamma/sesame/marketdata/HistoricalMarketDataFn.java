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
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 * Function providing time series of market data for structured objects.
 * The underlying raw market data comes from the environment argument passed to each method
 */
public interface HistoricalMarketDataFn {

  /**
   * Returns a time series of FX spot rates for a currency pair.
   *
   * @param env  the function execution environment, not null
   * @param currencyPair  the currency pair, not null
   * @param dateRange  the range of dates to return, not null
   * @return the rates for the currency pair, not null
   */
  // TODO this should return an object with the rate and pair (FxRate?)
  Result<LocalDateDoubleTimeSeries> getFxRates(Environment env, CurrencyPair currencyPair, LocalDateRange dateRange);

  /**
   * Returns a time series of the rate for a node on a curve.
   *
   * @param env  the function execution environment, not null
   * @param node  the curve node, not null
   * @param dateRange  the range of dates to return, not null
   * @return the rate for the node, not null
   */
  // TODO this needs to return a time series bundle
  Result<LocalDateDoubleTimeSeries> getCurveNodeValues(Environment env, CurveNodeWithIdentifier node, LocalDateRange dateRange);

  /**
   * Returns a time series of the rate for the underlying of a node on a curve.
   *
   * @param env  the function execution environment, not null
   * @param node  the curve node, not null
   * @param dateRange  the range of dates to return, not null
   * @return the rate for the node's underlying, not null
   */
  // TODO this needs to return a time series bundle
  Result<LocalDateDoubleTimeSeries> getCurveNodeUnderlyingValue(Environment env, PointsCurveNodeWithIdentifier node, LocalDateRange dateRange);

  /**
   * Returns a time series of the value of the {@link MarketDataRequirementNames#MARKET_VALUE}
   * field for an external identifier.
   *
   * @param env  the function execution environment, not null
   * @param id  the external identifier for the values required, not null
   * @param dateRange  the range of dates to return, not null
   * @return the value of {@link MarketDataRequirementNames#MARKET_VALUE} for the ID, not null
   */
  Result<LocalDateDoubleTimeSeries> getMarketValues(Environment env, ExternalIdBundle id, LocalDateRange dateRange);

  /**
   * Returns a time series of the value of an arbitrary field of market data for an external identifier.
   *
   * @param env  the function execution environment, not null
   * @param id  the external identifier for the values required, not null
   * @param fieldName  the name of the field in the market data record, not null
   * @param dateRange  the range of dates to return, not null
   * @return the value of the field for the ID, not null
   */
  Result<LocalDateDoubleTimeSeries> getValues(Environment env, ExternalIdBundle id, FieldName fieldName, LocalDateRange dateRange);

}
