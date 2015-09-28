/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CurveBuildingBlock}.
 */
@Test
public class CurveBuildingBlockTest {

  private static final CurveName NAME1 = CurveName.of("Test1");
  private static final CurveName NAME2 = CurveName.of("Test2");
  private static final CurveParameterSize CPS1 = CurveParameterSize.of(NAME1, 3);
  private static final CurveParameterSize CPS2 = CurveParameterSize.of(NAME2, 4);

  //-------------------------------------------------------------------------
  public void test_of() {
    CurveBuildingBlock test = CurveBuildingBlock.of(ImmutableList.of(CPS1, CPS2));
    assertEquals(test.getData(), ImmutableList.of(CPS1, CPS2));
    assertEquals(test.getAllNames(), ImmutableSet.of(NAME1, NAME2));
    assertEquals(test.getCurveCount(), 2);
    assertEquals(test.getTotalParameterCount(), 7);
    assertEquals(test.getStart(NAME1), 0);
    assertEquals(test.getParameterCount(NAME1), 3);
    assertEquals(test.getStart(NAME2), 3);
    assertEquals(test.getParameterCount(NAME2), 4);
    assertThrowsIllegalArg(() -> test.getStart(CurveName.of("NotFound")));
    assertThrowsIllegalArg(() -> test.getParameterCount(CurveName.of("NotFound")));
  }

  //-------------------------------------------------------------------------
  public void test_split() {
    CurveBuildingBlock test = CurveBuildingBlock.of(ImmutableList.of(CPS1, CPS2));
    double[] array = {1, 2, 3, 4, 5, 6, 7};
    double[] array1 = {1, 2, 3};
    double[] array2 = {4, 5, 6, 7};
    assertEquals(test.splitValues(array), ImmutableMap.of(NAME1, array1, NAME2, array2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveBuildingBlock test = CurveBuildingBlock.of(ImmutableList.of(CPS1, CPS2));
    coverImmutableBean(test);
    CurveBuildingBlock test2 = CurveBuildingBlock.of(ImmutableList.of(CPS2));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveBuildingBlock test = CurveBuildingBlock.of(ImmutableList.of(CPS1, CPS2));
    assertSerialization(test);
  }

}
