/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.timeseries.util.TimeSeriesDifferenceOperator;
import com.opengamma.analytics.financial.timeseries.util.TimeSeriesPercentageChangeOperator;
import com.opengamma.analytics.financial.timeseries.util.TimeSeriesWeightedVolatilityOperator;

/**
 * Helper methods to create time series conversions for creating return series.
 */
public final class TimeSeriesReturnConverterFactory {

  /**
   * Private constructor to prevent instantiation.
   */
  private TimeSeriesReturnConverterFactory() {
  }

  //-------------------------------------------------------------------------
  public static TimeSeriesReturnConverter absolute() {
    return new DifferenceOperatorReturnConverter(new TimeSeriesDifferenceOperator());
  }

  public static TimeSeriesReturnConverter relative() {
    return new DifferenceOperatorReturnConverter(new TimeSeriesPercentageChangeOperator());
  }

  public static TimeSeriesReturnConverter relativeVolatilityWeighted(double lambda) {
    return new VolatilityWeightedReturnConverter(TimeSeriesWeightedVolatilityOperator.relative(lambda));
  }

  public static TimeSeriesReturnConverter absoluteVolatilityWeighted(double lambda) {
    return new VolatilityWeightedReturnConverter(TimeSeriesWeightedVolatilityOperator.absolute(lambda));
  }

}
