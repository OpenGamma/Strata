/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.payment;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the bucketed PV01, the present value curve parameter sensitivity of a {@code BulletPaymentTrade}.
 * This operates by algorithmic differentiation (AD).
 */
public class BulletPaymentBucketedPv01Function
    extends AbstractBulletPaymentFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(Payment product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider).build();
    return provider.curveParameterSensitivity(pointSensitivity);
  }

}
