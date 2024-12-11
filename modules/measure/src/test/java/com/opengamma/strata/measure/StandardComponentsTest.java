/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.measure.cms.CmsTradeCalculationFunctionTest;
import com.opengamma.strata.measure.deposit.TermDepositTradeCalculationFunctionTest;
import com.opengamma.strata.measure.fra.FraTradeCalculationFunctionTest;
import com.opengamma.strata.measure.fx.FxNdfTradeCalculationFunctionTest;
import com.opengamma.strata.measure.fx.FxSingleTradeCalculationFunctionTest;
import com.opengamma.strata.measure.fx.FxSwapTradeCalculationFunctionTest;
import com.opengamma.strata.measure.fxopt.FxCollarTradeCalculationFunctionTest;
import com.opengamma.strata.measure.swap.SwapTradeCalculationFunctionTest;

/**
 * Test {@link StandardComponents}.
 */
public class StandardComponentsTest {

  @Test
  public void test_standard() {
    CalculationFunctions test = StandardComponents.calculationFunctions();
    assertThat(test.findFunction(FraTradeCalculationFunctionTest.TRADE)).isPresent();
    assertThat(test.findFunction(FxSingleTradeCalculationFunctionTest.TRADE)).isPresent();
    assertThat(test.findFunction(FxNdfTradeCalculationFunctionTest.TRADE)).isPresent();
    assertThat(test.findFunction(FxSwapTradeCalculationFunctionTest.TRADE)).isPresent();
    assertThat(test.findFunction(FxCollarTradeCalculationFunctionTest.TRADE)).isPresent();
    assertThat(test.findFunction(SwapTradeCalculationFunctionTest.TRADE)).isPresent();
    assertThat(test.findFunction(TermDepositTradeCalculationFunctionTest.TRADE)).isPresent();
    assertThat(test.findFunction(CmsTradeCalculationFunctionTest.TRADE)).isPresent();
  }

  @Test
  public void coverage() {
    coverPrivateConstructor(StandardComponents.class);
  }

}
