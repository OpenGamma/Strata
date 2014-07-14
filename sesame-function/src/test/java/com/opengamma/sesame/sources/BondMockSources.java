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
import com.opengamma.sesame.bond.DiscountingBondCalculatorFactory;
import com.opengamma.sesame.bond.BondFn;
import com.opengamma.sesame.bond.DefaultBondFn;
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
  private static final String BOND_CURVE_NODE_ID_MAPPER = "Test Bond Mapper";
  private static final String BOND_CURVE_NAME = "USD Test Bond Curve";
  private static final String BOND_CURVE_CONFIG_NAME = "Test Bond Curve Config";
  public static final String BOND_ISSUER_KEY = "US GOVERNMENT";
  public static final String BOND_EXPOSURE_FUNCTIONS = "Test Bond Exposure Functions";
  private static final ExternalId s_USID = ExternalSchemes.financialRegionId("US");

  /*Bond*/
  public static final BondSecurity BOND_SECURITY = createBondSecurity();
  public static final BondTrade BOND_TRADE = createBondTrade();

  /*Bond Future*/
  public static final BondFutureSecurity BOND_FUTURE_SECURITY = createBondFutureSecurity();
  public static final BondFutureTrade BOND_FUTURE_TRADE = createBondFutureTrade();

  /*Bond Future Option*/
  public static final BondFutureOptionSecurity BOND_FUTURE_OPTION_SECURITY = createBondFutureOptionSecurity();
  public static final BondFutureOptionTrade BOND_FUTURE_OPTION_TRADE = createBondFutureOptionTrade();

  /*Environment*/
  private static final ZonedDateTime VALUATION_TIME = DateUtils.getUTCDate(2014, 1, 22);
  public static final Environment ENV = new SimpleEnvironment(BondMockSources.VALUATION_TIME,
                                                              BondMockSources.createMarketDataSource());

  private static CurveNodeIdMapper getBondCurveNodeIdMapper() {
    Map<Tenor, CurveInstrumentProvider> bondNodes = Maps.newHashMap();
    bondNodes.put(Tenor.ONE_YEAR, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B1")));
    bondNodes.put(Tenor.TWO_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B2")));
    bondNodes.put(Tenor.THREE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B3")));
    bondNodes.put(Tenor.FOUR_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B4")));
    bondNodes.put(Tenor.FIVE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B5")));
    return CurveNodeIdMapper.builder().name(BOND_CURVE_NODE_ID_MAPPER).periodicallyCompoundedRateNodeIds(bondNodes).build();
  }
  
  private static InterpolatedCurveDefinition getBondCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.ONE_YEAR, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.TWO_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.THREE_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.FOUR_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.FIVE_YEARS, 1));
    return new InterpolatedCurveDefinition(BOND_CURVE_NAME, nodes, Interpolator1DFactory.LINEAR,
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
    Set<Object> keys = Sets.newHashSet();
    keys.add(BOND_ISSUER_KEY);
    Set<LegalEntityFilter<LegalEntity>> filters = Sets.newHashSet();
    filters.add(new LegalEntityShortName());
    List<CurveTypeConfiguration> curveTypeConfigs = Lists.newArrayList();
    curveTypeConfigs.add(new IssuerCurveTypeConfiguration(keys, filters));
    
    Map<String, List<? extends CurveTypeConfiguration>> curveTypes = Maps.newHashMap();
    curveTypes.put(BOND_CURVE_NAME, curveTypeConfigs);
    
    return new CurveConstructionConfiguration(BOND_CURVE_CONFIG_NAME,
                                              Lists.newArrayList(new CurveGroupConfiguration(0, curveTypes)),
                                              Collections.EMPTY_LIST);
  }
  
  private static ExposureFunctions getExposureFunctions() {
    List<String> exposureFunctions = ImmutableList.of("Currency");
    Map<ExternalId, String> idsToNames = Maps.newHashMap();
    idsToNames.put(ExternalId.of("CurrencyISO", Currency.USD.getCode()), BOND_CURVE_CONFIG_NAME);
    return new ExposureFunctions(BOND_EXPOSURE_FUNCTIONS, exposureFunctions, idsToNames);
  }
  
  public static MarketDataSource createMarketDataSource() {
    return MapMarketDataSource.builder()
        .add(createId("B1"), 0.01)
        .add(createId("B2"), 0.015)
        .add(createId("B3"), 0.02)
        .add(createId("B4"), 0.025)
        .add(createId("B5"), 0.03)
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
    SimpleRegion region = new SimpleRegion();
    region.addExternalId(s_USID);
    when(mock.changeManager()).thenReturn(MOCK_CHANGE_MANAGER);
    when(mock.getHighestLevelRegion(any(ExternalId.class)))
        .thenReturn(region);
    return mock;
  }
  
  private static ConventionSource mockConventionSource() {
    return mock(ConventionSource.class);
  }
  
  private static ConventionBundleSource mockConventionBundleSource() {
    ConventionBundleSource mock = mock(ConventionBundleSource.class);
    
    String usBondConvention = "US_TREASURY_BOND_CONVENTION";
    ExternalId conventionId = ExternalId.of(InMemoryConventionBundleMaster.SIMPLE_NAME_SCHEME, usBondConvention);
    ConventionBundle convention =
        new ConventionBundleImpl(conventionId.toBundle(), usBondConvention, DayCounts.THIRTY_360,
                                 new ModifiedFollowingBusinessDayConvention(), Period.ofYears(1), 1, false,
                                 ExternalSchemes.financialRegionId("US"));
    when(mock.getConventionBundle(eq(conventionId))).thenReturn(convention);
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
    
    // curve def
    when(mock.get(eq(Object.class), eq(BOND_CURVE_NAME), any(VersionCorrection.class)))
      .thenReturn(ImmutableSet.of(ConfigItem.<Object>of(getBondCurveDefinition())));
    
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
    when(mock.getSingle(eq(BondMockSources.BOND_SECURITY.getExternalIdBundle())))
        .thenReturn(BondMockSources.BOND_SECURITY);
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

  private static BondSecurity createBondSecurity() {

    String issuerName = BondMockSources.BOND_ISSUER_KEY;
    String issuerDomicile = "US";
    String issuerType = "Sovereign";
    ZonedDateTime effectiveDate = DateUtils.getUTCDate(2014, 6, 18);
    ZonedDateTime maturityDate = DateUtils.getUTCDate(2015, 6, 18);
    Currency currency = Currency.USD;
    YieldConvention yieldConvention = SimpleYieldConvention.US_TREASURY_EQUIVALANT;
    Expiry lastTradeDate = new Expiry(maturityDate);
    String couponType = "Fixed";
    double couponRate = 0.02;
    Period couponPeriod = Period.parse("P6M");
    Frequency couponFrequency = PeriodFrequency.of(couponPeriod);
    DayCount dayCountConvention = DayCounts.ACT_ACT_ICMA;
    ZonedDateTime firstCouponDate = effectiveDate;
    ZonedDateTime interestAccrualDate = effectiveDate.minus(couponPeriod);
    ZonedDateTime settlementDate = maturityDate; // assume 0 day settlement lag
    Double issuancePrice = null;
    double totalAmountIssued = 100_000_000;
    double minimumAmount = 1;
    double minimumIncrement = 1;
    double parAmount = 100;
    double redemptionValue = 100;

    GovernmentBondSecurity bond =
        new GovernmentBondSecurity(issuerName, issuerType, issuerDomicile, issuerType, currency, yieldConvention,
                                   lastTradeDate, couponType, couponRate, couponFrequency, dayCountConvention,
                                   interestAccrualDate, settlementDate, firstCouponDate, issuancePrice,
                                   totalAmountIssued, minimumAmount, minimumIncrement, parAmount, redemptionValue);
    // Need this for time series lookup
    ExternalId bondId = ExternalSchemes.isinSecurityId("Test bond");
    bond.setExternalIdBundle(bondId.toBundle());
    return bond;
  }

  private static BondTrade createBondTrade() {
    Counterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "COUNTERPARTY"));
    BigDecimal tradeQuantity = BigDecimal.valueOf(1);
    LocalDate tradeDate = LocalDate.of(2014, 1, 1);
    OffsetTime tradeTime = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
    SimpleTrade trade = new SimpleTrade(BOND_SECURITY, tradeQuantity, counterparty, tradeDate, tradeTime);
    trade.setPremium(10.0);
    trade.setPremiumDate(tradeDate);
    trade.setPremiumCurrency(Currency.USD);
    return new BondTrade(trade);
  }

  private static BondFutureSecurity createBondFutureSecurity() {

    Currency currency = Currency.USD;

    ZonedDateTime deliveryDate = DateUtils.getUTCDate(2014, 6, 18);
    Expiry expiry = new Expiry(deliveryDate);
    String tradingExchange = "";
    String settlementExchange = "";
    double unitAmount = 1;
    Collection<BondFutureDeliverable> basket = new ArrayList<>();
    BondFutureDeliverable bondFutureDeliverable =
        new BondFutureDeliverable(BOND_SECURITY.getExternalIdBundle(), 0.9);
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
    trade.setPremiumCurrency(Currency.USD);
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
    trade.setPremiumCurrency(Currency.USD);
    return new BondFutureOptionTrade(trade);
  }

}
