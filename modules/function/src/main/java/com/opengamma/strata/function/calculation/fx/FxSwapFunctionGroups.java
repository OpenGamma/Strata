/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.engine.config.pricing.FunctionGroup;
import com.opengamma.strata.finance.fx.FxSwapTrade;

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
          .addFunction(Measure.PAR_SPREAD, FxSwapParSpreadFunction.class)
          .addFunction(Measure.PRESENT_VALUE, FxSwapPvFunction.class)
          .addFunction(Measure.PV01, FxSwapPv01Function.class)
          .addFunction(Measure.BUCKETED_PV01, FxSwapBucketedPv01Function.class)
          .addFunction(Measure.CURRENCY_EXPOSURE, FxSwapCurrencyExposureFunction.class)
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
   *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measure#PV01 PV01}
   *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
   *   <li>{@linkplain Measure#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measure#CURRENCY_EXPOSURE Currency exposure}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<FxSwapTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
