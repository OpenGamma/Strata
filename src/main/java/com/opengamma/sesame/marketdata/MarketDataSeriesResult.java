/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;

import org.threeten.bp.LocalDate;

import com.opengamma.sesame.FunctionResult;
import com.opengamma.timeseries.date.DateTimeSeries;

/**
 * TODO TOO MANY GENERICS
 * TODO can the generic param be replaced with something like MarketDataResultMap? MarketDataResults?
 */
public interface MarketDataSeriesResult extends FunctionResult<Map<MarketDataRequirement, MarketDataItem<?>>> {

  <T extends DateTimeSeries<? extends LocalDate, ?>> T getSingleSeries();

  MarketDataStatus getStatus(MarketDataRequirement requirement);

  <T extends DateTimeSeries<? extends LocalDate, ?>> T getSeries(MarketDataRequirement requirement);

  // TODO method to convert to an HTS bundle? would fail if not all LocalDateDoubleTimeSeries
}
