/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.OvernightIndices.CHF_TOIS;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.finance.rate.OvernightCompoundedRateObservation;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link ForwardOvernightCompoundedRateObservationFn}.
 */
public class ForwardOvernightCompoundedRateObservationFnTest {

  private static final LocalDate DUMMY_ACCRUAL_START_DATE = date(2015, 1, 1); // Accrual dates irrelevant for the rate
  private static final LocalDate DUMMY_ACCRUAL_END_DATE = date(2015, 1, 31); // Accrual dates irrelevant for the rate
  private static final LocalDate FIXING_START_DATE = date(2015, 1, 8);
  private static final LocalDate FIXING_END_DATE = date(2015, 1, 15); // 1w only to decrease data
  private static final LocalDate[] FIXING_DATES = new LocalDate[] {
      date(2015, 1, 7), date(2015, 1, 8), date(2015, 1, 9),
      date(2015, 1, 12), date(2015, 1, 13), date(2015, 1, 14), date(2015, 1, 15)};
  private static final double[] FIXING_RATES = {
      0.0012, 0.0023, 0.0034,
      0.0045, 0.0056, 0.0067, 0.0078};
  private static final double[] FORWARD_RATES = {
      0.0112, 0.0123, 0.0134,
      0.0145, 0.0156, 0.0167, 0.0178};

  private static final double TOLERANCE_RATE = 1.0E-10;
  private static final double EPS_FD = 1.0E-7;

  private static final ForwardOvernightCompoundedRateObservationFn OBS_FWD_ONCMP =
      ForwardOvernightCompoundedRateObservationFn.DEFAULT;

  /** No cutoff period and the period entirely forward. Test the forward part only. */
  @Test
  public void rateFedFundNoCutOffForward() { // publication=1, cutoff=0, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    RatesProvider mockProv = mock(RatesProvider.class);
    double rateCmp = 0.0123;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    double rateExpected = rateCmp;
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** No cutoff period and the period entirely forward. Test the forward part only against FD approximation. */
  @Test
  public void rateFedFundNoCutOffForwardSensitivity() { // publication=1, cutoff=0, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    RatesProvider mockProv = mock(RatesProvider.class);
    double rateCmp = 0.0123;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    PointSensitivityBuilder rateSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_START_DATE, FIXING_END_DATE, 1.0);
    when(mockProv.overnightIndexRatePeriodSensitivity(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateSensitivity);
    RatesProvider mockProvUp = mock(RatesProvider.class);
    when(mockProvUp.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateCmp + EPS_FD);
    RatesProvider mockProvDw = mock(RatesProvider.class);
    when(mockProvDw.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateCmp - EPS_FD);

    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), FIXING_START_DATE, FIXING_END_DATE, sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
    }
  }

  /** Two days cutoff and the period is entirely forward. Test forward part plus cutoff specifics.
   * Almost all Overnight Compounding coupon (OIS) don't use cutoff period.*/
  @Test
  public void rateFedFund2CutOffForward() { // publication=1, cutoff=2, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double investmentFactor = 1.0;
    double afNonCutoff = 0.0;
    for (int i = 1; i < 5; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNonCutoff += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afNonCutoff;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = USD_FED_FUND.getDayCount().yearFraction(fixingCutOff, endDate);
    double rateExpected = ((1.0 + rateCmp * afNonCutoff) * (1.0d + FORWARD_RATES[4] * afCutOff) - 1.0d)
        / (afNonCutoff + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Two days cutoff and the period is entirely forward. Test forward part plus cutoff specifics against FD.
   * Almost all Overnight Compounding coupon (OIS) don't use cutoff period.*/
  @Test
  public void rateFedFund2CutOffForwardSensitivity() { // publication=1, cutoff=2, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    RatesProvider mockProv = mock(RatesProvider.class);
    int nFixings = FIXING_DATES.length;
    RatesProvider[] mockProvUp = new RatesProvider[nFixings];
    RatesProvider[] mockProvDw = new RatesProvider[nFixings];
    RatesProvider mockProvPeriodUp = mock(RatesProvider.class);
    RatesProvider mockProvPeriodDw = mock(RatesProvider.class);
    double[][] forwardRatesUp = new double[nFixings][nFixings];
    double[][] forwardRatesDw = new double[nFixings][nFixings];
    for (int i = 0; i < nFixings; i++) {
      mockProvUp[i] = mock(RatesProvider.class);
      mockProvDw[i] = mock(RatesProvider.class);
      for (int j = 0; j < nFixings; j++) {
        double rateForUp = i == j ? FORWARD_RATES[j] + EPS_FD : FORWARD_RATES[j];
        double rateForDw = i == j ? FORWARD_RATES[j] - EPS_FD : FORWARD_RATES[j];
        forwardRatesUp[i][j] = rateForUp;
        forwardRatesDw[i][j] = rateForDw;
      }
    }
    for (int i = 0; i < nFixings; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      when(mockProvPeriodUp.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      when(mockProvPeriodDw.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
      PointSensitivityBuilder rateSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1.0);
      when(mockProv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(rateSensitivity);
      for (int j = 0; j < nFixings; ++j) {
        when(mockProvUp[j].overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(forwardRatesUp[j][i]);
        when(mockProvDw[j].overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(forwardRatesDw[j][i]);
      }
    }
    double investmentFactor = 1.0;
    double afNonCutoff = 0.0;
    for (int i = 1; i < 5; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNonCutoff += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afNonCutoff;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    when(mockProvPeriodUp.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp + EPS_FD);
    when(mockProvPeriodDw.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp - EPS_FD);
    PointSensitivityBuilder rateSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), 1.0);
    when(mockProv.overnightIndexRatePeriodSensitivity(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateSensitivity);
    for (int i = 0; i < nFixings; ++i) {
      when(mockProvUp[i].overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
          USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
      when(mockProvDw[i].overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
          USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    }

    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderExpected1 = PointSensitivityBuilder.none();
      for (int i = 0; i < nFixings; ++i) {
        when(mockProvUp[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        when(mockProvDw[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp[i]);
        double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw[i]);
        double cutoffSensitivity = 0.5 * (rateUp - rateDw) / EPS_FD; // [4] is nonzero 
        LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
        LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
        sensitivityBuilderExpected1 = cutoffSensitivity == 0.0 ? sensitivityBuilderExpected1
            : sensitivityBuilderExpected1.combinedWith(OvernightRateSensitivity.of(USD_FED_FUND,
                USD_FED_FUND.getCurrency(), FIXING_DATES[i], fixingEndDate, cutoffSensitivity));
      }
      double ratePeriodUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvPeriodUp);
      double ratePeriodDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvPeriodDw);
      double periodSensitivity = 0.5 * (ratePeriodUp - ratePeriodDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected2 = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE),
          periodSensitivity);
      PointSensitivityBuilder sensitivityBuilderExpected = sensitivityBuilderExpected1
          .combinedWith(sensitivityBuilderExpected2);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
    }
  }

  /** No cutoff and one already fixed ON rate. Test the already fixed portion with only one fixed ON rate.*/
  @Test
  public void rateFedFund0CutOffValuation1() {
    // publication=1, cutoff=0, effective offset=0, TS: Fixing 1
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < 2; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < 2; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = 2; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    LocalDate fixingknown = FIXING_DATES[1];
    LocalDate endDateKnown = USD_FED_FUND.calculateMaturityFromEffective(fixingknown);
    double afKnown = USD_FED_FUND.getDayCount().yearFraction(fixingknown, endDateKnown);
    double investmentFactor = 1.0;
    double afNoCutoff = 0.0;
    for (int i = 2; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNoCutoff += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afNoCutoff;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        FIXING_END_DATE)).thenReturn(rateCmp);
    double rateExpected = ((1.0d + FIXING_RATES[1] * afKnown) * (1.0 + rateCmp * afNoCutoff) - 1.0d)
        / (afKnown + afNoCutoff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** No cutoff and one already fixed ON rate. Test the already fixed portion with only one fixed ON rate against FD.*/
  @Test
  public void rateFedFund0CutOffValuation1Sensitivity() {
    // publication=1, cutoff=0, effective offset=0, TS: Fixing 1
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < 2; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    RatesProvider mockProvUp = mock(RatesProvider.class);
    RatesProvider mockProvDw = mock(RatesProvider.class);
    when(mockProvUp.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    when(mockProvDw.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    double investmentFactor = 1.0;
    double afNoCutoff = 0.0;
    for (int i = 2; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNoCutoff += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afNoCutoff;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        FIXING_END_DATE)).thenReturn(rateCmp);
    when(mockProvUp.overnightIndexRatePeriod(USD_FED_FUND, USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        FIXING_END_DATE)).thenReturn(rateCmp + EPS_FD);
    when(mockProvDw.overnightIndexRatePeriod(USD_FED_FUND, USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        FIXING_END_DATE)).thenReturn(rateCmp - EPS_FD);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE), FIXING_END_DATE, 1.0d);
    when(
        mockProv.overnightIndexRatePeriodSensitivity(USD_FED_FUND,
            USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
            FIXING_END_DATE)).thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE), FIXING_END_DATE,
          sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
    }
  }

  /** No cutoff period and two already fixed ON rate. ON index is SONIA. */
  @Test
  public void rateSonia0CutOffValuation2() {
    // publication=0, cutoff=0, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(GBP_SONIA, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0d;
    double investmentFactorKnown = 1.0d;
    for (int i = 0; i < lastFixing - 1; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = GBP_SONIA.calculateMaturityFromEffective(fixingknown);
      double af = GBP_SONIA.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      investmentFactorKnown *= 1.0d + FIXING_RATES[i + 1] * af;
    }
    double afNoCutoff = 0.0d;
    double investmentFactorNoCutoff = 1.0d;
    for (int i = lastFixing; i < 6; i++) {
      LocalDate endDate = GBP_SONIA.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = GBP_SONIA.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNoCutoff += af;
      investmentFactorNoCutoff *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactorNoCutoff - 1.0d) / afNoCutoff;
    when(mockProv.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp);
    double rateExpected = (investmentFactorKnown * (1.0 + rateCmp * afNoCutoff) - 1.0d)
        / (afKnown + afNoCutoff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation. 
   * No cutoff period and two already fixed ON rate. ON index is SONIA. */
  @Test
  public void rateSonia0CutOffValuation2Sensitivity() {
    // publication=0, cutoff=0, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(GBP_SONIA, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    RatesProvider mockProvUp = mock(RatesProvider.class);
    RatesProvider mockProvDw = mock(RatesProvider.class);
    when(mockProvUp.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    when(mockProvDw.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    double afNoCutoff = 0.0d;
    double investmentFactorNoCutoff = 1.0d;
    for (int i = lastFixing; i < 6; i++) {
      LocalDate endDate = GBP_SONIA.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = GBP_SONIA.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNoCutoff += af;
      investmentFactorNoCutoff *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactorNoCutoff - 1.0d) / afNoCutoff;
    when(mockProv.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp);
    when(mockProvUp.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp + EPS_FD);
    when(mockProvDw.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp - EPS_FD);
    OvernightRateSensitivity periodSensitivity = OvernightRateSensitivity.of(GBP_SONIA, GBP_SONIA.getCurrency(),
        FIXING_DATES[lastFixing], FIXING_DATES[6], 1.0d);
    when(mockProv.overnightIndexRatePeriodSensitivity(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      OvernightRateSensitivity sensitivityBuilderExpected = OvernightRateSensitivity.of(GBP_SONIA,
          GBP_SONIA.getCurrency(), FIXING_DATES[lastFixing], FIXING_DATES[6], sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
    }
  }

  /** No cutoff period and two already fixed ON rate. ON index is TOIS (with a effective offset of 1; TN rate). */
  @Test
  public void rateTois0CutOffValuation2() {
    // publication=0, cutoff=0, effective offset=1, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(CHF_TOIS, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(CHF_TOIS)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(CHF_TOIS, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(CHF_TOIS, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0d;
    double investmentFactorKnown = 1.0d;
    for (int i = 1; i < lastFixing; i++) {
      LocalDate fixingknown = FIXING_DATES[i];
      LocalDate startDateKnown = CHF_TOIS.calculateEffectiveFromFixing(fixingknown);
      LocalDate endDateKnown = CHF_TOIS.calculateMaturityFromEffective(startDateKnown);
      double af = CHF_TOIS.getDayCount().yearFraction(startDateKnown, endDateKnown);
      afKnown += af;
      investmentFactorKnown *= 1.0d + af * FIXING_RATES[i];
    }
    double afNoCutoff = 0.0d;
    double investmentFactorNoCutoff = 1.0d;
    for (int i = lastFixing; i < 6; i++) {
      LocalDate fixing = FIXING_DATES[i];
      LocalDate startDate = CHF_TOIS.calculateEffectiveFromFixing(fixing);
      LocalDate endDate = CHF_TOIS.calculateMaturityFromEffective(startDate);
      double af = CHF_TOIS.getDayCount().yearFraction(startDate, endDate);
      afNoCutoff += af;
      investmentFactorNoCutoff *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactorNoCutoff - 1.0d) / afNoCutoff;
    when(mockProv.overnightIndexRatePeriod(CHF_TOIS, CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5])))).thenReturn(rateCmp);
    double rateExpected = (investmentFactorKnown * (1.0 + rateCmp * afNoCutoff) - 1.0d)
        / (afKnown + afNoCutoff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation. 
   * No cutoff period and two already fixed ON rate. ON index is TOIS (with a effective offset of 1; TN rate). */
  @Test
  public void rateTois0CutOffValuation2Sensitivity() {
    // publication=0, cutoff=0, effective offset=1, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(CHF_TOIS, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(CHF_TOIS)).thenReturn(tsb.build());
    RatesProvider mockProvUp = mock(RatesProvider.class);
    RatesProvider mockProvDw = mock(RatesProvider.class);
    when(mockProvUp.timeSeries(CHF_TOIS)).thenReturn(tsb.build());
    when(mockProvDw.timeSeries(CHF_TOIS)).thenReturn(tsb.build());
    double afNoCutoff = 0.0d;
    double investmentFactorNoCutoff = 1.0d;
    for (int i = lastFixing; i < 6; i++) {
      LocalDate fixing = FIXING_DATES[i];
      LocalDate startDate = CHF_TOIS.calculateEffectiveFromFixing(fixing);
      LocalDate endDate = CHF_TOIS.calculateMaturityFromEffective(startDate);
      double af = CHF_TOIS.getDayCount().yearFraction(startDate, endDate);
      afNoCutoff += af;
      investmentFactorNoCutoff *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactorNoCutoff - 1.0d) / afNoCutoff;
    when(mockProv.overnightIndexRatePeriod(CHF_TOIS, CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5])))).thenReturn(
        rateCmp);
    when(mockProvUp.overnightIndexRatePeriod(CHF_TOIS, CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5])))).thenReturn(
        rateCmp + EPS_FD);
    when(mockProvDw.overnightIndexRatePeriod(CHF_TOIS, CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5])))).thenReturn(
        rateCmp - EPS_FD);
    OvernightRateSensitivity periodSensitivity = OvernightRateSensitivity.of(CHF_TOIS, CHF_TOIS.getCurrency(),
        CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5])), 1.0d);
    when(mockProv.overnightIndexRatePeriodSensitivity(CHF_TOIS,
        CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5]))))
        .thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      OvernightRateSensitivity sensitivityBuilderExpected = OvernightRateSensitivity.of(CHF_TOIS,
          CHF_TOIS.getCurrency(), CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
          CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5])),
          sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
    }
  }

  /** No cutoff and two already fixed ON rate. ON index is Fed Fund. */
  @Test
  public void rateFedFund0CutOffValuation2() {
    // publication=1, cutoff=0, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 12), date(2015, 1, 13)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0d;
    double investmentFactorKnown = 1.0d;
    for (int i = 0; i < lastFixing - 1; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = USD_FED_FUND.calculateMaturityFromEffective(fixingknown);
      double af = USD_FED_FUND.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      investmentFactorKnown *= 1.0d + FIXING_RATES[i + 1] * af;
    }
    double investmentFactor = 1.0;
    double afNoCutoff = 0.0;
    for (int i = lastFixing; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNoCutoff += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afNoCutoff;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp);
    double rateExpected = (investmentFactorKnown * (1.0 + rateCmp * afNoCutoff) - 1.0d)
        / (afKnown + afNoCutoff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation. 
   * No cutoff and two already fixed ON rate. ON index is Fed Fund. */
  @Test
  public void rateFedFund0CutOffValuation2Sensitivity() {
    // publication=1, cutoff=0, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 12), date(2015, 1, 13)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    RatesProvider mockProvUp = mock(RatesProvider.class);
    RatesProvider mockProvDw = mock(RatesProvider.class);
    when(mockProvUp.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    when(mockProvDw.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    double investmentFactor = 1.0;
    double afNoCutoff = 0.0;
    for (int i = lastFixing; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNoCutoff += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afNoCutoff;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp);
    when(mockProvUp.overnightIndexRatePeriod(USD_FED_FUND, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp + EPS_FD);
    when(mockProvDw.overnightIndexRatePeriod(USD_FED_FUND, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp - EPS_FD);
    OvernightRateSensitivity periodSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_DATES[lastFixing], FIXING_DATES[6], 1.0d);
    when(mockProv.overnightIndexRatePeriodSensitivity(USD_FED_FUND, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      OvernightRateSensitivity sensitivityBuilderExpected = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(),
          FIXING_DATES[lastFixing], FIXING_DATES[6], sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);

      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
    }
  }

  /** No cutoff, all ON rates already fixed. Time series up to 14-Jan (last fixing date used). */
  @Test
  public void rateFedFund0CutOffValuationEndTs14() {
    // publication=1, cutoff=0, effective offset=0, TS: Fixing all
    LocalDate[] valuationDate = {date(2015, 1, 15), date(2015, 1, 16), date(2015, 1, 17)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 6;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0d;
    double investmentFactorKnown = 1.0d;
    for (int i = 0; i < 5; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = USD_FED_FUND.calculateMaturityFromEffective(fixingknown);
      double af = USD_FED_FUND.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      investmentFactorKnown *= 1.0d + FIXING_RATES[i + 1] * af;
    }
    double rateExpected = (investmentFactorKnown - 1.0d) / afKnown;
    for (int loopvaldate = 0; loopvaldate < valuationDate.length; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity. No cutoff, all ON rates already fixed. Thus expected sensitivity is none. 
   * Time series up to 14-Jan (last fixing date used). */
  @Test
  public void rateFedFund0CutOffValuationEndTs14Sensitivity() {
    // publication=1, cutoff=0, effective offset=0, TS: Fixing all
    LocalDate[] valuationDate = {date(2015, 1, 15), date(2015, 1, 16), date(2015, 1, 17)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 6;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int loopvaldate = 0; loopvaldate < valuationDate.length; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(sensitivityComputed, PointSensitivityBuilder.none());
    }
  }

  /** No cutoff, all ON rates already fixed. Time series up to 15-Jan (one day after the last fixing date). */
  @Test
  public void rateFedFund0CutOffValuationEndTs15() {
    // publication=1, cutoff=0, effective offset=0, TS: Fixing all
    LocalDate[] valuationDate = {date(2015, 1, 16), date(2015, 1, 17)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 7;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0d;
    double investmentFactorKnown = 1.0d;
    for (int i = 0; i < 5; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = USD_FED_FUND.calculateMaturityFromEffective(fixingknown);
      double af = USD_FED_FUND.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      investmentFactorKnown *= 1.0d + FIXING_RATES[i + 1] * af;
    }
    double rateExpected = (investmentFactorKnown - 1.0d) / afKnown;
    for (int loopvaldate = 0; loopvaldate < valuationDate.length; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity. No cutoff, all ON rates already fixed. Thus expected sensitivity is none. 
   * Time series up to 15-Jan (one day after the last fixing date). */
  @Test
  public void rateFedFund0CutOffValuationEndTs15Sensitivity() {
    // publication=1, cutoff=0, effective offset=0, TS: Fixing all
    LocalDate[] valuationDate = {date(2015, 1, 16), date(2015, 1, 17)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 7;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int loopvaldate = 0; loopvaldate < valuationDate.length; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(sensitivityComputed, PointSensitivityBuilder.none());
    }
  }

  /** Two days cutoff, all ON rates already fixed. */
  @Test
  public void rateFedFund2CutOffValuationEnd() {
    // publication=1, cutoff=2, effective offset=0, TS: Fixing all
    LocalDate[] valuationDate = {date(2015, 1, 14), date(2015, 1, 15), date(2015, 1, 16)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 5;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0d;
    double investmentFactorKnown = 1.0d;
    for (int i = 0; i < 4; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = USD_FED_FUND.calculateMaturityFromEffective(fixingknown);
      double af = USD_FED_FUND.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      investmentFactorKnown *= 1.0d + FIXING_RATES[i + 1] * af;
    }
    LocalDate fixingknown = FIXING_DATES[5];
    LocalDate endDateKnown = USD_FED_FUND.calculateMaturityFromEffective(fixingknown);
    double af = USD_FED_FUND.getDayCount().yearFraction(fixingknown, endDateKnown);
    afKnown += af;
    investmentFactorKnown *= 1.0d + FIXING_RATES[4] * af; //Cutoff
    double rateExpected = (investmentFactorKnown - 1.0d) / afKnown;
    for (int loopvaldate = 0; loopvaldate < 3; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity. Two days cutoff, all ON rates already fixed. Thus none is expected. */
  @Test
  public void rateFedFund2CutOffValuationEndSensitivity() {
    // publication=1, cutoff=2, effective offset=0, TS: Fixing all
    LocalDate[] valuationDate = {date(2015, 1, 14), date(2015, 1, 15), date(2015, 1, 16)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 5;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int loopvaldate = 0; loopvaldate < 3; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(sensitivityComputed, PointSensitivityBuilder.none());
    }
  }

  /** One past fixing missing. Checking the error thrown. */
  @Test
  public void rateFedFund0CutOffValuation2MissingFixing() {
    // publication=1, cutoff=0, effective offset=0, TS: Fixing 2
    LocalDate valuationDate = date(2015, 1, 13);
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 2;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    when(mockProv.getValuationDate()).thenReturn(valuationDate);
    assertThrows(
        () -> OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv),
        PricingException.class);
    assertThrows(
        () -> OBS_FWD_ONCMP.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv),
        PricingException.class);
  }

  private static final YieldCurve FED_FUND_CURVE;
  static {
    CombinedInterpolatorExtrapolator interp = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.FLAT_EXTRAPOLATOR,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    double[] time_usd = new double[] {0.0, 0.5, 1.0, 2.0, 5.0, 10.0};
    double[] rate_usd = new double[] {0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135};
    InterpolatedDoublesCurve curve_usd = InterpolatedDoublesCurve.from(time_usd, rate_usd, interp);
    FED_FUND_CURVE = new YieldCurve("USD-Fed-Fund", curve_usd);
  }
  private static LocalDateDoubleTimeSeriesBuilder TIME_SERIES_BUILDER = LocalDateDoubleTimeSeries.builder();
  static {
    for (int i = 0; i < FIXING_DATES.length; i++) {
      TIME_SERIES_BUILDER.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
  }
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  /** Test parameter sensitivity with fd calculator. No cutoff. */
  @Test
  public void rateNoCutOffForwardParameterSensitivity() { // publication=1, cutoff=0, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
          .valuationDate(valuationDate[loopvaldate])
          .indexCurves(ImmutableMap.of(USD_FED_FUND, FED_FUND_CURVE))
          .timeSeries(ImmutableMap.of(USD_FED_FUND, TIME_SERIES_BUILDER.build()))
          .dayCount(ACT_ACT_ISDA)
          .build();
      PointSensitivityBuilder sensitivityBuilderComputed =
          OBS_FWD_ONCMP.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
      CurveParameterSensitivity parameterSensitivityComputed =
          prov.parameterSensitivity(sensitivityBuilderComputed.build());
      CurveParameterSensitivity parameterSensitivityExpected =
          CAL_FD.sensitivity(prov, (p) -> CurrencyAmount.of(USD_FED_FUND.getCurrency(),
              OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(parameterSensitivityExpected, EPS_FD * 10.0));
    }
  }

  /** Test parameter sensitivity with fd calculator. Two days cutoff. */
  @Test
  public void rate2CutOffForwardParameterSensitivity() { // publication=1, cutoff=2, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
          .valuationDate(valuationDate[loopvaldate])
          .indexCurves(ImmutableMap.of(USD_FED_FUND, FED_FUND_CURVE))
          .timeSeries(ImmutableMap.of(USD_FED_FUND, TIME_SERIES_BUILDER.build()))
          .dayCount(ACT_ACT_ISDA)
          .build();
      PointSensitivityBuilder sensitivityBuilderComputed =
          OBS_FWD_ONCMP.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, prov);
      CurveParameterSensitivity parameterSensitivityComputed =
          prov.parameterSensitivity(sensitivityBuilderComputed.build());
      CurveParameterSensitivity parameterSensitivityExpected =
          CAL_FD.sensitivity(prov, (p) -> CurrencyAmount.of(USD_FED_FUND.getCurrency(),
              OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(parameterSensitivityExpected, EPS_FD * 10.0));
    }
  }

}
