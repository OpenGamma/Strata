/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.ResultStatus.AWAITING_MARKET_DATA;
import static com.opengamma.sesame.ResultStatus.SUCCESS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.util.money.Currency;

public class EquityPresentValueTest {

  EquityPresentValueFunction _equityPresentValueFunction;
  MarketDataProvider _marketDataProviderFunction;


  @BeforeMethod
  public void setUp() {
    _marketDataProviderFunction = new MarketDataProvider(new StandardResultGenerator());
    _equityPresentValueFunction = new EquityPresentValue(_marketDataProviderFunction);
  }

  @Test
  public void testMarketDataUnavailable() {
    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());
    FunctionResult<Double> result = _equityPresentValueFunction.presentValue(security);
    assertThat(result.getStatus(), is(AWAITING_MARKET_DATA));
  }

  @Test
  public void testMarketDataAvailable() {
    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());

    FunctionResult<Double> result = _equityPresentValueFunction.presentValue(security);
    assertThat(result.getStatus(), is(SUCCESS));
    assertThat(result.getResult(), is(123.45));
  }

}
