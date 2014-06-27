/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.exposure;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;

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
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.impl.SimpleCounterparty;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.id.ExternalId;
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
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.FXMatrixFn;
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
import com.opengamma.sesame.marketdata.StrategyAwareMarketDataSource;
import com.opengamma.sesame.trade.InterestRateFutureTrade;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Expiry;
import com.opengamma.util.tuple.Pair;

/**
 * Tests of ExposureFunctions at the view level
 */
@Test(groups = TestGroup.INTEGRATION)
public class ExposureFunctionTest {

  private static FXMatrix _emptyFxMatrix = new FXMatrix();
  private static SimpleEnvironment _environment;
  private static ImmutableMap<Class<?>, Object> _components = InterestRateMockSources.generateBaseComponents();

  @BeforeClass
  private void setUpClass() {

    ZonedDateTime valTime = LocalDate.of(2014, 6, 1).atStartOfDay(ZoneOffset.UTC);
    StrategyAwareMarketDataSource marketDataSource = InterestRateMockSources.createMarketDataFactory().create(
            new FixedHistoricalMarketDataSpecification(valTime.toLocalDate()));

     _environment = new SimpleEnvironment(valTime, marketDataSource);

    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(_components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);
  }

  private FunctionModelConfig createFunctionModelConfig(ExposureFunctions exposureFunctions) {

    FunctionModelConfig config =
        config(
            arguments(
                function(
                    ConfigDbMarketExposureSelectorFn.class,
                    argument("exposureConfig", ConfigLink.resolved(exposureFunctions))),
                function(
                    RootFinderConfiguration.class,
                    argument("rootFinderAbsoluteTolerance", 1e-9),
                    argument("rootFinderRelativeTolerance", 1e-9),
                    argument("rootFinderMaxIterations", 1000)),
                function(
                    DefaultCurveNodeConverterFn.class,
                    argument("timeSeriesDuration", RetrievalPeriod.of(Period.ofYears(1)))),
                function(
                    DefaultHistoricalMarketDataFn.class,
                    argument("dataSource", "BLOOMBERG")),
                function(
                    DefaultHistoricalTimeSeriesFn.class,
                    argument("resolutionKey", "DEFAULT_TSS"),
                    argument("htsRetrievalPeriod", RetrievalPeriod.of((Period.ofYears(1))))),
                function(
                    DefaultDiscountingMulticurveBundleFn.class,
                    argument("impliedCurveNames", StringSet.of()))
            ),
            implementations(
                DiscountingMulticurveCombinerFn.class, ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                FXMatrixFn.class, DefaultFXMatrixFn.class,
                CurveNodeConverterFn.class, DefaultCurveNodeConverterFn.class,
                MarketDataFn.class, DefaultMarketDataFn.class,
                FXMatrixFn.class, DefaultFXMatrixFn.class,
                HistoricalMarketDataFn.class, DefaultHistoricalMarketDataFn.class
            )
        );
    return config;
  }

  @Test
  public void tradeAttributeExposureViaTradePass() {

    FunctionModelConfig config = createFunctionModelConfig(InterestRateMockSources.mockTradeAttributeExposureFunctions());

    ExposureFunctionsDiscountingMulticurveCombinerFn multicurveCombinerFunction =
        FunctionModel.build(
            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
            config,
            ComponentMap.of(_components));

    InterestRateFutureTrade trade = createInterestRateFutureTrade();
    trade.addAttribute("TEST", "PASS");

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> result =
        multicurveCombinerFunction.createMergedMulticurveBundle(_environment, trade, _emptyFxMatrix);

    assertThat(result.isSuccess(), is((true)));

    Pair pair = (Pair) result.getValue();
    MulticurveProviderDiscount multicurveProviderDiscount = (MulticurveProviderDiscount) pair.getFirst();

    assertThat(multicurveProviderDiscount.getAllCurveNames(),
               containsInAnyOrder(
                   InterestRateMockSources.USD_LIBOR3M_CURVE_NAME,
                   InterestRateMockSources.USD_OIS_CURVE_NAME));
  }

  @Test
  public void tradeAttributeExposureViaTradeFail() {

    FunctionModelConfig config = createFunctionModelConfig(InterestRateMockSources.mockTradeAttributeExposureFunctions());

    ExposureFunctionsDiscountingMulticurveCombinerFn multicurveCombinerFunction =
        FunctionModel.build(
            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
            config,
            ComponentMap.of(_components));

    InterestRateFutureTrade trade = createInterestRateFutureTrade();
    trade.addAttribute("TEST", "FAIL");

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> result =
        multicurveCombinerFunction.createMergedMulticurveBundle(_environment, trade, _emptyFxMatrix);

    //There are no curves for this trade defined in the exposure function, so the result should fail
    assertThat(result.isSuccess(), is((false)));
  }


  @Test
  public void counterpartyExposureViaTradePass() {

    FunctionModelConfig config = createFunctionModelConfig(InterestRateMockSources.mockCounterpartyExposureFunctions());

    ExposureFunctionsDiscountingMulticurveCombinerFn multicurveCombinerFunction =
        FunctionModel.build(
            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
            config,
            ComponentMap.of(_components));

    InterestRateFutureSecurity security = createInterestRateFutureSecurity(Currency.USD);
    SimpleCounterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "PASS"));
    InterestRateFutureTrade trade = createInterestRateFutureTrade(security, counterparty);

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> result =
        multicurveCombinerFunction.createMergedMulticurveBundle(_environment, trade, _emptyFxMatrix);

    assertThat(result.isSuccess(), is((true)));

    Pair pair = (Pair) result.getValue();
    MulticurveProviderDiscount multicurveProviderDiscount = (MulticurveProviderDiscount) pair.getFirst();

    assertThat(multicurveProviderDiscount.getAllCurveNames(),
               containsInAnyOrder(
                   InterestRateMockSources.USD_LIBOR3M_CURVE_NAME,
                   InterestRateMockSources.USD_OIS_CURVE_NAME));
  }

  @Test
  public void counterpartyExposureViaTradeFail() {

    FunctionModelConfig config = createFunctionModelConfig(InterestRateMockSources.mockCounterpartyExposureFunctions());

    ExposureFunctionsDiscountingMulticurveCombinerFn multicurveCombinerFunction =
        FunctionModel.build(
            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
            config,
            ComponentMap.of(_components));

    InterestRateFutureSecurity security = createInterestRateFutureSecurity(Currency.USD);
    SimpleCounterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "FAIL"));
    InterestRateFutureTrade trade = createInterestRateFutureTrade(security, counterparty);

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> result =
        multicurveCombinerFunction.createMergedMulticurveBundle(_environment, trade, _emptyFxMatrix);

    //There are no curves for this counterparty defined in the exposure function, so the result should fail
    assertThat(result.isSuccess(), is((false)));
  }

  @Test
  public void currencyUsdExposureViaSecurityPass() {

    FunctionModelConfig config = createFunctionModelConfig(InterestRateMockSources.mockCurrencyExposureFunctions());

    ExposureFunctionsDiscountingMulticurveCombinerFn multicurveCombinerFunction =
        FunctionModel.build(
            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
            config,
            ComponentMap.of(_components));

    FRASecurity security = createSingleFra(Currency.USD);

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> result =
        multicurveCombinerFunction.createMergedMulticurveBundle(_environment, security, _emptyFxMatrix);

    assertThat(result.isSuccess(), is((true)));

    Pair pair = (Pair) result.getValue();
    MulticurveProviderDiscount multicurveProviderDiscount = (MulticurveProviderDiscount) pair.getFirst();

    assertThat(multicurveProviderDiscount.getAllCurveNames(),
               containsInAnyOrder(
                   InterestRateMockSources.USD_LIBOR3M_CURVE_NAME,
                   InterestRateMockSources.USD_OIS_CURVE_NAME));
  }

  @Test
  public void currencyUsdExposureViaTradePass() {

    FunctionModelConfig config = createFunctionModelConfig(InterestRateMockSources.mockCurrencyExposureFunctions());

    ExposureFunctionsDiscountingMulticurveCombinerFn multicurveCombinerFunction =
        FunctionModel.build(
            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
            config,
            ComponentMap.of(_components));

    InterestRateFutureSecurity security = createInterestRateFutureSecurity(Currency.USD);
    InterestRateFutureTrade trade = createInterestRateFutureTrade(security);

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> result =
        multicurveCombinerFunction.createMergedMulticurveBundle(_environment, trade, _emptyFxMatrix);
    assertThat(result.isSuccess(), is((true)));

    Pair pair = (Pair) result.getValue();
    MulticurveProviderDiscount multicurveProviderDiscount = (MulticurveProviderDiscount) pair.getFirst();

    assertThat(multicurveProviderDiscount.getAllCurveNames(),
               containsInAnyOrder(
                   InterestRateMockSources.USD_LIBOR3M_CURVE_NAME,
                   InterestRateMockSources.USD_OIS_CURVE_NAME));
  }

  @Test
  public void currencyGbpExposureViaTradeFail() {

    FunctionModelConfig config = createFunctionModelConfig(InterestRateMockSources.mockCurrencyExposureFunctions());

    ExposureFunctionsDiscountingMulticurveCombinerFn multicurveCombinerFunction =
        FunctionModel.build(
            ExposureFunctionsDiscountingMulticurveCombinerFn.class,
            config,
            ComponentMap.of(_components));

    InterestRateFutureSecurity security = createInterestRateFutureSecurity(Currency.GBP);
    InterestRateFutureTrade trade = createInterestRateFutureTrade(security);

    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> result =
        multicurveCombinerFunction.createMergedMulticurveBundle(_environment, trade, _emptyFxMatrix);

    //There are no GBP curves in the defined exposure function, so the result should fail
    assertThat(result.isSuccess(), is((false)));

  }

  //Only used to test the deprecated security lookup
  private FRASecurity createSingleFra(Currency currency) {
    return new FRASecurity(currency,
                           ExternalSchemes.financialRegionId("US"),
                           DateUtils.getUTCDate(2014, 9, 12),
                           DateUtils.getUTCDate(2014, 12, 12),
                           0.0125,
                           -10000000,
                           InterestRateMockSources.getLiborIndexId(),
                           DateUtils.getUTCDate(2014, 1, 22));
  }

  private InterestRateFutureTrade createInterestRateFutureTrade() {
    return createInterestRateFutureTrade(createInterestRateFutureSecurity());
  }

  private InterestRateFutureTrade createInterestRateFutureTrade(InterestRateFutureSecurity security) {
    SimpleCounterparty counterparty = new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "TEST"));
    return createInterestRateFutureTrade(security, counterparty);
  }

  private InterestRateFutureTrade createInterestRateFutureTrade(InterestRateFutureSecurity security,
                                                                SimpleCounterparty counterparty) {
    BigDecimal tradeQuantity = BigDecimal.valueOf(10);
    LocalDate tradeDate = LocalDate.of(2000, 1, 1);
    OffsetTime tradeTime = OffsetTime.of(LocalTime.of(0, 0), ZoneOffset.UTC);
    SimpleTrade trade = new SimpleTrade(security, tradeQuantity, counterparty, tradeDate, tradeTime);
    trade.setPremium(0.0);
    trade.setPremiumCurrency(security.getCurrency());
    return new InterestRateFutureTrade(trade);
  }

  private InterestRateFutureSecurity createInterestRateFutureSecurity() {
    return createInterestRateFutureSecurity(Currency.USD);
  }

  private InterestRateFutureSecurity createInterestRateFutureSecurity(Currency currency) {
    Expiry expiry = new Expiry(ZonedDateTime.of(LocalDate.of(2014, 6, 18), LocalTime.of(0, 0), ZoneId.systemDefault()));
    String tradingExchange = "";
    String settlementExchange = "";
    double unitAmount = 1000;
    ExternalId underlyingId = InterestRateMockSources.getLiborIndexId();
    String category = "";
    return new InterestRateFutureSecurity(expiry,
                                          tradingExchange,
                                          settlementExchange,
                                          currency,
                                          unitAmount,
                                          underlyingId,
                                          category);
  }

}
