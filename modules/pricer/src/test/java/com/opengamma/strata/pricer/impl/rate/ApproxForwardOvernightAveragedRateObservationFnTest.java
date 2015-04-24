/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.finance.rate.OvernightAveragedRateObservation;
import com.opengamma.strata.pricer.CurveSensitivityTestUtil;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Test {@link ApproxForwardOvernightAveragedRateObservationFn}.
 */
public class ApproxForwardOvernightAveragedRateObservationFnTest {

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
  private static final double[] FORWARD_RATES = {
      0.0112, 0.0123, 0.0134,
      0.0145, 0.0156, 0.0167, 0.0178};

  private static final double TOLERANCE_RATE = 1.0E-10;
  private static final double TOLERANCE_APPROX = 1.0E-6;
  private static final double EPS_FD = 1.0E-7;

  private static final ApproxForwardOvernightAveragedRateObservationFn OBS_FN_APPROX_FWD =
      ApproxForwardOvernightAveragedRateObservationFn.DEFAULT;
  private static final ForwardOvernightAveragedRateObservationFn OBS_FN_DET_FWD =
      ForwardOvernightAveragedRateObservationFn.DEFAULT;

  //-------------------------------------------------------------------------
  /** Compare the rate estimated with approximation to the rate estimated by daily forward. */
  @Test
  public void comparisonApproxVNoApprox() {
    LocalDate valuationDate = date(2015, 1, 5);
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.getValuationDate()).thenReturn(valuationDate);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double investmentFactor = 1.0;
    double totalAf = 0.0;
    for (int i = 1; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      totalAf += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / totalAf;
    when(mockEnv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    double rateApprox = OBS_FN_APPROX_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
    double rateDet = OBS_FN_DET_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
    assertEquals(rateDet, rateApprox, TOLERANCE_APPROX);
  }

  /** No cutoff period and the period entirely forward. Test the approximation part only. */
  @Test
  public void rateFedFundNoCutOffForward() { // publication=1, cutoff=0, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double investmentFactor = 1.0;
    double totalAf = 0.0;
    for (int i = 1; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      totalAf += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / totalAf;
    when(mockEnv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    double rateExpected = Math.log(1.0 + rateCmp * totalAf) / totalAf;
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation. No cutoff period and the period entirely forward. */
  @Test
  public void rateFedFundNoCutOffForwardSensitivity() { // publication=1, cutoff=0, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    PricingEnvironment mockEnvUp = mock(PricingEnvironment.class);
    PricingEnvironment mockEnvDw = mock(PricingEnvironment.class);
    double investmentFactor = 1.0;
    double totalAf = 0.0;
    for (int i = 1; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      totalAf += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / totalAf;
    when(mockEnv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    when(mockEnvUp.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateCmp + EPS_FD);
    when(mockEnvDw.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateCmp - EPS_FD);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_START_DATE, FIXING_END_DATE, 1d);
    when(mockEnv.overnightIndexRatePeriodSensitivity(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE))
        .thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockEnvUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockEnvDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FN_APPROX_FWD.rate(mockEnvUp, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      double rateDw = OBS_FN_APPROX_FWD.rate(mockEnvDw, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), FIXING_START_DATE, FIXING_END_DATE, sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FN_APPROX_FWD.rateSensitivity(mockEnv, ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      CurveSensitivityTestUtil.assertMulticurveSensitivity(sensitivityBuilderComputed.build(),
          sensitivityBuilderExpected.build(), EPS_FD);
    }
  }

  /** Two days cutoff and the period is entirely forward. Test Approximation part plus cutoff specifics.*/
  @Test
  public void rateFedFund2CutOffForward() { // publication=1, cutoff=2, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double investmentFactor = 1.0;
    double afApprox = 0.0;
    for (int i = 1; i < 5; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afApprox += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afApprox;
    when(mockEnv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = USD_FED_FUND.getDayCount().yearFraction(fixingCutOff, endDate);
    double rateExpected = (Math.log(1.0 + rateCmp * afApprox) + FORWARD_RATES[4] * afCutOff) / (afApprox + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation.  
   * Two days cutoff and the period is entirely forward. Test Approximation part plus cutoff specifics.*/
  @Test
  public void rateFedFund2CutOffForwardSensitivity() { // publication=1, cutoff=2, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    int nRates = FIXING_DATES.length;
    PricingEnvironment[] mockEnvUp = new PricingEnvironment[nRates];
    PricingEnvironment[] mockEnvDw = new PricingEnvironment[nRates];
    PricingEnvironment mockEnvPeriodUp = mock(PricingEnvironment.class);
    PricingEnvironment mockEnvPeriodDw = mock(PricingEnvironment.class);
    for (int i = 0; i < nRates; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
      PointSensitivityBuilder pointSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockEnv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(pointSensitivity);
    }
    double investmentFactor = 1.0;
    double afApprox = 0.0;
    for (int i = 1; i < 5; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afApprox += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afApprox;
    when(mockEnv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    PointSensitivityBuilder pointSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), 1d);
    when(mockEnv.overnightIndexRatePeriodSensitivity(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(pointSensitivity);
    setPricingEnvironments(mockEnvUp, mockEnvDw, mockEnvPeriodUp, mockEnvPeriodDw, ro, USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), rateCmp);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockEnvPeriodUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockEnvPeriodDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FN_APPROX_FWD.rateSensitivity(mockEnv, ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);

      PointSensitivityBuilder sensitivityBuilderExpected1 = PointSensitivityBuilder.none();
      for (int i = 0; i < nRates; ++i) {
        when(mockEnvUp[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        when(mockEnvDw[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        double rateUp = OBS_FN_APPROX_FWD.rate(mockEnvUp[i], ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
        double rateDw = OBS_FN_APPROX_FWD.rate(mockEnvDw[i], ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
        double res = 0.5 * (rateUp - rateDw) / EPS_FD;
        LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
        LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
        sensitivityBuilderExpected1 = res == 0.0 ? sensitivityBuilderExpected1 : sensitivityBuilderExpected1
            .combinedWith(OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(), FIXING_DATES[i],
                fixingEndDate, res));
      }
      double ratePeriodUp = OBS_FN_APPROX_FWD.rate(mockEnvPeriodUp, ro, DUMMY_ACCRUAL_START_DATE,
          DUMMY_ACCRUAL_END_DATE);
      double ratePeriodDw = OBS_FN_APPROX_FWD.rate(mockEnvPeriodDw, ro, DUMMY_ACCRUAL_START_DATE,
          DUMMY_ACCRUAL_END_DATE);
      double periodSensitivityExpected = 0.5 * (ratePeriodUp - ratePeriodDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected2 = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE),
          periodSensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderExpected = sensitivityBuilderExpected1
          .combinedWith(sensitivityBuilderExpected2);

      CurveSensitivityTestUtil.assertMulticurveSensitivity(sensitivityBuilderComputed.build(),
          sensitivityBuilderExpected.build(), EPS_FD);
    }
  }

  private void setPricingEnvironments(PricingEnvironment[] mockEnvUp, PricingEnvironment[] mockEnvDw,
      PricingEnvironment mockEnvPeriodUp, PricingEnvironment mockEnvPeriodDw,
      OvernightAveragedRateObservation ro, OvernightIndex index, LocalDate periodStartDate, LocalDate PeriodEndDate,
      double rateCmp) {
    int nRates = FIXING_DATES.length;
    double[][] ratesUp = new double[nRates][];
    double[][] ratesDw = new double[nRates][];
    for (int i = 0; i < nRates; ++i) {
      mockEnvUp[i] = mock(PricingEnvironment.class);
      mockEnvDw[i] = mock(PricingEnvironment.class);
      ratesUp[i] = Arrays.copyOf(FIXING_RATES, nRates);
      ratesDw[i] = Arrays.copyOf(FIXING_RATES, nRates);
      ratesUp[i][i] += EPS_FD;
      ratesDw[i][i] -= EPS_FD;
    }
    for (int i = 0; i < nRates; i++) {
      when(mockEnvPeriodUp.overnightIndexRate(index, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      when(mockEnvPeriodDw.overnightIndexRate(index, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      when(mockEnvUp[i].overnightIndexRatePeriod(index, FIXING_START_DATE,
          index.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
      when(mockEnvDw[i].overnightIndexRatePeriod(index, FIXING_START_DATE,
          index.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
      for (int j = 0; j < nRates; ++j) {
        when(mockEnvUp[j].overnightIndexRate(index, FIXING_DATES[i])).thenReturn(ratesUp[j][i]);
        when(mockEnvDw[j].overnightIndexRate(index, FIXING_DATES[i])).thenReturn(ratesDw[j][i]);
      }
    }
    when(mockEnvPeriodUp.overnightIndexRatePeriod(index, periodStartDate, PeriodEndDate)).thenReturn(
        rateCmp + EPS_FD);
    when(mockEnvPeriodDw.overnightIndexRatePeriod(index, periodStartDate, PeriodEndDate)).thenReturn(
        rateCmp - EPS_FD);
  }

  /** Two days cutoff and one already fixed ON rate. Test the already fixed portion with only one fixed ON rate.*/
  @Test
  public void rateFedFund2CutOffValuation1() {
    // publication=1, cutoff=2, effective offset=0, TS: Fixing 1
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < 2; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < 2; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = 2; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    LocalDate fixingknown = FIXING_DATES[1];
    LocalDate endDateKnown = USD_FED_FUND.calculateMaturityFromEffective(fixingknown);
    double afKnown = USD_FED_FUND.getDayCount().yearFraction(fixingknown, endDateKnown);
    double investmentFactor = 1.0;
    double afApprox = 0.0;
    for (int i = 2; i < 5; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afApprox += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afApprox;
    when(mockEnv.overnightIndexRatePeriod(USD_FED_FUND, USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDateCutOff = USD_FED_FUND.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = USD_FED_FUND.getDayCount().yearFraction(fixingCutOff, endDateCutOff);
    double rateExpected = (FIXING_RATES[1] * afKnown + Math.log(1.0 + rateCmp * afApprox) + FORWARD_RATES[4] * afCutOff)
        / (afKnown + afApprox + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation.  
   * Two days cutoff and one already fixed ON rate. Test the already fixed portion with only one fixed ON rate.*/
  @Test
  public void rateFedFund2CutOffValuation1Sensitivity() {
    // publication=1, cutoff=2, effective offset=0, TS: Fixing 1
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    for (int i = 0; i < 2; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    int nRates = FIXING_DATES.length;
    PricingEnvironment[] mockEnvUp = new PricingEnvironment[nRates];
    PricingEnvironment[] mockEnvDw = new PricingEnvironment[nRates];
    PricingEnvironment mockEnvPeriodUp = mock(PricingEnvironment.class);
    PricingEnvironment mockEnvPeriodDw = mock(PricingEnvironment.class);
    when(mockEnv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < 2; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockEnv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(
          PointSensitivityBuilder.none());
    }
    for (int i = 2; i < nRates; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
      PointSensitivityBuilder pointSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockEnv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(pointSensitivity);
    }
    double investmentFactor = 1.0;
    double afApprox = 0.0;
    for (int i = 2; i < 5; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afApprox += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afApprox;
    when(mockEnv.overnightIndexRatePeriod(USD_FED_FUND, USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), 1d);
    when(mockEnv.overnightIndexRatePeriodSensitivity(USD_FED_FUND,
            USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(periodSensitivity);
    setPricingEnvironments(mockEnvUp, mockEnvDw, mockEnvPeriodUp, mockEnvPeriodDw, ro, USD_FED_FUND, USD_FED_FUND
        .getFixingCalendar().next(FIXING_START_DATE), USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE),
        rateCmp);
    when(mockEnvPeriodUp.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    when(mockEnvPeriodDw.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < 2; i++) {
      when(mockEnvPeriodUp.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockEnvPeriodDw.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      for (int j = 0; j < nRates; ++j) {
        when(mockEnvUp[j].overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
        when(mockEnvDw[j].overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      }
    }
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockEnvPeriodUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockEnvPeriodDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FN_APPROX_FWD.rateSensitivity(mockEnv, ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);

      PointSensitivityBuilder sensitivityBuilderExpected1 = PointSensitivityBuilder.none();
      for (int i = 0; i < nRates; ++i) {
        when(mockEnvUp[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        when(mockEnvDw[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        double rateUp = OBS_FN_APPROX_FWD.rate(mockEnvUp[i], ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
        double rateDw = OBS_FN_APPROX_FWD.rate(mockEnvDw[i], ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
        double res = 0.5 * (rateUp - rateDw) / EPS_FD;
        LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
        LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
        sensitivityBuilderExpected1 = res == 0.0 ? sensitivityBuilderExpected1 : sensitivityBuilderExpected1
            .combinedWith(OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(), FIXING_DATES[i],
                fixingEndDate, res));
      }
      double ratePeriodUp = OBS_FN_APPROX_FWD.rate(mockEnvPeriodUp, ro, DUMMY_ACCRUAL_START_DATE,
          DUMMY_ACCRUAL_END_DATE);
      double ratePeriodDw = OBS_FN_APPROX_FWD.rate(mockEnvPeriodDw, ro, DUMMY_ACCRUAL_START_DATE,
          DUMMY_ACCRUAL_END_DATE);
      double periodSensitivityExpected = 0.5 * (ratePeriodUp - ratePeriodDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected2 = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
          USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), periodSensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderExpected = sensitivityBuilderExpected1
          .combinedWith(sensitivityBuilderExpected2);

      CurveSensitivityTestUtil.assertMulticurveSensitivity(sensitivityBuilderComputed.build(),
          sensitivityBuilderExpected.build(), EPS_FD);
    }
  }

  /** Two days cutoff and two already fixed ON rate. ON index is Fed Fund. */
  @Test
  public void rateFedFund2CutOffValuation2() {
    // publication=1, cutoff=2, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 12), date(2015, 1, 13)};
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0;
    double accruedKnown = 0.0;
    for (int i = 0; i < lastFixing - 1; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = USD_FED_FUND.calculateMaturityFromEffective(fixingknown);
      double af = USD_FED_FUND.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      accruedKnown += FIXING_RATES[i + 1] * af;
    }
    double investmentFactor = 1.0;
    double afApprox = 0.0;
    for (int i = lastFixing; i < 5; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afApprox += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afApprox;
    when(mockEnv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_DATES[lastFixing],
        FIXING_DATES[5])).thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDateCutOff = USD_FED_FUND.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = USD_FED_FUND.getDayCount().yearFraction(fixingCutOff, endDateCutOff);
    double rateExpected = (accruedKnown + Math.log(1.0 + rateCmp * afApprox) + FORWARD_RATES[4] * afCutOff)
        / (afKnown + afApprox + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Two days cutoff and two already fixed ON rate. ON index is SONIA. */
  @Test
  public void rateSonia2CutOffValuation2() {
    // publication=0, cutoff=2, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(GBP_SONIA, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockEnv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0;
    double accruedKnown = 0.0;
    for (int i = 0; i < lastFixing - 1; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = GBP_SONIA.calculateMaturityFromEffective(fixingknown);
      double af = GBP_SONIA.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      accruedKnown += FIXING_RATES[i + 1] * af;
    }
    double investmentFactor = 1.0;
    double afApprox = 0.0;
    for (int i = lastFixing; i < 5; i++) {
      LocalDate endDate = GBP_SONIA.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = GBP_SONIA.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afApprox += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afApprox;
    when(mockEnv.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[5])).thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDateCutOff = GBP_SONIA.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = GBP_SONIA.getDayCount().yearFraction(fixingCutOff, endDateCutOff);
    double rateExpected = (accruedKnown + Math.log(1.0 + rateCmp * afApprox) + FORWARD_RATES[4] * afCutOff)
        / (afKnown + afApprox + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** No cutoff period and two already fixed ON rate. ON index is SONIA. */
  @Test
  public void rateSonia0CutOffValuation2() {
    // publication=0, cutoff=0, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12)};
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(GBP_SONIA, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockEnv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0;
    double accruedKnown = 0.0;
    for (int i = 0; i < lastFixing - 1; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = GBP_SONIA.calculateMaturityFromEffective(fixingknown);
      double af = GBP_SONIA.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      accruedKnown += FIXING_RATES[i + 1] * af;
    }
    double investmentFactor = 1.0;
    double afApprox = 0.0;
    for (int i = lastFixing; i < 6; i++) {
      LocalDate endDate = GBP_SONIA.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = GBP_SONIA.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afApprox += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afApprox;
    when(mockEnv.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp);
    double rateExpected = (accruedKnown + Math.log(1.0 + rateCmp * afApprox))
        / (afKnown + afApprox);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** One past fixing missing. Checking the error thrown. */
  @Test
  public void rateFedFund2CutOffValuation2MissingFixing() {
    // publication=1, cutoff=2, effective offset=0, TS: Fixing 2
    LocalDate valuationDate = date(2015, 1, 13);
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 2;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    when(mockEnv.getValuationDate()).thenReturn(valuationDate);
    assertThrows(
        () -> OBS_FN_APPROX_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE),
        PricingException.class);
    assertThrows(
        () -> OBS_FN_APPROX_FWD.rateSensitivity(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE),
        PricingException.class);
  }

  /** Two days cutoff, all ON rates already fixed. */
  @Test
  public void rateFedFund2CutOffValuationEnd() {
    // publication=1, cutoff=2, effective offset=0, TS: Fixing all
    LocalDate[] valuationDate = {date(2015, 1, 15), date(2015, 1, 16)};
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 6;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
    }
    double afKnown = 0.0;
    double accruedKnown = 0.0;
    for (int i = 0; i < 4; i++) {
      LocalDate fixingknown = FIXING_DATES[i + 1];
      LocalDate endDateKnown = USD_FED_FUND.calculateMaturityFromEffective(fixingknown);
      double af = USD_FED_FUND.getDayCount().yearFraction(fixingknown, endDateKnown);
      afKnown += af;
      accruedKnown += FIXING_RATES[i + 1] * af;
    }
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDateCutOff = USD_FED_FUND.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = USD_FED_FUND.getDayCount().yearFraction(fixingCutOff, endDateCutOff);
    double rateExpected = (accruedKnown + FIXING_RATES[4] * afCutOff)
        / (afKnown + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(mockEnv, ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate Sensitivity. Two days cutoff, all ON rates already fixed. Thus none is expected*/
  @Test
  public void rateFedFund2CutOffValuationEndSensitivity() {
    // publication=1, cutoff=2, effective offset=0, TS: Fixing all
    LocalDate[] valuationDate = {date(2015, 1, 15), date(2015, 1, 16) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 6;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockEnv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderExpected = OBS_FN_APPROX_FWD.rateSensitivity(mockEnv, ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE);
      assertEquals(sensitivityBuilderExpected, PointSensitivityBuilder.none());
    }
  }
}
