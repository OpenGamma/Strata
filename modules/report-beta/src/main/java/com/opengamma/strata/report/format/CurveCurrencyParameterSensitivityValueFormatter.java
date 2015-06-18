/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.format;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;

import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

/**
 * Formatter for curve currency parameter sensitivity.
 */
public class CurveCurrencyParameterSensitivityValueFormatter
    implements ValueFormatter<CurveCurrencyParameterSensitivity> {

  private final DoubleValueFormatter doubleFormatter = new DoubleValueFormatter();
  
  @Override
  public String formatForCsv(CurveCurrencyParameterSensitivity sensitivity) {
    return getSensitivityString(sensitivity, d -> doubleFormatter.formatForCsv(d));
  }

  @Override
  public String formatForDisplay(CurveCurrencyParameterSensitivity sensitivity) {
    return getSensitivityString(sensitivity, d -> doubleFormatter.formatForDisplay(d));
  }

  private String getSensitivityString(CurveCurrencyParameterSensitivity sensitivity, DoubleFunction<String> formatFn) {
    StringBuilder sb = new StringBuilder();
    Optional<List<CurveParameterMetadata>> parameterMetadata = sensitivity.getMetadata().getParameters();
    IntFunction<String> labelProvider = parameterMetadata.isPresent() ?
        i -> parameterMetadata.get().get(i).getLabel() : i -> String.valueOf(i + 1);
    for (int i = 0; i < sensitivity.getSensitivity().length; i++) {
      String formattedSensitivity = formatFn.apply(sensitivity.getSensitivity()[i]);
      sb.append(labelProvider.apply(i)).append(" = ").append(formattedSensitivity);
      if (i < sensitivity.getSensitivity().length - 1) {
        sb.append(" | ");
      }
    }
    return sb.toString();
  }

}
