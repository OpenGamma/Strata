/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 * Function capable of providing an FX return series for currency pairs.
 */
public interface FXReturnSeriesFn {

  /**
   * Get the return series for the supplied currency pair.
   *
   * @param dateRange  the date range of the series, not null
   * @param currencyPair  the pair to get the return series for, not null
   * @return the return series for the currency pair, a failure result if not found
   */
  Result<LocalDateDoubleTimeSeries> calculateReturnSeries(Environment env, LocalDateRange dateRange, CurrencyPair currencyPair);

  /**
   * Calculates the return series based on another time-series.
   * 
   * @param timeSeries  the input time-series, not null
   * @return the return series, a failure result if not found
   */
  LocalDateDoubleTimeSeries calculateReturnSeries(Environment env, LocalDateDoubleTimeSeries timeSeries);

}
