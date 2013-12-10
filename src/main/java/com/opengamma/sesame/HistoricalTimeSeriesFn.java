/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.result.FunctionResult;

public interface HistoricalTimeSeriesFn {

  FunctionResult<HistoricalTimeSeriesBundle> getHtsForCurve(CurveSpecification curveName);

  FunctionResult<LocalDateDoubleTimeSeries> getHtsForCurrencyPair(CurrencyPair currencyPair);
}
