/**
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

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.finance.rate.OvernightAveragedRateObservation;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link ForwardOvernightAveragedRateObservationFn}.
 */
public class ForwardOvernightAveragedRateObservationFnTest {

  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 1); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2015, 1, 1); // Accrual dates irrelevant for the rate
  private static final LocalDate FIXING_START_DATE = date(2015, 1, 8);
  private static final LocalDate FIXING_END_DATE = date(2015, 1, 15); // 1w only to decrease data
  private static final LocalDate[] FIXING_DATES = new LocalDate[] {
      date(2015, 1, 7), date(2015, 1, 8), date(2015, 1, 9),
      date(2015, 1, 12), date(2015, 1, 13), date(2015, 1, 14), date(2015, 1, 15)};
  private static final double[] FIXING_RATES = {
      0.0012, 0.0023, 0.0034,
      0.0045, 0.0056, 0.0067, 0.0078};
  private static final double TOLERANCE_RATE = 1.0E-10;
  private static final double EPS_FD = 1.0E-7;

  //-------------------------------------------------------------------------
  /** Test for the case where publication lag=1, effective offset=0 (USD conventions) and no cutoff period. */
  @Test
  public void rateFedFundNoCutOff() {
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    // Accrual dates = fixing dates
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5; // Fixing in the observation period are from 1 to 5 (inclusive)
    for (int i = 1; i <= indexLast; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }

  /** Test against FD approximation for the case where publication lag=1, effective offset=0 (USD conventions) and 
   * no cutoff period. Note that all the rates are bumped here, i.e., all the rates are treated as forward rates.*/
  @Test
  public void rateFedFundNoCutOffSensitivity() {
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
      OvernightRateSensitivity sensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockProv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(sensitivity);
    }
    OvernightAveragedRateObservation ro = OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE,
        FIXING_END_DATE, 0);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    PointSensitivityBuilder sensitivityBuilderComputed = obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE,
        DUMMY_ACCRUAL_END_DATE, mockProv);
    PointSensitivities sensitivityComputed = sensitivityBuilderComputed.build().normalized();
    Double[] sensitivityExpected = computedSensitivityFD(ro, USD_FED_FUND);
    assertEquals(sensitivityComputed.getSensitivities().size(), sensitivityExpected.length);
    for (int i = 0; i < sensitivityExpected.length; ++i) {
      assertEquals(sensitivityComputed.getSensitivities().get(i).getSensitivity(), sensitivityExpected[i], EPS_FD);
    }
  }

  /** Test for the case where publication lag=1, effective offset=0 (USD conventions) and cutoff=2 (FedFund swaps). */
  @Test
  public void rateFedFund() {
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5; // Fixing in the observation period are from 1 to 5 (inclusive), but last is modified by cut-off
    for (int i = 1; i <= indexLast - 1; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    // CutOff
    LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[indexLast]);
    double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[indexLast], endDate);
    accrualFactorTotal += af;
    accruedRate += FIXING_RATES[indexLast - 1] * af;
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }

  /** Test against FD approximation for the case where publication lag=1, effective offset=0 (USD conventions) and 
   * cutoff=2 (FedFund swaps). 
   * Note that all the rates are bumped here, i.e., all the rates are treated as forward rates. */
  @Test
  public void rateFedFundSensitivity() {
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
      OvernightRateSensitivity sensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockProv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(sensitivity);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;

    PointSensitivityBuilder sensitivityBuilderComputed = obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE,
        DUMMY_ACCRUAL_END_DATE, mockProv);
    PointSensitivities sensitivityComputed = sensitivityBuilderComputed.build().normalized();
    Double[] sensitivityExpected = computedSensitivityFD(ro, USD_FED_FUND);
    assertEquals(sensitivityComputed.getSensitivities().size(), sensitivityExpected.length);
    for (int i = 0; i < sensitivityExpected.length; ++i) {
      assertEquals(sensitivityComputed.getSensitivities().get(i).getSensitivity(), sensitivityExpected[i], EPS_FD);
    }
  }

  /**
   * Test for the case where publication lag=0, effective offset=1 (CHF conventions) and no cutoff period. 
   * The arithmetic average coupons are used mainly in USD. This test is more for completeness than a real case.
   */
  @Test
  public void rateChfNoCutOff() {
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(CHF_TOIS, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(CHF_TOIS, FIXING_START_DATE, FIXING_END_DATE, 0);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5; // Fixing in the observation period are from 1 to 5 (inclusive)
    for (int i = 1; i <= indexLast; i++) {
      LocalDate startDate = CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate endDate = CHF_TOIS.calculateMaturityFromEffective(startDate);
      double af = CHF_TOIS.getDayCount().yearFraction(startDate, endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }

  /**
   * Test for the case where publication lag=0, effective offset=1 (CHF conventions) and no cutoff period. 
   * The arithmetic average coupons are used mainly in USD. This test is more for completeness than a real case.
   */
  @Test
  public void rateChfNoCutOffSensitivity() {
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(CHF_TOIS, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      LocalDate fixingStartDate = CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = CHF_TOIS.calculateMaturityFromEffective(fixingStartDate);
      OvernightRateSensitivity sensitivity = OvernightRateSensitivity.of(CHF_TOIS, CHF_TOIS.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockProv.overnightIndexRateSensitivity(CHF_TOIS, FIXING_DATES[i])).thenReturn(sensitivity);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(CHF_TOIS, FIXING_START_DATE, FIXING_END_DATE, 0);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    PointSensitivityBuilder sensitivityBuilderComputed = obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE,
        DUMMY_ACCRUAL_END_DATE, mockProv);
    PointSensitivities sensitivityComputed = sensitivityBuilderComputed.build().normalized();
    Double[] sensitivityExpected = computedSensitivityFD(ro, CHF_TOIS);
    assertEquals(sensitivityComputed.getSensitivities().size(), sensitivityExpected.length);
    for (int i = 0; i < sensitivityExpected.length; ++i) {
      assertEquals(sensitivityComputed.getSensitivities().get(i).getSensitivity(), sensitivityExpected[i], EPS_FD);
    }
  }

  /** Test for the case where publication lag=0, effective offset=0 (GBP conventions) and no cutoff period. 
    *   The arithmetic average coupons are used mainly in USD. This test is more for completeness than a real case. */
  @Test
  public void rateGbpNoCutOff() {
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(GBP_SONIA, FIXING_START_DATE, FIXING_END_DATE, 0);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5; // Fixing in the observation period are from 1 to 5 (inclusive)
    for (int i = 1; i <= indexLast; i++) {
      LocalDate startDate = FIXING_DATES[i]; // no effective lag
      LocalDate endDate = GBP_SONIA.calculateMaturityFromEffective(startDate);
      double af = GBP_SONIA.getDayCount().yearFraction(startDate, endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }

  /** Test for the case where publication lag=0, effective offset=0 (GBP conventions) and no cutoff period. 
    *   The arithmetic average coupons are used mainly in USD. This test is more for completeness than a real case. */
  @Test
  public void rateGbpNoCutOffSensitivity() {
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      LocalDate fixingStartDate = GBP_SONIA.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = GBP_SONIA.calculateMaturityFromEffective(fixingStartDate);
      OvernightRateSensitivity sensitivity = OvernightRateSensitivity.of(GBP_SONIA, GBP_SONIA.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockProv.overnightIndexRateSensitivity(GBP_SONIA, FIXING_DATES[i])).thenReturn(sensitivity);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(GBP_SONIA, FIXING_START_DATE, FIXING_END_DATE, 0);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    PointSensitivityBuilder sensitivityBuilderComputed = obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE,
        DUMMY_ACCRUAL_END_DATE, mockProv);
    PointSensitivities sensitivityComputed = sensitivityBuilderComputed.build().normalized();
    Double[] sensitivityExpected = computedSensitivityFD(ro, GBP_SONIA);
    assertEquals(sensitivityComputed.getSensitivities().size(), sensitivityExpected.length);
    for (int i = 0; i < sensitivityExpected.length; ++i) {
      assertEquals(sensitivityComputed.getSensitivities().get(i).getSensitivity(), sensitivityExpected[i], EPS_FD);
    }
  }

  private static final YieldCurve ON_INDEX_CURVE;
  static {
    CombinedInterpolatorExtrapolator interp = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.FLAT_EXTRAPOLATOR,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    double[] time = new double[] {0.0, 0.5, 1.0, 2.0, 5.0, 10.0 };
    double[] rate = new double[] {0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135 };
    InterpolatedDoublesCurve curve_usd = InterpolatedDoublesCurve.from(time, rate, interp);
    ON_INDEX_CURVE = new YieldCurve("ON", curve_usd);
  }
  private static LocalDateDoubleTimeSeriesBuilder TIME_SERIES_BUILDER = LocalDateDoubleTimeSeries.builder();
  static {
    for (int i = 0; i < FIXING_DATES.length; i++) {
      TIME_SERIES_BUILDER.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
  }
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  /** Test parameter sensitivity with finite difference sensitivity calculator. Two days cutoff period. */
  @Test
  public void rateFedFundTwoDaysCutoffParameterSensitivity() {
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8) };
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
          .valuationDate(valuationDate[loopvaldate])
          .indexCurves(ImmutableMap.of(USD_FED_FUND, ON_INDEX_CURVE))
        .timeSeries(ImmutableMap.of(USD_FED_FUND, TIME_SERIES_BUILDER.build()))
        .dayCount(ACT_ACT_ISDA)
        .build();
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;

      PointSensitivityBuilder sensitivityBuilderComputed =
          obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
    CurveParameterSensitivity parameterSensitivityComputed =
        prov.parameterSensitivity(sensitivityBuilderComputed.build());

    CurveParameterSensitivity parameterSensitivityExpected =
        CAL_FD.sensitivity(prov, (p) -> CurrencyAmount.of(USD_FED_FUND.getCurrency(),
            obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, (p))));
    assertTrue(parameterSensitivityComputed.equalWithTolerance(parameterSensitivityExpected, EPS_FD * 10.0));
    }
  }

  /** Test parameter sensitivity with finite difference sensitivity calculator. No cutoff period. */
  @Test
  public void rateChfNoCutOffParameterSensitivity() {
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8) };
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
          .valuationDate(valuationDate[loopvaldate])
          .indexCurves(ImmutableMap.of(CHF_TOIS, ON_INDEX_CURVE))
          .timeSeries(ImmutableMap.of(CHF_TOIS, TIME_SERIES_BUILDER.build()))
          .dayCount(ACT_ACT_ISDA)
          .build();
      OvernightAveragedRateObservation ro =
          OvernightAveragedRateObservation.of(CHF_TOIS, FIXING_START_DATE, FIXING_END_DATE, 0);
      ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;

      PointSensitivityBuilder sensitivityBuilderComputed =
          obsFn.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
      CurveParameterSensitivity parameterSensitivityComputed =
          prov.parameterSensitivity(sensitivityBuilderComputed.build());

      CurveParameterSensitivity parameterSensitivityExpected =
          CAL_FD.sensitivity(prov, (p) -> CurrencyAmount.of(CHF_TOIS.getCurrency(),
              obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(parameterSensitivityExpected, EPS_FD * 10.0));
    }
  }

  private Double[] computedSensitivityFD(OvernightAveragedRateObservation ro, OvernightIndex index) {
    int nRates = FIXING_DATES.length;
    RatesProvider[] mockProvUp = new RatesProvider[nRates];
    RatesProvider[] mockProvDw = new RatesProvider[nRates];
    double[][] ratesUp = new double[nRates][];
    double[][] ratesDw = new double[nRates][];
    for (int i = 0; i < nRates; ++i) {
      mockProvUp[i] = mock(RatesProvider.class);
      mockProvDw[i] = mock(RatesProvider.class);
      ratesUp[i] = Arrays.copyOf(FIXING_RATES, nRates);
      ratesDw[i] = Arrays.copyOf(FIXING_RATES, nRates);
      ratesUp[i][i] += EPS_FD;
      ratesDw[i][i] -= EPS_FD;
    }
    for (int i = 0; i < nRates; i++) {
      for (int j = 0; j < nRates; ++j) {
        when(mockProvUp[j].overnightIndexRate(index, FIXING_DATES[i])).thenReturn(ratesUp[j][i]);
        when(mockProvDw[j].overnightIndexRate(index, FIXING_DATES[i])).thenReturn(ratesDw[j][i]);
      }
    }
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    List<Double> sensitivityExpected = new ArrayList<Double>();
    for (int i = 0; i < nRates; ++i) {
      double rateUp = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp[i]);
      double rateDw = obsFn.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw[i]);
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
