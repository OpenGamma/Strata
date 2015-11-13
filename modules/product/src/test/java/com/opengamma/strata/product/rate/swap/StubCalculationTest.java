/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class StubCalculationTest {

  //-------------------------------------------------------------------------
  public void test_ofFixedRate() {
    StubCalculation test = StubCalculation.ofFixedRate(0.025d);
    assertEquals(test.getFixedRate(), OptionalDouble.of(0.025d));
    assertEquals(test.getIndex(), Optional.empty());
    assertEquals(test.getIndexInterpolated(), Optional.empty());
  }

  public void test_ofIborRate() {
    StubCalculation test = StubCalculation.ofIborRate(GBP_LIBOR_3M);
    assertEquals(test.getFixedRate(), OptionalDouble.empty());
    assertEquals(test.getIndex(), Optional.of(GBP_LIBOR_3M));
    assertEquals(test.getIndexInterpolated(), Optional.empty());
  }

  public void test_ofIborInterpolatedRate() {
    StubCalculation test = StubCalculation.ofIborInterpolatedRate(GBP_LIBOR_1M, GBP_LIBOR_3M);
    assertEquals(test.getFixedRate(), OptionalDouble.empty());
    assertEquals(test.getIndex(), Optional.of(GBP_LIBOR_1M));
    assertEquals(test.getIndexInterpolated(), Optional.of(GBP_LIBOR_3M));
  }

  public void test_ofIborInterpolatedRate_invalid_interpolatedSameIndex() {
    assertThrowsIllegalArg(() -> StubCalculation.ofIborInterpolatedRate(GBP_LIBOR_3M, GBP_LIBOR_3M));
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> StubCalculation.ofIborRate(null));
    assertThrowsIllegalArg(() -> StubCalculation.ofIborInterpolatedRate(null, GBP_LIBOR_3M));
    assertThrowsIllegalArg(() -> StubCalculation.ofIborInterpolatedRate(GBP_LIBOR_3M, null));
    assertThrowsIllegalArg(() -> StubCalculation.ofIborInterpolatedRate(null, null));
  }

  //-------------------------------------------------------------------------
  public void test_builder_invalid_fixedAndIbor() {
    assertThrowsIllegalArg(() -> StubCalculation.builder()
        .fixedRate(0.025d)
        .index(GBP_LIBOR_3M)
        .build());
  }

  public void test_builder_invalid_interpolatedWithoutBase() {
    assertThrowsIllegalArg(() -> StubCalculation.builder()
        .indexInterpolated(GBP_LIBOR_3M)
        .build());
  }

  public void test_builder_invalid_interpolatedSameIndex() {
    assertThrowsIllegalArg(() -> StubCalculation.builder()
        .index(GBP_LIBOR_3M)
        .indexInterpolated(GBP_LIBOR_3M)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    StubCalculation test = StubCalculation.ofIborInterpolatedRate(GBP_LIBOR_1M, GBP_LIBOR_3M);
    coverImmutableBean(test);
    StubCalculation test2 = StubCalculation.ofFixedRate(0.028d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    StubCalculation test = StubCalculation.ofIborRate(GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
