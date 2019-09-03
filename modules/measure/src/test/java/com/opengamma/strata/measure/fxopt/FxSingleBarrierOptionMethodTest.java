/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.measure.fxopt.FxSingleBarrierOptionMethod.BLACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.product.fxopt.FxSingleBarrierOptionTrade;

/**
 * Test {@link FxSingleBarrierOptionMethod}.
 */
public class FxSingleBarrierOptionMethodTest {

  private static final FxSingleBarrierOptionTrade TRADE = FxSingleBarrierOptionTradeCalculationFunctionTest.TRADE;
  private static final CalculationTarget TARGET = new CalculationTarget() {};

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {FxSingleBarrierOptionMethod.BLACK, "Black"},
        {FxSingleBarrierOptionMethod.TRINOMIAL_TREE, "TrinomialTree"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FxSingleBarrierOptionMethod convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FxSingleBarrierOptionMethod convention, String name) {
    assertThat(FxSingleBarrierOptionMethod.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxSingleBarrierOptionMethod.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxSingleBarrierOptionMethod.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_filter() {
    assertThat(BLACK.filter(TRADE, Measures.PRESENT_VALUE)).isEqualTo(Optional.of(BLACK));
    assertThat(BLACK.filter(TARGET, Measures.PRESENT_VALUE)).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(FxSingleBarrierOptionMethod.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(FxSingleBarrierOptionMethod.BLACK);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FxSingleBarrierOptionMethod.class, FxSingleBarrierOptionMethod.BLACK);
  }

}
