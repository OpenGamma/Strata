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
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.hamcrest.MatcherAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.opengamma.analytics.financial.schedule.ScheduleCalculatorFactory;
import com.opengamma.analytics.financial.schedule.TimeSeriesSamplingFunctionFactory;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
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
import com.opengamma.sesame.ConfigDbMarketExposureSelectorProvider;
import com.opengamma.sesame.CurrencyPairs;
import com.opengamma.sesame.CurrencyPairsFunction;
import com.opengamma.sesame.CurveDefinitionProvider;
import com.opengamma.sesame.CurveDefinitionProviderFunction;
import com.opengamma.sesame.CurveSpecificationMarketDataProvider;
import com.opengamma.sesame.CurveSpecificationMarketDataProviderFunction;
import com.opengamma.sesame.CurveSpecificationProvider;
import com.opengamma.sesame.CurveSpecificationProviderFunction;
import com.opengamma.sesame.DiscountingMulticurveBundleProvider;
import com.opengamma.sesame.DiscountingMulticurveBundleProviderFunction;
import com.opengamma.sesame.FXMatrixProvider;
import com.opengamma.sesame.FXMatrixProviderFunction;
import com.opengamma.sesame.FunctionResult;
import com.opengamma.sesame.FxReturnSeriesProvider;
import com.opengamma.sesame.FxReturnSeriesProviderFunction;
import com.opengamma.sesame.HistoricalTimeSeriesProvider;
import com.opengamma.sesame.HistoricalTimeSeriesProviderFunction;
import com.opengamma.sesame.MarketExposureSelectorProvider;
import com.opengamma.sesame.ResultStatus;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.ValuationTimeProvider;
import com.opengamma.sesame.ValuationTimeProviderFunction;
import com.opengamma.sesame.cache.CachingProxyDecorator;
import com.opengamma.sesame.cache.ExecutingMethodsThreadLocal;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.graph.CompositeNodeDecorator;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.marketdata.CurveNodeMarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.sesame.marketdata.MarketDataProvider;
import com.opengamma.sesame.marketdata.MarketDataProviderFunction;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.ehcache.EHCacheUtils;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;

import net.sf.ehcache.CacheManager;

@Test(groups = TestGroup.UNIT)
public class FXForwardPnlSeriesFunctionTest {

  private static final Logger s_logger = LoggerFactory.getLogger(FXForwardPnlSeriesFunctionTest.class);

  private static final AtomicLong s_nextId = new AtomicLong(0);
  private static ZonedDateTime s_valuationTime;

  private CacheManager _cacheManager;

  @BeforeClass
  public void setUpClass() {
    _cacheManager = EHCacheUtils.createTestCacheManager(getClass());
  }

  @Test
  public void buildGraph() {
    FunctionMetadata calculatePnl = ConfigUtils.createMetadata(FxForwardPnLSeriesFunction.class, "calculatePnlSeries");
    FunctionConfig config = createFunctionConfig();
    ComponentMap componentMap = componentMap(ConfigSource.class,
                                             ConventionSource.class,
                                             ConventionBundleSource.class,
                                             HistoricalTimeSeriesResolver.class,
                                             SecuritySource.class,
                                             HolidaySource.class,
                                             HistoricalTimeSeriesSource.class,
                                             MarketDataProviderFunction.class,
                                             RegionSource.class);
    GraphConfig graphConfig = new GraphConfig(config, componentMap, NodeDecorator.IDENTITY);
    FunctionModel functionModel = FunctionModel.forFunction(calculatePnl, graphConfig);
    Object fn = functionModel.build(new FunctionBuilder(), componentMap).getReceiver();
    assertTrue(fn instanceof FxForwardPnLSeriesFunction);
    System.out.println(functionModel.prettyPrint(true));
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithNoData() throws IOException {
    FunctionResult<LocalDateDoubleTimeSeries> pnl = executeAgainstRemoteServer(Collections.<MarketDataRequirement, MarketDataItem>emptyMap());
    assertNotNull(pnl);
    MatcherAssert.assertThat(pnl.getStatus(), is((ResultStatus) MISSING_DATA));
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithData() throws IOException {
    FunctionResult<LocalDateDoubleTimeSeries> pnl = executeAgainstRemoteServer(loadMarketDataForForward());
    assertNotNull(pnl);
    assertThat(pnl.getStatus(), is((ResultStatus) SUCCESS));
  }

  private FunctionResult<LocalDateDoubleTimeSeries> executeAgainstRemoteServer(
      Map<MarketDataRequirement, MarketDataItem> marketData) {
    String serverUrl = "http://localhost:8080";
    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    MarketDataProvider marketDataProvider = new MarketDataProvider();
    marketDataProvider.resetMarketData(s_valuationTime, marketData);
    Map<Class<?>, Object> comps = ImmutableMap.of(HistoricalTimeSeriesResolver.class, htsResolver,
                                                  MarketDataProviderFunction.class, marketDataProvider);
    ComponentMap componentMap = ComponentMap.loadComponents(serverUrl).with(comps);
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    CompositeNodeDecorator decorator = new CompositeNodeDecorator(TimingProxy.INSTANCE, cachingDecorator);
    GraphConfig graphConfig = new GraphConfig(createFunctionConfig(), componentMap, decorator);
    FxForwardPnLSeriesFunction pvFunction = FunctionModel.build(FxForwardPnLSeriesFunction.class, "calculatePnlSeries", graphConfig);
    ExternalId regionId = ExternalId.of(ExternalSchemes.FINANCIAL, "US");
    ZonedDateTime forwardDate = ZonedDateTime.of(2014, 11, 7, 12, 0, 0, 0, ZoneOffset.UTC);
    FXForwardSecurity security = new FXForwardSecurity(EUR, 10_000_000, USD, 14_000_000, forwardDate, regionId);
    security.setUniqueId(UniqueId.of("sec", "123"));
    //TracingProxy.start(new FullTracer());
    FunctionResult<LocalDateDoubleTimeSeries> result = null;
    for (int i = 0; i < 100; i++) {
      result = pvFunction.calculatePnlSeries(security);
      System.out.println();
    }
    //System.out.println(TracingProxy.end().prettyPrint());
    logMarketData(marketDataProvider.getCollectedRequests());
    return result;
  }


  private static FunctionConfig createFunctionConfig() {
    String exposureConfig = "EUR-USD[ON-OIS][EURIBOR6M-FRA/IRS][EURIBOR3M-FRA/BS]-[ON-OIS][LIBOR3M-FRA/IRS]";
    s_valuationTime = ZonedDateTime.of(2013, 11, 7, 11, 0, 0, 0, ZoneOffset.UTC);
    return
        config(
            arguments(
                function(ConfigDbMarketExposureSelectorProvider.class,
                         argument("exposureConfigName", exposureConfig)),
                function(DiscountingFxForwardPnLSeries.class,
                         argument("seriesPeriod", Period.ofYears(5)),
                         argument("outputCurrency", Optional.of(Currency.USD))),
                function(FxReturnSeriesProvider.class,
                         argument("timeSeriesSamplingFunction", TimeSeriesSamplingFunctionFactory.NO_PADDING_FUNCTION),
                         argument("returnSeriesWeighting", FxReturnSeriesProvider.ReturnSeriesWeighting.NONE),
                         argument("schedule", ScheduleCalculatorFactory.DAILY_CALCULATOR),
                         argument("returnSeriesType", FxReturnSeriesProvider.ReturnSeriesType.ABSOLUTE)),
                function(ValuationTimeProvider.class,
                         argument("valuationTime", s_valuationTime)),
                function(RootFinderConfiguration.class,
                         argument("rootFinderAbsoluteTolerance", 1e-9),
                         argument("rootFinderRelativeTolerance", 1e-9),
                         argument("rootFinderMaxIterations", 1000)),
                function(CurrencyPairs.class,
                         argument("currencyPairs", ImmutableSet.of(CurrencyPair.of(USD, JPY),
                                                                   CurrencyPair.of(EUR, USD),
                                                                   CurrencyPair.of(GBP, USD)))),
                function(HistoricalTimeSeriesProvider.class,
                         argument("resolutionKey", "DEFAULT_TSS"),
                         argument("htsRetrievalPeriod", Period.ofYears(1)))),
            implementations(FxForwardPnLSeriesFunction.class,
                            DiscountingFxForwardPnLSeries.class,
                            FxReturnSeriesProviderFunction.class,
                            FxReturnSeriesProvider.class,
                            FxForwardCalculatorProvider.class,
                            FxForwardDiscountingCalculatorProvider.class,
                            MarketExposureSelectorProvider.class,
                            ConfigDbMarketExposureSelectorProvider.class,
                            CurrencyPairsFunction.class,
                            CurrencyPairs.class,
                            FinancialSecurityVisitor.class,
                            FXForwardSecurityConverter.class,
                            InstrumentExposuresProvider.class,
                            ConfigDBInstrumentExposuresProvider.class,
                            CurveSpecificationMarketDataProviderFunction.class,
                            CurveSpecificationMarketDataProvider.class,
                            FXMatrixProviderFunction.class,
                            FXMatrixProvider.class,
                            CurveDefinitionProviderFunction.class,
                            CurveDefinitionProvider.class,
                            DiscountingMulticurveBundleProviderFunction.class,
                            DiscountingMulticurveBundleProvider.class,
                            CurveSpecificationProviderFunction.class,
                            CurveSpecificationProvider.class,
                            ValuationTimeProviderFunction.class,
                            ValuationTimeProvider.class,
                            CurveConstructionConfigurationSource.class,
                            ConfigDBCurveConstructionConfigurationSource.class,
                            HistoricalTimeSeriesProviderFunction.class,
                            HistoricalTimeSeriesProvider.class));
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
    try (InputStream stream = FXForwardPVFunction.class.getResourceAsStream(fileName);
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
