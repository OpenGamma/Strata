/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import static com.opengamma.strata.collect.TestHelper.caputureSystemOut;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.examples.blog.multicurve1.CalibrationPV01Example;
import com.opengamma.strata.examples.blog.multicurve1.CalibrationPVPerformanceExample;

/**
 * Test blog related examples do not throw exceptions.
 */
public class BlogsTest {

  private static final String[] NO_ARGS = new String[0];

  //-------------------------------------------------------------------------
  @Test
  public void test_multicurve1_pv01() {
    String captured = caputureSystemOut(() -> CalibrationPV01Example.main(NO_ARGS));
    assertThat(captured.contains("Calibration and export finished")).isTrue();
    assertValidCaptured(captured);
  }

  @Test
  public void test_multicurve1_perf() {
    String captured = caputureSystemOut(() -> CalibrationPVPerformanceExample.main(NO_ARGS));
    assertThat(captured.contains("Performance estimate for curve calibration")).isTrue();
    assertValidCaptured(captured);
  }

  private void assertValidCaptured(String captured) {
    assertThat(captured.contains("ERROR")).as(captured).isFalse();
    assertThat(captured.contains("FAIL")).as(captured).isFalse();
    assertThat(captured.contains("Exception")).as(captured).isFalse();
    assertThat(captured.contains("drill down")).as(captured).isFalse();
  }

}
