package com.opengamma.sesame.marketdata;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetTime;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.historicaltimeseries.impl.SimpleHistoricalTimeSeries;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.impl.SimpleCounterparty;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.security.future.FederalFundsFutureSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.CurrencyPairsFn;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
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
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.HistoricalTimeSeriesFn;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.fedfundsfuture.DefaultFedFundsFutureFn;
import com.opengamma.sesame.fedfundsfuture.FedFundsFutureCalculatorFactory;
import com.opengamma.sesame.fedfundsfuture.FedFundsFutureDiscountingCalculatorFactory;
import com.opengamma.sesame.fedfundsfuture.FedFundsFutureFn;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.interestrate.InterestRateMockSources;
import com.opengamma.sesame.trade.FedFundsFutureTrade;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Expiry;
import com.opengamma.util.tuple.Pair;

@SuppressWarnings("deprecation")
@Test(groups = TestGroup.UNIT)
public class FedFundFutureCurveTest {

  private static final ZonedDateTime VALUATION_TIME = DateUtils.getUTCDate(2014, 4, 17);
  private static final String CURVE_CONSTRUCTION_CONFIGURATION_USD_FFF = "USD_ON-FFF";

  private static final double TOLERANCE_PV = 1.0E-4;

  private static final double EXPECTED_PV = 0.0000;
  private static final int NB_TRADE = 3;
  private static final double[] EXPECTED_PRICE = new double[NB_TRADE];// Price used for curve calibration
  private static final LocalDate[] EXPIRY_DATE = new LocalDate[NB_TRADE];
  static {
    EXPECTED_PRICE[0] =  0.9990; // Price used for curve calibration FFJ4
    EXPECTED_PRICE[1] =  0.999075; // Price used for curve calibration FFK4
    EXPECTED_PRICE[2] =  0.999025; // Price used for curve calibration FFM4
    EXPIRY_DATE[0] = LocalDate.of(2014, 4, 30);
    EXPIRY_DATE[1] = LocalDate.of(2014, 5, 30);
    EXPIRY_DATE[2] = LocalDate.of(2014, 6, 30);
  }
  

  private static final String TRADING_EX = "CME";
  private static final String SETTLE_EX = "CME";
  private static final Currency CCY = Currency.USD;
  private static final double UNIT_AMOUNT = 5000000.0d/12.0d;
  private static final ExternalId FED_FUND_INDEX_ID = InterestRateMockSources.getOvernightIndexId();
  private static final String CATEGORY = "Category";
  private static final int NB_CONTRACTS = 20 ; // 100 m
  private static final BigDecimal TRADE_QUANTITY = BigDecimal.valueOf(NB_CONTRACTS);
  private static final Counterparty COUNTERPARTY = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "COUNTERPARTY"));
  private static final LocalDate TRADE_DATE = VALUATION_TIME.toLocalDate();
  private static final OffsetTime TRADE_TIME = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
  
  private InterestRateMockSources _interestRateMockSources = new InterestRateMockSources();

  private DefaultDiscountingMulticurveBundleFn _curveBundle;
  
  private FedFundsFutureFn _fedFundsFutureFn;
  
  @BeforeClass
  public void setUpClass() throws IOException {
    FunctionModelConfig config = config(
      arguments( 
          function(ConfigDbMarketExposureSelectorFn.class,
              argument("exposureConfig", ConfigLink.resolved(_interestRateMockSources.mockFFExposureFunctions()))),
              function(DefaultDiscountingMulticurveBundleFn.class, argument("impliedCurveNames", StringSet.of())),
        function(RootFinderConfiguration.class, argument("rootFinderAbsoluteTolerance", 1e-9),
            argument("rootFinderRelativeTolerance", 1e-9), argument("rootFinderMaxIterations", 1000)),
        function(DefaultHistoricalTimeSeriesFn.class, argument("resolutionKey", "DEFAULT_TSS"), argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1))))),
      implementations(FedFundsFutureFn.class, DefaultFedFundsFutureFn.class,
          FedFundsFutureCalculatorFactory.class, FedFundsFutureDiscountingCalculatorFactory.class,
          CurrencyPairsFn.class, DefaultCurrencyPairsFn.class,
                        CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                        FXMatrixFn.class, DefaultFXMatrixFn.class,
                        DiscountingMulticurveCombinerFn.class, ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                        CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                        DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                        CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                        HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class,
                        MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                        MarketDataFn.class, DefaultMarketDataFn.class));

    ImmutableMap<Class<?>, Object> components = generateComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    _curveBundle = FunctionModel.build(DefaultDiscountingMulticurveBundleFn.class, config, ComponentMap.of(components));

    _fedFundsFutureFn = FunctionModel.build(FedFundsFutureFn.class, config, ComponentMap.of(components));
  }
  
  /**
   * Build the curve with Fed Fund futures (including the current one).
   * Re-price futures trade with trade date the calibration date and trade price the calibration price and compare to 0.
   */
  @Test
  public void buildCurve() {
    MarketDataSource dataSource = _interestRateMockSources.createMarketDataSource(VALUATION_TIME.toLocalDate(), false);
    Environment env = new SimpleEnvironment(VALUATION_TIME, dataSource);
    Result<Pair<MulticurveProviderDiscount,CurveBuildingBlockBundle>> pairProviderBlock = 
        _curveBundle.generateBundle(env, ConfigLink.resolvable(CURVE_CONSTRUCTION_CONFIGURATION_USD_FFF , 
            CurveConstructionConfiguration.class).resolve());
    if (!pairProviderBlock.isSuccess()) {
      fail(pairProviderBlock.getFailureMessage());
    }
    // Re-pricing FF futures trades
    FedFundsFutureTrade[] ffTrades = new FedFundsFutureTrade[NB_TRADE];
    for(int i = 0; i < NB_TRADE; i++) {
      ffTrades[i] = createFFTrade(EXPIRY_DATE[i], EXPECTED_PRICE[i]);
    }
    for(int i = 0; i < NB_TRADE; i++) {
      Result<MultipleCurrencyAmount> resultPVJ4 = _fedFundsFutureFn.calculatePV(env, ffTrades[i]);
      if (resultPVJ4.isSuccess()) {
        MultipleCurrencyAmount mca = resultPVJ4.getValue();
        assertThat("FedFundFutureCurve: node " + i, mca.getCurrencyAmount(Currency.USD).getAmount(), is(closeTo(EXPECTED_PV, TOLERANCE_PV)));
      } else {
        fail(resultPVJ4.getFailureMessage());
      }      
    }
  } 
  
  private FedFundsFutureTrade createFFTrade(LocalDate expiryDate, double tradePrice) {    
    Expiry expiry = new Expiry(ZonedDateTime.of(expiryDate, LocalTime.of(0, 0), ZoneId.systemDefault()));
    FederalFundsFutureSecurity fedFundsFuture = 
        new FederalFundsFutureSecurity(expiry, TRADING_EX, SETTLE_EX, CCY, UNIT_AMOUNT, FED_FUND_INDEX_ID, CATEGORY);
    fedFundsFuture.setExternalIdBundle(ExternalSchemes.syntheticSecurityId("Test future").toBundle());
    SimpleTrade trade = new SimpleTrade(fedFundsFuture, TRADE_QUANTITY, COUNTERPARTY, TRADE_DATE, TRADE_TIME);
    trade.setPremiumCurrency(Currency.USD);
    trade.setPremium(tradePrice);
    return new FedFundsFutureTrade(trade);
  }
  
  private ImmutableMap<Class<?>, Object> generateComponents() {
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    for (Map.Entry<Class<?>, Object> keys: _interestRateMockSources.generateBaseComponents().entrySet()) {
      if (keys.getKey().equals(HistoricalTimeSeriesSource.class)) {
        appendHistoricalTimeSeriesSource((HistoricalTimeSeriesSource) keys.getValue());
      }
      builder.put(keys.getKey(), keys.getValue());
    }
    return builder.build();
  }
  
  private void appendHistoricalTimeSeriesSource(HistoricalTimeSeriesSource mock) {
    HistoricalTimeSeries irFuturePrices = new SimpleHistoricalTimeSeries(UniqueId.of("Blah", "1"), ImmutableLocalDateDoubleTimeSeries.of(VALUATION_TIME.toLocalDate(), 0.975));
    when(mock.getHistoricalTimeSeries(eq(MarketDataRequirementNames.MARKET_VALUE),
                                      eq(ExternalSchemes.syntheticSecurityId("Test future").toBundle()),
                                      eq("DEFAULT_TSS"),
                                      any(LocalDate.class),
                                      eq(true),
                                      any(LocalDate.class),
                                      eq(true))).thenReturn(irFuturePrices);
  }
  
}

  