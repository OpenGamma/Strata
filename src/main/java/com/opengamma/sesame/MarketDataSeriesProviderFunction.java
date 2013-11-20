/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.util.time.LocalDateRange;

/**
 * TODO this is highly provisional
 * TODO should this return MarketDataFunctionResult or should there be a new type for time series of data?
 * MarketDataSeriesFunctionResult? containing MarketDataSeriesValue?
 * TODO can this reliably hide the dataSource, dataProvider and dataField from functions?
 * the MarketDataRequirement for a data type should provide dataField
 * can the other two be configured at a higher level?
 * current engine uses HistoricalTimeSeriesRating. will probably do us for now
 */
public interface MarketDataSeriesProviderFunction {

  MarketDataFunctionResult requestData(MarketDataRequirement requirement, LocalDateRange range);

  MarketDataFunctionResult requestData(Set<MarketDataRequirement> requirements, LocalDateRange range);

}
