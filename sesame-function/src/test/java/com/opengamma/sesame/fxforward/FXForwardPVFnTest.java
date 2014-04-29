/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static com.opengamma.util.money.Currency.EUR;
import static com.opengamma.util.money.Currency.GBP;
import static com.opengamma.util.money.Currency.JPY;
import static com.opengamma.util.money.Currency.USD;
import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.SuccessStatus.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.position.Trade;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.security.impl.SimpleSecurityLink;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.marketdata.spec.MarketData;
import com.opengamma.financial.analytics.CurrencyLabelledMatrix1D;
import com.opengamma.financial.analytics.conversion.FXForwardSecurityConverter;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.analytics.curve.exposure.InstrumentExposuresProvider;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.master.historicaltimeseries.impl.RemoteHistoricalTimeSeriesResolver;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
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
import com.opengamma.sesame.DirectExecutorService;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.sesame.MarketdataResourcesLoader;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.cache.CachingProxyDecorator;
import com.opengamma.sesame.cache.ExecutingMethodsThreadLocal;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.CycleArguments;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.engine.FunctionService;
import com.opengamma.sesame.engine.ResultItem;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.LDClient;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.ResettableLiveMarketDataSource;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.util.ehcache.EHCacheUtils;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

@Test(groups = TestGroup.UNIT)
public class FXForwardPVFnTest {

  private static final Logger s_logger = LoggerFactory.getLogger(FXForwardPVFnTest.class);

  private static final AtomicLong s_nextId = new AtomicLong(0);
  
  private Cache _cache;

  @BeforeClass
  public void setUpClass() {
    CacheManager cacheManager = EHCacheUtils.createTestCacheManager(getClass());
    String cacheName = "testCache";
    EHCacheUtils.addCache(cacheManager, cacheName);
    _cache = EHCacheUtils.getCacheFromManager(cacheManager, cacheName);
  }

  @Test
  public void buildGraph() {
    FunctionMetadata calculatePV = EngineUtils.createMetadata(FXForwardPVFn.class, "calculatePV");
    FunctionModelConfig config = createFunctionConfig();
    ComponentMap componentMap = componentMap(ConfigSource.class,
                                             ConventionSource.class,
                                             ConventionBundleSource.class,
                                             HistoricalTimeSeriesResolver.class,
                                             SecuritySource.class,
                                             HolidaySource.class,
                                             HistoricalTimeSeriesSource.class,
                                             MarketDataFn.class,
                                             HistoricalMarketDataFn.class,
                                             RegionSource.class);
    FunctionModel functionModel = FunctionModel.forFunction(calculatePV, config, componentMap.getComponentTypes());
    Object fn = functionModel.build(new FunctionBuilder(), componentMap).getReceiver();
    assertTrue(fn instanceof FXForwardPVFn);
    System.out.println(functionModel.prettyPrint(true));
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithNoData() throws IOException {
    Result<CurrencyLabelledMatrix1D> pv = executeAgainstRemoteServer(Collections.<ExternalIdBundle, Double>emptyMap());
    assertNotNull(pv);
    MatcherAssert.assertThat(pv.getStatus(), is((ResultStatus) MISSING_DATA));
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithData() throws IOException {
    Result<CurrencyLabelledMatrix1D> pv = executeAgainstRemoteServer(
        MarketdataResourcesLoader.getData("marketdata.properties", ExternalSchemes.BLOOMBERG_TICKER));
    assertNotNull(pv);
    assertThat(pv.getStatus(), is((ResultStatus) SUCCESS));
  }

  private Result<CurrencyLabelledMatrix1D> executeAgainstRemoteServer(Map<ExternalIdBundle, Double> marketData) {
    String serverUrl = "http://localhost:8080";
    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    Map<Class<?>, Object> comps = ImmutableMap.<Class<?>, Object>of(HistoricalTimeSeriesResolver.class, htsResolver);
    ComponentMap componentMap = ComponentMap.loadComponents(serverUrl).with(comps);
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cache, new ExecutingMethodsThreadLocal());
    FXForwardPVFn pvFunction = FunctionModel.build(FXForwardPVFn.class,
                                                   createFunctionConfig(),
                                                   componentMap,
                                                   TimingProxy.INSTANCE,
                                                   cachingDecorator);
    ExternalId regionId = ExternalId.of(ExternalSchemes.FINANCIAL, "US");
    ZonedDateTime forwardDate = ZonedDateTime.of(2014, 11, 7, 12, 0, 0, 0, ZoneOffset.UTC);
    FXForwardSecurity security = new FXForwardSecurity(EUR, 10_000_000, USD, 14_000_000, forwardDate, regionId);
    security.setUniqueId(UniqueId.of("sec", "123"));
    //TracingProxy.start(new FullTracer());
    Result<CurrencyLabelledMatrix1D> result = null;

    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    FieldName fieldName = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);
    ResettableLiveMarketDataSource.Builder builder = new ResettableLiveMarketDataSource.Builder(MarketData.live(), mock(LDClient.class));
    ResettableLiveMarketDataSource dataSource = builder.data(fieldName, marketData).build();
    SimpleEnvironment env = new SimpleEnvironment(valuationTime, dataSource);

    for (int i = 0; i < 100; i++) {
      result = pvFunction.calculatePV(env, security);
      System.out.println();
    }
    //System.out.println(TracingProxy.end().prettyPrint());
    System.out.println("requested data: " + dataSource.getRequestedData());
    return result;
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeYieldCurveAgainstRemoteServer() throws IOException {

    // String serverUrl = "http://devsvr-lx-2:8080";
    String serverUrl = "http://localhost:8080";
    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    Map<ExternalIdBundle, Double> marketData = MarketdataResourcesLoader.getData("yield-curve-marketdata.properties",
                                                                                 ExternalSchemes.BLOOMBERG_TICKER);
    marketData.put(ExternalIdBundle.of(ExternalSchemes.BLOOMBERG_TICKER, "JPY Curncy"), 98.86);
    Map<Class<?>, Object> comps = ImmutableMap.<Class<?>, Object>of(HistoricalTimeSeriesResolver.class, htsResolver);
    ComponentMap componentMap = ComponentMap.loadComponents(serverUrl).with(comps);

    DiscountingMulticurveBundleFn bundleProvider =
        FunctionModel.build(DiscountingMulticurveBundleFn.class, createFunctionConfig(), componentMap);
    Result<Pair<MulticurveProviderDiscount,CurveBuildingBlockBundle>> result;
    ResettableLiveMarketDataSource dataSource = null;

    try {
      ConfigSource configSource = componentMap.getComponent(ConfigSource.class);
      CurveConstructionConfiguration curveConfig = configSource.get(CurveConstructionConfiguration.class, "Z-Marc JPY Dsc - FX USD", VersionCorrection.LATEST)
          .iterator().next().getValue();

      FieldName fieldName = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);
      ResettableLiveMarketDataSource.Builder builder = new ResettableLiveMarketDataSource.Builder(MarketData.live(), mock(LDClient.class));
      dataSource = builder.data(fieldName, marketData).build();
      SimpleEnvironment env = new SimpleEnvironment(valuationTime, dataSource);
      result = bundleProvider.generateBundle(env, curveConfig);
    } catch (Exception e) {
      if (dataSource != null) {
        System.out.println(dataSource.getRequestedData());
      }
      throw e;
    }
    assertNotNull(result);
    assertThat(result.getStatus(), is((ResultStatus) SUCCESS));

    // Can examine result.getValue().getCurve("Z-Marc JPY Discounting - USD FX")) which should match view
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void curveBundleOnly() throws IOException {
    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    ConfigLink<CurrencyMatrix> currencyMatrixLink = ConfigLink.resolvable("BloombergLiveData", CurrencyMatrix.class);

    ViewConfig viewConfig =
        configureView("Curve Bundle only",
                  nonPortfolioOutput("Curve Bundle",
                       output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE,
                              config(
                                  arguments(
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
                                               argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1)))),
                                      function(DefaultDiscountingMulticurveBundleFn.class,
                                               argument("impliedCurveNames", StringSet.of()),
                                               argument("curveConfig", ConfigLink.resolvable("Temple USD",
                                                                                     CurveConstructionConfiguration.class))),
                                      function(DefaultMarketDataFn.class,
                                               argument("currencyMatrix", currencyMatrixLink)),
                                      function(DefaultHistoricalMarketDataFn.class,
                                               argument("currencyMatrix", currencyMatrixLink),
                                               argument("dataSource", "BLOOMBERG")))))));

    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(DiscountingMulticurveBundleFn.class);
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(DiscountingFXForwardPVFn.class,
                                      DefaultCurrencyPairsFn.class,
                                      FXForwardSecurityConverter.class,
                                      ConfigDBInstrumentExposuresProvider.class,
                                      DefaultCurveSpecificationMarketDataFn.class,
                                      ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                                      DefaultFXMatrixFn.class,
                                      DefaultCurveDefinitionFn.class,
                                      DefaultDiscountingMulticurveBundleFn.class,
                                      DefaultCurveSpecificationFn.class,
                                      ConfigDBCurveConstructionConfigurationSource.class,
                                      DefaultHistoricalTimeSeriesFn.class,
                                      FXForwardDiscountingCalculatorFn.class,
                                      ConfigDbMarketExposureSelectorFn.class,
                                      DefaultMarketDataFn.class,
                                      DefaultHistoricalMarketDataFn.class);

    String serverUrl = "http://devsvr-lx-2:8080";
    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    Map<Class<?>, Object> comps = ImmutableMap.<Class<?>, Object>of(HistoricalTimeSeriesResolver.class, htsResolver);
    ComponentMap componentMap = ComponentMap.loadComponents(serverUrl).with(comps);
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider();
    ServiceContext serviceContext =
        ServiceContext.of(componentMap.getComponents()).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    ViewFactory viewFactory = new ViewFactory(new DirectExecutorService(),
                               componentMap,
                               availableOutputs,
                               availableImplementations,
                               FunctionModelConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.noneOf(FunctionService.class));
    View view = viewFactory.createView(viewConfig);
    Map<ExternalIdBundle, Double> marketData = MarketdataResourcesLoader.getData("/marketdata.properties",
                                                                                 ExternalSchemes.BLOOMBERG_TICKER);
    FieldName fieldName = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);
    ResettableLiveMarketDataSource.Builder builder = new ResettableLiveMarketDataSource.Builder(MarketData.live(), mock(LDClient.class));
    ResettableLiveMarketDataSource dataSource = builder.data(fieldName, marketData).build();
    CycleArguments cycleArguments = new CycleArguments(valuationTime, VersionCorrection.LATEST, dataSource);
    Results results = view.run(cycleArguments);
    System.out.println(results);
    ResultItem resultItem = results.get("Curve Bundle");
    Result<?> result = resultItem.getResult();
    assertTrue(result.isSuccess());
    Object value = result.getValue();
    assertNotNull(value);
    System.out.println(value);
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
    ConfigLink<ExposureFunctions> exposureConfig = ConfigLink.resolvable("Temple Exposure Config", ExposureFunctions.class);

    ViewConfig viewConfig =
        configureView("FX forward PV view",
                      column("Present Value",
                             output(OutputNames.FX_PRESENT_VALUE, FXForwardSecurity.class,
                                    config(
                                        arguments(
                                            function(ConfigDbMarketExposureSelectorFn.class,
                                                     argument("exposureConfig", exposureConfig)),
                                            function(RootFinderConfiguration.class,
                                                     argument("rootFinderAbsoluteTolerance", 1e-9),
                                                     argument("rootFinderRelativeTolerance", 1e-9),
                                                     argument("rootFinderMaxIterations", 1000)),
                                            function(DefaultCurrencyPairsFn.class,
                                                     argument("currencyPairs",
                                                              ImmutableSet.of(CurrencyPair.of(USD, JPY),
                                                                              CurrencyPair.of(EUR, USD),
                                                                              CurrencyPair.of(GBP, USD)))),
                                            function(DefaultHistoricalTimeSeriesFn.class,
                                                     argument("resolutionKey", "DEFAULT_TSS"),
                                                     argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1)))),
                                            function(DefaultDiscountingMulticurveBundleFn.class,
                                                     argument("impliedCurveNames",
                                                              StringSet.of())))))));

    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
    String serverUrl = "http://devsvr-lx-2:8080";
    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    Map<Class<?>, Object> comps = ImmutableMap.<Class<?>, Object>of(HistoricalTimeSeriesResolver.class, htsResolver);
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
                                      ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                                      DefaultCurveSpecificationFn.class,
                                      ConfigDBCurveConstructionConfigurationSource.class,
                                      DefaultHistoricalTimeSeriesFn.class,
                                      FXForwardDiscountingCalculatorFn.class,
                                      ConfigDbMarketExposureSelectorFn.class);
    long startEngine = System.currentTimeMillis();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider();
    ServiceContext serviceContext =
        ServiceContext.of(componentMap.getComponents()).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    ViewFactory viewFactory = new ViewFactory(executor,
                               componentMap,
                               availableOutputs,
                               availableImplementations,
                               FunctionModelConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.of(FunctionService.CACHING, FunctionService.TRACING));
    s_logger.info("created engine in {}ms", System.currentTimeMillis() - startEngine);
    long graphStart = System.currentTimeMillis();
    View view = viewFactory.createView(viewConfig, FXForwardSecurity.class);
    s_logger.info("view built in {}ms", System.currentTimeMillis() - graphStart);
    //@SuppressWarnings("unchecked")
    //Set<Pair<Integer, Integer>> traceFunctions = Sets.newHashSet(Pairs.of(0, 0), Pairs.of(1, 0));
    //CycleArguments cycleArguments = new CycleArguments(valuationTime, marketDataFactory, traceFunctions);
    ZonedDateTime valuationTime = ZonedDateTime.of(2013, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
    Map<ExternalIdBundle, Double> marketData = MarketdataResourcesLoader.getData("marketdata.properties",
                                                                                 ExternalSchemes.BLOOMBERG_TICKER);
    FieldName fieldName = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);
    ResettableLiveMarketDataSource.Builder builder = new ResettableLiveMarketDataSource.Builder(MarketData.live(), mock(LDClient.class));
    ResettableLiveMarketDataSource dataSource = builder.data(fieldName, marketData).build();
    CycleArguments cycleArguments = new CycleArguments(valuationTime, VersionCorrection.LATEST, dataSource);
    //int nRuns = 1;
    int nRuns = 20;
    for (int i = 0; i < nRuns; i++) {
      long start = System.currentTimeMillis();
      view.run(cycleArguments, trades);
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

  private static FunctionModelConfig createFunctionConfig() {
    return
        config(
            arguments(
                function(ConfigDbMarketExposureSelectorFn.class,
                         argument("exposureConfig", ConfigLink.resolved( mock(ExposureFunctions.class)))),
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
                         argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1)))),
                function(DefaultDiscountingMulticurveBundleFn.class,
                         argument("impliedCurveNames", StringSet.of()))),
            implementations(FXForwardPVFn.class,
                            DiscountingFXForwardPVFn.class,
                            FXForwardCalculatorFn.class,
                            FXForwardDiscountingCalculatorFn.class,
                            MarketExposureSelectorFn.class,
                            ConfigDbMarketExposureSelectorFn.class,
                            CurrencyPairsFn.class,
                            DefaultCurrencyPairsFn.class,
                            FinancialSecurityVisitor.class,
                            FXForwardSecurityConverter.class,
                            InstrumentExposuresProvider.class,
                            ConfigDBInstrumentExposuresProvider.class,
                            CurveSpecificationMarketDataFn.class,
                            DefaultCurveSpecificationMarketDataFn.class,
                            FXMatrixFn.class,
                            DefaultFXMatrixFn.class,
                            CurveDefinitionFn.class,
                            DefaultCurveDefinitionFn.class,
                            DiscountingMulticurveBundleFn.class,
                            DefaultDiscountingMulticurveBundleFn.class,
                            DiscountingMulticurveCombinerFn.class,
                            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                            CurveSpecificationFn.class,
                            DefaultCurveSpecificationFn.class,
                            CurveConstructionConfigurationSource.class,
                            ConfigDBCurveConstructionConfigurationSource.class,
                            HistoricalTimeSeriesFn.class,
                            DefaultHistoricalTimeSeriesFn.class));
  }

  private static ComponentMap componentMap(Class<?>... componentTypes) {
    Map<Class<?>, Object> compMap = Maps.newHashMap();
    for (Class<?> componentType : componentTypes) {
      compMap.put(componentType, mock(componentType));
    }
    return ComponentMap.of(compMap);
  }
}
