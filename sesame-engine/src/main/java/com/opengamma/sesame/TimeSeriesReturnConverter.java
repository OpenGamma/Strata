/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;

/**
 * Provides a conversion from a spot-rate time-series to an FX return series.
 */
public interface TimeSeriesReturnConverter {

  /**
   * Converts the provided FX spot rate series to an FX return series.
   *
   * @param spotSeries  the series to be converted, not null
   * @return a return series
   */
  LocalDateDoubleTimeSeries convert(LocalDateDoubleTimeSeries spotSeries);

}
