/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.integration_tests;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.Sets;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.convention.frequency.PeriodFrequency;
import com.opengamma.financial.currency.CurrencyMatrix;
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
import com.opengamma.master.security.ManageableSecurity;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.DefaultCurveNodeConverterFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleResolverFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.irs.DiscountingInterestRateSwapCalculatorFactory;
import com.opengamma.sesame.irs.DiscountingInterestRateSwapFn;
import com.opengamma.sesame.irs.InterestRateSwapCalculatorFactory;
import com.opengamma.sesame.irs.InterestRateSwapFn;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.server.FunctionServer;
import com.opengamma.sesame.server.FunctionServerRequest;
import com.opengamma.sesame.server.IndividualCycleOptions;
import com.opengamma.sesame.server.RemoteFunctionServer;
import com.opengamma.util.GUIDGenerator;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;


/**
 * Tests that a view can be run against a remote server.
 * The tests cover the validation of a successful PV result
 * and a the curve bundle used to price the swap.
 */

@Test(groups = TestGroup.INTEGRATION, enabled = false)
public class RemoteInterestRateSwapTest {

  private static final String URL = "http://localhost:8080/jax";
  private static final String CURVE_RESULT = "Curve Bundle";
  private FunctionServer _functionServer;
  private IndividualCycleOptions _cycleOptions;
  private ConfigLink<ExposureFunctions> _exposureConfig;
  private ConfigLink<CurrencyMatrix> _currencyMatrixLink;
  private ConfigLink<CurveConstructionConfiguration> _curveConstructionConfiguration;
  private List<ManageableSecurity> _inputs = new ArrayList<>();


  @BeforeClass
  public void setUp() {

    /* Create a RemoteFunctionServer to executes view requests RESTfully.*/
    _functionServer = new RemoteFunctionServer(URI.create(URL));

    /* Single cycle options containing the market data specification and valuation time */
    _cycleOptions = IndividualCycleOptions.builder()
        .valuationTime(DateUtils.getUTCDate(2014, 1, 22))
        .marketDataSpec(LiveMarketDataSpecification.of("Bloomberg"))
        .build();

    /* Configuration links matching the curve exposure function, currency matrix and curve bundle
       as named on the remote server. These are needed as specific arguments in the creation
       of the ViewConfig. */
    _exposureConfig = ConfigLink.resolvable("USD CSA Exposure Functions", ExposureFunctions.class);
    _currencyMatrixLink = ConfigLink.resolvable("BloombergLiveData", CurrencyMatrix.class);
    _curveConstructionConfiguration = ConfigLink.resolvable("USD TO GBP CSA USD Curve Construction Configuration",
                                                            CurveConstructionConfiguration.class);

    /* Add a single Fixed vs Libor 3m Swap to the ManageableSecurity list */
    _inputs.add(createFixedVsLibor3mSwap());
  }

  @Test(enabled = false)
  public void testSingleSwapPVExecution() {

    /* Building the output specific request, based on a the view config, the single cycle options
       and the List<ManageableSecurity> containing a single swap */
    FunctionServerRequest<IndividualCycleOptions> request =
        FunctionServerRequest.<IndividualCycleOptions>builder()
            .viewConfig(createViewConfig(OutputNames.PRESENT_VALUE))
            .inputs(_inputs)
            .cycleOptions(_cycleOptions)
            .build();

    /* Execute the engine cycle and extract the result */
    Results results = _functionServer.executeSingleCycle(request);
    Result result = results.get(0,0).getResult();
    assertThat(result.isSuccess(), is(true));

  }

  @Test(enabled = false)
  public void testSingleSwapReceiveLegCashFlowsExecution() {

    FunctionServerRequest<IndividualCycleOptions> request =
        FunctionServerRequest.<IndividualCycleOptions>builder()
            .viewConfig(createViewConfig(OutputNames.RECEIVE_LEG_CASH_FLOWS))
            .inputs(_inputs)
            .cycleOptions(_cycleOptions)
            .build();

    Results results = _functionServer.executeSingleCycle(request);
    Result result = results.get(0,0).getResult();
    assertThat(result.isSuccess(), is(true));

  }

  @Test(enabled = false)
  public void testSingleSwapPayLegCashFlowsExecution() {

    FunctionServerRequest<IndividualCycleOptions> request =
        FunctionServerRequest.<IndividualCycleOptions>builder()
            .viewConfig(createViewConfig(OutputNames.PAY_LEG_CASH_FLOWS))
            .inputs(_inputs)
            .cycleOptions(_cycleOptions)
            .build();

    Results results = _functionServer.executeSingleCycle(request);
    Result result = results.get(0,0).getResult();
    assertThat(result.isSuccess(), is(true));

  }

  @Test(enabled = false)
  public void testSingleSwapBucketedPV01Execution() {

    FunctionServerRequest<IndividualCycleOptions> request =
        FunctionServerRequest.<IndividualCycleOptions>builder()
            .viewConfig(createViewConfig(OutputNames.BUCKETED_PV01))
            .inputs(_inputs)
            .cycleOptions(_cycleOptions)
            .build();

    Results results = _functionServer.executeSingleCycle(request);
    Result result = results.get(0,0).getResult();
    assertThat(result.isSuccess(), is(true));

  }

  @Test(enabled = false)
  public void testSingleSwapPV01Execution() {

    FunctionServerRequest<IndividualCycleOptions> request =
        FunctionServerRequest.<IndividualCycleOptions>builder()
            .viewConfig(createViewConfig(OutputNames.PV01))
            .inputs(_inputs)
            .cycleOptions(_cycleOptions)
            .build();

    Results results = _functionServer.executeSingleCycle(request);
    Result result = results.get(0,0).getResult();
    assertThat(result.isSuccess(), is(true));

  }

  @Test(enabled = false)
  public void testCurveBundleExecution() {

    FunctionServerRequest<IndividualCycleOptions> request =
        FunctionServerRequest.<IndividualCycleOptions>builder()
            .viewConfig(createCurveBundleConfig())
            .cycleOptions(_cycleOptions)
            .build();

    Results results = _functionServer.executeSingleCycle(request);
    Result result = results.getNonPortfolioResults().get(CURVE_RESULT).getResult();
    assertThat(result.isSuccess(), is(true));

  }

  /* Output specific view configuration for interest rate swaps */
  private ViewConfig createViewConfig(String output) {

    return
      configureView(
        "IRS Remote view",
        column(OutputNames.PRESENT_VALUE,
          config(
              arguments(
                  function(ConfigDbMarketExposureSelectorFn.class, argument("exposureConfig", _exposureConfig)),
                  function(
                      RootFinderConfiguration.class,
                      argument("rootFinderAbsoluteTolerance", 1e-9),
                      argument("rootFinderRelativeTolerance", 1e-9),
                      argument("rootFinderMaxIterations", 1000)),
                  function(DefaultCurveNodeConverterFn.class,
                           argument("timeSeriesDuration", RetrievalPeriod.of(Period.ofYears(1)))),
                  function(DefaultHistoricalMarketDataFn.class,
                           argument("dataSource", "BLOOMBERG"),
                           argument("currencyMatrix", _currencyMatrixLink)),
                  function(DefaultMarketDataFn.class,
                           argument("dataSource", "BLOOMBERG"),
                           argument("currencyMatrix", _currencyMatrixLink)),
                  function(
                      DefaultHistoricalTimeSeriesFn.class,
                      argument("resolutionKey", "DEFAULT_TSS"),
                      argument("htsRetrievalPeriod", RetrievalPeriod.of((Period.ofYears(1))))),
                  function(
                      DefaultDiscountingMulticurveBundleFn.class,
                      argument("impliedCurveNames", StringSet.of()))),
              implementations(InterestRateSwapFn.class, DiscountingInterestRateSwapFn.class,
                              InterestRateSwapCalculatorFactory.class, DiscountingInterestRateSwapCalculatorFactory.class)
          ),
          output(output, InterestRateSwapSecurity.class)
        )
      );
  }

  /* A non portfolio output view configuration to capture the build curves */
  private ViewConfig createCurveBundleConfig() {

    return configureView(
             "Curve Bundle only",
             nonPortfolioOutput(
                 CURVE_RESULT,
                  output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE,
                         config(
                             arguments(
                                 function(
                                     RootFinderConfiguration.class,
                                     argument("rootFinderAbsoluteTolerance", 1e-9),
                                     argument("rootFinderRelativeTolerance", 1e-9),
                                     argument("rootFinderMaxIterations", 1000)),
                                 function(DefaultCurveNodeConverterFn.class,
                                          argument("timeSeriesDuration", RetrievalPeriod.of(Period.ofYears(1)))),
                                 function(DefaultHistoricalMarketDataFn.class,
                                          argument("dataSource", "BLOOMBERG"),
                                          argument("currencyMatrix", _currencyMatrixLink)),
                                 function(DefaultMarketDataFn.class,
                                          argument("dataSource", "BLOOMBERG"),
                                          argument("currencyMatrix", _currencyMatrixLink)),
                                 function(
                                     DefaultHistoricalTimeSeriesFn.class,
                                     argument("resolutionKey", "DEFAULT_TSS"),
                                     argument("htsRetrievalPeriod", RetrievalPeriod.of((Period.ofYears(1))))),
                                 function(
                                     DefaultDiscountingMulticurveBundleResolverFn.class,
                                     argument("curveConfig", _curveConstructionConfiguration)),
                                 function(
                                     DefaultDiscountingMulticurveBundleFn.class,
                                     argument("impliedCurveNames", StringSet.of())))))
             ));
  }

  private InterestRateSwapSecurity createFixedVsLibor3mSwap() {

    InterestRateSwapNotional notional = new InterestRateSwapNotional(Currency.USD, 1_000_000);
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
    payLeg.setAccrualPeriodFrequency(freq6m);
    payLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setMaturityDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
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
    receiveLeg.setAccrualPeriodFrequency(freq3m);
    receiveLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setAccrualPeriodCalendars(calendarUSNY);
    receiveLeg.setResetPeriodFrequency(freq3m);
    receiveLeg.setResetPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setResetPeriodCalendars(calendarUSNY);
    receiveLeg.setFixingDateBusinessDayConvention(BusinessDayConventions.PRECEDING);
    receiveLeg.setMaturityDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setFixingDateCalendars(calendarUSNY);
    receiveLeg.setFixingDateOffset(-2);
    receiveLeg.setFloatingRateType(FloatingRateType.IBOR);
    receiveLeg.setFloatingReferenceRateId(ExternalId.of("BLOOMBERG_TICKER", "US0003M Index"));
    receiveLeg.setPayReceiveType(PayReceiveType.RECEIVE);

    legs.add(receiveLeg);

    return new InterestRateSwapSecurity(
        ExternalIdBundle.of(ExternalId.of("UUID", GUIDGenerator.generate().toString())),
        "Fixed vs Libor 3m",
        LocalDate.of(2014, 9, 12), // effective date
        LocalDate.of(2021, 9, 12), // maturity date,
        legs);
  }

}
