/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.deposit;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.deposit.TermDepositTrade;

/**
 * Contains function groups for built-in Term Deposit calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class TermDepositFunctionGroups {

  /**
   * The group with pricers based on discounting methods.
   */
  private static final FunctionGroup<TermDepositTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(TermDepositTrade.class).name("TermDepositDiscounting")
          .addFunction(Measures.PAR_RATE, TermDepositCalculationFunction.class)
          .addFunction(Measures.PAR_SPREAD, TermDepositCalculationFunction.class)
          .addFunction(Measures.PRESENT_VALUE, TermDepositCalculationFunction.class)
          .addFunction(Measures.PV01, TermDepositCalculationFunction.class)
          .addFunction(Measures.BUCKETED_PV01, TermDepositCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private TermDepositFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on Term Deposit trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measures#PAR_RATE Par rate}
   *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measures#PV01 PV01}
   *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<TermDepositTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
