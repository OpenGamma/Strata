/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link CurveNodeClashAction}.
 */
public class CurveNodeClashActionTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {CurveNodeClashAction.DROP_THIS, "DropThis"},
        {CurveNodeClashAction.DROP_OTHER, "DropOther"},
        {CurveNodeClashAction.EXCEPTION, "Exception"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(CurveNodeClashAction convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(CurveNodeClashAction convention, String name) {
    assertThat(CurveNodeClashAction.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveNodeClashAction.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveNodeClashAction.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(CurveNodeClashAction.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(CurveNodeClashAction.DROP_THIS);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CurveNodeClashAction.class, CurveNodeClashAction.DROP_THIS);
  }

}
