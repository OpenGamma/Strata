/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.linearalgebra.SVDecompositionCommons;

/**
 * Test.
 */
@Test
public class BroydenVectorRootFinderTest extends VectorRootFinderTest {
  private static final BaseNewtonVectorRootFinder DEFAULT = new BroydenVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS);
  private static final BaseNewtonVectorRootFinder SV = new BroydenVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS, new SVDecompositionCommons());
  private static final BaseNewtonVectorRootFinder DEFAULT_JACOBIAN_2D = new BroydenVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS);
  private static final BaseNewtonVectorRootFinder SV_JACOBIAN_2D = new BroydenVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS, new SVDecompositionCommons());
  private static final BaseNewtonVectorRootFinder DEFAULT_JACOBIAN_3D = new BroydenVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS);
  private static final BaseNewtonVectorRootFinder SV_JACOBIAN_3D = new BroydenVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS, new SVDecompositionCommons());

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSingular1() {
    assertFunction2D(DEFAULT, EPS);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSingular2() {
    assertFunction2D(DEFAULT_JACOBIAN_2D, EPS);
  }

  @Test
  public void test() {
    assertLinear(DEFAULT, EPS);
    assertLinear(SV, EPS);
    assertFunction2D(SV, EPS);
    assertFunction2D(SV_JACOBIAN_2D, EPS);
    assertFunction3D(DEFAULT, EPS);
    assertFunction3D(DEFAULT_JACOBIAN_3D, EPS);
    assertFunction3D(SV, EPS);
    assertFunction3D(SV_JACOBIAN_3D, EPS);
    assertYieldCurveBootstrap(DEFAULT, EPS);
  }
}
