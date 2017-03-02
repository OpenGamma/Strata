/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.PriceIndices.GB_RPIX;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.InflationRateSensitivity;
import com.opengamma.strata.product.rate.InflationInterpolatedRateComputation;

/**
 * Test {@link ForwardInflationInterpolatedRateComputationFn}.
 */
@Test
public class ForwardInflationInterpolatedRateComputationFnTest {

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 10);

  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 4); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2016, 1, 5); // Accrual dates irrelevant for the rate
  private static final YearMonth REF_START_MONTH = YearMonth.of(2014, 10);
  private static final YearMonth REF_START_MONTH_INTERP = YearMonth.of(2014, 11);
  private static final YearMonth REF_END_MONTH = YearMonth.of(2015, 10);
  private static final YearMonth REF_END_MONTH_INTERP = YearMonth.of(2015, 11);
  private static final double RATE_START = 317.0;
  private static final double RATE_START_INTERP = 325.0;
  private static final double RATE_END = 344.0;
  private static final double RATE_END_INTERP = 349.0;
  private static final double WEIGHT = 0.5;

  private static final double EPS = 1.0e-12;
  private static final double EPS_FD = 1.0e-4;

  //-------------------------------------------------------------------------
  public void test_rate() {
    ImmutableRatesProvider prov = createProvider(RATE_START, RATE_START_INTERP, RATE_END, RATE_END_INTERP);

    InflationInterpolatedRateComputation ro =
        InflationInterpolatedRateComputation.of(GB_RPIX, REF_START_MONTH, REF_END_MONTH, WEIGHT);
    ForwardInflationInterpolatedRateComputationFn obsFn = ForwardInflationInterpolatedRateComputationFn.DEFAULT;

    double rateExpected = (WEIGHT * RATE_END + (1.0 - WEIGHT) * RATE_END_INTERP) /
        (WEIGHT * RATE_START + (1.0 - WEIGHT) * RATE_START_INTERP) - 1.0;
    assertEquals(obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov), rateExpected, EPS);

    // explain
    ExplainMapBuilder builder = ExplainMap.builder();
    assertEquals(obsFn.explainRate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov, builder), rateExpected, EPS);

    ExplainMap built = builder.build();
    assertEquals(built.get(ExplainKey.OBSERVATIONS).isPresent(), true);
    assertEquals(built.get(ExplainKey.OBSERVATIONS).get().size(), 4);
    ExplainMap explain0 = built.get(ExplainKey.OBSERVATIONS).get().get(0);
    assertEquals(explain0.get(ExplainKey.FIXING_DATE), Optional.of(REF_START_MONTH.atEndOfMonth()));
    assertEquals(explain0.get(ExplainKey.INDEX), Optional.of(GB_RPIX));
    assertEquals(explain0.get(ExplainKey.INDEX_VALUE), Optional.of(RATE_START));
    assertEquals(explain0.get(ExplainKey.WEIGHT), Optional.of(WEIGHT));
    ExplainMap explain1 = built.get(ExplainKey.OBSERVATIONS).get().get(1);
    assertEquals(explain1.get(ExplainKey.FIXING_DATE), Optional.of(REF_START_MONTH_INTERP.atEndOfMonth()));
    assertEquals(explain1.get(ExplainKey.INDEX), Optional.of(GB_RPIX));
    assertEquals(explain1.get(ExplainKey.INDEX_VALUE), Optional.of(RATE_START_INTERP));
    assertEquals(explain1.get(ExplainKey.WEIGHT), Optional.of(1d - WEIGHT));
    ExplainMap explain2 = built.get(ExplainKey.OBSERVATIONS).get().get(2);
    assertEquals(explain2.get(ExplainKey.FIXING_DATE), Optional.of(REF_END_MONTH.atEndOfMonth()));
    assertEquals(explain2.get(ExplainKey.INDEX), Optional.of(GB_RPIX));
    assertEquals(explain2.get(ExplainKey.INDEX_VALUE), Optional.of(RATE_END));
    assertEquals(explain2.get(ExplainKey.WEIGHT), Optional.of(WEIGHT));
    ExplainMap explain3 = built.get(ExplainKey.OBSERVATIONS).get().get(3);
    assertEquals(explain3.get(ExplainKey.FIXING_DATE), Optional.of(REF_END_MONTH_INTERP.atEndOfMonth()));
    assertEquals(explain3.get(ExplainKey.INDEX), Optional.of(GB_RPIX));
    assertEquals(explain3.get(ExplainKey.INDEX_VALUE), Optional.of(RATE_END_INTERP));
    assertEquals(explain3.get(ExplainKey.WEIGHT), Optional.of(1d - WEIGHT));
    assertEquals(built.get(ExplainKey.COMBINED_RATE).get().doubleValue(), rateExpected, EPS);
  }

  //-------------------------------------------------------------------------
  public void test_rateSensitivity() {
    ImmutableRatesProvider prov = createProvider(RATE_START, RATE_START_INTERP, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provSrtUp = createProvider(RATE_START + EPS_FD, RATE_START_INTERP, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provSrtDw = createProvider(RATE_START - EPS_FD, RATE_START_INTERP, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provSrtIntUp = createProvider(RATE_START, RATE_START_INTERP + EPS_FD, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provSrtIntDw = createProvider(RATE_START, RATE_START_INTERP - EPS_FD, RATE_END, RATE_END_INTERP);
    ImmutableRatesProvider provEndUp = createProvider(RATE_START, RATE_START_INTERP, RATE_END + EPS_FD, RATE_END_INTERP);
    ImmutableRatesProvider provEndDw = createProvider(RATE_START, RATE_START_INTERP, RATE_END - EPS_FD, RATE_END_INTERP);
    ImmutableRatesProvider provEndIntUp = createProvider(RATE_START, RATE_START_INTERP, RATE_END, RATE_END_INTERP + EPS_FD);
    ImmutableRatesProvider provEndIntDw = createProvider(RATE_START, RATE_START_INTERP, RATE_END, RATE_END_INTERP - EPS_FD);

    InflationInterpolatedRateComputation ro =
        InflationInterpolatedRateComputation.of(GB_RPIX, REF_START_MONTH, REF_END_MONTH, WEIGHT);
    ForwardInflationInterpolatedRateComputationFn obsFn = ForwardInflationInterpolatedRateComputationFn.DEFAULT;

    double rateSrtUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provSrtUp);
    double rateSrtDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provSrtDw);
    double rateSrtIntUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provSrtIntUp);
    double rateSrtIntDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provSrtIntDw);
    double rateEndUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndUp);
    double rateEndDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndDw);
    double rateEndIntUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndIntUp);
    double rateEndIntDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, provEndIntDw);

    PointSensitivityBuilder sensSrt = InflationRateSensitivity.of(
        PriceIndexObservation.of(GB_RPIX, REF_START_MONTH), 0.5 * (rateSrtUp - rateSrtDw) / EPS_FD);
    PointSensitivityBuilder sensSrtInt = InflationRateSensitivity.of(
        PriceIndexObservation.of(GB_RPIX, REF_START_MONTH_INTERP), 0.5 * (rateSrtIntUp - rateSrtIntDw) / EPS_FD);
    PointSensitivityBuilder sensEnd = InflationRateSensitivity.of(
        PriceIndexObservation.of(GB_RPIX, REF_END_MONTH), 0.5 * (rateEndUp - rateEndDw) / EPS_FD);
    PointSensitivityBuilder sensEndInt = InflationRateSensitivity.of(
        PriceIndexObservation.of(GB_RPIX, REF_END_MONTH_INTERP), 0.5 * (rateEndIntUp - rateEndIntDw) / EPS_FD);
    PointSensitivityBuilder sensiExpected =
        sensSrt.combinedWith(sensSrtInt).combinedWith(sensEnd).combinedWith(sensEndInt);

    PointSensitivityBuilder sensiComputed =
        obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
    assertTrue(sensiComputed.build().normalized().equalWithTolerance(sensiExpected.build().normalized(), EPS_FD));
  }

  private ImmutableRatesProvider createProvider(
      double rateStart,
      double rateStartInterp,
      double rateEnd,
      double rateEndInterp) {

    LocalDateDoubleTimeSeries timeSeries = LocalDateDoubleTimeSeries.of(VAL_DATE.with(lastDayOfMonth()), 300);
    InterpolatedNodalCurve curve = InterpolatedNodalCurve.of(
        Curves.prices("GB-RPIX"),
        DoubleArray.of(4, 5, 16, 17),
        DoubleArray.of(rateStart, rateStartInterp, rateEnd, rateEndInterp),
        INTERPOLATOR);
    return ImmutableRatesProvider.builder(VAL_DATE)
        .priceIndexCurve(GB_RPIX, curve)
        .timeSeries(GB_RPIX, timeSeries)
        .build();
  }

}
