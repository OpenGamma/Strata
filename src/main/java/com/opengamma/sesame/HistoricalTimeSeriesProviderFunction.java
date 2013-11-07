/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;

public interface HistoricalTimeSeriesProviderFunction {

  FunctionResult<HistoricalTimeSeriesBundle> getHtsForCurve(CurveSpecification curveName);
}
