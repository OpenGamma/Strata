/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Period;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SequenceDate}.
 */
public class SequenceDateTest {

  private static final DateSequence SEQUENCE = DateSequences.QUARTERLY_IMM_3_SERIAL;
  private static final YearMonth YM_2020_02 = YearMonth.of(2020, 2);
  private static final YearMonth YM_2020_03 = YearMonth.of(2020, 3);
  private static final Period PERIOD = Period.ofMonths(2);

  @Test
  public void test_base_YearMonth() {
    SequenceDate test = SequenceDate.base(YM_2020_02);
    assertThat(test.getYearMonth()).hasValue(YM_2020_02);
    assertThat(test.getMinimumPeriod()).isEmpty();
    assertThat(test.getSequenceNumber()).isEqualTo(1);
    assertThat(test.isFullSequence()).isFalse();
  }

  @Test
  public void test_base_YearMonth_int() {
    SequenceDate test = SequenceDate.base(YM_2020_02, 2);
    assertThat(test.getYearMonth()).hasValue(YM_2020_02);
    assertThat(test.getMinimumPeriod()).isEmpty();
    assertThat(test.getSequenceNumber()).isEqualTo(2);
    assertThat(test.isFullSequence()).isFalse();

    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(YM_2020_02, 1))).isEqualTo(date(2020, 3, 18));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(YM_2020_02, 2))).isEqualTo(date(2020, 6, 17));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(YM_2020_02, 3))).isEqualTo(date(2020, 9, 16));

    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(YM_2020_03, 1))).isEqualTo(date(2020, 3, 18));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(YM_2020_03, 2))).isEqualTo(date(2020, 6, 17));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(YM_2020_03, 3))).isEqualTo(date(2020, 9, 16));
  }

  @Test
  public void test_base_int() {
    SequenceDate test = SequenceDate.base(2);
    assertThat(test.getYearMonth()).isEmpty();
    assertThat(test.getMinimumPeriod()).isEmpty();
    assertThat(test.getSequenceNumber()).isEqualTo(2);
    assertThat(test.isFullSequence()).isFalse();

    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(1))).isEqualTo(date(2020, 3, 18));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(2))).isEqualTo(date(2020, 6, 17));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(3))).isEqualTo(date(2020, 9, 16));

    assertThat(SEQUENCE.selectDate(date(2020, 3, 18), SequenceDate.base(1))).isEqualTo(date(2020, 6, 17));
    assertThat(SEQUENCE.selectDateOrSame(date(2020, 3, 18), SequenceDate.base(1))).isEqualTo(date(2020, 3, 18));
  }

  @Test
  public void test_base_Period_int() {
    SequenceDate test = SequenceDate.base(PERIOD, 3);
    assertThat(test.getYearMonth()).isEmpty();
    assertThat(test.getMinimumPeriod()).hasValue(PERIOD);
    assertThat(test.getSequenceNumber()).isEqualTo(3);
    assertThat(test.isFullSequence()).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> SequenceDate.base(Period.ofMonths(-1), 3));
    assertThatIllegalArgumentException().isThrownBy(() -> SequenceDate.base(Period.ofDays(-1), 3));

    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(Period.ofMonths(1), 1))).isEqualTo(date(2020, 3, 18));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(Period.ofMonths(2), 1))).isEqualTo(date(2020, 3, 18));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(Period.ofMonths(3), 1))).isEqualTo(date(2020, 6, 17));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(Period.ofMonths(4), 1))).isEqualTo(date(2020, 6, 17));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(Period.ofMonths(5), 1))).isEqualTo(date(2020, 6, 17));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(Period.ofMonths(6), 1))).isEqualTo(date(2020, 9, 16));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.base(Period.ofMonths(7), 1))).isEqualTo(date(2020, 9, 16));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_full_YearMonth() {
    SequenceDate test = SequenceDate.full(YM_2020_02);
    assertThat(test.getYearMonth()).hasValue(YM_2020_02);
    assertThat(test.getMinimumPeriod()).isEmpty();
    assertThat(test.getSequenceNumber()).isEqualTo(1);
    assertThat(test.isFullSequence()).isTrue();
  }

  @Test
  public void test_full_YearMonth_int() {
    SequenceDate test = SequenceDate.full(YM_2020_02, 2);
    assertThat(test.getYearMonth()).hasValue(YM_2020_02);
    assertThat(test.getMinimumPeriod()).isEmpty();
    assertThat(test.getSequenceNumber()).isEqualTo(2);
    assertThat(test.isFullSequence()).isTrue();
  }

  @Test
  public void test_full_int() {
    SequenceDate test = SequenceDate.full(2);
    assertThat(test.getYearMonth()).isEmpty();
    assertThat(test.getMinimumPeriod()).isEmpty();
    assertThat(test.getSequenceNumber()).isEqualTo(2);
    assertThat(test.isFullSequence()).isTrue();

    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.full(1))).isEqualTo(date(2020, 1, 15));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.full(2))).isEqualTo(date(2020, 2, 19));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.full(3))).isEqualTo(date(2020, 3, 18));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.full(4))).isEqualTo(date(2020, 6, 17));
    assertThat(SEQUENCE.selectDate(date(2020, 1, 1), SequenceDate.full(5))).isEqualTo(date(2020, 9, 16));

    assertThat(SEQUENCE.selectDate(date(2020, 3, 18), SequenceDate.full(1))).isEqualTo(date(2020, 4, 15));
    assertThat(SEQUENCE.selectDateOrSame(date(2020, 3, 18), SequenceDate.full(1))).isEqualTo(date(2020, 3, 18));
  }

  @Test
  public void test_full_Period_int() {
    SequenceDate test = SequenceDate.full(PERIOD, 3);
    assertThat(test.getYearMonth()).isEmpty();
    assertThat(test.getMinimumPeriod()).hasValue(PERIOD);
    assertThat(test.getSequenceNumber()).isEqualTo(3);
    assertThat(test.isFullSequence()).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SequenceDate test = SequenceDate.full(PERIOD, 3);
    SequenceDate test2 = SequenceDate.base(YM_2020_02, 2);
    coverImmutableBean(test);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SequenceDate test = SequenceDate.full(PERIOD, 3);
    assertSerialization(test);
  }

}
