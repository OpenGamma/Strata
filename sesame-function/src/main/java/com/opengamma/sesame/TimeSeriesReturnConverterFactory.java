/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

/**
 * Helper methods to create time series conversions for creating return series.
 */
public final class TimeSeriesReturnConverterFactory {

  /**
   * Private constructor to prevent instantiation.
   */
  private TimeSeriesReturnConverterFactory() {
  }

  enum ConversionType {
    ABSOLUTE, RELATIVE
  }

  //-------------------------------------------------------------------------
  public static TimeSeriesReturnConverter absolute() {
    return new DifferenceOperatorReturnConverter(ConversionType.ABSOLUTE);
  }

  public static TimeSeriesReturnConverter relative() {
    return new DifferenceOperatorReturnConverter(ConversionType.RELATIVE);
  }

  public static TimeSeriesReturnConverter absoluteVolatilityWeighted(double lambda) {
    return new VolatilityWeightedReturnConverter(ConversionType.ABSOLUTE, lambda);
  }

  public static TimeSeriesReturnConverter relativeVolatilityWeighted(double lambda) {
    return new VolatilityWeightedReturnConverter(ConversionType.RELATIVE, lambda);
  }

}
