/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import static com.opengamma.collect.TestHelper.date;
import static com.opengamma.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.basics.index.OvernightIndices.CHF_TOIS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.observation.OvernightAveragedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Test {@link ForwardOvernightAveragedRateObservationFn}.
 */
public class ForwardOvernightAverageRateObservationFnTest {

  private static final LocalDate DUMMY_ACCRUAL_DATE = date(2015, 1, 1); // Accrual dates irrelevant for the rate
  private static final LocalDate FIXING_START_DATE = date(2015, 1, 8);
  private static final LocalDate FIXING_END_DATE = date(2015, 1, 15); // 1w only to decrease data
  private static final LocalDate[] FIXING_DATES = new LocalDate[] {
    date(2015, 1, 7), date(2015, 1, 8), date(2015, 1, 9), 
    date(2015, 1, 12), date(2015, 1, 13), date(2015, 1, 14), date(2015, 1, 15)};
  private static final double[] FIXING_RATES = {
    0.0012, 0.0023, 0.0034, 
    0.0045, 0.0056, 0.0067, 0.0078};
  private static final double TOLERANCE_RATE = 1.0E-10;
  
  @Test
  public void rateFedFundNoCutOff() { // publication=1, cutoff=0, effective offset=0
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 0);
    // Accrual dates = fixing dates
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5;
    for (int i = 1; i < indexLast + 1; i++) {
      LocalDate endDate = USD_FED_FUND.calculateMaturityFromEffective(FIXING_DATES[i]);
      double af = USD_FED_FUND.getDayCount().yearFraction(FIXING_DATES[i], endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(mockEnv, ro, DUMMY_ACCRUAL_DATE, DUMMY_ACCRUAL_DATE);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }
  
  @Test
  public void rateFedFund() { // publication=1, cutoff=2, effective offset=0
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(USD_FED_FUND, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(USD_FED_FUND, FIXING_START_DATE, FIXING_END_DATE, 2);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5;
    for (int i = 1; i < indexLast; i++) {
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
    double rateComputed = obsFn.rate(mockEnv, ro, DUMMY_ACCRUAL_DATE, DUMMY_ACCRUAL_DATE);
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }
  
  @Test
  public void rateChfNoCutOff() { // publication=0, cutoff=0, effective offset=1
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    for (int i = 0; i < FIXING_DATES.length; i++) {
      when(mockEnv.overnightIndexRate(CHF_TOIS, FIXING_DATES[i])).thenReturn(FIXING_RATES[i]);
    }
    OvernightAveragedRateObservation ro =
        OvernightAveragedRateObservation.of(CHF_TOIS, FIXING_START_DATE, FIXING_END_DATE, 0);
    ForwardOvernightAveragedRateObservationFn obsFn = ForwardOvernightAveragedRateObservationFn.DEFAULT;
    double accrualFactorTotal = 0.0d;
    double accruedRate = 0.0d;
    int indexLast = 5;
    for (int i = 1; i < indexLast + 1; i++) {
      LocalDate startDate = CHF_TOIS.calculateEffectiveFromFixing(FIXING_DATES[i]);
      LocalDate endDate = CHF_TOIS.calculateMaturityFromEffective(startDate);
      double af = CHF_TOIS.getDayCount().yearFraction(startDate, endDate);
      accrualFactorTotal += af;
      accruedRate += FIXING_RATES[i] * af;
    }
    double rateExpected = accruedRate / accrualFactorTotal;
    double rateComputed = obsFn.rate(mockEnv, ro, DUMMY_ACCRUAL_DATE, DUMMY_ACCRUAL_DATE); // Accrual irrelevant for rate
    assertEquals(rateExpected, rateComputed, TOLERANCE_RATE);
  }
  
}
