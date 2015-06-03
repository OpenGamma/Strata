/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.format;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Formatter for currency amounts.
 */
public class CurrencyAmountValueFormatter implements ValueFormatter<CurrencyAmount> {

  private final DoubleValueFormatter doubleFormatter = new DoubleValueFormatter();

  @Override
  public String formatForCsv(CurrencyAmount amount) {
    return doubleFormatter.formatForCsv(amount.getAmount());
  }

  @Override
  public String formatForDisplay(CurrencyAmount amount) {
    return doubleFormatter.formatForDisplay(amount.getAmount(), amount.getCurrency().getMinorUnitDigits());
  }

}
