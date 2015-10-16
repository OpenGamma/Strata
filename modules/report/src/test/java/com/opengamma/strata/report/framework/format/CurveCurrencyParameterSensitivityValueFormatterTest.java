/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

/**
 * Test {@link CurveCurrencyParameterSensitivityValueFormatter}.
 */
@Test
public class CurveCurrencyParameterSensitivityValueFormatterTest {

  public void formatForCsv() {
    CurveMetadata curveMetadata = DefaultCurveMetadata.of("foo");
    CurveCurrencyParameterSensitivity sensitivity =
        CurveCurrencyParameterSensitivity.of(curveMetadata, Currency.AED, DoubleArray.of(10, 20, 30));
    String str = CurveCurrencyParameterSensitivityValueFormatter.INSTANCE.formatForCsv(sensitivity);
    assertThat(str).isEqualTo("1 = 10 | 2 = 20 | 3 = 30");
  }

  public void formatForDisplay() {
    CurveMetadata curveMetadata = DefaultCurveMetadata.of("foo");
    CurveCurrencyParameterSensitivity sensitivity =
        CurveCurrencyParameterSensitivity.of(curveMetadata, Currency.AED, DoubleArray.of(1, 2, 3));
    String str = CurveCurrencyParameterSensitivityValueFormatter.INSTANCE.formatForDisplay(sensitivity);
    assertThat(str).isEqualTo("1 = 1.00        | 2 = 2.00        | 3 = 3.00       ");
  }

}
