/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// NOTE: this is from OG-Maths

/**
 * Tests for values being equal allowing for a level of floating point fuzz
 * Based on the OG-Maths C++ fuzzy equals test code.
 */
public class FuzzyEqualsTest {

  double getNaN() {
    return Double.longBitsToDouble(0x7FF1010101010101L);
  }

  double getPosInf() {
    return Double.longBitsToDouble(0x7FF0000000000000L);
  }

  double getNegInf() {
    return Double.longBitsToDouble(0xFFF0000000000000L);
  }

  double getNegZero() {
    return Double.longBitsToDouble(8000000000000000L);
  }

  @Test
  public void testEquals_SingleValueFuzzyEqualsDouble() {
    double nanValue = getNaN();
    double pinf = getPosInf();
    double ninf = getNegInf();
    double neg0 = getNegZero();

    // NaN branch
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(nanValue, nanValue)).isFalse();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(nanValue, 1)).isFalse();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(1, nanValue)).isFalse();

    // Inf branches
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(pinf, pinf)).isTrue();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(ninf, ninf)).isTrue();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(pinf, ninf)).isFalse();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(ninf, pinf)).isFalse();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(pinf, Double.MAX_VALUE)).isFalse();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(ninf, -Double.MAX_VALUE)).isFalse();

    // val 0 branches
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(0.e0, 0.e0)).isTrue();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(0.e0, neg0)).isTrue();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(neg0, 0.e0)).isTrue();
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(neg0, neg0)).isTrue();

    // same value as it trips the return true on "difference less than abs tol" branch
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(FuzzyEquals.getEps(), 2.e0 * FuzzyEquals.getEps())).isTrue();

    // same value as it trips the return true on "difference less than relative error" branch
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(1.e308, 9.99999999999999e0 * 1.e307)).isTrue();

    // fail, just plain different
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(1.e0, 2.e0)).isFalse();

  }

  @Test
  public void testEquals_ArrayFuzzyEqualsDouble() {

    double[] data = {1.0e0, 2.0e0, 3.0e0, 4.0e0};
    double[] same = {1.0e0, 2.0e0, 3.0e0, 4.0e0};
    double[] diff = {-1.0e0, 2.0e0, 3.0e0, 4.0e0};
    double[] lendiff = {-1.0e0, 2.0e0, 3.0e0};

    assertThat(FuzzyEquals.ArrayFuzzyEquals(data, lendiff)).isFalse();
    assertThat(FuzzyEquals.ArrayFuzzyEquals(data, diff)).isFalse();
    assertThat(FuzzyEquals.ArrayFuzzyEquals(data, same)).isTrue();
  }

  @Test
  public void testEquals_ArrayOfArraysFuzzyEqualsDouble() {

    double[][] data = {{1.0e0, 2.0e0, 3.0e0, 4.0e0}, {5.e0, 6.e0, 7.e0, 8.e0}, {9.e0, 10.e0, 11.e0, 12.e0}};
    double[][] same = {{1.0e0, 2.0e0, 3.0e0, 4.0e0}, {5.e0, 6.e0, 7.e0, 8.e0}, {9.e0, 10.e0, 11.e0, 12.e0}};
    double[][] diffvalue = {{-1.0e0, 2.0e0, 3.0e0, 4.0e0}, {5.e0, 6.e0, 7.e0, 8.e0}, {9.e0, 10.e0, 11.e0, 12.e0}};
    double[][] diffrowlen = {{1.0e0, 2.0e0, 3.0e0, 4.0e0}, {5.e0, 6.e0, 7.e0}, {9.e0, 10.e0, 11.e0, 12.e0}};
    double[][] diffrowcount = {{1.0e0, 2.0e0, 3.0e0, 4.0e0}, {5.e0, 6.e0, 7.e0, 8.e0}};

    assertThat(FuzzyEquals.ArrayFuzzyEquals(data, diffvalue)).isFalse();
    assertThat(FuzzyEquals.ArrayFuzzyEquals(data, diffrowlen)).isFalse();
    assertThat(FuzzyEquals.ArrayFuzzyEquals(data, diffrowcount)).isFalse();
    assertThat(FuzzyEquals.ArrayFuzzyEquals(data, same)).isTrue();

    assertThat(
        FuzzyEquals.ArrayFuzzyEquals(data, diffvalue, FuzzyEquals.getDefaultTolerance(), FuzzyEquals.getDefaultTolerance()))
            .isFalse();
    assertThat(
        FuzzyEquals.ArrayFuzzyEquals(data, diffrowlen, FuzzyEquals.getDefaultTolerance(), FuzzyEquals.getDefaultTolerance()))
            .isFalse();
    assertThat(
        FuzzyEquals.ArrayFuzzyEquals(data, diffrowcount, FuzzyEquals.getDefaultTolerance(), FuzzyEquals.getDefaultTolerance()))
            .isFalse();
    assertThat(FuzzyEquals.ArrayFuzzyEquals(data, same, FuzzyEquals.getDefaultTolerance(), FuzzyEquals.getDefaultTolerance()))
        .isTrue();

    // same value as it trips the return true on "difference less than abs tol" branch
    assertThat(FuzzyEquals.ArrayFuzzyEquals(new double[][] {{FuzzyEquals.getEps()}},
        new double[][] {{2.e0 * FuzzyEquals.getEps()}}, FuzzyEquals.getDefaultTolerance(),
        FuzzyEquals.getDefaultTolerance())).isTrue();

    // same value as it trips the return true on "difference less than relative error" branch
    assertThat(FuzzyEquals.ArrayFuzzyEquals(new double[][] {{1.e308}}, new double[][] {{9.99999999999999e0 * 1.e307}},
        FuzzyEquals.getDefaultTolerance(), FuzzyEquals.getDefaultTolerance())).isTrue();

  }

  @Test
  public void testEquals_CheckEPSIsAppropriatelySmall() {
    assertThat(FuzzyEquals.getEps() < 5e-16).isTrue();
  }

  @Test
  public void testEquals_CheckDefaultToleranceAppropriatelySmall() {
    assertThat(FuzzyEquals.getEps() < 10 * 5e-16).isTrue();
  }

}
