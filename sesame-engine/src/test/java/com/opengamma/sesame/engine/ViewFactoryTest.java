/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authz.AuthorizationException;
import org.testng.annotations.Test;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.position.Trade;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.security.Security;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.security.impl.SimpleSecurityLink;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.VersionCorrection;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.EngineTestUtils;
import com.opengamma.sesame.LazyLinkedPositionOrTrade;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.config.EmptyFunctionArguments;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.example.CashFlowDescriptionFn;
import com.opengamma.sesame.example.CashFlowIdDescriptionFn;
import com.opengamma.sesame.example.DefaultCashFlowDescriptionFn;
import com.opengamma.sesame.example.DefaultEquityDescriptionFn;
import com.opengamma.sesame.example.DefaultIdSchemeFn;
import com.opengamma.sesame.example.EquityDescriptionFn;
import com.opengamma.sesame.example.EquityIdDescriptionFn;
import com.opengamma.sesame.example.IdSchemeFn;
import com.opengamma.sesame.example.MockEquityPresentValue;
import com.opengamma.sesame.example.MockEquityPresentValueFn;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.MapMarketDataSource;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

@Test(groups = TestGroup.UNIT)
public class ViewFactoryTest {

  private static final String DESCRIPTION_HEADER = "Description";
  private static final String PRESENT_VALUE_HEADER = "PV";
  private static final String BLOOMBERG_HEADER = "Bloomberg Ticker";
  private static final String ACTIV_HEADER = "ACTIV Symbol";

  @Test
  public void basicFunctionWithTrade() {
    ViewConfig viewConfig =
        configureView("Trivial Test View",
                      column(DESCRIPTION_HEADER,
                             output(OutputNames.DESCRIPTION, EquitySecurity.class,
                                    config(
                                        implementations(EquityDescriptionFn.class,
                                                        DefaultEquityDescriptionFn.class)))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    ViewFactory viewFactory = createViewFactory(availableOutputs);
    List<Trade> trades = ImmutableList.of(EngineTestUtils.createEquityTrade());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class));
    Results results = view.run(cycleArguments, trades);
    assertEquals(EngineTestUtils.EQUITY_NAME, results.get(0, 0).getResult().getValue());
    System.out.println(results);
  }

  @Test
  public void basicFunctionWithTradeAndNoSecurity() {
    ViewConfig viewConfig =
        configureView("Trivial Test View",
                      column(DESCRIPTION_HEADER,
                             output(OutputNames.DESCRIPTION, EquitySecurity.class,
                                    config(
                                        implementations(EquityDescriptionFn.class,
                                                        DefaultEquityDescriptionFn.class)))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    ViewFactory viewFactory = createViewFactory(availableOutputs);
    SimpleTrade trade = (SimpleTrade) EngineTestUtils.createEquityTrade();
    trade.setSecurityLink(new SimpleSecurityLink(trade.getSecurityLink().getExternalId()));
    List<Trade> trades = ImmutableList.<Trade>of(trade);
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class));
    Results results = view.run(cycleArguments, trades);
    assertEquals(FailureStatus.INVALID_INPUT, results.get(0, 0).getResult().getStatus());
    assertEquals(true, results.get(0, 0).getResult().getFailureMessage().contains("PositionOrTrade.getSecurity() returned null"));
  }

  @Test
  public void basicFunctionWithTradeAndUnknownSecurityType() {
    ViewConfig viewConfig =
        configureView("Trivial Test View",
                      column(DESCRIPTION_HEADER,
                             output(OutputNames.DESCRIPTION, EquitySecurity.class,
                                    config(
                                        implementations(EquityDescriptionFn.class,
                                                        DefaultEquityDescriptionFn.class)))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    ViewFactory viewFactory = createViewFactory(availableOutputs);
    Trade trade = (SimpleTrade) EngineTestUtils.createCashFlowTrade();
    List<Trade> trades = ImmutableList.of(trade);
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class));
    Results results = view.run(cycleArguments, trades);
    assertEquals(FailureStatus.INVALID_INPUT, results.get(0, 0).getResult().getStatus());
    assertEquals(true, results.get(0, 0).getResult().getFailureMessage().contains("No function found for security"));
  }

  private ViewFactory createViewFactory(AvailableOutputs availableOutputs) {
    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    return new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                           availableOutputs,
                           new AvailableImplementationsImpl(),
                           FunctionModelConfig.EMPTY,
                           FunctionService.DEFAULT_SERVICES,
                           cachingManager);
  }

  @Test
  public void basicFunctionWithSecurity() {
    ViewConfig viewConfig =
        configureView("Trivial Test View",
                      column(DESCRIPTION_HEADER,
                             output(OutputNames.DESCRIPTION, EquitySecurity.class,
                                    config(
                                        implementations(EquityDescriptionFn.class,
                                                        DefaultEquityDescriptionFn.class)))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    ViewFactory viewFactory = createViewFactory(availableOutputs);
    List<Security> securities = ImmutableList.of(EngineTestUtils.createEquityTrade().getSecurity());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class));
    Results results = view.run(cycleArguments, securities);
    assertEquals(EngineTestUtils.EQUITY_NAME, results.get(0, 0).getResult().getValue());
    System.out.println(results);
  }

  @Test
  public void simpleFunctionWithMarketData() {
    ViewConfig viewConfig =
        configureView("Equity PV",
                      column(PRESENT_VALUE_HEADER, OutputNames.PRESENT_VALUE,
                             config(
                                 implementations(MockEquityPresentValueFn.class, MockEquityPresentValue.class,
                                                 MarketDataFn.class, DefaultMarketDataFn.class))));

    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(MockEquityPresentValueFn.class);


    CachingManager cachingManager = new NoOpCachingManager(
        ComponentMap.EMPTY.with(CurrencyMatrix.class, mock(CurrencyMatrix.class)));
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              new AvailableImplementationsImpl(),
                                              FunctionModelConfig.EMPTY,
                                              FunctionService.NONE,
                                              cachingManager);
    Trade trade = EngineTestUtils.createEquityTrade();
    List<Trade> trades = ImmutableList.of(trade);

    ExternalIdBundle securityId = trade.getSecurity().getExternalIdBundle();
    MarketDataSource dataSource = MapMarketDataSource.of(securityId, 123.45);

    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, dataSource);
    Results results = view.run(cycleArguments, trades);
    assertEquals(123.45, results.get(0, 0).getResult().getValue());
    System.out.println(results);
  }

  @Test
  public void defaultColumnOutput() {
    ViewConfig viewConfig =
        configureView("Trivial Test View",
                      column(DESCRIPTION_HEADER, OutputNames.DESCRIPTION,
                             config(
                                 implementations(EquityDescriptionFn.class,
                                                 DefaultEquityDescriptionFn.class))));

    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    ViewFactory viewFactory = createViewFactory(availableOutputs);
    List<Trade> trades = ImmutableList.of(EngineTestUtils.createEquityTrade());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class));
    Results results = view.run(cycleArguments, trades);
    assertEquals(EngineTestUtils.EQUITY_NAME, results.get(0, 0).getResult().getValue());
    System.out.println(results);
  }

  @Test
  public void overridesAndConfig() {
    ViewConfig viewConfig =
        configureView("name",
                      column(OutputNames.DESCRIPTION),
                      column(BLOOMBERG_HEADER, OutputNames.DESCRIPTION,
                             config(
                                 arguments(
                                     function(DefaultIdSchemeFn.class,
                                              argument("scheme", ExternalSchemes.BLOOMBERG_TICKER)))),
                             output(EquitySecurity.class,
                                    config(
                                        implementations(EquityDescriptionFn.class,
                                                        EquityIdDescriptionFn.class))),
                             output(CashFlowSecurity.class,
                                    config(
                                        implementations(CashFlowDescriptionFn.class,
                                                        CashFlowIdDescriptionFn.class)))),
                      column(ACTIV_HEADER, OutputNames.DESCRIPTION,
                             config(
                                 arguments(
                                     function(DefaultIdSchemeFn.class,
                                              argument("scheme", ExternalSchemes.ACTIVFEED_TICKER)))),
                             output(EquitySecurity.class,
                                    config(
                                        implementations(EquityDescriptionFn.class,
                                                        EquityIdDescriptionFn.class))),
                             output(CashFlowSecurity.class,
                                    config(
                                        implementations(CashFlowDescriptionFn.class,
                                                        CashFlowIdDescriptionFn.class)))));

    FunctionModelConfig defaultConfig = config(implementations(EquityDescriptionFn.class, DefaultEquityDescriptionFn.class,
                                                          CashFlowDescriptionFn.class, DefaultCashFlowDescriptionFn.class));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class, CashFlowDescriptionFn.class);
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(DefaultIdSchemeFn.class);
    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              availableImplementations,
                                              defaultConfig,
                                              EnumSet.noneOf(FunctionService.class),
                                              cachingManager);
    List<Trade> trades = ImmutableList.of(EngineTestUtils.createEquityTrade(), EngineTestUtils.createCashFlowTrade());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class, CashFlowSecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class));
    Results results = view.run(cycleArguments, trades);

    assertEquals(EngineTestUtils.EQUITY_NAME, results.get(0, 0).getResult().getValue());
    assertEquals(EngineTestUtils.EQUITY_BLOOMBERG_TICKER, results.get(0, 1).getResult().getValue());
    assertEquals(EngineTestUtils.EQUITY_ACTIV_SYMBOL, results.get(0, 2).getResult().getValue());

    assertEquals(EngineTestUtils.CASH_FLOW_NAME, results.get(1, 0).getResult().getValue());
    assertEquals(EngineTestUtils.CASH_FLOW_BLOOMBERG_TICKER, results.get(1, 1).getResult().getValue());
    assertEquals(EngineTestUtils.CASH_FLOW_ACTIV_SYMBOL, results.get(1, 2).getResult().getValue());

    System.out.println(results);
  }

  @Test
  public void portfolioOutputsCallTracing() {
    ViewConfig viewConfig =
        configureView("Trivial Test View",
                      column(DESCRIPTION_HEADER,
                             output(OutputNames.DESCRIPTION, EquitySecurity.class,
                                    config(
                                        implementations(EquityDescriptionFn.class,
                                                        DefaultEquityDescriptionFn.class)))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              new AvailableImplementationsImpl(),
                                              FunctionModelConfig.EMPTY,
                                              EnumSet.of(FunctionService.TRACING),
                                              cachingManager);
    List<Trade> trades = ImmutableList.of(EngineTestUtils.createEquityTrade());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    @SuppressWarnings("unchecked")
    Set<Pair<Integer,Integer>> traceCells = Sets.newHashSet(Pairs.of(0, 0));
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class),
                                                       EmptyFunctionArguments.INSTANCE,
                                                       Collections.<Class<?>, Object>emptyMap(),
                                                       traceCells,
                                                       Collections.<String>emptySet(),
                                                       false);
    Results results = view.run(cycleArguments, trades);
    CallGraph trace = results.get(0, 0).getCallGraph();
    assertNotNull(trace);
    System.out.println(trace.prettyPrint());
  }

  @Test
  public void nonPortfolioOutputWithNoArgs() {
    String name = "the unique output name";
    ViewConfig viewConfig =
        configureView("Non portfolio output with no args",
                      nonPortfolioOutput(name, output("Foo")));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(NonPortfolioFunctionWithNoArgs.class);
    AvailableImplementationsImpl availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(NonPortfolioFunctionWithNoArgsImpl.class);
    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              availableImplementations,
                                              FunctionModelConfig.EMPTY,
                                              EnumSet.noneOf(FunctionService.class),
                                              cachingManager);
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class));
    Results results = view.run(cycleArguments);
    ResultItem item = results.get(name);
    assertNotNull(item);
    assertTrue(item.getResult().isSuccess());
    assertEquals("foo", item.getResult().getValue());
  }

  @Test
  public void nonPortfolioOutputWithArgs() {
    String name = "the unique output name";
    ViewConfig viewConfig =
        configureView("Non portfolio output with args",
                      nonPortfolioOutput(name,
                                         output("Foo",
                                                config(
                                                    arguments(
                                                        function(NonPortfolioFunctionWithArgsImpl.class,
                                                                 argument("notTheTarget1", "bar"),
                                                                 argument("notTheTarget2", "baz")))))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(NonPortfolioFunctionWithArgs.class);
    AvailableImplementationsImpl availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(NonPortfolioFunctionWithArgsImpl.class);
    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              availableImplementations,
                                              FunctionModelConfig.EMPTY,
                                              EnumSet.noneOf(FunctionService.class),
                                              cachingManager);
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class));
    Results results = view.run(cycleArguments);
    ResultItem item = results.get(name);
    assertNotNull(item);
    assertTrue(item.getResult().isSuccess());
    assertEquals("foobarbaz", item.getResult().getValue());
  }

  @Test
  public void nonPortfolioOutputsCallTracing() {
    String name = "the unique output name";
    ViewConfig viewConfig =
        configureView("Non portfolio output with no args",
                      nonPortfolioOutput(name, output("Foo")));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(NonPortfolioFunctionWithNoArgs.class);
    AvailableImplementationsImpl availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(NonPortfolioFunctionWithNoArgsImpl.class);
    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              availableImplementations,
                                              FunctionModelConfig.EMPTY,
                                              EnumSet.of(FunctionService.TRACING),
                                              cachingManager);
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class),
                                                       EmptyFunctionArguments.INSTANCE,
                                                       Collections.<Class<?>, Object>emptyMap(),
                                                       Collections.<Pair<Integer,Integer>>emptySet(),
                                                       ImmutableSet.of(name),
                                                       false);
    Results results = view.run(cycleArguments);
    ResultItem item = results.get(name);
    assertNotNull(item);
    assertNotNull(item.getCallGraph());
  }

  @Test
  public void methodArgsKeyedByInterface() {
    String name = "the unique output name";
    ViewConfig viewConfig =
        configureView("Non portfolio output with args",
                      nonPortfolioOutput(name,
                                         output("Foo",
                                                config(
                                                    arguments(
                                                        function(NonPortfolioFunctionWithArgs.class,
                                                                 argument("notTheTarget1", "bar"),
                                                                 argument("notTheTarget2", "baz")))))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(NonPortfolioFunctionWithArgs.class);
    AvailableImplementationsImpl availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(NonPortfolioFunctionWithArgsImpl.class);
    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              availableImplementations,
                                              FunctionModelConfig.EMPTY,
                                              EnumSet.noneOf(FunctionService.class),
                                              cachingManager);
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST,
                                                       mock(MarketDataSource.class));
    Results results = view.run(cycleArguments);
    ResultItem item = results.get(name);
    assertNotNull(item);
    assertTrue(item.getResult().isSuccess());
    assertEquals("foobarbaz", item.getResult().getValue());
  }

  @Test
  public void methodArgsKeyedByBoth() {
    String name = "the unique output name";
    ViewConfig viewConfig =
        configureView("Non portfolio output with args",
                      nonPortfolioOutput(name,
                                         output("Foo",
                                                config(
                                                    arguments(
                                                        function(NonPortfolioFunctionWithArgsImpl.class,
                                                                 argument("notTheTarget1", "bar")),
                                                        function(NonPortfolioFunctionWithArgs.class,
                                                                 argument("notTheTarget2", "baz")))))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(NonPortfolioFunctionWithArgs.class);
    AvailableImplementationsImpl availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(NonPortfolioFunctionWithArgsImpl.class);
    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              availableImplementations,
                                              FunctionModelConfig.EMPTY,
                                              EnumSet.noneOf(FunctionService.class),
                                              cachingManager);
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, mock(MarketDataSource.class));
    Results results = view.run(cycleArguments);
    ResultItem item = results.get(name);
    assertNotNull(item);
    assertTrue(item.getResult().isSuccess());
    assertEquals("foobarbaz", item.getResult().getValue());
  }

  /**
   * checks that authorization failures during security resolution cause the results to be failures with a status
   * of PERMISSION_DENIED
   */
  @Test
  public void insufficientPermissionsToViewSecurity() {
    ViewConfig viewConfig =
        configureView("name",
                      column(OutputNames.DESCRIPTION,
                             config(
                                 implementations(EquityDescriptionFn.class, DefaultEquityDescriptionFn.class,
                                                 CashFlowDescriptionFn.class, DefaultCashFlowDescriptionFn.class))),
                      column(BLOOMBERG_HEADER, OutputNames.DESCRIPTION,
                             config(
                                 implementations(IdSchemeFn.class, DefaultIdSchemeFn.class),
                                 arguments(
                                     function(DefaultIdSchemeFn.class,
                                              argument("scheme", ExternalSchemes.BLOOMBERG_TICKER)))),
                             output(EquitySecurity.class,
                                    config(
                                        implementations(EquityDescriptionFn.class, EquityIdDescriptionFn.class))),
                             output(CashFlowSecurity.class,
                                    config(
                                        implementations(CashFlowDescriptionFn.class, CashFlowIdDescriptionFn.class)))));

    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class, CashFlowDescriptionFn.class);
    CachingManager cachingManager = new NoOpCachingManager(ComponentMap.EMPTY);
    ViewFactory viewFactory = new ViewFactory(new EngineTestUtils.DirectExecutorService(),
                                              availableOutputs,
                                              new AvailableImplementationsImpl(),
                                              FunctionModelConfig.EMPTY,
                                              EnumSet.noneOf(FunctionService.class),
                                              cachingManager);
    Trade equityTrade = EngineTestUtils.createEquityTrade();
    Trade cashFlowTrade = EngineTestUtils.createCashFlowTrade();

    Security equitySecurity = equityTrade.getSecurity();
    Security cashFlowSecurity = cashFlowTrade.getSecurity();

    ExternalIdBundle equityId = equitySecurity.getExternalIdBundle();
    ExternalIdBundle cashFlowId = cashFlowSecurity.getExternalIdBundle();

    List<LazyLinkedPositionOrTrade> trades = ImmutableList.of(new LazyLinkedPositionOrTrade(equityTrade),
                                                              new LazyLinkedPositionOrTrade(cashFlowTrade));
    ZonedDateTime now = ZonedDateTime.now();
    VersionCorrection versionCorrection = VersionCorrection.of(now.toInstant(), null);
    SecuritySource securitySource = mock(SecuritySource.class);
    when(securitySource.getSingle(equityId, versionCorrection)).thenReturn(equitySecurity);
    when(securitySource.getSingle(cashFlowId, versionCorrection)).thenThrow(new AuthorizationException());

    FixedInstantVersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(now.toInstant());
    Map<Class<?>, Object> services = ImmutableMap.of(SecuritySource.class, securitySource,
                                                     VersionCorrectionProvider.class, vcProvider);
    ServiceContext serviceContext = ServiceContext.of(services);
    ThreadLocalServiceContext.init(serviceContext);

    View view = viewFactory.createView(viewConfig, EquitySecurity.class, CashFlowSecurity.class);
    CycleArguments cycleArguments = new CycleArguments(now, VersionCorrection.LATEST, mock(MarketDataSource.class));
    Results results = view.run(cycleArguments, trades);

    // equity results should be ok as the user has permission to see the security
    assertEquals(EngineTestUtils.EQUITY_NAME, results.get(0, 0).getResult().getValue());
    assertEquals(EngineTestUtils.EQUITY_BLOOMBERG_TICKER, results.get(0, 1).getResult().getValue());

    // cash flow results should all be failures with 'permission denied' messages
    Result<?> result1 = results.get(1, 0).getResult();
    Result<?> result2 = results.get(1, 1).getResult();
    assertEquals(FailureStatus.PERMISSION_DENIED, result1.getStatus());
    assertEquals(FailureStatus.PERMISSION_DENIED, result2.getStatus());
    assertTrue(result1.getFailureMessage().startsWith("Permission Denied"));
    assertTrue(result2.getFailureMessage().startsWith("Permission Denied"));
  }

  public interface NonPortfolioFunctionWithNoArgs {

    @Output("Foo")
    String foo();
  }

  public static class NonPortfolioFunctionWithNoArgsImpl implements NonPortfolioFunctionWithNoArgs {

    @Override
    public String foo() {
      return "foo";
    }
  }

  public interface NonPortfolioFunctionWithArgs {

    @Output("Foo")
    String foo(String notTheTarget1, String notTheTarget2);
  }

  public static class NonPortfolioFunctionWithArgsImpl implements NonPortfolioFunctionWithArgs {

    @Override
    public String foo(String notTheTarget1, String notTheTarget2) {
      return "foo" + notTheTarget1 + notTheTarget2;
    }
  }
}
