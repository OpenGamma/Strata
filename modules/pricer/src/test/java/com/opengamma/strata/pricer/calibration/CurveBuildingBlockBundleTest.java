/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Test {@link CurveBuildingBlockBundle}.
 */
@Test
public class CurveBuildingBlockBundleTest {

  private static final CurveName NAME1 = CurveName.of("Test1");
  private static final CurveName NAME2 = CurveName.of("Test2");
  private static final CurveParameterSize CPS1 = CurveParameterSize.of(NAME1, 3);
  private static final CurveParameterSize CPS2 = CurveParameterSize.of(NAME2, 4);
  private static final CurveBuildingBlock BLOCK1 = CurveBuildingBlock.of(ImmutableList.of(CPS1, CPS2));
  private static final CurveBuildingBlock BLOCK2 = CurveBuildingBlock.of(ImmutableList.of(CPS2));
  private static final DoubleMatrix2D MATRIX = DoubleMatrix2D.EMPTY_MATRIX;
  private static final Pair<CurveBuildingBlock, DoubleMatrix2D> PAIR = Pair.of(BLOCK1, MATRIX);
  private static final Pair<CurveBuildingBlock, DoubleMatrix2D> PAIR2 = Pair.of(BLOCK2, MATRIX);

  //-------------------------------------------------------------------------
  public void test_of() {
    CurveBuildingBlockBundle test = CurveBuildingBlockBundle.of(ImmutableMap.of(NAME1, PAIR));
    assertEquals(test.getBlocks(), ImmutableMap.of(NAME1, PAIR));
    assertEquals(test.getBlock(NAME1), PAIR);
    assertThrowsIllegalArg(() -> test.getBlock(CurveName.of("NotFound")));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveBuildingBlockBundle test = CurveBuildingBlockBundle.of(ImmutableMap.of(NAME1, PAIR));
    coverImmutableBean(test);
    CurveBuildingBlockBundle test2 = CurveBuildingBlockBundle.of(ImmutableMap.of(NAME2, PAIR2));
    coverBeanEquals(test, test2);
  }

}
