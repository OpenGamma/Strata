/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.List;

import org.testng.annotations.Test;

/**
 * Test {@link FuturesExpiryCurveNodeId}.
 */
@Test
public class FuturesExpiryCurveNodeIdTest {

  private static final YearMonth YM_2015_06 = YearMonth.of(2015, 6);
  private static final YearMonth YM_2015_07 = YearMonth.of(2015, 7);

  public void test_of() {
    FuturesExpiryCurveNodeId test = FuturesExpiryCurveNodeId.of(YM_2015_06);
    assertThat(test.getExpiry()).isEqualTo(YM_2015_06);
  }

  public void test_of_ints() {
    FuturesExpiryCurveNodeId test = FuturesExpiryCurveNodeId.of(2015, 6);
    assertThat(test.getExpiry()).isEqualTo(YM_2015_06);
  }

  //-------------------------------------------------------------------------
  public void test_listOf() {
    List<FuturesExpiryCurveNodeId> test = FuturesExpiryCurveNodeId.listOf(YM_2015_06, YM_2015_07);
    assertThat(test.get(0)).isEqualTo(FuturesExpiryCurveNodeId.of(YM_2015_06));
    assertThat(test.get(1)).isEqualTo(FuturesExpiryCurveNodeId.of(YM_2015_07));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FuturesExpiryCurveNodeId test = FuturesExpiryCurveNodeId.of(YM_2015_06);
    coverImmutableBean(test);
    FuturesExpiryCurveNodeId test2 = FuturesExpiryCurveNodeId.of(YM_2015_07);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FuturesExpiryCurveNodeId test = FuturesExpiryCurveNodeId.of(YM_2015_06);
    assertSerialization(test);
  }

}
