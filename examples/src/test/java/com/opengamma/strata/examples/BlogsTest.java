/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import static com.opengamma.strata.collect.TestHelper.caputureSystemOut;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.examples.blog.multicurve1.CalibrationPV01Example;
import com.opengamma.strata.examples.blog.multicurve1.CalibrationPVPerformanceExample;

/**
 * Test blog related examples do not throw exceptions.
 */
@Test
public class BlogsTest {

  private static final String[] NO_ARGS = new String[0];

  //-------------------------------------------------------------------------
  public void test_multicurve1_pv01() {
    String captured = caputureSystemOut(() -> CalibrationPV01Example.main(NO_ARGS));
    assertTrue(captured.contains("Calibration and export finished"));
    assertValidCaptured(captured);
  }

  public void test_multicurve1_perf() {
    String captured = caputureSystemOut(() -> CalibrationPVPerformanceExample.main(NO_ARGS));
    assertTrue(captured.contains("Performance estimate for curve calibration"));
    assertValidCaptured(captured);
  }

  private void assertValidCaptured(String captured) {
    assertFalse(captured.contains("ERROR"), captured);
    assertFalse(captured.contains("FAIL"), captured);
    assertFalse(captured.contains("Exception"), captured);
    assertFalse(captured.contains("drill down"), captured);
  }

}
