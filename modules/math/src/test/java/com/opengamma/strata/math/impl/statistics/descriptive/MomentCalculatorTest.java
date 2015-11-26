/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static org.testng.Assert.assertEquals;

import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.statistics.distribution.ChiSquareDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.StudentTDistribution;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

/**
 * Test.
 */
@Test
public class MomentCalculatorTest {

  private static final double STD = 2.;
  private static final double DOF = 10;
  private static final Function<double[], Double> SAMPLE_VARIANCE = new SampleVarianceCalculator();
  private static final Function<double[], Double> POPULATION_VARIANCE = new PopulationVarianceCalculator();
  private static final Function<double[], Double> SAMPLE_STD = new SampleStandardDeviationCalculator();
  private static final Function<double[], Double> POPULATION_STD = new PopulationStandardDeviationCalculator();
  private static final Function<double[], Double> SAMPLE_SKEWNESS = new SampleSkewnessCalculator();
  private static final Function<double[], Double> SAMPLE_FISHER_KURTOSIS = new SampleFisherKurtosisCalculator();
  private static final RandomEngine ENGINE = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, STD, ENGINE);
  private static final ProbabilityDistribution<Double> STUDENT_T = new StudentTDistribution(DOF, ENGINE);
  private static final ProbabilityDistribution<Double> CHI_SQ = new ChiSquareDistribution(DOF, ENGINE);
  private static final double[] NORMAL_DATA = new double[500000];
  private static final double[] STUDENT_T_DATA = new double[500000];
  private static final double[] CHI_SQ_DATA = new double[500000];
  private static final double EPS = 0.1;
  static {
    for (int i = 0; i < 500000; i++) {
      NORMAL_DATA[i] = NORMAL.nextRandom();
      STUDENT_T_DATA[i] = STUDENT_T.nextRandom();
      CHI_SQ_DATA[i] = CHI_SQ.nextRandom();
    }
  }

  @Test
  public void testNull() {
    assertNullArg(SAMPLE_VARIANCE);
    assertNullArg(SAMPLE_STD);
    assertNullArg(POPULATION_VARIANCE);
    assertNullArg(POPULATION_STD);
    assertNullArg(SAMPLE_SKEWNESS);
    assertNullArg(SAMPLE_FISHER_KURTOSIS);
  }

  @Test
  public void testInsufficientData() {
    assertInsufficientData(SAMPLE_VARIANCE);
    assertInsufficientData(SAMPLE_STD);
    assertInsufficientData(POPULATION_VARIANCE);
    assertInsufficientData(POPULATION_STD);
    assertInsufficientData(SAMPLE_SKEWNESS);
    assertInsufficientData(SAMPLE_FISHER_KURTOSIS);
  }

  private void assertNullArg(Function<double[], Double> f) {
    try {
      f.apply((double[]) null);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  private void assertInsufficientData(Function<double[], Double> f) {
    try {
      f.apply(new double[] {1.});
      Assert.fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  @Test
  public void testNormal() {
    assertEquals(SAMPLE_VARIANCE.apply(NORMAL_DATA), STD * STD, EPS);
    assertEquals(POPULATION_VARIANCE.apply(NORMAL_DATA), STD * STD, EPS);
    assertEquals(SAMPLE_STD.apply(NORMAL_DATA), STD, EPS);
    assertEquals(POPULATION_STD.apply(NORMAL_DATA), STD, EPS);
    assertEquals(SAMPLE_SKEWNESS.apply(NORMAL_DATA), 0., EPS);
    assertEquals(SAMPLE_FISHER_KURTOSIS.apply(NORMAL_DATA), 0., EPS);
  }

  @Test
  public void testStudentT() {
    double variance = DOF / (DOF - 2);
    assertEquals(SAMPLE_VARIANCE.apply(STUDENT_T_DATA), variance, EPS);
    assertEquals(POPULATION_VARIANCE.apply(STUDENT_T_DATA), variance, EPS);
    assertEquals(SAMPLE_STD.apply(STUDENT_T_DATA), Math.sqrt(variance), EPS);
    assertEquals(POPULATION_STD.apply(STUDENT_T_DATA), Math.sqrt(variance), EPS);
    assertEquals(SAMPLE_SKEWNESS.apply(STUDENT_T_DATA), 0., EPS);
    assertEquals(SAMPLE_FISHER_KURTOSIS.apply(STUDENT_T_DATA), 6 / (DOF - 4), EPS);
  }

  @Test
  public void testChiSq() {
    double variance = 2 * DOF;
    assertEquals(SAMPLE_VARIANCE.apply(CHI_SQ_DATA), variance, EPS);
    assertEquals(POPULATION_VARIANCE.apply(CHI_SQ_DATA), variance, EPS);
    assertEquals(SAMPLE_STD.apply(CHI_SQ_DATA), Math.sqrt(variance), EPS);
    assertEquals(POPULATION_STD.apply(CHI_SQ_DATA), Math.sqrt(variance), EPS);
    assertEquals(SAMPLE_SKEWNESS.apply(CHI_SQ_DATA), Math.sqrt(8 / DOF), EPS);
    assertEquals(SAMPLE_FISHER_KURTOSIS.apply(CHI_SQ_DATA), 12 / DOF, EPS);
  }

}
