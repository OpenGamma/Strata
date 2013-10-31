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

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

public class EquityPresentValueTest {

  EquityPresentValueFunction _equityPresentValueFunction;

  @BeforeTest
  public void setUp() {
    _equityPresentValueFunction = new EquityPresentValue();
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

    Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>> marketData = new HashMap<>();
    marketData.put(
        StandardMarketDataRequirement.of(security, MarketDataRequirementNames.MARKET_VALUE),
        Pairs.of(MarketDataStatus.AVAILABLE, new SingleMarketDataValue(123.45)));

    FunctionResult<Double> result = _equityPresentValueFunction.presentValue(security);
    assertThat(result.getStatus(), is(SUCCESS));
    assertThat(result.getResult(), is(123.45));
  }

}
