/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fx;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketData;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.observable.QuoteId;

@Test
public class FxRateMarketDataFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("test", "EUR/USD"));
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(Currency.EUR, Currency.USD);
  private static final FxRateId RATE_ID = FxRateId.of(CURRENCY_PAIR);

  public void requirements() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    MarketDataRequirements requirements = function.requirements(RATE_ID, config());
    assertThat(requirements).isEqualTo(MarketDataRequirements.of(QUOTE_ID));
  }

  public void requirementsInverse() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    MarketDataRequirements requirements = function.requirements(FxRateId.of(CURRENCY_PAIR.inverse()), config());
    assertThat(requirements).isEqualTo(MarketDataRequirements.of(QUOTE_ID));
  }

  public void requirementsMissingConfig() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    String regex = "No configuration found .*FxRateConfig";
    assertThrowsIllegalArg(() -> function.requirements(RATE_ID, MarketDataConfig.empty()), regex);
  }

  public void requirementsNoConfigForPair() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    CurrencyPair gbpUsd = CurrencyPair.of(Currency.GBP, Currency.USD);
    assertThat(function.requirements(FxRateId.of(gbpUsd), config())).isEqualTo(MarketDataRequirements.empty());
  }

  public void build() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    MarketDataBox<Double> quoteBox = MarketDataBox.ofSingleValue(1.1d);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(QUOTE_ID, quoteBox)
        .build();
    MarketDataBox<FxRate> rateBox = function.build(RATE_ID, config(), marketData, REF_DATA);
    assertThat(rateBox.isSingleValue()).isTrue();
    assertThat(rateBox.getSingleValue()).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.1d));
  }

  public void buildInverse() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    MarketDataBox<Double> quoteBox = MarketDataBox.ofSingleValue(1.1d);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(QUOTE_ID, quoteBox)
        .build();
    MarketDataBox<FxRate> rateBox = function.build(FxRateId.of(CURRENCY_PAIR.inverse()), config(), marketData, REF_DATA);
    assertThat(rateBox.isSingleValue()).isTrue();
    assertThat(rateBox.getSingleValue()).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.1d));
  }

  public void buildScenario() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    MarketDataBox<Double> quoteBox = MarketDataBox.ofScenarioValues(1.1d, 1.2d, 1.3d);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(LocalDate.of(2011, 3, 8))
        .addBox(QUOTE_ID, quoteBox)
        .build();
    MarketDataBox<FxRate> rateBox = function.build(RATE_ID, config(), marketData, REF_DATA);
    assertThat(rateBox.isSingleValue()).isFalse();
    assertThat(rateBox.getScenarioCount()).isEqualTo(3);
    assertThat(rateBox.getValue(0)).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.1d));
    assertThat(rateBox.getValue(1)).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.2d));
    assertThat(rateBox.getValue(2)).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.3d));
  }

  public void buildMissingConfig() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    String regex = "No configuration found .*FxRateConfig";
    assertThrowsIllegalArg(
        () -> function.build(RATE_ID, MarketDataConfig.empty(), ScenarioMarketData.empty(), REF_DATA), regex);
  }

  public void buildNoConfigForPair() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    String regex = "No FX rate configuration available for GBP/USD";
    CurrencyPair gbpUsd = CurrencyPair.of(Currency.GBP, Currency.USD);
    assertThrowsIllegalArg(
        () -> function.build(FxRateId.of(gbpUsd), config(), ScenarioMarketData.empty(), REF_DATA), regex);
  }

  private static MarketDataConfig config() {
    Map<CurrencyPair, QuoteId> ratesMap = ImmutableMap.of(CURRENCY_PAIR, QUOTE_ID);
    FxRateConfig fxRateConfig = FxRateConfig.builder().observableRates(ratesMap).build();
    return MarketDataConfig.builder().add(ObservableSource.NONE, fxRateConfig).build();
  }

}
