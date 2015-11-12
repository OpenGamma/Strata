/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit.harness;

import java.time.LocalDate;
import java.util.List;

import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.collect.array.DoubleArray;

public interface Calculator {

  double calculateScalarValue(
      LocalDate valuationDate,
      TradeSource tradeSource,
      Measure measure);

  DoubleArray calculateVectorValue(
      LocalDate valuationDate,
      TradeSource tradeSource,
      Measure measure);

  Results calculateResults(
      LocalDate valuationDate,
      TradeSource tradeSource,
      List<Measure> measures);

}
