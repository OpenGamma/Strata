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
import static com.opengamma.util.money.Currency.USD;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;

import org.testng.annotations.Test;
import org.threeten.bp.Instant;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.holiday.HolidaySource;
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
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.util.test.TestGroup;

/**
 *
 */
@Test(groups = TestGroup.UNIT)
public class FXForwardPVFunctionTest {

  @Test
  public void buildGraph() {
    FunctionMetadata calculatePV = ConfigUtils.createMetadata(FXForwardPVFunction.class, "calculatePV");
    FunctionConfig config =
        config(
            implementations(FXForwardPVFunction.class, DiscountingFXForwardPV.class,
                            CurrencyPairsFunction.class, CurrencyPairs.class,
                            MarketDataProviderFunction.class, MarketDataProvider.class,
                            FinancialSecurityVisitor.class, FXForwardSecurityConverter.class,
                            InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class,
                            DiscountingMulticurveBundleProviderFunction.class, DiscountingMulticurveBundleProvider.class,
                            CurveSpecificationProviderFunction.class, CurveSpecificationProvider.class,
                            ValuationTimeProviderFunction.class, ValuationTimeProvider.class,
                            CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class),
            arguments(
                function(ValuationTimeProvider.class,
                         argument("valuationTime", Instant.now())),
                function(RootFinderConfiguration.class,
                         argument("rootFinderAbsoluteTolerance", 1d),
                         argument("rootFinderRelativeTolerance", 1d),
                         argument("rootFinderMaxIterations", 1)),
                function(CurrencyPairs.class,
                         argument("currencyPairs", ImmutableSet.of(CurrencyPair.of(EUR, USD), CurrencyPair.of(GBP, USD))))));
    ComponentMap componentMap = componentMap(ConfigSource.class,
                                             ConventionSource.class,
                                             ConventionBundleSource.class,
                                             HistoricalTimeSeriesResolver.class,
                                             SecuritySource.class,
                                             HolidaySource.class,
                                             RegionSource.class);
    GraphConfig graphConfig = new GraphConfig(config, componentMap, NodeDecorator.IDENTITY);
    FunctionModel functionModel = FunctionModel.forFunction(calculatePV, graphConfig);
    Object fn = functionModel.build(componentMap).getReceiver();
    assertTrue(fn instanceof FXForwardPVFunction);
  }

  private static ComponentMap componentMap(Class<?>... componentTypes) {
    Map<Class<?>, Object> compMap = Maps.newHashMap();
    for (Class<?> componentType : componentTypes) {
      compMap.put(componentType, mock(componentType));
    }
    return ComponentMap.of(compMap);
  }
}
