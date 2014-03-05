/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.time.LocalDateRange;

/**
 * TODO I don't this is needed
 */
public interface HistoricalMarketDataSource {

  MarketDataItem<LocalDateDoubleTimeSeries> get(ExternalIdBundle idBundle, String dataField, LocalDateRange dateRange);
}
