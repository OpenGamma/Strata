/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

/**
 * Test {@link FutureValueNotional}.
 */
public class FutureValueNotionalTest {

  private static final double VALUE = 102345d;
  private static final LocalDate VAL_DATE = LocalDate.of(2017, 7, 7);
  private static final int NUM_DAYS = 512;

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    FutureValueNotional test1 = FutureValueNotional.of(VALUE, VAL_DATE, NUM_DAYS);
    assertThat(test1.getValue()).isEqualTo(OptionalDouble.of(VALUE));
    assertThat(test1.getDayCountDays()).isEqualTo(OptionalInt.of(NUM_DAYS));
    assertThat(test1.getValueDate()).isEqualTo(Optional.of(VAL_DATE));

    FutureValueNotional test2 = FutureValueNotional.of(VALUE);
    assertThat(test2.getValue()).isEqualTo(OptionalDouble.of(VALUE));
    assertThat(test2.getDayCountDays()).isEqualTo(OptionalInt.empty());
    assertThat(test2.getValueDate()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_builder() {
    FutureValueNotional test1 = FutureValueNotional.builder()
        .value(VALUE)
        .valueDate(VAL_DATE)
        .dayCountDays(NUM_DAYS)
        .build();
    assertThat(test1.getValue()).isEqualTo(OptionalDouble.of(VALUE));
    assertThat(test1.getDayCountDays()).isEqualTo(OptionalInt.of(NUM_DAYS));
    assertThat(test1.getValueDate()).isEqualTo(Optional.of(VAL_DATE));

    FutureValueNotional test2 = FutureValueNotional.builder()
        .value(VALUE)
        .build();
    assertThat(test2.getValue()).isEqualTo(OptionalDouble.of(VALUE));
    assertThat(test2.getDayCountDays()).isEqualTo(OptionalInt.empty());
    assertThat(test2.getValueDate()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_exceptions() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FutureValueNotional.of(VALUE, null, NUM_DAYS));
  }

  @Test
  public void test_auto() {
    assertThat(FutureValueNotional.autoCalculate().getValue()).isEqualTo(OptionalDouble.empty());
    assertThat(FutureValueNotional.autoCalculate().getDayCountDays()).isEqualTo(OptionalInt.empty());
    assertThat(FutureValueNotional.autoCalculate().getValueDate()).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FutureValueNotional futureValueNotionalOne = FutureValueNotional.builder()
        .value(1001d)
        .dayCountDays(30)
        .valueDate(LocalDate.of(2016, 6, 6))
        .build();
    coverImmutableBean(futureValueNotionalOne);
    FutureValueNotional futureValueNotionalTwo = FutureValueNotional.builder()
        .value(2001d)
        .dayCountDays(60)
        .valueDate(LocalDate.of(2017, 6, 6))
        .build();
    coverBeanEquals(futureValueNotionalOne, futureValueNotionalTwo);
  }

  @Test
  public void test_serialization() {
    FutureValueNotional futureValueNotional = FutureValueNotional.of(VALUE, VAL_DATE, NUM_DAYS);
    assertSerialization(futureValueNotional);
  }

}
