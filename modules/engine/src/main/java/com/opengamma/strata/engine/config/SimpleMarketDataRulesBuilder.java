/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

/**
 * Builder for {@link SimpleMarketDataRules} that allows mappings to be added one target type at a time.
 * <p>
 * Instances are created by calling {@link SimpleMarketDataRules#builder()}.
 */
public final class SimpleMarketDataRulesBuilder {

  /** Mappings for targets, keyed by the type of targets to which they apply. */
  private final Map<Class<? extends CalculationTarget>, MarketDataMappings> mappings = new HashMap<>();

  SimpleMarketDataRulesBuilder() {
  }

  /**
   * Adds a set of market data mappings for a target type.
   *
   * @param targetType  the target type
   * @param mappings  a set of market data mappings that should be used for the target
   * @return this builder
   */
  public SimpleMarketDataRulesBuilder addMappings(Class<? extends CalculationTarget> targetType, MarketDataMappings mappings) {
    ArgChecker.notNull(targetType, "targetType");
    ArgChecker.notNull(mappings, "mappings");
    this.mappings.put(targetType, mappings);
    return this;
  }

  /**
   * Returns a set of market data rules built from the data in this builder.
   *
   * @return a set of market data rules built from the data in this builder
   */
  public SimpleMarketDataRules build() {
    return new SimpleMarketDataRules(mappings);
  }
}
