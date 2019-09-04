/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.rate.FixedRateComputation;

/**
 * Test {@link FixedRateStubCalculation}.
 */
public class FixedRateStubCalculationTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1000);

  //-------------------------------------------------------------------------
  @Test
  public void test_ofFixedRate() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofFixedRate(0.025d);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.of(0.025d));
    assertThat(test.getKnownAmount()).isEqualTo(Optional.empty());
    assertThat(test.isFixedRate()).isTrue();
    assertThat(test.isKnownAmount()).isFalse();
  }

  @Test
  public void test_ofKnownAmount() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofKnownAmount(GBP_P1000);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.empty());
    assertThat(test.getKnownAmount()).isEqualTo(Optional.of(GBP_P1000));
    assertThat(test.isFixedRate()).isFalse();
    assertThat(test.isKnownAmount()).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_invalid_fixedAndKnown() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedRateStubCalculation.meta().builder()
            .set(FixedRateStubCalculation.meta().fixedRate(), 0.025d)
            .set(FixedRateStubCalculation.meta().knownAmount(), GBP_P1000)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createRateComputation_NONE() {
    FixedRateStubCalculation test = FixedRateStubCalculation.NONE;
    assertThat(test.createRateComputation(3d)).isEqualTo(FixedRateComputation.of(3d));
  }

  @Test
  public void test_createRateComputation_fixedRate() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofFixedRate(0.025d);
    assertThat(test.createRateComputation(3d)).isEqualTo(FixedRateComputation.of(0.025d));
  }

  @Test
  public void test_createRateComputation_knownAmount() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofKnownAmount(GBP_P1000);
    assertThat(test.createRateComputation(3d)).isEqualTo(KnownAmountRateComputation.of(GBP_P1000));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofFixedRate(0.025d);
    coverImmutableBean(test);
    FixedRateStubCalculation test2 = FixedRateStubCalculation.ofKnownAmount(GBP_P1000);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofFixedRate(0.025d);
    assertSerialization(test);
  }

}
