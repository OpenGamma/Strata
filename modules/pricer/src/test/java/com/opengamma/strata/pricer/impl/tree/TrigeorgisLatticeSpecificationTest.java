/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link TrigeorgisLatticeSpecification}.
 */
@Test
public class TrigeorgisLatticeSpecificationTest {

  private static final int NUM = 35;
  private static final double VOL = 0.13;
  private static final double RATE = 0.03;
  private static final double DT = 2d / NUM;

  public void test_formula() {
    TrigeorgisLatticeSpecification test = new TrigeorgisLatticeSpecification();
    DoubleArray computed = test.getParametersTrinomial(VOL, RATE, DT);
    double dx = VOL * Math.sqrt(3d * DT);
    double nu = RATE - 0.5 * VOL * VOL;
    double u = Math.exp(dx);
    double d = Math.exp(-dx);
    double up = 0.5 * ((VOL * VOL * DT + nu * nu * DT * DT) / (dx * dx) + nu * DT / dx);
    double dm = 1d - (VOL * VOL * DT + nu * nu * DT * DT) / (dx * dx);
    double dp = 0.5 * ((VOL * VOL * DT + nu * nu * DT * DT) / (dx * dx) - nu * DT / dx);
    DoubleArray expected = DoubleArray.of(u, 1d, d, up, dm, dp);
    assertTrue(DoubleArrayMath.fuzzyEquals(computed.toArray(), expected.toArray(), 1.0e-14));
  }

}
