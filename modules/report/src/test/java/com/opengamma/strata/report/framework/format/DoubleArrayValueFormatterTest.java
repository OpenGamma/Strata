/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Test {@link DoubleArrayValueFormatter}.
 */
@Test
public class DoubleArrayValueFormatterTest {

  public void formatForDisplay() {
    double[] array = {1, 2, 3};
    assertThat(DoubleArrayValueFormatter.INSTANCE.formatForDisplay(array)).isEqualTo("[1.0, 2.0, 3.0]");
  }

  public void formatForCsv() {
    double[] array = {1, 2, 3};
    assertThat(DoubleArrayValueFormatter.INSTANCE.formatForCsv(array)).isEqualTo("[1.0 2.0 3.0]");
  }
}
