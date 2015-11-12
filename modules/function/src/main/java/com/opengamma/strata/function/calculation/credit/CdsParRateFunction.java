/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import java.time.LocalDate;

import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.product.credit.ExpandedCds;

/**
 * Calculates the par rate of a {@code CdsTrade} for each of a set of scenarios.
 */
public class CdsParRateFunction
    extends AbstractCdsFunction<Double> {

  @Override
  protected Double execute(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    return pricer().parRate(product, yieldCurveParRates, creditCurveParRates, valuationDate, recoveryRate);
  }

}
