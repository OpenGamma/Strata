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
 * Test {@link CoxRossRubinsteinLatticeSpecification}.
 */
@Test
public class CoxRossRubinsteinLatticeSpecificationTest {

  private static final int NUM = 35;
  private static final double VOL = 0.12;
  private static final double RATE = 0.03;
  private static final double DT = 2d / NUM;

  public void test_formula() {
    CoxRossRubinsteinLatticeSpecification test = new CoxRossRubinsteinLatticeSpecification();
    DoubleArray computed = test.getParametersTrinomial(VOL, RATE, DT);
    double u = Math.exp(VOL * Math.sqrt(2.0 * DT));
    double d = Math.exp(-VOL * Math.sqrt(2.0 * DT));
    double up = Math.pow((Math.exp(0.5 * RATE * DT) - Math.exp(-VOL * Math.sqrt(0.5 * DT)))
        / (Math.exp(VOL * Math.sqrt(0.5 * DT)) - Math.exp(-VOL * Math.sqrt(0.5 * DT))), 2);
    double dp = Math.pow((Math.exp(VOL * Math.sqrt(0.5 * DT)) - Math.exp(0.5 * RATE * DT))
        / (Math.exp(VOL * Math.sqrt(0.5 * DT)) - Math.exp(-VOL * Math.sqrt(0.5 * DT))), 2);
    DoubleArray expected = DoubleArray.of(u, 1d, d, up, 1d - up - dp, dp);
    assertTrue(DoubleArrayMath.fuzzyEquals(computed.toArray(), expected.toArray(), 1.0e-14));
  }

}
