/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

/**
 * A market data rule decides what market data should be used in calculations for a calculation target.
 * <p>
 * A rule returns a set of {@link MarketDataMappings} for a calculation target that matches the rule, otherwise
 * it returns an empty {@code Optional}.
 */
public interface MarketDataRule {

  /**
   * Returns a set of market data mappings for the target if it matches this rule, otherwise an empty {@code Optional}.
   *
   * @param target  a calculation target
   * @return a set of market data mappings for the target if it matches this rule, otherwise an empty {@code Optional}
   */
  public abstract Optional<MarketDataMappings> mappings(CalculationTarget target);
}
