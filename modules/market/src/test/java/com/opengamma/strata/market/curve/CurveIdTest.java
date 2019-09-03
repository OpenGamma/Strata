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

import org.junit.jupiter.api.Test;

import com.opengamma.strata.data.ObservableSource;

/**
 * Test {@link CurveId}.
 */
public class CurveIdTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");

  //-------------------------------------------------------------------------
  @Test
  public void test_of_String() {
    CurveId test = CurveId.of("Group", "Name");
    assertThat(test.getCurveGroupName()).isEqualTo(CurveGroupName.of("Group"));
    assertThat(test.getCurveName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(Curve.class);
    assertThat(test.getMarketDataName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.toString()).isEqualTo("CurveId:Group/Name");
  }

  @Test
  public void test_of_Types() {
    CurveId test = CurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"));
    assertThat(test.getCurveGroupName()).isEqualTo(CurveGroupName.of("Group"));
    assertThat(test.getCurveName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(Curve.class);
    assertThat(test.getMarketDataName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.toString()).isEqualTo("CurveId:Group/Name");
  }

  @Test
  public void test_of_TypesSource() {
    CurveId test = CurveId.of(CurveGroupName.of("Group"), CurveName.of("Name"), OBS_SOURCE);
    assertThat(test.getCurveGroupName()).isEqualTo(CurveGroupName.of("Group"));
    assertThat(test.getCurveName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.getObservableSource()).isEqualTo(OBS_SOURCE);
    assertThat(test.getMarketDataType()).isEqualTo(Curve.class);
    assertThat(test.getMarketDataName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.toString()).isEqualTo("CurveId:Group/Name/Vendor");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CurveId test = CurveId.of("Group", "Name");
    coverImmutableBean(test);
    CurveId test2 = CurveId.of("Group2", "Name2");
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    CurveId test = CurveId.of("Group", "Name");
    assertSerialization(test);
  }

}
