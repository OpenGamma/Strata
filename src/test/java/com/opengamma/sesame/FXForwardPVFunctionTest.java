/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.util.money.Currency.EUR;
import static com.opengamma.util.money.Currency.GBP;
import static com.opengamma.util.money.Currency.JPY;
import static com.opengamma.util.money.Currency.USD;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
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
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.marketdata.CurveNodeMarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.MarketDataValue;
import com.opengamma.sesame.marketdata.SingleMarketDataValue;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

@Test(groups = TestGroup.UNIT)
public class FXForwardPVFunctionTest {

  @Test
  public void buildGraph() {
    FunctionMetadata calculatePV = ConfigUtils.createMetadata(FXForwardPVFunction.class, "calculatePV");
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
    FunctionModel functionModel = FunctionModel.forFunction(calculatePV, graphConfig);
    Object fn = functionModel.build(componentMap).getReceiver();
    assertTrue(fn instanceof FXForwardPVFunction);
    System.out.println(functionModel.prettyPrint());
  }

  @Test(groups = TestGroup.INTEGRATION)
  //@Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithNoData() throws IOException {
    FunctionResult<CurrencyLabelledMatrix1D> pv = executeAgainstRemoteServer(
        Collections.<MarketDataRequirement, Pair<MarketDataStatus, MarketDataValue>>emptyMap());
    assertNotNull(pv);
  }

  @Test(groups = TestGroup.INTEGRATION)
  //@Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithData() throws IOException {
    FunctionResult<CurrencyLabelledMatrix1D> pv = executeAgainstRemoteServer(loadMarketData());
    assertNotNull(pv);
  }

  private FunctionResult<CurrencyLabelledMatrix1D> executeAgainstRemoteServer(
      Map<MarketDataRequirement, Pair<MarketDataStatus, MarketDataValue>> marketData) {
    String serverUrl = "http://localhost:8080";
    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    MarketDataProvider marketDataProvider = new MarketDataProvider();
    marketDataProvider.resetMarketData(marketData);
    Map<Class<?>, Object> comps = ImmutableMap.of(HistoricalTimeSeriesResolver.class, htsResolver,
                                                  MarketDataProviderFunction.class, marketDataProvider);
    ComponentMap componentMap = ComponentMap.loadComponents(serverUrl).with(comps);
    GraphConfig graphConfig = new GraphConfig(createFunctionConfig(), componentMap, NodeDecorator.IDENTITY);
    FXForwardPVFunction pvFunction = FunctionModel.build(FXForwardPVFunction.class, "calculatePV", graphConfig);
    ExternalId regionId = ExternalId.of(ExternalSchemes.FINANCIAL, "US");
    ZonedDateTime forwardDate = ZonedDateTime.of(2014, 11, 7, 12, 0, 0, 0, ZoneOffset.UTC);
    FXForwardSecurity security = new FXForwardSecurity(JPY, 1000000, USD, 1350000, forwardDate, regionId);
    security.setUniqueId(UniqueId.of("sec", "123"));
    return pvFunction.calculatePV(security);
  }

  private static FunctionConfig createFunctionConfig() {
    return
        config(
            arguments(
                function(DiscountingFXForwardPV.class,
                         argument("exposureConfigNames", ImmutableSet.of("Exposure Functions Test"))),
                function(ValuationTimeProvider.class,
                         argument("valuationTime", Instant.now())),
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
            implementations(FXForwardPVFunction.class, DiscountingFXForwardPV.class,
                            CurrencyPairsFunction.class, CurrencyPairs.class,
                            FinancialSecurityVisitor.class, FXForwardSecurityConverter.class,
                            InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class,
                            CurveSpecificationMarketDataProviderFunction.class, CurveSpecificationMarketDataProvider.class,
                            FXMatrixProviderFunction.class, FXMatrixProvider.class,
                            CurveDefinitionProviderFunction.class, CurveDefinitionProvider.class,
                            DiscountingMulticurveBundleProviderFunction.class, DiscountingMulticurveBundleProvider.class,
                            CurveSpecificationProviderFunction.class, CurveSpecificationProvider.class,
                            ValuationTimeProviderFunction.class, ValuationTimeProvider.class,
                            CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class,
                            HistoricalTimeSeriesProviderFunction.class, HistoricalTimeSeriesProvider.class));
  }

  private static ComponentMap componentMap(Class<?>... componentTypes) {
    Map<Class<?>, Object> compMap = Maps.newHashMap();
    for (Class<?> componentType : componentTypes) {
      compMap.put(componentType, mock(componentType));
    }
    return ComponentMap.of(compMap);
  }

  private static Map<MarketDataRequirement, Pair<MarketDataStatus, MarketDataValue>> loadMarketData() throws IOException {
    Properties properties = new Properties();
    try (BufferedReader reader = new BufferedReader(new FileReader("/Users/chris/Desktop/marketdata.properties"))) {
      properties.load(reader);
    }
    Map<MarketDataRequirement, Pair<MarketDataStatus, MarketDataValue>> data = Maps.newHashMap();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String id = (String) entry.getKey();
      String value = (String) entry.getValue();
      addValue(data, id, Double.valueOf(value));
    }
    return data;
  }

  private static Pair<MarketDataStatus, MarketDataValue> addValue(
      Map<MarketDataRequirement, Pair<MarketDataStatus, MarketDataValue>> marketData, String ticker, double value) {

    return marketData.put(
        new CurveNodeMarketDataRequirement(ExternalSchemes.bloombergTickerSecurityId(ticker), "Market_Value"),
        Pairs.<MarketDataStatus, MarketDataValue>of(MarketDataStatus.AVAILABLE, new SingleMarketDataValue(value)));
  }
}
