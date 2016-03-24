/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.security;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.SecurityPosition;

/**
 * Contains function groups for built-in simple security calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class SecurityPositionFunctionGroups {

  /**
   * The group with pricers based on market methods.
   */
  private static final FunctionGroup<SecurityPosition> MARKET_GROUP =
      DefaultFunctionGroup.builder(SecurityPosition.class).name("SecurityPositionMarket")
          .addFunction(Measures.PRESENT_VALUE, SecurityPositionCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private SecurityPositionFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on simple security
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
  public static FunctionGroup<SecurityPosition> market() {
    return MARKET_GROUP;
  }

}
