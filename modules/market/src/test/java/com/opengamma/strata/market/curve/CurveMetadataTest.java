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

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link CurveMetadata}.
 */
@Test
public class CurveMetadataTest {

  private static final String NAME = "TestCurve";
  private static final CurveName CURVE_NAME = CurveName.of(NAME);

  //-------------------------------------------------------------------------
  public void test_of_String_noParameterMetadata() {
    CurveMetadata test = CurveMetadata.of(NAME);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameters().isPresent()).isFalse();
  }

  public void test_of_CurveName_noParameterMetadata() {
    CurveMetadata test = CurveMetadata.of(CURVE_NAME);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameters().isPresent()).isFalse();
  }

  public void test_of_String() {
    CurveMetadata test = CurveMetadata.of(NAME, ImmutableList.of(CurveParameterMetadata.empty()));
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameters().isPresent()).isTrue();
    assertThat(test.getParameters().get()).containsExactly(CurveParameterMetadata.empty());
  }

  public void test_of_CurveName() {
    CurveMetadata test = CurveMetadata.of(CURVE_NAME, ImmutableList.of(CurveParameterMetadata.empty()));
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameters().isPresent()).isTrue();
    assertThat(test.getParameters().get()).containsExactly(CurveParameterMetadata.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveMetadata test = CurveMetadata.of(CURVE_NAME, ImmutableList.of(CurveParameterMetadata.empty()));
    coverImmutableBean(test);
    CurveMetadata test2 = CurveMetadata.of("Coverage");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveMetadata test = CurveMetadata.of(CURVE_NAME, ImmutableList.of(CurveParameterMetadata.empty()));
    assertSerialization(test);
  }

}
