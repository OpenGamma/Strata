/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Formatter for currency amounts.
 */
public class CurrencyAmountValueFormatter implements ValueFormatter<CurrencyAmount> {

  private final DoubleValueFormatter doubleFormatter = new DoubleValueFormatter();

  /** The single shared instance of this class. */
  public static final CurrencyAmountValueFormatter INSTANCE = new CurrencyAmountValueFormatter();

  private CurrencyAmountValueFormatter() {
  }

  @Override
  public String formatForCsv(CurrencyAmount amount) {
    return doubleFormatter.formatForCsv(amount.getAmount());
  }

  @Override
  public String formatForDisplay(CurrencyAmount amount) {
    return doubleFormatter.formatForDisplay(amount.getAmount(), amount.getCurrency().getMinorUnitDigits());
  }

}
