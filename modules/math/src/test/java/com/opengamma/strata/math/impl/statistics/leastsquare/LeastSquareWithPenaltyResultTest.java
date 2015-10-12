/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.leastsquare;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * 
 */
@Test
public class LeastSquareWithPenaltyResultTest {

  @Test
  public void test() {

    double chi2 = 13.234324;
    double pen = 2.3445;
    int nParms = 12;
    DoubleMatrix1D parms = DoubleMatrix1D.filled(nParms, 0.5);
    DoubleMatrix2D cov = DoubleMatrix2D.filled(nParms, nParms);

    LeastSquareWithPenaltyResults res = new LeastSquareWithPenaltyResults(chi2, pen, parms, cov);
    assertEquals(chi2, res.getChiSq());
    assertEquals(pen, res.getPenalty());

    DoubleMatrix2D invJac = DoubleMatrix2D.filled(nParms, 5);
    res = new LeastSquareWithPenaltyResults(chi2, pen, parms, cov, invJac);
    assertEquals(chi2, res.getChiSq());
    assertEquals(pen, res.getPenalty());
  }

}
