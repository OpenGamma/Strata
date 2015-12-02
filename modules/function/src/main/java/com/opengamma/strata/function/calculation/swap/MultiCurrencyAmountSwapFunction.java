/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toMultiCurrencyArray;

import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Abstract supertype for functions which calculate measures for {@link SwapTrade} and
 * return {@link MultiCurrencyAmount}.
 */
public abstract class MultiCurrencyAmountSwapFunction extends AbstractSwapFunction<MultiCurrencyAmount> {

  @Override
  public MultiCurrencyValuesArray execute(SwapTrade trade, CalculationMarketData marketData) {
    ExpandedSwap product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toMultiCurrencyArray());
  }
}
