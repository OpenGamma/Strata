/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.cern.MersenneTwister;
import com.opengamma.strata.math.impl.cern.MersenneTwister64;
import com.opengamma.strata.math.impl.cern.RandomEngine;
import com.opengamma.strata.math.impl.statistics.distribution.ChiSquareDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.StudentTDistribution;

/**
 * Test.
 */
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> f.apply((double[]) null));
  }

  private void assertInsufficientData(Function<double[], Double> f) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> f.apply(new double[] {1.}));
  }

  @Test
  public void testNormal() {
    assertThat(SAMPLE_VARIANCE.apply(NORMAL_DATA)).isCloseTo(STD * STD, offset(EPS));
    assertThat(POPULATION_VARIANCE.apply(NORMAL_DATA)).isCloseTo(STD * STD, offset(EPS));
    assertThat(SAMPLE_STD.apply(NORMAL_DATA)).isCloseTo(STD, offset(EPS));
    assertThat(POPULATION_STD.apply(NORMAL_DATA)).isCloseTo(STD, offset(EPS));
    assertThat(SAMPLE_SKEWNESS.apply(NORMAL_DATA)).isCloseTo(0., offset(EPS));
    assertThat(SAMPLE_FISHER_KURTOSIS.apply(NORMAL_DATA)).isCloseTo(0., offset(EPS));
  }

  @Test
  public void testStudentT() {
    double variance = DOF / (DOF - 2);
    assertThat(SAMPLE_VARIANCE.apply(STUDENT_T_DATA)).isCloseTo(variance, offset(EPS));
    assertThat(POPULATION_VARIANCE.apply(STUDENT_T_DATA)).isCloseTo(variance, offset(EPS));
    assertThat(SAMPLE_STD.apply(STUDENT_T_DATA)).isCloseTo(Math.sqrt(variance), offset(EPS));
    assertThat(POPULATION_STD.apply(STUDENT_T_DATA)).isCloseTo(Math.sqrt(variance), offset(EPS));
    assertThat(SAMPLE_SKEWNESS.apply(STUDENT_T_DATA)).isCloseTo(0., offset(EPS));
    assertThat(SAMPLE_FISHER_KURTOSIS.apply(STUDENT_T_DATA)).isCloseTo(6 / (DOF - 4), offset(EPS));
  }

  @Test
  public void testChiSq() {
    double variance = 2 * DOF;
    assertThat(SAMPLE_VARIANCE.apply(CHI_SQ_DATA)).isCloseTo(variance, offset(EPS));
    assertThat(POPULATION_VARIANCE.apply(CHI_SQ_DATA)).isCloseTo(variance, offset(EPS));
    assertThat(SAMPLE_STD.apply(CHI_SQ_DATA)).isCloseTo(Math.sqrt(variance), offset(EPS));
    assertThat(POPULATION_STD.apply(CHI_SQ_DATA)).isCloseTo(Math.sqrt(variance), offset(EPS));
    assertThat(SAMPLE_SKEWNESS.apply(CHI_SQ_DATA)).isCloseTo(Math.sqrt(8 / DOF), offset(EPS));
    assertThat(SAMPLE_FISHER_KURTOSIS.apply(CHI_SQ_DATA)).isCloseTo(12 / DOF, offset(EPS));
  }

}
