/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.deposit;

import com.opengamma.strata.finance.rate.deposit.ExpandedTermDeposit;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the present value parameter sensitivity of a {@code TermDepositTrade} for each of a set of scenarios.
 */
public class TermDepositPvParameterSensitivityFunction
    extends AbstractTermDepositFunction<CurveParameterSensitivities> {

  @Override
  protected CurveParameterSensitivities execute(ExpandedTermDeposit product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity);
  }

}
