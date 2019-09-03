/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LOG_LINEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link CurveExtrapolator}.
 */
public class CurveExtrapolatorTest {

  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {CurveExtrapolators.EXCEPTION, "Exception"},
        {CurveExtrapolators.EXPONENTIAL, "Exponential"},
        {CurveExtrapolators.FLAT, "Flat"},
        {CurveExtrapolators.INTERPOLATOR, "Interpolator"},
        {CurveExtrapolators.LINEAR, "Linear"},
        {CurveExtrapolators.LOG_LINEAR, "LogLinear"},
        {CurveExtrapolators.PRODUCT_LINEAR, "ProductLinear"},
        {CurveExtrapolators.QUADRATIC_LEFT, "QuadraticLeft"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(CurveExtrapolator convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(CurveExtrapolator convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(CurveExtrapolator convention, String name) {
    assertThat(CurveExtrapolator.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(CurveExtrapolator convention, String name) {
    ImmutableMap<String, CurveExtrapolator> map = CurveExtrapolator.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveExtrapolator.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveExtrapolator.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(CurveExtrapolators.class);
    coverPrivateConstructor(StandardCurveExtrapolators.class);
    assertThat(FLAT.equals(null)).isFalse();
    assertThat(FLAT.equals(ANOTHER_TYPE)).isFalse();
  }

  @Test
  public void test_serialization() {
    assertSerialization(FLAT);
    assertSerialization(LINEAR);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CurveExtrapolator.class, FLAT);
    assertJodaConvert(CurveExtrapolator.class, LOG_LINEAR);
  }

}
