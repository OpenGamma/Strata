/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.Tenor.TENOR_12M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

/**
 * Test {@link TenorCurveNodeId}.
 */
@Test
public class TenorCurveNodeIdTest {

  public void test_of() {
    TenorCurveNodeId test = TenorCurveNodeId.of(TENOR_1M);
    assertThat(test.getTenor()).isEqualTo(TENOR_1M);
  }

  //-------------------------------------------------------------------------
  public void test_listOf() {
    List<TenorCurveNodeId> test = TenorCurveNodeId.listOf(TENOR_1M, TENOR_12M);
    assertThat(test.get(0)).isEqualTo(TenorCurveNodeId.of(TENOR_1M));
    assertThat(test.get(1)).isEqualTo(TenorCurveNodeId.of(TENOR_12M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TenorCurveNodeId test = TenorCurveNodeId.of(TENOR_1M);
    coverImmutableBean(test);
    TenorCurveNodeId test2 = TenorCurveNodeId.of(TENOR_12M);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    TenorCurveNodeId test = TenorCurveNodeId.of(TENOR_1M);
    assertSerialization(test);
  }

}
