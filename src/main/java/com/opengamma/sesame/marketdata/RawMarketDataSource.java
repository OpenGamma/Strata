/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import org.threeten.bp.LocalDate;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.timeseries.date.DateTimeSeries;
import com.opengamma.util.time.LocalDateRange;

/**
 * TODO not sure if this is a good idea, just trying it on for size
 */
public interface RawMarketDataSource {

  // TODO is dataField needed for all impls? what about live?
  Object get(ExternalIdBundle idBundle, String dataField);

  DateTimeSeries<LocalDate, ?> get(ExternalIdBundle idBundle, String dataField, LocalDateRange dateRange);

}
