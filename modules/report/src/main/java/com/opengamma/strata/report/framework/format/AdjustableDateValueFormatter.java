/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import com.opengamma.strata.basics.date.AdjustableDate;

/**
 * Formatter for adjustable dates.
 */
final class AdjustableDateValueFormatter
    implements ValueFormatter<AdjustableDate> {

  /**
   * The single shared instance of this formatter.
   */
  static final AdjustableDateValueFormatter INSTANCE = new AdjustableDateValueFormatter();

  // restricted constructor
  private AdjustableDateValueFormatter() {
  }

  //-------------------------------------------------------------------------
  @Override
  public String formatForCsv(AdjustableDate amount) {
    return amount.getUnadjusted().toString();
  }

  @Override
  public String formatForDisplay(AdjustableDate amount) {
    return amount.getUnadjusted().toString();
  }

}
