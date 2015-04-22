/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.OvernightIndices.CHF_TOIS;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.finance.rate.OvernightCompoundedRateObservation;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.PricingException;

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
  }

}
