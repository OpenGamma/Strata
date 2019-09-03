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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test {@link JacobianCalibrationMatrix}.
 */
public class JacobianCalibrationMatrixTest {

  private static final CurveName NAME1 = CurveName.of("Test1");
  private static final CurveName NAME2 = CurveName.of("Test2");
  private static final CurveName NAME3 = CurveName.of("Test3");
  private static final CurveParameterSize CPS1 = CurveParameterSize.of(NAME1, 3);
  private static final CurveParameterSize CPS2 = CurveParameterSize.of(NAME2, 2);
  private static final List<CurveParameterSize> CPS = ImmutableList.of(CPS1, CPS2);
  private static final DoubleMatrix MATRIX = DoubleMatrix.of(2, 2, 1d, 2d, 2d, 3d);
  private static final DoubleMatrix MATRIX2 = DoubleMatrix.of(2, 2, 2d, 2d, 3d, 3d);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    JacobianCalibrationMatrix test = JacobianCalibrationMatrix.of(CPS, MATRIX);
    assertThat(test.getOrder()).isEqualTo(CPS);
    assertThat(test.getJacobianMatrix()).isEqualTo(MATRIX);
    assertThat(test.getCurveCount()).isEqualTo(2);
    assertThat(test.getTotalParameterCount()).isEqualTo(5);
    assertThat(test.containsCurve(NAME1)).isTrue();
    assertThat(test.containsCurve(NAME2)).isTrue();
    assertThat(test.containsCurve(NAME3)).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_split() {
    JacobianCalibrationMatrix test = JacobianCalibrationMatrix.of(CPS, MATRIX);
    DoubleArray array = DoubleArray.of(1, 2, 3, 4, 5);
    DoubleArray array1 = DoubleArray.of(1, 2, 3);
    DoubleArray array2 = DoubleArray.of(4, 5);
    assertThat(test.splitValues(array)).isEqualTo(ImmutableMap.of(NAME1, array1, NAME2, array2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    JacobianCalibrationMatrix test = JacobianCalibrationMatrix.of(CPS, MATRIX);
    coverImmutableBean(test);
    JacobianCalibrationMatrix test2 = JacobianCalibrationMatrix.of(ImmutableList.of(CPS1), MATRIX2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    JacobianCalibrationMatrix test = JacobianCalibrationMatrix.of(CPS, MATRIX);
    assertSerialization(test);
  }

}
