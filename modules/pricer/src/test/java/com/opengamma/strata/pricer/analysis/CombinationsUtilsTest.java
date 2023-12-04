/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;

public class CombinationsUtilsTest {

  @Test
  public void test1() {
    double[][] inputs = {
        {1.0, 2.0, 3.0}};
    double[][] results = CombinationsUtils.combinations(inputs);
    System.out.println(DoubleMatrix.ofUnsafe(results));
  }

  @Test
  public void test2() {
    double[][] inputs = {
        {1.0, 2.0, 3.0},
        {4.0, 5.0}};
    double[][] results = CombinationsUtils.combinations(inputs);
    System.out.println(DoubleMatrix.ofUnsafe(results));
  }
  
  @Test
  public void test3() {
    double[][] inputs = {
        {1.0, 2.0, 3.0},
        {4.0, 5.0},
        {6.0, 7.0, 8.0, 9.0}};
    double[][] results = CombinationsUtils.combinations(inputs);
    System.out.println(DoubleMatrix.ofUnsafe(results));
  }

}
