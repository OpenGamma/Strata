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
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;

/**
 * Function which calculates values of a measure for an {@link FxSwapTrade} and returns a {@link MultiCurrencyAmount}.
 */
public abstract class MultiCurrencyAmountFxSwapFunction extends AbstractFxSwapFunction<MultiCurrencyAmount> {

  @Override
  public MultiCurrencyValuesArray execute(FxSwapTrade trade, CalculationMarketData marketData) {
    ExpandedFxSwap product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toMultiCurrencyArray());
  }
}
