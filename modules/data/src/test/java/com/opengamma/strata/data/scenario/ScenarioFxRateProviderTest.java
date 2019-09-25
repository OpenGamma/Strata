/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ObservableSource;

/**
 * Test {@link ScenarioFxRateProvider}.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ScenarioFxRateProviderTest {

  private ScenarioFxRateProvider fxRateProvider;

  @BeforeAll
  public void setUp() throws Exception {
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), FxRate.of(Currency.GBP, Currency.USD, 1.4d))
        .build();

    fxRateProvider = ScenarioFxRateProvider.of(marketData);
  }

  @Test
  public void convert() {
    assertThat(fxRateProvider.convert(2, Currency.GBP, Currency.GBP, 0)).isEqualTo(2d);
    assertThat(fxRateProvider.convert(10, Currency.GBP, Currency.USD, 0)).isEqualTo(14d);
  }

  @Test
  public void fxRate() {
    assertThat(fxRateProvider.fxRate(Currency.GBP, Currency.GBP, 0)).isEqualTo(1d);
    assertThat(fxRateProvider.fxRate(Currency.GBP, Currency.USD, 0)).isEqualTo(1.4d);
  }

  @Test
  public void specifySource() {
    ObservableSource testSource = ObservableSource.of("test");
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), FxRate.of(Currency.GBP, Currency.USD, 1.4d))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD, testSource), FxRate.of(Currency.GBP, Currency.USD, 1.41d))
        .build();

    ScenarioFxRateProvider defaultRateProvider = ScenarioFxRateProvider.of(marketData);
    ScenarioFxRateProvider sourceRateProvider = ScenarioFxRateProvider.of(marketData, testSource);
    assertThat(defaultRateProvider.fxRate(Currency.GBP, Currency.USD, 0)).isEqualTo(1.4d);
    assertThat(sourceRateProvider.fxRate(Currency.GBP, Currency.USD, 0)).isEqualTo(1.41d);
  }
}
