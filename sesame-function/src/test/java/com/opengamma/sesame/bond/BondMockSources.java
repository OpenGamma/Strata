/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.testng.internal.annotations.Sets;
import org.threeten.bp.Period;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.holiday.impl.WeekendHolidaySource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.legalentity.LegalEntitySource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.region.impl.SimpleRegion;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.marketdata.spec.MarketData;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
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
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.VersionCorrection;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.LDClient;
import com.opengamma.sesame.marketdata.ResettableLiveMarketDataSource;
import com.opengamma.sesame.marketdata.StrategyAwareMarketDataSource;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.Tenor;

/**
 * Unit test helper to mock sources for bond pricing.
 */
public class BondMockSources {
  
  private static final ChangeManager MOCK_CHANGE_MANAGER = mock(ChangeManager.class);
  
  private static final String TICKER = "Ticker";
  
  private static final String BOND_CURVE_NODE_ID_MAPPER = "Test Bond Mapper";
  
  private static final String BOND_CURVE_NAME = "USD Test Bond Curve";
  
  private static final String BOND_CURVE_CONFIG_NAME = "Test Bond Curve Config";
  
  public static final String BOND_ISSUER_KEY = "US GOVERNMENT";
  
  public static final String BOND_EXPOSURE_FUNCTIONS = "Test Bond Exposure Functions";
  
  private static final ExternalId s_USID = ExternalSchemes.financialRegionId("US");
  
  private CurveNodeIdMapper getBondCurveNodeIdMapper() {
    Map<Tenor, CurveInstrumentProvider> bondNodes = Maps.newHashMap();
    bondNodes.put(Tenor.ONE_YEAR, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B1")));
    bondNodes.put(Tenor.TWO_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B2")));
    bondNodes.put(Tenor.THREE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B3")));
    bondNodes.put(Tenor.FOUR_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B4")));
    bondNodes.put(Tenor.FIVE_YEARS, new StaticCurveInstrumentProvider(ExternalId.of(TICKER, "B5")));
    return CurveNodeIdMapper.builder().name(BOND_CURVE_NODE_ID_MAPPER).periodicallyCompoundedRateNodeIds(bondNodes).build();
  }
  
  private InterpolatedCurveDefinition getBondCurveDefinition() {
    Set<CurveNode> nodes = new TreeSet<>();
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.ONE_YEAR, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.TWO_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.THREE_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.FOUR_YEARS, 1));
    nodes.add(new PeriodicallyCompoundedRateNode(BOND_CURVE_NODE_ID_MAPPER, Tenor.FIVE_YEARS, 1));
    return new InterpolatedCurveDefinition(BOND_CURVE_NAME, nodes, Interpolator1DFactory.LINEAR, Interpolator1DFactory.FLAT_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  }
  
  @SuppressWarnings("unchecked")
  private CurveConstructionConfiguration getBondCurveConfig() {
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
  
  private ExposureFunctions getExposureFunctions() {
    List<String> exposureFunctions = ImmutableList.of("Currency");
    Map<ExternalId, String> idsToNames = Maps.newHashMap();
    idsToNames.put(ExternalId.of("CurrencyISO", Currency.USD.getCode()), BOND_CURVE_CONFIG_NAME);
    return new ExposureFunctions(BOND_EXPOSURE_FUNCTIONS, exposureFunctions, idsToNames);
  }
  
  public StrategyAwareMarketDataSource createMarketDataSource() {
    Map<ExternalIdBundle, Double> marketData = Maps.newHashMap();
    marketData.put(ExternalId.of(TICKER, "B1").toBundle(), 0.01);
    marketData.put(ExternalId.of(TICKER, "B2").toBundle(), 0.015);
    marketData.put(ExternalId.of(TICKER, "B3").toBundle(), 0.02);
    marketData.put(ExternalId.of(TICKER, "B4").toBundle(), 0.025);
    marketData.put(ExternalId.of(TICKER, "B5").toBundle(), 0.03);
    FieldName fieldName = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);
    return new ResettableLiveMarketDataSource.Builder(MarketData.live(), mock(LDClient.class)).data(fieldName, marketData).build();
  }

  private ImmutableMap<Class<?>, Object> generateComponentMap(Object... components) {
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    for (Object component : components) {
      builder.put(component.getClass().getInterfaces()[0], component);
    }
    return builder.build();
  }

  private HolidaySource mockHolidaySource() {
    return new WeekendHolidaySource();
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
    return mock(ConventionSource.class);
  }
  
  private ConventionBundleSource mockConventionBundleSource() {
    ConventionBundleSource mock = mock(ConventionBundleSource.class);
    
    String usBondConvention = "US_TREASURY_BOND_CONVENTION";
    ExternalId conventionId = ExternalId.of(InMemoryConventionBundleMaster.SIMPLE_NAME_SCHEME, usBondConvention);
    ConventionBundle convention = new ConventionBundleImpl(conventionId.toBundle(), usBondConvention, DayCounts.THIRTY_360, new ModifiedFollowingBusinessDayConvention(), Period.ofYears(1), 1, false, ExternalSchemes.financialRegionId("US"));
    when(mock.getConventionBundle(eq(conventionId))).thenReturn(convention);
    return mock;
  }
  
  private LegalEntitySource mockLegalEntitySource() {
    return mock(LegalEntitySource.class);
  }
  
  private ConfigSource mockConfigSource() {
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
  
  private SecuritySource mockSecuritySource() {
    return mock(SecuritySource.class);
  }
  
  private HistoricalTimeSeriesSource mockHistoricalTimeSeriesSource() {
    return mock(HistoricalTimeSeriesSource.class);
  }

  public ImmutableMap<Class<?>, Object> generateBaseComponents() {
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
}
