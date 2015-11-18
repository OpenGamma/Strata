/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import java.time.LocalDate;

import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.IsdaCreditCurveInputs;
import com.opengamma.strata.market.curve.IsdaYieldCurveInputs;
import com.opengamma.strata.product.credit.ExpandedCds;

/**
 * Calculates vector CS01 of a {@code CdsTrade} for each of a set of scenarios.
 * <p>
 * This calculates the vector PV change to a series of 1 basis point shifts in par credit spread rates at each curve node.
 */
public class CdsCs01BucketedParFunction
    extends AbstractCdsFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(
      ExpandedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    return pricer().cs01BucketedPar(
        product, yieldCurveInputs, creditCurveInputs, valuationDate, recoveryRate, scalingFactor);
  }

}
