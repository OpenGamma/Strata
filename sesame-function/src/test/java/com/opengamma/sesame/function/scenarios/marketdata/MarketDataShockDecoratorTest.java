/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.marketdata;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import java.util.Map;

import org.testng.annotations.Test;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.currency.SimpleCurrencyMatrix;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.config.DecoratorConfig;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

@SuppressWarnings("unchecked")
@Test(groups = TestGroup.UNIT)
public class MarketDataShockDecoratorTest {

  private static final String SCHEME = "scheme";
  private static final String VALUE1 = "value1";
  private static final String VALUE2 = "value2";
  private static final ExternalIdBundle ID1 = ExternalIdBundle.of(SCHEME, VALUE1);
  private static final ExternalIdBundle ID2 = ExternalIdBundle.of(SCHEME, VALUE2);
  private static final FunctionModelConfig CONFIG =
      config(implementations(Fn.class, Impl.class,
                             MarketDataFn.class, DefaultMarketDataFn.class),
             arguments(function(DefaultMarketDataFn.class, argument("currencyMatrix", new SimpleCurrencyMatrix()))));
  private static final MarketDataMatcher MATCHER1 = MarketDataMatcher.idEquals(SCHEME, VALUE1);
  private static final MarketDataMatcher MATCHER2 = MarketDataMatcher.idEquals(SCHEME, VALUE2);
  private static final FunctionModelConfig DECORATED_CONFIG = DecoratorConfig.decorate(CONFIG, MarketDataShockDecorator.class);
  private static final MarketDataSource MARKET_DATA_SOURCE = mock(MarketDataSource.class);
  private static final Fn FN = FunctionModel.build(Fn.class, DECORATED_CONFIG);
  private static final double DELTA = 1e-8;
  private static final FieldName FIELD_NAME = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);

  static {
    when(MARKET_DATA_SOURCE.get(ID1, FIELD_NAME)).thenReturn((Result) Result.success(1d));
    when(MARKET_DATA_SOURCE.get(ID2, FIELD_NAME)).thenReturn((Result) Result.success(2d));
  }

  /**
   * apply a single shock to one piece of market data but not another
   */
  @SuppressWarnings("unchecked")
  @Test
  public void singleShock() {
    MarketDataShock shock = MarketDataShock.relativeShift(0.5, MATCHER1);
    MarketDataShockDecorator.Shocks shocks = MarketDataShockDecorator.shocks(shock);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(MarketDataShockDecorator.class, shocks);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    assertEquals(1.5, FN.foo(env, ID1).getValue(), DELTA);
    assertEquals(2d, FN.foo(env, ID2).getValue(), DELTA);
  }

  /**
   * apply a shock to one piece of data and a different shock to another
   */
  @Test
  public void multipleSeparateShocks() {
    MarketDataShock relativeShift = MarketDataShock.relativeShift(0.5, MATCHER1);
    MarketDataShock absoluteShift = MarketDataShock.absoluteShift(0.1, MATCHER2);
    MarketDataShockDecorator.Shocks shocks = MarketDataShockDecorator.shocks(absoluteShift, relativeShift);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(MarketDataShockDecorator.class, shocks);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    assertEquals(1.5, FN.foo(env, ID1).getValue(), DELTA);
    assertEquals(2.1, FN.foo(env, ID2).getValue(), DELTA);
  }

  /**
   * apply two shocks to the same piece of data
   */
  @Test
  public void multipleShocksOnSameData() {
    MarketDataShock absoluteShift = MarketDataShock.absoluteShift(0.1, MATCHER1);
    MarketDataShock relativeShift = MarketDataShock.relativeShift(0.5, MATCHER1);
    MarketDataShockDecorator.Shocks shocks = MarketDataShockDecorator.shocks(absoluteShift, relativeShift);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(MarketDataShockDecorator.class, shocks);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    assertEquals(1.65, FN.foo(env, ID1).getValue(), DELTA);
    assertEquals(2d, FN.foo(env, ID2).getValue(), DELTA);
  }


  /**
   * apply two shocks to the same piece of data
   */
  @Test
  public void multipleShocksOnSameDataReversed() {
    MarketDataShock relativeShift = MarketDataShock.relativeShift(0.5, MATCHER1);
    MarketDataShock absoluteShift = MarketDataShock.absoluteShift(0.1, MATCHER1);
    MarketDataShockDecorator.Shocks shocks = MarketDataShockDecorator.shocks(relativeShift, absoluteShift);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(MarketDataShockDecorator.class, shocks);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    assertEquals(1.6, FN.foo(env, ID1).getValue(), DELTA);
    assertEquals(2d, FN.foo(env, ID2).getValue(), DELTA);
  }

  /**
   * try (and fail) to shock a piece of market data that isn't a double
   */
  public void dataNotDouble() {
    MarketDataSource marketDataSource = mock(MarketDataSource.class);
    when(marketDataSource.get(ID1, FIELD_NAME)).thenReturn((Result) Result.success("not a double"));
    when(marketDataSource.get(ID2, FIELD_NAME)).thenReturn((Result) Result.success(2d));
    MarketDataShock shock = MarketDataShock.relativeShift(0.5, MATCHER1);
    MarketDataShockDecorator.Shocks shocks = MarketDataShockDecorator.shocks(shock);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(MarketDataShockDecorator.class, shocks);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), marketDataSource, scenarioArgs);

    assertFalse(FN.foo(env, ID1).isSuccess());
    assertEquals(2d, FN.foo(env, ID2).getValue(), DELTA);
  }

  public interface Fn {

    @Output("Foo")
    Result<Double> foo(Environment env, ExternalIdBundle id);
  }

  public static class Impl implements Fn {

    private final MarketDataFn _marketDataFn;

    public Impl(MarketDataFn marketDataFn) {
      _marketDataFn = marketDataFn;
    }

    @Override
    public Result<Double> foo(Environment env, ExternalIdBundle id) {
      return _marketDataFn.getMarketValue(env, id);
    }
  }
}
