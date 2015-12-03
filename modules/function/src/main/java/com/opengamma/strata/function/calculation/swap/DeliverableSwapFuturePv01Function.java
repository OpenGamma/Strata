/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toMultiCurrencyArray;

import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.product.swap.DeliverableSwapFutureTrade;

/**
 * Calculates PV01, the present value sensitivity of a {@code DeliverableSwapFutureTrade}.
 * This operates by algorithmic differentiation (AD).
 */
public class DeliverableSwapFuturePv01Function
    extends AbstractDeliverableSwapFutureFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(DeliverableSwapFutureTrade trade, SingleCalculationMarketData marketData) {
    MarketDataRatesProvider provider = new MarketDataRatesProvider(marketData);
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(trade, provider);
    return provider.curveParameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  @Override
  public MultiCurrencyValuesArray execute(DeliverableSwapFutureTrade trade, CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(md -> execute(trade, md))
        .collect(toMultiCurrencyArray());
  }
}
