/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link ConstantNodalCurve}.
 */
public class ConstantNodalCurveTest {

  private static final int SIZE = 1;
  private static final String NAME = "TestCurve";
  private static final CurveName CURVE_NAME = CurveName.of(NAME);
  private static final CurveMetadata METADATA = Curves.zeroRates(CURVE_NAME, ACT_365F);
  private static final CurveMetadata METADATA_ENTRIES =
      Curves.zeroRates(CURVE_NAME, ACT_365F, ParameterMetadata.listOfEmpty(SIZE));
  private static final CurveMetadata METADATA_ENTRIES2 =
      Curves.zeroRates(CURVE_NAME, ACT_365F, ParameterMetadata.listOfEmpty(SIZE + 2));
  private static final CurveMetadata METADATA_NOPARAM = Curves.zeroRates(CURVE_NAME, ACT_365F);
  private static final double XVALUE = 2d;
  private static final DoubleArray XVALUE_ARRAY = DoubleArray.of(XVALUE);
  private static final double YVALUE = 7d;
  private static final DoubleArray YVALUE_ARRAY = DoubleArray.of(YVALUE);
  private static final DoubleArray XVALUE_ARRAY_NEW = DoubleArray.of(3d);
  private static final double YVALUE_BUMPED = 5d;
  private static final DoubleArray YVALUE_BUMPED_ARRAY = DoubleArray.of(YVALUE_BUMPED);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_CurveMetadata() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE);
    ConstantNodalCurve testRe = ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE);
    assertThat(test).isEqualTo(testRe);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getParameter(0)).isEqualTo(YVALUE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getParameter(1));
    assertThat(test.getParameterMetadata(0)).isSameAs(METADATA_ENTRIES.getParameterMetadata().get().get(0));
    assertThat(test.withParameter(0, 2d)).isEqualTo(ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, 2d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.withParameter(1, 2d));
    assertThat(test.withPerturbation((i, v, m) -> v - 2d)).isEqualTo(
        ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE_BUMPED));
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUE_ARRAY);
    assertThat(test.getYValues()).isEqualTo(YVALUE_ARRAY);
  }

  @Test
  public void test_of_noCurveMetadata() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA_NOPARAM, XVALUE, YVALUE);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getParameter(0)).isEqualTo(YVALUE);
    assertThat(test.getParameterMetadata(0)).isEqualTo(SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, XVALUE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withNode() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE);
    SimpleCurveParameterMetadata param = SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, XVALUE);
    ConstantNodalCurve test = base.withNode(XVALUE, 2d, param);
    assertThat(test.getXValue()).isEqualTo(XVALUE);
    assertThat(test.getYValue()).isEqualTo(2d);
    assertThat(test.getParameterMetadata(0)).isEqualTo(param);
  }

  @Test
  public void test_withNode_invalid() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.withNode(1, 2, ParameterMetadata.empty()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_values() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertThat(test.yValue(10.2421)).isEqualTo(YVALUE);
    assertThat(test.yValueParameterSensitivity(10.2421).getMarketDataName()).isEqualTo(CURVE_NAME);
    assertThat(test.yValueParameterSensitivity(10.2421).getSensitivity()).isEqualTo(DoubleArray.of(1d));
    assertThat(test.firstDerivative(10.2421)).isEqualTo(0d);
    assertThat(test.values().toMap()).isEqualTo(ImmutableMap.of(XVALUE, YVALUE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withMetadata() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    ConstantNodalCurve test = base.withMetadata(METADATA_ENTRIES);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUE_ARRAY);
    assertThat(test.getYValues()).isEqualTo(YVALUE_ARRAY);
  }

  @Test
  public void test_withMetadata_badSize() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withMetadata(METADATA_ENTRIES2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withValues() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    ConstantNodalCurve test = base.withYValues(YVALUE_BUMPED_ARRAY);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUE_ARRAY);
    assertThat(test.getYValues()).isEqualTo(YVALUE_BUMPED_ARRAY);
  }

  @Test
  public void test_withValues_badSize() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withYValues(DoubleArray.EMPTY));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withYValues(DoubleArray.of(4d, 6d)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withValuesXy() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    ConstantNodalCurve test = base.withValues(XVALUE_ARRAY_NEW, YVALUE_BUMPED_ARRAY);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUE_ARRAY_NEW);
    assertThat(test.getYValues()).isEqualTo(YVALUE_BUMPED_ARRAY);
  }

  @Test
  public void test_withValuesXy_badSize() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withValues(DoubleArray.EMPTY, DoubleArray.EMPTY));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withValues(DoubleArray.of(4d), DoubleArray.of(6d, 0d)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    coverImmutableBean(test);
    ConstantNodalCurve test2 = ConstantNodalCurve.of(METADATA_ENTRIES, 55d, 23d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertSerialization(test);
  }

}
