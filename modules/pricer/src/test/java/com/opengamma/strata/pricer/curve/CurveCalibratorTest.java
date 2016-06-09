/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Tests {@link CurveCalibrator}.
 */
@Test
public class CurveCalibratorTest {

  public void test_toString() {
    assertThat(CurveCalibrator.standard().toString()).isEqualTo("CurveCalibrator[ParSpread]");
  }

}
