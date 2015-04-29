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
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.finance.rate.OvernightAveragedRateObservation;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.rate.RatesProvider;
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
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(valuationDate);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    double rateApprox = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    double rateDet = OBS_FN_DET_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
    assertEquals(rateDet, rateApprox, TOLERANCE_APPROX);
  }

  /** No cutoff period and the period entirely forward. Test the approximation part only. */
  @Test
  public void rateFedFundNoCutOffForward() { // publication=1, cutoff=0, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8)};
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    double rateExpected = Math.log(1.0 + rateCmp * totalAf) / totalAf;
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation. No cutoff period and the period entirely forward. */
  @Test
  public void rateFedFundNoCutOffForwardSensitivity() { // publication=1, cutoff=0, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    RatesProvider mockProv = mock(RatesProvider.class);
    RatesProvider mockProvUp = mock(RatesProvider.class);
    RatesProvider mockProvDw = mock(RatesProvider.class);
    double investmentFactor = 1.0;
    double totalAf = 0.0;
    for (int i = 1; i < 6; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      totalAf += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / totalAf;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(rateCmp);
    when(mockProvUp.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateCmp + EPS_FD);
    when(mockProvDw.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE)).thenReturn(
        rateCmp - EPS_FD);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_START_DATE, FIXING_END_DATE, 1d);
    when(mockProv.overnightIndexRatePeriodSensitivity(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE))
        .thenReturn(periodSensitivity);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp);
      double rateDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw);
      double sensitivityExpected = 0.5 * (rateUp - rateDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), FIXING_START_DATE, FIXING_END_DATE, sensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FN_APPROX_FWD.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
    }
  }

  /** Two days cutoff and the period is entirely forward. Test Approximation part plus cutoff specifics.*/
  @Test
  public void rateFedFund2CutOffForward() { // publication=1, cutoff=2, effective offset=0, Forward
    LocalDate[] valuationDate = {date(2015, 1, 1), date(2015, 1, 8) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    RatesProvider mockProv = mock(RatesProvider.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = USD_FED_FUND.getDayCount().yearFraction(fixingCutOff, endDate);
    double rateExpected = (Math.log(1.0 + rateCmp * afApprox) + FORWARD_RATES[4] * afCutOff) / (afApprox + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
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
    RatesProvider mockProv = mock(RatesProvider.class);
    int nRates = FIXING_DATES.length;
    RatesProvider[] mockProvUp = new RatesProvider[nRates];
    RatesProvider[] mockProvDw = new RatesProvider[nRates];
    RatesProvider mockProvPeriodUp = mock(RatesProvider.class);
    RatesProvider mockProvPeriodDw = mock(RatesProvider.class);
    for (int i = 0; i < nRates; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
      PointSensitivityBuilder pointSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockProv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(pointSensitivity);
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
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    PointSensitivityBuilder pointSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), 1d);
    when(mockProv.overnightIndexRatePeriodSensitivity(USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(pointSensitivity);
    setRatesProviders(mockProvUp, mockProvDw, mockProvPeriodUp, mockProvPeriodDw, ro, USD_FED_FUND, FIXING_START_DATE,
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), rateCmp, null);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FN_APPROX_FWD.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);

      PointSensitivityBuilder sensitivityBuilderExpected1 = PointSensitivityBuilder.none();
      for (int i = 0; i < nRates; ++i) {
        when(mockProvUp[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        when(mockProvDw[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        double rateUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp[i]);
        double rateDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw[i]);
        double res = 0.5 * (rateUp - rateDw) / EPS_FD;
        LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
        LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
        sensitivityBuilderExpected1 = res == 0.0 ? sensitivityBuilderExpected1 : sensitivityBuilderExpected1
            .combinedWith(OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(), FIXING_DATES[i],
                fixingEndDate, res));
      }
      double ratePeriodUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE,
          DUMMY_ACCRUAL_END_DATE, mockProvPeriodUp);
      double ratePeriodDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE,
          DUMMY_ACCRUAL_END_DATE, mockProvPeriodDw);
      double periodSensitivityExpected = 0.5 * (ratePeriodUp - ratePeriodDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected2 = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), FIXING_START_DATE, USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE),
          periodSensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderExpected = sensitivityBuilderExpected1
          .combinedWith(sensitivityBuilderExpected2);

      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
    }
  }

  private void setRatesProviders(RatesProvider[] mockProvUp, RatesProvider[] mockProvDw,
      RatesProvider mockProvPeriodUp, RatesProvider mockProvPeriodDw, OvernightAveragedRateObservation ro,
      OvernightIndex index, LocalDate periodStartDate, LocalDate PeriodEndDate, double rateCmp,
      LocalDateDoubleTimeSeriesBuilder tsb) {
    int nRates = FIXING_DATES.length;
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
      when(mockProvPeriodUp.overnightIndexRate(index, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      when(mockProvPeriodDw.overnightIndexRate(index, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      when(mockProvUp[i].overnightIndexRatePeriod(index, FIXING_START_DATE,
          index.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
      when(mockProvDw[i].overnightIndexRatePeriod(index, FIXING_START_DATE,
          index.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
      for (int j = 0; j < nRates; ++j) {
        when(mockProvUp[j].overnightIndexRate(index, FIXING_DATES[i])).thenReturn(ratesUp[j][i]);
        when(mockProvDw[j].overnightIndexRate(index, FIXING_DATES[i])).thenReturn(ratesDw[j][i]);
      }
    }
    when(mockProvPeriodUp.overnightIndexRatePeriod(index, periodStartDate, PeriodEndDate)).thenReturn(
        rateCmp + EPS_FD);
    when(mockProvPeriodDw.overnightIndexRatePeriod(index, periodStartDate, PeriodEndDate)).thenReturn(
        rateCmp - EPS_FD);
    if (tsb != null) {
      when(mockProvPeriodUp.timeSeries(index)).thenReturn(tsb.build());
      when(mockProvPeriodDw.timeSeries(index)).thenReturn(tsb.build());
      for (int i = 0; i < nRates; i++) {
        when(mockProvUp[i].timeSeries(index)).thenReturn(tsb.build());
        when(mockProvDw[i].timeSeries(index)).thenReturn(tsb.build());
      }
    }
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
    double afApprox = 0.0;
    for (int i = 2; i < 5; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      afApprox += af;
      investmentFactor *= 1.0d + af * FORWARD_RATES[i];
    }
    double rateCmp = (investmentFactor - 1.0d) / afApprox;
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDateCutOff = USD_FED_FUND.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = USD_FED_FUND.getDayCount().yearFraction(fixingCutOff, endDateCutOff);
    double rateExpected = (FIXING_RATES[1] * afKnown + Math.log(1.0 + rateCmp * afApprox) + FORWARD_RATES[4] * afCutOff)
        / (afKnown + afApprox + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
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
    RatesProvider mockProv = mock(RatesProvider.class);
    int nRates = FIXING_DATES.length;
    RatesProvider[] mockProvUp = new RatesProvider[nRates];
    RatesProvider[] mockProvDw = new RatesProvider[nRates];
    RatesProvider mockProvPeriodUp = mock(RatesProvider.class);
    RatesProvider mockProvPeriodDw = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < 2; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockProv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(
          PointSensitivityBuilder.none());
    }
    for (int i = 2; i < nRates; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
      PointSensitivityBuilder pointSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockProv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(pointSensitivity);
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
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(rateCmp);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), 1d);
    when(mockProv.overnightIndexRatePeriodSensitivity(USD_FED_FUND,
            USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
        USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE))).thenReturn(periodSensitivity);
    setRatesProviders(mockProvUp, mockProvDw, mockProvPeriodUp, mockProvPeriodDw, ro, USD_FED_FUND, USD_FED_FUND
        .getFixingCalendar().next(FIXING_START_DATE), USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE),
        rateCmp, tsb);
    for (int i = 0; i < 2; i++) {
      when(mockProvPeriodUp.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockProvPeriodDw.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      for (int j = 0; j < nRates; ++j) {
        when(mockProvUp[j].overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
        when(mockProvDw[j].overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      }
    }
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FN_APPROX_FWD.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);

      PointSensitivityBuilder sensitivityBuilderExpected1 = PointSensitivityBuilder.none();
      for (int i = 0; i < nRates; ++i) {
        when(mockProvUp[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        when(mockProvDw[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        double rateUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp[i]);
        double rateDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw[i]);
        double res = 0.5 * (rateUp - rateDw) / EPS_FD;
        LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
        LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
        sensitivityBuilderExpected1 = res == 0.0 ? sensitivityBuilderExpected1 : sensitivityBuilderExpected1
            .combinedWith(OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(), FIXING_DATES[i],
                fixingEndDate, res));
      }
      double ratePeriodUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE,
          mockProvPeriodUp);
      double ratePeriodDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE,
          mockProvPeriodDw);
      double periodSensitivityExpected = 0.5 * (ratePeriodUp - ratePeriodDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected2 = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), USD_FED_FUND.getFixingCalendar().next(FIXING_START_DATE),
          USD_FED_FUND.getFixingCalendar().previous(FIXING_END_DATE), periodSensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderExpected = sensitivityBuilderExpected1
          .combinedWith(sensitivityBuilderExpected2);

      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
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
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_DATES[lastFixing],
        FIXING_DATES[5])).thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDateCutOff = USD_FED_FUND.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = USD_FED_FUND.getDayCount().yearFraction(fixingCutOff, endDateCutOff);
    double rateExpected = (accruedKnown + Math.log(1.0 + rateCmp * afApprox) + FORWARD_RATES[4] * afCutOff)
        / (afKnown + afApprox + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation.  
   * Two days cutoff and two already fixed ON rate. ON index is Fed Fund. */
  @Test
  public void rateFedFund2CutOffValuation2Sensitivity() {
    // publication=1, cutoff=2, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 12), date(2015, 1, 13) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    int nRates = FIXING_DATES.length;
    RatesProvider[] mockProvUp = new RatesProvider[nRates];
    RatesProvider[] mockProvDw = new RatesProvider[nRates];
    RatesProvider mockProvPeriodUp = mock(RatesProvider.class);
    RatesProvider mockProvPeriodDw = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockProv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(
          PointSensitivityBuilder.none());
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
      PointSensitivityBuilder pointSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockProv.overnightIndexRateSensitivity(USD_FED_FUND, FIXING_DATES[i])).thenReturn(pointSensitivity);
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
    when(mockProv.overnightIndexRatePeriod(USD_FED_FUND, FIXING_DATES[lastFixing],
        FIXING_DATES[5])).thenReturn(rateCmp);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(),
        FIXING_DATES[lastFixing], FIXING_DATES[5], 1d);
    when(mockProv.overnightIndexRatePeriodSensitivity(USD_FED_FUND, FIXING_DATES[lastFixing], FIXING_DATES[5]))
        .thenReturn(periodSensitivity);
    setRatesProviders(mockProvUp, mockProvDw, mockProvPeriodUp, mockProvPeriodDw, ro, USD_FED_FUND,
        FIXING_DATES[lastFixing], FIXING_DATES[5], rateCmp, tsb);
    when(mockProvPeriodUp.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    when(mockProvPeriodDw.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProvPeriodUp.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockProvPeriodDw.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      for (int j = 0; j < nRates; ++j) {
        when(mockProvUp[j].overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
        when(mockProvDw[j].overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      }
    }
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FN_APPROX_FWD.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);

      PointSensitivityBuilder sensitivityBuilderExpected1 = PointSensitivityBuilder.none();
      for (int i = 0; i < nRates; ++i) {
        when(mockProvUp[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        when(mockProvDw[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        double rateUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp[i]);
        double rateDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw[i]);
        double res = 0.5 * (rateUp - rateDw) / EPS_FD;
        LocalDate fixingStartDate = USD_FED_FUND.calculateEffectiveFromFixing(FIXING_DATES[i]);
        LocalDate fixingEndDate = USD_FED_FUND.calculateMaturityFromEffective(fixingStartDate);
        sensitivityBuilderExpected1 = res == 0.0 ? sensitivityBuilderExpected1 : sensitivityBuilderExpected1
            .combinedWith(OvernightRateSensitivity.of(USD_FED_FUND, USD_FED_FUND.getCurrency(), FIXING_DATES[i],
                fixingEndDate, res));
      }
      double ratePeriodUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE,
          mockProvPeriodUp);
      double ratePeriodDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE,
          mockProvPeriodDw);
      double periodSensitivityExpected = 0.5 * (ratePeriodUp - ratePeriodDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected2 = OvernightRateSensitivity.of(USD_FED_FUND,
          USD_FED_FUND.getCurrency(), FIXING_DATES[lastFixing], FIXING_DATES[5], periodSensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderExpected = sensitivityBuilderExpected1
          .combinedWith(sensitivityBuilderExpected2);

      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
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
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockProv.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[5])).thenReturn(rateCmp);
    LocalDate fixingCutOff = FIXING_DATES[5];
    LocalDate endDateCutOff = GBP_SONIA.calculateMaturityFromEffective(fixingCutOff);
    double afCutOff = GBP_SONIA.getDayCount().yearFraction(fixingCutOff, endDateCutOff);
    double rateExpected = (accruedKnown + Math.log(1.0 + rateCmp * afApprox) + FORWARD_RATES[4] * afCutOff)
        / (afKnown + afApprox + afCutOff);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation.  
   * Two days cutoff and two already fixed ON rate. ON index is SONIA. */
  @Test
  public void rateSonia2CutOffValuation2Sensitivity() {
    // publication=0, cutoff=2, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(GBP_SONIA, FIXING_START_DATE, FIXING_END_DATE, 2);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    int nRates = FIXING_DATES.length;
    RatesProvider[] mockProvUp = new RatesProvider[nRates];
    RatesProvider[] mockProvDw = new RatesProvider[nRates];
    RatesProvider mockProvPeriodUp = mock(RatesProvider.class);
    RatesProvider mockProvPeriodDw = mock(RatesProvider.class);
    when(mockProv.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockProv.overnightIndexRateSensitivity(GBP_SONIA, FIXING_DATES[i])).thenReturn(
          PointSensitivityBuilder.none());
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      LocalDate fixingStartDate = GBP_SONIA.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = GBP_SONIA.calculateMaturityFromEffective(fixingStartDate);
      PointSensitivityBuilder pointSensitivity = OvernightRateSensitivity.of(GBP_SONIA, GBP_SONIA.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockProv.overnightIndexRateSensitivity(GBP_SONIA, FIXING_DATES[i])).thenReturn(pointSensitivity);
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
    when(mockProv.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[5])).thenReturn(rateCmp);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.of(GBP_SONIA, GBP_SONIA.getCurrency(),
        FIXING_DATES[lastFixing], FIXING_DATES[5], 1d);
    when(mockProv.overnightIndexRatePeriodSensitivity(GBP_SONIA, FIXING_DATES[lastFixing], FIXING_DATES[5]))
        .thenReturn(periodSensitivity);
    setRatesProviders(mockProvUp, mockProvDw, mockProvPeriodUp, mockProvPeriodDw, ro, GBP_SONIA,
        FIXING_DATES[lastFixing], FIXING_DATES[5], rateCmp, tsb);
    when(mockProvPeriodUp.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    when(mockProvPeriodDw.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProvPeriodUp.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockProvPeriodDw.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      for (int j = 0; j < nRates; ++j) {
        when(mockProvUp[j].overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
        when(mockProvDw[j].overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      }
    }
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FN_APPROX_FWD.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);

      PointSensitivityBuilder sensitivityBuilderExpected1 = PointSensitivityBuilder.none();
      for (int i = 0; i < nRates; ++i) {
        when(mockProvUp[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        when(mockProvDw[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        double rateUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp[i]);
        double rateDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw[i]);
        double res = 0.5 * (rateUp - rateDw) / EPS_FD;
        LocalDate fixingStartDate = GBP_SONIA.calculateEffectiveFromFixing(FIXING_DATES[i]);
        LocalDate fixingEndDate = GBP_SONIA.calculateMaturityFromEffective(fixingStartDate);
        sensitivityBuilderExpected1 = res == 0.0 ? sensitivityBuilderExpected1 : sensitivityBuilderExpected1
            .combinedWith(OvernightRateSensitivity.of(GBP_SONIA, GBP_SONIA.getCurrency(), FIXING_DATES[i],
                fixingEndDate, res));
      }
      double ratePeriodUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE,
          mockProvPeriodUp);
      double ratePeriodDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE,
          mockProvPeriodDw);
      double periodSensitivityExpected = 0.5 * (ratePeriodUp - ratePeriodDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected2 = OvernightRateSensitivity.of(GBP_SONIA,
          GBP_SONIA.getCurrency(), FIXING_DATES[lastFixing], FIXING_DATES[5], periodSensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderExpected = sensitivityBuilderExpected1
          .combinedWith(sensitivityBuilderExpected2);

      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
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
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
    when(mockProv.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp);
    double rateExpected = (accruedKnown + Math.log(1.0 + rateCmp * afApprox))
        / (afKnown + afApprox);
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
    }
  }

  /** Test rate sensitivity against FD approximation. 
   * No cutoff period and two already fixed ON rate. ON index is SONIA. */
  @Test
  public void rateSonia0CutOffValuation2Sensitivity() {
    // publication=0, cutoff=0, effective offset=0, TS: Fixing 2
    LocalDate[] valuationDate = {date(2015, 1, 9), date(2015, 1, 12) };
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(GBP_SONIA, FIXING_START_DATE, FIXING_END_DATE, 0);
    LocalDateDoubleTimeSeriesBuilder tsb = LocalDateDoubleTimeSeries.builder();
    int lastFixing = 3;
    for (int i = 0; i < lastFixing; i++) {
      tsb.put(FIXING_DATES[i], FIXING_RATES[i]);
    }
    RatesProvider mockProv = mock(RatesProvider.class);
    int nRates = FIXING_DATES.length;
    RatesProvider[] mockProvUp = new RatesProvider[nRates];
    RatesProvider[] mockProvDw = new RatesProvider[nRates];
    RatesProvider mockProvPeriodUp = mock(RatesProvider.class);
    RatesProvider mockProvPeriodDw = mock(RatesProvider.class);
    when(mockProv.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockProv.overnightIndexRateSensitivity(GBP_SONIA, FIXING_DATES[i])).thenReturn(
          PointSensitivityBuilder.none());
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
      LocalDate fixingStartDate = GBP_SONIA.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate fixingEndDate = GBP_SONIA.calculateMaturityFromEffective(fixingStartDate);
      PointSensitivityBuilder pointSensitivity = OvernightRateSensitivity.of(GBP_SONIA, GBP_SONIA.getCurrency(),
          FIXING_DATES[i], fixingEndDate, 1d);
      when(mockProv.overnightIndexRateSensitivity(GBP_SONIA, FIXING_DATES[i])).thenReturn(pointSensitivity);
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
    when(mockProv.overnightIndexRatePeriod(GBP_SONIA, FIXING_DATES[lastFixing],
        FIXING_DATES[6])).thenReturn(rateCmp);
    PointSensitivityBuilder periodSensitivity = OvernightRateSensitivity.of(GBP_SONIA, GBP_SONIA.getCurrency(),
        FIXING_DATES[lastFixing], FIXING_DATES[6], 1d);
    when(mockProv.overnightIndexRatePeriodSensitivity(GBP_SONIA, FIXING_DATES[lastFixing], FIXING_DATES[6]))
        .thenReturn(periodSensitivity);
    setRatesProviders(mockProvUp, mockProvDw, mockProvPeriodUp, mockProvPeriodDw, ro, GBP_SONIA,
        FIXING_DATES[lastFixing], FIXING_DATES[6], rateCmp, tsb);
    when(mockProvPeriodUp.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    when(mockProvPeriodDw.timeSeries(GBP_SONIA)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProvPeriodUp.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      when(mockProvPeriodDw.overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      for (int j = 0; j < nRates; ++j) {
        when(mockProvUp[j].overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
        when(mockProvDw[j].overnightIndexRate(GBP_SONIA, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
      }
    }
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodUp.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      when(mockProvPeriodDw.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderComputed = OBS_FN_APPROX_FWD.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);

      PointSensitivityBuilder sensitivityBuilderExpected1 = PointSensitivityBuilder.none();
      for (int i = 0; i < nRates; ++i) {
        when(mockProvUp[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        when(mockProvDw[i].getValuationDate()).thenReturn(valuationDate[loopvaldate]);
        double rateUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvUp[i]);
        double rateDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProvDw[i]);
        double res = 0.5 * (rateUp - rateDw) / EPS_FD;
        LocalDate fixingStartDate = GBP_SONIA.calculateEffectiveFromFixing(FIXING_DATES[i]);
        LocalDate fixingEndDate = GBP_SONIA.calculateMaturityFromEffective(fixingStartDate);
        sensitivityBuilderExpected1 = res == 0.0 ? sensitivityBuilderExpected1 : sensitivityBuilderExpected1
            .combinedWith(OvernightRateSensitivity.of(GBP_SONIA, GBP_SONIA.getCurrency(), FIXING_DATES[i],
                fixingEndDate, res));
      }
      double ratePeriodUp = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE,
          mockProvPeriodUp);
      double ratePeriodDw = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE,
          mockProvPeriodDw);
      double periodSensitivityExpected = 0.5 * (ratePeriodUp - ratePeriodDw) / EPS_FD;
      PointSensitivityBuilder sensitivityBuilderExpected2 = OvernightRateSensitivity.of(GBP_SONIA,
          GBP_SONIA.getCurrency(), FIXING_DATES[lastFixing], FIXING_DATES[6], periodSensitivityExpected);
      PointSensitivityBuilder sensitivityBuilderExpected = sensitivityBuilderExpected1
          .combinedWith(sensitivityBuilderExpected2);

      assertTrue(sensitivityBuilderComputed.build().normalized().equalWithTolerance(
          sensitivityBuilderExpected.build().normalized(), EPS_FD));
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
        () -> OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv),
        PricingException.class);
    assertThrows(
        () -> OBS_FN_APPROX_FWD.rateSensitivity(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv),
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
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int i = 0; i < lastFixing; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    for (int i = lastFixing; i < FIXING_DATES.length; i++) {
      when(mockProv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FORWARD_RATES[i]);
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
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      double rateComputed = OBS_FN_APPROX_FWD.rate(ro, DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
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
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.timeSeries(USD_FED_FUND)).thenReturn(tsb.build());
    for (int loopvaldate = 0; loopvaldate < 2; loopvaldate++) {
      when(mockProv.getValuationDate()).thenReturn(valuationDate[loopvaldate]);
      PointSensitivityBuilder sensitivityBuilderExpected = OBS_FN_APPROX_FWD.rateSensitivity(ro,
          DUMMY_ACCRUAL_START_DATE, DUMMY_ACCRUAL_END_DATE, mockProv);
      assertEquals(sensitivityBuilderExpected, PointSensitivityBuilder.none());
    }
  }
}
