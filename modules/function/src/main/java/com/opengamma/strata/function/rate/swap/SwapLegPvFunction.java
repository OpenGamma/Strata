/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.marketdata.key.IndexCurveKey;
import com.opengamma.strata.marketdata.key.IndexRateKey;
import com.opengamma.strata.marketdata.key.ObservableKey;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapLegPricer;

/**
 * Calculates the present value of one leg of an interest rate swap for each of a set of scenarios.
 * <p>
 * The result consists of a list of present values, one for each scenario.
 */
public class SwapLegPvFunction implements EngineSingleFunction<SwapTrade, List<CurrencyAmount>> {

  /**
   * Whether to get calculate for the pay leg or the receive leg.
   */
  private final PayReceive payReceive;

  /**
   * Creates an instance.
   * 
   * @param payReceive  whether to get the present value of the first pay or receive leg
   */
  public SwapLegPvFunction(PayReceive payReceive) {
    this.payReceive = payReceive;
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationRequirements requirements(SwapTrade trade) {
    Optional<SwapLeg> optionalLeg = trade.getProduct().getLeg(payReceive);
    if (!optionalLeg.isPresent()) {
      return CalculationRequirements.EMPTY;
    }
    SwapLeg leg = optionalLeg.get();
    Set<Index> indices = leg.allIndices();
    Set<ObservableKey> indexRateKeys =
        indices.stream()
            .map(IndexRateKey::of)
            .collect(toImmutableSet());
    Set<IndexCurveKey> forwardCurveKeys =
        indices.stream()
            .map(IndexCurveKey::of)
            .collect(toImmutableSet());
    Set<DiscountingCurveKey> discountingCurveKeys =
        ImmutableSet.of(DiscountingCurveKey.of(leg.getCurrency()));

    return CalculationRequirements.builder()
        .singleValueRequirements(Sets.union(forwardCurveKeys, discountingCurveKeys))
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(leg.getCurrency())
        .build();
  }

  @Override
  public List<CurrencyAmount> execute(SwapTrade trade, CalculationMarketData marketData) {
    Optional<SwapLeg> optionalLeg = trade.getProduct().getLeg(payReceive);

    if (!optionalLeg.isPresent()) {
      throw new IllegalArgumentException(
          Messages.format(
              "No {} leg found on {}",
              payReceive,
              trade.getProduct()));
    }
    ExpandedSwapLeg leg = optionalLeg.get().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> DiscountingSwapLegPricer.DEFAULT.presentValue(leg, provider))
        .collect(toImmutableList());
  }

}
