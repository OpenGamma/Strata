/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.testng.annotations.Test;

/**
 * Test {@link FutureValueNotional}.
 */
@Test
public class FutureValueNotionalTest {

  private static final double VALUE = 102345d;
  private static final LocalDate VAL_DATE = LocalDate.of(2017, 7, 7);
  private static final int NUM_DAYS = 512;

  //-------------------------------------------------------------------------
  public void test_of() {
    FutureValueNotional test1 = FutureValueNotional.of(VALUE, VAL_DATE, NUM_DAYS);
    assertEquals(test1.getValue(), OptionalDouble.of(VALUE));
    assertEquals(test1.getDayCountDays(), OptionalInt.of(NUM_DAYS));
    assertEquals(test1.getValueDate(), Optional.of(VAL_DATE));

    FutureValueNotional test2 = FutureValueNotional.of(VALUE);
    assertEquals(test2.getValue(), OptionalDouble.of(VALUE));
    assertEquals(test2.getDayCountDays(), OptionalInt.empty());
    assertEquals(test2.getValueDate(), Optional.empty());
  }

  public void test_builder() {
    FutureValueNotional test1 = FutureValueNotional.builder()
        .value(VALUE)
        .valueDate(VAL_DATE)
        .dayCountDays(NUM_DAYS)
        .build();
    assertEquals(test1.getValue(), OptionalDouble.of(VALUE));
    assertEquals(test1.getDayCountDays(), OptionalInt.of(NUM_DAYS));
    assertEquals(test1.getValueDate(), Optional.of(VAL_DATE));

    FutureValueNotional test2 = FutureValueNotional.builder()
        .value(VALUE)
        .build();
    assertEquals(test2.getValue(), OptionalDouble.of(VALUE));
    assertEquals(test2.getDayCountDays(), OptionalInt.empty());
    assertEquals(test2.getValueDate(), Optional.empty());
  }

  public void test_exceptions() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FutureValueNotional.of(VALUE, null, NUM_DAYS));
  }

  public void test_auto() {
    assertEquals(FutureValueNotional.autoCalculate().getValue(), OptionalDouble.empty());
    assertEquals(FutureValueNotional.autoCalculate().getDayCountDays(), OptionalInt.empty());
    assertEquals(FutureValueNotional.autoCalculate().getValueDate(), Optional.empty());
  }

  //-------------------------------------------------------------------------
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

  public void test_serialization() {
    FutureValueNotional futureValueNotional = FutureValueNotional.of(VALUE, VAL_DATE, NUM_DAYS);
    assertSerialization(futureValueNotional);
  }

}
