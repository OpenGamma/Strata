/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
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
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
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
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.conversion.FXForwardSecurityConverter;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
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
import com.opengamma.sesame.DefaultFXReturnSeriesFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DefaultValuationTimeFn;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.FXReturnSeriesFn;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.TimeSeriesReturnConverterFactory;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.sesame.cache.CachingProxyDecorator;
import com.opengamma.sesame.cache.ExecutingMethodsThreadLocal;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.EagerMarketDataFn;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.HistoricalRawMarketDataSource;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.sesame.trace.Tracer;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.ehcache.EHCacheUtils;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;

import net.sf.ehcache.CacheManager;

@Test(groups = TestGroup.UNIT)
public class FXForwardPnlSeriesFunctionTest {

  private static final ZonedDateTime s_valuationTime = ZonedDateTime.of(2013, 11, 7, 11, 0, 0, 0, ZoneOffset.UTC);

  private CacheManager _cacheManager;

  @BeforeClass
  public void setUpClass() {
    _cacheManager = EHCacheUtils.createTestCacheManager(getClass());
  }

  @Test
  public void buildGraph() {
    FunctionMetadata calculatePnl = EngineUtils.createMetadata(FXForwardPnLSeriesFn.class, "calculatePnlSeries");
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
                                             RegionSource.class,
                                             ValuationTimeFn.class);
    FunctionModel functionModel = FunctionModel.forFunction(calculatePnl, config, componentMap.getComponentTypes());
    Object fn = functionModel.build(new FunctionBuilder(), componentMap).getReceiver();
    assertTrue(fn instanceof FXForwardPnLSeriesFn);
    System.out.println(functionModel.prettyPrint(true));
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithNoData() throws IOException {
    Result<LocalDateDoubleTimeSeries> pnl = executeAgainstRemoteServer();
    assertNotNull(pnl);
    MatcherAssert.assertThat(pnl.getStatus(), is((ResultStatus) MISSING_DATA));
  }

  //@Test(groups = TestGroup.INTEGRATION)
  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithData() throws IOException {
    Result<LocalDateDoubleTimeSeries> pnl = executeAgainstRemoteServer();
    assertNotNull(pnl);
    assertThat(pnl.getStatus(), is((ResultStatus) SUCCESS));
  }

  private Result<LocalDateDoubleTimeSeries> executeAgainstRemoteServer() {
    String serverUrl = "http://localhost:8080";
    ComponentMap serverComponents = ComponentMap.loadComponents(serverUrl);
    ConfigSource configSource = serverComponents.getComponent(ConfigSource.class);
    HistoricalTimeSeriesSource timeSeriesSource = serverComponents.getComponent(HistoricalTimeSeriesSource.class);
    LocalDate date = LocalDate.of(2013, 11, 7);
    HistoricalRawMarketDataSource rawDataSource =
        new HistoricalRawMarketDataSource(timeSeriesSource, date, "BLOOMBERG", "Market_Value");
    // TODO set up a service context and do this with a link
    CurrencyMatrix currencyMatrix = configSource.getLatestByName(CurrencyMatrix.class, "BloombergLiveData");
    MarketDataFn marketDataProvider = new EagerMarketDataFn(currencyMatrix, rawDataSource);

    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    Map<Class<?>, Object> comps = ImmutableMap.of(HistoricalTimeSeriesResolver.class, htsResolver,
                                                  MarketDataFn.class, marketDataProvider,
                                                  ValuationTimeFn.class, new DefaultValuationTimeFn(s_valuationTime));
    ComponentMap componentMap = serverComponents.with(comps);

    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, new ExecutingMethodsThreadLocal());
    FXForwardPnLSeriesFn pvFunction = FunctionModel.build(FXForwardPnLSeriesFn.class,
                                                          createFunctionConfig(),
                                                          componentMap,
                                                          TimingProxy.INSTANCE,
                                                          TracingProxy.INSTANCE,
                                                          cachingDecorator);
    ExternalId regionId = ExternalId.of(ExternalSchemes.FINANCIAL, "US");
    ZonedDateTime forwardDate = ZonedDateTime.of(2014, 11, 7, 12, 0, 0, 0, ZoneOffset.UTC);
    FXForwardSecurity security = new FXForwardSecurity(EUR, 10_000_000, USD, 14_000_000, forwardDate, regionId);
    security.setUniqueId(UniqueId.of("sec", "123"));
    TracingProxy.start(Tracer.create(true));
    Result<LocalDateDoubleTimeSeries> result = null;
    int nRuns = 100;
    //int nRuns = 1;
    for (int i = 0; i < nRuns; i++) {
      result = pvFunction.calculatePnlSeries(security, date);
      System.out.println();
    }
    System.out.println(TracingProxy.end().prettyPrint());
    return result;
  }

  private static FunctionModelConfig createFunctionConfig() {
    ConfigLink<ExposureFunctions> exposureConfig =
        ConfigLink.of("EUR-USD_ON-OIS_EURIBOR6M-FRAIRS_EURIBOR3M-FRABS_-_ON-OIS_LIBOR3M-FRAIRS",
                      mock(ExposureFunctions.class));

    return
        config(
            arguments(
                function(ConfigDbMarketExposureSelectorFn.class,
                         argument("exposureConfig", exposureConfig)),
                function(DiscountingFXForwardSpotPnLSeriesFn.class,
                         argument("seriesPeriod", Period.ofYears(1)),
                         argument("outputCurrency", Optional.of(Currency.USD))),
                function(DefaultFXReturnSeriesFn.class,
                         argument("timeSeriesSamplingFunction", TimeSeriesSamplingFunctionFactory.NO_PADDING_FUNCTION),
                         argument("timeSeriesConverter", TimeSeriesReturnConverterFactory.absolute()),
                         argument("schedule", ScheduleCalculatorFactory.DAILY_CALCULATOR)),
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
                         // TODO will need to handle this differently when arg values are strings and primitives
                         // will need string conversion for values like this which can be parsed
                         argument("htsRetrievalPeriod", Period.ofYears(1))),
                function(DefaultDiscountingMulticurveBundleFn.class,
                         argument("impliedCurveNames", Collections.emptySet()))),
            implementations(FXForwardPnLSeriesFn.class,
                            DiscountingFXForwardSpotPnLSeriesFn.class,
                            FXReturnSeriesFn.class,
                            DefaultFXReturnSeriesFn.class,
                            FXForwardCalculatorFn.class,
                            FXForwardDiscountingCalculatorFn.class,
                            DiscountingMulticurveCombinerFn.class,
                            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
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
