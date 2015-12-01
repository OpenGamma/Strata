/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;

import com.google.common.base.Strings;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.CurveParameterMetadata;

/**
 * Formatter for curve currency parameter sensitivity.
 */
class CurveCurrencyParameterSensitivityValueFormatter
    implements ValueFormatter<CurveCurrencyParameterSensitivity> {

  /**
   * The single shared instance of this formatter.
   */
  static final CurveCurrencyParameterSensitivityValueFormatter INSTANCE =
      new CurveCurrencyParameterSensitivityValueFormatter();

  private static final int PADDED_FIELD_WIDTH = 15;

  private final DoubleValueFormatter doubleFormatter = DoubleValueFormatter.INSTANCE;

  // restricted constructor
  private CurveCurrencyParameterSensitivityValueFormatter() {
  }

  //-------------------------------------------------------------------------
  @Override
  public String formatForCsv(CurveCurrencyParameterSensitivity sensitivity) {
    return getSensitivityString(sensitivity, doubleFormatter::formatForCsv, false);
  }

  @Override
  public String formatForDisplay(CurveCurrencyParameterSensitivity sensitivity) {
    return getSensitivityString(sensitivity, doubleFormatter::formatForDisplay, true);
  }

  private String getSensitivityString(
      CurveCurrencyParameterSensitivity sensitivity,
      DoubleFunction<String> formatFn,
      boolean pad) {

    StringBuilder sb = new StringBuilder();
    Optional<List<CurveParameterMetadata>> parameterMetadata = sensitivity.getMetadata().getParameterMetadata();
    IntFunction<String> labelProvider = parameterMetadata.isPresent() ?
        i -> parameterMetadata.get().get(i).getLabel() :
        i -> String.valueOf(i + 1);

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
