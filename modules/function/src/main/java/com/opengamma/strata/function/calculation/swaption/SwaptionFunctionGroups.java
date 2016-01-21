/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swaption;

import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Contains function groups for built-in swaption calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class SwaptionFunctionGroups {

  /**
   * The group with pricers based on standard methods.
   */
  private static final FunctionGroup<SwaptionTrade> STANDARD_GROUP =
      DefaultFunctionGroup.builder(SwaptionTrade.class).name("Swaption")
          .addFunction(Measure.PRESENT_VALUE, SwaptionCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private SwaptionFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on Swaption trades,
   * using the standard calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<SwaptionTrade> standard() {
    return STANDARD_GROUP;
  }

}
