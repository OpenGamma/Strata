/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;

/**
 * Returns the notional amount of a {@code SwapTrade}.
 */
public class SwapTradeNotionalFunction
    implements CalculationSingleFunction<SwapTrade, List<List<CurrencyAmount>>> {
  // TODO: what is correct result?
  // which leg as they can differ?
  // what notional - current period, initial, final or max?

  @Override
  public CalculationRequirements requirements(SwapTrade target) {
    return CalculationRequirements.empty();
  }

  @Override
  public List<List<CurrencyAmount>> execute(SwapTrade input, CalculationMarketData marketData) {
    List<CurrencyAmount> notional = getNotional(input);
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(i -> notional)
        .collect(Collectors.toList());
  }

  // Not a MultiCurrencyAmount as legs should be kept separate
  private List<CurrencyAmount> getNotional(SwapTrade input) {
    return input.getProduct().getLegs().stream()
        .filter(RateCalculationSwapLeg.class::isInstance)
        .map(RateCalculationSwapLeg.class::cast)
        .map(l -> CurrencyAmount.of(l.getNotionalSchedule().getCurrency(), l.getNotionalSchedule().getAmount().getInitialValue()))
        .distinct() // if legs have the same notional then represent these as a single item
        .collect(Collectors.toList());
  }

}
