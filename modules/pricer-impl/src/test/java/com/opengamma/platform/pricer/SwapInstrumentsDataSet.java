/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer;

import java.time.LocalDate;

import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Data set of swaps used in the end-to-end tests.
 */
public class SwapInstrumentsDataSet {

  private SwapInstrumentsDataSet() {
  }

  public static final LocalDateDoubleTimeSeries TS_USDLIBOR1M =
      LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2013, 12, 10), 0.00123)
        .put(LocalDate.of(2013, 12, 12), 0.00123)
        .build();
  public static final LocalDateDoubleTimeSeries TS_USDLIBOR3M =
      LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2013, 12, 10), 0.0024185)
        .put(LocalDate.of(2013, 12, 12), 0.0024285)
        .build();
  public static final LocalDateDoubleTimeSeries TS_USDLIBOR6M = 
      LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2013, 12, 10), 0.0030)
        .put(LocalDate.of(2013, 12, 12), 0.0035)
        .build();
  public static final LocalDateDoubleTimeSeries TS_USDON =
      LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2014, 1, 17), 0.0007)
        .put(LocalDate.of(2014, 1, 21), 0.0007)
        .put(LocalDate.of(2014, 1, 22), 0.0007)
        .build();

}
