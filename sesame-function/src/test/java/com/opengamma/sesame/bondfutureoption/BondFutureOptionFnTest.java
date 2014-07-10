/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfutureoption;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.fail;

import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.historicaltimeseries.impl.SimpleHistoricalTimeSeries;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.security.bond.BondSecurity;
import com.opengamma.financial.security.future.BondFutureSecurity;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.ExposureFunctionsIssuerProviderFn;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.InterpolatedIssuerBundleFn;
import com.opengamma.sesame.IssuerProviderBundleFn;
import com.opengamma.sesame.IssuerProviderFn;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.bond.BondMockSources;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.trade.BondFutureOptionTrade;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;

/**
 * Test for bond future options using the black calculator.
 */
@Test(groups = TestGroup.UNIT)
public class BondFutureOptionFnTest {

  private static final ZonedDateTime VALUATION_TIME = DateUtils.getUTCDate(2014, 1, 22);
  
  private static final Environment ENV =
      new SimpleEnvironment(VALUATION_TIME,
                            BondMockSources.createMarketDataSource());
  
  private BondFutureOptionFn _bondFutureOptionFn;
  
  private BondSecurity _bond = BondMockSources.createBondSecurity();
  
  private BondFutureSecurity _bondFuture = BondMockSources.createBondFutureSecurity();
  
  private BondFutureOptionTrade _bondFutureOptionTrade = BondMockSources.createBondFutureOptionTrade();

  @BeforeClass
  public void setUp() {
    FunctionModelConfig config = config(
      arguments(
                function(ConfigDbMarketExposureSelectorFn.class,
                         argument("exposureConfig", ConfigLink.resolvable(BondMockSources.BOND_EXPOSURE_FUNCTIONS, ExposureFunctions.class))),
                function(RootFinderConfiguration.class,
                         argument("rootFinderAbsoluteTolerance", 1e-9),
                         argument("rootFinderRelativeTolerance", 1e-9),
                         argument("rootFinderMaxIterations", 1000)),
                function(DefaultDiscountingMulticurveBundleFn.class,
                         argument("impliedCurveNames", StringSet.of())),
                function(DefaultHistoricalTimeSeriesFn.class,
                         argument("resolutionKey", "DEFAULT_TSS"),
                         argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1)))
                )
      ),
      implementations(BondFutureOptionFn.class, DefaultBondFutureOptionFn.class,
                      BondFutureOptionCalculatorFactory.class, BondFutureOptionBlackCalculatorFactory.class,
                      CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                      FXMatrixFn.class, DefaultFXMatrixFn.class,
                      BlackBondFuturesProviderFn.class, TestBlackBondFuturesProviderFn.class,
                      DiscountingMulticurveCombinerFn.class, ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                      IssuerProviderFn.class, ExposureFunctionsIssuerProviderFn.class,
                      IssuerProviderBundleFn.class, InterpolatedIssuerBundleFn.class,
                      CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                      DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                      CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                      CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class,
                      HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class,
                      MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                      MarketDataFn.class, DefaultMarketDataFn.class)
    );

    ImmutableMap<Class<?>, Object> components = generateComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);
    
    _bondFutureOptionFn = FunctionModel.build(BondFutureOptionFn.class, config, ComponentMap.of(components));
  }
  
  private ImmutableMap<Class<?>, Object> generateComponents() {
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    for (Map.Entry<Class<?>, Object> keys: BondMockSources.generateBaseComponents().entrySet()) {
      if (keys.getKey().equals(HistoricalTimeSeriesSource.class)) {
        appendHistoricalTimeSeriesSourceMock((HistoricalTimeSeriesSource) keys.getValue());
      }
      if (keys.getKey().equals(SecuritySource.class)) {
        appendSecuritySourceMock((SecuritySource) keys.getValue());
      }
      builder.put(keys.getKey(), keys.getValue());
    }
    return builder.build();
  }
  
  private void appendHistoricalTimeSeriesSourceMock(HistoricalTimeSeriesSource mock) {
    HistoricalTimeSeries irFuturePrices = new SimpleHistoricalTimeSeries(UniqueId.of("Blah", "1"), ImmutableLocalDateDoubleTimeSeries.of(VALUATION_TIME.toLocalDate(), 0.975));
    when(mock.getHistoricalTimeSeries(eq(MarketDataRequirementNames.MARKET_VALUE),
                                      any(ExternalIdBundle.class),
                                      eq("DEFAULT_TSS"),
                                      any(LocalDate.class),
                                      eq(true),
                                      any(LocalDate.class),
                                      eq(true))).thenReturn(irFuturePrices);
  }
  
  private void appendSecuritySourceMock(SecuritySource mock) {
    when(mock.getSingle(eq(_bond.getExternalIdBundle()))).thenReturn(_bond);
    when(mock.getSingle(eq(_bondFuture.getExternalIdBundle()))).thenReturn(_bondFuture);
  }

  @Test
  public void testPresentValue() {
    Result<MultipleCurrencyAmount> pvComputed = _bondFutureOptionFn.calculatePV(ENV, _bondFutureOptionTrade);
    if (!pvComputed.isSuccess()) {
      fail(pvComputed.getFailureMessage());
    }
  }
}
