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

import com.opengamma.engine.target.ComputationTargetType;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.currency.SimpleCurrencyMatrix;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class CurrencyPairMarketDataRequirementTest {

  private static final double DELTA = 0.00001;
  private static final String MARKET_VALUE = "Market_Value";
  private static final double GBPUSD_RATE = 1.61;
  private static final double USDCHF_RATE = 0.91;
  private static final double EURUSD_RATE = 1.35;

  @Test
  public void fixedRate() {
    SimpleCurrencyMatrix matrix = new SimpleCurrencyMatrix();
    matrix.setFixedConversion(Currency.GBP, Currency.USD, GBPUSD_RATE);

    CurrencyPairMarketDataRequirement requirement = new CurrencyPairMarketDataRequirement(CurrencyPair.parse("GBP/USD"));
    Double spotRate = (Double) requirement.getFxRate(matrix, mock(RawMarketDataSource.class)).getValue();
    assertEquals(GBPUSD_RATE, spotRate, DELTA);

    CurrencyPairMarketDataRequirement reciprocal = new CurrencyPairMarketDataRequirement(CurrencyPair.parse("USD/GBP"));
    Double reciprocalSpotRate = (Double) reciprocal.getFxRate(matrix, mock(RawMarketDataSource.class)).getValue();
    assertEquals(1 / GBPUSD_RATE, reciprocalSpotRate, DELTA);
  }

  @Test
  public void lookUpRate() {
    SimpleCurrencyMatrix matrix = new SimpleCurrencyMatrix();
    ExternalId rateId = ExternalId.of("x", "GBPUSD");
    ValueRequirement valueReq = new ValueRequirement(MARKET_VALUE, ComputationTargetType.PRIMITIVE, rateId);
    matrix.setLiveData(Currency.GBP, Currency.USD, valueReq);
    RawMarketDataSource dataSource = mock(RawMarketDataSource.class);
    Object value = GBPUSD_RATE;
    when(dataSource.get(ExternalIdBundle.of(rateId), MARKET_VALUE)).thenAnswer(new Returns(MarketDataItem.available(value)));

    CurrencyPairMarketDataRequirement requirement = new CurrencyPairMarketDataRequirement(CurrencyPair.parse("GBP/USD"));
    Double spotRate = (Double) requirement.getFxRate(matrix, dataSource).getValue();
    assertEquals(GBPUSD_RATE, spotRate, DELTA);

    CurrencyPairMarketDataRequirement reciprocal = new CurrencyPairMarketDataRequirement(CurrencyPair.parse("USD/GBP"));
    Double reciprocalSpotRate = (Double) reciprocal.getFxRate(matrix, dataSource).getValue();
    assertEquals(1 / GBPUSD_RATE, reciprocalSpotRate, DELTA);
  }

  @Test
  public void crossFixed() {
    SimpleCurrencyMatrix matrix = new SimpleCurrencyMatrix();
    matrix.setFixedConversion(Currency.USD, Currency.CHF, USDCHF_RATE);
    matrix.setFixedConversion(Currency.EUR, Currency.USD, EURUSD_RATE);
    matrix.setCrossConversion(Currency.EUR, Currency.CHF, Currency.USD);

    CurrencyPairMarketDataRequirement requirement = new CurrencyPairMarketDataRequirement(CurrencyPair.parse("EUR/CHF"));
    Double spotRate = (Double) requirement.getFxRate(matrix, mock(RawMarketDataSource.class)).getValue();
    assertEquals(USDCHF_RATE * EURUSD_RATE, spotRate, DELTA);

    CurrencyPairMarketDataRequirement reciprocal = new CurrencyPairMarketDataRequirement(CurrencyPair.parse("CHF/EUR"));
    Double reciprocalSpotRate = (Double) reciprocal.getFxRate(matrix, mock(RawMarketDataSource.class)).getValue();
    assertEquals(1 / (USDCHF_RATE * EURUSD_RATE), reciprocalSpotRate, DELTA);
  }

  @Test
  public void crossLookup() {
    SimpleCurrencyMatrix matrix = new SimpleCurrencyMatrix();

    ExternalId usdchfRateId = ExternalId.of("x", "USDCHF");
    ValueRequirement usdchfReq = new ValueRequirement(MARKET_VALUE, ComputationTargetType.PRIMITIVE, usdchfRateId);
    matrix.setLiveData(Currency.USD, Currency.CHF, usdchfReq);
    Object usdchfValue = USDCHF_RATE;

    ExternalId eurusdRateId = ExternalId.of("x", "EURUSD");
    ValueRequirement eurusdReq = new ValueRequirement(MARKET_VALUE, ComputationTargetType.PRIMITIVE, eurusdRateId);
    matrix.setLiveData(Currency.EUR, Currency.USD, eurusdReq);
    Object eurusdValue = EURUSD_RATE;

    matrix.setCrossConversion(Currency.EUR, Currency.CHF, Currency.USD);

    RawMarketDataSource dataSource = mock(RawMarketDataSource.class);
    when(dataSource.get(ExternalIdBundle.of(usdchfRateId), MARKET_VALUE)).thenAnswer(new Returns(MarketDataItem.available(usdchfValue)));
    when(dataSource.get(ExternalIdBundle.of(eurusdRateId), MARKET_VALUE)).thenAnswer(new Returns(MarketDataItem.available(eurusdValue)));

    CurrencyPairMarketDataRequirement requirement = new CurrencyPairMarketDataRequirement(CurrencyPair.parse("EUR/CHF"));
    Double spotRate = (Double) requirement.getFxRate(matrix, dataSource).getValue();
    assertEquals(USDCHF_RATE * EURUSD_RATE, spotRate, DELTA);

    CurrencyPairMarketDataRequirement reciprocal = new CurrencyPairMarketDataRequirement(CurrencyPair.parse("CHF/EUR"));
    Double reciprocalSpotRate = (Double) reciprocal.getFxRate(matrix, dataSource).getValue();
    assertEquals(1 / (USDCHF_RATE * EURUSD_RATE), reciprocalSpotRate, DELTA);
  }

  // TODO same tests but for time series
}
