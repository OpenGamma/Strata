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
import com.opengamma.strata.market.value.ValueType;

/**
 * Test {@link CurveMetadata}.
 */
@Test
public class DefaultCurveMetadataTest {

  private static final String NAME = "TestCurve";
  private static final CurveName CURVE_NAME = CurveName.of(NAME);

  //-------------------------------------------------------------------------
  public void test_of_String_noMetadata() {
    CurveMetadata test = DefaultCurveMetadata.of(NAME);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getDayCount()).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_of_CurveName_noMetadata() {
    CurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getDayCount()).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_builder() {
    DefaultCurveMetadata test = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(ACT_360)
        .parameterMetadata(ImmutableList.of(CurveParameterMetadata.empty()))
        .build();
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.DISCOUNT_FACTOR);
    assertThat(test.getDayCount()).isEqualTo(Optional.of(ACT_360));
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(CurveParameterMetadata.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultCurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME);
    coverImmutableBean(test);
    DefaultCurveMetadata test2 = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(ACT_360)
        .parameterMetadata(CurveParameterMetadata.empty())
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME);
    assertSerialization(test);
  }

}
