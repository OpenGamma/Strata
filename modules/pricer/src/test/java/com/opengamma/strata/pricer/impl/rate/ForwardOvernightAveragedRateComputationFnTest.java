/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.OvernightIndices.CHF_TOIS;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.OvernightRateSensitivity;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.rate.OvernightAveragedRateComputation;

/**
 * Test {@link ForwardOvernightAveragedRateComputationFn}.
 */
@Test
public class ForwardOvernightAveragedRateComputationFnTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 1); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2015, 1, 1); // Accrual dates irrelevant for the rate
  private static final LocalDate START_DATE = date(2015, 1, 8);
  private static final LocalDate END_DATE = date(2015, 1, 15); // 1w only to decrease data
  private static final LocalDate[] FIXING_DATES = new LocalDate[] {
      date(2015, 1, 7), date(2015, 1, 8), date(2015, 1, 9),
      date(2015, 1, 12), date(2015, 1, 13), date(2015, 1, 14), date(2015, 1, 15)};
  private static final OvernightIndexObservation[] USD_OBS = new OvernightIndexObservation[] {
      OvernightIndexObservation.of(USD_FED_FUND, date(2015, 1, 7), REF_DATA),
      OvernightIndexObservation.of(USD_FED_FUND, date(2015, 1, 8), REF_DATA),
      OvernightIndexObservation.of(USD_FED_FUND, date(2015, 1, 9), REF_DATA),
      OvernightIndexObservation.of(USD_FED_FUND, date(2015, 1, 12), REF_DATA),
      OvernightIndexObservation.of(USD_FED_FUND, date(2015, 1, 13), REF_DATA),
      OvernightIndexObservation.of(USD_FED_FUND, date(2015, 1, 14), REF_DATA),
      OvernightIndexObservation.of(USD_FED_FUND, date(2015, 1, 15), REF_DATA)};
  private static final OvernightIndexObservation[] GBP_OBS = new OvernightIndexObservation[] {
      OvernightIndexObservation.of(GBP_SONIA, date(2015, 1, 7), REF_DATA),
      OvernightIndexObservation.of(GBP_SONIA, date(2015, 1, 8), REF_DATA),
      OvernightIndexObservation.of(GBP_SONIA, date(2015, 1, 9), REF_DATA),
      OvernightIndexObservation.of(GBP_SONIA, date(2015, 1, 12), REF_DATA),
      OvernightIndexObservation.of(GBP_SONIA, date(2015, 1, 13), REF_DATA),
      OvernightIndexObservation.of(GBP_SONIA, date(2015, 1, 14), REF_DATA),
      OvernightIndexObservation.of(GBP_SONIA, date(2015, 1, 15), REF_DATA)};
  private static final OvernightIndexObservation[] CHF_OBS = new OvernightIndexObservation[] {
      OvernightIndexObservation.of(CHF_TOIS, date(2015, 1, 7), REF_DATA),
      OvernightIndexObservation.of(CHF_TOIS, date(2015, 1, 8), REF_DATA),
      OvernightIndexObservation.of(CHF_TOIS, date(2015, 1, 9), REF_DATA),
      OvernightIndexObservation.of(CHF_TOIS, date(2015, 1, 12), REF_DATA),
      OvernightIndexObservation.of(CHF_TOIS, date(2015, 1, 13), REF_DATA),
      OvernightIndexObservation.of(CHF_TOIS, date(2015, 1, 14), REF_DATA),
      OvernightIndexObservation.of(CHF_TOIS, date(2015, 1, 15), REF_DATA)};
  private static final double[] FIXING_RATES = {
      0.0012, 0.0023, 0.0034,
      0.0045, 0.0056, 0.0067, 0.0078};
  private static final double TOLERANCE_RATE = 1.0E-10;
  private static final double EPS_FD = 1.0E-7;

  //-------------------------------------------------------------------------
  /** Test for the case where publication lag=1, effective offset=0 (USD conventions) and no cutoff period. */
  public void rateFedFundNoCutOff() {
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    for (int i = 0; i < USD_OBS.length; i++) {
      when(mockRates.rate(USD_OBS[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateComputation ro =
        OvernightAveragedRateComputation.of(USD_FED_FUND, START_DATE, END_DATE, 0, REF_DATA);
    // Accrual dates = fixing dates
    ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5; // Fixing in the observation period are from 1 to 5 (inclusive)
    for (int i = 1; i <= indexLast; i++) {
      LocalDate endDate = USD_OBS[i].getMaturityDate();
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);

    // explain
    ExplainMapBuilder builder = ExplainMap.builder();
    double explainedRate = obsFn.explainRate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv, builder);
    assertEquals(explainedRate, rateExpected, TOLERANCE_RATE);

    ExplainMap built = builder.build();
    assertEquals(built.get(ExplainKey.OBSERVATIONS).isPresent(), false);
    assertEquals(built.get(ExplainKey.COMBINED_RATE).get().doubleValue(), rateExpected, TOLERANCE_RATE);
  }

  /** Test against FD approximation for the case where publication lag=1, effective offset=0 (USD conventions) and 
   * no cutoff period. Note that all the rates are bumped here, i.e., all the rates are treated as forward rates.*/
  public void rateFedFundNoCutOffSensitivity() {
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    for (int i = 0; i < USD_OBS.length; i++) {
      when(mockRates.rate(USD_OBS[i])).thenReturn(FIXING_RATES[i]);
      LocalDate fixingEndDate = USD_OBS[i].getMaturityDate();
      OvernightRateSensitivity sensitivity = OvernightRateSensitivity.ofPeriod(USD_OBS[i],
          fixingEndDate, USD_FED_FUND.getCurrency(), 1d);
      when(mockRates.ratePointSensitivity(USD_OBS[i])).thenReturn(sensitivity);
    }
    OvernightAveragedRateComputation ro =
        OvernightAveragedRateComputation.of(USD_FED_FUND, START_DATE, END_DATE, 0, REF_DATA);
    ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;
    PointSensitivityBuilder sensitivityBuilderComputed = obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE,
        DUMMY_ACCRUAL_END_DATE, simpleProv);
    PointSensitivities sensitivityComputed = sensitivityBuilderComputed.build().normalized();
    Double[] sensitivityExpected = computedSensitivityFD(ro, USD_FED_FUND, USD_OBS);
    assertEquals(sensitivityComputed.getSensitivities().size(), sensitivityExpected.length);
    for (int i = 0; i < sensitivityExpected.length; ++i) {
      assertEquals(sensitivityComputed.getSensitivities().get(i).getSensitivity(), sensitivityExpected[i], EPS_FD);
    }
  }

  /** Test for the case where publication lag=1, effective offset=0 (USD conventions) and cutoff=2 (FedFund swaps). */
  public void rateFedFund() {
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    for (int i = 0; i < USD_OBS.length; i++) {
      when(mockRates.rate(USD_OBS[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateComputation ro =
        OvernightAveragedRateComputation.of(USD_FED_FUND, START_DATE, END_DATE, 2, REF_DATA);
    ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5; // Fixing in the observation period are from 1 to 5 (inclusive), but last is modified by cut-off
    for (int i = 1; i <= indexLast - 1; i++) {
      LocalDate endDate = USD_OBS[i].getMaturityDate();
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    // CutOff
    LocalDate endDate = USD_OBS[indexLast].getMaturityDate();
    double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[indexLast], endDate);
    accrualFactorTotal += af;
    accruedRate += FIXING_RATES[indexLast - 1] * af;
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }

  /** Test against FD approximation for the case where publication lag=1, effective offset=0 (USD conventions) and 
   * cutoff=2 (FedFund swaps). 
   * Note that all the rates are bumped here, i.e., all the rates are treated as forward rates. */
  public void rateFedFundSensitivity() {
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    for (int i = 0; i < USD_OBS.length; i++) {
      when(mockRates.rate(USD_OBS[i])).thenReturn(FIXING_RATES[i]);
      OvernightRateSensitivity sensitivity = OvernightRateSensitivity.of(USD_OBS[i], USD_FED_FUND.getCurrency(), 1d);
      when(mockRates.ratePointSensitivity(USD_OBS[i])).thenReturn(sensitivity);
    }
    OvernightAveragedRateComputation ro =
        OvernightAveragedRateComputation.of(USD_FED_FUND, START_DATE, END_DATE, 2, REF_DATA);
    ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;

    PointSensitivityBuilder sensitivityBuilderComputed = obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE,
        DUMMY_ACCRUAL_END_DATE, simpleProv);
    PointSensitivities sensitivityComputed = sensitivityBuilderComputed.build().normalized();
    Double[] sensitivityExpected = computedSensitivityFD(ro, USD_FED_FUND, USD_OBS);
    assertEquals(sensitivityComputed.getSensitivities().size(), sensitivityExpected.length);
    for (int i = 0; i < sensitivityExpected.length; ++i) {
      assertEquals(sensitivityComputed.getSensitivities().get(i).getSensitivity(), sensitivityExpected[i], EPS_FD);
    }
  }

  /**
   * Test for the case where publication lag=0, effective offset=1 (CHF conventions) and no cutoff period.
   * The arithmetic average coupons are used mainly in USD. This test is more for completeness than a real case.
   */
  public void rateChfNoCutOff() {
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(CHF_TOIS);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    for (int i = 0; i < CHF_OBS.length; i++) {
      when(mockRates.rate(CHF_OBS[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateComputation ro =
        OvernightAveragedRateComputation.of(CHF_TOIS, START_DATE, END_DATE, 0, REF_DATA);
    ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5; // Fixing in the observation period are from 0 to 4 (inclusive)
    for (int i = 0; i < indexLast; i++) {
      LocalDate startDate = CHF_OBS[i].getEffectiveDate();
      LocalDate endDate = CHF_OBS[i].getMaturityDate();
      double af = CHF_TOIS.getDayCount().yearFraction(startDate, endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }

  /**
   * Test for the case where publication lag=0, effective offset=1 (CHF conventions) and no cutoff period.
   * The arithmetic average coupons are used mainly in USD. This test is more for completeness than a real case.
   */
  public void rateChfNoCutOffSensitivity() {
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(CHF_TOIS);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    for (int i = 0; i < CHF_OBS.length; i++) {
      when(mockRates.rate(CHF_OBS[i])).thenReturn(FIXING_RATES[i]);
      OvernightRateSensitivity sensitivity = OvernightRateSensitivity.of(CHF_OBS[i], CHF_TOIS.getCurrency(), 1d);
      when(mockRates.ratePointSensitivity(CHF_OBS[i])).thenReturn(sensitivity);
    }
    OvernightAveragedRateComputation ro =
        OvernightAveragedRateComputation.of(CHF_TOIS, START_DATE, END_DATE, 0, REF_DATA);
    ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;
    PointSensitivityBuilder sensitivityBuilderComputed = obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE,
        DUMMY_ACCRUAL_END_DATE, simpleProv);
    PointSensitivities sensitivityComputed = sensitivityBuilderComputed.build().normalized();
    Double[] sensitivityExpected = computedSensitivityFD(ro, CHF_TOIS, CHF_OBS);
    assertEquals(sensitivityComputed.getSensitivities().size(), sensitivityExpected.length);
    for (int i = 0; i < sensitivityExpected.length; ++i) {
      assertEquals(sensitivityComputed.getSensitivities().get(i).getSensitivity(), sensitivityExpected[i], EPS_FD);
    }
  }

  /** Test for the case where publication lag=0, effective offset=0 (GBP conventions) and no cutoff period.
    *   The arithmetic average coupons are used mainly in USD. This test is more for completeness than a real case. */
  public void rateGbpNoCutOff() {
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(GBP_SONIA);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    for (int i = 0; i < GBP_OBS.length; i++) {
      when(mockRates.rate(GBP_OBS[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateComputation ro =
        OvernightAveragedRateComputation.of(GBP_SONIA, START_DATE, END_DATE, 0, REF_DATA);
    ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5; // Fixing in the observation period are from 1 to 5 (inclusive)
    for (int i = 1; i <= indexLast; i++) {
      LocalDate startDate = GBP_OBS[i].getEffectiveDate();
      LocalDate endDate = GBP_OBS[i].getMaturityDate();
      double af = GBP_SONIA.getDayCount().yearFraction(startDate, endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }

  /** Test for the case where publication lag=0, effective offset=0 (GBP conventions) and no cutoff period.
    *   The arithmetic average coupons are used mainly in USD. This test is more for completeness than a real case. */
  public void rateGbpNoCutOffSensitivity() {
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(GBP_SONIA);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    for (int i = 0; i < GBP_OBS.length; i++) {
      when(mockRates.rate(GBP_OBS[i])).thenReturn(FIXING_RATES[i]);
      OvernightRateSensitivity sensitivity = OvernightRateSensitivity.of(GBP_OBS[i], GBP_SONIA.getCurrency(), 1d);
      when(mockRates.ratePointSensitivity(GBP_OBS[i])).thenReturn(sensitivity);
    }
    OvernightAveragedRateComputation ro =
        OvernightAveragedRateComputation.of(GBP_SONIA, START_DATE, END_DATE, 0, REF_DATA);
    ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;
    PointSensitivityBuilder sensitivityBuilderComputed = obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE,
        DUMMY_ACCRUAL_END_DATE, simpleProv);
    PointSensitivities sensitivityComputed = sensitivityBuilderComputed.build().normalized();
    Double[] sensitivityExpected = computedSensitivityFD(ro, GBP_SONIA, GBP_OBS);
    assertEquals(sensitivityComputed.getSensitivities().size(), sensitivityExpected.length);
    for (int i = 0; i < sensitivityExpected.length; ++i) {
      assertEquals(sensitivityComputed.getSensitivities().get(i).getSensitivity(), sensitivityExpected[i], EPS_FD);
    }
  }

  //-------------------------------------------------------------------------
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.DOUBLE_QUADRATIC;
  private static final LocalDateDoubleTimeSeries TIME_SERIES;
  static {
    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < FIXING_DATES.length; i++) {
      builder.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    TIME_SERIES = builder.build();
  }
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  /** Test parameter sensitivity with finite difference sensitivity calculator. Two days cutoff period. */
  public void rateFedFundTwoDaysCutoffParameterSensitivity() {
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    DoubleArray time = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
    DoubleArray rate = DoubleArray.of(0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135);

    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      Curve onCurve = InterpolatedNodalCurve.of(
          Curves.zeroRates("ON", ACT_ACT_ISDA), time, rate, INTERPOLATOR);
      ImmutableRatesProvider prov = ImmutableRatesProvider.builder(valuationDate[loopvaldate])
          .overnightIndexCurve(USD_FED_FUND, onCurve, TIME_SERIES)
          .build();
      OvernightAveragedRateComputation ro =
          OvernightAveragedRateComputation.of(USD_FED_FUND, START_DATE, END_DATE, 2, REF_DATA);
      ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;

      PointSensitivityBuilder sensitivityBuilderComputed =
          obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
      CurrencyParameterSensitivities parameterSensitivityComputed =
          prov.parameterSensitivity(sensitivityBuilderComputed.build());

      CurrencyParameterSensitivities parameterSensitivityExpected =
          CAL_FD.sensitivity(prov, (p) -> CurrencyAmount.of(USD_FED_FUND.getCurrency(),
              obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(parameterSensitivityExpected, EPS_FD * 10.0));
    }
  }

  /** Test parameter sensitivity with finite difference sensitivity calculator. No cutoff period. */
  public void rateChfNoCutOffParameterSensitivity() {
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    DoubleArray time = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
    DoubleArray rate = DoubleArray.of(0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135);

    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      Curve onCurve = InterpolatedNodalCurve.of(
          Curves.zeroRates("ON", ACT_ACT_ISDA), time, rate, INTERPOLATOR);
      ImmutableRatesProvider prov = ImmutableRatesProvider.builder(valuationDate[loopvaldate])
          .overnightIndexCurve(CHF_TOIS, onCurve, TIME_SERIES)
          .build();
      OvernightAveragedRateComputation ro =
          OvernightAveragedRateComputation.of(CHF_TOIS, START_DATE, END_DATE, 0, REF_DATA);
      ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;

      PointSensitivityBuilder sensitivityBuilderComputed =
          obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
      CurrencyParameterSensitivities parameterSensitivityComputed =
          prov.parameterSensitivity(sensitivityBuilderComputed.build());

      CurrencyParameterSensitivities parameterSensitivityExpected =
          CAL_FD.sensitivity(prov, (p) -> CurrencyAmount.of(CHF_TOIS.getCurrency(),
              obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(parameterSensitivityExpected, EPS_FD * 10.0));
    }
  }

  private Double[] computedSensitivityFD(
      OvernightAveragedRateComputation ro, OvernightIndex index, OvernightIndexObservation[] indexObs) {

    int nRates = FIXING_DATES.length;
    OvernightIndexRates[] mockRatesUp = new OvernightIndexRates[nRates];
    SimpleRatesProvider[] simpleProvUp = new SimpleRatesProvider[nRates];
    OvernightIndexRates[] mockRatesDw = new OvernightIndexRates[nRates];
    SimpleRatesProvider[] simpleProvDw = new SimpleRatesProvider[nRates];
    double[][] ratesUp = new double[nRates][];
    double[][] ratesDw = new double[nRates][];
    for (int i = 0; i < nRates; ++i) {
      mockRatesUp[i] = mock(OvernightIndexRates.class);
      simpleProvUp[i] = new SimpleRatesProvider(mockRatesUp[i]);
      mockRatesDw[i] = mock(OvernightIndexRates.class);
      simpleProvDw[i] = new SimpleRatesProvider(mockRatesDw[i]);
      ratesUp[i] = Arrays.copyOf(FIXING_RATES, nRates);
      ratesDw[i] = Arrays.copyOf(FIXING_RATES, nRates);
      ratesUp[i][i] += EPS_FD;
      ratesDw[i][i] -= EPS_FD;
    }
    for (int i = 0; i < nRates; i++) {
      for (int j = 0; j < nRates; ++j) {
        when(mockRatesUp[j].rate(indexObs[i])).thenReturn(ratesUp[j][i]);
        when(mockRatesDw[j].rate(indexObs[i])).thenReturn(ratesDw[j][i]);
      }
    }
    ForwardOvernightAveragedRateComputationFn obsFn = ForwardOvernightAveragedRateComputationFn.DEFAULT;
    List<Double> sensitivityExpected = new ArrayList<Double>();
    for (int i = 0; i < nRates; ++i) {
      double rateUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvUp[i]);
      double rateDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvDw[i]);
      double res = 0.5 * (rateUp - rateDw) / EPS_FD;
      if (Math.abs(res) > 1.0e-14) {
        sensitivityExpected.add(res);
      }
    }
    int size = sensitivityExpected.size();
    Double[] result = new Double[size];
    return sensitivityExpected.toArray(result);
  }
}
