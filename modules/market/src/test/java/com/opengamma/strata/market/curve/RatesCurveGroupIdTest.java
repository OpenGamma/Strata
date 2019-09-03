/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.data.ObservableSource;

/**
 * Test {@link RatesCurveGroupId}.
 */
public class RatesCurveGroupIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final ObservableSource OBS_SOURCE2 = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  @Test
  public void test_of_String() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1.toString());
    assertThat(test.getCurveGroupName()).isEqualTo(GROUP1);
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(RatesCurveGroup.class);
    assertThat(test.toString()).isEqualTo("RatesCurveGroupId:Group1");
  }

  @Test
  public void test_of_Type() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1);
    assertThat(test.getCurveGroupName()).isEqualTo(GROUP1);
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(RatesCurveGroup.class);
    assertThat(test.toString()).isEqualTo("RatesCurveGroupId:Group1");
  }

  @Test
  public void test_of_TypeSource() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1, OBS_SOURCE2);
    assertThat(test.getCurveGroupName()).isEqualTo(GROUP1);
    assertThat(test.getObservableSource()).isEqualTo(OBS_SOURCE2);
    assertThat(test.getMarketDataType()).isEqualTo(RatesCurveGroup.class);
    assertThat(test.toString()).isEqualTo("RatesCurveGroupId:Group1/Vendor");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1);
    coverImmutableBean(test);
    RatesCurveGroupId test2 = RatesCurveGroupId.of(GROUP2, OBS_SOURCE2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    RatesCurveGroupId test = RatesCurveGroupId.of(GROUP1);
    assertSerialization(test);
  }

}
