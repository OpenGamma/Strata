/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.sources;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.testng.internal.annotations.Sets;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetTime;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.analytics.financial.legalentity.LegalEntity;
import com.opengamma.analytics.financial.legalentity.LegalEntityFilter;
import com.opengamma.analytics.financial.legalentity.LegalEntityShortName;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.historicaltimeseries.impl.SimpleHistoricalTimeSeries;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.holiday.impl.WeekendHolidaySource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.legalentity.LegalEntitySource;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.impl.SimpleCounterparty;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.region.impl.SimpleRegion;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveNodeIdMapper;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.InterpolatedCurveDefinition;
import com.opengamma.financial.analytics.curve.IssuerCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.analytics.ircurve.CurveInstrumentProvider;
import com.opengamma.financial.analytics.ircurve.StaticCurveInstrumentProvider;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.PeriodicallyCompoundedRateNode;
import com.opengamma.financial.convention.ConventionBundle;
import com.opengamma.financial.convention.ConventionBundleImpl;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.financial.convention.InMemoryConventionBundleMaster;
import com.opengamma.financial.convention.businessday.ModifiedFollowingBusinessDayConvention;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.convention.frequency.Frequency;
import com.opengamma.financial.convention.frequency.PeriodFrequency;
import com.opengamma.financial.convention.yield.SimpleYieldConvention;
import com.opengamma.financial.convention.yield.YieldConvention;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.security.bond.BondSecurity;
import com.opengamma.financial.security.bond.CorporateBondSecurity;
import com.opengamma.financial.security.bond.GovernmentBondSecurity;
import com.opengamma.financial.security.future.BondFutureDeliverable;
import com.opengamma.financial.security.future.BondFutureSecurity;
import com.opengamma.financial.security.option.BondFutureOptionSecurity;
import com.opengamma.financial.security.option.EuropeanExerciseType;
import com.opengamma.financial.security.option.ExerciseType;
import com.opengamma.financial.security.option.OptionType;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
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
import com.opengamma.sesame.bond.BondCalculatorFactory;
import com.opengamma.sesame.bond.BondFn;
import com.opengamma.sesame.bond.DefaultBondFn;
import com.opengamma.sesame.bond.DiscountingBondCalculatorFactory;
import com.opengamma.sesame.bondfuture.BondFutureCalculatorFactory;
import com.opengamma.sesame.bondfuture.BondFutureDiscountingCalculatorFactory;
import com.opengamma.sesame.bondfuture.BondFutureFn;
import com.opengamma.sesame.bondfuture.DefaultBondFutureFn;
import com.opengamma.sesame.bondfutureoption.BlackBondFuturesProviderFn;
import com.opengamma.sesame.bondfutureoption.BondFutureOptionBlackCalculatorFactory;
import com.opengamma.sesame.bondfutureoption.BondFutureOptionCalculatorFactory;
import com.opengamma.sesame.bondfutureoption.BondFutureOptionFn;
import com.opengamma.sesame.bondfutureoption.DefaultBondFutureOptionFn;
import com.opengamma.sesame.bondfutureoption.TestBlackBondFuturesProviderFn;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.MapMarketDataSource;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.sesame.trade.BondFutureOptionTrade;
import com.opengamma.sesame.trade.BondFutureTrade;
import com.opengamma.sesame.trade.BondTrade;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Expiry;
import com.opengamma.util.time.Tenor;

/**
 * Unit test helper to mock sources for bond pricing.
 */
public class BondMockSources {
  
  private static final ChangeManager MOCK_CHANGE_MANAGER = mock(ChangeManager.class);

  /*Static data*/
  private static final String TICKER = "Ticker";
  private static final String GOVERNMENT_BOND_ISSUER_KEY = "UK GOVERNMENT";
  private static final String CORPORATE_BOND_ISSUER_KEY = "TELECOM ITALIA SPA";
  private static final String BOND_EXPOSURE_FUNCTIONS = "Test Bond Exposure Functions";
  private static final ExternalId GB_ID = ExternalSchemes.financialRegionId("GB");
  private static final ExternalId US_ID = ExternalSchemes.financialRegionId("US");
  private static final ExternalId IT_ID = ExternalSchemes.financialRegionId("IT");

  /*USD and GBP curve share all the same data, except the name*/
  private static final String BOND_CURVE_NODE_ID_MAPPER = "Test Bond Mapper";
  private static final String BOND_USD_CURVE_NAME = "USD Bond Curve";
  public static final String BOND_GBP_CURVE_NAME = "GBP Bond Curve";
  private static final String BOND_CURVE_CONFIG_NAME = "Test Bond Curve Config";

  /*Bond*/
  public static final BondSecurity GOVERNMENT_BOND_SECURITY = createGovernmentBondSecurity();
  public static final BondSecurity CORPORATE_BOND_SECURITY = createCorporateBondSecurity();
  public static final BondTrade GOVERNMENT_BOND_TRADE = createGovernmentBondTrade();
  public static final BondTrade CORPORATE_BOND_TRADE = createCorporateBondTrade();

  /*Bond Future*/
  public static final BondFutureSecurity BOND_FUTURE_SECURITY = createBondFutureSecurity();
  public static final BondFutureTrade BOND_FUTURE_TRADE = createBondFutureTrade();

  /*Bond Future Option*/
  public static final BondFutureOptionSecurity BOND_FUTURE_OPTION_SECURITY = createBondFutureOptionSecurity();
  public static final BondFutureOptionTrade BOND_FUTURE_OPTION_TRADE = createBondFutureOptionTrade();

  /*Environment*/
  private static final ZonedDateTime VALUATION_TIME = DateUtils.getUTCDate(2014, 7, 22);
  public static final Environment ENV = new SimpleEnvironment(BondMockSources.VALUATION_TIME,
                                                              BondMockSources.createMarketDataSource());

  private static CurveNodeIdMapper getBondCurveNodeIdMapper() {
    Map<Tenor, CurveInstrumentProvider> bondNodes = Maps.newHashMap();
    bondNodes.put(Tenor.ONE_YEAR, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B1")));
    bondNodes.put(Tenor.TWO_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B2")));
    bondNodes.put(Tenor.THREE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B3")));
    bondNodes.put(Tenor.FOUR_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B4")));
    bondNodes.put(Tenor.FIVE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B5")));
    bondNodes.put(Tenor.SIX_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B6")));
    bondNodes.put(Tenor.SEVEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B7")));
    bondNodes.put(Tenor.EIGHT_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B8")));
    bondNodes.put(Tenor.NINE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B9")));
    bondNodes.put(Tenor.TEN_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B10")));
    return CurveNodeIdMapper.builder().name(BOND_CURVE_NODE_ID_MAPPER)
                                      .periodicallyCompoundedRateNodeIds(bondNodes)
                                      .build();
  }

  private static InterpolatedCurveDefinition getBondUsdCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.ONE_YEAR, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.TWO_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.THREE_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.FOUR_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.FIVE_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.SIX_YEARS, 1));
    return new InterpolatedCurveDefinition(BOND_USD_CURVE_NAME, nodes, Interpolator1DFactory.LINEAR,
                                           Interpolator1DFactory.FLAT_EXTRAPOLATOR,
                                           Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  }

  private static InterpolatedCurveDefinition getBondGbpCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.ONE_YEAR, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.TWO_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.THREE_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.FOUR_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.FIVE_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.SIX_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.SEVEN_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.EIGHT_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.NINE_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.TEN_YEARS, 1));
    return new InterpolatedCurveDefinition(BOND_GBP_CURVE_NAME, nodes, Interpolator1DFactory.LINEAR,
                                           Interpolator1DFactory.FLAT_EXTRAPOLATOR,
                                           Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  }

  public static FunctionModelConfig getConfig() {

    return config(
        arguments(
            function(ConfigDbMarketExposureSelectorFn.class,
                     argument("exposureConfig", ConfigLink.resolvable(BondMockSources.BOND_EXPOSURE_FUNCTIONS,
                                                                      ExposureFunctions.class))),
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
        implementations(CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                        FXMatrixFn.class, DefaultFXMatrixFn.class,
                        DiscountingMulticurveCombinerFn.class, ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                        IssuerProviderFn.class, ExposureFunctionsIssuerProviderFn.class,
                        IssuerProviderBundleFn.class, InterpolatedIssuerBundleFn.class,
                        CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                        DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                        CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                        CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class,
                        HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class,
                        MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                        MarketDataFn.class, DefaultMarketDataFn.class,
                        /*Bond*/
                        BondFn.class, DefaultBondFn.class,
                        BondCalculatorFactory.class, DiscountingBondCalculatorFactory.class,
                        /*Bond Future Option*/
                        BondFutureOptionFn.class, DefaultBondFutureOptionFn.class,
                        BondFutureOptionCalculatorFactory.class, BondFutureOptionBlackCalculatorFactory.class,
                        BlackBondFuturesProviderFn.class, TestBlackBondFuturesProviderFn.class,
                        /*Bond Future*/
                        BondFutureFn.class, DefaultBondFutureFn.class,
                        BondFutureCalculatorFactory.class, BondFutureDiscountingCalculatorFactory.class)
    );

  }
  
  @SuppressWarnings("unchecked")
  private static CurveConstructionConfiguration getBondCurveConfig() {
    Set<LegalEntityFilter<LegalEntity>> filters = Sets.newHashSet();
    filters.add(new LegalEntityShortName());

    Set<Object> govKeys = Sets.newHashSet();
    govKeys.add(GOVERNMENT_BOND_ISSUER_KEY);

    Set<Object> corpKeys = Sets.newHashSet();
    corpKeys.add(CORPORATE_BOND_ISSUER_KEY);

    List<CurveTypeConfiguration> configs = Lists.newArrayList();
    configs.add(new IssuerCurveTypeConfiguration(corpKeys, filters));
    configs.add(new IssuerCurveTypeConfiguration(govKeys, filters));
    
    Map<String, List<? extends CurveTypeConfiguration>> curveTypes = Maps.newHashMap();
    curveTypes.put(BOND_GBP_CURVE_NAME, configs);
    
    return new CurveConstructionConfiguration(BOND_CURVE_CONFIG_NAME,
                                              Lists.newArrayList(new CurveGroupConfiguration(0, curveTypes)),
                                              Collections.EMPTY_LIST);
  }
  
  private static ExposureFunctions getExposureFunctions() {
    List<String> exposureFunctions = ImmutableList.of("Currency");
    Map<ExternalId, String> idsToNames = Maps.newHashMap();
    idsToNames.put(ExternalId.of("CurrencyISO", Currency.GBP.getCode()), BOND_CURVE_CONFIG_NAME);
    return new ExposureFunctions(BOND_EXPOSURE_FUNCTIONS, exposureFunctions, idsToNames);
  }

  public static MarketDataSource createMarketDataSource() {
    return MapMarketDataSource.builder()
          .add(createId("B1"), 0.009154010130285646)
          .add(createId("B2"), 0.013529850844352658)
          .add(createId("B3"), 0.0172583393761524)
          .add(createId("B4"), 0.020001507249248547)
          .add(createId("B5"), 0.022004447649196877)
          .add(createId("B6"), 0.023628241845802613)
          .add(createId("B7"), 0.025005300419649393)
          .add(createId("B8"), 0.02619214367588991)
          .add(createId("B9"), 0.02719250291972944)
          .add(createId("B10"), 0.02808602151907749)
          .add(ExternalId.of("ISIN", "Test Corp bond"), 108.672)
          .add(ExternalId.of("ISIN", "Test Gov bond"), 136.375)
          .build();
  }

  private static ExternalId createId(String ticker) {
    return ExternalId.of(TICKER, ticker);
  }

  private static ImmutableMap<Class<?>, Object> generateComponentMap(Object... components) {
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    for (Object component : components) {
      builder.put(component.getClass().getInterfaces()[0], component);
    }
    return builder.build();
  }

  private static HolidaySource mockHolidaySource() {
    return new WeekendHolidaySource();
  }

  private static RegionSource mockRegionSource() {
    RegionSource mock = mock(RegionSource.class);

    SimpleRegion usRegion = new SimpleRegion();
    usRegion.addExternalId(US_ID);
    SimpleRegion euRegion = new SimpleRegion();
    euRegion.addExternalId(IT_ID);
    SimpleRegion gbRegion = new SimpleRegion();
    gbRegion.addExternalId(GB_ID);

    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);
    when(mock.getHighestLevelRegion(eq(US_ID)))
        .thenReturn(usRegion);
    when(mock.getHighestLevelRegion(eq(IT_ID)))
        .thenReturn(euRegion);
    when(mock.getHighestLevelRegion(eq(GB_ID)))
        .thenReturn(gbRegion);
    return mock;
  }
  
  private static ConventionSource mockConventionSource() {
    return mock(ConventionSource.class);
  }
  
  private static ConventionBundleSource mockConventionBundleSource() {
    ConventionBundleSource mock = mock(ConventionBundleSource.class);
    
    String usBondConvention = "US_TREASURY_BOND_CONVENTION";
    ExternalId usConventionId = ExternalId.of(InMemoryConventionBundleMaster.SIMPLE_NAME_SCHEME, usBondConvention);
    ConventionBundle usConvention =
        new ConventionBundleImpl(usConventionId.toBundle(), usBondConvention, DayCounts.THIRTY_360,
                                 new ModifiedFollowingBusinessDayConvention(), Period.ofYears(1), 1, false, US_ID);
    when(mock.getConventionBundle(eq(usConventionId))).thenReturn(usConvention);

    String gbBondConvention = "GB_TREASURY_BOND_CONVENTION";
    ExternalId gbConventionId = ExternalId.of(InMemoryConventionBundleMaster.SIMPLE_NAME_SCHEME, gbBondConvention);
    ConventionBundle gbConvention =
        new ConventionBundleImpl(gbConventionId.toBundle(), gbBondConvention, DayCounts.THIRTY_360,
                                 new ModifiedFollowingBusinessDayConvention(), Period.ofYears(1), 0, false, GB_ID);
    when(mock.getConventionBundle(eq(gbConventionId))).thenReturn(gbConvention);

    String itBondConvention = "IT_CORPORATE_BOND_CONVENTION";
    ExternalId itConventionId = ExternalId.of(InMemoryConventionBundleMaster.SIMPLE_NAME_SCHEME, itBondConvention);
    ConventionBundle itConvention =
        new ConventionBundleImpl(itConventionId.toBundle(), itBondConvention, DayCounts.ACT_ACT_ICMA,
                                 new ModifiedFollowingBusinessDayConvention(), Period.ofYears(1), 3, false, IT_ID);
    when(mock.getConventionBundle(eq(itConventionId))).thenReturn(itConvention);

    return mock;
  }
  
  private static LegalEntitySource mockLegalEntitySource() {
    return mock(LegalEntitySource.class);
  }
  
  private static ConfigSource mockConfigSource() {
    ConfigSource mock = mock(ConfigSource.class);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);
    
    // curve node id mapper
    when(mock.getSingle(CurveNodeIdMapper.class, BOND_CURVE_NODE_ID_MAPPER, VersionCorrection.LATEST))
      .thenReturn(getBondCurveNodeIdMapper());
    
    // USD curve def
    ConfigItem<Object> bondUsdCurveDefinitionItem = ConfigItem.<Object>of(getBondUsdCurveDefinition());
    when(mock.get(eq(Object.class), eq(BOND_USD_CURVE_NAME), any(VersionCorrection.class)))
      .thenReturn(ImmutableSet.of(bondUsdCurveDefinitionItem));
    when(mock.getSingle(eq(CurveDefinition.class), eq(BOND_USD_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn((CurveDefinition) bondUsdCurveDefinitionItem.getValue());

    // GBP curve def
    ConfigItem<Object> bondGbpCurveDefinitionItem = ConfigItem.<Object>of(getBondGbpCurveDefinition());
    when(mock.get(eq(Object.class), eq(BOND_GBP_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn(ImmutableSet.of(bondGbpCurveDefinitionItem));
    when(mock.getSingle(eq(CurveDefinition.class), eq(BOND_GBP_CURVE_NAME), any(VersionCorrection.class)))
        .thenReturn((CurveDefinition) bondGbpCurveDefinitionItem.getValue());


    // curve config
    when(mock.get(eq(CurveConstructionConfiguration.class), eq(BOND_CURVE_CONFIG_NAME), any(VersionCorrection.class)))
      .thenReturn(ImmutableSet.of(ConfigItem.of(getBondCurveConfig())));
    
    // exposure function
    when(mock.get(eq(ExposureFunctions.class), eq(BOND_EXPOSURE_FUNCTIONS), any(VersionCorrection.class)))
      .thenReturn(ImmutableSet.of(ConfigItem.of(getExposureFunctions())));
    
    return mock;
  }
  
  private static SecuritySource mockSecuritySource() {
    SecuritySource mock = mock(SecuritySource.class);
    when(mock.getSingle(eq(BondMockSources.GOVERNMENT_BOND_SECURITY.getExternalIdBundle())))
        .thenReturn(BondMockSources.GOVERNMENT_BOND_SECURITY);
    when(mock.getSingle(eq(BondMockSources.CORPORATE_BOND_SECURITY.getExternalIdBundle())))
        .thenReturn(BondMockSources.CORPORATE_BOND_SECURITY);
    when(mock.getSingle(eq(BondMockSources.BOND_FUTURE_SECURITY.getExternalIdBundle())))
        .thenReturn(BondMockSources.BOND_FUTURE_SECURITY);
    return mock;
  }

  private static HistoricalTimeSeriesSource mockHistoricalTimeSeriesSource() {
    HistoricalTimeSeriesSource mock =  mock(HistoricalTimeSeriesSource.class);

    HistoricalTimeSeries irFuturePrices = new SimpleHistoricalTimeSeries(UniqueId.of("Blah", "1"),
                                                                         ImmutableLocalDateDoubleTimeSeries.of(
                                                                         VALUATION_TIME.toLocalDate(),
                                                                         0.975));
    when(mock.getHistoricalTimeSeries(eq(MarketDataRequirementNames.MARKET_VALUE),
                                      any(ExternalIdBundle.class),
                                      eq("DEFAULT_TSS"),
                                      any(LocalDate.class),
                                      eq(true),
                                      any(LocalDate.class),
                                      eq(true))).thenReturn(irFuturePrices);
    return mock;
  }

  public static ImmutableMap<Class<?>, Object> generateBaseComponents() {
    return generateComponentMap(mockHolidaySource(),
                                mockRegionSource(),
                                mockConventionSource(),
                                mockConventionBundleSource(),
                                mockConfigSource(),
                                mockSecuritySource(),
                                mockHistoricalTimeSeriesSource(),
                                mock(HistoricalTimeSeriesResolver.class),
                                mock(HistoricalMarketDataFn.class),
                                mock(CurrencyMatrix.class),
                                mockLegalEntitySource());
  }

  private static BondSecurity createGovernmentBondSecurity() {

    String issuerName = BondMockSources.GOVERNMENT_BOND_ISSUER_KEY;
    String issuerDomicile = "GB";
    String issuerType = "Sovereign";
    Currency currency = Currency.GBP;
    YieldConvention yieldConvention = SimpleYieldConvention.UK_BUMP_DMO_METHOD;
    DayCount dayCountConvention = DayCounts.ACT_ACT_ICMA;

    Period couponPeriod = Period.parse("P6M");
    String couponType = "Fixed";
    double couponRate = 8.0;
    Frequency couponFrequency = PeriodFrequency.of(couponPeriod);

    ZonedDateTime maturityDate = DateUtils.getUTCDate(2021, 6, 7);
    ZonedDateTime firstCouponDate = DateUtils.getUTCDate(1996, 6, 7);
    ZonedDateTime interestAccrualDate = firstCouponDate.minus(couponPeriod);
    ZonedDateTime settlementDate = DateUtils.getUTCDate(2014, 6, 13);
    Expiry lastTradeDate = new Expiry(maturityDate);

    double issuancePrice = 100.0;
    double totalAmountIssued = 23499000000.0;
    double minimumAmount = 0.01;
    double minimumIncrement = 0.01;
    double parAmount = 100;
    double redemptionValue = 100;

    GovernmentBondSecurity bond =
        new GovernmentBondSecurity(issuerName, issuerType, issuerDomicile, issuerType, currency, yieldConvention,
                                   lastTradeDate, couponType, couponRate, couponFrequency, dayCountConvention,
                                   interestAccrualDate, settlementDate, firstCouponDate, issuancePrice,
                                   totalAmountIssued, minimumAmount, minimumIncrement, parAmount, redemptionValue);
    // Need this for time series lookup
    ExternalId bondId = ExternalSchemes.isinSecurityId("Test Gov bond");
    bond.setExternalIdBundle(bondId.toBundle());
    return bond;
  }

  private static BondSecurity createCorporateBondSecurity() {

    String issuerName = BondMockSources.CORPORATE_BOND_ISSUER_KEY;
    String issuerDomicile = "IT";
    String issuerType = "Corporate";
    Currency currency = Currency.GBP;
    YieldConvention yieldConvention = SimpleYieldConvention.US_STREET;
    DayCount dayCountConvention = DayCounts.ACT_ACT_ICMA;

    String couponType = "Fixed";
    double couponRate = 6.375;
    Period couponPeriod = Period.ofYears(1);
    Frequency couponFrequency = PeriodFrequency.of(couponPeriod);

    ZonedDateTime maturityDate = DateUtils.getUTCDate(2019, 6, 24);
    ZonedDateTime firstCouponDate = DateUtils.getUTCDate(2005, 6, 24);
    ZonedDateTime interestAccrualDate = firstCouponDate.minus(couponPeriod);
    ZonedDateTime settlementDate = DateUtils.getUTCDate(2014, 6, 13);
    Expiry lastTradeDate = new Expiry(maturityDate);

    double issuancePrice = 98.85;
    double totalAmountIssued = 850000000;
    double minimumAmount = 50000;
    double minimumIncrement = 50000;
    double parAmount = 50000;
    double redemptionValue = 100;

    CorporateBondSecurity bond =
        new CorporateBondSecurity(issuerName, issuerType, issuerDomicile, issuerType, currency, yieldConvention,
                                  lastTradeDate, couponType, couponRate, couponFrequency, dayCountConvention,
                                  interestAccrualDate, settlementDate, firstCouponDate, issuancePrice,
                                  totalAmountIssued, minimumAmount, minimumIncrement, parAmount, redemptionValue);
    // Need this for time series lookup
    ExternalId bondId = ExternalSchemes.isinSecurityId("Test Corp bond");
    bond.setExternalIdBundle(bondId.toBundle());
    return bond;
  }

  private static BondTrade createGovernmentBondTrade() {
    Counterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "COUNTERPARTY"));
    BigDecimal tradeQuantity = BigDecimal.valueOf(10000);
    LocalDate tradeDate = LocalDate.of(2014, 7, 23);
    OffsetTime tradeTime = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
    SimpleTrade trade = new SimpleTrade(GOVERNMENT_BOND_SECURITY, tradeQuantity, counterparty, tradeDate, tradeTime);
    trade.setPremium(0.0);
    trade.setPremiumDate(tradeDate);
    trade.setPremiumCurrency(Currency.GBP);
    return new BondTrade(trade);
  }

  private static BondTrade createCorporateBondTrade() {
    Counterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "COUNTERPARTY"));
    BigDecimal tradeQuantity = BigDecimal.valueOf(10000);
    LocalDate tradeDate = LocalDate.of(2014, 7, 2);
    OffsetTime tradeTime = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
    SimpleTrade trade = new SimpleTrade(CORPORATE_BOND_SECURITY, tradeQuantity, counterparty, tradeDate, tradeTime);
    trade.setPremiumDate(tradeDate);
    trade.setPremium(0.0);
    trade.setPremiumCurrency(Currency.GBP);
    return new BondTrade(trade);
  }

  private static BondFutureSecurity createBondFutureSecurity() {

    Currency currency = Currency.GBP;

    ZonedDateTime deliveryDate = DateUtils.getUTCDate(2014, 6, 18);
    Expiry expiry = new Expiry(deliveryDate);
    String tradingExchange = "";
    String settlementExchange = "";
    double unitAmount = 1;
    Collection<BondFutureDeliverable> basket = new ArrayList<>();
    BondFutureDeliverable bondFutureDeliverable =
        new BondFutureDeliverable(GOVERNMENT_BOND_SECURITY.getExternalIdBundle(), 0.9);
    basket.add(bondFutureDeliverable);

    ZonedDateTime firstDeliveryDate = deliveryDate;
    ZonedDateTime lastDeliveryDate = deliveryDate;
    String category = "test";

    BondFutureSecurity security =  new BondFutureSecurity(expiry, tradingExchange, settlementExchange, currency, unitAmount, basket,
                                  firstDeliveryDate, lastDeliveryDate, category);
    security.setExternalIdBundle(ExternalSchemes.isinSecurityId("Test bond future").toBundle());
    return security;
  }

  private static BondFutureTrade createBondFutureTrade() {

    Counterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "COUNTERPARTY"));
    BigDecimal tradeQuantity = BigDecimal.valueOf(1);
    LocalDate tradeDate = LocalDate.of(2000, 1, 1);
    OffsetTime tradeTime = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
    SimpleTrade trade = new SimpleTrade(BOND_FUTURE_SECURITY, tradeQuantity, counterparty, tradeDate, tradeTime);
    trade.setPremium(10.0);
    trade.setPremiumCurrency(Currency.GBP);
    return new BondFutureTrade(trade);
  }

  private static BondFutureOptionSecurity createBondFutureOptionSecurity() {

    String tradingExchange = "";
    String settlementExchange = "";
    Expiry expiry = BOND_FUTURE_SECURITY.getExpiry();
    ExerciseType exerciseType = new EuropeanExerciseType();
    ExternalId underlyingId = Iterables.getOnlyElement(BOND_FUTURE_SECURITY.getExternalIdBundle());
    double pointValue = Double.NaN;
    Currency currency = BOND_FUTURE_SECURITY.getCurrency();
    double strike = 0.2;
    OptionType optionType = OptionType.PUT;
    boolean margined = true;
    BondFutureOptionSecurity security = new BondFutureOptionSecurity(tradingExchange, settlementExchange, expiry,
                                                                   exerciseType, underlyingId, pointValue, margined,
                                                                   currency, strike, optionType);
    security.setExternalIdBundle(ExternalSchemes.isinSecurityId("Test bond future option").toBundle());
    return security;
  }

  private static BondFutureOptionTrade createBondFutureOptionTrade() {

    Counterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "COUNTERPARTY"));
    BigDecimal tradeQuantity = BigDecimal.valueOf(10);
    LocalDate tradeDate = LocalDate.of(2000, 1, 1);
    OffsetTime tradeTime = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
    SimpleTrade trade = new SimpleTrade(BOND_FUTURE_OPTION_SECURITY, tradeQuantity, counterparty, tradeDate, tradeTime);
    trade.setPremium(10.0);
    trade.setPremiumCurrency(Currency.GBP);
    return new BondFutureOptionTrade(trade);
  }

}
