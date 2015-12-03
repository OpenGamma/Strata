/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.fx;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.key.QuoteKey;

@Test
public class FxRateMarketDataFunctionTest {

  private static final QuoteKey QUOTE_KEY = QuoteKey.of(StandardId.of("test", "EUR/USD"));
  private static final QuoteId QUOTE_ID = QUOTE_KEY.toMarketDataId(MarketDataFeed.NONE);
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
    String regex = "No default configuration found with type .*FxRateConfig";
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
    CalculationEnvironment marketData = CalculationEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(QUOTE_ID, quoteBox)
        .build();
    MarketDataBox<FxRate> rateBox = function.build(RATE_ID, marketData, config());
    assertThat(rateBox.isSingleValue()).isTrue();
    assertThat(rateBox.getSingleValue()).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.1d));
  }

  public void buildInverse() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    MarketDataBox<Double> quoteBox = MarketDataBox.ofSingleValue(1.1d);
    CalculationEnvironment marketData = CalculationEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(QUOTE_ID, quoteBox)
        .build();
    MarketDataBox<FxRate> rateBox = function.build(FxRateId.of(CURRENCY_PAIR.inverse()), marketData, config());
    assertThat(rateBox.isSingleValue()).isTrue();
    assertThat(rateBox.getSingleValue()).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.1d));
  }

  public void buildScenario() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    MarketDataBox<Double> quoteBox = MarketDataBox.ofScenarioValues(1.1d, 1.2d, 1.3d);
    CalculationEnvironment marketData = CalculationEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(QUOTE_ID, quoteBox)
        .build();
    MarketDataBox<FxRate> rateBox = function.build(RATE_ID, marketData, config());
    assertThat(rateBox.isSingleValue()).isFalse();
    assertThat(rateBox.getScenarioCount()).isEqualTo(3);
    assertThat(rateBox.getValue(0)).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.1d));
    assertThat(rateBox.getValue(1)).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.2d));
    assertThat(rateBox.getValue(2)).isEqualTo(FxRate.of(CURRENCY_PAIR, 1.3d));
  }

  public void buildMissingConfig() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    String regex = "No default configuration found with type .*FxRateConfig";
    assertThrowsIllegalArg(() -> function.build(RATE_ID, CalculationEnvironment.empty(), MarketDataConfig.empty()), regex);
  }

  public void buildNoConfigForPair() {
    FxRateMarketDataFunction function = new FxRateMarketDataFunction();
    String regex = "No FX rate configuration available for GBP/USD";
    CurrencyPair gbpUsd = CurrencyPair.of(Currency.GBP, Currency.USD);
    assertThrowsIllegalArg(() -> function.build(FxRateId.of(gbpUsd), CalculationEnvironment.empty(), config()), regex);
  }

  private static MarketDataConfig config() {
    Map<CurrencyPair, QuoteKey> ratesMap = ImmutableMap.of(CURRENCY_PAIR, QUOTE_KEY);
    FxRateConfig fxRateConfig = FxRateConfig.builder().observableRates(ratesMap).build();
    return MarketDataConfig.builder().addDefault(fxRateConfig).build();
  }
}
