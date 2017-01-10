/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Formatter for double amounts.
 */
final class DoubleValueFormatter
    implements ValueFormatter<Double> {

  /**
   * The single shared instance of this formatter.
   */
  static final DoubleValueFormatter INSTANCE = new DoubleValueFormatter();

  /**
   * The decimal format.
   */
  private static final DecimalFormat FULL_AMOUNT_FORMAT =
      new DecimalFormat("#.##########", new DecimalFormatSymbols(Locale.ENGLISH));
  /**
   * The format cache.
   */
  private final Map<Integer, DecimalFormat> displayFormatCache = new HashMap<Integer, DecimalFormat>();

  // restricted constructor
  private DoubleValueFormatter() {
  }

  //-------------------------------------------------------------------------
  @Override
  public String formatForCsv(Double amount) {
    return FULL_AMOUNT_FORMAT.format(amount.doubleValue());
  }

  @Override
  public String formatForDisplay(Double object) {
    return formatForDisplay(object, 2);
  }

  /**
   * Formats a double value for display, using the specified number of decimal places.
   *
   * @param amount  the amount
   * @param decimalPlaces  the number of decimal places to display
   * @return the formatted amount
   */
  public String formatForDisplay(double amount, int decimalPlaces) {
    DecimalFormat format = getDecimalPlacesFormat(decimalPlaces);
    return format.format(amount);
  }

  //-------------------------------------------------------------------------
  private DecimalFormat getDecimalPlacesFormat(int decimalPlaces) {
    if (!displayFormatCache.containsKey(decimalPlaces)) {
      DecimalFormat format = new DecimalFormat("#,##0;(#,##0)", new DecimalFormatSymbols(Locale.ENGLISH));
      format.setMinimumFractionDigits(decimalPlaces);
      format.setMaximumFractionDigits(decimalPlaces);
      displayFormatCache.put(decimalPlaces, format);
      return format;
    }
    return displayFormatCache.get(decimalPlaces);
  }

}
