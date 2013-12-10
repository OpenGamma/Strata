/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.Period;

import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.FunctionResult;

/**
 * Provides FX return series for currency pairs.
 */
public interface FXReturnSeriesFn {

  /**
   * Get the return series for the supplied currency pair.
   *
   *
   *
   * @param seriesPeriod
   * @param currencyPair the pair to get the return series for, not null
   * @return the return series for the currency pair if available, not null
   */
  FunctionResult<LocalDateDoubleTimeSeries> calculateReturnSeries(Period seriesPeriod, CurrencyPair currencyPair);

  LocalDateDoubleTimeSeries calculateReturnSeries(LocalDateDoubleTimeSeries timeSeries);
}
