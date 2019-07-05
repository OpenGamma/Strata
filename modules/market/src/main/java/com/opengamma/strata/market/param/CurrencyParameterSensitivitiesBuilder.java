/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.DoublePredicate;
import java.util.function.UnaryOperator;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketDataName;

/**
 * Builder for {@code CurrencyParameterSensitivities}.
 */
public final class CurrencyParameterSensitivitiesBuilder {

  /**
   * The map of sensitivity data.
   */
  private final SortedMap<Pair<MarketDataName<?>, Currency>, CurrencyParameterSensitivityBuilder> data =
      new TreeMap<>();

  //-------------------------------------------------------------------------
  // restricted constructor
  CurrencyParameterSensitivitiesBuilder() {
  }

  // restricted constructor
  CurrencyParameterSensitivitiesBuilder(List<CurrencyParameterSensitivity> sensitivities) {
    sensitivities.forEach(this::add);
  }

  //-------------------------------------------------------------------------
  /**
   * Adds sensitivities to the builder.
   * <p>
   * Values with the same market data name and currency will be merged.
   * 
   * @param sensitivities  the sensitivities to add
   * @return this, for chaining
   */
  public CurrencyParameterSensitivitiesBuilder add(CurrencyParameterSensitivities sensitivities) {
    return add(sensitivities.getSensitivities());
  }

  /**
   * Adds sensitivities to the builder.
   * <p>
   * Values with the same market data name and currency will be merged.
   * 
   * @param sensitivities  the sensitivities to add
   * @return this, for chaining
   */
  public CurrencyParameterSensitivitiesBuilder add(List<CurrencyParameterSensitivity> sensitivities) {
    sensitivities.forEach(this::add);
    return this;
  }

  /**
   * Adds a sensitivity to the builder.
   * <p>
   * Values with the same market data name and currency will be merged.
   * 
   * @param sensToAdd  the sensitivity to add
   * @return this, for chaining
   */
  public CurrencyParameterSensitivitiesBuilder add(CurrencyParameterSensitivity sensToAdd) {
    sensToAdd.sensitivities()
        .forEach((md, value) -> add(sensToAdd.getMarketDataName(), sensToAdd.getCurrency(), md, value));
    return this;
  }

  /**
   * Adds a single sensitivity to the builder.
   * <p>
   * Values with the same market data name and currency will be merged.
   * 
   * @param marketDataName  the curve name
   * @param currency  the currency of the sensitivity
   * @param metadata  the sensitivity metadata, not empty
   * @param sensitivityValue  the sensitivity value
   * @return this, for chaining
   */
  public CurrencyParameterSensitivitiesBuilder add(
      MarketDataName<?> marketDataName,
      Currency currency,
      ParameterMetadata metadata,
      double sensitivityValue) {

    data.computeIfAbsent(Pair.of(marketDataName, currency),
        t -> new CurrencyParameterSensitivityBuilder(marketDataName, currency))
        .add(metadata, sensitivityValue);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Maps the sensitivity metadata.
   * <p>
   * If the function returns the same metadata for two different inputs, the sensitivity value will be summed.
   * For example, this could be used to normalize tenors.
   * 
   * @param metadataFn  the function to adjust the metadata
   * @return this, for chaining
   */
  public CurrencyParameterSensitivitiesBuilder mapMetadata(UnaryOperator<ParameterMetadata> metadataFn) {
    for (CurrencyParameterSensitivityBuilder builder : data.values()) {
      builder.mapMetadata(metadataFn);
    }
    return this;
  }

  /**
   * Filters the sensitivity values.
   * <p>
   * For example, this could be used to remove sensitivities near to zero.
   * 
   * @param predicate  the predicate to test the value, return true to retain the value
   * @return this, for chaining
   */
  public CurrencyParameterSensitivitiesBuilder filterSensitivity(DoublePredicate predicate) {
    for (Iterator<CurrencyParameterSensitivityBuilder> it = data.values().iterator(); it.hasNext();) {
      CurrencyParameterSensitivityBuilder builder = it.next();
      builder.filterSensitivity(predicate);
      if (builder.isEmpty()) {
        it.remove();
      }
    }
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the sensitivity from the provided data.
   * <p>
   * If all the values added are tenor-based, or all are date-based, then the resulting
   * sensitivity will have the tenors sorted.
   * 
   * @return the sensitivities instance
   */
  public CurrencyParameterSensitivities build() {
    return CurrencyParameterSensitivities.of(data.values().stream()
        .map(CurrencyParameterSensitivityBuilder::build)
        .sorted(CurrencyParameterSensitivity::compareKey)
        .collect(toImmutableList()));
  }

}
