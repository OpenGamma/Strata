/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test.
 */
public class IborRateStubCalculationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1000);
  private static final LocalDate DATE = date(2015, 6, 30);

  //-------------------------------------------------------------------------
  @Test
  public void test_ofFixedRate() {
    IborRateStubCalculation test = IborRateStubCalculation.ofFixedRate(0.025d);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.of(0.025d));
    assertThat(test.getIndex()).isEqualTo(Optional.empty());
    assertThat(test.getIndexInterpolated()).isEqualTo(Optional.empty());
    assertThat(test.isFixedRate()).isTrue();
    assertThat(test.isKnownAmount()).isFalse();
    assertThat(test.isFloatingRate()).isFalse();
    assertThat(test.isInterpolated()).isFalse();
  }

  @Test
  public void test_ofKnownAmount() {
    IborRateStubCalculation test = IborRateStubCalculation.ofKnownAmount(GBP_P1000);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.empty());
    assertThat(test.getKnownAmount()).isEqualTo(Optional.of(GBP_P1000));
    assertThat(test.getIndex()).isEqualTo(Optional.empty());
    assertThat(test.getIndexInterpolated()).isEqualTo(Optional.empty());
    assertThat(test.isFixedRate()).isFalse();
    assertThat(test.isKnownAmount()).isTrue();
    assertThat(test.isFloatingRate()).isFalse();
    assertThat(test.isInterpolated()).isFalse();
  }

  @Test
  public void test_ofIborRate() {
    IborRateStubCalculation test = IborRateStubCalculation.ofIborRate(GBP_LIBOR_3M);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.empty());
    assertThat(test.getIndex()).isEqualTo(Optional.of(GBP_LIBOR_3M));
    assertThat(test.getIndexInterpolated()).isEqualTo(Optional.empty());
    assertThat(test.isFixedRate()).isFalse();
    assertThat(test.isKnownAmount()).isFalse();
    assertThat(test.isFloatingRate()).isTrue();
    assertThat(test.isInterpolated()).isFalse();
  }

  @Test
  public void test_ofIborInterpolatedRate() {
    IborRateStubCalculation test = IborRateStubCalculation.ofIborInterpolatedRate(GBP_LIBOR_1M, GBP_LIBOR_3M);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.empty());
    assertThat(test.getIndex()).isEqualTo(Optional.of(GBP_LIBOR_1M));
    assertThat(test.getIndexInterpolated()).isEqualTo(Optional.of(GBP_LIBOR_3M));
    assertThat(test.isFixedRate()).isFalse();
    assertThat(test.isKnownAmount()).isFalse();
    assertThat(test.isFloatingRate()).isTrue();
    assertThat(test.isInterpolated()).isTrue();
  }

  @Test
  public void test_ofIborInterpolatedRate_invalid_interpolatedSameIndex() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.ofIborInterpolatedRate(GBP_LIBOR_3M, GBP_LIBOR_3M));
  }

  @Test
  public void test_of_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.ofIborRate(null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.ofIborInterpolatedRate(null, GBP_LIBOR_3M));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.ofIborInterpolatedRate(GBP_LIBOR_3M, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.ofIborInterpolatedRate(null, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_invalid_fixedAndIbor() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.builder()
            .fixedRate(0.025d)
            .index(GBP_LIBOR_3M)
            .build());
  }

  @Test
  public void test_builder_invalid_fixedAndKnown() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.builder()
            .fixedRate(0.025d)
            .knownAmount(GBP_P1000)
            .build());
  }

  @Test
  public void test_builder_invalid_knownAndIbor() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.builder()
            .knownAmount(GBP_P1000)
            .index(GBP_LIBOR_3M)
            .build());
  }

  @Test
  public void test_builder_invalid_interpolatedWithoutBase() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.builder()
            .indexInterpolated(GBP_LIBOR_3M)
            .build());
  }

  @Test
  public void test_builder_invalid_interpolatedSameIndex() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateStubCalculation.builder()
            .index(GBP_LIBOR_3M)
            .indexInterpolated(GBP_LIBOR_3M)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createRateComputation_NONE() {
    IborRateStubCalculation test = IborRateStubCalculation.NONE;
    assertThat(test.createRateComputation(DATE, GBP_LIBOR_3M, REF_DATA))
        .isEqualTo(IborRateComputation.of(GBP_LIBOR_3M, DATE, REF_DATA));
  }

  @Test
  public void test_createRateComputation_fixedRate() {
    IborRateStubCalculation test = IborRateStubCalculation.ofFixedRate(0.025d);
    assertThat(test.createRateComputation(DATE, GBP_LIBOR_3M, REF_DATA)).isEqualTo(FixedRateComputation.of(0.025d));
  }

  @Test
  public void test_createRateComputation_knownAmount() {
    IborRateStubCalculation test = IborRateStubCalculation.ofKnownAmount(GBP_P1000);
    assertThat(test.createRateComputation(DATE, GBP_LIBOR_3M, REF_DATA)).isEqualTo(KnownAmountRateComputation.of(GBP_P1000));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborRateStubCalculation test = IborRateStubCalculation.ofIborInterpolatedRate(GBP_LIBOR_1M, GBP_LIBOR_3M);
    coverImmutableBean(test);
    IborRateStubCalculation test2 = IborRateStubCalculation.ofFixedRate(0.028d);
    coverBeanEquals(test, test2);
    IborRateStubCalculation test3 = IborRateStubCalculation.ofKnownAmount(GBP_P1000);
    coverBeanEquals(test, test3);
  }

  @Test
  public void test_serialization() {
    IborRateStubCalculation test = IborRateStubCalculation.ofIborRate(GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
