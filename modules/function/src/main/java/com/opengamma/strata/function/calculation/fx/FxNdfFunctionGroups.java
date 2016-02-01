/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.fx.FxNdfTrade;

/**
 * Contains function groups for built-in FX Non-Deliverable Forward (NDF) calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class FxNdfFunctionGroups {

  /**
   * The group with pricers based on discounting methods.
   */
  private static final FunctionGroup<FxNdfTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(FxNdfTrade.class).name("FxNdfDiscounting")
          .addFunction(Measures.PRESENT_VALUE, FxNdfCalculationFunction.class)
          .addFunction(Measures.PV01, FxNdfCalculationFunction.class)
          .addFunction(Measures.BUCKETED_PV01, FxNdfCalculationFunction.class)
          .addFunction(Measures.CURRENCY_EXPOSURE, FxNdfCalculationFunction.class)
          .addFunction(Measures.CURRENT_CASH, FxNdfCalculationFunction.class)
          .addFunction(Measures.FORWARD_FX_RATE, FxNdfCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private FxNdfFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on FX NDF trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
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
  public static FunctionGroup<FxNdfTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
