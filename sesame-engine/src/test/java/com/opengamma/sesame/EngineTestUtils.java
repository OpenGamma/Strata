/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.money.Currency.AUD;
import static com.opengamma.util.money.Currency.GBP;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.threeten.bp.ZonedDateTime;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.security.impl.SimpleSecurityLink;
import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.sesame.cache.MethodInvocationKey;

/**
 * Helper methods for engine tests.
 */
public class EngineTestUtils {

  public static final String EQUITY_NAME = "An equity security";
  public static final String CASH_FLOW_NAME = "A cash flow security";
  public static final String EQUITY_BLOOMBERG_TICKER = "ACME US Equity";
  public static final String EQUITY_ACTIV_SYMBOL = "ACME.";
  public static final String CASH_FLOW_BLOOMBERG_TICKER = "TEST US Cash Flow";
  public static final String CASH_FLOW_ACTIV_SYMBOL = "CASHFLOW.";

  private static final UniqueId EQUITY_TRADE_ID = UniqueId.of("trdId", "321");
  private static final UniqueId CASH_FLOW_TRADE_ID = UniqueId.of("trdId", "432");
  private static final long MAX_CACHE_ENTRIES = 100_000;

  private EngineTestUtils() {
  }

  public static SimpleTrade createEquityTrade() {
    EquitySecurity security = new EquitySecurity("exc", "exc", "compName", AUD);
    security.setUniqueId(UniqueId.of("secId", "123"));
    security.setName(EQUITY_NAME);
    security.addExternalId(ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, EQUITY_BLOOMBERG_TICKER));
    security.addExternalId(ExternalId.of(ExternalSchemes.ACTIVFEED_TICKER, EQUITY_ACTIV_SYMBOL));
    SimpleTrade trade = new SimpleTrade();
    trade.setQuantity(BigDecimal.ONE);
    SimpleSecurityLink securityLink = new SimpleSecurityLink(security.getExternalIdBundle());
    securityLink.setTarget(security);
    trade.setSecurityLink(securityLink);
    trade.setUniqueId(EQUITY_TRADE_ID);
    return trade;
  }

  public static SimpleTrade createCashFlowTrade() {
    CashFlowSecurity security = new CashFlowSecurity(GBP, ZonedDateTime.now(), 12345d);
    security.setUniqueId(UniqueId.of("secId", "234"));
    security.setName(CASH_FLOW_NAME);
    security.addExternalId(ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, CASH_FLOW_BLOOMBERG_TICKER));
    security.addExternalId(ExternalId.of(ExternalSchemes.ACTIVFEED_TICKER, CASH_FLOW_ACTIV_SYMBOL));
    SimpleTrade trade = new SimpleTrade();
    trade.setQuantity(BigDecimal.ONE);
    SimpleSecurityLink securityLink = new SimpleSecurityLink(security.getExternalIdBundle());
    securityLink.setTarget(security);
    trade.setSecurityLink(securityLink);
    trade.setUniqueId(CASH_FLOW_TRADE_ID);
    return trade;
  }

  /**
   * @return a cache configured for use with the engine
   */
  public static Cache<MethodInvocationKey, FutureTask<Object>> createCache() {
    int concurrencyLevel = Runtime.getRuntime().availableProcessors() + 2;
    return CacheBuilder.newBuilder().maximumSize(MAX_CACHE_ENTRIES).concurrencyLevel(concurrencyLevel).build();
  }

  /**
   * {@link ExecutorService} that uses the calling thread to run all tasks. Nice and simple for unit tests.
   */
  public static class DirectExecutorService extends AbstractExecutorService {

    @Override
    public void execute(Runnable command) {
      command.run();
    }

    @Override
    public void shutdown() {
      throw new UnsupportedOperationException("shutdown not supported");
    }

    @Override
    public List<Runnable> shutdownNow() {
      throw new UnsupportedOperationException("shutdownNow not supported");
    }

    @Override
    public boolean isShutdown() {
      throw new UnsupportedOperationException("isShutdown not supported");
    }

    @Override
    public boolean isTerminated() {
      throw new UnsupportedOperationException("isTerminated not supported");
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      throw new UnsupportedOperationException("awaitTermination not supported");
    }
  }
}
