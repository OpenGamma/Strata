/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;

/**
 * Market data rules specify what market data should be used when calculating measures for a target.
 * <p>
 * For example, a calculation might require a USD discounting curve, but the system can contain
 * multiple curve groups, each with a USD discounting curve. The market data rules allow the system
 * to choose the correct curve.
 */
public interface MarketDataRules {

  /**
   * Returns set a of market data rules matching any target which is an instance of any of the target types.
   *
   * @param mappings  the market data mappings used for a target matching this set of rules
   * @param targetTypes  types that targets must implement to in order to match this set of rules
   * @return a market data rule that matches any target that is an instance of any of the target types
   */
  @SafeVarargs
  public static MarketDataRules ofTargetTypes(
      MarketDataMappings mappings,
      Class<? extends CalculationTarget>... targetTypes) {

    return TypedMarketDataRules.of(mappings, targetTypes);
  }

  /**
   * Returns an empty set of rules.
   *
   * @return an empty set of rules
   */
  public static MarketDataRules empty() {
    return EmptyMarketDataRules.INSTANCE;
  }

  /**
   * Returns a set of market data rules that match any target.
   *
   * @param mappings  the mappings used for any target passed to this set of rules
   * @return a market data rule that matches any target
   */
  public static MarketDataRules anyTarget(MarketDataMappings mappings) {
    return AllTargetsMarketDataRules.of(mappings);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the market data mappings that specify which market data should be used when
   * performing calculations for the target.
   *
   * @param target  the target
   * @return the mappings that specify which market data should be used when performing calculations for the target
   */
  public abstract Optional<MarketDataMappings> mappings(CalculationTarget target);

  /**
   * Combines these rules with the specified rules.
   * <p>
   * The resulting rules will return mappings from this set of rules if available,
   * otherwise mappings will be returned from the other rules.
   *
   * @param otherRules  the other rules
   * @return the combined rules
   */
  public default MarketDataRules composedWith(MarketDataRules... otherRules) {
    MarketDataRules[] rulesArray = new MarketDataRules[1 + otherRules.length];
    rulesArray[0] = this;
    System.arraycopy(otherRules, 0, rulesArray, 1, otherRules.length);
    return CompositeMarketDataRules.of(rulesArray);
  }
}
