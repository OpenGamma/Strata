/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.config;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.type.TypedString;

/**
 * A mutable builder for building an instance of {@link MarketDataConfig}.
 */
public final class MarketDataConfigBuilder {

  /** The configuration objects, keyed by their type and name. */
  private final Map<Class<?>, SingleTypeMarketDataConfig> values = new HashMap<>();

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


    Class<?> configType = value.getClass();
    SingleTypeMarketDataConfig configs = configsForType(configType);
    values.put(configType, configs.withConfig(name, value));
    return this;
  }

  /**
   * Adds an item of configuration under the specified name.
   *
   * @param name  the name of the configuration item
   * @param value  the configuration item
   * @return this builder
   */
  public MarketDataConfigBuilder add(TypedString<?> name, Object value) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(value, "value");


    Class<?> configType = value.getClass();
    SingleTypeMarketDataConfig configs = configsForType(configType);
    values.put(configType, configs.withConfig(name.toString(), value));
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

  /**
   * Returns a set of configuration object for the specified type, creating one and adding it to
   * the map if not found.
   */
  private SingleTypeMarketDataConfig configsForType(Class<?> configType) {
    SingleTypeMarketDataConfig configs = values.get(configType);
    if (configs != null) {
      return configs;
    }
    SingleTypeMarketDataConfig newConfigs = SingleTypeMarketDataConfig.builder().configType(configType).build();
    values.put(configType, newConfigs);
    return newConfigs;
  }
}
