/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import static com.opengamma.sesame.function.scenarios.curvedata.CurveDataPointShifts.PointShift;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.Tenor;

@Test(groups = TestGroup.UNIT)
public class CurveDataPointShiftsTest {

  private static final List<PointShift> SHIFT_LIST = Lists.newArrayList(PointShift.of(Tenor.ofMonths(3), 0.1),
                                                                        PointShift.of(Tenor.ofMonths(6), 0.2),
                                                                        PointShift.of(Tenor.ofMonths(9), 0.3),
                                                                        PointShift.of(Tenor.ofYears(1), 0.4));
  private static final CurveSpecificationMatcher MATCHER = CurveSpecificationMatcher.named(CurveTestUtils.CURVE_NAME);

  @Test
  public void absolute() {
    CurveDataPointShifts shifts = CurveDataPointShifts.absolute(MATCHER, SHIFT_LIST);
    Result<Map<ExternalIdBundle, Double>> result = shifts.apply(CurveTestUtils.CURVE_SPEC, CurveTestUtils.VALUE_MAP);

    assertTrue(result.isSuccess());
    CurveTestUtils.checkValues(result.getValue(), 0.2, 0.4, 0.4, 0.8);
  }

  @Test
  public void relative() {
    CurveDataPointShifts shifts = CurveDataPointShifts.relative(MATCHER, SHIFT_LIST);
    Result<Map<ExternalIdBundle, Double>> result = shifts.apply(CurveTestUtils.CURVE_SPEC, CurveTestUtils.VALUE_MAP);

    assertTrue(result.isSuccess());
    CurveTestUtils.checkValues(result.getValue(), 0.11, 0.24, 0.61, 0.56);
  }

  @Test
  public void noMatch() {
    CurveDataPointShifts shifts = CurveDataPointShifts.absolute(CurveSpecificationMatcher.named("a different curve"), SHIFT_LIST);
    Result<Map<ExternalIdBundle, Double>> result = shifts.apply(CurveTestUtils.CURVE_SPEC, CurveTestUtils.VALUE_MAP);

    assertTrue(result.isSuccess());
    CurveTestUtils.checkValues(result.getValue(), 0.1, 0.2, 0.7, 0.4);
  }

  @Test
  public void subsetOfPoints() {
    CurveDataPointShifts shifts = CurveDataPointShifts.absolute(MATCHER,
                                                                PointShift.of(Tenor.ofMonths(3), 0.1),
                                                                PointShift.of(Tenor.ofMonths(9), 0.3));
    Result<Map<ExternalIdBundle, Double>> result = shifts.apply(CurveTestUtils.CURVE_SPEC, CurveTestUtils.VALUE_MAP);

    assertTrue(result.isSuccess());
    CurveTestUtils.checkValues(result.getValue(), 0.2, 0.2, 0.4, 0.4);
  }

  @Test
  public void unknownTenor() {
    CurveDataPointShifts shifts = CurveDataPointShifts.absolute(MATCHER, PointShift.of(Tenor.ofMonths(2), 0.1));
    Result<Map<ExternalIdBundle, Double>> result = shifts.apply(CurveTestUtils.CURVE_SPEC, CurveTestUtils.VALUE_MAP);

    assertFalse(result.isSuccess());
  }
}
