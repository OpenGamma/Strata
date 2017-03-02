/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.swap.NegativeRateMethod.ALLOW_NEGATIVE;
import static com.opengamma.strata.product.swap.NegativeRateMethod.NOT_NEGATIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test.
 */
@Test
public class RateAccrualPeriodTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_2014_03_28 = date(2014, 3, 28);
  private static final LocalDate DATE_2014_03_30 = date(2014, 3, 30);
  private static final LocalDate DATE_2014_03_31 = date(2014, 3, 31);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_07_01 = date(2014, 7, 1);
  private static final IborRateComputation GBP_LIBOR_3M_2014_03_27 =
      IborRateComputation.of(GBP_LIBOR_3M, date(2014, 3, 27), REF_DATA);
  private static final IborRateComputation GBP_LIBOR_3M_2014_03_28 =
      IborRateComputation.of(GBP_LIBOR_3M, DATE_2014_03_28, REF_DATA);

  public void test_builder() {
    RateAccrualPeriod test = RateAccrualPeriod.builder()
        .startDate(DATE_2014_03_31)
        .endDate(DATE_2014_07_01)
        .unadjustedStartDate(DATE_2014_03_30)
        .unadjustedEndDate(DATE_2014_06_30)
        .yearFraction(0.25d)
        .rateComputation(GBP_LIBOR_3M_2014_03_28)
        .build();
    assertEquals(test.getStartDate(), DATE_2014_03_31);
    assertEquals(test.getEndDate(), DATE_2014_07_01);
    assertEquals(test.getUnadjustedStartDate(), DATE_2014_03_30);
    assertEquals(test.getUnadjustedEndDate(), DATE_2014_06_30);
    assertEquals(test.getYearFraction(), 0.25d, 0d);
    assertEquals(test.getRateComputation(), GBP_LIBOR_3M_2014_03_28);
    assertEquals(test.getGearing(), 1d, 0d);
    assertEquals(test.getSpread(), 0d, 0d);
    assertEquals(test.getNegativeRateMethod(), ALLOW_NEGATIVE);
  }

  public void test_builder_defaultDates() {
    RateAccrualPeriod test = RateAccrualPeriod.builder()
        .startDate(DATE_2014_03_31)
        .endDate(DATE_2014_07_01)
        .yearFraction(0.25d)
        .rateComputation(GBP_LIBOR_3M_2014_03_28)
        .build();
    assertEquals(test.getStartDate(), DATE_2014_03_31);
    assertEquals(test.getEndDate(), DATE_2014_07_01);
    assertEquals(test.getUnadjustedStartDate(), DATE_2014_03_31);
    assertEquals(test.getUnadjustedEndDate(), DATE_2014_07_01);
    assertEquals(test.getYearFraction(), 0.25d, 0d);
    assertEquals(test.getRateComputation(), GBP_LIBOR_3M_2014_03_28);
    assertEquals(test.getGearing(), 1d, 0d);
    assertEquals(test.getSpread(), 0d, 0d);
    assertEquals(test.getNegativeRateMethod(), ALLOW_NEGATIVE);
  }

  public void test_builder_schedulePeriod() {
    SchedulePeriod schedulePeriod = SchedulePeriod.of(DATE_2014_03_31, DATE_2014_07_01, DATE_2014_03_30, DATE_2014_06_30);
    RateAccrualPeriod test = RateAccrualPeriod.builder(schedulePeriod)
        .yearFraction(0.25d)
        .rateComputation(GBP_LIBOR_3M_2014_03_28)
        .build();
    assertEquals(test.getStartDate(), DATE_2014_03_31);
    assertEquals(test.getEndDate(), DATE_2014_07_01);
    assertEquals(test.getUnadjustedStartDate(), DATE_2014_03_30);
    assertEquals(test.getUnadjustedEndDate(), DATE_2014_06_30);
    assertEquals(test.getYearFraction(), 0.25d, 0d);
    assertEquals(test.getRateComputation(), GBP_LIBOR_3M_2014_03_28);
    assertEquals(test.getGearing(), 1d, 0d);
    assertEquals(test.getSpread(), 0d, 0d);
    assertEquals(test.getNegativeRateMethod(), ALLOW_NEGATIVE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RateAccrualPeriod test = RateAccrualPeriod.builder()
        .startDate(DATE_2014_03_31)
        .endDate(DATE_2014_07_01)
        .unadjustedStartDate(DATE_2014_03_30)
        .unadjustedEndDate(DATE_2014_06_30)
        .yearFraction(0.25d)
        .rateComputation(GBP_LIBOR_3M_2014_03_28)
        .build();
    coverImmutableBean(test);
    RateAccrualPeriod test2 = RateAccrualPeriod.builder()
        .startDate(DATE_2014_03_30)
        .endDate(DATE_2014_06_30)
        .unadjustedStartDate(DATE_2014_03_31)
        .unadjustedEndDate(DATE_2014_07_01)
        .yearFraction(0.26d)
        .rateComputation(GBP_LIBOR_3M_2014_03_27)
        .gearing(1.1d)
        .spread(0.25d)
        .negativeRateMethod(NOT_NEGATIVE)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RateAccrualPeriod test = RateAccrualPeriod.builder()
        .startDate(DATE_2014_03_31)
        .endDate(DATE_2014_07_01)
        .unadjustedStartDate(DATE_2014_03_30)
        .unadjustedEndDate(DATE_2014_06_30)
        .yearFraction(0.25d)
        .rateComputation(GBP_LIBOR_3M_2014_03_28)
        .build();
    assertSerialization(test);
  }

}
