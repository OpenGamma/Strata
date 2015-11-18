/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.curve.IsdaCreditCurveInputs;
import com.opengamma.strata.market.curve.IsdaYieldCurveInputs;
import com.opengamma.strata.product.credit.ExpandedCds;

/**
 * Calculates scalar CS01 of a {@code CdsTrade} for each of a set of scenarios.
 * <p>
 * This is the scalar PV change to a 1 basis point shift in hazard rates.
 */
public class CdsCs01ParallelHazardFunction
    extends AbstractCdsFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(
      ExpandedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    return pricer().cs01ParallelHazard(
        product, yieldCurveInputs, creditCurveInputs, valuationDate, recoveryRate, scalingFactor);
  }

}
