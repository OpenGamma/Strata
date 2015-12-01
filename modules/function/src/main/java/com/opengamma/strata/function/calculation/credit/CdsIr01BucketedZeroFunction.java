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
 * Calculates vector IR01 of a {@code CdsTrade} for each of a set of scenarios.
 * <p>
 * This calculates the vector PV change to a series of 1 basis point shifts in zero rates at each curve node.
 */
public class CdsIr01BucketedZeroFunction
    extends AbstractCdsFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(
      ExpandedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor) {

    return pricer().ir01BucketedZero(
        product, yieldCurveInputs, creditCurveInputs, valuationDate, recoveryRate, scalingFactor);
  }

}
