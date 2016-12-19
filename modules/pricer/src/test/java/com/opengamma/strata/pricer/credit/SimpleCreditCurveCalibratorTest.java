/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;

import org.testng.annotations.Test;

/**
 * Test {@link SimpleCreditCurveCalibrator}.
 */
@Test
public class SimpleCreditCurveCalibratorTest extends IsdaCompliantCreditCurveCalibratorBase {

  // calibrators
  private static final SimpleCreditCurveCalibrator BUILDER_ISDA =
      new SimpleCreditCurveCalibrator(AccrualOnDefaultFormula.ORIGINAL_ISDA);
  private static final SimpleCreditCurveCalibrator BUILDER_MARKIT =
      new SimpleCreditCurveCalibrator(AccrualOnDefaultFormula.MARKIT_FIX);

  private static final double TOL = 1e-14;

  public void regression_consistency_test() {
    testCalibrationAgainstISDA(BUILDER_ISDA, ACT_365F, EUR, TOL);
    testCalibrationAgainstISDA(BUILDER_MARKIT, ACT_365F, EUR, TOL);
  }

}
