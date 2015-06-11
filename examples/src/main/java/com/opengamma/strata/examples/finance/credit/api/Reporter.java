/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit.api;

import com.opengamma.strata.report.ReportCalculationResults;

import java.io.OutputStream;


public interface Reporter {

  String reportAsciiAsString(
      ReportCalculationResults reportCalculationResults
  );

  void reportAsciiToScreen(
      ReportCalculationResults reportCalculationResults
  );

  void reportAsciiToLogger(
      ReportCalculationResults reportCalculationResults
  );

  void reportAscii(
      ReportCalculationResults reportCalculationResults,
      OutputStream out
  );

}
