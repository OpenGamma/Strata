/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.testng.annotations.Test;

@Test
public class FutureValueNotionalTest {
  
  public static final double VALUE = 102345d;
  public static final LocalDate  VAL_DATE = LocalDate.of(2017, 7, 7);
  public static final int NUM_DAYS = 512;
  
  public void test_of() {
    FutureValueNotional futureValueNotional = FutureValueNotional.of(VALUE, VAL_DATE, NUM_DAYS);
    assertEquals(futureValueNotional.getValue(), OptionalDouble.of(VALUE));
    assertEquals(futureValueNotional.getCalculationPeriodNumberOfDays(), OptionalInt.of(NUM_DAYS));
    assertEquals(futureValueNotional.getValueDate(), Optional.of(VAL_DATE));
  }
  
  public void test_builder() {
    FutureValueNotional futureValueNotional = FutureValueNotional.builder()
        .value(VALUE)
        .valueDate(VAL_DATE)
        .calculationPeriodNumberOfDays(NUM_DAYS)
        .build();
    assertEquals(futureValueNotional.getValue(), OptionalDouble.of(VALUE));
    assertEquals(futureValueNotional.getCalculationPeriodNumberOfDays(), OptionalInt.of(NUM_DAYS));
    assertEquals(futureValueNotional.getValueDate(), Optional.of(VAL_DATE));
  }
  
  public void test_auto() {
    assertEquals(FutureValueNotional.auto().getValue(), OptionalDouble.empty());
    assertEquals(FutureValueNotional.auto().getCalculationPeriodNumberOfDays(), OptionalInt.empty());
    assertEquals(FutureValueNotional.auto().getValueDate(), Optional.empty());
  }
  
  //-------------------------------------------------------------------------
  public void coverage() {
    FutureValueNotional futureValueNotionalOne = FutureValueNotional.builder()
        .value(1001d)
        .calculationPeriodNumberOfDays(30)
        .valueDate(LocalDate.of(2016, 6, 6))
        .build();
    coverImmutableBean(futureValueNotionalOne);
    FutureValueNotional futureValueNotionalTwo = FutureValueNotional.builder()
        .value(2001d)
        .calculationPeriodNumberOfDays(60)
        .valueDate(LocalDate.of(2017, 6, 6))
        .build();
    coverBeanEquals(futureValueNotionalOne, futureValueNotionalTwo);
  }
  
  public void test_serialization() {
    FutureValueNotional futureValueNotional = FutureValueNotional.of(VALUE, VAL_DATE, NUM_DAYS);
    assertSerialization(futureValueNotional);
  }
}
