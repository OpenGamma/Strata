/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.ExpandedFxSingle;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates PV01, the present value sensitivity of a {@code FxSingleTrade}.
 * This operates by algorithmic differentiation (AD).
 */
public class FxSinglePv01Function
    extends AbstractFxSingleFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(ExpandedFxSingle product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity).total();
  }

}
