/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class CurveSpecificationMatcherTest {

  private static final String SPEC_NAME1 = "curveSpec1";
  private static final String SPEC_NAME2 = "curveSpec2";

  private static final CurveSpecification CURVE_SPEC1 =
      new CurveSpecification(LocalDate.now(), SPEC_NAME1, Collections.<CurveNodeWithIdentifier>emptyList());
  private static final CurveSpecification CURVE_SPEC2 =
      new CurveSpecification(LocalDate.now(), SPEC_NAME2, Collections.<CurveNodeWithIdentifier>emptyList());
  private static final CurveSpecification OTHER_SPEC =
      new CurveSpecification(LocalDate.now(), "otherSpec", Collections.<CurveNodeWithIdentifier>emptyList());

  @Test
  public void named() {
    CurveSpecificationMatcher matcher = CurveSpecificationMatcher.named(SPEC_NAME1, SPEC_NAME2);
    assertTrue(matcher.matches(CURVE_SPEC1));
    assertTrue(matcher.matches(CURVE_SPEC2));
    assertFalse(matcher.matches(OTHER_SPEC));
  }

  @Test
  public void nameLike() {
    CurveSpecificationMatcher curveSpecMatcher = CurveSpecificationMatcher.nameLike("curveSpec?");
    assertTrue(curveSpecMatcher.matches(CURVE_SPEC1));
    assertTrue(curveSpecMatcher.matches(CURVE_SPEC2));
    assertFalse(curveSpecMatcher.matches(OTHER_SPEC));

    CurveSpecificationMatcher oneMatcher = CurveSpecificationMatcher.nameLike("*1");
    assertTrue(oneMatcher.matches(CURVE_SPEC1));
    assertFalse(oneMatcher.matches(CURVE_SPEC2));
    assertFalse(oneMatcher.matches(OTHER_SPEC));
  }
}
