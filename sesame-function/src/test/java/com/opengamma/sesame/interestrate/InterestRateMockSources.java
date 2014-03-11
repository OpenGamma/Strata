/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.interestrate;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.mockito.Matchers;
import org.threeten.bp.LocalTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.holiday.impl.WeekendHolidaySource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.region.impl.SimpleRegion;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveNodeIdMapper;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.DiscountingCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.IborCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.InterpolatedCurveDefinition;
import com.opengamma.financial.analytics.curve.OvernightCurveTypeConfiguration;
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
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.security.index.IborIndex;
import com.opengamma.financial.security.index.OvernightIndex;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.MarketdataResourcesLoader;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.sesame.marketdata.RecordingMarketDataSource;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.Tenor;

/**
 *
 */
public class InterestRateMockSources {

  private static final ChangeManager MOCK_CHANGE_MANAGER = mock(ChangeManager.class);

  private static final String CURVE_CONSTRUCTION_CONFIGURATION = "USD_ON-OIS_LIBOR3M-FRAIRS_1U";

  private static final String USD_DISC_MAPPER = "Test USD Discounting Mapper";
  private static final String USD_DISC_OVERNIGHT_MAPPER = "Test USD Discounting Overnight Mapper";
  private static final String LIBOR_3M_MAPPER = "Test 3m Libor Mapper";

  private static final String LIBOR_CURVE_NAME ="USD-LIBOR3M-FRAIRS";
  private static final String ON_CURVE_NAME ="USD-ON-OIS";

  private static final String DISC_LEG_CONVENTION =  "USD 1Y Pay Lag Fixed Leg";
  private static final String DISC_RECEIVE_LEG_CONVENTION = "USD OIS Overnight Leg";
  private static final String DISC_CONVENTION =  "USD DepositON";
  private static final String LIBOR_PAY_LEG_CONVENTION =  "USD IRS Fixed Leg";
  private static final String LIBOR_RECEIVE_LEG_CONVENTION = "USD 3M IRS Ibor Leg";
  private static final String LIBOR_CONVENTION =  "USD Libor";
  private static final String LIBOR_INDEX =  "Libor Index";
  private static final String USD_OVERNIGHT_CONVENTION =  "USD Overnight";
  private static final String USD_OVERNIGHT_INDEX =  "USD Overnight Index";

  private static final ExternalId _discPayLegConventionId = ExternalId.of("CONVENTION", DISC_LEG_CONVENTION);
  private static final ExternalId _discReceiveLegConventionId = ExternalId.of("CONVENTION", DISC_RECEIVE_LEG_CONVENTION);
  private static final ExternalId _discConventionId = ExternalId.of("CONVENTION", DISC_CONVENTION);
  private static final ExternalId _liborPayLegConventionId = ExternalId.of("CONVENTION", LIBOR_PAY_LEG_CONVENTION);
  private static final ExternalId _liborReceiveLegConventionId = ExternalId.of("CONVENTION", LIBOR_RECEIVE_LEG_CONVENTION);
  private static final ExternalId _liborConventionId = ExternalId.of("CONVENTION", LIBOR_CONVENTION);
  private static final ExternalId _onConventionId = ExternalId.of("CONVENTION", USD_OVERNIGHT_CONVENTION);

  private static final ExternalId _liborIndexId = ExternalId.of("CONVENTION", LIBOR_INDEX);
  private static final ExternalId _onIndexId = ExternalId.of("CONVENTION", USD_OVERNIGHT_INDEX);
  private static final String TICKER = "Ticker";

  private static ExternalId s_USID = ExternalSchemes.financialRegionId("US");
  private static ExternalId s_USGBID = ExternalSchemes.financialRegionId("US+GB");
  private static Currency s_USD = Currency.USD;

  public static ExternalId getLiborIndexId() {
    return _liborIndexId;
  }

  public static ImmutableMap<Class<?>, Object> generateBaseComponents() {
    return generateComponentMap(mockHolidaySource(),
                                mockRegionSource(),
                                mockConventionSource(),
                                mockConfigSource(),
                                mockSecuritySource(),
                                mockHistoricalTimeSeriesSource(),
                                mock(HistoricalMarketDataFn.class),
                                mock(CurrencyMatrix.class));
  }

  public static MarketDataFactory createMarketDataFactory() {
    MarketDataFactory mock = mock(MarketDataFactory.class);
    when(mock.create(Matchers.<MarketDataSpecification>any())).thenReturn(createMarketDataSource());
    return mock;
  }

  public static MarketDataSource createMarketDataSource() {
    try {
      Map<ExternalIdBundle, Double> marketData = MarketdataResourcesLoader.getData("/usdMarketQuotes.properties", TICKER);
      FieldName fieldName = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);
      RecordingMarketDataSource.Builder builder = new RecordingMarketDataSource.Builder();
      return builder.data(fieldName, marketData).build();
    } catch (IOException e) {
      throw new OpenGammaRuntimeException("Exception whilst loading file", e);
    }
  }

  public static  ExposureFunctions mockExposureFunctions() {
    List<String> exposureFunctions =  ImmutableList.of("Currency");
    Map<ExternalId, String> idsToNames = new HashMap<>();
    idsToNames.put(ExternalId.of("CurrencyISO", "USD"), CURVE_CONSTRUCTION_CONFIGURATION);
    return new ExposureFunctions("USD_ON-OIS_LIBOR3M-FRAIRS", exposureFunctions, idsToNames);
  }


  private static CurveNodeIdMapper getUSDDiscountingCurveMapper() {
    Map<Tenor, CurveInstrumentProvider> cashNodes = Maps.newHashMap();
    cashNodes.put(Tenor.ONE_DAY, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D1")));
    cashNodes.put(Tenor.TWO_DAYS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D2")));

    Map<Tenor, CurveInstrumentProvider> swapNodes = Maps.newHashMap();
    swapNodes.put(Tenor.ONE_MONTH, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D3")));
    swapNodes.put(Tenor.TWO_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D4")));
    swapNodes.put(Tenor.THREE_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D5")));
    swapNodes.put(Tenor.SIX_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D6")));
    swapNodes.put(Tenor.NINE_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D7")));
    swapNodes.put(Tenor.ONE_YEAR, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D8")));
    swapNodes.put(Tenor.TWO_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D9")));
    swapNodes.put(Tenor.THREE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D10")));
    swapNodes.put(Tenor.FOUR_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D11")));
    swapNodes.put(Tenor.FIVE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D12")));
    swapNodes.put(Tenor.SIX_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D13")));
    swapNodes.put(Tenor.SEVEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D14")));
    swapNodes.put(Tenor.EIGHT_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D15")));
    swapNodes.put(Tenor.NINE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D16")));
    swapNodes.put(Tenor.TEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D17")));

    return CurveNodeIdMapper.builder()
        .name(USD_DISC_MAPPER)
        .cashNodeIds(cashNodes)
        .swapNodeIds(swapNodes)
        .build();
  }

  private static CurveNodeIdMapper getUSDDiscountingOvernightCurveMapper() {
    Map<Tenor, CurveInstrumentProvider> cashNodes = Maps.newHashMap();
    cashNodes.put(Tenor.OVERNIGHT, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D2")));
    return CurveNodeIdMapper.builder()
        .name(USD_DISC_OVERNIGHT_MAPPER)
        .cashNodeIds(cashNodes)
        .build();
  }

  private static InterpolatedCurveDefinition getUSDDiscountingCurveDefinition() {
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

  private static CurveNodeIdMapper get3MLiborCurveMapper() {
    Map<Tenor, CurveInstrumentProvider> cashNodes = Maps.newHashMap();
    cashNodes.put(Tenor.THREE_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L1")));

    Map<Tenor, CurveInstrumentProvider> fraNodes = Maps.newHashMap();
    fraNodes.put(Tenor.SIX_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L2")));
    fraNodes.put(Tenor.NINE_MONTHS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L3")));

    Map<Tenor, CurveInstrumentProvider> swapNodes = Maps.newHashMap();
    swapNodes.put(Tenor.ONE_YEAR, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L4")));
    swapNodes.put(Tenor.TWO_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L5")));
    swapNodes.put(Tenor.THREE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L6")));
    swapNodes.put(Tenor.FOUR_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L7")));
    swapNodes.put(Tenor.FIVE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L8")));
    swapNodes.put(Tenor.SEVEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L9")));
    swapNodes.put(Tenor.TEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L10")));
    swapNodes.put(Tenor.ofYears(12), new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L11")));
    swapNodes.put(Tenor.ofYears(15), new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L12")));
    swapNodes.put(Tenor.ofYears(20), new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L13")));
    swapNodes.put(Tenor.ofYears(25),new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L14")));
    swapNodes.put(Tenor.ofYears(30),new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "L15")));

    return CurveNodeIdMapper.builder()
        .name(LIBOR_3M_MAPPER)
        .cashNodeIds(cashNodes)
        .fraNodeIds(fraNodes)
        .swapNodeIds(swapNodes)
        .build();
  }

  private static InterpolatedCurveDefinition get3MLiborCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new CashNode(Tenor.ofDays(0), Tenor.THREE_MONTHS, _liborIndexId, LIBOR_3M_MAPPER));
    nodes.add(new FRANode(Tenor.THREE_MONTHS, Tenor.SIX_MONTHS, _liborIndexId, LIBOR_3M_MAPPER));
    nodes.add(new FRANode(Tenor.SIX_MONTHS, Tenor.NINE_MONTHS, _liborIndexId, LIBOR_3M_MAPPER));
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

  private static ImmutableMap<Class<?>, Object> generateComponentMap(Object... components) {
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    for (Object component : components) {
      builder.put(component.getClass().getInterfaces()[0], component);
    }
    return builder.build();
  }


  private static HistoricalTimeSeriesSource mockHistoricalTimeSeriesSource() {
    HistoricalTimeSeriesSource mock = mock(HistoricalTimeSeriesSource.class);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);
    return mock;
  }

  private static HolidaySource mockHolidaySource() {
    return new WeekendHolidaySource();
  }

  private static RegionSource mockRegionSource() {
    RegionSource mock = mock(RegionSource.class);
    SimpleRegion region = new SimpleRegion();
    region.addExternalId(s_USID);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);
    when(mock.getHighestLevelRegion(any(ExternalId.class)))
        .thenReturn(region);
    return mock;
  }

  private static ConventionSource mockConventionSource() {
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

    OvernightIndexConvention onConvention =
        new OvernightIndexConvention(USD_OVERNIGHT_CONVENTION, _onConventionId.toBundle(), act360, 1, s_USD, s_USID);
    when(mock.getSingle(_onConventionId, FinancialConvention.class))
        .thenReturn(onConvention);
    when(mock.getSingle(_onConventionId, OvernightIndexConvention.class))
        .thenReturn(onConvention);
    when(mock.getSingle(eq(_onConventionId.toBundle()), any(VersionCorrection.class)))
        .thenReturn(onConvention);

    OISLegConvention descReceiveLegConvention =
        new OISLegConvention(DISC_RECEIVE_LEG_CONVENTION, _discReceiveLegConventionId.toBundle(),
                             _onIndexId, Tenor.ONE_YEAR, modifiedFollowing, 2, true, noStub, false, 2);
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
                                     _liborIndexId, true, "Linear", Tenor.THREE_MONTHS, 2, true, shortStart, false, 0);
    when(mock.getSingle(any(ExternalId.class), eq(VanillaIborLegConvention.class)))
        .thenReturn(liborReceiveLegConvention);
    when(mock.getSingle(_liborReceiveLegConventionId, FinancialConvention.class))
        .thenReturn(liborReceiveLegConvention);

    IborIndexConvention liborConvention =
        new IborIndexConvention(LIBOR_CONVENTION, _liborConventionId.toBundle(), act360, modifiedFollowing, 2,
                                true, s_USD, LocalTime.of(11, 0), "Europe/London", s_USGBID, s_USID, "");
    when(mock.getSingle(_liborConventionId, FinancialConvention.class))
        .thenReturn(liborConvention);
    when(mock.getSingle(any(ExternalId.class), eq(IborIndexConvention.class)))
        .thenReturn(liborConvention);
    when(mock.getSingle(_liborConventionId))
        .thenReturn(liborConvention);
    when(mock.getSingle(eq(_liborConventionId.toBundle()), any(VersionCorrection.class)))
        .thenReturn(liborConvention);

    return mock;
  }

  private static SecuritySource mockSecuritySource() {
    SecuritySource mock = mock(SecuritySource.class);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);

    OvernightIndex onIndex = new OvernightIndex(USD_OVERNIGHT_INDEX, _onConventionId);
    when(mock.getSingle(_onIndexId.toBundle()))
        .thenReturn(onIndex);
    when(mock.getSingle(eq(_onIndexId.toBundle()), any(VersionCorrection.class)))
        .thenReturn(onIndex);

    IborIndex iIndex = new IborIndex(LIBOR_INDEX, Tenor.THREE_MONTHS, _liborConventionId);
    when(mock.getSingle(_liborIndexId.toBundle()))
        .thenReturn(iIndex);
    when(mock.getSingle(eq(_liborIndexId.toBundle()), any(VersionCorrection.class)))
        .thenReturn(iIndex);

    return mock;
  }

  private static ConfigSource mockConfigSource() {

    //Config source mock
    ConfigSource mock = mock(ConfigSource.class);

    IborCurveTypeConfiguration liborCurveTypeConfig = new IborCurveTypeConfiguration(_liborIndexId, Tenor.THREE_MONTHS);
    OvernightCurveTypeConfiguration onCurveTypeConfig = new OvernightCurveTypeConfiguration(_onIndexId);
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
        .thenReturn(ImmutableSet.of(ConfigItem.<Object>of(getUSDDiscountingCurveDefinition())));
    when(mock.get(eq(Object.class), eq(LIBOR_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(ConfigItem.<Object>of(get3MLiborCurveDefinition())));

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

}
