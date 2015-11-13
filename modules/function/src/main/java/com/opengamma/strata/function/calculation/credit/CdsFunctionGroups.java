/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.credit.CdsTrade;

/**
 * Contains function groups for built-in CDS calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class CdsFunctionGroups {

  /**
   * The group with pricers based on discounting methods.
   */
  private static final FunctionGroup<CdsTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(CdsTrade.class).name("CdsDiscounting")
          .addFunction(Measure.PRESENT_VALUE, CdsPvFunction.class)
          .addFunction(Measure.PAR_RATE, CdsParRateFunction.class)
          .addFunction(Measure.RECOVERY01, CdsRecovery01Function.class)
          .addFunction(Measure.JUMP_TO_DEFAULT, CdsJumpToDefaultFunction.class)
          .addFunction(Measure.IR01_PARALLEL_PAR, CdsIr01ParallelParFunction.class)
          .addFunction(Measure.IR01_PARALLEL_ZERO, CdsIr01ParallelZeroFunction.class)
          .addFunction(Measure.IR01_BUCKETED_PAR, CdsIr01BucketedParFunction.class)
          .addFunction(Measure.IR01_BUCKETED_ZERO, CdsIr01BucketedZeroFunction.class)
          .addFunction(Measure.CS01_PARALLEL_PAR, CdsCs01ParallelParFunction.class)
          .addFunction(Measure.CS01_PARALLEL_HAZARD, CdsCs01ParallelHazardFunction.class)
          .addFunction(Measure.CS01_BUCKETED_PAR, CdsCs01BucketedParFunction.class)
          .addFunction(Measure.CS01_BUCKETED_HAZARD, CdsCs01BucketedHazardFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private CdsFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on FRA trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measure#IR01_PARALLEL_PAR Scalar IR01, based on par interest rates}
   *   <li>{@linkplain Measure#IR01_BUCKETED_PAR Vector curve node IR01, based on par interest rates}
   *   <li>{@linkplain Measure#CS01_PARALLEL_PAR Scalar CS01, based on credit par rates}
   *   <li>{@linkplain Measure#CS01_BUCKETED_PAR Vector curve node CS01, based on credit par rates}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<CdsTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
