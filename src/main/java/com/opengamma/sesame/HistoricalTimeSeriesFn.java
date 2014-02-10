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
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.Result;

/**
 * Function capable of providing a historical time-series bundle.
 */
public interface HistoricalTimeSeriesFn {

  /**
   * Finds the time-series for the curve specification.
   * 
   * @param curve  the curve specification, not null
   * @return the time-series bundle, a failure result if not found
   */
  Result<HistoricalTimeSeriesBundle> getHtsForCurve(CurveSpecification curve);

  /**
   * Finds the time-series for the curve specification at a valuation date.
   * 
   * @param curve  the curve specification, not null
   * @param valuationDate  the valuation date, not null
   * @return the time-series bundle, a failure result if not found
   */
  Result<HistoricalTimeSeriesBundle> getHtsForCurve(CurveSpecification curve, LocalDate valuationDate);

  /**
   * Finds the time-series for the currency pair.
   * 
   * @param currencyPair  the currency pair, not null
   * @return the time-series bundle, a failure result if not found
   */
  Result<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(CurrencyPair currencyPair);

}
