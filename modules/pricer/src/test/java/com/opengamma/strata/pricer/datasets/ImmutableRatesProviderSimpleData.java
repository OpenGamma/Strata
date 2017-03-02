/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.datasets;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;

import java.time.LocalDate;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Simple instances of ImmutableRateProvider to be used in tests.
 */
public class ImmutableRatesProviderSimpleData {
  
  public static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 16);  

  public static final ImmutableRatesProvider IMM_PROV_EUR_NOFIX;
  public static final ImmutableRatesProvider IMM_PROV_EUR_FIX;
  static {
    CurveInterpolator interp = CurveInterpolators.DOUBLE_QUADRATIC;
    DoubleArray time_eur = DoubleArray.of(0.0, 0.1, 0.25, 0.5, 0.75, 1.0, 2.0);
    DoubleArray rate_eur = DoubleArray.of(0.0160, 0.0165, 0.0155, 0.0155, 0.0155, 0.0150, 0.0140);
    InterpolatedNodalCurve dscCurve =
        InterpolatedNodalCurve.of(Curves.zeroRates("EUR-Discount", ACT_365F), time_eur, rate_eur, interp);
    DoubleArray time_index = DoubleArray.of(0.0, 0.25, 0.5, 1.0);
    DoubleArray rate_index = DoubleArray.of(0.0180, 0.0180, 0.0175, 0.0165);
    InterpolatedNodalCurve indexCurve =
        InterpolatedNodalCurve.of(Curves.zeroRates("EUR-EURIBOR6M", ACT_365F), time_index, rate_index, interp);
    IMM_PROV_EUR_NOFIX = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(EUR, dscCurve)
        .iborIndexCurve(EUR_EURIBOR_6M, indexCurve)
        .build();
    LocalDateDoubleTimeSeries tsE6 = LocalDateDoubleTimeSeries.builder()
        .put(VAL_DATE, 0.012345).build();
    IMM_PROV_EUR_FIX = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(EUR, dscCurve)
        .iborIndexCurve(EUR_EURIBOR_6M, indexCurve, tsE6)
        .build();
  }

}
