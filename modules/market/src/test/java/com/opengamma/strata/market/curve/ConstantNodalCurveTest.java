/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link ConstantNodalCurve}.
 */
@Test
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
  private static final DoubleArray XVALUE = DoubleArray.of(2d);
  private static final DoubleArray YVALUE = DoubleArray.of(7d);
  private static final DoubleArray XVALUE_NEW = DoubleArray.of(3d);
  private static final DoubleArray YVALUE_BUMPED = DoubleArray.of(5d);

  //-------------------------------------------------------------------------
  public void test_of_CurveMetadata() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE);
    ConstantNodalCurve testRe = ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE.get(0), YVALUE.get(0));
    assertThat(test).isEqualTo(testRe);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getParameter(0)).isEqualTo(YVALUE.get(0));
    assertThrowsIllegalArg(() -> test.getParameter(1));
    assertThat(test.getParameterMetadata(0)).isSameAs(METADATA_ENTRIES.getParameterMetadata().get().get(0));
    assertThat(test.withParameter(0, 2d)).isEqualTo(
        ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE.with(0, 2d)));
    assertThrowsIllegalArg(() -> test.withParameter(1, 2d));
    assertThat(test.withPerturbation((i, v, m) -> v - 2d)).isEqualTo(
        ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE_BUMPED));
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUE);
    assertThat(test.getYValues()).isEqualTo(YVALUE);
  }

  public void test_of_noCurveMetadata() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA_NOPARAM, XVALUE, YVALUE);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getParameter(0)).isEqualTo(YVALUE.get(0));
    assertThat(test.getParameterMetadata(0)).isEqualTo(SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, XVALUE.get(0)));
  }

  //-------------------------------------------------------------------------
  public void test_withNode() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE);
    SimpleCurveParameterMetadata param = SimpleCurveParameterMetadata.of(ValueType.YEAR_FRACTION, XVALUE.get(0));
    ConstantNodalCurve test = base.withNode(XVALUE.get(0), 2d, param);
    assertThat(test.getXValue()).isEqualTo(XVALUE.get(0));
    assertThat(test.getYValue()).isEqualTo(2d);
    assertThat(test.getParameterMetadata(0)).isEqualTo(param);
  }

  public void test_withNode_invalid() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA_ENTRIES, XVALUE, YVALUE);
    assertThrowsIllegalArg(() -> test.withNode(1, 2, ParameterMetadata.empty()));
  }

  //-------------------------------------------------------------------------
  public void test_values() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertThat(test.yValue(10.2421)).isEqualTo(YVALUE.get(0));
    assertThat(test.yValueParameterSensitivity(10.2421).getMarketDataName()).isEqualTo(CURVE_NAME);
    assertThat(test.yValueParameterSensitivity(10.2421).getSensitivity()).isEqualTo(DoubleArray.of(1d));
    assertThat(test.firstDerivative(10.2421)).isEqualTo(0d);
  }

  //-------------------------------------------------------------------------
  public void test_withMetadata() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    ConstantNodalCurve test = base.withMetadata(METADATA_ENTRIES);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUE);
    assertThat(test.getYValues()).isEqualTo(YVALUE);
  }

  public void test_withMetadata_badSize() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertThrowsIllegalArg(() -> base.withMetadata(METADATA_ENTRIES2));
  }

  //-------------------------------------------------------------------------
  public void test_withValues() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    ConstantNodalCurve test = base.withYValues(YVALUE_BUMPED);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUE);
    assertThat(test.getYValues()).isEqualTo(YVALUE_BUMPED);
  }

  public void test_withValues_badSize() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertThrowsIllegalArg(() -> base.withYValues(DoubleArray.EMPTY));
    assertThrowsIllegalArg(() -> base.withYValues(DoubleArray.of(4d, 6d)));
  }

  //-------------------------------------------------------------------------
  public void test_withValuesXy() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    ConstantNodalCurve test = base.withValues(XVALUE_NEW, YVALUE_BUMPED);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUE_NEW);
    assertThat(test.getYValues()).isEqualTo(YVALUE_BUMPED);
  }

  public void test_withValuesXy_badSize() {
    ConstantNodalCurve base = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertThrowsIllegalArg(() -> base.withValues(DoubleArray.EMPTY, DoubleArray.EMPTY));
    assertThrowsIllegalArg(() -> base.withValues(DoubleArray.of(4d), DoubleArray.of(6d, 0d)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    coverImmutableBean(test);
    ConstantNodalCurve test2 = ConstantNodalCurve.of(METADATA_ENTRIES, 55d, 23d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ConstantNodalCurve test = ConstantNodalCurve.of(METADATA, XVALUE, YVALUE);
    assertSerialization(test);
  }

}
