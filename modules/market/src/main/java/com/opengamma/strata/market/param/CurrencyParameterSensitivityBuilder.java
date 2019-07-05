/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoublePredicate;
import java.util.function.UnaryOperator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.data.MarketDataName;

/**
 * Builder for {@code CurrencyParameterSensitivity}
 */
final class CurrencyParameterSensitivityBuilder {

  /**
   * The market data name.
   */
  private final MarketDataName<?> marketDataName;
  /**
   * The currency.
   */
  private final Currency currency;
  /**
   * The map of sensitivity data.
   */
  private final Map<ParameterMetadata, Double> sensitivity = new LinkedHashMap<>();

  //-------------------------------------------------------------------------
  // restricted constructor
  CurrencyParameterSensitivityBuilder(MarketDataName<?> marketDataName, Currency currency) {
    this.marketDataName = ArgChecker.notNull(marketDataName, "marketDataName");
    this.currency = ArgChecker.notNull(currency, "currency");
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a single sensitivity to the builder.
   * <p>
   * If the key already exists, the sensitivity value will be merged.
   * 
   * @param metadata  the sensitivity metadata
   * @param sensitivityValue  the sensitivity value
   * @return this, for chaining
   */
  CurrencyParameterSensitivityBuilder add(ParameterMetadata metadata, double sensitivityValue) {
    if (metadata.equals(ParameterMetadata.empty())) {
      throw new IllegalArgumentException("Builder does not allow empty parameter metadata");
    }
    this.sensitivity.merge(metadata, sensitivityValue, Double::sum);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Maps the sensitivity metadata.
   * 
   * @return this, for chaining
   */
  CurrencyParameterSensitivityBuilder mapMetadata(UnaryOperator<ParameterMetadata> metadataFn) {
    ImmutableMap<ParameterMetadata, Double> base = ImmutableMap.copyOf(sensitivity);
    sensitivity.clear();
    MapStream.of(base)
        .mapKeys(metadataFn)
        .forEach((md, v) -> add(md, v));
    return this;
  }

  /**
   * Filters the sensitivity values.
   * 
   * @return this, for chaining
   */
  CurrencyParameterSensitivityBuilder filterSensitivity(DoublePredicate predicate) {
    for (Iterator<Double> it = sensitivity.values().iterator(); it.hasNext();) {
      Double value = it.next();
      if (!predicate.test(value)) {
        it.remove();
      }
    }
    return this;
  }

  /**
   * Returns whether this sensitivity builder has any values.
   *
   * @return whether the sensitivity is empty
   */
  boolean isEmpty() {
    return sensitivity.isEmpty();
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the sensitivity from the provided data.
   * 
   * @return the sensitivities instance
   */
  CurrencyParameterSensitivity build() {
    ImmutableSet<Class<?>> metadataTypes =
        sensitivity.keySet().stream().map(Object::getClass).collect(toImmutableSet());
    if (metadataTypes.size() == 1) {
      if (TenoredParameterMetadata.class.isAssignableFrom(metadataTypes.iterator().next())) {
        Map<ParameterMetadata, Double> sorted = MapStream.of(sensitivity)
            .sortedKeys(Comparator.comparing(k -> ((TenoredParameterMetadata) k).getTenor()))
            .toMap();
        return CurrencyParameterSensitivity.of(marketDataName, currency, sorted);
      }
      if (DatedParameterMetadata.class.isAssignableFrom(metadataTypes.iterator().next())) {
        Map<ParameterMetadata, Double> sorted = MapStream.of(sensitivity)
            .sortedKeys(Comparator.comparing(k -> ((DatedParameterMetadata) k).getDate()))
            .toMap();
        return CurrencyParameterSensitivity.of(marketDataName, currency, sorted);
      }
    }
    return CurrencyParameterSensitivity.of(marketDataName, currency, sensitivity);
  }

}
