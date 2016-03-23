/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.security;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.GenericSecurityTrade;

/**
 * Contains function groups for built-in generic security calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class GenericSecurityTradeFunctionGroups {

  /**
   * The group with pricers based on market methods.
   */
  private static final FunctionGroup<GenericSecurityTrade> MARKET_GROUP =
      DefaultFunctionGroup.builder(GenericSecurityTrade.class).name("GenericSecurityTradeMarket")
          .addFunction(Measures.PRESENT_VALUE, GenericSecurityTradeCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private GenericSecurityTradeFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on generic security
   * trades based solely on querying the market for the present value.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<GenericSecurityTrade> market() {
    return MARKET_GROUP;
  }

}
