/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.calc.config.pricing.PricingRules;
import com.opengamma.strata.function.calculation.deposit.TermDepositCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fra.FraCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fx.FxNdfCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fx.FxSingleCalculationFunctionTest;
import com.opengamma.strata.function.calculation.fx.FxSwapCalculationFunctionTest;
import com.opengamma.strata.function.calculation.swap.SwapCalculationFunctionTest;

/**
 * Test {@link StandardPricingRules}.
 */
@Test
public class StandardPricingRulesTest {

  public void test_standard() {
    PricingRules test = StandardPricingRules.standard();
    assertThat(test.configuredMeasures(FraCalculationFunctionTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(FxSingleCalculationFunctionTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(FxNdfCalculationFunctionTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(FxSwapCalculationFunctionTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(SwapCalculationFunctionTest.TRADE)).isNotEmpty();
    assertThat(test.configuredMeasures(TermDepositCalculationFunctionTest.TRADE)).isNotEmpty();
  }

  public void coverage() {
    coverPrivateConstructor(StandardPricingRules.class);
  }

}
