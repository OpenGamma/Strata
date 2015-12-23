/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swaption;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.SwaptionVolatilitiesKey;
import com.opengamma.strata.market.value.SwaptionVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionCashParYieldProductPricer;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionPhysicalProductPricer;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swaption.ExpandedSwaption;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Perform calculations on a single {@code SwaptionTrade} for each of a set of scenarios.
 * <p>
 * The default reporting currency is determined from the fixed leg of the underlying swap.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractSwaptionFunction<T>
    extends AbstractCalculationFunction<SwaptionTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractSwaptionFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractSwaptionFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the physical swaption pricer.
   * 
   * @return the pricer
   */
  protected VolatilitySwaptionPhysicalProductPricer physicalPricer() {
    return VolatilitySwaptionPhysicalProductPricer.DEFAULT;
  }

  /**
   * Returns the cash par-yield swaption pricer.
   * 
   * @return the pricer
   */
  protected VolatilitySwaptionCashParYieldProductPricer cashParYieldPricer() {
    return VolatilitySwaptionCashParYieldProductPricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(SwaptionTrade trade) {
    Swaption swaption = trade.getProduct();
    Swap swap = swaption.getUnderlying();
    Set<Index> indices = swap.allIndices();
    if (swap.getLegs(SwapLegType.FIXED).size() != 1 || swap.getLegs(SwapLegType.IBOR).size() != 1) {
      throw new IllegalArgumentException("Swaption pricing only supports Fixed-Ibor swaps");
    }
    if (indices.size() != 1) {
      throw new IllegalArgumentException("Swaption pricing only supports swaps with a single Ibor index");
    }
    if (swap.isCrossCurrency()) {
      throw new IllegalArgumentException("Swaption pricing only supports single currency swaps");
    }
    Currency currency = swap.getLegs().get(0).getCurrency();
    IborIndex index = (IborIndex) indices.iterator().next();

    return FunctionRequirements.builder()
        .singleValueRequirements(
            SwaptionVolatilitiesKey.of(index),
            DiscountCurveKey.of(currency),
            IborIndexCurveKey.of(index))
        .timeSeriesRequirements(
            IndexRateKey.of(index))
        .outputCurrencies(swap.getLegs().stream().map(SwapLeg::getCurrency).collect(toImmutableSet()))
        .build();
  }

  @Override
  public ScenarioResult<T> execute(SwaptionTrade trade, CalculationMarketData marketData) {
    ExpandedSwaption product = trade.getProduct().expand();
    Swap swap = trade.getProduct().getUnderlying();
    Set<Index> indices = swap.allIndices();
    IborIndex index = (IborIndex) indices.iterator().next();
    SwaptionVolatilitiesKey volKey = SwaptionVolatilitiesKey.of(index);
    List<T> result = new ArrayList<>();
    for (int i = 0; i < marketData.getScenarioCount(); i++) {
      SingleCalculationMarketData scmd = new SingleCalculationMarketData(marketData, i);
      MarketDataRatesProvider md = new MarketDataRatesProvider(scmd);
      SwaptionVolatilities volatilities = scmd.getValue(volKey);
      result.add(execute(product, md, volatilities));
    }
    return result.stream().collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(SwaptionTrade target) {
    Swap swap = target.getProduct().getUnderlying();
    return swap.getLegs(SwapLegType.FIXED).stream().findFirst().map(leg -> leg.getCurrency());
  }

  // execute for a single trade
  protected abstract T execute(ExpandedSwaption product, RatesProvider provider, SwaptionVolatilities volatilities);

}
