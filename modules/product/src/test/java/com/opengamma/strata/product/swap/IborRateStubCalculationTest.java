/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test.
 */
@Test
public class IborRateStubCalculationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1000);
  private static final LocalDate DATE = date(2015, 6, 30);

  //-------------------------------------------------------------------------
  public void test_ofFixedRate() {
    IborRateStubCalculation test = IborRateStubCalculation.ofFixedRate(0.025d);
    assertEquals(test.getFixedRate(), OptionalDouble.of(0.025d));
    assertEquals(test.getIndex(), Optional.empty());
    assertEquals(test.getIndexInterpolated(), Optional.empty());
    assertEquals(test.isFixedRate(), true);
    assertEquals(test.isKnownAmount(), false);
    assertEquals(test.isFloatingRate(), false);
    assertEquals(test.isInterpolated(), false);
  }

  public void test_ofKnownAmount() {
    IborRateStubCalculation test = IborRateStubCalculation.ofKnownAmount(GBP_P1000);
    assertEquals(test.getFixedRate(), OptionalDouble.empty());
    assertEquals(test.getKnownAmount(), Optional.of(GBP_P1000));
    assertEquals(test.getIndex(), Optional.empty());
    assertEquals(test.getIndexInterpolated(), Optional.empty());
    assertEquals(test.isFixedRate(), false);
    assertEquals(test.isKnownAmount(), true);
    assertEquals(test.isFloatingRate(), false);
    assertEquals(test.isInterpolated(), false);
  }

  public void test_ofIborRate() {
    IborRateStubCalculation test = IborRateStubCalculation.ofIborRate(GBP_LIBOR_3M);
    assertEquals(test.getFixedRate(), OptionalDouble.empty());
    assertEquals(test.getIndex(), Optional.of(GBP_LIBOR_3M));
    assertEquals(test.getIndexInterpolated(), Optional.empty());
    assertEquals(test.isFixedRate(), false);
    assertEquals(test.isKnownAmount(), false);
    assertEquals(test.isFloatingRate(), true);
    assertEquals(test.isInterpolated(), false);
  }

  public void test_ofIborInterpolatedRate() {
    IborRateStubCalculation test = IborRateStubCalculation.ofIborInterpolatedRate(GBP_LIBOR_1M, GBP_LIBOR_3M);
    assertEquals(test.getFixedRate(), OptionalDouble.empty());
    assertEquals(test.getIndex(), Optional.of(GBP_LIBOR_1M));
    assertEquals(test.getIndexInterpolated(), Optional.of(GBP_LIBOR_3M));
    assertEquals(test.isFixedRate(), false);
    assertEquals(test.isKnownAmount(), false);
    assertEquals(test.isFloatingRate(), true);
    assertEquals(test.isInterpolated(), true);
  }

  public void test_ofIborInterpolatedRate_invalid_interpolatedSameIndex() {
    assertThrowsIllegalArg(() -> IborRateStubCalculation.ofIborInterpolatedRate(GBP_LIBOR_3M, GBP_LIBOR_3M));
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> IborRateStubCalculation.ofIborRate(null));
    assertThrowsIllegalArg(() -> IborRateStubCalculation.ofIborInterpolatedRate(null, GBP_LIBOR_3M));
    assertThrowsIllegalArg(() -> IborRateStubCalculation.ofIborInterpolatedRate(GBP_LIBOR_3M, null));
    assertThrowsIllegalArg(() -> IborRateStubCalculation.ofIborInterpolatedRate(null, null));
  }

  //-------------------------------------------------------------------------
  public void test_builder_invalid_fixedAndIbor() {
    assertThrowsIllegalArg(() -> IborRateStubCalculation.builder()
        .fixedRate(0.025d)
        .index(GBP_LIBOR_3M)
        .build());
  }

  public void test_builder_invalid_fixedAndKnown() {
    assertThrowsIllegalArg(() -> IborRateStubCalculation.builder()
        .fixedRate(0.025d)
        .knownAmount(GBP_P1000)
        .build());
  }

  public void test_builder_invalid_knownAndIbor() {
    assertThrowsIllegalArg(() -> IborRateStubCalculation.builder()
        .knownAmount(GBP_P1000)
        .index(GBP_LIBOR_3M)
        .build());
  }

  public void test_builder_invalid_interpolatedWithoutBase() {
    assertThrowsIllegalArg(() -> IborRateStubCalculation.builder()
        .indexInterpolated(GBP_LIBOR_3M)
        .build());
  }

  public void test_builder_invalid_interpolatedSameIndex() {
    assertThrowsIllegalArg(() -> IborRateStubCalculation.builder()
        .index(GBP_LIBOR_3M)
        .indexInterpolated(GBP_LIBOR_3M)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_createRateComputation_NONE() {
    IborRateStubCalculation test = IborRateStubCalculation.NONE;
    assertEquals(test.createRateComputation(DATE, GBP_LIBOR_3M, REF_DATA), IborRateComputation.of(GBP_LIBOR_3M, DATE, REF_DATA));
  }

  public void test_createRateComputation_fixedRate() {
    IborRateStubCalculation test = IborRateStubCalculation.ofFixedRate(0.025d);
    assertEquals(test.createRateComputation(DATE, GBP_LIBOR_3M, REF_DATA), FixedRateComputation.of(0.025d));
  }

  public void test_createRateComputation_knownAmount() {
    IborRateStubCalculation test = IborRateStubCalculation.ofKnownAmount(GBP_P1000);
    assertEquals(test.createRateComputation(DATE, GBP_LIBOR_3M, REF_DATA), KnownAmountRateComputation.of(GBP_P1000));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborRateStubCalculation test = IborRateStubCalculation.ofIborInterpolatedRate(GBP_LIBOR_1M, GBP_LIBOR_3M);
    coverImmutableBean(test);
    IborRateStubCalculation test2 = IborRateStubCalculation.ofFixedRate(0.028d);
    coverBeanEquals(test, test2);
    IborRateStubCalculation test3 = IborRateStubCalculation.ofKnownAmount(GBP_P1000);
    coverBeanEquals(test, test3);
  }

  public void test_serialization() {
    IborRateStubCalculation test = IborRateStubCalculation.ofIborRate(GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
