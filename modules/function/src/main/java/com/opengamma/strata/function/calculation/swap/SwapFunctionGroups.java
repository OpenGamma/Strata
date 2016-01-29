/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Contains function groups for built-in Swap calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class SwapFunctionGroups {

  /**
   * The group with pricers based on discounting methods.
   */
  private static final FunctionGroup<SwapTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(SwapTrade.class).name("SwapDiscounting")
          .addFunction(Measures.PAR_RATE, SwapCalculationFunction.class)
          .addFunction(Measures.PAR_SPREAD, SwapCalculationFunction.class)
          .addFunction(Measures.LEG_INITIAL_NOTIONAL, SwapCalculationFunction.class)
          .addFunction(Measures.PRESENT_VALUE, SwapCalculationFunction.class)
          .addFunction(Measures.EXPLAIN_PRESENT_VALUE, SwapCalculationFunction.class)
          .addFunction(Measures.CASH_FLOWS, SwapCalculationFunction.class)
          .addFunction(Measures.LEG_PRESENT_VALUE, SwapCalculationFunction.class)
          .addFunction(Measures.PV01, SwapCalculationFunction.class)
          .addFunction(Measures.BUCKETED_PV01, SwapCalculationFunction.class)
          .addFunction(Measures.BUCKETED_GAMMA_PV01, SwapCalculationFunction.class)
          .addFunction(Measures.ACCRUED_INTEREST, SwapCalculationFunction.class)
          .addFunction(Measures.CURRENCY_EXPOSURE, SwapCalculationFunction.class)
          .addFunction(Measures.CURRENT_CASH, SwapCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private SwapFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on Swap trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measures#PAR_RATE Par rate}
   *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measures#EXPLAIN_PRESENT_VALUE Explain present value}
   *   <li>{@linkplain Measures#CASH_FLOWS Cash flows}
   *   <li>{@linkplain Measures#PV01 PV01}
   *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
   *   <li>{@linkplain Measures#BUCKETED_GAMMA_PV01 Gamma PV01}
   *   <li>{@linkplain Measures#ACCRUED_INTEREST Accrued interest}
   *   <li>{@linkplain Measures#LEG_INITIAL_NOTIONAL Leg initial notional}
   *   <li>{@linkplain Measures#LEG_PRESENT_VALUE Leg present value}
   *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
   *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<SwapTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
