/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.math.function.Function1D;
import com.opengamma.timeseries.date.DateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.ArgumentChecker;

/**
 * Converts a spot series to a return series based on the supplied difference operator.
 */
public class DifferenceOperatorReturnConverter implements TimeSeriesReturnConverter {

  /**
   * Calculates an absolute return time series
   */
  private final Function1D<DateDoubleTimeSeries<?>, DateDoubleTimeSeries<?>> _differenceOperator;

  /**
   * Constructor with the desired difference operator.
   *
   * @param differenceOperator the operator to be used, not null
   */
  public DifferenceOperatorReturnConverter(Function1D<DateDoubleTimeSeries<?>, DateDoubleTimeSeries<?>> differenceOperator) {
    _differenceOperator = ArgumentChecker.notNull(differenceOperator, "differenceOperator");
  }

  @Override
  public LocalDateDoubleTimeSeries convert(LocalDateDoubleTimeSeries spotSeries) {
    return (LocalDateDoubleTimeSeries) _differenceOperator.evaluate(spotSeries);
  }

}
