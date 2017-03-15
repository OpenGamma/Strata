/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Test {@link FormatSettingsProvider}.
 */
@Test
public class FormatSettingsProviderTest {

  public void doubleArray() {
    FormatSettingsProvider settingsProvider = FormatSettingsProvider.INSTANCE;
    FormatSettings<Object> defaultSettings = FormatSettings.of(FormatCategory.TEXT, ValueFormatters.UNSUPPORTED);
    FormatSettings<double[]> settings = settingsProvider.settings(double[].class, defaultSettings);
    ValueFormatter<double[]> formatter = settings.getFormatter();
    double[] array = {1, 2, 3};

    assertThat(formatter.formatForDisplay(array)).isEqualTo("[1.0, 2.0, 3.0]");
    assertThat(formatter.formatForCsv(array)).isEqualTo("[1.0 2.0 3.0]");
  }

}
