/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The simplest possible provider of observable market data values.
 * <p>
 * This interface defines a simple way to get market data values.
 */
public interface ObservableValues {

  /**
   * Obtains observable values based on a map.
   * 
   * @param map  the map of key to value
   * @return the observable values
   */
  public static ObservableValues of(Map<? extends ObservableKey, Double> map) {
    return ImmutableObservableValues.of(map);
  }

  /**
   * Obtains observable values wrapping a single key and value.
   * <p>
   * This will be most useful for tests.
   * 
   * @param key  the single key to store
   * @param value  the single value to store
   * @return the observable values
   */
  public static ObservableValues of(ObservableKey key, double value) {
    return ImmutableObservableValues.of(ImmutableMap.of(key, value));
  }

  /**
   * Obtains observable values based on a map of IDs.
   * 
   * @param map  the map of key to value
   * @return the observable values
   */
  public static ObservableValues ofIdMap(Map<? extends ObservableId, Double> map) {
    ArgChecker.notNull(map, "map");
    return ImmutableObservableValues.of(map.entrySet().stream()
        .collect(toImmutableMap(e -> e.getKey().toObservableKey(), e -> e.getValue())));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if a value is available for the market data key.
   * 
   * @param marketDataKey  the market data key
   * @return true if data for the key is available, false if not
   */
  public abstract boolean containsValue(ObservableKey marketDataKey);

  /**
   * Gets the value for the market data key.
   * 
   * @param marketDataKey  the market data key
   * @return the market data value
   * @throws RuntimeException if the value is not available
   */
  public abstract double getValue(ObservableKey marketDataKey);

}
