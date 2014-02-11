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
import static com.opengamma.sesame.config.ConfigBuilder.defaultConfig;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static com.opengamma.sesame.config.ConfigBuilder.viewDef;
import static com.opengamma.util.money.Currency.AUD;
import static com.opengamma.util.money.Currency.GBP;
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
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.mockito.Matchers;
import org.testng.annotations.Test;
import org.threeten.bp.ZoneOffset;
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
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.EquityPresentValue;
import com.opengamma.sesame.EquityPresentValueFn;
import com.opengamma.sesame.ResettableMarketDataFn;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.example.CashFlowDescriptionFn;
import com.opengamma.sesame.example.CashFlowIdDescriptionFn;
import com.opengamma.sesame.example.DefaultCashFlowDescriptionFn;
import com.opengamma.sesame.example.DefaultEquityDescriptionFn;
import com.opengamma.sesame.example.DefaultIdSchemeFn;
import com.opengamma.sesame.example.EquityDescriptionFn;
import com.opengamma.sesame.example.EquityIdDescriptionFn;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.marketdata.DefaultResettableMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.SimpleMarketDataFactory;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

import net.sf.ehcache.CacheManager;

@Test(groups = TestGroup.UNIT)
public class EngineTest {

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
    ViewDef viewDef =
        viewDef("Trivial Test View",
                column(DESCRIPTION_HEADER,
                       output(OutputNames.DESCRIPTION, EquitySecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, DefaultEquityDescriptionFn.class)))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    Engine engine = new Engine(new DirectExecutorService(), availableOutputs, new AvailableImplementationsImpl());
    List<Trade> trades = ImmutableList.of(createEquityTrade());
    View view = engine.createView(viewDef, trades);
    Results results = view.run(new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, mockMarketDataFactory()));
    assertEquals(EQUITY_NAME, results.get(0, 0).getResult().getValue());
    System.out.println(results);
  }

  @Test
  public void basicFunctionWithSecurity() {
    ViewDef viewDef =
        viewDef("Trivial Test View",
                column(DESCRIPTION_HEADER,
                       output(OutputNames.DESCRIPTION, EquitySecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, DefaultEquityDescriptionFn.class)))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    Engine engine = new Engine(new DirectExecutorService(), availableOutputs, new AvailableImplementationsImpl());
    List<Security> securities = ImmutableList.of(createEquityTrade().getSecurity());
    View view = engine.createView(viewDef, securities);
    Results results = view.run(new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, mockMarketDataFactory()));
    assertEquals(EQUITY_NAME, results.get(0, 0).getResult().getValue());
    System.out.println(results);
  }

  @Test
  public void simpleFunctionWithMarketData() {
    ViewDef viewDef =
        viewDef("Equity PV",
                column(PRESENT_VALUE_HEADER,
                       defaultConfig(OutputNames.PRESENT_VALUE,
                                     config(
                                         implementations(EquityPresentValueFn.class, EquityPresentValue.class)))));

    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityPresentValueFn.class);

    Engine engine = new Engine(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               new AvailableImplementationsImpl(),
                               FunctionConfig.EMPTY,
                               CacheManager.getInstance(),
                               EngineService.DEFAULT_SERVICES);
    Trade trade = createEquityTrade();
    List<Trade> trades = ImmutableList.of(trade);

    // todo - we shouldn't be casting here
    MarketDataRequirement requirement = MarketDataRequirementFactory.of((FinancialSecurity) trade.getSecurity(),
                                                                        MarketDataRequirementNames.MARKET_VALUE);
    MarketDataItem item = MarketDataItem.available(123.45);
    Map<MarketDataRequirement, MarketDataItem> marketData = ImmutableMap.of(requirement, item);
    ResettableMarketDataFn marketDataProvider = new DefaultResettableMarketDataFn();

    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    marketDataProvider.resetMarketData(valuationTime, marketData);
    MarketDataFactory marketDataFactory = new SimpleMarketDataFactory(marketDataProvider);

    View view = engine.createView(viewDef, trades);
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, marketDataFactory);
    Results results = view.run(cycleArguments);
    assertEquals(123.45, results.get(0, 0).getResult().getValue());
    System.out.println(results);
  }

  @Test
  public void defaultColumnOutput() {
    ViewDef viewDef =
        viewDef("Trivial Test View",
                column(DESCRIPTION_HEADER,
                       defaultConfig(OutputNames.DESCRIPTION,
                                     config(
                                         implementations(EquityDescriptionFn.class, DefaultEquityDescriptionFn.class)))));

    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    Engine engine = new Engine(new DirectExecutorService(), availableOutputs, new AvailableImplementationsImpl());
    List<Trade> trades = ImmutableList.of(createEquityTrade());
    View view = engine.createView(viewDef, trades);
    Results results = view.run(new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, mockMarketDataFactory()));
    assertEquals(EQUITY_NAME, results.get(0, 0).getResult().getValue());
    System.out.println(results);
  }

  @Test
  public void overridesAndConfig() {
    ViewDef viewDef =
        viewDef("name",
                column(OutputNames.DESCRIPTION),
                column(BLOOMBERG_HEADER,
                       defaultConfig(OutputNames.DESCRIPTION,
                                     config(
                                         arguments(
                                             function(DefaultIdSchemeFn.class,
                                                      argument("scheme", ExternalSchemes.BLOOMBERG_TICKER))))),
                       output(EquitySecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, EquityIdDescriptionFn.class))),
                       output(CashFlowSecurity.class,
                              config(
                                  implementations(CashFlowDescriptionFn.class, CashFlowIdDescriptionFn.class)))),
                column(ACTIV_HEADER,
                       defaultConfig(OutputNames.DESCRIPTION,
                                     config(
                                         arguments(
                                             function(DefaultIdSchemeFn.class,
                                                      argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                       output(EquitySecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, EquityIdDescriptionFn.class))),
                       output(CashFlowSecurity.class,
                              config(
                                  implementations(CashFlowDescriptionFn.class, CashFlowIdDescriptionFn.class)))));

    FunctionConfig defaultConfig = config(implementations(EquityDescriptionFn.class, DefaultEquityDescriptionFn.class,
                                                          CashFlowDescriptionFn.class, DefaultCashFlowDescriptionFn.class));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class, CashFlowDescriptionFn.class);
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(DefaultIdSchemeFn.class);
    Engine engine = new Engine(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               availableImplementations,
                               defaultConfig,
                               CacheManager.getInstance(),
                               EnumSet.noneOf(EngineService.class));
    List<Trade> trades = ImmutableList.of(createEquityTrade(), createCashFlowTrade());
    View view = engine.createView(viewDef, trades);
    Results results = view.run(new CycleArguments(ZonedDateTime.now(), VersionCorrection.LATEST, mockMarketDataFactory()));

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
    ViewDef viewDef =
        viewDef("Trivial Test View",
                column(DESCRIPTION_HEADER,
                       output(OutputNames.DESCRIPTION, EquitySecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, DefaultEquityDescriptionFn.class)))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityDescriptionFn.class);
    Engine engine = new Engine(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               new AvailableImplementationsImpl(),
                               FunctionConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.of(EngineService.TRACING));
    List<Trade> trades = ImmutableList.of(createEquityTrade());
    View view = engine.createView(viewDef, trades);
    @SuppressWarnings("unchecked")
    Set<Pair<Integer,Integer>> traceCells = Sets.newHashSet(Pairs.of(0, 0));
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataFactory(),
                                                       VersionCorrection.LATEST,
                                                       traceCells,
                                                       Collections.<String>emptySet());
    Results results = view.run(cycleArguments);
    CallGraph callGraph = results.get(0, 0).getCallGraph();
    assertNotNull(callGraph);
    System.out.println(callGraph.prettyPrint());
  }

  @Test
  public void nonPortfolioOutputWithNoArgs() {
    String name = "the unique output name";
    ViewDef viewDef =
        viewDef("Non portfolio output with no args",
                nonPortfolioOutput(name, output("Foo")));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(NonPortfolioFunctionWithNoArgs.class);
    AvailableImplementationsImpl availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(NonPortfolioFunctionWithNoArgsImpl.class);
    Engine engine = new Engine(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               availableImplementations,
                               FunctionConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.noneOf(EngineService.class));
    View view = engine.createView(viewDef, Collections.emptyList());
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataFactory(),
                                                       VersionCorrection.LATEST,
                                                       Collections.<Pair<Integer,Integer>>emptySet(),
                                                       Collections.<String>emptySet());
    Results results = view.run(cycleArguments);
    ResultItem item = results.get(name);
    assertNotNull(item);
    assertTrue(item.getResult().isValueAvailable());
    assertEquals("foo", item.getResult().getValue());
  }

  @Test
  public void nonPortfolioOutputWithArgs() {
    String name = "the unique output name";
    ViewDef viewDef =
        viewDef("Non portfolio output with args",
                nonPortfolioOutput(name,
                                   output("Foo",
                                          config(
                                              arguments(
                                                  function(NonPortfolioFunctionWithArgsImpl.class,
                                                           argument("notTheTarget", "bar")))))));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(NonPortfolioFunctionWithArgs.class);
    AvailableImplementationsImpl availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(NonPortfolioFunctionWithArgsImpl.class);
    Engine engine = new Engine(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               availableImplementations,
                               FunctionConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.noneOf(EngineService.class));
    View view = engine.createView(viewDef, Collections.emptyList());
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataFactory(),
                                                       VersionCorrection.LATEST,
                                                       Collections.<Pair<Integer,Integer>>emptySet(),
                                                       Collections.<String>emptySet());
    Results results = view.run(cycleArguments);
    ResultItem item = results.get(name);
    assertNotNull(item);
    assertTrue(item.getResult().isValueAvailable());
    assertEquals("foobar", item.getResult().getValue());
  }

  @Test
  public void nonPortfolioOutputsCallTracing() {
    String name = "the unique output name";
    ViewDef viewDef =
        viewDef("Non portfolio output with no args",
                nonPortfolioOutput(name, output("Foo")));
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(NonPortfolioFunctionWithNoArgs.class);
    AvailableImplementationsImpl availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(NonPortfolioFunctionWithNoArgsImpl.class);
    Engine engine = new Engine(new DirectExecutorService(),
                               ComponentMap.EMPTY,
                               availableOutputs,
                               availableImplementations,
                               FunctionConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.of(EngineService.TRACING));
    View view = engine.createView(viewDef, Collections.emptyList());
    CycleArguments cycleArguments = new CycleArguments(ZonedDateTime.now(),
                                                       mockMarketDataFactory(),
                                                       VersionCorrection.LATEST,
                                                       Collections.<Pair<Integer,Integer>>emptySet(),
                                                       ImmutableSet.of(name));
    Results results = view.run(cycleArguments);
    ResultItem item = results.get(name);
    assertNotNull(item);
    assertNotNull(item.getCallGraph());
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

  private static MarketDataFactory mockMarketDataFactory() {
    MarketDataFactory factory = mock(MarketDataFactory.class);
    MarketDataFn provider = mock(MarketDataFn.class);
    when(factory.create(Matchers.<ComponentMap>any())).thenReturn(provider);
    return factory;
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

    // TODO annotating the params is a workaround for [SSM-107]
    @Output("Foo")
    String foo(@Named("notTheTarget") String notTheTarget);
  }

  public static class NonPortfolioFunctionWithArgsImpl implements NonPortfolioFunctionWithArgs {

    @Override
    public String foo(String notTheTarget) {
      return "foo" + notTheTarget;
    }
  }
}
