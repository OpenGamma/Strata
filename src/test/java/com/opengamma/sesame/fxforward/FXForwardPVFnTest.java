/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.sesame.FailureStatus.MISSING_DATA;
import static com.opengamma.sesame.SuccessStatus.SUCCESS;
import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static com.opengamma.sesame.config.ConfigBuilder.viewDef;
import static com.opengamma.util.money.Currency.EUR;
import static com.opengamma.util.money.Currency.GBP;
import static com.opengamma.util.money.Currency.JPY;
import static com.opengamma.util.money.Currency.USD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.hamcrest.MatcherAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.position.Trade;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.security.impl.SimpleSecurityLink;
import com.opengamma.financial.analytics.CurrencyLabelledMatrix1D;
import com.opengamma.financial.analytics.conversion.FXForwardSecurityConverter;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.analytics.curve.exposure.InstrumentExposuresProvider;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.master.historicaltimeseries.impl.RemoteHistoricalTimeSeriesResolver;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.CurrencyPairsFn;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DefaultValuationTimeFn;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.FunctionResult;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.sesame.ResultStatus;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.sesame.cache.CachingProxyDecorator;
import com.opengamma.sesame.cache.ExecutingMethodsThreadLocal;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.CycleArguments;
import com.opengamma.sesame.engine.Engine;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.graph.CompositeNodeDecorator;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.marketdata.CurveNodeMarketDataRequirement;
import com.opengamma.sesame.marketdata.DefaultResettableMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.SimpleMarketDataFactory;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ehcache.EHCacheUtils;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;

import net.sf.ehcache.CacheManager;

@Test(groups = TestGroup.UNIT)
public class FXForwardPVFnTest {

  private static final Logger s_logger = LoggerFactory.getLogger(FXForwardPVFnTest.class);

  private static final AtomicLong s_nextId = new AtomicLong(0);
  
  private CacheManager _cacheManager;

  @BeforeClass
  public void setUpClass() {
    _cacheManager = EHCacheUtils.createTestCacheManager(getClass());
  }

  @Test
  public void buildGraph() {
    FunctionMetadata calculatePV = ConfigUtils.createMetadata(FXForwardPVFn.class, "calculatePV");
    FunctionConfig config = createFunctionConfig();
    ComponentMap componentMap = componentMap(ConfigSource.class,
                                             ConventionSource.class,
                                             ConventionBundleSource.class,
                                             HistoricalTimeSeriesResolver.class,
                                             SecuritySource.class,
                                             HolidaySource.class,
                                             HistoricalTimeSeriesSource.class,
                                             MarketDataFn.class,
                                             RegionSource.class,
                                             ValuationTimeFn.class);
    GraphConfig graphConfig = new GraphConfig(config, componentMap, NodeDecorator.IDENTITY);
    FunctionModel functionModel = FunctionModel.forFunction(calculatePV, graphConfig);
    Object fn = functionModel.build(new FunctionBuilder(), componentMap).getReceiver();
    assertTrue(fn instanceof FXForwardPVFn);
    System.out.println(functionModel.prettyPrint(true));
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithNoData() throws IOException {
    FunctionResult<CurrencyLabelledMatrix1D> pv = executeAgainstRemoteServer(
        Collections.<MarketDataRequirement, MarketDataItem>emptyMap());
    assertNotNull(pv);
    MatcherAssert.assertThat(pv.getStatus(), is((ResultStatus) MISSING_DATA));
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithData() throws IOException {
    FunctionResult<CurrencyLabelledMatrix1D> pv = executeAgainstRemoteServer(loadMarketDataForForward());
    assertNotNull(pv);
    assertThat(pv.getStatus(), is((ResultStatus) SUCCESS));
  }

  private FunctionResult<CurrencyLabelledMatrix1D> executeAgainstRemoteServer(
      Map<MarketDataRequirement, MarketDataItem> marketData) {
    String serverUrl = "http://localhost:8080";
    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    DefaultResettableMarketDataFn marketDataFn = new DefaultResettableMarketDataFn();
    marketDataFn.resetMarketData(valuationTime, marketData);
    Map<Class<?>, Object> comps = ImmutableMap.of(HistoricalTimeSeriesResolver.class, htsResolver,
                                                  // TODO these 2 are normally provided by the engine. SSM-59
                                                  MarketDataFn.class, marketDataFn,
                                                  ValuationTimeFn.class, new DefaultValuationTimeFn(valuationTime));
    ComponentMap componentMap = ComponentMap.loadComponents(serverUrl).with(comps);
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    CompositeNodeDecorator decorator = new CompositeNodeDecorator(TimingProxy.INSTANCE, cachingDecorator);
    GraphConfig graphConfig = new GraphConfig(createFunctionConfig(), componentMap, decorator);
    //GraphConfig graphConfig = new GraphConfig(createFunctionConfig(), componentMap, TimingProxy.INSTANCE);
    //GraphConfig graphConfig = new GraphConfig(createFunctionConfig(), componentMap, NodeDecorator.IDENTITY);
    FXForwardPVFn pvFunction = FunctionModel.build(FXForwardPVFn.class, graphConfig);
    ExternalId regionId = ExternalId.of(ExternalSchemes.FINANCIAL, "US");
    ZonedDateTime forwardDate = ZonedDateTime.of(2014, 11, 7, 12, 0, 0, 0, ZoneOffset.UTC);
    FXForwardSecurity security = new FXForwardSecurity(EUR, 10_000_000, USD, 14_000_000, forwardDate, regionId);
    security.setUniqueId(UniqueId.of("sec", "123"));
    //TracingProxy.start(new FullTracer());
    FunctionResult<CurrencyLabelledMatrix1D> result = null;
    for (int i = 0; i < 100; i++) {
      result = pvFunction.calculatePV(security);
      System.out.println();
    }
    //System.out.println(TracingProxy.end().prettyPrint());
    logMarketData(marketDataFn.getCollectedRequests());
    return result;
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeYieldCurveAgainstRemoteServer() throws IOException {

    //String serverUrl = "http://devsvr-lx-2:8080";
    String serverUrl = "http://localhost:8080";
    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    DefaultResettableMarketDataFn marketDataFn = new DefaultResettableMarketDataFn();
    Map<MarketDataRequirement, MarketDataItem> marketData = loadMarketDataForYieldCurve();
    addValue(marketData, MarketDataRequirementFactory.of(CurrencyPair.of(USD, JPY)), 98.86);
    marketDataFn.resetMarketData(valuationTime, marketData);
    Map<Class<?>, Object> comps = ImmutableMap.of(HistoricalTimeSeriesResolver.class, htsResolver,
                                                  // TODO this is provided by the engine. SSM-59
                                                  // how should it be handled when not using the engine?
                                                  MarketDataFn.class, marketDataFn,
                                                  ValuationTimeFn.class, new DefaultValuationTimeFn(valuationTime));
    ComponentMap componentMap = ComponentMap.loadComponents(serverUrl).with(comps);

    GraphConfig graphConfig = new GraphConfig(createFunctionConfig(), componentMap, NodeDecorator.IDENTITY);
    DiscountingMulticurveBundleFn bundleProvider =
        FunctionModel.build(DiscountingMulticurveBundleFn.class, graphConfig);
    FunctionResult<Pair<MulticurveProviderDiscount,CurveBuildingBlockBundle>> result;
    try {
      result = bundleProvider.generateBundle("Z-Marc JPY Dsc - FX USD");
    } catch (Exception e) {
      logMarketData(marketDataFn.getCollectedRequests());
      throw e;
    }
    assertNotNull(result);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));

    // Can examine result.getResult().getCurve("Z-Marc JPY Discounting - USD FX")) which should match view
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void engine() throws Exception {
    //int nTrades = 1_000_000;
    //int nTrades = 10_000;
    int nTrades = 1_000;
    //int nTrades = 1;
    //int nTrades = 2;
    long startTrades = System.currentTimeMillis();
    List<Trade> trades = Lists.newArrayListWithCapacity(nTrades);
    for (int i = 0; i < nTrades; i++) {
      trades.add(createRandomFxForwardTrade());
    }
    s_logger.info("created {} trades in {}ms", nTrades, System.currentTimeMillis() - startTrades);
    String exposureConfig = "EUR-USD_ON-OIS_EURIBOR6M-FRAIRS_EURIBOR3M-FRABS_-_ON-OIS_LIBOR3M-FRAIRS";
    ViewDef viewDef =
        viewDef("FX forward PV view",
                column("Present Value",
                       output(OutputNames.FX_PRESENT_VALUE, FXForwardSecurity.class,
                              config(
                                  arguments(
                                      function(ConfigDbMarketExposureSelectorFn.class,
                                               argument("exposureConfigName", exposureConfig)),
                                      function(RootFinderConfiguration.class,
                                               argument("rootFinderAbsoluteTolerance", 1e-9),
                                               argument("rootFinderRelativeTolerance", 1e-9),
                                               argument("rootFinderMaxIterations", 1000)),
                                      function(DefaultCurrencyPairsFn.class,
                                               argument("currencyPairs", ImmutableSet.of(CurrencyPair.of(USD, JPY),
                                                                                         CurrencyPair.of(EUR, USD),
                                                                                         CurrencyPair.of(GBP, USD)))),
                                      function(DefaultHistoricalTimeSeriesFn.class,
                                               argument("resolutionKey", "DEFAULT_TSS"),
                                               argument("htsRetrievalPeriod", Period.ofYears(1))))))));

    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
    //ExecutorService executor = new EngineTest.DirectExecutorService();
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    CompositeNodeDecorator decorator = new CompositeNodeDecorator(cachingDecorator, TracingProxy.INSTANCE);
    String serverUrl = "http://localhost:8080";
    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    DefaultResettableMarketDataFn marketDataFn = new DefaultResettableMarketDataFn();
    marketDataFn.resetMarketData(valuationTime, FXForwardPVFnTest.loadMarketDataForForward());
    Map<Class<?>, Object> comps = ImmutableMap.<Class<?>, Object>of(HistoricalTimeSeriesResolver.class, htsResolver);
    MarketDataFactory marketDataFactory = new SimpleMarketDataFactory(marketDataFn);
    long startComponents = System.currentTimeMillis();
    ComponentMap componentMap = ComponentMap.loadComponents(serverUrl).with(comps);
    s_logger.info("loaded components in {}ms", System.currentTimeMillis() - startComponents);
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(FXForwardPVFn.class);
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(DiscountingFXForwardPVFn.class,
                                      DefaultCurrencyPairsFn.class,
                                      FXForwardSecurityConverter.class,
                                      ConfigDBInstrumentExposuresProvider.class,
                                      DefaultCurveSpecificationMarketDataFn.class,
                                      DefaultFXMatrixFn.class,
                                      DefaultCurveDefinitionFn.class,
                                      DefaultDiscountingMulticurveBundleFn.class,
                                      DefaultCurveSpecificationFn.class,
                                      ConfigDBCurveConstructionConfigurationSource.class,
                                      DefaultHistoricalTimeSeriesFn.class,
                                      FxForwardDiscountingCalculatorFn.class,
                                      ConfigDbMarketExposureSelectorFn.class);
    long startEngine = System.currentTimeMillis();
    Engine engine = new Engine(executor,
                               componentMap,
                               availableOutputs,
                               availableImplementations,
                               FunctionConfig.EMPTY,
                               decorator);
    s_logger.info("created engine in {}ms", System.currentTimeMillis() - startEngine);
    long graphStart = System.currentTimeMillis();
    Engine.View view = engine.createView(viewDef, trades);
    s_logger.info("view built in {}ms", System.currentTimeMillis() - graphStart);
    //@SuppressWarnings("unchecked")
    //Set<Pair<Integer, Integer>> traceFunctions = Sets.newHashSet(Pairs.of(0, 0), Pairs.of(1, 0));
    //CycleArguments cycleArguments = new CycleArguments(valuationTime, marketDataFactory, traceFunctions);
    CycleArguments cycleArguments = new CycleArguments(valuationTime, marketDataFactory);
    //int nRuns = 1;
    int nRuns = 20;
    for (int i = 0; i < nRuns; i++) {
      long start = System.currentTimeMillis();
      view.run(cycleArguments);
      //Results results = view.run(cycleArguments);
      //System.out.println(results.get(0, 0).getCallGraph().prettyPrint());
      //System.out.println(results.get(1, 0).getCallGraph().prettyPrint());
      long time = System.currentTimeMillis() - start;
      s_logger.info("view executed in {}ms", time);
      Thread.sleep(1000);
    }
  }

  private static Trade createRandomFxForwardTrade() {
    ExternalId regionId = ExternalId.of(ExternalSchemes.FINANCIAL, "US");
    double usdAmount = 10_000_000 * Math.random();
    double eurAmount = usdAmount * (1.31 + 0.04 * Math.random());
    Currency payCcy;
    Currency recCcy;
    double payAmount;
    double recAmount;
    if (Math.random() < 0.5) {
      payAmount = usdAmount;
      payCcy = USD;
      recAmount = eurAmount;
      recCcy = EUR;
    } else {
      payAmount = eurAmount;
      payCcy = EUR;
      recAmount = usdAmount;
      recCcy = USD;
    }
    ZonedDateTime forwardDate = ZonedDateTime.now().plusWeeks((long) (104 * Math.random()));
    FXForwardSecurity security = new FXForwardSecurity(payCcy, payAmount, recCcy, recAmount, forwardDate, regionId);
    String id = Long.toString(s_nextId.getAndIncrement());
    security.setUniqueId(UniqueId.of("fxFwdSec", id));
    security.setName("FX forward " + id);
    SimpleTrade trade = new SimpleTrade();
    SimpleSecurityLink securityLink = new SimpleSecurityLink(ExternalId.of("fxFwdSecEx", id));
    securityLink.setTarget(security);
    trade.setSecurityLink(securityLink);
    trade.setUniqueId(UniqueId.of("fxFwdTrd", id));
    return trade;
  }

  private static FunctionConfig createFunctionConfig() {
    String exposureConfig = "EUR-USD_ON-OIS_EURIBOR6M-FRAIRS_EURIBOR3M-FRABS_-_ON-OIS_LIBOR3M-FRAIRS";
    return
        config(
            arguments(
                function(ConfigDbMarketExposureSelectorFn.class,
                         argument("exposureConfigName", exposureConfig)),
                function(RootFinderConfiguration.class,
                         argument("rootFinderAbsoluteTolerance", 1e-9),
                         argument("rootFinderRelativeTolerance", 1e-9),
                         argument("rootFinderMaxIterations", 1000)),
                function(DefaultCurrencyPairsFn.class,
                         argument("currencyPairs", ImmutableSet.of(CurrencyPair.of(USD, JPY),
                                                                   CurrencyPair.of(EUR, USD),
                                                                   CurrencyPair.of(GBP, USD)))),
                function(DefaultHistoricalTimeSeriesFn.class,
                         argument("resolutionKey", "DEFAULT_TSS"),
                         argument("htsRetrievalPeriod", Period.ofYears(1)))),
            implementations(FXForwardPVFn.class, DiscountingFXForwardPVFn.class,
                            FxForwardCalculatorFn.class, FxForwardDiscountingCalculatorFn.class,
                            MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                            CurrencyPairsFn.class, DefaultCurrencyPairsFn.class,
                            FinancialSecurityVisitor.class, FXForwardSecurityConverter.class,
                            InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class,
                            CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                            FXMatrixFn.class, DefaultFXMatrixFn.class,
                            CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                            DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                            CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                            CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class,
                            HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class));
  }

  private static ComponentMap componentMap(Class<?>... componentTypes) {
    Map<Class<?>, Object> compMap = Maps.newHashMap();
    for (Class<?> componentType : componentTypes) {
      compMap.put(componentType, mock(componentType));
    }
    return ComponentMap.of(compMap);
  }

  // TODO move this somewhere else now it's shared with the engine test
  public static Map<MarketDataRequirement, MarketDataItem> loadMarketDataForForward() throws IOException {
    return loadMarketData("/marketdata.properties");
  }

  private static Map<MarketDataRequirement, MarketDataItem> loadMarketDataForYieldCurve() throws IOException {
    return loadMarketData("/yield-curve-marketdata.properties");
  }

  private static Map<MarketDataRequirement, MarketDataItem> loadMarketData(String fileName) throws IOException {
    Properties properties = new Properties();
    try (InputStream stream = FXForwardPVFn.class.getResourceAsStream(fileName);
         Reader reader = new BufferedReader(new InputStreamReader(stream))) {
      properties.load(reader);
    }
    Map<MarketDataRequirement, MarketDataItem> data = Maps.newHashMap();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String id = (String) entry.getKey();
      String value = (String) entry.getValue();
      addValue(data, id, Double.valueOf(value) / 100);
    }
    return data;
  }

  private static MarketDataItem addValue(Map<MarketDataRequirement, MarketDataItem> marketData, String ticker, double value) {
    return addValue(marketData, new CurveNodeMarketDataRequirement(ExternalSchemes.bloombergTickerSecurityId(ticker), "Market_Value"), value);
  }

  private static MarketDataItem addValue(Map<MarketDataRequirement, MarketDataItem> marketData,
                                         MarketDataRequirement requirement,
                                         double value) {
    return marketData.put(requirement, MarketDataItem.available(value));
  }

  private static void logMarketData(Set<MarketDataRequirement> requirements) {
    for (MarketDataRequirement requirement : requirements) {
      if (requirement instanceof CurveNodeMarketDataRequirement) {
        ExternalId id = ((CurveNodeMarketDataRequirement) requirement).getExternalId();
        System.out.println(id);
      } else {
        System.out.println(requirement);
      }
    }
  }
}
