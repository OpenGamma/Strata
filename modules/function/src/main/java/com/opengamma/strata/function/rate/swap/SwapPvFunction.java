/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.marketdata.key.IndexCurveKey;
import com.opengamma.strata.marketdata.key.IndexRateKey;
import com.opengamma.strata.marketdata.key.ObservableKey;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;

/**
 * Calculates the present value of an interest rate swap for each of a set of scenarios.
 */
public class SwapPvFunction implements EngineSingleFunction<SwapTrade, List<MultiCurrencyAmount>> {

  @Override
  public CalculationRequirements requirements(SwapTrade trade) {
    Swap swap = trade.getProduct();
    Set<Index> indices = swap.allIndices();

    Set<ObservableKey> indexRateKeys =
        indices.stream()
            .map(IndexRateKey::of)
            .collect(toImmutableSet());

    Set<IndexCurveKey> indexCurveKeys =
        indices.stream()
            .map(IndexCurveKey::of)
            .collect(toImmutableSet());

    Set<DiscountingCurveKey> discountingCurveKeys =
        swap.getLegs().stream()
            .map(SwapLeg::getCurrency)
            .map(DiscountingCurveKey::of)
            .collect(toImmutableSet());

    return CalculationRequirements.builder()
        .singleValueRequirements(Sets.union(indexCurveKeys, discountingCurveKeys))
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(swap.getLegs().stream().map(SwapLeg::getCurrency).collect(toImmutableSet()))
        .build();
  }

  @Override
  public List<MultiCurrencyAmount> execute(SwapTrade trade, CalculationMarketData marketData) {
    Swap swap = trade.getProduct();
    ExpandedSwap expandedSwap = swap.expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> DiscountingSwapProductPricer.DEFAULT.presentValue(expandedSwap, provider))
        .collect(toList());
  }
}
