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
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.time.LocalDateRange;

/**
 * Function providing market data to clients. When data is requested a value is returned containing the status
 * and possibly the value for every item that was been requested.
 */
public interface HistoricalMarketDataFn {

  MarketDataItem<LocalDateDoubleTimeSeries> getFxRates(Environment env, CurrencyPair currencyPair, LocalDateRange dateRange);

  MarketDataItem<LocalDateDoubleTimeSeries> getCurveNodeValues(Environment env, CurveNodeWithIdentifier node, LocalDateRange dateRange);

  MarketDataItem<LocalDateDoubleTimeSeries> getCurveNodeUnderlyingValue(Environment env, PointsCurveNodeWithIdentifier node, LocalDateRange dateRange);

  MarketDataItem<LocalDateDoubleTimeSeries> getMarketValues(Environment env, ExternalIdBundle id, LocalDateRange dateRange);

  MarketDataItem<LocalDateDoubleTimeSeries> getValues(Environment env, ExternalIdBundle id, FieldName fieldName, LocalDateRange dateRange);
}
