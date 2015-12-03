/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toMultiCurrencyArray;

import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;

/**
 * Function which calculates values of a measure for an {@link FxNdfTrade} and returns a {@link MultiCurrencyAmount}.
 */
public abstract class MultiCurrencyAmountFxNdfFunction extends AbstractFxNdfFunction<MultiCurrencyAmount> {

  @Override
  public ScenarioResult<MultiCurrencyAmount> execute(FxNdfTrade trade, CalculationMarketData marketData) {
    ExpandedFxNdf product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toMultiCurrencyArray());
  }
}
