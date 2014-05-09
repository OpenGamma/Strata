/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.sesame;

import static com.opengamma.id.VersionCorrection.LATEST;
import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeMsg;
import org.fudgemsg.mapping.FudgeDeserializer;
import org.fudgemsg.wire.FudgeMsgReader;
import org.fudgemsg.wire.xml.FudgeXMLStreamReader;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import com.google.common.base.Throwables;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.holiday.impl.WeekendHolidaySource;
import com.opengamma.core.security.Security;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.curve.AbstractCurveDefinition;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveNodeIdMapper;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.DiscountingCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.InterpolatedCurveDefinition;
import com.opengamma.financial.analytics.curve.OvernightCurveTypeConfiguration;
import com.opengamma.financial.convention.FinancialConvention;
import com.opengamma.financial.convention.OISLegConvention;
import com.opengamma.financial.convention.OvernightIndexConvention;
import com.opengamma.financial.convention.SwapFixedLegConvention;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.VersionCorrection;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.JodaBeanSerialization;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Pair;

/**
 * Test for {@code InterpolatedMulticurveBundleFn}.
 */
@Test(groups = TestGroup.UNIT)
public class InterpolatedMulticurveBundleFnTest {

  private static Tenor[] s_tenors;

  static {
    s_tenors = new Tenor[] {Tenor.ONE_WEEK, Tenor.ONE_MONTH, Tenor.THREE_MONTHS, Tenor.SIX_MONTHS, Tenor.ONE_YEAR,
    Tenor.TWO_YEARS, Tenor.THREE_YEARS, Tenor.FIVE_YEARS, Tenor.TEN_YEARS};
  }

  private static final FieldName MARKET_VALUE = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);

  private final class TestVersionCorrectionProvider implements VersionCorrectionProvider {
    @Override
    public VersionCorrection getPortfolioVersionCorrection() {
      return LATEST;
    }

    @Override
    public VersionCorrection getConfigVersionCorrection() {
      return LATEST;
    }
  }

  private static final Class<?>[] MOCK_CLASSES = {
    ConfigSource.class,
    ConventionSource.class,
    SecuritySource.class,
    CurrencyMatrix.class
  };
  
  private static final RootFinderConfiguration ROOT_FINDER_CONFIG = new RootFinderConfiguration(1e-9, 1e-9, 1000);

  private DiscountingMulticurveBundleFn _multicurveBundleFn;

  private Environment _environment;

  private CurveConstructionConfiguration _usdDiscountingCCC;

  @BeforeClass
  public void init() throws IOException {
    //builds graph, initializing mocks
    
    ClassToInstanceMap<Object> mocks = MockUtils.strictMocks(MOCK_CLASSES);
    
    initConfigSource(mocks.getInstance(ConfigSource.class));
    initConventionSource(mocks.getInstance(ConventionSource.class));
    initSecuritySource(mocks.getInstance(SecuritySource.class));

    mocks.putInstance(RootFinderConfiguration.class, ROOT_FINDER_CONFIG);
    mocks.putInstance(HolidaySource.class, new WeekendHolidaySource());
    
    ComponentMap components = ComponentMap.of(mocks);
    
    FunctionModelConfig config = config(
        arguments(
            function(DefaultHistoricalTimeSeriesFn.class,
                     argument("resolutionKey", "DEFAULT_TSS_CONFIG"),
                     argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofDays(1)))),
            function(DefaultDiscountingMulticurveBundleFn.class,
                     argument("impliedCurveNames", StringSet.of()))),
         implementations(CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
             CurveSpecificationFn.class, DefaultCurveSpecificationFn.class, 
             CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
             HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class,
             FXMatrixFn.class, DefaultFXMatrixFn.class,
             DiscountingMulticurveBundleFn.class, InterpolatedMulticurveBundleFn.class,
             MarketDataFn.class, DefaultMarketDataFn.class)
           );

    _multicurveBundleFn = FunctionModel.build(DiscountingMulticurveBundleFn.class, config, components);
    
    ZonedDateTime valuationDate = ZonedDateTime.of(2014, 1, 10, 11, 0, 0, 0, ZoneId.of("America/Chicago"));
    MarketDataSource marketDataSource =
        MarketDataResourcesLoader.getPreloadedSource(
            "/regression/curve_testing/usdMarketQuotes.discountFactors.properties",
            "Ticker");

    _environment = new SimpleEnvironment(valuationDate, marketDataSource);
    
    VersionCorrectionProvider vcProvider = new TestVersionCorrectionProvider();
    
    ServiceContext serviceContext = ServiceContext.of(mocks).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);
    
    MockUtils.enableStrict();
    
    _usdDiscountingCCC = createUSDCurveConstructionConfig();
  }

  /**
   * Check that curve built from discount factor nodes has expected discount factors.
   */
  @Test
  public void testUSD() {
    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundle = _multicurveBundleFn.generateBundle(_environment, _usdDiscountingCCC);
    assertTrue("Curve bundle result failed", bundle.isSuccess());
    Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> value = bundle.getValue();
    MulticurveProviderDiscount multicurve = value.getFirst();
    double expectedDF = 1.0;
    for (Tenor tenor : s_tenors) {
      double time = TimeCalculator.getTimeBetween(_environment.getValuationTime(), _environment.getValuationTime().plus(tenor.getPeriod()));
      expectedDF = (Double) _environment.getMarketDataSource().get(ExternalIdBundle.of("Ticker", tenor.toFormattedString().substring(1)), MARKET_VALUE).getValue();
      double actualDF = multicurve.getDiscountFactor(Currency.USD, time);
      // simple check that market data values are being returned as discount factors
      assertEquals("USD DF for tenor " + tenor + " (" + tenor.toFormattedString() + ")  mismatch", expectedDF, actualDF, 10E-6);
    }
    // check we have flat extrapolation
    double actualDF = multicurve.getDiscountFactor(Currency.USD, 10000d);
    assertEquals("Right extrapolation not flat", expectedDF, actualDF, 10E-6);
  }

  private CurveConstructionConfiguration createUSDCurveConstructionConfig() {
    List<CurveTypeConfiguration> ctc = Lists.newArrayList(new DiscountingCurveTypeConfiguration("USD"), new OvernightCurveTypeConfiguration(ExternalId.of("BLOOMBERG_TICKER", "FEDL01 Index")));
    Map<String, List<? extends CurveTypeConfiguration>> ct = Maps.newHashMap();
    ct.put("USD DiscountingDiscountFactorNodes", ctc);
    List<CurveGroupConfiguration> cgc = Lists.newArrayList(new CurveGroupConfiguration(0, ct ));
    CurveConstructionConfiguration ccc = new CurveConstructionConfiguration("USD", cgc, Collections.<String>emptyList());
    return ccc;
  }
  
  private void initConfigSource(ConfigSource cs) {
    InterpolatedCurveDefinition usdDiscountingCurve = loadConfig(InterpolatedCurveDefinition.class, "USD_DiscountingDiscountFactorNodes.xml", false);
    when(cs.get(Object.class, "USD DiscountingDiscountFactorNodes", LATEST)).thenReturn(Collections.singletonList(ConfigItem.<Object> of(usdDiscountingCurve)));
    when(cs.get(AbstractCurveDefinition.class, "USD DiscountingDiscountFactorNodes", LATEST)).thenReturn(Collections.singletonList(ConfigItem.<AbstractCurveDefinition> of(usdDiscountingCurve)));
    when(cs.getSingle(InterpolatedCurveDefinition.class, "USD DiscountingDiscountFactorNodes", LATEST)).thenReturn(usdDiscountingCurve);
    
    CurveNodeIdMapper nodeIdMapper = loadConfig(CurveNodeIdMapper.class, "USD_OIS_NodeMapper.xml", true);
    when(cs.getSingle(CurveNodeIdMapper.class, "USD OIS Node Mapper", LATEST)).thenReturn(nodeIdMapper);
  }
  
  private void initConventionSource(ConventionSource cs) {
    SwapFixedLegConvention fixedLegConvention = loadConvention(SwapFixedLegConvention.class, "USD_OIS_Fixed_Leg.xml", false);
    when(cs.getSingle(ExternalId.of("CONVENTION", "USD OIS Fixed Leg"), FinancialConvention.class)).thenReturn(fixedLegConvention);
    
    FinancialConvention oisOvernight = loadConvention(OISLegConvention.class, "USD_OIS_Overnight_Leg.xml", false);
    when(cs.getSingle(ExternalId.of("CONVENTION", "USD OIS Overnight Leg"), FinancialConvention.class)).thenReturn(oisOvernight);
    
    OvernightIndexConvention ff = loadConvention(OvernightIndexConvention.class, "FedFundsEffectiveRateU.xml", false);
    when(cs.getSingle(ExternalId.of("BLOOMBERG_CONVENTION_NAME", "Federal Funds Effective Rate U"), FinancialConvention.class)).thenReturn(ff);
    when(cs.getSingle(ExternalId.of("BLOOMBERG_CONVENTION_NAME", "Federal Funds Effective Rate U"), OvernightIndexConvention.class)).thenReturn(ff);
    when(cs.getSingle(ExternalId.of("BLOOMBERG_CONVENTION_NAME", "Federal Funds Effective Rate U").toBundle(), LATEST)).thenReturn(ff);
  }
  
  private void initSecuritySource(SecuritySource ss) {
    
    Security sec = loadSecurity(Security.class, "FEDL01 Index.xml", false);
    when(ss.getSingle(ExternalId.of("BLOOMBERG_TICKER", "FEDL01 Index").toBundle())).thenReturn(sec);
    when(ss.getSingle(ExternalId.of("BLOOMBERG_TICKER", "FEDL01 Index").toBundle(), LATEST)).thenReturn(sec);
  }
  
  
  private Security loadSecurity(Class<Security> clazz, String path, boolean isFudge) {
    return loadBean(clazz, "security/" + path, isFudge);
  }


  private <T> T loadConfig(Class<T> clazz, String path, boolean isFudge) {
    return loadBean(clazz, "config/" + path, isFudge);
  }
  
  private <T> T loadConvention(Class<T> clazz, String path, boolean isFudge) {
    return loadBean(clazz, "convention/" + path, isFudge);
  }
  
  private <T> T loadBean(Class<T> clazz, String path, boolean isFudge) {
    try {
      File file = new ClassPathResource("regression/curve_testing/" + path).getFile();
      if (!isFudge) {
        return JodaBeanSerialization.deserializer().xmlReader().read(new FileInputStream(file), clazz);
      } else {
        FudgeContext context = OpenGammaFudgeContext.getInstance();
        FudgeXMLStreamReader streamReader = new FudgeXMLStreamReader(context, new FileReader(file));
        // Don't close fudgeMsgReader; the caller will close the stream later
        @SuppressWarnings("resource")
        FudgeMsgReader fudgeMsgReader = new FudgeMsgReader(streamReader);
        FudgeMsg msg = fudgeMsgReader.nextMessage();
        return new FudgeDeserializer(context).fudgeMsgToObject(clazz, msg);
      }
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }

}
