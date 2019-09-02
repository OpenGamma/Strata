/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * 
 */
public class LeastSquareWithPenaltyResultTest {

  @Test
  public void test() {

    double chi2 = 13.234324;
    double pen = 2.3445;
    int nParms = 12;
    DoubleArray parms = DoubleArray.filled(nParms, 0.5);
    DoubleMatrix cov = DoubleMatrix.filled(nParms, nParms);

    LeastSquareWithPenaltyResults res = new LeastSquareWithPenaltyResults(chi2, pen, parms, cov);
    assertThat(chi2).isEqualTo(res.getChiSq());
    assertThat(pen).isEqualTo(res.getPenalty());

    DoubleMatrix invJac = DoubleMatrix.filled(nParms, 5);
    res = new LeastSquareWithPenaltyResults(chi2, pen, parms, cov, invJac);
    assertThat(chi2).isEqualTo(res.getChiSq());
    assertThat(pen).isEqualTo(res.getPenalty());
  }

}
