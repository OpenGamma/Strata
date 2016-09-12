/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.FunctionUtils;

/**
 * Test.
 */
@Test
public class BasisFunctionGeneratorTest {
  private static final BasisFunctionGenerator GENERATOR = new BasisFunctionGenerator();
  private static final double[] KNOTS;

  static {
    final int n = 10;
    KNOTS = new double[n + 1];
    for (int i = 0; i < n + 1; i++) {
      KNOTS[i] = 0 + i * 1.0;
    }

  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFunctionIndexOutOfRange1() {
    BasisFunctionKnots k = BasisFunctionKnots.fromKnots(KNOTS, 2);
    GENERATOR.generate(k, -1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFunctionIndexOutOfRange2() {
    BasisFunctionKnots k = BasisFunctionKnots.fromKnots(KNOTS, 5);
    int nS = k.getNumSplines();
    GENERATOR.generate(k, nS);
  }

  @Test
  public void testZeroOrder() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 0);
    final Function<Double, Double> func = GENERATOR.generate(knots, 4);
    assertEquals(0.0, func.apply(3.5), 0.0);
    assertEquals(1.0, func.apply(4.78), 0.0);
    assertEquals(1.0, func.apply(4.0), 0.0);
    assertEquals(0.0, func.apply(5.0), 0.0);
  }

  @Test
  public void testFirstOrder() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 1);
    final Function<Double, Double> func = GENERATOR.generate(knots, 3);
    assertEquals(0.0, func.apply(1.76), 0.0);
    assertEquals(1.0, func.apply(3.0), 0.0);
    assertEquals(0, func.apply(4.0), 0.0);
    assertEquals(0.5, func.apply(2.5), 0.0);
  }

  @Test
  public void testSecondOrder() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 2);
    final Function<Double, Double> func = GENERATOR.generate(knots, 3);
    assertEquals(0.0, func.apply(0.76), 0.0);
    assertEquals(0.125, func.apply(1.5), 0.0);
    assertEquals(0.5, func.apply(2.0), 0.0);
    assertEquals(0.75, func.apply(2.5), 0.0);
    assertEquals(0.0, func.apply(4.0), 0.0);
  }

  @Test
  public void testThirdOrder() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 3);
    final Function<Double, Double> func = GENERATOR.generate(knots, 3);
    assertEquals(0.0, func.apply(-0.1), 0.0);
    assertEquals(1. / 6., func.apply(1.0), 0.0);
    assertEquals(2. / 3., func.apply(2.0), 0.0);
    assertEquals(1 / 48., func.apply(3.5), 0.0);
    assertEquals(0.0, func.apply(4.0), 0.0);
  }

  @Test
  public void testTwoD() {

    BasisFunctionKnots knots1 = BasisFunctionKnots.fromInternalKnots(KNOTS, 2);
    BasisFunctionKnots knots2 = BasisFunctionKnots.fromInternalKnots(KNOTS, 3);
    List<Function<double[], Double>> set = GENERATOR.generateSet(new BasisFunctionKnots[] {knots1, knots2 });

    //pick of one of the basis functions for testing 
    int index = FunctionUtils.toTensorIndex(new int[] {3, 3 }, new int[] {knots1.getNumSplines(), knots2.getNumSplines() });
    Function<double[], Double> func = set.get(index);
    assertEquals(1. / 3., func.apply(new double[] {2.0, 2.0 }), 0.0);
    assertEquals(1. / 2., func.apply(new double[] {2.5, 2.0 }), 0.0);
    assertEquals(1. / 8. / 48., func.apply(new double[] {1.5, 3.5 }), 0.0);
    assertEquals(0.0, func.apply(new double[] {4.0, 2.5 }), 0.0);
  }

  @Test
  public void testThreeD() {
    BasisFunctionKnots knots1 = BasisFunctionKnots.fromInternalKnots(KNOTS, 2);
    BasisFunctionKnots knots2 = BasisFunctionKnots.fromInternalKnots(KNOTS, 3);
    BasisFunctionKnots knots3 = BasisFunctionKnots.fromInternalKnots(KNOTS, 1);
    List<Function<double[], Double>> set = GENERATOR.generateSet(new BasisFunctionKnots[] {knots1, knots2, knots3 });

    //pick of one of the basis functions for testing 
    int index = FunctionUtils.toTensorIndex(new int[] {3, 3, 3 }, new int[] {knots1.getNumSplines(), knots2.getNumSplines(),
      knots3.getNumSplines() });
    Function<double[], Double> func = set.get(index);
    assertEquals(1. / 3., func.apply(new double[] {2.0, 2.0, 3.0 }), 0.0);
  }

}
