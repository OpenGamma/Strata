package com.opengamma.sesame.credit;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import com.google.common.base.Throwables;
import com.google.common.collect.ClassToInstanceMap;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.holiday.impl.WeekendHolidaySource;
import com.opengamma.core.region.Region;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.curve.AbstractCurveDefinition;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.curve.IsdaYieldCurveDefinition;
import com.opengamma.financial.convention.IsdaYieldCurveConvention;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.VersionCorrection;
import com.opengamma.master.holiday.HolidayMaster;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.MarketDataResourcesLoader;
import com.opengamma.sesame.MockUtils;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.MapMarketDataSource;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.JodaBeanSerialization;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * Tests the building of ISDA compliant yield curves.
 */
@Test(groups = TestGroup.UNIT)
public class DefaultIsdaCompliantYieldCurveFnTest {
  
  private final VersionCorrection _testVersionCorrection = VersionCorrection.of(Instant.now(), Instant.now());
  
  private final class TestVersionCorrectionProvider implements VersionCorrectionProvider {
    @Override
    public VersionCorrection getPortfolioVersionCorrection() {
      return _testVersionCorrection;
    }

    @Override
    public VersionCorrection getConfigVersionCorrection() {
      return _testVersionCorrection;
    }
  }

  private static final Class<?>[] MOCK_CLASSES = {
    ConfigSource.class,
    HistoricalTimeSeriesSource.class,
    ConventionSource.class,
    HistoricalMarketDataFn.class,
    SecuritySource.class,
    HolidayMaster.class,
    RegionSource.class,
    CurrencyMatrix.class
  };
  
  private static final RootFinderConfiguration ROOT_FINDER_CONFIG = new RootFinderConfiguration(1e-9, 1e-9, 1000);

  private IsdaCompliantYieldCurveFn _ycBuilder;

  private Environment _environment;

  private IsdaYieldCurveDefinition _usdISDAYC;
  
  @BeforeClass
  public void init() throws IOException {
    //builds graph, initializing mocks
    
    ClassToInstanceMap<Object> mocks = MockUtils.mocks(MOCK_CLASSES);
    
    initConfigSource(mocks.getInstance(ConfigSource.class));
    initConventionSource(mocks.getInstance(ConventionSource.class));
    initRegionSource(mocks.getInstance(RegionSource.class));
    
    mocks.putInstance(RootFinderConfiguration.class, ROOT_FINDER_CONFIG);
    mocks.putInstance(HolidaySource.class, new WeekendHolidaySource());
    
    ComponentMap components = ComponentMap.of(mocks);
    
    FunctionModelConfig config = config(
        arguments(
            function(DefaultHistoricalTimeSeriesFn.class,
                     argument("resolutionKey", "DEFAULT_TSS_CONFIG"),
                     argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofDays(1))))),
         implementations(CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
             CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
             MarketDataFn.class, DefaultMarketDataFn.class)
           );

    _ycBuilder = FunctionModel.build(DefaultIsdaCompliantYieldCurveFn.class, config, components);
    
    ZonedDateTime valuationDate = ZonedDateTime.of(2014, 1, 10, 11, 0, 0, 0, ZoneId.of("America/Chicago"));
    Map<ExternalIdBundle, Double> marketData = 
          MarketDataResourcesLoader.getData("/regression/isda_curve_testing/usdMarketQuotes.properties", "Ticker");
    
    MarketDataSource marketDataSource = MapMarketDataSource.builder()
                                                           .addAll(marketData)
                                                           .build();
    
    _environment = new SimpleEnvironment(valuationDate, marketDataSource);
    
    VersionCorrectionProvider vcProvider = new TestVersionCorrectionProvider();
    
    ServiceContext serviceContext = ServiceContext.of(mocks).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);
    
    _usdISDAYC = createISDAYieldCurveConstructionConfig();
    
  }
  
  private void initRegionSource(RegionSource mock) {
    Region usRegion = MockUtils.strictMock(Region.class);
    when(usRegion.getCurrency()).thenReturn(Currency.USD);
    when(mock.getHighestLevelRegion(ExternalId.of("FINANCIAL_REGION", "US"))).thenReturn(usRegion);
  }

  @Test
  public void testBuildISDACompliantCurve() {
    //TODO - add a numbers check here
    Result<ISDACompliantYieldCurve> isdaCompliantCurve = _ycBuilder.buildISDACompliantCurve(_environment, _usdISDAYC);
    
    if (!isdaCompliantCurve.isSuccess()) {
      fail("Curve bundle result failed with " + isdaCompliantCurve.getFailureMessage());
    }
    
    ISDACompliantYieldCurve value = isdaCompliantCurve.getValue();
    
    System.out.println(value);
  }
  
  @Test
  public void testMarketDataLookupFailure() {
    CurveSpecificationMarketDataFn specMDFn = mock(CurveSpecificationMarketDataFn.class);
    Environment anyEnv = any();
    CurveSpecification anySpec = any();
    when(specMDFn.requestData(anyEnv, anySpec))
      .thenReturn(Result.<Map<ExternalIdBundle,Double>> failure(FailureStatus.ERROR, "Failed miserably!"));
    
    IsdaCompliantYieldCurveFn fn = new DefaultIsdaCompliantYieldCurveFn(specMDFn, 
                                                                        mock(RegionSource.class), 
                                                                        mock(HolidaySource.class));
    
    Result<ISDACompliantYieldCurve> result = fn.buildISDACompliantCurve(this._environment, _usdISDAYC);
    
    assertFalse("Expected failure", result.isSuccess());
    
  }

  private IsdaYieldCurveDefinition createISDAYieldCurveConstructionConfig() {
    String curveConfigPath = "isda_ycdef_usd.xml";
    return loadConfig(IsdaYieldCurveDefinition.class, curveConfigPath);
  }
  
  private void initConfigSource(ConfigSource cs) {
    IsdaYieldCurveDefinition def = createISDAYieldCurveConstructionConfig();
    when(cs.get(Object.class, "ISDA USD YC", _testVersionCorrection))
        .thenReturn(Collections.singleton(ConfigItem.<Object>of(def)));
    when(cs.get(AbstractCurveDefinition.class, "ISDA USD YC", _testVersionCorrection))
        .thenReturn(Collections.singleton(ConfigItem.<AbstractCurveDefinition>of(def)));
    
  }
  
  private void initConventionSource(ConventionSource cs) {
    IsdaYieldCurveConvention ycConvention = loadConvention(IsdaYieldCurveConvention.class, "isda_ycconv_usd.xml");
    when(cs.getSingle(ExternalIdBundle.of("CONVENTION", "ISDA YC USD"), _testVersionCorrection))
        .thenReturn(ycConvention);
    
  }
  
  private <T> T loadConfig(Class<T> clazz, String path) {
    return loadBean(clazz, "config/" + path);
  }
  
  private <T> T loadConvention(Class<T> clazz, String path) {
    return loadBean(clazz, "convention/" + path);
  }
  
  private <T> T loadBean(Class<T> clazz, String path) {
    try {
      File file = new ClassPathResource("regression/isda_curve_testing/" + path).getFile();
        return JodaBeanSerialization.deserializer().xmlReader().read(new FileInputStream(file), clazz);
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }

  }

}
