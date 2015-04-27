/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.deposit;

import com.opengamma.strata.finance.rate.deposit.ExpandedTermDeposit;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Calculates the par spread parameter sensitivity of a {@code TermDepositTrade} for each of a set of scenarios.
 */
public class TermDepositParSpreadParameterSensitivityFunction
    extends AbstractTermDepositFunction<CurveParameterSensitivity> {

  @Override
  protected CurveParameterSensitivity execute(ExpandedTermDeposit product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().parSpreadSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity);
  }

}
