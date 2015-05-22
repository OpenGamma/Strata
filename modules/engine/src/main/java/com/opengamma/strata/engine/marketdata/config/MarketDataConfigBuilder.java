/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.config;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * A mutable builder for building an instance of {@link MarketDataConfig}.
 */
public final class MarketDataConfigBuilder {

  /** The configuration objects, keyed by their type and name. */
  private final Map<Pair<Class<?>, String>, Object> values = new HashMap<>();

  /**
   * Package-private constructor used by {@link MarketDataConfig#builder()}.
   */
  MarketDataConfigBuilder() {
  }

  /**
   * Adds an item of configuration under the specified name.
   *
   * @param name  the name of the configuration item
   * @param value  the configuration item
   * @return this builder
   */
  public MarketDataConfigBuilder add(String name, Object value) {
    ArgChecker.notEmpty(name, "name");
    ArgChecker.notNull(value, "value");

    values.put(Pair.of(value.getClass(), name), value);
    return this;
  }

  /**
   * Returns a {@link MarketDataConfig} instance built from the data in this builder.
   *
   * @return a {@link MarketDataConfig} instance built from the data in this builder
   */
  public MarketDataConfig build() {
    return new MarketDataConfig(values);
  }
}
