/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link RatesCurveCalibrator}.
 */
public class RatesCurveCalibratorTest {

  @Test
  public void test_toString() {
    assertThat(RatesCurveCalibrator.standard().toString()).isEqualTo("CurveCalibrator[ParSpread]");
  }

}
