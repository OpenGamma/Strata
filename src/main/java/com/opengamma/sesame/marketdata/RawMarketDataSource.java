/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.time.LocalDateRange;

/**
 * TODO should this have bulk methods?
 * TODO is there any reason to have this and MarketDataFn any more?
 */
/* package */ interface RawMarketDataSource extends DateRangeCalculator {

  // TODO is dataField needed for all impls? what about live?
  MarketDataItem get(ExternalIdBundle idBundle, String dataField);

  // TODO I'm torn about whether to add a type param to MarketDataItem. it would make this method clearer
  // could return MarketDataItem<DateTimeSeries<LocalDate, ?>>
  MarketDataItem get(ExternalIdBundle idBundle, String dataField, LocalDateRange dateRange);
}
