/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.swap.DeliverableSwapFutureTrade;

/**
 * Contains function groups for built-in Deliverable Swap Future calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class DeliverableSwapFutureFunctionGroups {

  /**
   * The group with pricers based on discounting methods.
   */
  private static final FunctionGroup<DeliverableSwapFutureTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(DeliverableSwapFutureTrade.class).name("DeliverableSwapFutureDiscounting")
          .addFunction(Measures.PRESENT_VALUE, DeliverableSwapFutureCalculationFunction.class)
          .addFunction(Measures.PV01, DeliverableSwapFutureCalculationFunction.class)
          .addFunction(Measures.BUCKETED_PV01, DeliverableSwapFutureCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private DeliverableSwapFutureFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on the trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measures#PV01 PV01}
   *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<DeliverableSwapFutureTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
