/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.measure.fxopt.FxVanillaOptionMethod.BLACK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;

/**
 * Test {@link FxVanillaOptionMethod}.
 */
public class FxVanillaOptionMethodTest {

  private static final FxVanillaOptionTrade TRADE = FxVanillaOptionTradeCalculationFunctionTest.TRADE;
  private static final CalculationTarget TARGET = new CalculationTarget() {};

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {FxVanillaOptionMethod.BLACK, "Black"},
        {FxVanillaOptionMethod.VANNA_VOLGA, "VannaVolga"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FxVanillaOptionMethod convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FxVanillaOptionMethod convention, String name) {
    assertThat(FxVanillaOptionMethod.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxVanillaOptionMethod.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxVanillaOptionMethod.of(null));
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
    coverEnum(FxVanillaOptionMethod.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(FxVanillaOptionMethod.BLACK);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FxVanillaOptionMethod.class, FxVanillaOptionMethod.BLACK);
  }

}
