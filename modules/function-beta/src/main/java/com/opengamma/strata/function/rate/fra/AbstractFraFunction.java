/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.fra;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.finance.rate.fra.Fra;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.marketdata.key.IndexCurveKey;
import com.opengamma.strata.marketdata.key.IndexRateKey;
import com.opengamma.strata.marketdata.key.ObservableKey;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraProductPricer;

/**
 * Calculates a result of a {@code FraTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractFraFunction<T>
    implements EngineSingleFunction<FraTrade, List<T>> {

  /**
   * Returns the Fra pricer.
   * 
   * @return the pricer
   */
  protected DiscountingFraProductPricer pricer() {
    return DiscountingFraProductPricer.DEFAULT;
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationRequirements requirements(FraTrade trade) {
    Fra fra = trade.getProduct();

    Set<Index> indices = new HashSet<>();
    indices.add(fra.getIndex());
    fra.getIndexInterpolated().ifPresent(indices::add);

    Set<ObservableKey> indexRateKeys =
        indices.stream()
            .map(IndexRateKey::of)
            .collect(toImmutableSet());

    Set<IndexCurveKey> indexCurveKeys =
        indices.stream()
            .map(IndexCurveKey::of)
            .collect(toImmutableSet());

    Set<DiscountingCurveKey> discountingCurveKeys = ImmutableSet.of(DiscountingCurveKey.of(fra.getCurrency()));

    return CalculationRequirements.builder()
        .singleValueRequirements(Sets.union(indexCurveKeys, discountingCurveKeys))
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(fra.getCurrency())
        .build();
  }

  @Override
  public List<T> execute(FraTrade trade, CalculationMarketData marketData) {
    ExpandedFra product = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toList());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedFra product, RatesProvider provider);

}
