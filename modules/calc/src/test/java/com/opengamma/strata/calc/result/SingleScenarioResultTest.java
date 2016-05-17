/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.result;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRatesArray;
import com.opengamma.strata.calc.runner.ScenarioFxRateProvider;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link SingleScenarioResult}.
 */
@Test
public class SingleScenarioResultTest {

  public void create() {
    SingleScenarioResult<String> test = SingleScenarioResult.of(3, "A");
    assertEquals(test.getScenarioCount(), 3);
    assertEquals(test.getValue(), "A");
    assertEquals(test.get(0), "A");
    assertEquals(test.get(1), "A");
    assertEquals(test.get(2), "A");
    assertEquals(test.stream().collect(toList()), ImmutableList.of("A", "A", "A"));
  }

  public void convertCurrencyAmount() {
    FxRatesArray rates = FxRatesArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);
    SingleScenarioResult<CurrencyAmount> test = SingleScenarioResult.of(3, CurrencyAmount.of(GBP, 2));

    ScenarioResult<?> convertedList = test.convertedTo(USD, fxProvider);
    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(USD, 2 * 1.61),
        CurrencyAmount.of(USD, 2 * 1.62),
        CurrencyAmount.of(USD, 2 * 1.63));
    DefaultScenarioResult<CurrencyAmount> expectedList = DefaultScenarioResult.of(expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  public void coverage() {
    SingleScenarioResult<String> test = SingleScenarioResult.of(3, "A");
    coverImmutableBean(test);
    SingleScenarioResult<String> test2 = SingleScenarioResult.of(2, "B");
    coverBeanEquals(test, test2);
  }

}
