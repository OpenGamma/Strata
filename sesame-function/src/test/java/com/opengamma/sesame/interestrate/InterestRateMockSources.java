/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.interestrate;

import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.FLAT_EXTRAPOLATOR;
import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.LINEAR;
import static com.opengamma.financial.convention.initializer.PerCurrencyConventionHelper.FED_FUNDS_FUTURE;
import static com.opengamma.financial.convention.initializer.PerCurrencyConventionHelper.SCHEME_NAME;
import static com.opengamma.sesame.sabr.SabrSurfaceSelector.SabrSurfaceName;
import static com.opengamma.util.money.Currency.GBP;
import static com.opengamma.util.money.Currency.USD;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.mockito.Matchers;
import org.threeten.bp.LocalDate;
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
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.historicaltimeseries.impl.SimpleHistoricalTimeSeries;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.link.ConventionLink;
import com.opengamma.core.link.SnapshotLink;
import com.opengamma.core.position.Counterparty;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.region.impl.SimpleRegion;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveNodeIdMapper;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.DiscountingCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.IborCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.InterpolatedCurveDefinition;
import com.opengamma.financial.analytics.curve.OvernightCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.exposure.CounterpartyExposureFunction;
import com.opengamma.financial.analytics.curve.exposure.CurrencyExposureFunction;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.analytics.curve.exposure.TradeAttributeExposureFunction;
import com.opengamma.financial.analytics.ircurve.BloombergFutureCurveInstrumentProvider;
import com.opengamma.financial.analytics.ircurve.CurveInstrumentProvider;
import com.opengamma.financial.analytics.ircurve.StaticCurveInstrumentProvider;
import com.opengamma.financial.analytics.ircurve.strips.CashNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.DataFieldType;
import com.opengamma.financial.analytics.ircurve.strips.FRANode;
import com.opengamma.financial.analytics.ircurve.strips.RateFutureNode;
import com.opengamma.financial.analytics.ircurve.strips.SwapNode;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.financial.convention.DepositConvention;
import com.opengamma.financial.convention.FederalFundsFutureConvention;
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
import com.opengamma.financial.convention.expirycalc.ExchangeTradedInstrumentExpiryCalculator;
import com.opengamma.financial.convention.expirycalc.FedFundFutureAndFutureOptionMonthlyExpiryCalculator;
import com.opengamma.financial.convention.initializer.PerCurrencyConventionHelper;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.index.IborIndex;
import com.opengamma.financial.security.index.OvernightIndex;
import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.master.convention.ConventionDocument;
import com.opengamma.master.convention.ConventionMaster;
import com.opengamma.master.convention.impl.InMemoryConventionMaster;
import com.opengamma.master.convention.impl.MasterConventionSource;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.master.region.RegionDocument;
import com.opengamma.master.region.RegionMaster;
import com.opengamma.master.region.impl.InMemoryRegionMaster;
import com.opengamma.master.region.impl.MasterRegionSource;
import com.opengamma.master.security.SecurityDocument;
import com.opengamma.master.security.SecurityMaster;
import com.opengamma.master.security.impl.InMemorySecurityMaster;
import com.opengamma.master.security.impl.MasterSecuritySource;
import com.opengamma.sesame.MarketDataResourcesLoader;
import com.opengamma.sesame.holidays.UsdHolidaySource;
import com.opengamma.sesame.marketdata.DefaultStrategyAwareMarketDataSource;
import com.opengamma.sesame.marketdata.MapMarketDataSource;
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.sesame.sabr.SabrConfigSelector;
import com.opengamma.sesame.sabr.SabrExpiryTenorSurface;
import com.opengamma.sesame.sabr.SabrNode;
import com.opengamma.sesame.sabr.SabrSurfaceSelector;
import com.opengamma.sesame.sabr.SabrSwaptionConfig;
import com.opengamma.sesame.sabr.SabrSwaptionDataConfig;
import com.opengamma.sesame.sabr.SabrSwaptionInterpolationConfig;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Tenor;

/**
 * TODO private constructor, everything is static
 */
public class InterestRateMockSources {

  private static final ChangeManager MOCK_CHANGE_MANAGER = mock(ChangeManager.class);

  private static final String CURVE_CONSTRUCTION_CONFIGURATION_USD_OIS_LIB3 = "USD_ON-OIS_LIBOR3M-FRAIRS_1U";
  private static final String CURVE_CONSTRUCTION_CONFIGURATION_USD_FFF = "USD_ON-FFF";
  
  private static final String OG_TICKER = "OG_TICKER";

  private static final String USD_DISC_MAPPER = "Test USD Discounting Mapper";
  private static final String USD_FF_MAPPER = "USD FFS-FFF OG Mapper";
  private static final String USD_DISC_OVERNIGHT_MAPPER = "Test USD Discounting Overnight Mapper";
  private static final String USD_LIBOR3M_MAPPER = "Test 3m Libor Mapper";

  private static final String CURVE_CONSTRUCTION_CONFIGURATION_COUNTERPARTY_A = "CCC_COUNTERPARTY_A";
  private static final ExternalId COUNTERPARTY_A = ExternalId.of(Counterparty.DEFAULT_SCHEME, "PASS");
  private static final String EXPOSURE_FUNCTION_COUNTERPARTY = "EF_COUNTERPARTY";

  private static final String CURVE_CONSTRUCTION_CONFIGURATION_CURRENCY_USD = "CCC_CURRENCY_USD";
  private static final ExternalId CURRENCY_USD = ExternalId.of(Currency.OBJECT_SCHEME, "USD");
  private static final String EXPOSURE_FUNCTION_CURRENCY = "EF_CURRENCY";

  private static final String CURVE_CONSTRUCTION_CONFIGURATION_TRADE_A = "CCC_TRADE_A";
  private static final String EXPOSURE_FUNCTION_TRADE = "EF_TRADE";

  /** USD 3M Libor curve name */
  public static final String USD_LIBOR3M_CURVE_NAME = "USD-LIBOR3M-FRAIRS";
  /** USD OIS curve name */
  public static final String USD_OIS_CURVE_NAME = "USD-ON-OIS";
  private static final String USD_FFF_CURVE_NAME = "USD-ON-FFF";

  private static final String DISC_LEG_CONVENTION = "USD 1Y Pay Lag Fixed Leg";
  private static final String DISC_RECEIVE_LEG_CONVENTION = "USD OIS Overnight Leg";
  private static final String DISC_CONVENTION = "USD DepositON";
  private static final String LIBOR_PAY_LEG_CONVENTION_NAME = "USD IRS Fixed Leg";
  private static final String LIBOR_RECEIVE_LEG_CONVENTION_NAME = "USD 3M IRS Ibor Leg";
  private static final String LIBOR_CONVENTION = "USD Libor";
  private static final String LIBOR_INDEX = "USD 3M IRS Ibor Leg";
  private static final String USD_OVERNIGHT_CONVENTION = "USD Overnight";
  private static final String USD_FEDFUND_INDEX = "USD Fed Funds";
  private static final String USD_FEDFUNDFUTURES_CONVENTION = "Fed Funds Future";

  private static final ExternalId _discPayLegConventionId = ExternalId.of("CONVENTION", DISC_LEG_CONVENTION);
  private static final ExternalId _discReceiveLegConventionId = ExternalId.of("CONVENTION", DISC_RECEIVE_LEG_CONVENTION);
  private static final ExternalId _discConventionId = ExternalId.of("CONVENTION", DISC_CONVENTION);
  private static final ExternalId _liborPayLegConventionId = ExternalId.of("CONVENTION", LIBOR_PAY_LEG_CONVENTION_NAME);
  private static final ExternalId _liborReceiveLegConventionId = ExternalId.of("CONVENTION", LIBOR_RECEIVE_LEG_CONVENTION_NAME);
  private static final ExternalId _liborConventionId = ExternalId.of("CONVENTION", LIBOR_CONVENTION);
  private static final ExternalId _onConventionId = ExternalId.of("CONVENTION", USD_OVERNIGHT_CONVENTION);
  private static final ExternalId _fffConventionId = ExternalId.of("CONVENTION", USD_FEDFUNDFUTURES_CONVENTION);

  private static final ExternalId _liborIndexId = ExternalId.of("SEC", LIBOR_INDEX);
  private static final ExternalId _onIndexId = ExternalId.of(OG_TICKER, USD_FEDFUND_INDEX);
  private static final UniqueId _onIndexUniqueId = UniqueId.of(OG_TICKER, USD_FEDFUND_INDEX);
  private static final String TICKER = "Ticker";

  private static final ExternalId s_USID = ExternalSchemes.financialRegionId("US");
  private static final ExternalId s_GBID = ExternalSchemes.financialRegionId("GB");
  private static final ExternalId s_USGBID = ExternalSchemes.financialRegionId("US+GB");
  private static final Currency s_USD = USD;

  private static final SwapFixedLegConvention LIBOR_PAY_LEG_CONVENTION =
      createLiborFixedLegConvention();

  private static SwapFixedLegConvention createLiborFixedLegConvention() {
    SwapFixedLegConvention legConvention =
        new SwapFixedLegConvention(LIBOR_PAY_LEG_CONVENTION_NAME, _liborPayLegConventionId.toBundle(),
                                   Tenor.SIX_MONTHS, DayCounts.THIRTY_U_360, BusinessDayConventions.MODIFIED_FOLLOWING,
                                   s_USD, s_USGBID, 2, true, StubType.SHORT_START, false, 0);
    legConvention.setUniqueId(UniqueId.of("CONV", "11"));
    return legConvention;
  }

  public static ExternalId getLiborIndexId() {
    return _liborIndexId;
  }
  
  public static ExternalId getOvernightIndexId() {
    return _onIndexId;
  }

  public static ImmutableMap<Class<?>, Object> generateBaseComponents() {
    return generateComponentMap(mockHolidaySource(),
                                mockRegionSource(),
                                mockConventionSource(),
                                mock(ConventionBundleSource.class),
                                mockConfigSource(),
                                mockSecuritySource(),
                                mockHistoricalTimeSeriesSource(),
                                mock(HistoricalTimeSeriesResolver.class),
                                mock(CurrencyMatrix.class));
  }

  public static MarketDataFactory createMarketDataFactory() {
    MarketDataFactory mock = mock(MarketDataFactory.class);
    when(mock.create(Matchers.<MarketDataSpecification>any())).thenReturn(new DefaultStrategyAwareMarketDataSource(
        LiveMarketDataSpecification.LIVE_SPEC, createMarketDataSource()));
    return mock;
  }

  public static MarketDataSource createMarketDataSource() {
    return createMarketDataSource(LocalDate.of(2014, 1, 22));
  }
  
  public static MarketDataSource createMarketDataSource(LocalDate date) {
    return createMarketDataSource(date, true);
  }

  public static MarketDataSource createMarketDataSource(LocalDate date, boolean generateTicker) {
    String filename;
    if (date.equals(LocalDate.of(2014,1,22))) {
      filename = "/usdMarketQuotes-20140122.properties";
    } else if (date.equals(LocalDate.of(2014,2,18))) {
      filename = "/usdMarketQuotes-20140218.properties";
    } else if (date.equals(LocalDate.of(2014,4,17))) {
      filename = "/usdMarketQuotes-20140417.properties";
    } else {
      throw new OpenGammaRuntimeException("No data available for date: " + date);
    }

    try {
      Map<ExternalIdBundle, Double> marketData = MarketDataResourcesLoader.getData(filename,
                                                                                   generateTicker ?
                                                                                       TICKER :
                                                                                       null);
      MapMarketDataSource.Builder builder = MapMarketDataSource.builder();
      for (Map.Entry<ExternalIdBundle, Double> entry : marketData.entrySet()) {
        builder.add(entry.getKey(), entry.getValue());
      }
      return builder.build();
    } catch (IOException e) {
      throw new OpenGammaRuntimeException("Exception whilst loading file", e);
    }
  }


  public static ExposureFunctions mockExposureFunctions() {
    List<String> exposureFunctions =  ImmutableList.of(CurrencyExposureFunction.NAME);
    Map<ExternalId, String> idsToNames = new HashMap<>();
    idsToNames.put(ExternalId.of("CurrencyISO", "USD"), CURVE_CONSTRUCTION_CONFIGURATION_USD_OIS_LIB3);
    return new ExposureFunctions("USD_ON-OIS_LIBOR3M-FRAIRS", exposureFunctions, idsToNames);
  }


  public static ExposureFunctions mockFFExposureFunctions() {
    List<String> exposureFunctions =  ImmutableList.of(CurrencyExposureFunction.NAME);
    Map<ExternalId, String> idsToNames = new HashMap<>();
    idsToNames.put(ExternalId.of("CurrencyISO", "USD"), CURVE_CONSTRUCTION_CONFIGURATION_USD_FFF);
    return new ExposureFunctions("USD_FFF", exposureFunctions, idsToNames);
  }

  public static ExposureFunctions mockCounterpartyExposureFunctions() {
    List<String> exposureFunctions =  ImmutableList.of(CounterpartyExposureFunction.NAME);
    Map<ExternalId, String> idsToNames = new HashMap<>();
    idsToNames.put(COUNTERPARTY_A, CURVE_CONSTRUCTION_CONFIGURATION_COUNTERPARTY_A);
    return new ExposureFunctions(EXPOSURE_FUNCTION_COUNTERPARTY, exposureFunctions, idsToNames);
  }

  public static ExposureFunctions mockCurrencyExposureFunctions() {
    List<String> exposureFunctions =  ImmutableList.of(CurrencyExposureFunction.NAME);
    Map<ExternalId, String> idsToNames = new HashMap<>();
    idsToNames.put(CURRENCY_USD, CURVE_CONSTRUCTION_CONFIGURATION_CURRENCY_USD);
    return new ExposureFunctions(EXPOSURE_FUNCTION_CURRENCY, exposureFunctions, idsToNames);
  }

  public static ExposureFunctions mockTradeAttributeExposureFunctions() {
    List<String> exposureFunctions =  ImmutableList.of(TradeAttributeExposureFunction.NAME);
    Map<ExternalId, String> idsToNames = new HashMap<>();
    ExternalId id = ExternalId.of(TradeAttributeExposureFunction.TRADE_ATTRIBUTE_IDENTIFIER, "TEST=PASS");
    idsToNames.put(id, CURVE_CONSTRUCTION_CONFIGURATION_CURRENCY_USD);
    return new ExposureFunctions(EXPOSURE_FUNCTION_TRADE, exposureFunctions, idsToNames);
  }

  private static CurveNodeIdMapper getUSDDiscountingCurveMapper() {
    Map<Tenor, CurveInstrumentProvider> cashNodes = Maps.newHashMap();
    cashNodes.put(Tenor.ON, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D1")));
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
    cashNodes.put(Tenor.ON, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "D2")));
    return CurveNodeIdMapper.builder()
        .name(USD_DISC_OVERNIGHT_MAPPER)
        .cashNodeIds(cashNodes)
        .build();
  }

  /**
   * The node Id mapper for (USD) Fed Fund futures.
   * @return The mapper.
   */
  private static CurveNodeIdMapper getUSDFFMapper() {
    Map<Tenor, CurveInstrumentProvider> futuresNodes = Maps.newHashMap();
    futuresNodes.put(Tenor.ONE_DAY, new BloombergFutureCurveInstrumentProvider("FF", "Comdty", MarketDataRequirementNames.MARKET_VALUE, DataFieldType.OUTRIGHT));
    return CurveNodeIdMapper.builder()
        .name(USD_FF_MAPPER)
        .rateFutureNodeIds(futuresNodes)
        .build();
  }

  private static InterpolatedCurveDefinition getUSDDiscountingCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new CashNode(Tenor.ofDays(0), Tenor.ON, _discConventionId, USD_DISC_MAPPER));
    nodes.add(new CashNode(Tenor.ON, Tenor.ON, _discConventionId, USD_DISC_OVERNIGHT_MAPPER));
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
    return new InterpolatedCurveDefinition(USD_OIS_CURVE_NAME, nodes, "Linear", "FlatExtrapolator", "FlatExtrapolator");
  }


  /**
   * Returns the interpolated curve definition for a curve based on Fed Fund futures.
   * @return The definition.
   */
  private static InterpolatedCurveDefinition getUSDFedFundFuturesCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new RateFutureNode(1, Tenor.ONE_DAY, Tenor.ONE_MONTH, Tenor.ONE_DAY, _fffConventionId, USD_FF_MAPPER, "FFF-1"));
    nodes.add(new RateFutureNode(2, Tenor.ONE_DAY, Tenor.ONE_MONTH, Tenor.ONE_DAY, _fffConventionId, USD_FF_MAPPER, "FFF-2"));
    nodes.add(new RateFutureNode(3, Tenor.ONE_DAY, Tenor.ONE_MONTH, Tenor.ONE_DAY, _fffConventionId, USD_FF_MAPPER, "FFF-3"));
    nodes.add(new RateFutureNode(4, Tenor.ONE_DAY, Tenor.ONE_MONTH, Tenor.ONE_DAY, _fffConventionId, USD_FF_MAPPER, "FFF-4"));
    return new InterpolatedCurveDefinition(USD_FFF_CURVE_NAME, nodes, "Linear", "FlatExtrapolator", "FlatExtrapolator");
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
        .name(USD_LIBOR3M_MAPPER)
        .cashNodeIds(cashNodes)
        .fraNodeIds(fraNodes)
        .swapNodeIds(swapNodes)
        .build();
  }

  private static InterpolatedCurveDefinition get3MLiborCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new CashNode(Tenor.ofDays(0), Tenor.THREE_MONTHS, _liborIndexId, USD_LIBOR3M_MAPPER));
    nodes.add(new FRANode(Tenor.THREE_MONTHS, Tenor.SIX_MONTHS, _liborIndexId, USD_LIBOR3M_MAPPER));
    nodes.add(new FRANode(Tenor.SIX_MONTHS, Tenor.NINE_MONTHS, _liborIndexId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ONE_YEAR,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.TWO_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.THREE_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.FOUR_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.FIVE_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.SEVEN_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.TEN_YEARS,
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(12),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(15),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(20),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(25),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    nodes.add(new SwapNode(Tenor.ofDays(0), Tenor.ofYears(30),
                           _liborPayLegConventionId, _liborReceiveLegConventionId, USD_LIBOR3M_MAPPER));
    return new InterpolatedCurveDefinition(USD_LIBOR3M_CURVE_NAME, nodes, "Linear", "FlatExtrapolator", "FlatExtrapolator");
  }

  private static ImmutableMap<Class<?>, Object> generateComponentMap(Object... components) {
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    for (Object component : components) {
      builder.put(component.getClass().getInterfaces()[0], component);
    }
    return builder.build();
  }


  private static HistoricalTimeSeriesSource mockHistoricalTimeSeriesSource() {
    // return 5 years of flat data.
    final LocalDate now = LocalDate.now();
    final LocalDateDoubleTimeSeriesBuilder series = ImmutableLocalDateDoubleTimeSeries.builder();
    for (LocalDate date = LocalDate.now(); date.isAfter(now.minusYears(5)); date = DateUtils.previousWeekDay(date)) {
      series.put(date, 0.01);
    }
    final HistoricalTimeSeriesSource mock = mock(HistoricalTimeSeriesSource.class);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);
    final LocalDate[] dateFixing = new LocalDate[] {LocalDate.of(2014, 4, 1), LocalDate.of(2014, 4, 2), LocalDate.of(2014, 4, 3), LocalDate.of(2014, 4, 4),
      LocalDate.of(2014, 4, 7), LocalDate.of(2014, 4, 8), LocalDate.of(2014, 4, 9), LocalDate.of(2014, 4, 10), LocalDate.of(2014, 4, 11),
      LocalDate.of(2014, 4, 14), LocalDate.of(2014, 4, 15) };
    final double[] rateFixing = new double[] {0.0010, 0.0011, 0.0012, 0.0013,
      0.0014, 0.0015, 0.0015, 0.0015, 0.0015,
      0.0014, 0.0015 };
    final LocalDateDoubleTimeSeries fixingFedFund = ImmutableLocalDateDoubleTimeSeries.of(dateFixing, rateFixing);
    final HistoricalTimeSeries hts = new SimpleHistoricalTimeSeries(_onIndexUniqueId, fixingFedFund);
    when(mock.getHistoricalTimeSeries(eq(MarketDataRequirementNames.MARKET_VALUE), eq(_onIndexId.toBundle()), eq("DEFAULT_TSS"),
        any(LocalDate.class), eq(true), any(LocalDate.class), eq(true))).thenReturn(hts);
    when(mock.getHistoricalTimeSeries(anyString(), eq(getLiborIndexId().toBundle()), anyString(),
                                      any(LocalDate.class), anyBoolean(), any(LocalDate.class), anyBoolean()))
        .thenReturn(new SimpleHistoricalTimeSeries(UniqueId.of("HTSid", LIBOR_INDEX), series.build()));
    when(mock.getHistoricalTimeSeries(anyString(), eq(getOvernightIndexId().toBundle()), anyString(),
                                      any(LocalDate.class), anyBoolean(), any(LocalDate.class), anyBoolean()))
        .thenReturn(new SimpleHistoricalTimeSeries(UniqueId.of("HTSid", USD_OVERNIGHT_CONVENTION), series.build()));

    // TODO this is because DefaultHistoricalMarketDataFn uses a different method on the TS source from DefaultHistoricalTimeSeriesFn
    when(mock.getHistoricalTimeSeries(eq(getOvernightIndexId().toBundle()), anyString(), anyString(),
                                      anyString(), any(LocalDate.class), anyBoolean(), any(LocalDate.class), anyBoolean()))
        .thenReturn(new SimpleHistoricalTimeSeries(UniqueId.of("HTSid", USD_OVERNIGHT_CONVENTION), series.build()));

    return mock;
  }

  private static HolidaySource mockHolidaySource() {
    return new UsdHolidaySource();
  }

  private static RegionSource mockRegionSource() {
    RegionMaster regionMaster = new InMemoryRegionMaster();

    SimpleRegion regionUs = new SimpleRegion();
    regionUs.addExternalId(s_USID);
    regionUs.addExternalId(ExternalSchemes.currencyRegionId(USD));
    regionUs.setUniqueId(UniqueId.of("REGION", "1"));
    SimpleRegion regionGb = new SimpleRegion();
    regionGb.addExternalId(s_GBID);
    regionGb.addExternalId(ExternalSchemes.currencyRegionId(GBP));
    regionGb.setUniqueId(UniqueId.of("REGION", "2"));
    regionMaster.add(new RegionDocument(regionUs));
    regionMaster.add(new RegionDocument(regionGb));
    return new MasterRegionSource(regionMaster);
  }

  private static ConventionSource mockConventionSource() {
    BusinessDayConvention following = BusinessDayConventions.FOLLOWING;
    DayCount act360 = DayCounts.ACT_360;
    StubType noStub = StubType.NONE;

    ConventionMaster master = new InMemoryConventionMaster();
    ConventionSource mock = mock(ConventionSource.class);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);

    SwapFixedLegConvention descPayLegConvention =
        new SwapFixedLegConvention(DISC_LEG_CONVENTION, _discPayLegConventionId.toBundle(), Tenor.ONE_YEAR,
                                   act360,
                                   BusinessDayConventions.MODIFIED_FOLLOWING, s_USD, s_USID, 2, true,
                                   StubType.SHORT_START, false, 2);
    descPayLegConvention.setUniqueId(UniqueId.of("CONV", "1"));
    master.add(new ConventionDocument(descPayLegConvention));

    OvernightIndexConvention onConvention =
        new OvernightIndexConvention(USD_OVERNIGHT_CONVENTION, _onConventionId.toBundle(), act360, 1, s_USD, s_USID);
    onConvention.setUniqueId(UniqueId.of("CONV", "2"));
    master.add(new ConventionDocument(onConvention));

    FederalFundsFutureConvention fffConvention =
        new FederalFundsFutureConvention(USD_FEDFUNDFUTURES_CONVENTION, _fffConventionId.toBundle(), 
            ExternalId.of("EXPIRY_CONVENTION", FedFundFutureAndFutureOptionMonthlyExpiryCalculator.NAME), 
            s_USID, _onIndexId, 5000000);
    fffConvention.setUniqueId(UniqueId.of("CONV", "3"));
    master.add(new ConventionDocument(fffConvention));

    OISLegConvention descReceiveLegConvention =
        new OISLegConvention(DISC_RECEIVE_LEG_CONVENTION, _discReceiveLegConventionId.toBundle(),
                             _onIndexId, Tenor.ONE_YEAR,
                             BusinessDayConventions.MODIFIED_FOLLOWING, 2, true, noStub, false, 2);
    descReceiveLegConvention.setUniqueId(UniqueId.of("CONV", "4"));
    master.add(new ConventionDocument(descReceiveLegConvention));


    DepositConvention descConvention =
        new DepositConvention(DISC_CONVENTION, _discConventionId.toBundle(), act360, following, 0, false, s_USD, s_USID);
    descConvention.setUniqueId(UniqueId.of("CONV", "5"));
    master.add(new ConventionDocument(descConvention));
    master.add(new ConventionDocument(LIBOR_PAY_LEG_CONVENTION));


    VanillaIborLegConvention liborReceiveLegConvention =
        new VanillaIborLegConvention(LIBOR_RECEIVE_LEG_CONVENTION_NAME, _liborReceiveLegConventionId.toBundle(),
                                     _liborIndexId, true, "Linear", Tenor.THREE_MONTHS, 2, true,
                                     StubType.SHORT_START, false, 0);
    liborReceiveLegConvention.setUniqueId(UniqueId.of("CONV", "6"));
    master.add(new ConventionDocument(liborReceiveLegConvention));


    IborIndexConvention liborConvention =
        new IborIndexConvention(LIBOR_CONVENTION, _liborConventionId.toBundle(), act360,
                                BusinessDayConventions.MODIFIED_FOLLOWING, 2,
                                false, s_USD, LocalTime.of(11, 0), "Europe/London", s_USGBID, s_USID, "");
    liborConvention.setUniqueId(UniqueId.of("CONV", "7"));
    master.add(new ConventionDocument(liborConvention));


    // TODO - We have 2 ids for the same convention - we should try to converge them

    FederalFundsFutureConvention fedFundsFutureConvention =
        new FederalFundsFutureConvention(PerCurrencyConventionHelper.FED_FUNDS_FUTURE,
                                         ExternalIdBundle.of(ExternalId.of(SCHEME_NAME, FED_FUNDS_FUTURE)),
                                         ExternalId.of(ExchangeTradedInstrumentExpiryCalculator.SCHEME, FedFundFutureAndFutureOptionMonthlyExpiryCalculator.NAME),
                                         s_USID,
                                         _onIndexId,
                                         5000000);
    fedFundsFutureConvention.setUniqueId(UniqueId.of("CONV", "8"));
    master.add(new ConventionDocument(fedFundsFutureConvention));

    return new MasterConventionSource(master);
  }

  private static SecuritySource mockSecuritySource() {

    SecurityMaster securityMaster = new InMemorySecurityMaster();

    OvernightIndex onIndex = new OvernightIndex(USD_FEDFUND_INDEX, _onConventionId);
    onIndex.setUniqueId(UniqueId.of("SEC", "1"));
    onIndex.setExternalIdBundle(_onIndexId.toBundle());

    securityMaster.add(new SecurityDocument(onIndex));

    IborIndex iIndex = new IborIndex(LIBOR_INDEX, Tenor.THREE_MONTHS, _liborConventionId);
    iIndex.setUniqueId(UniqueId.of("SEC", "2"));
    iIndex.setExternalIdBundle(_liborIndexId.toBundle());

    securityMaster.add(new SecurityDocument(iIndex));

    return new MasterSecuritySource(securityMaster);
  }

  private static ConfigSource mockConfigSource() {

    //Config source mock
    ConfigSource mock = mock(ConfigSource.class);

    IborCurveTypeConfiguration liborCurveTypeConfig = new IborCurveTypeConfiguration(_liborIndexId, Tenor.THREE_MONTHS);
    OvernightCurveTypeConfiguration onCurveTypeConfig = new OvernightCurveTypeConfiguration(_onIndexId);
    DiscountingCurveTypeConfiguration discCurveTypeConfig = new DiscountingCurveTypeConfiguration("USD");

    Map<String, List<? extends CurveTypeConfiguration>> curveNameTypeMap = Maps.newHashMap();

    curveNameTypeMap.put(USD_LIBOR3M_CURVE_NAME, Arrays.asList(liborCurveTypeConfig));
    curveNameTypeMap.put(USD_OIS_CURVE_NAME, Arrays.asList(onCurveTypeConfig, discCurveTypeConfig));

    CurveGroupConfiguration curveGroupConfig = new CurveGroupConfiguration(0, curveNameTypeMap);
    List<CurveGroupConfiguration> curveGroupConfigs = ImmutableList.of(curveGroupConfig);

    List<String> exogenousConfigurations = ImmutableList.of();

    CurveConstructionConfiguration curveConfig = new CurveConstructionConfiguration(
        CURVE_CONSTRUCTION_CONFIGURATION_USD_OIS_LIB3, curveGroupConfigs, exogenousConfigurations);
    ConfigItem<CurveConstructionConfiguration> curveConfigItem = ConfigItem.of(curveConfig);
    curveConfigItem.setUniqueId(UniqueId.of("CONFIG", "1"));
    when(mock.get(eq(CurveConstructionConfiguration.class),
                  eq(CURVE_CONSTRUCTION_CONFIGURATION_USD_OIS_LIB3),
                  any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(curveConfigItem));

    //For currency exposure function integration test
    curveConfigItem.setUniqueId(UniqueId.of("CONFIG", CURVE_CONSTRUCTION_CONFIGURATION_CURRENCY_USD));
    when(mock.get(eq(CurveConstructionConfiguration.class),
                  eq(CURVE_CONSTRUCTION_CONFIGURATION_CURRENCY_USD),
                  any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(curveConfigItem));

    //For counterparty exposure function integration test
    curveConfigItem.setUniqueId(UniqueId.of("CONFIG", CURVE_CONSTRUCTION_CONFIGURATION_COUNTERPARTY_A));
    when(mock.get(eq(CurveConstructionConfiguration.class),
                  eq(CURVE_CONSTRUCTION_CONFIGURATION_COUNTERPARTY_A),
                  any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(curveConfigItem));

    //For trade exposure function integration test
    curveConfigItem.setUniqueId(UniqueId.of("CONFIG", CURVE_CONSTRUCTION_CONFIGURATION_TRADE_A));
    when(mock.get(eq(CurveConstructionConfiguration.class),
                  eq(CURVE_CONSTRUCTION_CONFIGURATION_TRADE_A),
                  any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(curveConfigItem));
    
    Map<String, List<? extends CurveTypeConfiguration>> fffCurveNameTypeMap = 
        ImmutableMap.<String, List<? extends CurveTypeConfiguration>>of(USD_FFF_CURVE_NAME, Arrays.asList(onCurveTypeConfig, discCurveTypeConfig));

    CurveGroupConfiguration fffCurveGroupConfig = new CurveGroupConfiguration(0, fffCurveNameTypeMap);
    List<CurveGroupConfiguration> fffCurveGroupConfigs = ImmutableList.of(fffCurveGroupConfig);
    CurveConstructionConfiguration fffCurveConfig = new CurveConstructionConfiguration(
        CURVE_CONSTRUCTION_CONFIGURATION_USD_FFF, fffCurveGroupConfigs, exogenousConfigurations);
    ConfigItem<CurveConstructionConfiguration> fffCurveConfigItem = ConfigItem.of(fffCurveConfig);
    fffCurveConfigItem.setUniqueId(UniqueId.of("CONFIG", "2"));
    when(mock.get(eq(CurveConstructionConfiguration.class),
                  eq(CURVE_CONSTRUCTION_CONFIGURATION_USD_FFF),
                  any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(fffCurveConfigItem));
    
    //return curve definitions via mock
    ConfigItem<Object> usdDiscountingCurveDefItem = ConfigItem.<Object>of(getUSDDiscountingCurveDefinition());
    usdDiscountingCurveDefItem.setUniqueId(UniqueId.of("CONFIG", "3"));
    when(mock.get(any(Class.class), eq(USD_OIS_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(usdDiscountingCurveDefItem));
    when(mock.getSingle(any(Class.class), eq(USD_OIS_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(usdDiscountingCurveDefItem.getValue());

    ConfigItem<Object> libor3mCurveDefinitionItem = ConfigItem.<Object>of(get3MLiborCurveDefinition());
    libor3mCurveDefinitionItem.setUniqueId(UniqueId.of("CONFIG", "4"));
    when(mock.get(eq(Object.class), eq(USD_LIBOR3M_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(libor3mCurveDefinitionItem));
    when(mock.getSingle(any(Class.class), eq(USD_LIBOR3M_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(libor3mCurveDefinitionItem.getValue());

    ConfigItem<Object> usdFedFundFuturesCurveDefItem = ConfigItem.<Object>of(getUSDFedFundFuturesCurveDefinition());
    usdFedFundFuturesCurveDefItem.setUniqueId(UniqueId.of("CONFIG", "5"));
    when(mock.get(eq(Object.class), eq(USD_FFF_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(usdFedFundFuturesCurveDefItem));
    when(mock.getSingle(any(Class.class), eq(USD_FFF_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(usdFedFundFuturesCurveDefItem.getValue());

    //return node mappers via mock

    // Doubled up mocking to work around CacheAwareConfigSource / calls to getSingle
    CurveNodeIdMapper usdDiscountingCurveMapper = getUSDDiscountingCurveMapper();
    ConfigItem<CurveNodeIdMapper> usdDiscountingCurveMapperItem = ConfigItem.of(usdDiscountingCurveMapper);
    usdDiscountingCurveMapperItem.setUniqueId(UniqueId.of("CONFIG", "6"));
    when(mock.getSingle(CurveNodeIdMapper.class, USD_DISC_MAPPER, VersionCorrection.LATEST))
        .thenReturn(usdDiscountingCurveMapper);
    when(mock.get(CurveNodeIdMapper.class, USD_DISC_MAPPER, VersionCorrection.LATEST))
        .thenReturn(ImmutableSet.of(usdDiscountingCurveMapperItem));

    CurveNodeIdMapper usdDiscountingOvernightCurveMapper = getUSDDiscountingOvernightCurveMapper();
    ConfigItem<CurveNodeIdMapper> usdDiscountingOvernightCurveMapperItem = ConfigItem.of(
        usdDiscountingOvernightCurveMapper);
    usdDiscountingOvernightCurveMapperItem.setUniqueId(UniqueId.of("CONFIG", "7"));
    when(mock.getSingle(CurveNodeIdMapper.class, USD_DISC_OVERNIGHT_MAPPER, VersionCorrection.LATEST))
        .thenReturn(usdDiscountingOvernightCurveMapper);
    when(mock.get(CurveNodeIdMapper.class, USD_DISC_OVERNIGHT_MAPPER, VersionCorrection.LATEST))
        .thenReturn(ImmutableSet.of(usdDiscountingOvernightCurveMapperItem));

    CurveNodeIdMapper libor3mCurveMapper = get3MLiborCurveMapper();
    ConfigItem<CurveNodeIdMapper> libor3mCurveMapperItem = ConfigItem.of(libor3mCurveMapper);
    libor3mCurveMapperItem.setUniqueId(UniqueId.of("CONFIG", "7"));
    when(mock.getSingle(CurveNodeIdMapper.class, USD_LIBOR3M_MAPPER, VersionCorrection.LATEST))
        .thenReturn(libor3mCurveMapper);
    when(mock.get(CurveNodeIdMapper.class, USD_LIBOR3M_MAPPER, VersionCorrection.LATEST))
        .thenReturn(ImmutableSet.of(libor3mCurveMapperItem));

    CurveNodeIdMapper usdffMapper = getUSDFFMapper();
    ConfigItem<CurveNodeIdMapper> usdffMapperItem = ConfigItem.of(usdffMapper);
    usdffMapperItem.setUniqueId(UniqueId.of("CONFIG", "9"));
    when(mock.getSingle(CurveNodeIdMapper.class, USD_FF_MAPPER, VersionCorrection.LATEST))
        .thenReturn(usdffMapper);
    when(mock.get(CurveNodeIdMapper.class, USD_FF_MAPPER, VersionCorrection.LATEST))
        .thenReturn(ImmutableSet.of(usdffMapperItem));

    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);

    SabrConfigSelector sabrConfigSelector = buildSabrConfigSelector();
    ConfigItem<SabrConfigSelector> sabrConfigItem = ConfigItem.of(sabrConfigSelector);
    sabrConfigItem.setUniqueId(UniqueId.of("CONFIG", "10"));
    when(mock.get(any(Class.class), eq("TEST_SABR"), any(VersionCorrection.class)))
        .thenReturn(ImmutableList.of(sabrConfigItem));

    return mock;
  }

  private static SabrConfigSelector buildSabrConfigSelector() {

    SabrExpiryTenorSurface alphaSurface = buildSabrExpiryTenorSurface(
        "USD_ALPHA",
        new double[]{0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10},
        new double[]{1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 10},
        new double[]{0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06});

    SabrExpiryTenorSurface betaSurface = buildSabrExpiryTenorSurface(
        "USD_BETA",
        new double[]{0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10},
        new double[]{1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 10},
        new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5});

    SabrExpiryTenorSurface rhoSurface = buildSabrExpiryTenorSurface(
        "USD_RHO",
        new double[]{0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10},
        new double[]{1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 10},
        new double[]{-0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, 0.00, 0.00, 0.00, 0.00});

    SabrExpiryTenorSurface nuSurface = buildSabrExpiryTenorSurface(
        "USD_NU",
        new double[]{0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10},
        new double[]{1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 10},
        new double[]{0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.30, 0.30, 0.30, 0.30});

    Map<SabrSurfaceName, SnapshotLink<SabrExpiryTenorSurface>> surfaceMap = ImmutableMap.of(
        SabrSurfaceName.ALPHA, SnapshotLink.resolved(alphaSurface),
        SabrSurfaceName.BETA, SnapshotLink.resolved(betaSurface),
        SabrSurfaceName.RHO, SnapshotLink.resolved(rhoSurface),
        SabrSurfaceName.NU, SnapshotLink.resolved(nuSurface));

    SabrSurfaceSelector<SwapFixedLegConvention, SabrExpiryTenorSurface> usdSurfaceSelector =
        SabrSurfaceSelector.<SwapFixedLegConvention, SabrExpiryTenorSurface>builder()
            .convention(ConventionLink.resolved(LIBOR_PAY_LEG_CONVENTION))
            .sabrSurfaceMap(surfaceMap)
            .build();

    SabrSwaptionDataConfig sabrSwaptionDataConfig = SabrSwaptionDataConfig.builder()
        .currencyMap(ImmutableMap.of(USD, usdSurfaceSelector))
        .build();

    SabrSwaptionInterpolationConfig interpolationConfig = SabrSwaptionInterpolationConfig.builder()
        .expiryInterpolatorName(LINEAR)
        .leftExpiryExtrapolatorName(FLAT_EXTRAPOLATOR)
        .rightExpiryExtrapolatorName(FLAT_EXTRAPOLATOR)
        .tenorInterpolatorName(LINEAR)
        .leftTenorExtrapolatorName(FLAT_EXTRAPOLATOR)
        .rightTenorExtrapolatorName(FLAT_EXTRAPOLATOR)
        .build();

    SabrSwaptionConfig sabrConfig = SabrSwaptionConfig.builder()
        .sabrDataConfig(ConfigLink.resolved(sabrSwaptionDataConfig))
        .sabrInterpolationConfig(ConfigLink.resolved(interpolationConfig))
        .build();

    Map<Class<? extends FinancialSecurity>, SabrSwaptionConfig> configurations =
        ImmutableMap.<Class<? extends FinancialSecurity>, SabrSwaptionConfig>of(SwaptionSecurity.class, sabrConfig);

    return SabrConfigSelector.builder().configurations(configurations).build();
  }

  private static SabrExpiryTenorSurface buildSabrExpiryTenorSurface(String name, double[] xs, double[] ys, double[] zs) {
    List<SabrNode> nodes = new ArrayList<>();
    for (int i = 0; i < xs.length; i++) {
      nodes.add(SabrNode.builder().x(xs[i]).y(ys[i]).z(zs[i]).build());
    }
    return new SabrExpiryTenorSurface(name, nodes);
  }

}
