/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FailureStatus.PENDING_DATA;
import static com.opengamma.util.result.SuccessStatus.SUCCESS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.marketdata.spec.MarketData;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.LDClient;
import com.opengamma.sesame.marketdata.ResettableLiveMarketDataSource;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;

public class EquityPresentValueTest {

  private EquityPresentValueFn _equityPresentValueFn;

  @BeforeMethod
  public void setUp() {
    _equityPresentValueFn = new EquityPresentValue(new DefaultMarketDataFn(mock(CurrencyMatrix.class)));
  }

  @Test
  public void testMarketDataUnavailable() {

    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());
    Map<ExternalIdBundle, Double> marketData = Collections.emptyMap();
    Environment env = new SimpleEnvironment(ZonedDateTime.now(), marketDataSource(marketData));
    Result<Double> result = _equityPresentValueFn.presentValue(env, security);
    assertThat(result.getStatus(), is((ResultStatus) PENDING_DATA));
  }

  @Test
  public void testMarketDataAvailable() {

    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());

    //Map<MarketDataRequirement, MarketDataItem> marketData = new HashMap<>();
    //MarketDataRequirement requirement = MarketDataRequirementFactory.of(security, MarketDataRequirementNames.MARKET_VALUE);
    //marketData.put(requirement, MarketDataItem.available(123.45));
    Map<ExternalIdBundle, Double> marketData = ImmutableMap.of(security.getExternalIdBundle(), 123.45);
    ResettableLiveMarketDataSource dataSource = marketDataSource(marketData);
    Environment env = new SimpleEnvironment(ZonedDateTime.now(), dataSource);
    Result<Double> result = _equityPresentValueFn.presentValue(env, security);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));
    assertThat(result.getValue(), is(123.45));
  }

  private ResettableLiveMarketDataSource marketDataSource(Map<ExternalIdBundle, Double> marketData) {
    FieldName fieldName = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);
    ResettableLiveMarketDataSource.Builder builder = new ResettableLiveMarketDataSource.Builder(MarketData.live(), mock(LDClient.class));
    return builder.data(fieldName, marketData).build();
  }
}
