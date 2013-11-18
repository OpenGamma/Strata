/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.money.UnorderedCurrencyPair;

/**
 * Provides FX return series for currency pairs.
 */
public interface FxReturnSeriesProviderFunction {

  /**
   * Get the return series for the supplied currency pair.
   *
   * @param currencyPair the pair to get the return series for, not null
   * @return the return series for the currency pair if available, not null
   */
  FunctionResult<LocalDateDoubleTimeSeries> getReturnSeries(UnorderedCurrencyPair currencyPair);
}
