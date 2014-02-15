/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static com.opengamma.sesame.config.ConfigBuilder.viewDef;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalTime;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.holiday.impl.WeekendHolidaySource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.region.impl.SimpleRegion;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveNodeIdMapper;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.DiscountingCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.IborCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.InterpolatedCurveDefinition;
import com.opengamma.financial.analytics.curve.OvernightCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.analytics.ircurve.CurveInstrumentProvider;
import com.opengamma.financial.analytics.ircurve.StaticCurveInstrumentProvider;
import com.opengamma.financial.analytics.ircurve.strips.CashNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.FRANode;
import com.opengamma.financial.analytics.ircurve.strips.SwapNode;
import com.opengamma.financial.convention.DepositConvention;
import com.opengamma.financial.convention.FinancialConvention;
import com.opengamma.financial.convention.IborIndexConvention;
import com.opengamma.financial.convention.OISLegConvention;
import com.opengamma.financial.convention.OvernightIndexConvention;
import com.opengamma.financial.convention.StubType;
import com.opengamma.financial.convention.SwapFixedLegConvention;
import com.opengamma.financial.convention.VanillaIborLegConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.CycleArguments;
import com.opengamma.sesame.engine.Engine;
import com.opengamma.sesame.engine.EngineService;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.View;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.marketdata.CurveNodeMarketDataRequirement;
import com.opengamma.sesame.marketdata.DefaultResettableMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataItem;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.SimpleMarketDataFactory;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Tenor;

import net.sf.ehcache.CacheManager;

@Test(groups = TestGroup.UNIT)
public class FRAFnTest {

  private static final ChangeManager MOCK_CHANGE_MANAGER = mock(ChangeManager.class);
  private static final ZonedDateTime STD_REFERENCE_DATE = DateUtils.getUTCDate(2014, 1, 22);
  private static final ZonedDateTime STD_ACCRUAL_START_DATE = DateUtils.getUTCDate(2014, 9, 12);
  private static final ZonedDateTime STD_ACCRUAL_END_DATE = DateUtils.getUTCDate(2014, 12, 12);

  private static final String USD_DISC_MAPPER = "Test USD Discounting Mapper";
  private static final String USD_DISC_OVERNIGHT_MAPPER = "Test USD Discounting Overnight Mapper";
  private static final String LIBOR_3M_MAPPER = "Test 3m Libor Mapper";
  
  private static final String DISC_LEG_CONVENTION =  "USD 1Y Pay Lag Fixed Leg";
  private static final String DISC_RECEIVE_LEG_CONVENTION = "USD OIS Overnight Leg";
  private static final String DISC_CONVENTION =  "USD DepositON";
  private static final String LIBOR_PAY_LEG_CONVENTION =  "USD IRS Fixed Leg";
  private static final String LIBOR_RECEIVE_LEG_CONVENTION = "USD 3M IRS Ibor Leg";
  private static final String LIBOR_CONVENTION =  "USD Libor";
  private static final String USD_OVERNIGHT_CONVENTION =  "USD Overnight";

  private static final String LIBOR_CURVE_NAME ="USD-LIBOR3M-FRAIRS";
  private static final String ON_CURVE_NAME ="USD-ON-OIS";

  private static final ExternalId _discPayLegConventionId = ExternalId.of("CONVENTION", DISC_LEG_CONVENTION);
  private static final ExternalId _discReceiveLegConventionId = ExternalId.of("CONVENTION", DISC_RECEIVE_LEG_CONVENTION);
  private static final ExternalId _discConventionId = ExternalId.of("CONVENTION", DISC_CONVENTION);
  private static final ExternalId _liborPayLegConventionId = ExternalId.of("CONVENTION", LIBOR_PAY_LEG_CONVENTION);
  private static final ExternalId _liborReceiveLegConventionId = ExternalId.of("CONVENTION", LIBOR_RECEIVE_LEG_CONVENTION);
  private static final ExternalId _liborConventionId = ExternalId.of("CONVENTION", LIBOR_CONVENTION);
  private static final ExternalId _onConventionId = ExternalId.of("CONVENTION", USD_OVERNIGHT_CONVENTION);

  private static final String CURVE_CONSTRUCTION_CONFIGURATION = "USD_ON-OIS_LIBOR3M-FRAIRS_1U";

  private static final double STD_TOLERANCE_PV = 1.0E-3;
  private static final double STD_TOLERANCE_RATE = 1.0E-5;

  private static ExternalId s_USID = ExternalSchemes.financialRegionId("US");
  private static ExternalId s_USGBID = ExternalSchemes.financialRegionId("US+GB");
  private static Currency s_USD = Currency.USD;

  private Results _results;


  @BeforeClass
  public void setUpClass() throws IOException {
    FunctionConfig config = config(
        arguments(
            function(ConfigDbMarketExposureSelectorFn.class,
                     argument("exposureConfig", ConfigLink.of("Test USD", mockExposureFunctions()))),
            function(RootFinderConfiguration.class,
                     argument("rootFinderAbsoluteTolerance", 1e-9),
                     argument("rootFinderRelativeTolerance", 1e-9),
                     argument("rootFinderMaxIterations", 1000)),
            function(DefaultCurrencyPairsFn.class,
                     argument("currencyPairs", ImmutableSet.of(/*no pairs*/))),
            function(DefaultHistoricalTimeSeriesFn.class,
                     argument("resolutionKey", "DEFAULT_TSS"),
                     argument("htsRetrievalPeriod", Period.ofYears(1))),
            function(DefaultDiscountingMulticurveBundleFn.class,
                     argument("impliedCurveNames", ImmutableSet.of()))
        ));
    ViewDef viewDef =
        viewDef("FX forward PV view",
                column("Present Value", output(OutputNames.PRESENT_VALUE, FRASecurity.class, config)),
                column("Par Rate", output(OutputNames.PAR_RATE, FRASecurity.class, config))
        );

    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);

    Map<Class<?>, Object> componentMap = generateComponentMap(mockHolidaySource(),
                                                              mockRegionSource(),
                                                              mockConventionSource(),
                                                              mockConfigSource(),
                                                              mockSecuritySource(),
                                                              mockHistoricalTimeSeriesSource());

    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    final ServiceContext serviceContext = ServiceContext.of(componentMap).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(FRAPVFn.class);
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(DiscountingFRAPVFn.class,
                                      DefaultCurrencyPairsFn.class,
                                      ConfigDBInstrumentExposuresProvider.class,
                                      FRADiscountingCalculatorFn.class,
                                      FRACalculatorFactory.class,
                                      DefaultCurveSpecificationMarketDataFn.class,
                                      DefaultFXMatrixFn.class,
                                      ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                                      DefaultCurveDefinitionFn.class,
                                      DefaultDiscountingMulticurveBundleFn.class,
                                      DefaultCurveSpecificationFn.class,
                                      ConfigDBCurveConstructionConfigurationSource.class,
                                      DefaultHistoricalTimeSeriesFn.class,
                                      ConfigDbMarketExposureSelectorFn.class);

    Engine engine = new Engine(executor,
                               ComponentMap.of(componentMap),
                               availableOutputs,
                               availableImplementations,
                               FunctionConfig.EMPTY,
                               CacheManager.getInstance(),
                               EnumSet.of(EngineService.CACHING, EngineService.TRACING));
    View view = engine.createView(viewDef, createSingleFra());

    ZonedDateTime valuationTime = DateUtils.getUTCDate(2014, 1, 22);
    DefaultResettableMarketDataFn marketDataFn = new DefaultResettableMarketDataFn();
    marketDataFn.resetMarketData(valuationTime, loadMarketDataForFra());
    MarketDataFactory marketDataFactory = new SimpleMarketDataFactory(marketDataFn);
    CycleArguments cycleArguments = new CycleArguments(valuationTime, VersionCorrection.LATEST, marketDataFactory);
    _results = view.run(cycleArguments);
  }

  @Test
  public void resultsFRA() {
    assertThat(_results.getRows().size(), is(1)); //Single FRA
    assertThat(_results.getColumnNames().size(), is(2)); //Columns PV and Par Rate
  }

  @Test
  public void discountingFRAPV() {
    Result<?> resultPV = _results.get(0, 0).getResult();
    assertThat(resultPV.isValueAvailable(), is((true)));

    MultipleCurrencyAmount mca = (MultipleCurrencyAmount) resultPV.getValue();
    assertEquals(mca.getCurrencyAmount(Currency.USD).getAmount(), 23182.5437, STD_TOLERANCE_PV);
  }

  @Test
  public void parRateFRA() {
    Result<?> resultParRate = _results.get(0, 1).getResult();
    assertThat(resultParRate.isValueAvailable(), is((true)));

    Double parRate = (Double) resultParRate.getValue();
    assertEquals(0.003315, parRate, STD_TOLERANCE_RATE);
  }

  private ExposureFunctions mockExposureFunctions() {
    List<String> exposureFunctions =  ImmutableList.of("Currency");
    Map<ExternalId, String> idsToNames = new HashMap<>();
    idsToNames.put(ExternalId.of("CurrencyISO", "USD"), CURVE_CONSTRUCTION_CONFIGURATION);
    return new ExposureFunctions("USD_ON-OIS_LIBOR3M-FRAIRS", exposureFunctions, idsToNames);
  }

  //TODO this should be shared as used by engine and FXForwardPVFnTest
  private Map<MarketDataRequirement, MarketDataItem> loadMarketDataForFra()  throws IOException {
    Properties properties = new Properties();
    try (InputStream stream = FRAPVFn.class.getResourceAsStream("/usdMarketQuotes.properties");
         Reader reader = new BufferedReader(new InputStreamReader(stream))) {
      properties.load(reader);
    }
    Map<MarketDataRequirement, MarketDataItem> data = Maps.newHashMap();
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String id = (String) entry.getKey();
      String value = (String) entry.getValue();
      addValue(data, id, Double.valueOf(value));
    }
    return data;
  }

  //TODO this should be shared as used by engine and FXForwardPVFnTest
  private static MarketDataItem addValue(Map<MarketDataRequirement, MarketDataItem> marketData, String ticker, double value) {
    return addValue(marketData, new CurveNodeMarketDataRequirement(ExternalId.of("Ticker", ticker), "Market_Value"), value);
  }

  //TODO this should be shared as used by engine and FXForwardPVFnTest
  private static MarketDataItem addValue(Map<MarketDataRequirement, MarketDataItem> marketData,
                                         MarketDataRequirement requirement,
                                         double value) {
    return marketData.put(requirement, MarketDataItem.available(value));
  }

  private RegionSource mockRegionSource() {
    RegionSource mock = mock(RegionSource.class);
    SimpleRegion region = new SimpleRegion();
    region.addExternalId(s_USID);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);
    when(mock.getHighestLevelRegion(any(ExternalId.class)))
        .thenReturn(region);
    return mock;
  }

  private ConventionSource mockConventionSource() {
    BusinessDayConvention modifiedFollowing = BusinessDayConventions.MODIFIED_FOLLOWING;
    BusinessDayConvention following = BusinessDayConventions.FOLLOWING;
    DayCount thirtyU360 = DayCounts.THIRTY_U_360;
    DayCount act360 = DayCounts.ACT_360;
    StubType shortStart = StubType.SHORT_START;
    StubType noStub = StubType.NONE;

    ConventionSource mock = mock(ConventionSource.class);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);

    SwapFixedLegConvention descPayLegConvention =
        new SwapFixedLegConvention(DISC_LEG_CONVENTION, _discPayLegConventionId.toBundle(), Tenor.ONE_YEAR,
                                   act360, modifiedFollowing, s_USD, s_USID, 2, true, shortStart, false, 2);
    when(mock.getSingle(_discPayLegConventionId, FinancialConvention.class))
        .thenReturn(descPayLegConvention);

    OvernightIndexConvention onConvention = new OvernightIndexConvention(USD_OVERNIGHT_CONVENTION,
                                                                         _onConventionId.toBundle(),
                                                                         act360,
                                                                         1,
                                                                         s_USD,
                                                                         s_USID);
    when(mock.getSingle(_onConventionId, FinancialConvention.class))
        .thenReturn(onConvention);
    when(mock.getSingle(_onConventionId, OvernightIndexConvention.class))
        .thenReturn(onConvention);

    OISLegConvention descReceiveLegConvention =
        new OISLegConvention(DISC_RECEIVE_LEG_CONVENTION, _discReceiveLegConventionId.toBundle(),
                             _onConventionId, Tenor.ONE_YEAR, modifiedFollowing, 2, true, noStub, false, 2);
    when(mock.getSingle(_discReceiveLegConventionId, FinancialConvention.class))
        .thenReturn(descReceiveLegConvention);

    DepositConvention descConvention =
        new DepositConvention(DISC_CONVENTION, _discConventionId.toBundle(), act360, following, 0, false, s_USD, s_USID);
    when(mock.getSingle(_discConventionId, FinancialConvention.class))
        .thenReturn(descConvention);
    when(mock.getSingle(_discConventionId))
        .thenReturn(descConvention);

    SwapFixedLegConvention liborPayLegConvention =
        new SwapFixedLegConvention(LIBOR_PAY_LEG_CONVENTION, _liborPayLegConventionId.toBundle(), Tenor.SIX_MONTHS,
                                   thirtyU360, modifiedFollowing, s_USD, s_USGBID, 2, true, shortStart, false, 0);
    when(mock.getSingle(_liborPayLegConventionId, FinancialConvention.class))
        .thenReturn(liborPayLegConvention);

    VanillaIborLegConvention liborReceiveLegConvention =
        new VanillaIborLegConvention(LIBOR_RECEIVE_LEG_CONVENTION, _liborReceiveLegConventionId.toBundle(),
                                     _liborConventionId, true, "Linear", Tenor.THREE_MONTHS, 2, true, shortStart, false, 0);
    when(mock.getSingle(any(ExternalId.class), eq(VanillaIborLegConvention.class)))
        .thenReturn(liborReceiveLegConvention);
    when(mock.getSingle(_liborReceiveLegConventionId, FinancialConvention.class))
        .thenReturn(liborReceiveLegConvention);

    IborIndexConvention liborConvention = new IborIndexConvention(LIBOR_CONVENTION,
                                                                  _liborConventionId.toBundle(),
                                                                  act360,
                                                                  modifiedFollowing,
                                                                  2,
                                                                  true,
                                                                  s_USD,
                                                                  LocalTime.of(11, 0),
                                                                  "Europe/London",
                                                                  s_USGBID,
                                                                  s_USID,
                                                                  "");
    when(mock.getSingle(_liborConventionId, FinancialConvention.class))
        .thenReturn(liborConvention);
    when(mock.getSingle(any(ExternalId.class), eq(IborIndexConvention.class)))
        .thenReturn(liborConvention);
    when(mock.getSingle(_liborConventionId))
        .thenReturn(liborConvention);

    return mock;
  }

  private ConfigSource mockConfigSource() {

    //Config source mock
    ConfigSource mock = mock(ConfigSource.class);

    IborCurveTypeConfiguration liborCurveTypeConfig = new IborCurveTypeConfiguration(_liborConventionId, Tenor.THREE_MONTHS);
    OvernightCurveTypeConfiguration onCurveTypeConfig = new OvernightCurveTypeConfiguration(_onConventionId);
    DiscountingCurveTypeConfiguration discCurveTypeConfig = new DiscountingCurveTypeConfiguration("USD");

    Map<String, List<? extends CurveTypeConfiguration>> curveNameTypeMap = Maps.newHashMap();

    curveNameTypeMap.put(LIBOR_CURVE_NAME, Arrays.asList(liborCurveTypeConfig));
    curveNameTypeMap.put(ON_CURVE_NAME, Arrays.asList(onCurveTypeConfig, discCurveTypeConfig));

    CurveGroupConfiguration curveGroupConfig = new CurveGroupConfiguration(0, curveNameTypeMap);
    List<CurveGroupConfiguration> curveGroupConfigs = ImmutableList.of(curveGroupConfig);

    List<String> exogenousConfigurations = ImmutableList.of();
    CurveConstructionConfiguration curveConfig = new CurveConstructionConfiguration(CURVE_CONSTRUCTION_CONFIGURATION, curveGroupConfigs, exogenousConfigurations);
    when(mock.get(eq(CurveConstructionConfiguration.class),
                  eq(CURVE_CONSTRUCTION_CONFIGURATION),
                  any(VersionCorrection.class)))
             .thenReturn(ImmutableSet.of(ConfigItem.of(curveConfig)));

    //return curve definitions via mock
    when(mock.get(eq(Object.class), eq(ON_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.<ConfigItem<Object>>of(ConfigItem.<Object>of(getUSDDiscountingCurveDefinition())));
    when(mock.get(eq(Object.class), eq(LIBOR_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.<ConfigItem<Object>>of(ConfigItem.<Object>of(get3MLiborCurveDefinition())));

    //return node mappers via mock
    when(mock.getSingle(CurveNodeIdMapper.class, USD_DISC_MAPPER, VersionCorrection.LATEST))
        .thenReturn(getUSDDiscountingCurveMapper());
    when(mock.getSingle(CurveNodeIdMapper.class, USD_DISC_OVERNIGHT_MAPPER, VersionCorrection.LATEST))
        .thenReturn(getUSDDiscountingOvernightCurveMapper());
    when(mock.getSingle(CurveNodeIdMapper.class, LIBOR_3M_MAPPER, VersionCorrection.LATEST))
        .thenReturn(get3MLiborCurveMapper());

    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);

    return mock;
  }

  private CurveNodeIdMapper getUSDDiscountingCurveMapper() {
    Map<Tenor, CurveInstrumentProvider> cashNodes = Maps.newHashMap();
    cashNodes.put(Tenor.OVERNIGHT, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D1")));
    cashNodes.put(Tenor.OVERNIGHT, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D2")));

    Map<Tenor, CurveInstrumentProvider> swapNodes = Maps.newHashMap();
    swapNodes.put(Tenor.ONE_MONTH, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D3")));
    swapNodes.put(Tenor.TWO_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D4")));
    swapNodes.put(Tenor.THREE_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D5")));
    swapNodes.put(Tenor.SIX_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D6")));
    swapNodes.put(Tenor.NINE_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D7")));
    swapNodes.put(Tenor.ONE_YEAR, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D8")));
    swapNodes.put(Tenor.TWO_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D9")));
    swapNodes.put(Tenor.THREE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D10")));
    swapNodes.put(Tenor.FOUR_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D11")));
    swapNodes.put(Tenor.FIVE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D12")));
    swapNodes.put(Tenor.SIX_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D13")));
    swapNodes.put(Tenor.SEVEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D14")));
    swapNodes.put(Tenor.EIGHT_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D15")));
    swapNodes.put(Tenor.NINE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D16")));
    swapNodes.put(Tenor.TEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D17")));

    return CurveNodeIdMapper.builder()
        .name(USD_DISC_MAPPER)
        .cashNodeIds(cashNodes)
        .swapNodeIds(swapNodes)
        .build();
  }

  private CurveNodeIdMapper getUSDDiscountingOvernightCurveMapper() {
    Map<Tenor, CurveInstrumentProvider> cashNodes = Maps.newHashMap();
    cashNodes.put(Tenor.OVERNIGHT, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "D2")));
    return CurveNodeIdMapper.builder()
        .name(USD_DISC_OVERNIGHT_MAPPER)
        .cashNodeIds(cashNodes)
        .build();
  }

  private InterpolatedCurveDefinition getUSDDiscountingCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new CashNode(Tenor.ofDays(0), Tenor.OVERNIGHT, _discConventionId, USD_DISC_MAPPER));
    nodes.add(new CashNode(Tenor.OVERNIGHT, Tenor.OVERNIGHT, _discConventionId, USD_DISC_OVERNIGHT_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ONE_MONTH, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.TWO_MONTHS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.THREE_MONTHS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.SIX_MONTHS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.NINE_MONTHS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ONE_YEAR, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.TWO_YEARS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.THREE_YEARS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.FOUR_YEARS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.FIVE_YEARS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.SIX_YEARS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.SEVEN_YEARS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.EIGHT_YEARS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.NINE_YEARS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.TEN_YEARS, _discPayLegConventionId, _discReceiveLegConventionId, USD_DISC_MAPPER));
    return new InterpolatedCurveDefinition(ON_CURVE_NAME, nodes, "Linear", "FlatExtrapolator", "FlatExtrapolator");
  }

  private CurveNodeIdMapper get3MLiborCurveMapper() {
    Map<Tenor, CurveInstrumentProvider> cashNodes = Maps.newHashMap();
    cashNodes.put(Tenor.THREE_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L1")));

    Map<Tenor, CurveInstrumentProvider> fraNodes = Maps.newHashMap();
    fraNodes.put(Tenor.SIX_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L2")));
    fraNodes.put(Tenor.NINE_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L3")));

    Map<Tenor, CurveInstrumentProvider> swapNodes = Maps.newHashMap();
    swapNodes.put(Tenor.ONE_YEAR, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L4")));
    swapNodes.put(Tenor.TWO_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L5")));
    swapNodes.put(Tenor.THREE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L6")));
    swapNodes.put(Tenor.FOUR_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L7")));
    swapNodes.put(Tenor.FIVE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L8")));
    swapNodes.put(Tenor.SEVEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L9")));
    swapNodes.put(Tenor.TEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L10")));
    swapNodes.put(Tenor.ofYears(12), new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L11")));
    swapNodes.put(Tenor.ofYears(15), new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L12")));
    swapNodes.put(Tenor.ofYears(20), new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L13")));
    swapNodes.put(Tenor.ofYears(25),new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L14")));
    swapNodes.put(Tenor.ofYears(30),new StaticCurveInstrumentProvider(ExternalId.of("Ticker", "L15")));

    return CurveNodeIdMapper.builder()
        .name(LIBOR_3M_MAPPER)
        .cashNodeIds(cashNodes)
        .fraNodeIds(fraNodes)
        .swapNodeIds(swapNodes)
        .build();
  }
  
  private InterpolatedCurveDefinition get3MLiborCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new CashNode(Tenor.ofDays(0), Tenor.THREE_MONTHS, _liborConventionId, LIBOR_3M_MAPPER));
    nodes.add(new FRANode(Tenor.THREE_MONTHS, Tenor.SIX_MONTHS, _liborConventionId, LIBOR_3M_MAPPER));
    nodes.add(new FRANode(Tenor.SIX_MONTHS, Tenor.NINE_MONTHS, _liborConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ONE_YEAR,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.TWO_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.THREE_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.FOUR_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.FIVE_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.SEVEN_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.TEN_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(12),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(15),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(20),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(25),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(30),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, LIBOR_3M_MAPPER));
    return new InterpolatedCurveDefinition(LIBOR_CURVE_NAME, nodes, "Linear", "FlatExtrapolator", "FlatExtrapolator");
  }

  private SecuritySource mockSecuritySource() {
    SecuritySource mock = mock(SecuritySource.class);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);
    return mock;
  }

  private HistoricalTimeSeriesSource mockHistoricalTimeSeriesSource() {
    HistoricalTimeSeriesSource mock = mock(HistoricalTimeSeriesSource.class);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);
    return mock;
  }

  private HolidaySource mockHolidaySource() {
    return new WeekendHolidaySource();
  }

  private ImmutableMap<Class<?>, Object> generateComponentMap(Object... components) {
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    for (Object component : components) {
      builder.put(component.getClass().getInterfaces()[0], component);
    }
    return builder.build();
  }

  private List<FRASecurity> createSingleFra() {
    ExternalId regionId = ExternalId.of("Reg", "123");
    ExternalId underlyingId = ExternalId.of("Und", "321");
    FRASecurity security = new FRASecurity(s_USD, regionId, STD_ACCRUAL_START_DATE, STD_ACCRUAL_END_DATE,
                                           0.0125, -10000000, underlyingId, STD_REFERENCE_DATE);
    return ImmutableList.of(security);
  }



}
