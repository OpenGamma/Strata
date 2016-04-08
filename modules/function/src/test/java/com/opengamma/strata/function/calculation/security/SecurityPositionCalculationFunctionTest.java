/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.security;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.ImmutableReferenceData;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPosition;

/**
 * Test {@link SecurityPositionCalculationFunction}.
 */
@Test
public class SecurityPositionCalculationFunctionTest {

  private static final double MARKET_PRICE = 99.42;
  private static final double TICK_SIZE = 0.01;
  private static final int TICK_VALUE = 10;
  private static final int QUANTITY = 20;
  private static final SecurityId SEC_ID = SecurityId.of("OG-Future", "Foo-Womble-Mar14");
  public static final SecurityPosition POSITION = SecurityPosition.ofNet(SEC_ID, QUANTITY);
  private static final GenericSecurity FUTURE = GenericSecurity.of(
      SecurityInfo.of(SEC_ID, TICK_SIZE, CurrencyAmount.of(EUR, TICK_VALUE)));
  private static final ReferenceData REF_DATA = ImmutableReferenceData.of(SEC_ID, FUTURE);
  private static final Currency CURRENCY = FUTURE.getCurrency();
  private static final LocalDate VAL_DATE = LocalDate.of(2013, 12, 8);

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<SecurityPosition> test = SecurityPositionFunctionGroups.market();
    assertThat(test.configuredMeasures(POSITION)).contains(
        Measures.PRESENT_VALUE);
    FunctionConfig<SecurityPosition> config =
        SecurityPositionFunctionGroups.market().functionConfig(POSITION, Measures.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(SecurityPositionCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    SecurityPositionCalculationFunction function = new SecurityPositionCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(POSITION, measures, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(ImmutableSet.of(QuoteKey.of(SEC_ID.getStandardId())));
    assertThat(reqs.getTimeSeriesRequirements()).isEmpty();
    assertThat(function.naturalCurrency(POSITION, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_presentValue() {
    SecurityPositionCalculationFunction function = new SecurityPositionCalculationFunction();
    CalculationMarketData md = marketData();
    
    double unitPv = (MARKET_PRICE / TICK_SIZE) * TICK_VALUE;
    CurrencyAmount expectedPv = CurrencyAmount.of(CURRENCY, unitPv * QUANTITY);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE);
    assertThat(function.calculate(POSITION, measures, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))));
  }

  //-------------------------------------------------------------------------
  private CalculationMarketData marketData() {
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(QuoteKey.of(SEC_ID.getStandardId()), MARKET_PRICE),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(SecurityPositionFunctionGroups.class);
    coverPrivateConstructor(SecurityMeasureCalculations.class);
  }

}
