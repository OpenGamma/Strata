/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.pnl;

import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.sesame.Environment;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.Result;

/**
 * Converts historical PnL numbers to use today's fx rate. Performs the
 * calculation TodayPnL(T) = PnL(T) * FX(T) / FX(today).
 */
public interface HistoricalPnLFXConverterFn {

  /**
   * Converts the passed series into today's FX rates.
   * @param env the environment
   * @param currencyPair the currency pair to use
   * @param hts the hts to convert
   * @return the converter hts
   */
  Result<LocalDateDoubleTimeSeries> convertToSpotRate(Environment env, CurrencyPair currencyPair, LocalDateDoubleTimeSeries hts);

}
