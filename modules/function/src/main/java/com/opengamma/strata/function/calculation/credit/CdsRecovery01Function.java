/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.product.credit.ExpandedCds;

/**
 * Calculates the recovery 01 of a {@code CdsTrade} for each of a set of scenarios.
 */
public class CdsRecovery01Function
    extends AbstractCdsFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    return pricer().recovery01(
        product, yieldCurveParRates, creditCurveParRates, valuationDate, recoveryRate, scalingFactor);
  }

}
