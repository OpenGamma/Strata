/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.equity;

import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.SuccessStatus.SUCCESS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.equity.DefaultEquityPresentValueFn;
import com.opengamma.sesame.equity.EquityPresentValueFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.MapMarketDataSource;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;

public class DefaultEquityPresentValueFnTest {

  private EquityPresentValueFn _equityPresentValueFn;

  @BeforeMethod
  public void setUp() {
    _equityPresentValueFn = new DefaultEquityPresentValueFn(new DefaultMarketDataFn(mock(CurrencyMatrix.class)));
  }

  @Test
  public void testMarketDataUnavailable() {
    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());
    Environment env = new SimpleEnvironment(ZonedDateTime.now(), MapMarketDataSource.of());
    Result<Double> result = _equityPresentValueFn.presentValue(env, security);
    assertThat(result.getStatus(), is((ResultStatus) MISSING_DATA));
  }

  @Test
  public void testMarketDataAvailable() {
    EquitySecurity security = new EquitySecurity("LSE", "LSE", "BloggsCo", Currency.GBP);
    security.setExternalIdBundle(ExternalSchemes.bloombergTickerSecurityId("BLGG").toBundle());
    MarketDataSource dataSource = MapMarketDataSource.of(security.getExternalIdBundle(), 123.45);
    Environment env = new SimpleEnvironment(ZonedDateTime.now(), dataSource);
    Result<Double> result = _equityPresentValueFn.presentValue(env, security);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));
    assertThat(result.getValue(), is(123.45));
  }

}
