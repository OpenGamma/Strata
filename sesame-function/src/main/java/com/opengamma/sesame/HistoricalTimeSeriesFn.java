/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.LocalDate;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.Result;

/**
 * Function capable of providing a historical time-series bundle.
 */
public interface HistoricalTimeSeriesFn {

  /**
   * Finds the time-series for the curve specification at a valuation date.
   * 
   * @param env the environment that the fixing requirements are needed for. 
   * @param curve  the curve specification, not null
   * @param endDate  the end date of the time series, inclusive, not null
   * @return the time-series bundle, a failure result if not found
   */
  Result<HistoricalTimeSeriesBundle> getHtsForCurve(Environment env, CurveSpecification curve, LocalDate endDate);

  /**
   * Finds the time-series for the currency pair.
   *
   * @param env the environment that the fixing requirements are needed for. 
   * @param currencyPair the currency pair, not null
   * @param endDate  the end date of the time series, inclusive, not null
   * @return the time-series bundle, a failure result if not found
   */
  Result<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(Environment env, CurrencyPair currencyPair, LocalDate endDate);
  
  /**
   * Finds the fixing requirements for the security.
   * 
   * @param env the environment that the fixing requirements are needed for. 
   * @param security the security to return the fixings for.
   * @return the bundle of fixing requirements.
   */
  Result<HistoricalTimeSeriesBundle> getFixingsForSecurity(Environment env, FinancialSecurity security);

}
