/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.calc.config.Measure;
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
          .addFunction(Measure.PAR_SPREAD, FxSingleParSpreadFunction.class)
          .addFunction(Measure.PRESENT_VALUE, FxSinglePvFunction.class)
          .addFunction(Measure.PV01, FxSinglePv01Function.class)
          .addFunction(Measure.BUCKETED_PV01, FxSingleBucketedPv01Function.class)
          .addFunction(Measure.CURRENCY_EXPOSURE, FxSingleCurrencyExposureFunction.class)
          .addFunction(Measure.FORWARD_FX_RATE, FxSingleForwardFxRateFunction.class)
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
   *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measure#PV01 PV01}
   *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
   *   <li>{@linkplain Measure#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measure#CURRENCY_EXPOSURE Currency exposure}
   *   <li>{@linkplain Measure#FORWARD_FX_RATE Forward FX rate}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<FxSingleTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
