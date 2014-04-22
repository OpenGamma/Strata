/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.analytics.curve.exposure.InstrumentExposuresProvider;
import com.opengamma.financial.security.fra.FRASecurity;
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
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.interestrate.InterestRateMockSources;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;

@Test(groups = TestGroup.UNIT)
public class FRAFnTest {

  private static final ZonedDateTime STD_REFERENCE_DATE = DateUtils.getUTCDate(2014, 1, 22);
  private static final ZonedDateTime STD_ACCRUAL_START_DATE = DateUtils.getUTCDate(2014, 9, 12);
  private static final ZonedDateTime STD_ACCRUAL_END_DATE = DateUtils.getUTCDate(2014, 12, 12);
  private static final ZonedDateTime VALUATION_TIME = DateUtils.getUTCDate(2014, 1, 22);

  private static final double STD_TOLERANCE_PV = 1.0E-3;
  private static final double STD_TOLERANCE_RATE = 1.0E-5;

  private static final double EXPECTED_PV = 23182.5437;
  private static final double EXPECTED_PAR_RATE = 0.003315;


  private FRAFn _fraFunction;
  private FRASecurity _fraSecurity = createSingleFra();
  private InterestRateMockSources _interestRateMockSources = new InterestRateMockSources();

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
            function(DefaultHistoricalTimeSeriesFn.class,
                     argument("resolutionKey", "DEFAULT_TSS"),
                     argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1)))),
            function(DefaultDiscountingMulticurveBundleFn.class,
                     argument("impliedCurveNames", StringSet.of()))),
        implementations(FRAFn.class, DiscountingFRAFn.class,
                        CurrencyPairsFn.class, DefaultCurrencyPairsFn.class,
                        InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class,
                        FRACalculatorFn.class, FRADiscountingCalculatorFn.class,
                        FRACalculatorFactory.class, FRACalculatorFactory.class,
                        CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                        FXMatrixFn.class, DefaultFXMatrixFn.class,
                        DiscountingMulticurveCombinerFn.class, ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                        CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                        DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                        CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                        CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class,
                        HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class,
                        MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                        MarketDataFn.class, DefaultMarketDataFn.class));

    Map<Class<?>, Object> components = _interestRateMockSources.generateBaseComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    _fraFunction = FunctionModel.build(FRAFn.class, config, ComponentMap.of(components));
  }

  @Test
  public void discountingFRAPV() {
    MarketDataSource dataSource = _interestRateMockSources.createMarketDataSource();
    Environment env = new SimpleEnvironment(VALUATION_TIME, dataSource);
    Result<MultipleCurrencyAmount> resultPV = _fraFunction.calculatePV(env, _fraSecurity);
    assertThat(resultPV.isSuccess(), is((true)));

    MultipleCurrencyAmount mca = resultPV.getValue();
    assertThat(mca.getCurrencyAmount(Currency.USD).getAmount(), is(closeTo(EXPECTED_PV, STD_TOLERANCE_PV)));
  }

  @Test
  public void parRateFRA() {
    MarketDataSource dataSource = _interestRateMockSources.createMarketDataSource();
    Environment env = new SimpleEnvironment(VALUATION_TIME, dataSource);
    Result<Double> resultParRate = _fraFunction.calculateParRate(env, _fraSecurity);
    assertThat(resultParRate.isSuccess(), is((true)));

    Double parRate = resultParRate.getValue();
    assertThat(parRate, is(closeTo(EXPECTED_PAR_RATE, STD_TOLERANCE_RATE)));
  }

  private FRASecurity createSingleFra() {
    return new FRASecurity(Currency.USD, ExternalSchemes.financialRegionId("US"), STD_ACCRUAL_START_DATE,
                           STD_ACCRUAL_END_DATE, 0.0125, -10000000, new InterestRateMockSources().getLiborIndexId(),
                           STD_REFERENCE_DATE);
  }

}
