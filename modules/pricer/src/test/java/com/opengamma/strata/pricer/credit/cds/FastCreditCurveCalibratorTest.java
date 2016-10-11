/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

import org.testng.annotations.Test;

/**
 * Test {@link FastCreditCurveCalibrator}.
 */
@Test
public class FastCreditCurveCalibratorTest extends IsdaCompliantCreditCurveCalibratorBase {

  // calibrators
  private static final FastCreditCurveCalibrator BUILDER_ISDA =
      new FastCreditCurveCalibrator(AccrualOnDefaultFormulae.ORIGINAL_ISDA);
  private static final FastCreditCurveCalibrator BUILDER_MARKIT =
      new FastCreditCurveCalibrator(AccrualOnDefaultFormulae.MARKIT_FIX);

  private static final double TOL = 1e-14;

  public void regression_consistency_test() {
    testCalibrationAgainstISDA(BUILDER_ISDA, TOL);
    testCalibrationAgainstISDA(BUILDER_MARKIT, TOL);
  }

  // TODO puf test, spread test  buildCurveFromPillarsTest in ParVsQuotedSpreadTest
  // test in PUFCreditCurveCalibrationTest

}
