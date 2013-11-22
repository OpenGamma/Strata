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
 * TODO can the generic param be replaced with something like MarketDataResultMap? MarketDataResults?
 */
public interface MarketDataSeriesResult extends FunctionResult<Map<MarketDataRequirement, MarketDataItem<?>>> {

  // TODO is everything actually a LocalDataDoubleTimeSeries? or do we need this generality
  <T extends DateTimeSeries<? extends LocalDate, ?>> T getSingleSeries();

  MarketDataStatus getStatus(MarketDataRequirement requirement);

    // TODO is everything actually a LocalDataDoubleTimeSeries? or do we need this generality
  <T extends DateTimeSeries<? extends LocalDate, ?>> T getSeries(MarketDataRequirement requirement);

  // TODO method to convert to an HTS bundle? would fail if not all LocalDateDoubleTimeSeries
}
