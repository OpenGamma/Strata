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

public interface HistoricalTimeSeriesFn {

  Result<HistoricalTimeSeriesBundle> getHtsForCurve(CurveSpecification curveName);

  Result<HistoricalTimeSeriesBundle> getHtsForCurve(CurveSpecification curve, LocalDate valuationDate);

  Result<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(CurrencyPair currencyPair);
}
