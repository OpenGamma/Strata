package com.opengamma.sesame.deliverableswapfuture;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetTime;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.historicaltimeseries.impl.SimpleHistoricalTimeSeries;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.impl.SimpleCounterparty;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.convention.frequency.PeriodFrequency;
import com.opengamma.financial.security.future.DeliverableSwapFutureSecurity;
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
import com.opengamma.id.UniqueId;
import com.opengamma.master.security.SecurityDocument;
import com.opengamma.master.security.SecurityMaster;
import com.opengamma.master.security.impl.MasterSecuritySource;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.CurveNodeConverterFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveNodeConverterFn;
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
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.interestrate.InterestRateMockSources;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.trade.DeliverableSwapFutureTrade;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Expiry;

/**
 * Tests the deliverable swap future analytic functions using the discounting calculator.
 */
@Test(groups = TestGroup.UNIT)
public class DeliverableSwapFutureFnTest {

  private static final ZonedDateTime VALUATION_TIME = DateUtils.getUTCDate(2014, 1, 22);
  
  private static final Environment ENV = new SimpleEnvironment(VALUATION_TIME, 
                                                              InterestRateMockSources.createMarketDataSource(
                                                                  LocalDate.of(2014, 2, 18)));

  private DeliverableSwapFutureFn _deliverableSwapFutureFn;
  
  private InterestRateSwapSecurity _underlyingSwap = createUnderlyingSwap();
  
  private DeliverableSwapFutureTrade _deliverableSwapFutureTrade = createDeliverableSwapFutureTrade();  
    
  @BeforeClass
  public void setUpClass() throws IOException {
    FunctionModelConfig config =
        config(
            arguments(
                function(ConfigDbMarketExposureSelectorFn.class,
                         argument("exposureConfig", 
                             ConfigLink.resolved(InterestRateMockSources.mockExposureFunctions()))),
                function(RootFinderConfiguration.class,
                         argument("rootFinderAbsoluteTolerance", 1e-9),
                         argument("rootFinderRelativeTolerance", 1e-9),
                         argument("rootFinderMaxIterations", 1000)),
                function(DefaultDiscountingMulticurveBundleFn.class,
                         argument("impliedCurveNames", StringSet.of())),
                function(DefaultHistoricalMarketDataFn.class,
                         argument("dataSource", "BLOOMBERG")),
                function(DefaultCurveNodeConverterFn.class,
                         argument("timeSeriesDuration", RetrievalPeriod.of(Period.ofYears(1)))),
                function(DefaultHistoricalTimeSeriesFn.class,
                         argument("resolutionKey", "DEFAULT_TSS"),
                         argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1))))),
            implementations(DeliverableSwapFutureFn.class, DefaultDeliverableSwapFutureFn.class,
                            DeliverableSwapFutureCalculatorFactory.class, 
                            DeliverableSwapFutureDiscountingCalculatorFactory.class,
                            CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                            CurveNodeConverterFn.class, DefaultCurveNodeConverterFn.class,
                            FXMatrixFn.class, DefaultFXMatrixFn.class,
                            DiscountingMulticurveCombinerFn.class, 
                            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                            CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                            DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                            CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                            CurveConstructionConfigurationSource.class, 
                            ConfigDBCurveConstructionConfigurationSource.class,
                            HistoricalMarketDataFn.class, DefaultHistoricalMarketDataFn.class,
                            HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class,
                            MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                            MarketDataFn.class, DefaultMarketDataFn.class));

    ImmutableMap<Class<?>, Object> components = generateComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    _deliverableSwapFutureFn = FunctionModel.build(DeliverableSwapFutureFn.class, config, ComponentMap.of(components));
  }
  
  
  private InterestRateSwapSecurity createUnderlyingSwap() {
    InterestRateSwapNotional notional = new InterestRateSwapNotional(Currency.USD, 1);
    Rate rate = new Rate(0.01);        
    BusinessDayConvention rollConv = BusinessDayConventions.MODIFIED_FOLLOWING;
    Set<ExternalId> calendarUSNY = Sets.newHashSet(ExternalId.of(ExternalSchemes.ISDA_HOLIDAY, "USNY"));
    
    FixedInterestRateSwapLeg fixedLeg = new FixedInterestRateSwapLeg();
    fixedLeg.setNotional(notional);
    fixedLeg.setRate(rate);
    fixedLeg.setAccrualPeriodBusinessDayConvention(rollConv);
    fixedLeg.setAccrualPeriodFrequency(PeriodFrequency.SEMI_ANNUAL);
    fixedLeg.setAccrualPeriodCalendars(calendarUSNY);
    fixedLeg.setPaymentDateBusinessDayConvention(rollConv);
    fixedLeg.setPaymentDateFrequency(PeriodFrequency.SEMI_ANNUAL);
    fixedLeg.setPaymentDateCalendars(calendarUSNY);        
    fixedLeg.setPayReceiveType(PayReceiveType.RECEIVE);
    fixedLeg.setDayCountConvention(DayCounts.THIRTY_360);

    FloatingInterestRateSwapLeg floatLeg = new FloatingInterestRateSwapLeg();
    floatLeg.setNotional(notional);
    floatLeg.setFloatingReferenceRateId(InterestRateMockSources.getLiborIndexId());
    floatLeg.setFloatingRateType(FloatingRateType.IBOR);
    floatLeg.setDayCountConvention(DayCounts.ACT_360);
    floatLeg.setResetPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING); 
    floatLeg.setResetPeriodCalendars(calendarUSNY);      
    floatLeg.setResetPeriodFrequency(PeriodFrequency.QUARTERLY);
    floatLeg.setAccrualPeriodBusinessDayConvention(rollConv);
    floatLeg.setAccrualPeriodFrequency(PeriodFrequency.QUARTERLY);
    floatLeg.setAccrualPeriodCalendars(calendarUSNY);
    floatLeg.setPaymentDateBusinessDayConvention(rollConv);
    floatLeg.setPaymentDateFrequency(PeriodFrequency.QUARTERLY);
    floatLeg.setPaymentDateCalendars(calendarUSNY);
    floatLeg.setFixingDateBusinessDayConvention(BusinessDayConventions.PRECEDING);
    floatLeg.setFixingDateCalendars(calendarUSNY);
    floatLeg.setFixingDateOffset(-2);
    PayReceiveType floatPayersOrRec = 
        fixedLeg.getPayReceiveType() == PayReceiveType.PAY ? PayReceiveType.RECEIVE : PayReceiveType.PAY;       
    floatLeg.setPayReceiveType(floatPayersOrRec);
    
    List<InterestRateSwapLeg> legs = new ArrayList<InterestRateSwapLeg>();
    legs.add(fixedLeg);
    legs.add(floatLeg);
    
    String swapId = Currency.USD + "_UnderlyingSwap";
    ExternalIdBundle externalSwapBundleId = ExternalSchemes.syntheticSecurityId(swapId).toBundle();
    
    InterestRateSwapSecurity irs = new InterestRateSwapSecurity(externalSwapBundleId, 
                                                                swapId, 
                                                                LocalDate.of(2014, 6, 18), 
                                                                LocalDate.of(2016, 6, 18), 
                                                                legs);        
                     
    return irs;
  }

  private DeliverableSwapFutureTrade createDeliverableSwapFutureTrade() {
        
    Expiry expiry = new Expiry(ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2014, 6, 15), 
                                                                 LocalTime.of(0, 0)), 
                                                                 ZoneId.systemDefault()));
    
    DeliverableSwapFutureSecurity dsf = new DeliverableSwapFutureSecurity(expiry, 
                                                                          Currency.USD + "DSF",
                                                                          Currency.USD + "DSF",
                                                                          Currency.USD, 
                                                                          1000,
                                                                          "DSF", 
                                                                          _underlyingSwap.getExternalIdBundle().
                                                                            getExternalIds().first(), 
                                                                          1);
    
    String dsfId = Currency.USD + "_TestDSF";
    ExternalIdBundle externalDsfBundle = ExternalSchemes.syntheticSecurityId(dsfId).toBundle();            
    dsf.setName(dsfId);
    dsf.setExternalIdBundle(externalDsfBundle);
    
    Counterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "COUNTERPARTY"));
    LocalDate tradeDate = LocalDate.of(2014, 6, 1).minusDays(2000);   // As per other product types
    OffsetTime tradeTime = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
    BigDecimal tradeQuantity = BigDecimal.valueOf(1);
    SimpleTrade trade = new SimpleTrade(dsf, tradeQuantity, counterparty , tradeDate, tradeTime);
    trade.setPremium(0.0);
                        
    return new DeliverableSwapFutureTrade(trade);
  }
  
  private ImmutableMap<Class<?>, Object> generateComponents() {
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    for (Map.Entry<Class<?>, Object> keys: InterestRateMockSources.generateBaseComponents().entrySet()) {
      if (!keys.getKey().equals(HistoricalTimeSeriesSource.class)) {
        builder.put(keys.getKey(), keys.getValue());
      }
      if (keys.getKey().equals(SecuritySource.class)) {
        appendSecuritySourceMock((SecuritySource) keys.getValue());
      }
    }
    builder.put(HistoricalTimeSeriesSource.class, mockHistoricalTimeSeriesSource());
    ImmutableMap<Class<?>, Object> components = builder.build();
    return components;
  }
  
  private HistoricalTimeSeriesSource mockHistoricalTimeSeriesSource() {
    HistoricalTimeSeriesSource mock = mock(HistoricalTimeSeriesSource.class);
    when(mock.changeManager()).thenReturn(mock(ChangeManager.class));
    
    HistoricalTimeSeries deliverableSwapFuturePrices = 
        new SimpleHistoricalTimeSeries(UniqueId.of("Blah", "1"), 
            ImmutableLocalDateDoubleTimeSeries.of(VALUATION_TIME.toLocalDate(), 0.975));
    
    when(mock.getHistoricalTimeSeries(eq(MarketDataRequirementNames.MARKET_VALUE),
                                      any(ExternalIdBundle.class),
                                      eq("DEFAULT_TSS"),
                                      any(LocalDate.class),
                                      eq(true),
                                      any(LocalDate.class),
                                      eq(true))).thenReturn(deliverableSwapFuturePrices);
    return mock;
  }
  
  private void appendSecuritySourceMock(SecuritySource mock) {
    SecurityMaster master = ((MasterSecuritySource) mock).getMaster();
    master.add(new SecurityDocument(_underlyingSwap));
  }
  
  @Test
  public void testPresentValue() {
    Result<Double> pvComputed = _deliverableSwapFutureFn.calculateSecurityModelPrice(ENV, _deliverableSwapFutureTrade);
    assertThat(pvComputed.isSuccess(), is(true));    
  }
  
  @Test
  public void testBucketedZeroDelta() {
    Result<BucketedCurveSensitivities> bucketedZeroDelta = 
        _deliverableSwapFutureFn.calculateBucketedZeroIRDelta(ENV, _deliverableSwapFutureTrade);    
    assertThat(bucketedZeroDelta.isSuccess(), is(true));
  }
}
