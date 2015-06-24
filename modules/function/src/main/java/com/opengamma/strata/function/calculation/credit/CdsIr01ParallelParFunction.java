/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;

/**
 * Calculates scalar IR01 of a {@code CdsTrade} for each of a set of scenarios.
 * This calculates the scalar PV change to a 1 basis point shift in par interest rates.
 */
public class CdsIr01ParallelParFunction
    extends AbstractCdsFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate) {

    return pricer().ir01ParallelPar(product, yieldCurveParRates, creditCurveParRates, valuationDate);
  }

}
