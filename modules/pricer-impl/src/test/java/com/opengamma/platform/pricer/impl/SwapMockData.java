/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import java.io.Serializable;
import java.time.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.interestrate.datasets.StandardDataSetsMulticurveUSD;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.basics.index.IborIndices;
import com.opengamma.basics.index.Index;
import com.opengamma.basics.index.OvernightIndices;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.util.tuple.Pair;

/**
 * Mocks and examples for swaps.
 */
public class SwapMockData {

  private SwapMockData() {
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
  public static final ImmutableMap<Index, LocalDateDoubleTimeSeries> TIME_SERIES = ImmutableMap.of(
      IborIndices.USD_LIBOR_1M, TS_USDLIBOR1M,
      IborIndices.USD_LIBOR_3M, TS_USDLIBOR3M,
      IborIndices.USD_LIBOR_6M, TS_USDLIBOR6M,
      OvernightIndices.USD_FED_FUND, TS_USDON);
  public static final ImmutableMap<Index, LocalDateDoubleTimeSeries> TIME_SERIES_ON = ImmutableMap.of(
      OvernightIndices.USD_FED_FUND, TS_USDON);

  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_OIS_PAIR =
      StandardDataSetsMulticurveUSD.getCurvesUSDOisL1L3L6();
  public static final MulticurveProviderDiscount MULTICURVE_OIS = MULTICURVE_OIS_PAIR.getFirst();

  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_OIS2_PAIR =
      StandardDataSetsMulticurveUSD.getCurvesUSDOisL1L3L6();
  public static final MulticurveProviderDiscount MULTICURVE_OIS2 = MULTICURVE_OIS2_PAIR.getFirst();

  public static final MulticurveProviderDiscount MULTICURVE_SERIALIZABLE = new SerMulticurveProviderDiscount();

  public static class SerMulticurveProviderDiscount extends MulticurveProviderDiscount implements Serializable {
    private static final long serialVersionUID = 1L;
  }

}
