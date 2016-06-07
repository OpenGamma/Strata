/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.function.calculation.deposit.TermDepositCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fra.FraCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fx.FxNdfCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fx.FxSingleCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fx.FxSwapCalculationFunctionTest;
import com.opengamma.strata.function.calculation.swap.SwapCalculationFunctionTest;

/**
 * Test {@link StandardComponents}.
 */
@Test
public class StandardComponentsTest {

  public void test_standard() {
    CalculationFunctions test = StandardComponents.calculationFunctions();
    assertEquals(test.findFunction(FraCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(FxSingleCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(FxNdfCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(FxSwapCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(SwapCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(TermDepositCalculationFunctionTest.TRADE).isPresent(), true);
  }

  public void coverage() {
    coverPrivateConstructor(StandardComponents.class);
  }

}
