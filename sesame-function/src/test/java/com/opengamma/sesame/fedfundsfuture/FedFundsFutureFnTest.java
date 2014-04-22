/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fedfundsfuture;

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
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.historicaltimeseries.impl.SimpleHistoricalTimeSeries;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.impl.SimpleCounterparty;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.security.future.FederalFundsFutureSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
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
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.trade.FedFundsFutureTrade;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Expiry;

/**
 * Tests the fed funds future analytics functions using the discounting calculator.
 */
public class FedFundsFutureFnTest {

  private static final InterestRateMockSources _interestRateMockSources = new InterestRateMockSources();

  private static final ZonedDateTime VALUATION_TIME = DateUtils.getUTCDate(2014, 1, 22);
  
  private static final Environment ENV =
      new SimpleEnvironment(VALUATION_TIME,
                            _interestRateMockSources.createMarketDataSource(LocalDate.of(2014, 2, 18)));
  
  private FedFundsFutureFn _fedFundsFutureFn;
  
  private FedFundsFutureTrade _fedFundsFutureTrade = createFedFundsFutureTrade();

  @BeforeClass
  public void setUpClass() throws IOException {
    FunctionModelConfig config = config(
                                        arguments(
                                                  function(ConfigDbMarketExposureSelectorFn.class,
                                                           argument("exposureConfig", ConfigLink.resolved(_interestRateMockSources.mockExposureFunctions()))),
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
                                        implementations(FedFundsFutureFn.class, DefaultFedFundsFutureFn.class,
                                                        FedFundsFutureCalculatorFactory.class, FedFundsFutureDiscountingCalculatorFactory.class,
                                                        CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                                                        FXMatrixFn.class, DefaultFXMatrixFn.class,
                                                        DiscountingMulticurveCombinerFn.class, ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                                                        CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                                                        DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                                                        CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                                                        CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class,
                                                        HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class,
                                                        MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                                                        MarketDataFn.class, DefaultMarketDataFn.class)
    );

    ImmutableMap<Class<?>, Object> components = generateComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    _fedFundsFutureFn = FunctionModel.build(FedFundsFutureFn.class, config, ComponentMap.of(components));
  }
  
  private ImmutableMap<Class<?>, Object> generateComponents() {
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    for (Map.Entry<Class<?>, Object> keys: _interestRateMockSources.generateBaseComponents().entrySet()) {
      if (!keys.getKey().equals(HistoricalTimeSeriesSource.class)) {
        builder.put(keys.getKey(), keys.getValue());
      }
    }
    builder.put(HistoricalTimeSeriesSource.class, mockHistoricalTimeSeriesSource());
    ImmutableMap<Class<?>, Object> components = builder.build();
    return components;
  }
  
  private HistoricalTimeSeriesSource mockHistoricalTimeSeriesSource() {
    HistoricalTimeSeriesSource mock = mock(HistoricalTimeSeriesSource.class);
    when(mock.changeManager()).thenReturn(mock(ChangeManager.class));
    
    ImmutableLocalDateDoubleTimeSeries of = ImmutableLocalDateDoubleTimeSeries.of(new LocalDate[] { VALUATION_TIME.toLocalDate().minusDays(1), VALUATION_TIME.toLocalDate() },
                                                                                  new double[] { 0.995, 0.995 });
    HistoricalTimeSeries fedFundsFuturePrices = new SimpleHistoricalTimeSeries(UniqueId.of("Blah", "1"), of);
    when(mock.getHistoricalTimeSeries(eq(MarketDataRequirementNames.MARKET_VALUE),
                                      any(ExternalIdBundle.class),
                                      eq("DEFAULT_TSS"),
                                      any(LocalDate.class),
                                      eq(true),
                                      any(LocalDate.class),
                                      eq(true))).thenReturn(fedFundsFuturePrices);
    return mock;
  }
  
  @Test
  public void testPresentValue() {
    Result<MultipleCurrencyAmount> pvComputed = _fedFundsFutureFn.calculatePV(ENV, _fedFundsFutureTrade);
    assertThat(pvComputed.isSuccess(), is(true));
  }
  
  private FedFundsFutureTrade createFedFundsFutureTrade() {
    
    Expiry expiry = new Expiry(ZonedDateTime.of(LocalDate.of(2014, 6, 18), LocalTime.of(0, 0), ZoneId.systemDefault()));
    String tradingExchange = "";
    String settlementExchange = "";
    Currency currency = Currency.USD;
    double unitAmount = 1000;
    ExternalId underlyingId = _interestRateMockSources.getOvernightIndexId();
    String category = "";
    FederalFundsFutureSecurity fedFundsFuture = new FederalFundsFutureSecurity(expiry, tradingExchange, settlementExchange, currency, unitAmount, underlyingId, category);
    // Need this for time series lookup
    fedFundsFuture.setExternalIdBundle(ExternalSchemes.syntheticSecurityId("Test future").toBundle());

    Counterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "COUNTERPARTY"));
    BigDecimal tradeQuantity = BigDecimal.valueOf(10);
    LocalDate tradeDate = LocalDate.of(2000, 1, 1);
    OffsetTime tradeTime = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
    SimpleTrade trade = new SimpleTrade(fedFundsFuture, tradeQuantity, counterparty, tradeDate, tradeTime);
    trade.setPremiumCurrency(Currency.USD);
    trade.setPremium(0.995);
    return new FedFundsFutureTrade(trade);
  }
}
