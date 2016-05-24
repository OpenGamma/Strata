/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.TypedString;

/**
 * A mutable builder for building an instance of {@link MarketDataConfig}.
 */
public final class MarketDataConfigBuilder {

  /** The configuration objects, keyed by their type and name. */
  private final Map<Class<?>, SingleTypeMarketDataConfig> values = new HashMap<>();

  /** The configuration objects where there is only one instance per type. */
  private final Map<Class<?>, Object> defaultValues = new HashMap<>();

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
    values.put(configType, configs.withConfig(name.getName(), value));
    return this;
  }

  /**
   * Adds an item of configuration that is the default of its type.
   * <p>
   * There can only be one default item for each type.
   * <p>
   * There is a class of configuration where there is always a one value shared between all calculations.
   * An example is the configuration which specifies which market quote to use when building FX rates for
   * a currency pair. All calculations use the same set of FX rates obtained from the same underlying
   * market data.
   *
   * @param value  the configuration value
   * @param <T>  the type used when looking up the configuration
   * @return this builder
   */
  public <T> MarketDataConfigBuilder addDefault(T value) {
    ArgChecker.notNull(value, "value");
    defaultValues.put(value.getClass(), value);
    return this;
  }

  /**
   * Returns a {@link MarketDataConfig} instance built from the data in this builder.
   *
   * @return a {@link MarketDataConfig} instance built from the data in this builder
   */
  public MarketDataConfig build() {
    return new MarketDataConfig(values, defaultValues);
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
