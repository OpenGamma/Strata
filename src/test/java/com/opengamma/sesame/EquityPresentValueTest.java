/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.FailureStatus.MISSING_DATA;
import static com.opengamma.sesame.SuccessStatus.SUCCESS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

public class EquityPresentValueTest {

  private EquityPresentValueFunction _equityPresentValueFunction;
  private ResettableMarketDataProviderFunction _marketDataProviderFunction;

  @BeforeMethod
  public void setUp() {
    _marketDataProviderFunction = new MarketDataProvider();
    _equityPresentValueFunction = new EquityPresentValue(_marketDataProviderFunction);
  }

  @Test
  public void testMarketDataUnavailable() {

    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());
    FunctionResult<Double> result = _equityPresentValueFunction.presentValue(security);
    assertThat(result.getStatus(), is((ResultStatus) MISSING_DATA));
  }

  @Test
  public void testMarketDataAvailable() {

    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());

    Map<MarketDataRequirement, Pair<MarketDataStatus,MarketDataValue>> marketData = new HashMap<>();
    marketData.put(
        StandardMarketDataRequirement.of(security, MarketDataRequirementNames.MARKET_VALUE),
        Pairs.<MarketDataStatus,MarketDataValue>of(MarketDataStatus.AVAILABLE, new SingleMarketDataValue(123.45)));
    _marketDataProviderFunction.resetMarketData(marketData);

    FunctionResult<Double> result = _equityPresentValueFunction.presentValue(security);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));
    assertThat(result.getResult(), is(123.45));
  }

}
