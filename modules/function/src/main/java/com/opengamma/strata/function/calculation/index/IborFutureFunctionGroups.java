/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Contains function groups for built-in Ibor Future calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class IborFutureFunctionGroups {

  /**
   * The group with pricers based on discounting methods.
   */
  private static final FunctionGroup<IborFutureTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(IborFutureTrade.class).name("IborFutureDiscounting")
          .addFunction(Measure.PAR_SPREAD, IborFutureParSpreadFunction.class)
          .addFunction(Measure.PRESENT_VALUE, IborFuturePvFunction.class)
          .addFunction(Measure.PV01, IborFuturePv01Function.class)
          .addFunction(Measure.BUCKETED_PV01, IborFutureBucketedPv01Function.class)
          .build();

  /**
   * Restricted constructor.
   */
  private IborFutureFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on the trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measure#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measure#PV01 PV01}
   *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<IborFutureTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
