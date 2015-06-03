/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.format;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Formatter for double amounts.
 */
public class DoubleValueFormatter implements ValueFormatter<Double> {

  private static final DecimalFormat FULL_AMOUNT_FORMAT = new DecimalFormat("#.##########");
  
  private final Map<Integer, DecimalFormat> displayFormatCache = new HashMap<Integer, DecimalFormat>();
  
  @Override
  public String formatForCsv(Double amount) {
    return formatForCsv(amount.doubleValue());
  }

  @Override
  public String formatForDisplay(Double object) {
    return formatForDisplay(object, 2);
  }
  
  //-------------------------------------------------------------------------
  public String formatForCsv(double amount) {
    return FULL_AMOUNT_FORMAT.format(amount);
  }
  
  public String formatForDisplay(double amount, int decimalPlaces) {
    DecimalFormat format = getDecimalPlacesFormat(decimalPlaces);
    return format.format(amount);
  }
  
  //-------------------------------------------------------------------------
  private DecimalFormat getDecimalPlacesFormat(int decimalPlaces) {
    if (!displayFormatCache.containsKey(decimalPlaces)) {
      DecimalFormat format = new DecimalFormat("#,##0;(#,##0)");
      format.setMinimumFractionDigits(decimalPlaces);
      format.setMaximumFractionDigits(decimalPlaces);
      displayFormatCache.put(decimalPlaces, format);
      return format;
    }
    return displayFormatCache.get(decimalPlaces);
  }

}
