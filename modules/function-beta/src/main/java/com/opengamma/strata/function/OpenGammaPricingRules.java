/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import com.opengamma.strata.engine.config.pricing.DefaultPricingRules;
import com.opengamma.strata.engine.config.pricing.PricingRule;
import com.opengamma.strata.engine.config.pricing.PricingRules;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.rate.fra.FraFunctionGroups;
import com.opengamma.strata.function.rate.swap.SwapFunctionGroups;

/**
 * Contains standard sets of pricing rules that provide full access to the built-in asset class coverage.
 */
public final class OpenGammaPricingRules {

  /**
   * The standard pricing rules.
   */
  private static final PricingRules STANDARD = DefaultPricingRules.of(
      PricingRule.builder(Trade.class).functionGroup(TradeFunctionGroups.all()).build(),
      PricingRule.builder(FraTrade.class).functionGroup(FraFunctionGroups.discounting()).build(),
      PricingRule.builder(SwapTrade.class).functionGroup(SwapFunctionGroups.discounting()).build());

  /**
   * Restricted constructor.
   */
  private OpenGammaPricingRules() {
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the standard pricing rules providing all supported measures using the default
   * calculation method across all built-in asset classes.
   * <p>
   * These pricing rules require no further configuration, so are designed to allow
   * easy access to all built-in asset class coverage.
   * <p>
   * The supported asset classes are:
   * <ul>
   *   <li>FRA ({@link FraTrade})
   *   <li>Swap ({@link SwapTrade})
   * </ul>
   * 
   * @return the default pricing rules
   */
  public static PricingRules standard() {
    return STANDARD;
  }

}
