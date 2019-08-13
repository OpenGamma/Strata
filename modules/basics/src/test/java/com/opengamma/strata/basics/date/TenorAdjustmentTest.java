/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_BUSINESS_DAY;
import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_DAY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
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
 * Test {@link TenorAdjustment}.
 */
public class TenorAdjustmentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final PeriodAdditionConvention PAC_NONE = PeriodAdditionConventions.NONE;
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendarIds.SAT_SUN);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_additionConventionNone() {
    TenorAdjustment test = TenorAdjustment.of(Tenor.of(Period.of(1, 2, 3)), PAC_NONE, BDA_NONE);
    assertThat(test.getTenor()).isEqualTo(Tenor.of(Period.of(1, 2, 3)));
    assertThat(test.getAdditionConvention()).isEqualTo(PAC_NONE);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("1Y2M3D");
  }

  @Test
  public void test_of_additionConventionLastDay() {
    TenorAdjustment test = TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getAdditionConvention()).isEqualTo(LAST_DAY);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toString()).isEqualTo("3M with LastDay then apply Following using calendar Sat/Sun");
  }

  @Test
  public void test_ofLastDay() {
    TenorAdjustment test = TenorAdjustment.ofLastDay(TENOR_3M, BDA_FOLLOW_SAT_SUN);
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getAdditionConvention()).isEqualTo(LAST_DAY);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toString()).isEqualTo("3M with LastDay then apply Following using calendar Sat/Sun");
  }

  @Test
  public void test_ofLastBusinessDay() {
    TenorAdjustment test = TenorAdjustment.ofLastBusinessDay(TENOR_3M, BDA_FOLLOW_SAT_SUN);
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getAdditionConvention()).isEqualTo(LAST_BUSINESS_DAY);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toString()).isEqualTo("3M with LastBusinessDay then apply Following using calendar Sat/Sun");
  }

  @Test
  public void test_of_invalid_conventionForPeriod() {
    assertThatIllegalArgumentException().isThrownBy(() -> TenorAdjustment.of(TENOR_1W, LAST_DAY, BDA_NONE));
    assertThatIllegalArgumentException().isThrownBy(() -> TenorAdjustment.of(TENOR_1W, LAST_BUSINESS_DAY, BDA_NONE));
    assertThatIllegalArgumentException().isThrownBy(() -> TenorAdjustment.ofLastDay(TENOR_1W, BDA_NONE));
    assertThatIllegalArgumentException().isThrownBy(() -> TenorAdjustment.ofLastBusinessDay(TENOR_1W, BDA_NONE));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_adjust() {
    return new Object[][] {
        // not last day
        {1, date(2014, 8, 15), date(2014, 9, 15)},
        {2, date(2014, 8, 15), date(2014, 10, 15)},
        {3, date(2014, 8, 15), date(2014, 11, 17)},
        // last day
        {1, date(2014, 2, 28), date(2014, 3, 31)},
        {1, date(2014, 6, 30), date(2014, 7, 31)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_adjust")
  public void test_adjust(int months, LocalDate date, LocalDate expected) {
    TenorAdjustment test = TenorAdjustment.of(Tenor.ofMonths(months), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertThat(test.adjust(date, REF_DATA)).isEqualTo(expected);
    assertThat(test.resolve(REF_DATA).adjust(date)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void equals() {
    TenorAdjustment a = TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    TenorAdjustment b = TenorAdjustment.of(TENOR_1M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    TenorAdjustment c = TenorAdjustment.of(TENOR_3M, PAC_NONE, BDA_FOLLOW_SAT_SUN);
    TenorAdjustment d = TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_NONE);
    assertThat(a.equals(b)).isEqualTo(false);
    assertThat(a.equals(c)).isEqualTo(false);
    assertThat(a.equals(d)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_beanBuilder() {
    TenorAdjustment test = TenorAdjustment.builder()
        .tenor(TENOR_3M)
        .additionConvention(LAST_DAY)
        .adjustment(BDA_FOLLOW_SAT_SUN)
        .build();
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getAdditionConvention()).isEqualTo(LAST_DAY);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

  @Test
  public void test_serialization() {
    assertSerialization(TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

}
