/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_BUSINESS_DAY;
import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_DAY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link PeriodAdjustment}.
 */
public class PeriodAdjustmentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final PeriodAdditionConvention PAC_NONE = PeriodAdditionConventions.NONE;
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendarIds.SAT_SUN);

  //-------------------------------------------------------------------------
  @Test
  public void test_NONE() {
    PeriodAdjustment test = PeriodAdjustment.NONE;
    assertThat(test.getPeriod()).isEqualTo(Period.ZERO);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("P0D");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_additionConventionNone() {
    PeriodAdjustment test = PeriodAdjustment.of(Period.of(1, 2, 3), PAC_NONE, BDA_NONE);
    assertThat(test.getPeriod()).isEqualTo(Period.of(1, 2, 3));
    assertThat(test.getAdditionConvention()).isEqualTo(PAC_NONE);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("P1Y2M3D");
  }

  @Test
  public void test_of_additionConventionLastDay() {
    PeriodAdjustment test = PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertThat(test.getPeriod()).isEqualTo(Period.ofMonths(3));
    assertThat(test.getAdditionConvention()).isEqualTo(LAST_DAY);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toString()).isEqualTo("P3M with LastDay then apply Following using calendar Sat/Sun");
  }

  @Test
  public void test_ofLastDay() {
    PeriodAdjustment test = PeriodAdjustment.ofLastDay(Period.ofMonths(3), BDA_FOLLOW_SAT_SUN);
    assertThat(test.getPeriod()).isEqualTo(Period.ofMonths(3));
    assertThat(test.getAdditionConvention()).isEqualTo(LAST_DAY);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toString()).isEqualTo("P3M with LastDay then apply Following using calendar Sat/Sun");
  }

  @Test
  public void test_ofLastBusinessDay() {
    PeriodAdjustment test = PeriodAdjustment.ofLastBusinessDay(Period.ofMonths(3), BDA_FOLLOW_SAT_SUN);
    assertThat(test.getPeriod()).isEqualTo(Period.ofMonths(3));
    assertThat(test.getAdditionConvention()).isEqualTo(LAST_BUSINESS_DAY);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toString()).isEqualTo("P3M with LastBusinessDay then apply Following using calendar Sat/Sun");
  }

  @Test
  public void test_of_invalid_conventionForPeriod() {
    Period period = Period.of(1, 2, 3);
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodAdjustment.of(period, LAST_DAY, BDA_NONE));
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodAdjustment.of(period, LAST_BUSINESS_DAY, BDA_NONE));
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodAdjustment.ofLastDay(period, BDA_NONE));
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodAdjustment.ofLastBusinessDay(period, BDA_NONE));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_adjust() {
    return new Object[][] {
        // not last day
        {0, date(2014, 8, 15), date(2014, 8, 15)},
        {1, date(2014, 8, 15), date(2014, 9, 15)},
        {2, date(2014, 8, 15), date(2014, 10, 15)},
        {3, date(2014, 8, 15), date(2014, 11, 17)},
        {-1, date(2014, 8, 15), date(2014, 7, 15)},
        {-2, date(2014, 8, 15), date(2014, 6, 16)},
        // last day
        {1, date(2014, 2, 28), date(2014, 3, 31)},
        {1, date(2014, 6, 30), date(2014, 7, 31)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_adjust")
  public void test_adjust(int months, LocalDate date, LocalDate expected) {
    PeriodAdjustment test = PeriodAdjustment.of(Period.ofMonths(months), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertThat(test.adjust(date, REF_DATA)).isEqualTo(expected);
    assertThat(test.resolve(REF_DATA).adjust(date)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void equals() {
    PeriodAdjustment a = PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    PeriodAdjustment b = PeriodAdjustment.of(Period.ofMonths(1), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    PeriodAdjustment c = PeriodAdjustment.of(Period.ofMonths(3), PAC_NONE, BDA_FOLLOW_SAT_SUN);
    PeriodAdjustment d = PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_NONE);
    assertThat(a.equals(b)).isEqualTo(false);
    assertThat(a.equals(c)).isEqualTo(false);
    assertThat(a.equals(d)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_beanBuilder() {
    PeriodAdjustment test = PeriodAdjustment.builder()
        .period(Period.ofMonths(3))
        .additionConvention(LAST_DAY)
        .adjustment(BDA_FOLLOW_SAT_SUN)
        .build();
    assertThat(test.getPeriod()).isEqualTo(Period.ofMonths(3));
    assertThat(test.getAdditionConvention()).isEqualTo(LAST_DAY);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

  @Test
  public void test_serialization() {
    assertSerialization(PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

}
