/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fra;

import com.opengamma.strata.calc.config.Measures;
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
          .addFunction(Measures.PAR_RATE, FraCalculationFunction.class)
          .addFunction(Measures.PAR_SPREAD, FraCalculationFunction.class)
          .addFunction(Measures.PRESENT_VALUE, FraCalculationFunction.class)
          .addFunction(Measures.EXPLAIN_PRESENT_VALUE, FraCalculationFunction.class)
          .addFunction(Measures.CASH_FLOWS, FraCalculationFunction.class)
          .addFunction(Measures.PV01, FraCalculationFunction.class)
          .addFunction(Measures.BUCKETED_PV01, FraCalculationFunction.class)
          .addFunction(Measures.BUCKETED_GAMMA_PV01, FraCalculationFunction.class)
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
   *   <li>{@linkplain Measures#PAR_RATE Par rate}
   *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
   *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
   *   <li>{@linkplain Measures#EXPLAIN_PRESENT_VALUE Explain present value}
   *   <li>{@linkplain Measures#CASH_FLOWS Cash flows}
   *   <li>{@linkplain Measures#PV01 PV01}
   *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
   *   <li>{@linkplain Measures#BUCKETED_GAMMA_PV01 Bucketed Gamma PV01}
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<FraTrade> discounting() {
    return DISCOUNTING_GROUP;
  }

}
