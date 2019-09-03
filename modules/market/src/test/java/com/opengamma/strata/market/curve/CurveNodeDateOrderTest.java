/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link CurveNodeDateOrder}.
 */
public class CurveNodeDateOrderTest {

  @Test
  public void test_DEFAULT() {
    CurveNodeDateOrder test = CurveNodeDateOrder.DEFAULT;
    assertThat(test.getMinGapInDays()).isEqualTo(1);
    assertThat(test.getAction()).isEqualTo(CurveNodeClashAction.EXCEPTION);
  }

  @Test
  public void test_of() {
    CurveNodeDateOrder test = CurveNodeDateOrder.of(2, CurveNodeClashAction.DROP_THIS);
    assertThat(test.getMinGapInDays()).isEqualTo(2);
    assertThat(test.getAction()).isEqualTo(CurveNodeClashAction.DROP_THIS);
  }

  @Test
  public void test_of_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveNodeDateOrder.of(0, CurveNodeClashAction.DROP_THIS));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CurveNodeDateOrder test = CurveNodeDateOrder.of(2, CurveNodeClashAction.DROP_THIS);
    coverImmutableBean(test);
    CurveNodeDateOrder test2 = CurveNodeDateOrder.of(3, CurveNodeClashAction.DROP_OTHER);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    CurveNodeDateOrder test = CurveNodeDateOrder.of(2, CurveNodeClashAction.DROP_THIS);
    assertSerialization(test);
  }

}
