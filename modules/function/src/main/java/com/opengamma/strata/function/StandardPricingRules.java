/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import com.opengamma.strata.engine.config.pricing.DefaultPricingRules;
import com.opengamma.strata.engine.config.pricing.PricingRule;
import com.opengamma.strata.engine.config.pricing.PricingRules;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.future.GenericFutureOptionTrade;
import com.opengamma.strata.finance.future.GenericFutureTrade;
import com.opengamma.strata.finance.rate.deposit.TermDepositTrade;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.calculation.credit.CdsFunctionGroups;
import com.opengamma.strata.function.calculation.future.GenericFutureFunctionGroups;
import com.opengamma.strata.function.calculation.future.GenericFutureOptionFunctionGroups;
import com.opengamma.strata.function.calculation.rate.deposit.TermDepositFunctionGroups;
import com.opengamma.strata.function.calculation.rate.fra.FraFunctionGroups;
import com.opengamma.strata.function.calculation.rate.swap.SwapFunctionGroups;

/**
 * Contains standard sets of pricing rules that provide full access to the built-in asset class coverage.
 * <p>
 * These rules can be obtained via {@link StandardComponents#pricingRules()}.
 */
final class StandardPricingRules {

  /**
   * The standard pricing rules.
   */
  private static final PricingRules STANDARD = DefaultPricingRules.of(
      PricingRule.builder(CdsTrade.class).functionGroup(CdsFunctionGroups.discounting()).build(),
      PricingRule.builder(FraTrade.class).functionGroup(FraFunctionGroups.discounting()).build(),
      PricingRule.builder(GenericFutureTrade.class).functionGroup(GenericFutureFunctionGroups.market()).build(),
      PricingRule.builder(GenericFutureOptionTrade.class).functionGroup(GenericFutureOptionFunctionGroups.market()).build(),
      PricingRule.builder(SwapTrade.class).functionGroup(SwapFunctionGroups.discounting()).build(),
      PricingRule.builder(TermDepositTrade.class).functionGroup(TermDepositFunctionGroups.discounting()).build());

  /**
   * Restricted constructor.
   */
  private StandardPricingRules() {
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
   *  <li>Credit Default Swap - {@link CdsTrade}
   *  <li>Forward Rate Agreement - {@link FraTrade}
   *  <li>Generic Future - {@link GenericFutureTrade}
   *  <li>Generic Future Option - {@link GenericFutureOptionTrade}
   *  <li>Rate Swap - {@link SwapTrade}
   *  <li>Term Deposit - {@link TermDepositTrade}
   * </ul>
   * 
   * @return the default pricing rules
   */
  static PricingRules standard() {
    return STANDARD;
  }

}
