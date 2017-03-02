/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link CurveMetadata}.
 */
@Test
public class DefaultCurveMetadataTest {

  private static final String NAME = "TestCurve";
  private static final CurveName CURVE_NAME = CurveName.of(NAME);
  private static final JacobianCalibrationMatrix JACOBIAN_DATA = JacobianCalibrationMatrix.of(
      ImmutableList.of(CurveParameterSize.of(CURVE_NAME, 1)),
      DoubleMatrix.filled(2, 2));

  //-------------------------------------------------------------------------
  public void test_of_String_noMetadata() {
    DefaultCurveMetadata test = DefaultCurveMetadata.of(NAME);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getInfo()).isEqualTo(ImmutableMap.of());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_of_CurveName_noMetadata() {
    DefaultCurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME);
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getInfo()).isEqualTo(ImmutableMap.of());
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_builder1() {
    DefaultCurveMetadata test = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME.toString())
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .dayCount(ACT_360)
        .jacobian(JACOBIAN_DATA)
        .addInfo(CurveInfoType.DAY_COUNT, null)
        .parameterMetadata(ImmutableList.of(ParameterMetadata.empty()))
        .build();
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.DISCOUNT_FACTOR);
    assertThat(test.findInfo(CurveInfoType.DAY_COUNT)).isEmpty();
    assertThat(test.getInfo(CurveInfoType.JACOBIAN)).isEqualTo(JACOBIAN_DATA);
    assertThat(test.findInfo(CurveInfoType.JACOBIAN)).isEqualTo(Optional.of(JACOBIAN_DATA));
    assertThat(test.findInfo(CurveInfoType.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(ParameterMetadata.empty());
  }

  public void test_builder2() {
    DefaultCurveMetadata test = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .addInfo(CurveInfoType.DAY_COUNT, ACT_360)
        .jacobian(JACOBIAN_DATA)
        .parameterMetadata(ParameterMetadata.empty())
        .build();
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.DISCOUNT_FACTOR);
    assertThat(test.getInfo(CurveInfoType.DAY_COUNT)).isEqualTo(ACT_360);
    assertThat(test.findInfo(CurveInfoType.DAY_COUNT)).isEqualTo(Optional.of(ACT_360));
    assertThat(test.getInfo(CurveInfoType.JACOBIAN)).isEqualTo(JACOBIAN_DATA);
    assertThat(test.findInfo(CurveInfoType.JACOBIAN)).isEqualTo(Optional.of(JACOBIAN_DATA));
    assertThat(test.findInfo(CurveInfoType.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(ParameterMetadata.empty());
  }

  public void test_builder3() {
    DefaultCurveMetadata test = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .parameterMetadata(ImmutableList.of(ParameterMetadata.empty()))
        .clearParameterMetadata()
        .build();
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getYValueType()).isEqualTo(ValueType.UNKNOWN);
    assertThat(test.getParameterMetadata().isPresent()).isFalse();
  }

  public void test_builder4() {
    DefaultCurveMetadata test = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .parameterMetadata(ParameterMetadata.empty())
        .parameterMetadata(ParameterMetadata.empty())  // second replaces first
        .build();
    assertThat(test.getCurveName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.DISCOUNT_FACTOR);
    assertThrowsIllegalArg(() -> test.getInfo(CurveInfoType.DAY_COUNT));
    assertThat(test.findInfo(CurveInfoType.DAY_COUNT)).isEqualTo(Optional.empty());
    assertThat(test.findInfo(CurveInfoType.JACOBIAN)).isEqualTo(Optional.empty());
    assertThat(test.findInfo(CurveInfoType.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsExactly(ParameterMetadata.empty());
  }

  //-------------------------------------------------------------------------
  public void test_withInfo() {
    DefaultCurveMetadata base = DefaultCurveMetadata.of(CURVE_NAME);
    assertThat(base.findInfo(CurveInfoType.DAY_COUNT).isPresent()).isFalse();
    DefaultCurveMetadata test = base.withInfo(CurveInfoType.DAY_COUNT, ACT_360);
    assertThat(base.findInfo(CurveInfoType.DAY_COUNT).isPresent()).isFalse();
    assertThat(test.findInfo(CurveInfoType.DAY_COUNT).isPresent()).isTrue();
  }

  //-------------------------------------------------------------------------
  public void test_withParameterMetadata() {
    DefaultCurveMetadata base = DefaultCurveMetadata.of(CURVE_NAME);
    DefaultCurveMetadata test = base.withParameterMetadata(ParameterMetadata.listOfEmpty(2));
    assertThat(test.getParameterMetadata().isPresent()).isTrue();
    assertThat(test.getParameterMetadata().get()).containsAll(ParameterMetadata.listOfEmpty(2));
    // redo for test coverage
    DefaultCurveMetadata test2 = test.withParameterMetadata(ParameterMetadata.listOfEmpty(3));
    assertThat(test2.getParameterMetadata().isPresent()).isTrue();
    assertThat(test2.getParameterMetadata().get()).containsAll(ParameterMetadata.listOfEmpty(3));
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
        .jacobian(JACOBIAN_DATA)
        .parameterMetadata(ParameterMetadata.empty())
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveMetadata test = DefaultCurveMetadata.of(CURVE_NAME);
    assertSerialization(test);
  }

}
