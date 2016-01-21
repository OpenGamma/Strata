/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import com.opengamma.strata.calc.config.Measure;
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
          .addFunction(Measure.PAR_RATE, SwapCalculationFunction.class)
          .addFunction(Measure.PAR_SPREAD, SwapCalculationFunction.class)
          .addFunction(Measure.LEG_INITIAL_NOTIONAL, SwapCalculationFunction.class)
          .addFunction(Measure.PRESENT_VALUE, SwapCalculationFunction.class)
          .addFunction(Measure.EXPLAIN_PRESENT_VALUE, SwapCalculationFunction.class)
          .addFunction(Measure.CASH_FLOWS, SwapCalculationFunction.class)
          .addFunction(Measure.LEG_PRESENT_VALUE, SwapCalculationFunction.class)
          .addFunction(Measure.PV01, SwapCalculationFunction.class)
          .addFunction(Measure.BUCKETED_PV01, SwapCalculationFunction.class)
          .addFunction(Measure.BUCKETED_GAMMA_PV01, SwapCalculationFunction.class)
          .addFunction(Measure.ACCRUED_INTEREST, SwapCalculationFunction.class)
          .addFunction(Measure.CURRENCY_EXPOSURE, SwapCalculationFunction.class)
          .addFunction(Measure.CURRENT_CASH, SwapCalculationFunction.class)
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
   *   <li>{@linkplain Measure#PAR_RATE Par rate}
   *   <li>{@linkplain Measure#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measure#EXPLAIN_PRESENT_VALUE Explain present value}
   *   <li>{@linkplain Measure#CASH_FLOWS Cash flows}
   *   <li>{@linkplain Measure#PV01 PV01}
   *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
   *   <li>{@linkplain Measure#BUCKETED_GAMMA_PV01 Gamma PV01}
   *   <li>{@linkplain Measure#ACCRUED_INTEREST Accrued interest}
   *   <li>{@linkplain Measure#LEG_INITIAL_NOTIONAL Leg initial notional}
   *   <li>{@linkplain Measure#LEG_PRESENT_VALUE Leg present value}
   *   <li>{@linkplain Measure#CURRENCY_EXPOSURE Currency exposure}
   *   <li>{@linkplain Measure#CURRENT_CASH Current cash}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<SwapTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
