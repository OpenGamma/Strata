/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.SuccessStatus.SUCCESS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.marketdata.DefaultResettableMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FunctionResult;
import com.opengamma.util.result.ResultStatus;

public class EquityPresentValueTest {

  private EquityPresentValueFn _equityPresentValueFn;
  private ResettableMarketDataFn _marketDataProviderFunction;
  private ZonedDateTime _valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);

  @BeforeMethod
  public void setUp() {
    _marketDataProviderFunction = new DefaultResettableMarketDataFn();
    _equityPresentValueFn = new EquityPresentValue(_marketDataProviderFunction);
  }

  @Test
  public void testMarketDataUnavailable() {

    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());
    FunctionResult<Double> result = _equityPresentValueFn.presentValue(security);
    assertThat(result.getStatus(), is((ResultStatus) MISSING_DATA));
  }

  @Test
  public void testMarketDataAvailable() {

    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());

    Map<MarketDataRequirement, MarketDataItem> marketData = new HashMap<>();
    MarketDataRequirement requirement = MarketDataRequirementFactory.of(security, MarketDataRequirementNames.MARKET_VALUE);
    marketData.put(requirement, MarketDataItem.available(123.45));
    _marketDataProviderFunction.resetMarketData(_valuationTime, marketData);

    FunctionResult<Double> result = _equityPresentValueFn.presentValue(security);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));
    assertThat(result.getResult(), is(123.45));
  }

}
