/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import java.util.List;
import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;

import com.google.common.base.Strings;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Formatter for currency parameter sensitivity.
 */
final class CurrencyParameterSensitivityValueFormatter
    implements ValueFormatter<CurrencyParameterSensitivity> {

  /**
   * The single shared instance of this formatter.
   */
  static final CurrencyParameterSensitivityValueFormatter INSTANCE =
      new CurrencyParameterSensitivityValueFormatter();

  private static final int PADDED_FIELD_WIDTH = 15;

  private final DoubleValueFormatter doubleFormatter = DoubleValueFormatter.INSTANCE;

  // restricted constructor
  private CurrencyParameterSensitivityValueFormatter() {
  }

  //-------------------------------------------------------------------------
  @Override
  public String formatForCsv(CurrencyParameterSensitivity sensitivity) {
    return getSensitivityString(sensitivity, doubleFormatter::formatForCsv, false);
  }

  @Override
  public String formatForDisplay(CurrencyParameterSensitivity sensitivity) {
    return getSensitivityString(sensitivity, doubleFormatter::formatForDisplay, true);
  }

  private String getSensitivityString(
      CurrencyParameterSensitivity sensitivity,
      DoubleFunction<String> formatFn,
      boolean pad) {

    StringBuilder sb = new StringBuilder();
    List<ParameterMetadata> parameterMetadata = sensitivity.getParameterMetadata();
    IntFunction<String> labelProvider =
        i -> Objects.toString(Strings.emptyToNull(parameterMetadata.get(i).getLabel()), String.valueOf(i + 1));

    for (int i = 0; i < sensitivity.getSensitivity().size(); i++) {
      String formattedSensitivity = formatFn.apply(sensitivity.getSensitivity().get(i));
      String field = labelProvider.apply(i) + " = " + formattedSensitivity;
      if (pad) {
        field = Strings.padEnd(field, PADDED_FIELD_WIDTH, ' ');
      }
      sb.append(field);
      if (i < sensitivity.getSensitivity().size() - 1) {
        sb.append(" | ");
      }
    }
    return sb.toString();
  }

}
