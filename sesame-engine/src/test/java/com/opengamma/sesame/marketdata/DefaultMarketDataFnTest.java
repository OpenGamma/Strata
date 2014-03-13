/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

import org.mockito.internal.stubbing.answers.Returns;
import org.testng.annotations.Test;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.target.ComputationTargetType;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.currency.SimpleCurrencyMatrix;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class DefaultMarketDataFnTest {

  private static final double DELTA = 0.00001;
  private static final FieldName MARKET_VALUE = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);
  private static final double GBPUSD_RATE = 1.61;
  private static final double USDCHF_RATE = 0.91;
  private static final double EURUSD_RATE = 1.35;

  @Test
  public void fixedRate() {
    SimpleCurrencyMatrix matrix = new SimpleCurrencyMatrix();
    matrix.setFixedConversion(Currency.GBP, Currency.USD, GBPUSD_RATE);
    DefaultMarketDataFn fn = new DefaultMarketDataFn(matrix);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), mock(MarketDataSource.class));

    Double spotRate = fn.getFxRate(env, CurrencyPair.parse("GBP/USD")).getValue();
    assertEquals(GBPUSD_RATE, spotRate, DELTA);

    Double reciprocalSpotRate = fn.getFxRate(env, CurrencyPair.parse("USD/GBP")).getValue();
    assertEquals(1 / GBPUSD_RATE, reciprocalSpotRate, DELTA);
  }

  @Test
  public void lookUpRate() {
    SimpleCurrencyMatrix matrix = new SimpleCurrencyMatrix();
    ExternalId rateId = ExternalId.of("x", "GBPUSD");
    ValueRequirement valueReq = new ValueRequirement(MARKET_VALUE.getName(), ComputationTargetType.PRIMITIVE, rateId);
    matrix.setLiveData(Currency.GBP, Currency.USD, valueReq);
    MarketDataSource dataSource = mock(MarketDataSource.class);
    Object value = GBPUSD_RATE;
    when(dataSource.get(rateId.toBundle(), MARKET_VALUE)).thenAnswer(new Returns(Result.success(value)));
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), dataSource);
    DefaultMarketDataFn fn = new DefaultMarketDataFn(matrix);

    Double spotRate = fn.getFxRate(env, CurrencyPair.parse("GBP/USD")).getValue();
    assertEquals(GBPUSD_RATE, spotRate, DELTA);

    Double reciprocalSpotRate = fn.getFxRate(env, CurrencyPair.parse("USD/GBP")).getValue();
    assertEquals(1 / GBPUSD_RATE, reciprocalSpotRate, DELTA);
  }

  @Test
  public void crossFixed() {
    SimpleCurrencyMatrix matrix = new SimpleCurrencyMatrix();
    matrix.setFixedConversion(Currency.USD, Currency.CHF, USDCHF_RATE);
    matrix.setFixedConversion(Currency.EUR, Currency.USD, EURUSD_RATE);
    matrix.setCrossConversion(Currency.EUR, Currency.CHF, Currency.USD);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), mock(MarketDataSource.class));
    DefaultMarketDataFn fn = new DefaultMarketDataFn(matrix);

    Double spotRate = fn.getFxRate(env, CurrencyPair.parse("EUR/CHF")).getValue();
    assertEquals(USDCHF_RATE * EURUSD_RATE, spotRate, DELTA);

    Double reciprocalSpotRate = fn.getFxRate(env, CurrencyPair.parse("CHF/EUR")).getValue();
    assertEquals(1 / (USDCHF_RATE * EURUSD_RATE), reciprocalSpotRate, DELTA);
  }

  @Test
  public void crossLookup() {
    SimpleCurrencyMatrix matrix = new SimpleCurrencyMatrix();

    ExternalId usdchfRateId = ExternalId.of("x", "USDCHF");
    ValueRequirement usdchfReq = new ValueRequirement(MARKET_VALUE.getName(), ComputationTargetType.PRIMITIVE, usdchfRateId);
    matrix.setLiveData(Currency.USD, Currency.CHF, usdchfReq);
    Object usdchfValue = USDCHF_RATE;

    ExternalId eurusdRateId = ExternalId.of("x", "EURUSD");
    ValueRequirement eurusdReq = new ValueRequirement(MARKET_VALUE.getName(), ComputationTargetType.PRIMITIVE, eurusdRateId);
    matrix.setLiveData(Currency.EUR, Currency.USD, eurusdReq);
    Object eurusdValue = EURUSD_RATE;

    matrix.setCrossConversion(Currency.EUR, Currency.CHF, Currency.USD);
    DefaultMarketDataFn fn = new DefaultMarketDataFn(matrix);

    MarketDataSource dataSource = mock(MarketDataSource.class);
    when(dataSource.get(ExternalIdBundle.of(usdchfRateId), MARKET_VALUE)).thenAnswer(new Returns(Result.success(usdchfValue)));
    when(dataSource.get(ExternalIdBundle.of(eurusdRateId), MARKET_VALUE)).thenAnswer(new Returns(Result.success(eurusdValue)));
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), dataSource);

    Double spotRate = fn.getFxRate(env, CurrencyPair.parse("EUR/CHF")).getValue();
    assertEquals(USDCHF_RATE * EURUSD_RATE, spotRate, DELTA);

    Double reciprocalSpotRate = fn.getFxRate(env, CurrencyPair.parse("CHF/EUR")).getValue();
    assertEquals(1 / (USDCHF_RATE * EURUSD_RATE), reciprocalSpotRate, DELTA);
  }
}
