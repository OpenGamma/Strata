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
import static com.opengamma.util.money.Currency.AUD;
import static com.opengamma.util.money.Currency.GBP;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
import com.opengamma.core.security.impl.SimpleSecurityLink;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.example.CashFlowDescriptionFn;
import com.opengamma.sesame.example.CashFlowIdDescriptionFn;
import com.opengamma.sesame.example.DefaultCashFlowDescriptionFn;
import com.opengamma.sesame.example.DefaultEquityDescriptionFn;
import com.opengamma.sesame.example.DefaultIdSchemeFn;
import com.opengamma.sesame.example.EquityDescriptionFn;
import com.opengamma.sesame.example.EquityIdDescriptionFn;
import com.opengamma.sesame.example.MockEquityPresentValue;
import com.opengamma.sesame.example.MockEquityPresentValueFn;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.sesame.marketdata.RecordingMarketDataSource;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

import net.sf.ehcache.CacheManager;

@Test(groups = TestGroup.UNIT)
public class ViewFactoryTest {

  private static final UniqueId EQUITY_TRADE_ID = UniqueId.of("trdId", "321");
  private static final String EQUITY_NAME = "An equity security";

  private static final UniqueId CASH_FLOW_TRADE_ID = UniqueId.of("trdId", "432");
  private static final String CASH_FLOW_NAME = "A cash flow security";

  private static final String DESCRIPTION_HEADER = "Description";
  private static final String PRESENT_VALUE_HEADER = "PV";
  private static final String BLOOMBERG_HEADER = "Bloomberg Ticker";
  private static final String ACTIV_HEADER = "ACTIV Symbol";
  private static final String EQUITY_BLOOMBERG_TICKER = "ACME US Equity";
  private static final String EQUITY_ACTIV_SYMBOL = "ACME.";
  private static final String CASH_FLOW_BLOOMBERG_TICKER = "TEST US Cash Flow";
  private static final String CASH_FLOW_ACTIV_SYMBOL = "CASHFLOW.";

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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(), availableOutputs, new AvailableImplementationsImpl());
    List<Trade> trades = ImmutableList.of(createEquityTrade());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, mockMarketDataSource());
    Results results = view.run(cycleArguments, trades);
    assertEquals(EQUITY_NAME, results.get(0, 0).getResult().getValue());
    System.out.println(results);
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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(), availableOutputs, new AvailableImplementationsImpl());
    List<Security> securities = ImmutableList.of(createEquityTrade().getSecurity());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, mockMarketDataSource());
    Results results = view.run(cycleArguments, securities);
    assertEquals(EQUITY_NAME, results.get(0, 0).getResult().getValue());
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

    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                                              ComponentMap.EMPTY.with(CurrencyMatrix.class, mock(CurrencyMatrix.class)),
                                              availableOutputs,
                                              new AvailableImplementationsImpl(),
                                              FunctionModelConfig.EMPTY,
                                              CacheManager.getInstance(),
                                              FunctionService.NONE);
    Trade trade = createEquityTrade();
    List<Trade> trades = ImmutableList.of(trade);

    ExternalIdBundle securityId = trade.getSecurity().getExternalIdBundle();
    Pair<ExternalIdBundle, FieldName> key = Pairs.of(securityId, FieldName.of(MarketDataRequirementNames.MARKET_VALUE));
    Map<Pair<ExternalIdBundle, FieldName>, Double> marketData = ImmutableMap.of(key, 123.45);
    RecordingMarketDataSource dataSource = new RecordingMarketDataSource(marketData);

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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(), availableOutputs, new AvailableImplementationsImpl());
    List<Trade> trades = ImmutableList.of(createEquityTrade());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, mockMarketDataSource());
    Results results = view.run(cycleArguments, trades);
    assertEquals(EQUITY_NAME, results.get(0, 0).getResult().getValue());
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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               availableImplementations,
                               defaultConfig,
                               CacheManager.getInstance(),
                               EnumSet.noneOf(FunctionService.class));
    List<Trade> trades = ImmutableList.of(createEquityTrade(), createCashFlowTrade());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class, CashFlowSecurity.class);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, mockMarketDataSource());
    Results results = view.run(cycleArguments, trades);

    assertEquals(EQUITY_NAME, results.get(0, 0).getResult().getValue());
    assertEquals(EQUITY_BLOOMBERG_TICKER, results.get(0, 1).getResult().getValue());
    assertEquals(EQUITY_ACTIV_SYMBOL, results.get(0, 2).getResult().getValue());

    assertEquals(CASH_FLOW_NAME, results.get(1, 0).getResult().getValue());
    assertEquals(CASH_FLOW_BLOOMBERG_TICKER, results.get(1, 1).getResult().getValue());
    assertEquals(CASH_FLOW_ACTIV_SYMBOL, results.get(1, 2).getResult().getValue());

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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               new AvailableImplementationsImpl(),
                               FunctionModelConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.of(FunctionService.TRACING));
    List<Trade> trades = ImmutableList.of(createEquityTrade());
    View view = viewFactory.createView(viewConfig, EquitySecurity.class);
    @SuppressWarnings("unchecked")
    Set<Pair<Integer,Integer>> traceCells = Sets.newHashSet(Pairs.of(0, 0));
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataSource(),
                                                       VersionCorrection.LATEST,
                                                       traceCells,
                                                       Collections.<String>emptySet());
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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               availableImplementations,
                               FunctionModelConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.noneOf(FunctionService.class));
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataSource(),
                                                       VersionCorrection.LATEST,
                                                       Collections.<Pair<Integer,Integer>>emptySet(),
                                                       Collections.<String>emptySet());
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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               availableImplementations,
                               FunctionModelConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.noneOf(FunctionService.class));
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataSource(),
                                                       VersionCorrection.LATEST,
                                                       Collections.<Pair<Integer,Integer>>emptySet(),
                                                       Collections.<String>emptySet());
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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               availableImplementations,
                               FunctionModelConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.of(FunctionService.TRACING));
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataSource(),
                                                       VersionCorrection.LATEST,
                                                       Collections.<Pair<Integer,Integer>>emptySet(),
                                                       ImmutableSet.of(name));
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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                                              ComponentMap.EMPTY,
                                              availableOutputs,
                                              availableImplementations,
                                              FunctionModelConfig.EMPTY,
                                              CacheManager.getInstance(),
                                              EnumSet.noneOf(FunctionService.class));
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataSource(),
                                                       VersionCorrection.LATEST,
                                                       Collections.<Pair<Integer,Integer>>emptySet(),
                                                       Collections.<String>emptySet());
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
    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                                              ComponentMap.EMPTY,
                                              availableOutputs,
                                              availableImplementations,
                                              FunctionModelConfig.EMPTY,
                                              CacheManager.getInstance(),
                                              EnumSet.noneOf(FunctionService.class));
    View view = viewFactory.createView(viewConfig);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataSource(),
                                                       VersionCorrection.LATEST,
                                                       Collections.<Pair<Integer,Integer>>emptySet(),
                                                       Collections.<String>emptySet());
    Results results = view.run(cycleArguments);
    ResultItem item = results.get(name);
    assertNotNull(item);
    assertTrue(item.getResult().isSuccess());
    assertEquals("foobarbaz", item.getResult().getValue());
  }

  private static Trade createEquityTrade() {
    EquitySecurity security = new EquitySecurity("exc", "exc", "compName", AUD);
    security.setUniqueId(UniqueId.of("secId", "123"));
    security.setName(EQUITY_NAME);
    security.addExternalId(ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, EQUITY_BLOOMBERG_TICKER));
    security.addExternalId(ExternalId.of(ExternalSchemes.ACTIVFEED_TICKER, EQUITY_ACTIV_SYMBOL));
    SimpleTrade trade = new SimpleTrade();
    SimpleSecurityLink securityLink = new SimpleSecurityLink(ExternalId.of("extId", "123"));
    securityLink.setTarget(security);
    trade.setSecurityLink(securityLink);
    trade.setUniqueId(EQUITY_TRADE_ID);
    return trade;
  }

  private static Trade createCashFlowTrade() {
    CashFlowSecurity security = new CashFlowSecurity(GBP, ZonedDateTime.now(), 12345d);
    security.setUniqueId(UniqueId.of("secId", "234"));
    security.setName(CASH_FLOW_NAME);
    security.addExternalId(ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, CASH_FLOW_BLOOMBERG_TICKER));
    security.addExternalId(ExternalId.of(ExternalSchemes.ACTIVFEED_TICKER, CASH_FLOW_ACTIV_SYMBOL));
    SimpleTrade trade = new SimpleTrade();
    SimpleSecurityLink securityLink = new SimpleSecurityLink(ExternalId.of("extId", "234"));
    securityLink.setTarget(security);
    trade.setSecurityLink(securityLink);
    trade.setUniqueId(CASH_FLOW_TRADE_ID);
    return trade;
  }

  private static MarketDataSource mockMarketDataSource() {
    return mock(MarketDataSource.class);
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
