/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.fx.FxSingleTrade;

/**
 * Contains function groups for built-in FX calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class FxSingleFunctionGroups {

  /**
   * The group with pricers based on discounting methods.
   */
  private static final FunctionGroup<FxSingleTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(FxSingleTrade.class).name("FxSingleDiscounting")
          .addFunction(Measures.PAR_SPREAD, FxSingleCalculationFunction.class)
          .addFunction(Measures.PRESENT_VALUE, FxSingleCalculationFunction.class)
          .addFunction(Measures.PV01, FxSingleCalculationFunction.class)
          .addFunction(Measures.BUCKETED_PV01, FxSingleCalculationFunction.class)
          .addFunction(Measures.CURRENCY_EXPOSURE, FxSingleCalculationFunction.class)
          .addFunction(Measures.CURRENT_CASH, FxSingleCalculationFunction.class)
          .addFunction(Measures.FORWARD_FX_RATE, FxSingleCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private FxSingleFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on FX trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measures#PV01 PV01}
   *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
   *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
   *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
   *   <li>{@linkplain Measures#FORWARD_FX_RATE Forward FX rate}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<FxSingleTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
