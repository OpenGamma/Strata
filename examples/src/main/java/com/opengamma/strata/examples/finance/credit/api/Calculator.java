/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit.api;

import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.report.ReportCalculationResults;

import java.time.LocalDate;
import java.util.List;

public interface Calculator {

  double calculateScalarValue(
      LocalDate valuationDate,
      TradeSource tradeSource,
      Measure measure
  );

  double[] calculateVectorValue(
      LocalDate valuationDate,
      TradeSource tradeSource,
      Measure measure
  );

  Results calculateResults(
      LocalDate valuationDate,
      TradeSource tradeSource,
      List<Measure> measures
  );

  ReportCalculationResults calculateReportingResults(
      LocalDate valuationDate,
      TradeSource tradeSource,
      List<Measure> measures
  );

}
