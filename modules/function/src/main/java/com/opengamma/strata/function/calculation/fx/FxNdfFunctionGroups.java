/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.engine.config.pricing.FunctionGroup;
import com.opengamma.strata.finance.fx.FxNdfTrade;

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
          .addFunction(Measure.PRESENT_VALUE, FxNdfPvFunction.class)
          .addFunction(Measure.PV01, FxNdfPv01Function.class)
          .addFunction(Measure.BUCKETED_PV01, FxNdfBucketedPv01Function.class)
          .addFunction(Measure.CURRENCY_EXPOSURE, FxNdfCurrencyExposureFunction.class)
          .addFunction(Measure.FORWARD_FX_RATE, FxNdfForwardFxRateFunction.class)
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
   *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measure#PV01 PV01}
   *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
   *   <li>{@linkplain Measure#CURRENCY_EXPOSURE Currency exposure}
   *   <li>{@linkplain Measure#FORWARD_FX_RATE Forward FX rate}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<FxNdfTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
