/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.financial.analytics.conversion.FXForwardSecurityConverter;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.convention.frequency.PeriodFrequency;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.currency.SimpleCurrencyMatrix;
import com.opengamma.financial.security.irs.FixedInterestRateSwapLeg;
import com.opengamma.financial.security.irs.FloatingInterestRateSwapLeg;
import com.opengamma.financial.security.irs.InterestRateSwapLeg;
import com.opengamma.financial.security.irs.InterestRateSwapNotional;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.financial.security.irs.PayReceiveType;
import com.opengamma.financial.security.irs.Rate;
import com.opengamma.financial.security.swap.FloatingRateType;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
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
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleResolverFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultFXReturnSeriesFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DiscountingMulticurveBundleResolverFn;
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
import com.opengamma.sesame.engine.ViewOutputs;
import com.opengamma.sesame.engine.ViewResultsDeserializer;
import com.opengamma.sesame.engine.ViewResultsSerializer;
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
import com.opengamma.sesame.irs.DiscountingInterestRateSwapCalculatorFactory;
import com.opengamma.sesame.irs.DiscountingInterestRateSwapFn;
import com.opengamma.sesame.irs.InterestRateSwapFn;
import com.opengamma.sesame.marketdata.CycleMarketDataFactory;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FixedHistoricalMarketDataFactory;
import com.opengamma.sesame.marketdata.StrategyAwareMarketDataSource;
import com.opengamma.sesame.pnl.DefaultHistoricalPnLFXConverterFn;
import com.opengamma.util.GUIDGenerator;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * Tests that recording of data for a view, allows the view
 * to be reproduced using just that data.
 */
@Test(groups = TestGroup.UNIT)
public class RecordingDataTest {

  private ConfigLink<CurrencyMatrix> _currencyMatrixLink =
      ConfigLink.resolvable("EmptyCurrencyMatrix", CurrencyMatrix.class);

  @Test
  public void testCurveViewCanBeSavedAndRead() {

    AvailableOutputs availableOutputs = createAvailableOutputs();
    AvailableImplementations availableImplementations = createAvailableImplementations();

    ImmutableMap<Class<?>, Object> components = InterestRateMockSources.generateBaseComponents();

    ViewFactory viewFactory = EngineTestUtils.createViewFactory(components,
                                                                availableOutputs,
                                                                availableImplementations);


    View view = viewFactory.createView(createCurveBundleConfig("TEST"));

    ZonedDateTime valTime = LocalDate.of(2014, 6, 1).atStartOfDay(ZoneOffset.UTC);

    StrategyAwareMarketDataSource marketDataSource =
        InterestRateMockSources.createMarketDataFactory().create(
            new FixedHistoricalMarketDataSpecification(valTime.toLocalDate()));

    CycleMarketDataFactory cycleMarketDataFactory = mock(CycleMarketDataFactory.class);
    when(cycleMarketDataFactory.getPrimaryMarketDataSource()).thenReturn(marketDataSource);

    VersionCorrection versionCorrection =
        ThreadLocalServiceContext.getInstance().get(VersionCorrectionProvider.class).getConfigVersionCorrection();
    CycleArguments cycleArguments = new CycleArguments(valTime, versionCorrection, cycleMarketDataFactory, true);

    Results run = view.run(cycleArguments);
    Result<Object> result = run.getNonPortfolioResults().get("TEST").getResult();
    if (!result.isSuccess()) {
      fail(result.getFailureMessage());
    }

    // Capture results
    ViewResultsSerializer serializer = new ViewResultsSerializer(run);

    // Write inputs to stream
    ByteArrayOutputStream inputsBaos = new ByteArrayOutputStream(1_000_000);
    serializer.serializeViewInputs(inputsBaos);

    ByteArrayOutputStream outputsBaos = new ByteArrayOutputStream(1_000_000);
    serializer.serializeViewOutputs(outputsBaos);

    ViewResultsDeserializer inputsDeserializer =
        new ViewResultsDeserializer(new ByteArrayInputStream(inputsBaos.toByteArray()));
    ViewInputs viewInputs2 = inputsDeserializer.deserialize(ViewInputs.class);
    assertThat(viewInputs2, is(notNullValue()));

    ViewResultsDeserializer outputsDeserializer =
        new ViewResultsDeserializer(new ByteArrayInputStream(outputsBaos.toByteArray()));
    ViewOutputs viewOutputs = outputsDeserializer.deserialize(ViewOutputs.class);
    assertThat(viewOutputs, is(notNullValue()));

    CapturedResultsLoader loader = new CapturedResultsLoader(viewInputs2, availableOutputs, availableImplementations);
    loader.addExtraConfigData("EmptyCurrencyMatrix", ConfigItem.of(new SimpleCurrencyMatrix()));
    Results run2 = loader.runViewFromInputs();

    Result<Object> result2 = run2.getNonPortfolioResults().get("TEST").getResult();
    if (!result2.isSuccess()) {
      fail(result2.toString());
    }

    // Check results are same as original ones
    ViewOutputs view2Outputs = ViewOutputs.builder()
        .nonPortfolioResults(run2.getNonPortfolioResults())
        .columnNames(run2.getColumnNames())
        .rows(run2.getRows())
        .build();

    assertThat(result2.getValue(), is(result.getValue()));
    assertThat(view2Outputs, is(viewOutputs));
  }

  @Test
  public void testPricingMethodCanBeCaptured() {

    // Test that we can capture results when we have trades/securities involved
    AvailableOutputs availableOutputs = createAvailableOutputs();
    AvailableImplementations availableImplementations = createAvailableImplementations();

    ViewFactory viewFactory = EngineTestUtils.createViewFactory(InterestRateMockSources.generateBaseComponents(),
                                                                availableOutputs,
                                                                availableImplementations);

    // Run view
    View view = viewFactory.createView(createIrsPricerConfig(), InterestRateSwapSecurity.class);

    ZonedDateTime valTime = LocalDate.of(2014, 6, 1).atStartOfDay(ZoneOffset.UTC);

    StrategyAwareMarketDataSource marketDataSource =
        InterestRateMockSources.createMarketDataFactory().create(
            new FixedHistoricalMarketDataSpecification(valTime.toLocalDate()));

    CycleMarketDataFactory cycleMarketDataFactory = mock(CycleMarketDataFactory.class);
    when(cycleMarketDataFactory.getPrimaryMarketDataSource()).thenReturn(marketDataSource);

    VersionCorrection versionCorrection =
        ThreadLocalServiceContext.getInstance().get(VersionCorrectionProvider.class).getConfigVersionCorrection();
    CycleArguments cycleArguments = new CycleArguments(valTime, versionCorrection, cycleMarketDataFactory, true);

    Results run = view.run(cycleArguments, ImmutableList.of(createFixedVsLibor3mSwap()));
    Result<Object> pvResult = run.get(0, 0).getResult();
    if (!pvResult.isSuccess()) {
      fail(pvResult.toString());
    }
    Result<Object> pv01Result = run.get(0, 1).getResult();
    if (!pv01Result.isSuccess()) {
      fail(pv01Result.toString());
    }

    // Capture results
    ViewResultsSerializer serializer = new ViewResultsSerializer(run);

    // Write inputs to stream
    ByteArrayOutputStream inputsBaos = new ByteArrayOutputStream(1_000_000);
    serializer.serializeViewInputs(inputsBaos);

    ByteArrayOutputStream outputsBaos = new ByteArrayOutputStream(1_000_000);
    serializer.serializeViewOutputs(outputsBaos);

    ViewResultsDeserializer inputsDeserializer =
        new ViewResultsDeserializer(new ByteArrayInputStream(inputsBaos.toByteArray()));
    ViewInputs viewInputs = inputsDeserializer.deserialize(ViewInputs.class);
    assertThat(viewInputs, is(notNullValue()));

    ViewResultsDeserializer outputsDeserializer =
        new ViewResultsDeserializer(new ByteArrayInputStream(outputsBaos.toByteArray()));
    ViewOutputs viewOutputs = outputsDeserializer.deserialize(ViewOutputs.class);
    assertThat(viewOutputs, is(notNullValue()));

    CapturedResultsLoader loader = new CapturedResultsLoader(viewInputs, availableOutputs, availableImplementations);
    loader.addExtraConfigData("EmptyCurrencyMatrix", ConfigItem.of(new SimpleCurrencyMatrix()));
    Results run2 = loader.runViewFromInputs();
    System.out.println(run2);

    Result<Object> pvResult2 = run2.get(0, 0).getResult();
    if (!pvResult2.isSuccess()) {
      fail(pvResult2.toString());
    }
    Result<Object> pv01Result2 = run2.get(0, 1).getResult();
    if (!pv01Result2.isSuccess()) {
      fail(pv01Result2.toString());
    }

    ViewOutputs view2Outputs = ViewOutputs.builder()
        .nonPortfolioResults(run2.getNonPortfolioResults())
        .columnNames(run2.getColumnNames())
        .rows(run2.getRows())
        .build();

    // Check results are same as original ones
    assertThat(pvResult2.getValue(), is(pvResult.getValue()));
    assertThat(pv01Result2.getValue(), is(pv01Result.getValue()));

    // TODO - this should work but seems to be prevented by a Fudge issue
    // assertThat(view2Outputs, is(viewOutputs));
  }

  private InterestRateSwapSecurity createFixedVsLibor3mSwap() {

    InterestRateSwapNotional notional = new InterestRateSwapNotional(Currency.USD, 100_000_000);
    PeriodFrequency freq6m = PeriodFrequency.of(Period.ofMonths(6));
    PeriodFrequency freq3m = PeriodFrequency.of(Period.ofMonths(3));
    Set<ExternalId> calendarUSNY = Sets.newHashSet(ExternalId.of(ExternalSchemes.ISDA_HOLIDAY, "USNY"));
    List<InterestRateSwapLeg> legs = new ArrayList<>();

    FixedInterestRateSwapLeg payLeg = new FixedInterestRateSwapLeg();
    payLeg.setNotional(notional);
    payLeg.setDayCountConvention(DayCounts.THIRTY_U_360);
    payLeg.setPaymentDateFrequency(freq6m);
    payLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setPaymentDateCalendars(calendarUSNY);
    payLeg.setMaturityDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setAccrualPeriodFrequency(freq6m);
    payLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setAccrualPeriodCalendars(calendarUSNY);
    payLeg.setRate(new Rate(0.0150));
    payLeg.setPayReceiveType(PayReceiveType.PAY);
    legs.add(payLeg);

    FloatingInterestRateSwapLeg receiveLeg = new FloatingInterestRateSwapLeg();
    receiveLeg.setNotional(notional);
    receiveLeg.setDayCountConvention(DayCounts.ACT_360);
    receiveLeg.setPaymentDateFrequency(freq3m);
    receiveLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setPaymentDateCalendars(calendarUSNY);
    receiveLeg.setMaturityDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setAccrualPeriodFrequency(freq3m);
    receiveLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setAccrualPeriodCalendars(calendarUSNY);
    receiveLeg.setResetPeriodFrequency(freq3m);
    receiveLeg.setResetPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setResetPeriodCalendars(calendarUSNY);
    receiveLeg.setFixingDateBusinessDayConvention(BusinessDayConventions.PRECEDING);
    receiveLeg.setFixingDateCalendars(calendarUSNY);
    receiveLeg.setFixingDateOffset(-2);
    receiveLeg.setFloatingRateType(FloatingRateType.IBOR);
    receiveLeg.setFloatingReferenceRateId(InterestRateMockSources.getLiborIndexId());
    receiveLeg.setPayReceiveType(PayReceiveType.RECEIVE);

    legs.add(receiveLeg);

    return new InterestRateSwapSecurity(
        ExternalIdBundle.of(ExternalId.of("UUID", GUIDGenerator.generate().toString())),
        "Fixed vs Libor 3m",
        LocalDate.of(2014, 9, 12), // effective date
        LocalDate.of(2021, 9, 12), // maturity date,
        legs);
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
                                                           function(DefaultMarketDataFn.class,
                                                                    argument("currencyMatrix", _currencyMatrixLink)),
                                                           function(DefaultHistoricalMarketDataFn.class,
                                                                    argument("dataSource", "BLOOMBERG"),
                                                                    argument("currencyMatrix", _currencyMatrixLink)),
                                                           function(
                                                               DefaultHistoricalTimeSeriesFn.class,
                                                               argument("resolutionKey", "DEFAULT_TSS"),
                                                               argument("htsRetrievalPeriod", RetrievalPeriod.of((Period.ofYears(1))))),
                                                           function(
                                                               DefaultDiscountingMulticurveBundleResolverFn.class,
                                                               argument("curveConfig", curveConstructionConfiguration)),
                                                           function(
                                                               DefaultDiscountingMulticurveBundleFn.class,
                                                               argument("impliedCurveNames", StringSet.of())))))));
  }

  private ViewConfig createIrsPricerConfig() {

    return configureView("IRS Pricer",
        config(
            arguments(
                function(ConfigDbMarketExposureSelectorFn.class,
                         argument("exposureConfig",
                                  ConfigLink.resolved(InterestRateMockSources.mockExposureFunctions()))),
                function(RootFinderConfiguration.class,
                         argument("rootFinderAbsoluteTolerance", 1e-10),
                         argument("rootFinderRelativeTolerance", 1e-10),
                         argument("rootFinderMaxIterations", 5000)),
                function(DefaultCurrencyPairsFn.class,
                         argument("currencyPairs", ImmutableSet.of(/*no pairs*/))),
                function(DefaultHistoricalTimeSeriesFn.class,
                         argument("resolutionKey", "DEFAULT_TSS"),
                         argument("htsRetrievalPeriod",  RetrievalPeriod.of(Period.ofYears(1)))),
                function(DefaultCurveNodeConverterFn.class,
                         argument("timeSeriesDuration", RetrievalPeriod.of(Period.ofYears(1)))),
                function(DefaultHistoricalMarketDataFn.class,
                         argument("dataSource", "BLOOMBERG"),
                         argument("currencyMatrix", _currencyMatrixLink)),
                function(DefaultMarketDataFn.class,
                         argument("currencyMatrix", _currencyMatrixLink)),
                function(DefaultDiscountingMulticurveBundleFn.class,
                         argument("impliedCurveNames", StringSet.of())))),
        column(
            "Present Value",
            output(OutputNames.PRESENT_VALUE, InterestRateSwapSecurity.class)),
        column(
            "PV01",
            output(OutputNames.PV01, InterestRateSwapSecurity.class)));
  }


  /**
   * Create the available outputs.
   *
   * @return the available outputs, not null
   */
  protected AvailableOutputs createAvailableOutputs() {
    AvailableOutputs available = new AvailableOutputsImpl();
    available.register(DiscountingMulticurveBundleResolverFn.class,
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
        DiscountingInterestRateSwapFn.class,
        DiscountingInterestRateSwapCalculatorFactory.class,
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
        DefaultDiscountingMulticurveBundleResolverFn.class,
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
