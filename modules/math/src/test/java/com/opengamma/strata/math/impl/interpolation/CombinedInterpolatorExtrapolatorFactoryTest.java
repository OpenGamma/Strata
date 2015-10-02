/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class CombinedInterpolatorExtrapolatorFactoryTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadInterpolatorName1() {
    CombinedInterpolatorExtrapolatorFactory.getInterpolator("Wrong name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadInterpolatorName2() {
    CombinedInterpolatorExtrapolatorFactory.getInterpolator("Wrong name", Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadInterpolatorName3() {
    CombinedInterpolatorExtrapolatorFactory.getInterpolator("Wrong name", Interpolator1DFactory.FLAT_EXTRAPOLATOR, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadExtrapolatorName1() {
    CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, "Wrong name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadExtrapolatorName2() {
    CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, "Wrong name", Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadExtrapolatorName3() {
    CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, Interpolator1DFactory.FLAT_EXTRAPOLATOR, "Wrong name");
  }

  @Test
  public void testNullExtrapolatorName() {
    final CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, null);
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertTrue(combined.getLeftExtrapolator() instanceof InterpolatorExtrapolator);
    assertTrue(combined.getRightExtrapolator() instanceof InterpolatorExtrapolator);
  }

  @Test
  public void testEmptyExtrapolatorName() {
    final CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, "");
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertTrue(combined.getLeftExtrapolator() instanceof InterpolatorExtrapolator);
    assertTrue(combined.getRightExtrapolator() instanceof InterpolatorExtrapolator);
  }

  @Test
  public void testNullLeftExtrapolatorName() {
    final CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, null, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertEquals(combined.getLeftExtrapolator().getClass(), FlatExtrapolator1D.class);
    assertEquals(combined.getRightExtrapolator().getClass(), FlatExtrapolator1D.class);
  }

  @Test
  public void testEmptyLeftExtrapolatorName() {
    final CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, "", Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertEquals(combined.getLeftExtrapolator().getClass(), FlatExtrapolator1D.class);
    assertEquals(combined.getRightExtrapolator().getClass(), FlatExtrapolator1D.class);
  }

  @Test
  public void testNullRightExtrapolatorName() {
    final CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, Interpolator1DFactory.FLAT_EXTRAPOLATOR, null);
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertEquals(combined.getLeftExtrapolator().getClass(), FlatExtrapolator1D.class);
    assertEquals(combined.getRightExtrapolator().getClass(), FlatExtrapolator1D.class);
  }

  @Test
  public void testEmptyRightExtrapolatorName() {
    final CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, Interpolator1DFactory.FLAT_EXTRAPOLATOR, "");
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertEquals(combined.getLeftExtrapolator().getClass(), FlatExtrapolator1D.class);
    assertEquals(combined.getRightExtrapolator().getClass(), FlatExtrapolator1D.class);
  }

  @Test
  public void testNullLeftAndRightExtrapolatorName() {
    final CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, null, null);
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertTrue(combined.getLeftExtrapolator() instanceof InterpolatorExtrapolator);
    assertTrue(combined.getRightExtrapolator() instanceof InterpolatorExtrapolator);
  }

  @Test
  public void testEmptyLeftAndRightExtrapolatorName() {
    final CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, "", "");
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertTrue(combined.getLeftExtrapolator() instanceof InterpolatorExtrapolator);
    assertTrue(combined.getRightExtrapolator() instanceof InterpolatorExtrapolator);
  }

  @Test
  public void testNoExtrapolator() {
    CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR);
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertTrue(combined.getLeftExtrapolator() instanceof InterpolatorExtrapolator);
    assertTrue(combined.getRightExtrapolator() instanceof InterpolatorExtrapolator);
    combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.NATURAL_CUBIC_SPLINE);
    assertEquals(combined.getInterpolator().getClass(), NaturalCubicSplineInterpolator1D.class);
  }

  @Test
  public void testOneExtrapolator() {
    CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertEquals(combined.getLeftExtrapolator().getClass(), FlatExtrapolator1D.class);
    assertEquals(combined.getRightExtrapolator().getClass(), FlatExtrapolator1D.class);
    combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.NATURAL_CUBIC_SPLINE, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    assertEquals(combined.getInterpolator().getClass(), NaturalCubicSplineInterpolator1D.class);
    assertEquals(combined.getLeftExtrapolator().getClass(), FlatExtrapolator1D.class);
    assertEquals(combined.getRightExtrapolator().getClass(), FlatExtrapolator1D.class);
  }

  @Test
  public void testTwoExtrapolators() {
    CombinedInterpolatorExtrapolator combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, Interpolator1DFactory.FLAT_EXTRAPOLATOR,
        Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    assertEquals(combined.getInterpolator().getClass(), LinearInterpolator1D.class);
    assertEquals(combined.getLeftExtrapolator().getClass(), FlatExtrapolator1D.class);
    assertEquals(combined.getRightExtrapolator().getClass(), LinearExtrapolator1D.class);
    combined = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.NATURAL_CUBIC_SPLINE, Interpolator1DFactory.FLAT_EXTRAPOLATOR, Interpolator1DFactory.LINEAR_EXTRAPOLATOR);
    assertEquals(combined.getInterpolator().getClass(), NaturalCubicSplineInterpolator1D.class);
    assertEquals(combined.getLeftExtrapolator().getClass(), FlatExtrapolator1D.class);
    assertEquals(combined.getRightExtrapolator().getClass(), LinearExtrapolator1D.class);
  }
}
