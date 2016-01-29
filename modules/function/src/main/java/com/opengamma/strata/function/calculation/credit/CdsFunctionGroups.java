/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import com.opengamma.strata.calc.config.Measures;
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
          .addFunction(Measures.PRESENT_VALUE, CdsCalculationFunction.class)
          .addFunction(Measures.PAR_RATE, CdsCalculationFunction.class)
          .addFunction(Measures.IR01_PARALLEL_ZERO, CdsCalculationFunction.class)
          .addFunction(Measures.IR01_BUCKETED_ZERO, CdsCalculationFunction.class)
          .addFunction(Measures.IR01_PARALLEL_PAR, CdsCalculationFunction.class)
          .addFunction(Measures.IR01_BUCKETED_PAR, CdsCalculationFunction.class)
          .addFunction(Measures.CS01_PARALLEL_PAR, CdsCalculationFunction.class)
          .addFunction(Measures.CS01_BUCKETED_PAR, CdsCalculationFunction.class)
          .addFunction(Measures.CS01_PARALLEL_HAZARD, CdsCalculationFunction.class)
          .addFunction(Measures.CS01_BUCKETED_HAZARD, CdsCalculationFunction.class)
          .addFunction(Measures.RECOVERY01, CdsCalculationFunction.class)
          .addFunction(Measures.JUMP_TO_DEFAULT, CdsCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private CdsFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on CDS trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measures#PAR_RATE Par rate}
   *   <li>{@linkplain Measures#IR01_PARALLEL_ZERO Scalar IR01, based on zero rates}
   *   <li>{@linkplain Measures#IR01_BUCKETED_ZERO Vector curve node IR01, based on zero rates}
   *   <li>{@linkplain Measures#IR01_PARALLEL_PAR Scalar IR01, based on par interest rates}
   *   <li>{@linkplain Measures#IR01_BUCKETED_PAR Vector curve node IR01, based on par interest rates}
   *   <li>{@linkplain Measures#CS01_PARALLEL_PAR Scalar CS01, based on credit par rates}
   *   <li>{@linkplain Measures#CS01_BUCKETED_PAR Vector curve node CS01, based on credit par rates}
   *   <li>{@linkplain Measures#CS01_PARALLEL_HAZARD Scalar CS01, based on hazard rates}
   *   <li>{@linkplain Measures#CS01_BUCKETED_HAZARD Vector curve node CS01, based on hazard rates}
   *   <li>{@linkplain Measures#RECOVERY01 Recovery01}
   *   <li>{@linkplain Measures#JUMP_TO_DEFAULT Jump to Default}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<CdsTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
