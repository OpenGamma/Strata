/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Calculates the bucketed PV01, the present value curve parameter sensitivity of a {@code IborFutureTrade}.
 * This operates by algorithmic differentiation (AD).
 */
public class IborFutureBucketedPv01Function
    extends AbstractIborFutureFunction<CurveCurrencyParameterSensitivities> {

  @Override
  protected CurveCurrencyParameterSensitivities execute(IborFutureTrade trade, SingleCalculationMarketData marketData) {
    MarketDataRatesProvider provider = new MarketDataRatesProvider(marketData);
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(trade, provider);
    return provider.curveParameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

}
