/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.util.Map;
import java.util.Map.Entry;

import com.opengamma.strata.collect.ArgChecker;

/**
 * The simplest possible provider of observable market data values.
 * <p>
 * This interface defines a simple way to get market data values.
 */
public interface ObservableValues {

  /**
   * Obtains observable values based on a map.
   * <p>
   * The map is not copied, and as such it must either be immutable or thread-safe.
   * 
   * @param map  the map of key to value
   * @return the observable values
   */
  public static ObservableValues of(Map<ObservableKey, Double> map) {
    ArgChecker.notNull(map, "map");
    return new ObservableValues() {
      @Override
      public boolean containsValue(ObservableKey marketDataKey) {
        return map.containsKey(marketDataKey);
      }

      @Override
      public double getValue(ObservableKey marketDataKey) {
        Double value = map.get(marketDataKey);
        if (value == null) {
          throw new IllegalArgumentException("Market data not found: " + marketDataKey);
        }
        return value;
      }
    };
  }

  /**
   * Obtains observable values based on a map.
   * <p>
   * The map is not copied, and as such it must either be immutable or thread-safe.
   * 
   * @param map  the map of key to value
   * @return the observable values
   */
  public static ObservableValues ofIdMap(Map<ObservableId, Double> map) {
    ArgChecker.notNull(map, "map");
    return new ObservableValues() {
      @Override
      public boolean containsValue(ObservableKey marketDataKey) {
        for (ObservableId id : map.keySet()) {
          if (id.toObservableKey().equals(marketDataKey)) {
            return true;
          }
        }
        return false;
      }

      @Override
      public double getValue(ObservableKey marketDataKey) {
        for (Entry<ObservableId, Double> entry : map.entrySet()) {
          if (entry.getKey().toObservableKey().equals(marketDataKey)) {
            return entry.getValue();
          }
        }
        throw new IllegalArgumentException("Market data not found: " + marketDataKey);
      }
    };
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
    ArgChecker.notNull(key, "key");
    return new ObservableValues() {
      @Override
      public boolean containsValue(ObservableKey marketDataKey) {
        return key.equals(marketDataKey);
      }

      @Override
      public double getValue(ObservableKey marketDataKey) {
        if (key.equals(marketDataKey)) {
          return value;
        }
        throw new IllegalArgumentException("Market data not found: " + marketDataKey);
      }
    };
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
