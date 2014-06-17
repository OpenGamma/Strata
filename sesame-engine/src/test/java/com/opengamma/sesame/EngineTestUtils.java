/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.money.Currency.AUD;
import static com.opengamma.util.money.Currency.GBP;
import static org.testng.AssertJUnit.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.ZonedDateTime;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.security.impl.SimpleSecurityLink;
import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.cache.MethodInvocationKey;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.CachingManager;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.DefaultCachingManager;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.engine.FunctionService;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.util.ArgumentChecker;

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

  public static ViewFactory createViewFactory(Map<Class<?>, Object> components,
                                              AvailableOutputs availableOutputs,
                                              AvailableImplementations availableImplementations) {

    ComponentMap componentMap = ComponentMap.of(components);

    Cache<MethodInvocationKey, FutureTask<Object>> cache =
        CacheBuilder.newBuilder().maximumSize(10000).build();

    CachingManager cachingManager = new DefaultCachingManager(componentMap, cache);

    FixedInstantVersionCorrectionProvider versionCorrectionProvider =
        new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components)
        .with(VersionCorrectionProvider.class, versionCorrectionProvider);
    ThreadLocalServiceContext.init(serviceContext);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    return new ViewFactory(executor,
                           availableOutputs,
                           availableImplementations,
                           FunctionModelConfig.EMPTY,
                           FunctionService.DEFAULT_SERVICES,
                           cachingManager);
  }


  /**
   * @return a cache configured for use with the engine
   */
  public static Cache<MethodInvocationKey, FutureTask<Object>> createCache() {
    int concurrencyLevel = Runtime.getRuntime().availableProcessors() + 2;
    return CacheBuilder.newBuilder().maximumSize(MAX_CACHE_ENTRIES).concurrencyLevel(concurrencyLevel).build();
  }

  public static void assertJsonEquals(Map<?, ?> expected, Map<?, ?> actual) {
    List<MapDifference> differences = compareMaps(expected, actual);

    if (!differences.isEmpty()) {
      fail("JSON not equal, differences:\n" + StringUtils.join(differences, "\n") + "\n");
    }
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

  public static class MapDifference {

    private final List<Object> _path;
    private final Object _left;
    private final Object _right;

    public MapDifference(@Nullable Object left, @Nullable Object right, List<Object> path) {
      _path = ArgumentChecker.notNull(path, "path");
      _left = left;
      _right = right;
    }

    @Override
    public String toString() {
      String left = (_left instanceof String) ? "\"" + _left + "\"" : Objects.toString(_left);
      String right = (_right instanceof String) ? "\"" + _right + "\"" : Objects.toString(_right);
      return left + ", " + right + ", " + _path;
    }

    @Override
    public int hashCode() {
      return Objects.hash(_path, _left, _right);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      MapDifference other = (MapDifference) obj;
      return Objects.equals(this._path, other._path) &&
          Objects.equals(this._left, other._left) &&
          Objects.equals(this._right, other._right);
    }
  }

  public static List<MapDifference> compareMaps(Map<?, ?> left, Map<?, ?> right) {
    List<Object> path = new ArrayList<>();
    return compareMaps(left, right, path);
  }

  @SuppressWarnings("unchecked")
  private static List<MapDifference> compareMaps(Object left, Object right, List<Object> path) {
    ImmutableList.Builder<MapDifference> differences = ImmutableList.builder();

    if (left instanceof Map && right instanceof Map) {
      Map leftMap = (Map) left;
      Map rightMap = (Map) right;
      Set<Object> keys = new TreeSet<Object>(leftMap.keySet());
      keys.addAll(rightMap.keySet());

      for (Object key : keys) {
        Object leftVal = leftMap.get(key);
        Object rightVal = rightMap.get(key);
        differences.addAll(compareMaps(leftVal, rightVal, newPath(path, key)));
      }
    } else if (left instanceof Collection && right instanceof Collection) {
      List<?> leftList = Lists.newArrayList(((Collection) left));
      List<?> rightList = Lists.newArrayList(((Collection) right));
      int size = Math.max(leftList.size(), rightList.size());

      for (int i = 0; i < size; i++) {
        Object leftVal;
        Object rightVal;

        if (i == leftList.size()) {
          leftVal = null;
          rightVal = rightList.get(i);
        } else if (i == rightList.size()) {
          leftVal = leftList.get(i);
          rightVal = null;
        } else {
          leftVal = leftList.get(i);
          rightVal = rightList.get(i);
        }
        differences.addAll(compareMaps(leftVal, rightVal, newPath(path, i)));
      }
    } else {
      if (!Objects.equals(left, right)) {
        differences.add(new MapDifference(left, right, path));
      }
    }
    return differences.build();
  }

  private static List<Object> newPath(List<Object> path, Object nextElement) {
    return ImmutableList.builder().addAll(path).add(nextElement).build();
  }
}
