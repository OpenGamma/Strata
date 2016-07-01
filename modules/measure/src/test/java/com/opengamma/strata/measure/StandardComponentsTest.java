/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.measure.deposit.TermDepositTradeCalculationFunctionTest;
import com.opengamma.strata.measure.fra.FraTradeCalculationFunctionTest;
import com.opengamma.strata.measure.fx.FxNdfTradeCalculationFunctionTest;
import com.opengamma.strata.measure.fx.FxSingleTradeCalculationFunctionTest;
import com.opengamma.strata.measure.fx.FxSwapTradeCalculationFunctionTest;
import com.opengamma.strata.measure.swap.SwapTradeCalculationFunctionTest;

/**
 * Test {@link StandardComponents}.
 */
@Test
public class StandardComponentsTest {

  public void test_standard() {
    CalculationFunctions test = StandardComponents.calculationFunctions();
    assertEquals(test.findFunction(FraTradeCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(FxSingleTradeCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(FxNdfTradeCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(FxSwapTradeCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(SwapTradeCalculationFunctionTest.TRADE).isPresent(), true);
    assertEquals(test.findFunction(TermDepositTradeCalculationFunctionTest.TRADE).isPresent(), true);
  }

  public void coverage() {
    coverPrivateConstructor(StandardComponents.class);
  }

}
