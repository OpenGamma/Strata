/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link CurveMetadata}.
 */
@Test
public class DefaultCurveMetadataTest {

  private static final String NAME = "TestCurve";
  private static final CurveName CURVE_NAME = CurveName.of(NAME);

  //-------------------------------------------------------------------------
  public void test_of_String_noParameterMetadata() {
    DefaultCurveMetadata test = DefaultCurveMetadata.of(NAME);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getDayCount()).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_of_CurveName_noParameterMetadata() {
    DefaultCurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getDayCount()).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_of_String_dayCount() {
    DefaultCurveMetadata test = DefaultCurveMetadata.of(NAME, ACT_360);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getDayCount()).isEqualTo(Optional.of(ACT_360));
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_of_CurveName_dayCount() {
    DefaultCurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME, ACT_360);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getDayCount()).isEqualTo(Optional.of(ACT_360));
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_of_String() {
    DefaultCurveMetadata test = DefaultCurveMetadata.of(NAME, ImmutableList.of(CurveParameterMetadata.empty()));
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getDayCount()).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(CurveParameterMetadata.empty());
  }

  public void test_of_CurveName() {
    DefaultCurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME, ImmutableList.of(CurveParameterMetadata.empty()));
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getDayCount()).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(CurveParameterMetadata.empty());
  }

  public void test_builder() {
    DefaultCurveMetadata test = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .dayCount(ACT_360)
        .parameterMetadata(CurveParameterMetadata.empty())
        .parameterMetadata(ImmutableList.of(CurveParameterMetadata.empty()))
        .build();
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getDayCount()).isEqualTo(Optional.of(ACT_360));
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(CurveParameterMetadata.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME, ImmutableList.of(CurveParameterMetadata.empty()));
    coverImmutableBean(test);
    CurveMetadata test2 = DefaultCurveMetadata.of("Coverage");
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME, ImmutableList.of(CurveParameterMetadata.empty()));
    assertSerialization(test);
  }

}
