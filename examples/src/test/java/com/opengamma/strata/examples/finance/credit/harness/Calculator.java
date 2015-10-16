/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit.harness;

import java.time.LocalDate;
import java.util.List;

import com.opengamma.strata.engine.calculation.Results;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

public interface Calculator {

  double calculateScalarValue(
      LocalDate valuationDate,
      TradeSource tradeSource,
      Measure measure);

  DoubleMatrix1D calculateVectorValue(
      LocalDate valuationDate,
      TradeSource tradeSource,
      Measure measure);

  Results calculateResults(
      LocalDate valuationDate,
      TradeSource tradeSource,
      List<Measure> measures);

}
