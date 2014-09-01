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
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
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
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.CurrencyPairsFn;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.CurveNodeConverterFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveNodeConverterFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleResolverFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultFXReturnSeriesFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.DiscountingMulticurveBundleResolverFn;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.FXReturnSeriesFn;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.TimeSeriesReturnConverterFactory;
import com.opengamma.sesame.component.CurrencyPairSet;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.pnl.DefaultHistoricalPnLFXConverterFn;
import com.opengamma.sesame.pnl.HistoricalPnLFXConverterFn;
import com.opengamma.sesame.pnl.PnLPeriodBound;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.LocalDateRange;

@Test(groups = TestGroup.UNIT)
public class FxForwardPnlSeriesFnTest {

  @Test
  public void buildGraph() {
    FunctionMetadata calculatePnl = EngineUtils.createMetadata(FXForwardPnLSeriesFn.class, "calculatePnlSeries");
    FunctionModelConfig config = createFunctionConfig(mock(CurrencyMatrix.class));
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
    FunctionModel functionModel = FunctionModel.forFunction(calculatePnl, config, componentMap.getComponentTypes());
    Object fn = functionModel.build(new FunctionBuilder(), componentMap).getReceiver();
    assertTrue(fn instanceof FXForwardPnLSeriesFn);
    System.out.println(functionModel.prettyPrint(true));
  }

  public static FunctionModelConfig createFunctionConfig(CurrencyMatrix currencyMatrix) {
    ConfigLink<ExposureFunctions> exposureConfig = ConfigLink.resolved(mock(ExposureFunctions.class));
    LocalDateRange range = LocalDateRange.of(LocalDate.of(2013, 1, 1), LocalDate.of(2014, 1, 1), true);
    return
        config(
            arguments(
                function(ConfigDbMarketExposureSelectorFn.class,
                         argument("exposureConfig", exposureConfig)),
                function(DefaultHistoricalPnLFXConverterFn.class,
                         argument("periodBound", PnLPeriodBound.START)),
                function(DiscountingFXForwardSpotPnLSeriesFn.class,
                         argument("useHistoricalSpot", true),
                         argument("dateRange", range),
                         argument("outputCurrency", Optional.of(Currency.USD)),
                         argument("timeSeriesConverter", TimeSeriesReturnConverterFactory.absolute())),
                function(RootFinderConfiguration.class,
                         argument("rootFinderAbsoluteTolerance", 1e-9),
                         argument("rootFinderRelativeTolerance", 1e-9),
                         argument("rootFinderMaxIterations", 1000)),
                function(DefaultCurrencyPairsFn.class,
                         argument("currencyPairs", CurrencyPairSet.of(CurrencyPair.of(USD, JPY),
                                                                      CurrencyPair.of(EUR, USD),
                                                                      CurrencyPair.of(GBP, USD)))),
                function(DefaultHistoricalTimeSeriesFn.class,
                         argument("resolutionKey", "DEFAULT_TSS"),
                         argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1)))),
                function(DefaultDiscountingMulticurveBundleFn.class,
                         argument("impliedCurveNames", StringSet.of())),
                function(DefaultHistoricalMarketDataFn.class,
                         argument("dataSource", "BLOOMBERG"),
                         argument("currencyMatrix", currencyMatrix)),
                function(DefaultCurveNodeConverterFn.class,
                         argument("timeSeriesDuration", RetrievalPeriod.of(Period.ofYears(1)))),
                function(DefaultMarketDataFn.class,
                         argument("currencyMatrix", currencyMatrix))),

            implementations(FXForwardPnLSeriesFn.class, DiscountingFXForwardSpotPnLSeriesFn.class,
                            FXReturnSeriesFn.class, DefaultFXReturnSeriesFn.class,
                            FXForwardCalculatorFn.class, FXForwardDiscountingCalculatorFn.class,
                            DiscountingMulticurveCombinerFn.class, ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                            MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                            CurrencyPairsFn.class, DefaultCurrencyPairsFn.class,
                            FinancialSecurityVisitor.class, FXForwardSecurityConverter.class,
                            InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class,
                            CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                            FXMatrixFn.class, DefaultFXMatrixFn.class,
                            CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                            DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                            DiscountingMulticurveBundleResolverFn.class, DefaultDiscountingMulticurveBundleResolverFn.class,
                            CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                            CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class,
                            CurveNodeConverterFn.class, DefaultCurveNodeConverterFn.class,
                            HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class,
                            MarketDataFn.class, DefaultMarketDataFn.class,
                            HistoricalMarketDataFn.class, DefaultHistoricalMarketDataFn.class,
                            HistoricalPnLFXConverterFn.class, DefaultHistoricalPnLFXConverterFn.class));
  }

  private static ComponentMap componentMap(Class<?>... componentTypes) {
    Map<Class<?>, Object> compMap = Maps.newHashMap();
    for (Class<?> componentType : componentTypes) {
      compMap.put(componentType, mock(componentType));
    }
    return ComponentMap.of(compMap);
  }

}
