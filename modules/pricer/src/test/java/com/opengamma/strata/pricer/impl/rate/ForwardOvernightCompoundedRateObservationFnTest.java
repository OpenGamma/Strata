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
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.finance.rate.OvernightCompoundedRateObservation;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.OvernightIndexRates;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    double rateCmp = 0.0123;
    when(mockRates.periodRate(FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    double rateExpected = rateCmp;
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** No cutoff period and the period entirely forward. Test the forward part only against FD approximation. */
  @Test
  public void rateFedFundNoCutOffForwardSensitivity() { // publication=1, cutoff=0, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightCompoundedRateObservation ro =
        OvernightCompoundedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    double rateCmp = 0.0123;
    when(mockRates.periodRate(FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    PointSensitivityBuilder rateSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_START_DATE, FIXING_END_DATE, 1.0);
    when(mockRates.periodRatePointSensitivity(FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateSensitivity);
    OvernightIndexRates mockRatesUp = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvUp = new SimpleRatesProvider(mockRatesUp);
    when(mockRatesUp.periodRate(FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateCmp + EPS_FD);
    OvernightIndexRates mockRatesDw = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvDw = new SimpleRatesProvider(mockRatesDw);
    when(mockRatesDw.periodRate(FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateCmp - EPS_FD);

    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), FIXING_START_DATE, FIXING_END_DATE, sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockRates.periodRate(FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE)))
        .thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = USD_FED_FUND.getDayCount().yearFraction(fixingCutOff, endDate);
    double rateExpected = ((1.0 + rateCmp * afNonCutoff) * (1.0d + FORWARD_RATES[4] * afCutOff) - 1.0d)
        / (afNonCutoff + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    int nFixings = FIXING_DATES.length;
    OvernightIndexRates[] mockRatesUp = new OvernightIndexRates[nFixings];
    SimpleRatesProvider[] simpleProvUp = new SimpleRatesProvider[nFixings];
    OvernightIndexRates[] mockRatesDw = new OvernightIndexRates[nFixings];
    SimpleRatesProvider[] simpleProvDw = new SimpleRatesProvider[nFixings];
    OvernightIndexRates mockRatesPeriodUp = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvPeriodUp = new SimpleRatesProvider(mockRatesPeriodUp);
    OvernightIndexRates mockRatesPeriodDw = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvPeriodDw = new SimpleRatesProvider(mockRatesPeriodDw);

    double[][] forwardRatesUp = new double[nFixings][nFixings];
    double[][] forwardRatesDw = new double[nFixings][nFixings];
    for (int i = 0; i < nFixings; i++) {
      mockRatesUp[i] = mock(OvernightIndexRates.class);
      simpleProvUp[i] = new SimpleRatesProvider(mockRatesUp[i]);
      mockRatesDw[i] = mock(OvernightIndexRates.class);
      simpleProvDw[i] = new SimpleRatesProvider(mockRatesDw[i]);
      for (int j = 0; j < nFixings; j++) {
        double rateForUp = i == j ? FORWARD_RATES[j] + EPS_FD : FORWARD_RATES[j];
        double rateForDw = i == j ? FORWARD_RATES[j] - EPS_FD : FORWARD_RATES[j];
        forwardRatesUp[i][j] = rateForUp;
        forwardRatesDw[i][j] = rateForDw;
      }
    }
    for (int i = 0; i < nFixings; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      when(mockRatesPeriodUp.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      when(mockRatesPeriodDw.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
      PointSensitivityBuilder rateSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1.0);
      when(mockRates.pointSensitivity(FIXING_DATES[i])).thenReturn(rateSensitivity);
      for (int j = 0; j < nFixings; ++j) {
        when(mockRatesUp[j].rate(FIXING_DATES[i])).thenReturn(forwardRatesUp[j][i]);
        when(mockRatesDw[j].rate(FIXING_DATES[i])).thenReturn(forwardRatesDw[j][i]);
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
    when(mockRates.periodRate(FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    when(mockRatesPeriodUp.periodRate(
        FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp + EPS_FD);
    when(mockRatesPeriodDw.periodRate(
        FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp - EPS_FD);
    PointSensitivityBuilder rateSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), 1.0);
    when(mockRates.periodRatePointSensitivity(
        FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateSensitivity);
    for (int i = 0; i < nFixings; ++i) {
      when(mockRatesUp[i].periodRate(
          FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
      when(mockRatesDw[i].periodRate(
          FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    }

    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesPeriodUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesPeriodDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderExpected1 = PointSensitivityBuilder.none();
      for (int i = 0; i < nFixings; ++i) {
        when(mockRatesUp[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        when(mockRatesDw[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvUp[i]);
        double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvDw[i]);
        double cutoffSensitivity = 0.5 * (rateUp - rateDw) / EPS_FD; // [4] is nonzero 
        LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
        LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
        sensitivityBuilderExpected1 = cutoffSensitivity == 0.0 ? sensitivityBuilderExpected1
            : sensitivityBuilderExpected1.combinedWith(OvernightRateSensitivity.of(USD_FED_FUND,
                USD_FED_FUND.getCurrency(), FIXING_DATES[i], fixingEndDate, cutoffSensitivity));
      }
      double ratePeriodUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvPeriodUp);
      double ratePeriodDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvPeriodDw);
      double periodSensitivity = 0.5 * (ratePeriodUp - ratePeriodDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected2 = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE),
          periodSensitivity);
      PointSensitivityBuilder sensitivityBuilderExpected = sensitivityBuilderExpected1
          .combinedWith(sensitivityBuilderExpected2);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < 2; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int i = 0; i < 2; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = 2; i < FIXING_DATES.length; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockRates.periodRate(
        USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE), FIXING_END_DATE)).thenReturn(rateCmp);
    double rateExpected = ((1.0d + FIXING_RATES[1] * afKnown) * (1.0 + rateCmp * afNoCutoff) - 1.0d)
        / (afKnown + afNoCutoff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < 2; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());

    OvernightIndexRates mockRatesUp = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvUp = new SimpleRatesProvider(mockRatesUp);
    OvernightIndexRates mockRatesDw = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvDw = new SimpleRatesProvider(mockRatesDw);
    when(mockRatesUp.getTimeSeries()).thenReturn(tsb.build());
    when(mockRatesDw.getTimeSeries()).thenReturn(tsb.build());
    double investmentFactor = 1.0;
    double afNoCutoff = 0.0;
    for (int i = 2; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNoCutoff += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afNoCutoff;
    when(mockRates.periodRate(
        USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE), FIXING_END_DATE)).thenReturn(rateCmp);
    when(mockRatesUp.periodRate(
        USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE), FIXING_END_DATE)).thenReturn(rateCmp + EPS_FD);
    when(mockRatesDw.periodRate(
        USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE), FIXING_END_DATE)).thenReturn(rateCmp - EPS_FD);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE), FIXING_END_DATE, 1.0d);
    when(mockRates.periodRatePointSensitivity(
        USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE), FIXING_END_DATE)).thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE), FIXING_END_DATE,
          sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(GBP_SONIA);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockRates.periodRate(FIXING_DATES[lastFixing], FIXING_DATES[6])).thenReturn(rateCmp);
    double rateExpected = (investmentFactorKnown * (1.0 + rateCmp * afNoCutoff) - 1.0d)
        / (afKnown + afNoCutoff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(GBP_SONIA);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());

    OvernightIndexRates mockRatesUp = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvUp = new SimpleRatesProvider(mockRatesUp);
    OvernightIndexRates mockRatesDw = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvDw = new SimpleRatesProvider(mockRatesDw);
    when(mockRatesUp.getTimeSeries()).thenReturn(tsb.build());
    when(mockRatesDw.getTimeSeries()).thenReturn(tsb.build());
    double afNoCutoff = 0.0d;
    double investmentFactorNoCutoff = 1.0d;
    for (int i = lastFixing; i < 6; i++) {
      LocalDate endDate = GBP_SONIA.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = GBP_SONIA.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNoCutoff += af;
      investmentFactorNoCutoff *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactorNoCutoff - 1.0d) / afNoCutoff;
    when(mockRates.periodRate(
        FIXING_DATES[lastFixing], FIXING_DATES[6])).thenReturn(rateCmp);
    when(mockRatesUp.periodRate(
        FIXING_DATES[lastFixing], FIXING_DATES[6])).thenReturn(rateCmp + EPS_FD);
    when(mockRatesDw.periodRate(
        FIXING_DATES[lastFixing], FIXING_DATES[6])).thenReturn(rateCmp - EPS_FD);
    OvernightRateSensitivity periodSensitivity = OvernightRateSensitivity.of(GBP_SONIA, GBP_SONIA.getCurrency(),
        FIXING_DATES[lastFixing], FIXING_DATES[6], 1.0d);
    when(mockRates.periodRatePointSensitivity(FIXING_DATES[lastFixing], FIXING_DATES[6]))
        .thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      OvernightRateSensitivity sensitivityBuilderExpected = OvernightRateSensitivity.of(GBP_SONIA,
          GBP_SONIA.getCurrency(), FIXING_DATES[lastFixing], FIXING_DATES[6], sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(CHF_TOIS);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockRates.periodRate(
        CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5]))))
        .thenReturn(rateCmp);
    double rateExpected = (investmentFactorKnown * (1.0 + rateCmp * afNoCutoff) - 1.0d)
        / (afKnown + afNoCutoff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(CHF_TOIS);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());

    OvernightIndexRates mockRatesUp = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvUp = new SimpleRatesProvider(mockRatesUp);
    OvernightIndexRates mockRatesDw = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvDw = new SimpleRatesProvider(mockRatesDw);
    when(mockRatesUp.getTimeSeries()).thenReturn(tsb.build());
    when(mockRatesDw.getTimeSeries()).thenReturn(tsb.build());
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
    when(mockRates.periodRate(
        CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5]))))
        .thenReturn(rateCmp);
    when(mockRatesUp.periodRate(
        CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5]))))
        .thenReturn(rateCmp + EPS_FD);
    when(mockRatesDw.periodRate(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5]))))
        .thenReturn(rateCmp - EPS_FD);
    OvernightRateSensitivity periodSensitivity = OvernightRateSensitivity.of(CHF_TOIS, CHF_TOIS.getCurrency(),
        CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5])), 1.0d);
    when(mockRates.periodRatePointSensitivity(
        CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
        CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5]))))
        .thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      OvernightRateSensitivity sensitivityBuilderExpected = OvernightRateSensitivity.of(CHF_TOIS,
          CHF_TOIS.getCurrency(), CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[lastFixing]),
          CHF_TOIS.calculateMaturityFromEffective(CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[5])),
          sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockRates.periodRate(FIXING_DATES[lastFixing], FIXING_DATES[6])).thenReturn(rateCmp);
    double rateExpected = (investmentFactorKnown * (1.0 + rateCmp * afNoCutoff) - 1.0d)
        / (afKnown + afNoCutoff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());

    OvernightIndexRates mockRatesUp = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvUp = new SimpleRatesProvider(mockRatesUp);
    OvernightIndexRates mockRatesDw = mock(OvernightIndexRates.class);
    SimpleRatesProvider simpleProvDw = new SimpleRatesProvider(mockRatesDw);
    when(mockRatesUp.getTimeSeries()).thenReturn(tsb.build());
    when(mockRatesDw.getTimeSeries()).thenReturn(tsb.build());
    double investmentFactor = 1.0;
    double afNoCutoff = 0.0;
    for (int i = lastFixing; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afNoCutoff += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afNoCutoff;
    when(mockRates.periodRate(FIXING_DATES[lastFixing], FIXING_DATES[6])).thenReturn(rateCmp);
    when(mockRatesUp.periodRate(
        FIXING_DATES[lastFixing], FIXING_DATES[6])).thenReturn(rateCmp + EPS_FD);
    when(mockRatesDw.periodRate(
        FIXING_DATES[lastFixing], FIXING_DATES[6])).thenReturn(rateCmp - EPS_FD);
    OvernightRateSensitivity periodSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_DATES[lastFixing], FIXING_DATES[6], 1.0d);
    when(mockRates.periodRatePointSensitivity(FIXING_DATES[lastFixing], FIXING_DATES[6])).thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockRatesDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvUp);
      double rateDw = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      OvernightRateSensitivity sensitivityBuilderExpected = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(),
          FIXING_DATES[lastFixing], FIXING_DATES[6], sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);

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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 6;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 6;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int loopvaldate = 0; loopvaldate < valuationDate.length; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 7;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 7;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int loopvaldate = 0; loopvaldate < valuationDate.length; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 5;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(mockRates);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 5;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int loopvaldate = 0; loopvaldate < 3; loopvaldate++) {
      when(mockRates.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityComputed = OBS_FWD_ONCMP.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv);
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
    OvernightIndexRates mockRates = mock(OvernightIndexRates.class);
    when(mockRates.getIndex()).thenReturn(USD_FED_FUND);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(valuationDate, mockRates);
    when(mockRates.getValuationDate()).thenReturn(valuationDate);

    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 2;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    when(mockRates.getTimeSeries()).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockRates.rate(FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    assertThrows(
        () -> OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv),
        PricingException.class);
    assertThrows(
        () -> OBS_FWD_ONCMP.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, simpleProv),
        PricingException.class);
  }

  //-------------------------------------------------------------------------
  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.DOUBLE_QUADRATIC_INSTANCE;
  private static final Curve FED_FUND_CURVE;
  static {
    double[] time_usd = new double[] {0.0, 0.5, 1.0, 2.0, 5.0, 10.0};
    double[] rate_usd = new double[] {0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135};
    FED_FUND_CURVE = InterpolatedNodalCurve.of("USD-Fed-Fund", time_usd, rate_usd, INTERPOLATOR);
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
      CurveParameterSensitivities parameterSensitivityComputed =
          prov.parameterSensitivity(sensitivityBuilderComputed.build());
      CurveParameterSensitivities parameterSensitivityExpected =
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
      CurveParameterSensitivities parameterSensitivityComputed =
          prov.parameterSensitivity(sensitivityBuilderComputed.build());
      CurveParameterSensitivities parameterSensitivityExpected =
          CAL_FD.sensitivity(prov, (p) -> CurrencyAmount.of(USD_FED_FUND.getCurrency(),
              OBS_FWD_ONCMP.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(parameterSensitivityExpected, EPS_FD * 10.0));
    }
  }

}
