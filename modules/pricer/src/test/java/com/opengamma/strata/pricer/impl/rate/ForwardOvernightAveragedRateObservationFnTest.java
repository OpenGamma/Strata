/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import static com.opengamma.strata.basics.index.OvernightIndices.CHF_TOIS;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.finance.rate.OvernightAveragedRateObservation;
import com.opengamma.strata.pricer.RatesProvider;

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

}
