/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import com.opengamma.strata.calc.config.pricing.DefaultPricingRules;
import com.opengamma.strata.calc.config.pricing.PricingRule;
import com.opengamma.strata.calc.config.pricing.PricingRules;
import com.opengamma.strata.function.calculation.credit.CdsFunctionGroups;
import com.opengamma.strata.function.calculation.deposit.TermDepositFunctionGroups;
import com.opengamma.strata.function.calculation.fra.FraFunctionGroups;
import com.opengamma.strata.function.calculation.future.GenericFutureFunctionGroups;
import com.opengamma.strata.function.calculation.future.GenericFutureOptionFunctionGroups;
import com.opengamma.strata.function.calculation.fx.FxNdfFunctionGroups;
import com.opengamma.strata.function.calculation.fx.FxSingleFunctionGroups;
import com.opengamma.strata.function.calculation.fx.FxSwapFunctionGroups;
import com.opengamma.strata.function.calculation.index.IborFutureFunctionGroups;
import com.opengamma.strata.function.calculation.payment.BulletPaymentFunctionGroups;
import com.opengamma.strata.function.calculation.swap.DeliverableSwapFutureFunctionGroups;
import com.opengamma.strata.function.calculation.swap.SwapFunctionGroups;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.future.GenericFutureOptionTrade;
import com.opengamma.strata.product.future.GenericFutureTrade;
import com.opengamma.strata.product.fx.FxNdfTrade;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.product.swap.DeliverableSwapFutureTrade;
import com.opengamma.strata.product.swap.SwapTrade;

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
      PricingRule.builder(BulletPaymentTrade.class).functionGroup(BulletPaymentFunctionGroups.discounting()).build(),
      PricingRule.builder(CdsTrade.class).functionGroup(CdsFunctionGroups.discounting()).build(),
      PricingRule.builder(DeliverableSwapFutureTrade.class)
          .functionGroup(DeliverableSwapFutureFunctionGroups.discounting()).build(),
      PricingRule.builder(FraTrade.class).functionGroup(FraFunctionGroups.discounting()).build(),
      PricingRule.builder(FxSingleTrade.class).functionGroup(FxSingleFunctionGroups.discounting()).build(),
      PricingRule.builder(FxNdfTrade.class).functionGroup(FxNdfFunctionGroups.discounting()).build(),
      PricingRule.builder(FxSwapTrade.class).functionGroup(FxSwapFunctionGroups.discounting()).build(),
      PricingRule.builder(GenericFutureTrade.class).functionGroup(GenericFutureFunctionGroups.market()).build(),
      PricingRule.builder(GenericFutureOptionTrade.class).functionGroup(GenericFutureOptionFunctionGroups.market()).build(),
      PricingRule.builder(IborFutureTrade.class).functionGroup(IborFutureFunctionGroups.discounting()).build(),
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
   *  <li>Bullet Payment - {@link BulletPaymentTrade}
   *  <li>Credit Default Swap - {@link CdsTrade}
   *  <li>Deliverable Swap Future - {@link DeliverableSwapFutureTrade}
   *  <li>Forward Rate Agreement - {@link FraTrade}
   *  <li>FX single (spot/forward) - {@link FxSingleTrade}
   *  <li>FX NDF - {@link FxNdfTrade}
   *  <li>FX swap - {@link FxSwapTrade}
   *  <li>Generic Future - {@link GenericFutureTrade}
   *  <li>Generic Future Option - {@link GenericFutureOptionTrade}
   *  <li>Ibor Future (STIR) - {@link IborFutureTrade}
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
