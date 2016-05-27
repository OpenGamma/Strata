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
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;

/**
 * Test {@link CurrencyParameterSensitivityValueFormatter}.
 */
@Test
public class CurrencyParameterSensitivityValueFormatterTest {

  public void formatForCsv() {
    CurrencyParameterSensitivity sensitivity =
        CurrencyParameterSensitivity.of(CurveName.of("foo"), Currency.AED, DoubleArray.of(10, 20, 30));
    String str = CurrencyParameterSensitivityValueFormatter.INSTANCE.formatForCsv(sensitivity);
    assertThat(str).isEqualTo("1 = 10 | 2 = 20 | 3 = 30");
  }

  public void formatForDisplay() {
    CurrencyParameterSensitivity sensitivity =
        CurrencyParameterSensitivity.of(CurveName.of("foo"), Currency.AED, DoubleArray.of(1, 2, 3));
    String str = CurrencyParameterSensitivityValueFormatter.INSTANCE.formatForDisplay(sensitivity);
    assertThat(str).isEqualTo("1 = 1.00        | 2 = 2.00        | 3 = 3.00       ");
  }

}
