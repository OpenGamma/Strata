/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.core.link.ConfigLink;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.financial.analytics.conversion.FXForwardSecurityConverter;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.id.VersionCorrection;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveNodeConverterFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultFXReturnSeriesFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.EngineTestUtils;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.engine.CycleArguments;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.sesame.engine.ViewInputs;
import com.opengamma.sesame.engine.ViewInputsDeserializer;
import com.opengamma.sesame.engine.ViewInputsSerializer;
import com.opengamma.sesame.equity.DefaultEquityPresentValueFn;
import com.opengamma.sesame.equity.EquityPresentValueFn;
import com.opengamma.sesame.fra.FRAFn;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.fxforward.DiscountingFXForwardPVFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardSpotPnLSeriesFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardYCNSPnLSeriesFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardYieldCurveNodeSensitivitiesFn;
import com.opengamma.sesame.fxforward.FXForwardDiscountingCalculatorFn;
import com.opengamma.sesame.fxforward.FXForwardPVFn;
import com.opengamma.sesame.fxforward.FXForwardPnLSeriesFn;
import com.opengamma.sesame.fxforward.FXForwardYCNSPnLSeriesFn;
import com.opengamma.sesame.fxforward.FXForwardYieldCurveNodeSensitivitiesFn;
import com.opengamma.sesame.interestrate.InterestRateMockSources;
import com.opengamma.sesame.irs.InterestRateSwapFn;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FixedHistoricalMarketDataFactory;
import com.opengamma.sesame.marketdata.StrategyAwareMarketDataSource;
import com.opengamma.sesame.pnl.DefaultHistoricalPnLFXConverterFn;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * Tests that recording of data for a view, allows the view
 * to be reproduced using just that data.
 */
@Test(groups = TestGroup.UNIT)
public class RecordingDataTest {

  @Test
  public void testCurveViewCanBeSavedAndRead() {

    AvailableOutputs availableOutputs = createAvailableOutputs();
    AvailableImplementations availableImplementations = createAvailableImplementations();

    ViewFactory viewFactory = EngineTestUtils.createViewFactory(InterestRateMockSources.generateBaseComponents(),
                                                                availableOutputs,
                                                                availableImplementations);

    // Run view
    View view = viewFactory.createView(createCurveBundleConfig("TEST"));

    ZonedDateTime valTime = LocalDate.of(2014, 6, 1).atStartOfDay(ZoneOffset.UTC);

    StrategyAwareMarketDataSource marketDataSource =
        InterestRateMockSources.createMarketDataFactory().create(
            new FixedHistoricalMarketDataSpecification(valTime.toLocalDate()));

    VersionCorrection versionCorrection =
        ThreadLocalServiceContext.getInstance().get(VersionCorrectionProvider.class).getConfigVersionCorrection();
    CycleArguments cycleArguments = new CycleArguments(valTime, versionCorrection, marketDataSource, true);

    Results run = view.run(cycleArguments);
    Result<Object> result = run.getNonPortfolioResults().get("TEST").getResult();
    assertThat(result.isSuccess(), is(true));

    // Capture results
    ViewInputs viewInputs = run.getViewInputs();
    ViewInputsSerializer serializer = new ViewInputsSerializer(viewInputs);

    // Write results to stream
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1_000_000);
    serializer.serialize(baos);

    ViewInputsDeserializer deserializer = new ViewInputsDeserializer(new ByteArrayInputStream(baos.toByteArray()));
    ViewInputs viewInputs2 = deserializer.deserialize();
    assertThat(viewInputs2, is(notNullValue()));

    CapturedResultsLoader loader = new CapturedResultsLoader(viewInputs2, availableOutputs, availableImplementations);
    Results run22 = loader.runViewFromInputs();

    Result<Object> result2 = run22.getNonPortfolioResults().get("TEST").getResult();
    assertThat(result2.isSuccess(), is(true));

    // Check results are same as original ones
    assertThat(result2.getValue(), is(result.getValue()));
  }

  private ViewConfig createCurveBundleConfig(String curveBundleOutputName) {

    CurveConstructionConfiguration curveConstructionConfiguration =
        ConfigLink.resolvable("USD_ON-OIS_LIBOR3M-FRAIRS_1U", CurveConstructionConfiguration.class).resolve();

    return configureView("Curve Bundle only",
                         nonPortfolioOutput(curveBundleOutputName,
                                            output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE,
                                                   config(
                                                       arguments(
                                                           function(
                                                               RootFinderConfiguration.class,
                                                               argument("rootFinderAbsoluteTolerance", 1e-9),
                                                               argument("rootFinderRelativeTolerance", 1e-9),
                                                               argument("rootFinderMaxIterations", 1000)),
                                                           function(DefaultCurveNodeConverterFn.class,
                                                                    argument("timeSeriesDuration", RetrievalPeriod.of(
                                                                        Period.ofYears(1)))),
                                                           function(DefaultHistoricalMarketDataFn.class,
                                                                    argument("dataSource", "BLOOMBERG")),
                                                           function(
                                                               DefaultHistoricalTimeSeriesFn.class,
                                                               argument("resolutionKey", "DEFAULT_TSS"),
                                                               argument("htsRetrievalPeriod", RetrievalPeriod.of((Period.ofYears(1))))),
                                                           function(
                                                               DefaultDiscountingMulticurveBundleFn.class,
                                                               argument("impliedCurveNames", StringSet.of()),
                                                               argument("curveConfig", curveConstructionConfiguration)))))));
  }


  /**
   * Create the available outputs.
   *
   * @return the available outputs, not null
   */
  protected AvailableOutputs createAvailableOutputs() {
    AvailableOutputs available = new AvailableOutputsImpl();
    available.register(DiscountingMulticurveBundleFn.class,
                       EquityPresentValueFn.class,
                       FRAFn.class,
                       InterestRateSwapFn.class,
                       FXForwardPnLSeriesFn.class,
                       FXForwardPVFn.class,
                       FXForwardYCNSPnLSeriesFn.class,
                       FXForwardYieldCurveNodeSensitivitiesFn.class);
    return available;
  }

  /**
   * Create the available implementations.
   *
   * @return the available implementations, not null
   */
  protected AvailableImplementations createAvailableImplementations() {
    AvailableImplementations available = new AvailableImplementationsImpl();
    available.register(
        DiscountingFXForwardYieldCurveNodeSensitivitiesFn.class,
        DiscountingFXForwardSpotPnLSeriesFn.class,
        DiscountingFXForwardYCNSPnLSeriesFn.class,
        DiscountingFXForwardPVFn.class,
        DefaultFXReturnSeriesFn.class,
        DefaultCurrencyPairsFn.class,
        DefaultEquityPresentValueFn.class,
        FXForwardSecurityConverter.class,
        ConfigDBInstrumentExposuresProvider.class,
        DefaultCurveSpecificationMarketDataFn.class,
        DefaultFXMatrixFn.class,
        DefaultCurveDefinitionFn.class,
        DefaultDiscountingMulticurveBundleFn.class,
        DefaultCurveSpecificationFn.class,
        ConfigDBCurveConstructionConfigurationSource.class,
        DefaultHistoricalTimeSeriesFn.class,
        FXForwardDiscountingCalculatorFn.class,
        ConfigDbMarketExposureSelectorFn.class,
        ExposureFunctionsDiscountingMulticurveCombinerFn.class,
        FixedHistoricalMarketDataFactory.class,
        DefaultMarketDataFn.class,
        DefaultHistoricalMarketDataFn.class,
        DefaultCurveNodeConverterFn.class,
        DefaultHistoricalPnLFXConverterFn.class);
    return available;
  }
}
