/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.columns;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutputs;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.EnumSet;
import java.util.Map;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Maps;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.link.SnapshotLink;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataSnapshot;
import com.opengamma.financial.analytics.isda.credit.YieldCurveDataSnapshot;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.sesame.DirectExecutorService;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.credit.snapshot.SnapshotCreditCurveDataProviderFn;
import com.opengamma.sesame.credit.snapshot.SnapshotYieldCurveDataProviderFn;
import com.opengamma.sesame.engine.CachingManager;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.DefaultCachingManager;
import com.opengamma.sesame.engine.FunctionService;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.scenarios.curvedata.FunctionTestUtils;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFn;

/**
 * Tests graph building for credit curve functions.
 */
@PrepareForTest(CreditCurveDataSnapshot.class)
public class StandardIsdaCompliantCreditCurveGraphTest extends PowerMockTestCase {
  
  private ViewFactory _viewFactory;

  @BeforeMethod
  public void beforeMethod() {
    
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

    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(IsdaCompliantYieldCurveFn.class,
                              IsdaCompliantCreditCurveFn.class);
    
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(DefaultIsdaCompliantYieldCurveFn.class,
                                      SnapshotYieldCurveDataProviderFn.class,
                                      SnapshotCreditCurveDataProviderFn.class,
                                      StandardIsdaCompliantCreditCurveFn.class
        );

    CachingManager cachingManager = new DefaultCachingManager(componentMap, FunctionTestUtils.createCache());
    _viewFactory = new ViewFactory(new DirectExecutorService(),
                                availableOutputs,
                                availableImplementations,
                                FunctionModelConfig.EMPTY,
                                EnumSet.noneOf(FunctionService.class),
                                cachingManager);
    
  }
  
  @Test
  public void testBuildYieldCurve() {
    
    ViewConfig config = createViewConfig();
    
    View view = _viewFactory.createView(config);
    
    FunctionModel functionModel = view.getFunctionModel("Yield curve");
    assertNotNull(functionModel);
    System.out.println(functionModel.prettyPrint());
    
  }

  @Test
  public void testBuildCreditCurve() {
    
    ViewConfig config = createViewConfig();
    
    View view = _viewFactory.createView(config);
    
    FunctionModel functionModel = view.getFunctionModel("Credit curve");
    assertNotNull(functionModel);
    System.out.println(functionModel.prettyPrint());
    
  }

  private ViewConfig createViewConfig() {

    ViewConfig viewConfig =
        configureView("Yield curve",
            config(
                arguments(
                    function(SnapshotYieldCurveDataProviderFn.class,
                             argument("snapshotLink", SnapshotLink.resolved(mock(YieldCurveDataSnapshot.class)))),
                     function(SnapshotCreditCurveDataProviderFn.class,
                              argument("snapshotLink", 
                                      SnapshotLink.resolved(mock(CreditCurveDataSnapshot.class)))))),
                 columns(),
                 nonPortfolioOutputs(
                   nonPortfolioOutput("Yield curve",
                       output(OutputNames.ISDA_YIELD_CURVE)),
                   nonPortfolioOutput("Credit curve",
                       output(OutputNames.ISDA_CREDIT_CURVE))));
    
    return viewConfig;

  }
  
  private static ComponentMap componentMap(Class<?>... componentTypes) {
    Map<Class<?>, Object> compMap = Maps.newHashMap();
    for (Class<?> componentType : componentTypes) {
      compMap.put(componentType, mock(componentType));
    }
    return ComponentMap.of(compMap);
  }
  
}
