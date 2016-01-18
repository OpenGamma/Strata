/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fra;

import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * Contains function groups for built-in FRA calculation functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class FraFunctionGroups {

  /**
   * The group with pricers based on discounting methods.
   */
  private static final FunctionGroup<FraTrade> DISCOUNTING_GROUP =
      DefaultFunctionGroup.builder(FraTrade.class).name("FraDiscounting")
          .addFunction(Measure.PAR_RATE, FraCalculationFunction.class)
          .addFunction(Measure.PAR_SPREAD, FraCalculationFunction.class)
          .addFunction(Measure.PRESENT_VALUE, FraCalculationFunction.class)
          .addFunction(Measure.EXPLAIN_PRESENT_VALUE, FraCalculationFunction.class)
          .addFunction(Measure.CASH_FLOWS, FraCalculationFunction.class)
          .addFunction(Measure.PV01, FraCalculationFunction.class)
          .addFunction(Measure.BUCKETED_PV01, FraCalculationFunction.class)
          .addFunction(Measure.BUCKETED_GAMMA_PV01, FraCalculationFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private FraFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on FRA trades,
   * using the standard discounting calculation method.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measure#PAR_RATE Par rate}
   *   <li>{@linkplain Measure#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measure#EXPLAIN_PRESENT_VALUE Explain present value}
   *   <li>{@linkplain Measure#CASH_FLOWS Cash flows}
   *   <li>{@linkplain Measure#PV01 PV01}
   *   <li>{@linkplain Measure#BUCKETED_PV01 Bucketed PV01}
   *   <li>{@linkplain Measure#BUCKETED_GAMMA_PV01 Bucketed Gamma PV01}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<FraTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
