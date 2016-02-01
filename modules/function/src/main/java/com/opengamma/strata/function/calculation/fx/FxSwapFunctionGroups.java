/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.fx.FxSwapTrade;

/**
 * Contains function groups for built-in FX swap calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class FxSwapFunctionGroups {

  /**
   * The group with pricers based on discounting methods.
   */
  private static final FunctionGroup<FxSwapTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(FxSwapTrade.class).name("FxSwapDiscounting")
          .addFunction(Measures.PAR_SPREAD, FxSwapCalculationFunction.class)
          .addFunction(Measures.PRESENT_VALUE, FxSwapCalculationFunction.class)
          .addFunction(Measures.PRESENT_VALUE_MULTI_CCY, FxSwapCalculationFunction.class)
          .addFunction(Measures.PV01, FxSwapCalculationFunction.class)
          .addFunction(Measures.BUCKETED_PV01, FxSwapCalculationFunction.class)
          .addFunction(Measures.CURRENCY_EXPOSURE, FxSwapCalculationFunction.class)
          .addFunction(Measures.CURRENT_CASH, FxSwapCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private FxSwapFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on FX swap trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
   *   <li>{@linkplain Measures#PV01 PV01}
   *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
   *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
   *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<FxSwapTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
