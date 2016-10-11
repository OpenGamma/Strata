/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * The lookup that provides access to rates in market data.
 * <p>
 * The rates market lookup provides access to discount curves and forward curves.
 * This includes Ibor index rates, Overnight index rates, Price index rates,
 * FX rates and discounting.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface RatesMarketDataLookup extends CalculationParameter {

  /**
   * Obtains an instance based on a map of discount and forward curve identifiers.
   * <p>
   * The discount and forward curves refer to the curve identifier.
   * The curves themselves are provided in {@link ScenarioMarketData}
   * using {@link CurveId} as the identifier.
   * 
   * @param discountCurveIds  the discount curve identifiers, keyed by currency
   * @param forwardCurveIds  the forward curves identifiers, keyed by index
   * @return the rates lookup containing the specified curves
   */
  public static RatesMarketDataLookup of(
      Map<Currency, CurveId> discountCurveIds,
      Map<Index, CurveId> forwardCurveIds) {

    return DefaultRatesMarketDataLookup.of(
        discountCurveIds, forwardCurveIds, ObservableSource.NONE, FxRateLookup.ofRates());
  }

  /**
   * Obtains an instance based on a map of discount and forward curve identifiers,
   * specifying the source of FX rates.
   * <p>
   * The discount and forward curves refer to the curve identifier.
   * The curves themselves are provided in {@link ScenarioMarketData}
   * using {@link CurveId} as the identifier.
   * The source of market data is rarely needed, as most applications use only one
   * underlying data source.
   * 
   * @param discountCurveIds  the discount curve identifiers, keyed by currency
   * @param forwardCurveIds  the forward curves identifiers, keyed by index
   * @param obsSource  the source of market data for quotes and other observable market data
   * @param fxLookup  the lookup used to obtain FX rates
   * @return the rates lookup containing the specified curves
   */
  public static RatesMarketDataLookup of(
      Map<Currency, CurveId> discountCurveIds,
      Map<Index, CurveId> forwardCurveIds,
      ObservableSource obsSource,
      FxRateLookup fxLookup) {

    return DefaultRatesMarketDataLookup.of(discountCurveIds, forwardCurveIds, obsSource, fxLookup);
  }

  /**
   * Obtains an instance based on a group of discount and forward curves.
   * <p>
   * The discount and forward curves refer to the curve name.
   * The curves themselves are provided in {@link ScenarioMarketData}
   * using {@link CurveId} as the identifier.
   * 
   * @param groupName  the curve group name
   * @param discountCurves  the discount curves, keyed by currency
   * @param forwardCurves  the forward curves, keyed by index
   * @return the rates lookup containing the specified curves
   */
  public static RatesMarketDataLookup of(
      CurveGroupName groupName,
      Map<Currency, CurveName> discountCurves,
      Map<? extends Index, CurveName> forwardCurves) {

    Map<Currency, CurveId> discountCurveIds = MapStream.of(discountCurves)
        .mapValues(c -> CurveId.of(groupName, c))
        .toMap();
    Map<? extends Index, CurveId> forwardCurveIds = MapStream.of(forwardCurves)
        .mapValues(c -> CurveId.of(groupName, c))
        .toMap();
    return DefaultRatesMarketDataLookup.of(discountCurveIds, forwardCurveIds, ObservableSource.NONE, FxRateLookup.ofRates());
  }

  /**
   * Obtains an instance based on a curve group.
   * <p>
   * The discount curves and forward curves from the group are extracted and used to build the lookup.
   * 
   * @param curveGroup  the curve group to base the lookup on
   * @return the rates lookup based on the specified group
   */
  public static RatesMarketDataLookup of(CurveGroup curveGroup) {
    CurveGroupName groupName = curveGroup.getName();
    Map<Currency, CurveId> discountCurves = MapStream.of(curveGroup.getDiscountCurves())
        .mapValues(c -> CurveId.of(groupName, c.getName()))
        .toMap();
    Map<Index, CurveId> forwardCurves = MapStream.of(curveGroup.getForwardCurves())
        .mapValues(c -> CurveId.of(groupName, c.getName()))
        .toMap();
    return DefaultRatesMarketDataLookup.of(discountCurves, forwardCurves, ObservableSource.NONE, FxRateLookup.ofRates());
  }

  /**
   * Obtains an instance based on a curve group definition.
   * <p>
   * The discount curves and forward curves from the group are extracted and used to build the lookup.
   *
   * @param curveGroupDefinition  the curve group to base the lookup on
   * @return the rates lookup based on the specified group
   */
  public static RatesMarketDataLookup of(CurveGroupDefinition curveGroupDefinition) {
    CurveGroupName groupName = curveGroupDefinition.getName();
    Map<Currency, CurveId> discountCurves = new HashMap<>();
    Map<Index, CurveId> forwardCurves = new HashMap<>();
    for (CurveGroupEntry entry : curveGroupDefinition.getEntries()) {
      CurveId curveId = CurveId.of(groupName, entry.getCurveName());
      entry.getDiscountCurrencies().forEach(ccy -> discountCurves.put(ccy, curveId));
      entry.getIndices().forEach(idx -> forwardCurves.put(idx, curveId));
    }
    return DefaultRatesMarketDataLookup.of(discountCurves, forwardCurves, ObservableSource.NONE, FxRateLookup.ofRates());
  }

  /**
   * Obtains an instance based on a curve group definition.
   * <p>
   * The discount curves and forward curves from the group are extracted and used to build the lookup.
   *
   * @param curveGroupDefinition  the curve group to base the lookup on
   * @param observableSource  the source of market data for quotes and other observable market data
   * @param fxLookup  the lookup used to obtain FX rates
   * @return the rates lookup based on the specified group
   */
  public static RatesMarketDataLookup of(CurveGroupDefinition curveGroupDefinition,
      ObservableSource observableSource,
      FxRateLookup fxLookup) {

    CurveGroupName groupName = curveGroupDefinition.getName();
    Map<Currency, CurveId> discountCurves = new HashMap<>();
    Map<Index, CurveId> forwardCurves = new HashMap<>();
    for (CurveGroupEntry entry : curveGroupDefinition.getEntries()) {
      CurveId curveId = CurveId.of(groupName, entry.getCurveName());
      entry.getDiscountCurrencies().forEach(ccy -> discountCurves.put(ccy, curveId));
      entry.getIndices().forEach(idx -> forwardCurves.put(idx, curveId));
    }
    return DefaultRatesMarketDataLookup.of(discountCurves, forwardCurves, observableSource, fxLookup);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type that the lookup will be queried by.
   * <p>
   * This returns {@code RatesMarketLookup.class}.
   * When querying parameters using {@link CalculationParameters#findParameter(Class)},
   * {@code RatesMarketLookup.class} must be passed in to find the instance.
   * 
   * @return the type of the parameter implementation
   */
  @Override
  default Class<? extends CalculationParameter> queryType() {
    return RatesMarketDataLookup.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the set of currencies that discount factors are provided for.
   *
   * @return the set of discount curve currencies
   */
  public abstract ImmutableSet<Currency> getDiscountCurrencies();

  /**
   * Gets the identifiers used to obtain the discount factors for the specified currency.
   * <p>
   * In most cases, the identifier will refer to a curve.
   * If the currency is not found, an exception is thrown.
   *
   * @param currency  the currency for which identifiers are required
   * @return the set of market data identifiers 
   * @throws IllegalArgumentException if the currency is not found
   */
  public abstract ImmutableSet<MarketDataId<?>> getDiscountMarketDataIds(Currency currency);

  /**
   * Gets the set of indices that forward rates are provided for.
   *
   * @return the set of forward curve indices
   */
  public abstract ImmutableSet<Index> getForwardIndices();

  /**
   * Gets the identifiers used to obtain the forward rates for the specified index.
   * <p>
   * In most cases, the identifier will refer to a curve.
   * If the index is not found, an exception is thrown.
   *
   * @param index  the index for which identifiers are required
   * @return the set of market data identifiers 
   * @throws IllegalArgumentException if the index is not found
   */
  public abstract ImmutableSet<MarketDataId<?>> getForwardMarketDataIds(Index index);

  //-------------------------------------------------------------------------
  /**
   * Creates market data requirements for the specified currencies.
   * <p>
   * This is used when discount factors are required, but forward curves are not.
   * 
   * @param currencies  the currencies, for which discount factors will be needed
   * @return the requirements
   * @throws IllegalArgumentException if unable to create requirements
   */
  public default FunctionRequirements requirements(Set<Currency> currencies) {
    return requirements(currencies, ImmutableSet.of());
  }

  /**
   * Creates market data requirements for the specified currency and indices.
   * 
   * @param currency  the currency, for which discount factors are needed
   * @param indices  the indices, for which forward curves and time-series will be needed
   * @return the requirements
   * @throws IllegalArgumentException if unable to create requirements
   */
  public default FunctionRequirements requirements(Currency currency, Index... indices) {
    return requirements(ImmutableSet.of(currency), ImmutableSet.copyOf(indices));
  }

  /**
   * Creates market data requirements for the specified currencies and indices.
   * 
   * @param currencies  the currencies, for which discount factors will be needed
   * @param indices  the indices, for which forward curves and time-series will be needed
   * @return the requirements
   * @throws IllegalArgumentException if unable to create requirements
   */
  public abstract FunctionRequirements requirements(Set<Currency> currencies, Set<? extends Index> indices);

  //-------------------------------------------------------------------------
  /**
   * Obtains a filtered view of the complete set of market data.
   * <p>
   * This method returns an instance that binds the lookup to the market data.
   * The input is {@link ScenarioMarketData}, which contains market data for all scenarios.
   * 
   * @param marketData  the complete set of market data for all scenarios
   * @return the filtered market data
   */
  public default RatesScenarioMarketData marketDataView(ScenarioMarketData marketData) {
    return DefaultRatesScenarioMarketData.of(this, marketData);
  }

  /**
   * Obtains a filtered view of the complete set of market data.
   * <p>
   * This method returns an instance that binds the lookup to the market data.
   * The input is {@link MarketData}, which contains market data for one scenario.
   * 
   * @param marketData  the complete set of market data for one scenario
   * @return the filtered market data
   */
  public default RatesMarketData marketDataView(MarketData marketData) {
    return DefaultRatesMarketData.of(this, marketData);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a rates provider based on the specified market data.
   * <p>
   * This provides a {@link RatesProvider} suitable for pricing a rates product.
   * Although this method can be used directly, it is typically invoked indirectly
   * via {@link RatesMarketData}:
   * <pre>
   *  // bind the baseData to this lookup
   *  RatesMarketData view = lookup.marketView(baseData);
   *  
   *  // pass around RatesMarketData within the function to use in pricing
   *  RatesProvider provider = view.ratesProvider();
   * </pre>
   * 
   * @param marketData  the complete set of market data for one scenario
   * @return the rates provider
   */
  public abstract RatesProvider ratesProvider(MarketData marketData);

  /**
   * Obtains an FX rate provider based on the specified market data.
   * <p>
   * This provides an {@link FxRateProvider} suitable for obtaining FX rates.
   * Although this method can be used directly, it is typically invoked indirectly
   * via {@link RatesMarketData}:
   * <pre>
   *  // bind the baseData to this lookup
   *  RatesMarketData view = lookup.marketView(baseData);
   *  
   *  // pass around RatesMarketData within the function to use in pricing
   *  RatesProvider provider = view.fxRateProvider();
   * </pre>
   * 
   * @param marketData  the complete set of market data for one scenario
   * @return the FX rate provider
   */
  public abstract FxRateProvider fxRateProvider(MarketData marketData);

}
