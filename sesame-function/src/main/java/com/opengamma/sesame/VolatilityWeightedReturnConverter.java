/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.timeseries.util.TimeSeriesRelativeWeightedDifferenceOperator;
import com.opengamma.analytics.financial.timeseries.util.TimeSeriesWeightedVolatilityOperator;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;

/**
 * Produces a volatility weighted return series from a spot rate series
 * using a fixed lambda value.
 */
public class VolatilityWeightedReturnConverter implements TimeSeriesReturnConverter {

  private static final TimeSeriesRelativeWeightedDifferenceOperator RELATIVE_WEIGHTED_DIFFERENCE =
      new TimeSeriesRelativeWeightedDifferenceOperator();

  private final TimeSeriesWeightedVolatilityOperator _weightedVolatilityOperator;

  public VolatilityWeightedReturnConverter(TimeSeriesWeightedVolatilityOperator weightedVolatilityOperator) {
    _weightedVolatilityOperator = weightedVolatilityOperator;
  }

  @Override
  public LocalDateDoubleTimeSeries convert(LocalDateDoubleTimeSeries spotSeries) {

    final LocalDateDoubleTimeSeries weightedVolSeries =
        (LocalDateDoubleTimeSeries) _weightedVolatilityOperator.evaluate(spotSeries);
    return (LocalDateDoubleTimeSeries) RELATIVE_WEIGHTED_DIFFERENCE.evaluate(spotSeries, weightedVolSeries);
  }

}
