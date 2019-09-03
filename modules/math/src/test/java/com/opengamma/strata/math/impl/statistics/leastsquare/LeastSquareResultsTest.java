/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
public class LeastSquareResultsTest {
  private static final DoubleArray PARAMS = DoubleArray.of(1.0, 2.0);
  private static final DoubleMatrix COVAR = DoubleMatrix.copyOf(new double[][] {{0.1, 0.2}, {0.2, 0.3}});
  private static final DoubleMatrix INV_JAC = DoubleMatrix.copyOf(new double[][] {{0.5, 0.6}, {0.7, 0.8}});

  @Test
  public void testNegativeChiSq1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(-1, PARAMS, COVAR));
  }

  @Test
  public void testNullParams1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(1, null, COVAR));
  }

  @Test
  public void testNullCovar1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(1, PARAMS, null));
  }

  @Test
  public void testNullWrongSize1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(1, DoubleArray.of(1.2), COVAR));
  }

  @Test
  public void testNotSquare1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(1, PARAMS, DoubleMatrix.copyOf(new double[][] {{0.2, 0.3}})));
  }

  @Test
  public void testNegativeChiSq2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(-1, PARAMS, COVAR, INV_JAC));
  }

  @Test
  public void testNullParams2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(1, null, COVAR, INV_JAC));
  }

  @Test
  public void testNullCovar2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(1, PARAMS, null, INV_JAC));
  }

  @Test
  public void testNullWrongSize2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(1, DoubleArray.of(1.2), COVAR, INV_JAC));
  }

  @Test
  public void testNotSquare2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LeastSquareResults(1, PARAMS, DoubleMatrix.copyOf(new double[][] {{0.2, 0.3}}), INV_JAC));
  }

  @Test
  public void testRecall() {
    final double chiSq = 12.46;
    LeastSquareResults res = new LeastSquareResults(chiSq, PARAMS, COVAR);
    assertThat(chiSq).isEqualTo(res.getChiSq());
    for (int i = 0; i < 2; i++) {
      assertThat(PARAMS.get(i)).isEqualTo(res.getFitParameters().get(i));
      for (int j = 0; j < 2; j++) {
        assertThat(COVAR.get(i, j)).isEqualTo(res.getCovariance().get(i, j));
      }
    }
    res = new LeastSquareResults(chiSq, PARAMS, COVAR, INV_JAC);
    assertThat(chiSq).isEqualTo(res.getChiSq());
    for (int i = 0; i < 2; i++) {
      assertThat(PARAMS.get(i)).isEqualTo(res.getFitParameters().get(i));
      for (int j = 0; j < 2; j++) {
        assertThat(COVAR.get(i, j)).isEqualTo(res.getCovariance().get(i, j));
        assertThat(INV_JAC.get(i, j)).isEqualTo(res.getFittingParameterSensitivityToData().get(i, j));
      }
    }
  }

  @Test
  public void testHashCode() {
    LeastSquareResults ls1 = new LeastSquareResults(1.0, PARAMS, COVAR);
    LeastSquareResults ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.0, 2.0),
        DoubleMatrix.copyOf(new double[][] {{0.1, 0.2}, {0.2, 0.3}}));
    assertThat(ls1.hashCode()).isEqualTo(ls2.hashCode());
    ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.0, 2.0),
        DoubleMatrix.copyOf(new double[][] {{0.1, 0.2}, {0.2, 0.3}}), null);
    assertThat(ls1.hashCode()).isEqualTo(ls2.hashCode());
    ls1 = new LeastSquareResults(1.0, PARAMS, COVAR, INV_JAC);
    ls2 = new LeastSquareResults(1.0,
        DoubleArray.of(1.0, 2.0),
        DoubleMatrix.copyOf(new double[][] {{0.1, 0.2}, {0.2, 0.3}}),
        DoubleMatrix.copyOf(new double[][] {{0.5, 0.6}, {0.7, 0.8}}));
    assertThat(ls1.hashCode()).isEqualTo(ls2.hashCode());
  }

  @Test
  public void testEquals() {
    LeastSquareResults ls1 = new LeastSquareResults(1.0, PARAMS, COVAR);
    LeastSquareResults ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.0, 2.0),
        DoubleMatrix.copyOf(new double[][] {{0.1, 0.2}, {0.2, 0.3}}));
    assertThat(ls1).isEqualTo(ls2);
    ls2 = new LeastSquareResults(1.0, PARAMS, COVAR, null);
    assertThat(ls1).isEqualTo(ls2);
    ls2 = new LeastSquareResults(1.1, PARAMS, COVAR);
    assertThat(ls1.equals(ls2)).isFalse();
    ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.1, 2.0), DoubleMatrix.copyOf(new double[][] {
        {0.1, 0.2}, {0.2, 0.3}}));
    assertThat(ls1.equals(ls2)).isFalse();
    ls2 = new LeastSquareResults(1.0, DoubleArray.of(1.0, 2.0), DoubleMatrix.copyOf(new double[][] {
        {0.1, 0.2}, {0.2, 0.4}}));
    assertThat(ls1.equals(ls2)).isFalse();
    ls2 = new LeastSquareResults(1.0, PARAMS, COVAR, INV_JAC);
    assertThat(ls1.equals(ls2)).isFalse();
    ls1 = new LeastSquareResults(1, PARAMS, COVAR, INV_JAC);
    ls2 = new LeastSquareResults(1, PARAMS, COVAR, COVAR);
    assertThat(ls1.equals(ls2)).isFalse();
  }

}
