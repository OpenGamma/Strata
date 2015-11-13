/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.deposit;

import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.deposit.ExpandedTermDeposit;

/**
 * Calculates the bucketed PV01, the present value curve parameter sensitivity of a {@code TermDepositTrade}.
 * This operates by algorithmic differentiation (AD).
 */
public class TermDepositBucketedPv01Function
    extends AbstractTermDepositFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(ExpandedTermDeposit product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity);
  }

}
