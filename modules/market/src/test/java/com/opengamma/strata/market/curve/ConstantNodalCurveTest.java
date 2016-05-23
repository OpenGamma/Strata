/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link ConstantNodalCurve}.
 */
@Test
public class ConstantNodalCurveTest {

  private static final String NAME = "TestCurve";
  private static final CurveName CURVE_NAME = CurveName.of(NAME);
  private static final CurveMetadata METADATA = DefaultCurveMetadata.of(CURVE_NAME);
  private static final CurveMetadata METADATA2 = DefaultCurveMetadata.of("Test2");
  private static final double VALUE = 6d;

  //-------------------------------------------------------------------------
  public void test_of_String() {
    ConstantNodalCurve test = ConstantNodalCurve.of(NAME, VALUE);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameter(0)).isEqualTo(VALUE);
    assertThat(test.getParameterMetadata(0)).isEqualTo(ParameterMetadata.empty());
    assertThat(test.withParameter(0, 2d)).isEqualTo(ConstantNodalCurve.of(NAME, 2d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d)).isEqualTo(ConstantNodalCurve.of(NAME, VALUE + 1d));
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(VALUE);
    assertThat(test.withMetadata(METADATA2)).isEqualTo(ConstantNodalCurve.of(METADATA2, VALUE));
  }

  public void test_of_CurveName() {
    ConstantNodalCurve test = ConstantNodalCurve.of(CURVE_NAME, VALUE);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameter(0)).isEqualTo(VALUE);
    assertThat(test.getParameterMetadata(0)).isEqualTo(ParameterMetadata.empty());
    assertThat(test.withParameter(0, 2d)).isEqualTo(ConstantNodalCurve.of(NAME, 2d));
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(VALUE);
    assertThat(test.withMetadata(METADATA2)).isEqualTo(ConstantNodalCurve.of(METADATA2, VALUE));
  }

  public void test_of_CurveMetadata() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA, VALUE);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getParameter(0)).isEqualTo(VALUE);
    assertThat(test.getParameterMetadata(0)).isEqualTo(ParameterMetadata.empty());
    assertThat(test.withParameter(0, 2d)).isEqualTo(ConstantNodalCurve.of(NAME, 2d));
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(VALUE);
    assertThat(test.withMetadata(METADATA2)).isEqualTo(ConstantNodalCurve.of(METADATA2, VALUE));
  }

  //-------------------------------------------------------------------------
  public void test_lookup() {
    ConstantNodalCurve test = ConstantNodalCurve.of(CURVE_NAME, VALUE);
    assertThat(test.yValue(0d)).isEqualTo(VALUE);
    assertThat(test.yValue(-10d)).isEqualTo(VALUE);
    assertThat(test.yValue(100d)).isEqualTo(VALUE);

    assertThat(test.yValueParameterSensitivity(0d).getSensitivity().toArray()).containsExactly(1d);
    assertThat(test.yValueParameterSensitivity(-10d).getSensitivity().toArray()).containsExactly(1d);
    assertThat(test.yValueParameterSensitivity(100d).getSensitivity().toArray()).containsExactly(1d);

    assertThat(test.firstDerivative(0d)).isEqualTo(0d);
    assertThat(test.firstDerivative(-10d)).isEqualTo(0d);
    assertThat(test.firstDerivative(100d)).isEqualTo(0d);
  }

  //-------------------------------------------------------------------------
  public void test_withYValues() {
    ConstantNodalCurve base = ConstantNodalCurve.of(CURVE_NAME, VALUE);
    ConstantNodalCurve test = base.withYValues(DoubleArray.of(4d));
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(4d);
  }

  public void test_withYValues_badSize() {
    ConstantNodalCurve base = ConstantNodalCurve.of(CURVE_NAME, VALUE);
    assertThrowsIllegalArg(() -> base.withYValues(DoubleArray.EMPTY));
    assertThrowsIllegalArg(() -> base.withYValues(DoubleArray.of(4d, 6d)));
  }

  //-------------------------------------------------------------------------
  public void test_applyPerturbation() {
    ConstantNodalCurve base = ConstantNodalCurve.of(CURVE_NAME, VALUE);
    ConstantNodalCurve result = ConstantNodalCurve.of(CURVE_NAME, 7d);
    Curve test = base.applyPerturbation(curve -> result);
    assertThat(test).isSameAs(result);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ConstantNodalCurve test = ConstantNodalCurve.of(CURVE_NAME, VALUE);
    coverImmutableBean(test);
    ConstantNodalCurve test2 = ConstantNodalCurve.of("Coverage", 9d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ConstantNodalCurve test = ConstantNodalCurve.of(CURVE_NAME, VALUE);
    assertSerialization(test);
  }

}
