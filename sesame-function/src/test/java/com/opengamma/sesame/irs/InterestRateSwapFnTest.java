/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.analytics.curve.exposure.InstrumentExposuresProvider;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.convention.frequency.PeriodFrequency;
import com.opengamma.financial.convention.rolldate.RollConvention;
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
import com.opengamma.util.GUIDGenerator;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.tuple.Pair;

@Test(groups = TestGroup.UNIT)
public class InterestRateSwapFnTest {

  private static final InterestRateMockSources _interestRateMockSources = new InterestRateMockSources();
  
  private static final double STD_TOLERANCE_PV = 1.0E-2;
  private static final double STD_TOLERANCE_RATE = 1.0E-5; //TODO What is the correct tolerance here
  private static final double STD_TOLERANCE_PV01 = 1.0E-5; //TODO What is the correct tolerance here

  private static final double EXPECTED_PV = -16376.245; //TODO What is the correct PV here
  private static final double EXPECTED_PAR_RATE = 0.0000; //TODO What is the correct par rate here
  private static final double EXPECTED_PV01 = 0.0000; //TODO What is the correct PV here

  private static final ZonedDateTime VALUATION_TIME = DateUtils.getUTCDate(2014, 1, 22);

  private InterestRateSwapFn _swapFunction;
  private InterestRateSwapSecurity _swapSecurity = createSingleSwap();
  private static final Environment ENV = new SimpleEnvironment(VALUATION_TIME,
      _interestRateMockSources.createMarketDataSource());

  @BeforeClass
  public void setUpClass() throws IOException {
    FunctionModelConfig config = config(
        arguments(
            function(ConfigDbMarketExposureSelectorFn.class,
                     argument("exposureConfig", ConfigLink.of("Test USD", _interestRateMockSources.mockExposureFunctions()))),
            function(RootFinderConfiguration.class,
                     argument("rootFinderAbsoluteTolerance", 1e-9),
                     argument("rootFinderRelativeTolerance", 1e-9),
                     argument("rootFinderMaxIterations", 1000)),
            function(DefaultCurrencyPairsFn.class,
                     argument("currencyPairs", ImmutableSet.of(/*no pairs*/))),
            function(DefaultHistoricalTimeSeriesFn.class,
                     argument("resolutionKey", "DEFAULT_TSS"),
                     argument("htsRetrievalPeriod",  RetrievalPeriod.of(Period.ofYears(1)))),
            function(DefaultDiscountingMulticurveBundleFn.class,
                     argument("impliedCurveNames",StringSet.of()))),
        implementations(InterestRateSwapFn.class, DiscountingInterestRateInterestRateSwapFn.class,
                        CurrencyPairsFn.class, DefaultCurrencyPairsFn.class,
                        InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class,
                        InterestRateSwapCalculatorFn.class, InterestRateSwapDiscountingCalculatorFn.class,
                        InterestRateSwapCalculatorFactory.class, InterestRateSwapCalculatorFactory.class,
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

    ImmutableMap<Class<?>, Object> components = _interestRateMockSources.generateBaseComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    _swapFunction = FunctionModel.build(InterestRateSwapFn.class, config, ComponentMap.of(components));
  }

  private InterestRateSwapSecurity createSingleSwap() {

    InterestRateSwapNotional notional = new InterestRateSwapNotional(Currency.USD, 1_000_000);

    List<InterestRateSwapLeg> legs = new ArrayList<>();

    PeriodFrequency freq6m = PeriodFrequency.of(Period.ofMonths(6));
    Set<ExternalId> calendarUSNY = Sets.newHashSet(ExternalId.of(ExternalSchemes.ISDA_HOLIDAY, "USNY"));

    FixedInterestRateSwapLeg payLeg = new FixedInterestRateSwapLeg();
    payLeg.setNotional(notional);
    payLeg.setDayCountConvention(DayCounts.THIRTY_U_360);
    payLeg.setPaymentDateFrequency(freq6m);
    payLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setPaymentDateCalendars(calendarUSNY);
    payLeg.setAccrualPeriodFrequency(freq6m);
    payLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setAccrualPeriodCalendars(calendarUSNY);
    payLeg.setRate(new Rate(0.02));
    payLeg.setPayReceiveType(PayReceiveType.PAY);
    legs.add(payLeg);

    PeriodFrequency freq3m = PeriodFrequency.of(Period.ofMonths(3));

    FloatingInterestRateSwapLeg receiveLeg = new FloatingInterestRateSwapLeg();
    receiveLeg.setNotional(notional);
    receiveLeg.setDayCountConvention(DayCounts.ACT_360);
    receiveLeg.setPaymentDateFrequency(freq3m);
    receiveLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setPaymentDateCalendars(calendarUSNY);
    receiveLeg.setAccrualPeriodFrequency(freq3m);
    receiveLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setAccrualPeriodCalendars(calendarUSNY);
    receiveLeg.setResetPeriodFrequency(freq3m);
    receiveLeg.setResetPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setResetPeriodCalendars(calendarUSNY);
    receiveLeg.setFixingDateBusinessDayConvention(BusinessDayConventions.PRECEDING);
    receiveLeg.setFixingDateCalendars(calendarUSNY);
    receiveLeg.setFixingDateOffset(-2);
    receiveLeg.setFloatingRateType(FloatingRateType.IBOR);
    receiveLeg.setFloatingReferenceRateId(_interestRateMockSources.getLiborIndexId());
    receiveLeg.setPayReceiveType(PayReceiveType.RECEIVE);
    receiveLeg.setRollConvention(RollConvention.EOM);
    legs.add(receiveLeg);

    return new InterestRateSwapSecurity(
        ExternalIdBundle.of(ExternalId.of("UUID", GUIDGenerator.generate().toString())),
        "test swap",
        LocalDate.of(2014, 3, 19), // effective date
        LocalDate.of(2015, 3, 18), // maturity date,
        legs);
  }

  @Test(enabled = false)
  public void interestRateSwapPV() {
    Result<MultipleCurrencyAmount> resultPV = _swapFunction.calculatePV(ENV, _swapSecurity);
    assertThat(resultPV.isSuccess(), is((true)));

    MultipleCurrencyAmount mca = resultPV.getValue();
    assertThat(mca.getCurrencyAmount(Currency.USD).getAmount(), is(closeTo(EXPECTED_PV, STD_TOLERANCE_PV)));
  }

  @Test(enabled = false)
  public void interestRateSwapParRate() {
    Result<Double> resultParRate = _swapFunction.calculateParRate(ENV, _swapSecurity);
    assertThat(resultParRate.isSuccess(), is((true)));

    Double parRate = resultParRate.getValue();
    assertThat(parRate, is(closeTo(EXPECTED_PAR_RATE, STD_TOLERANCE_RATE)));
  }

  @Test(enabled = false)
  public void interestRateSwapPV01() {
    Result<ReferenceAmount<Pair<String,Currency>>> resultPV01 = _swapFunction.calculatePV01(ENV, _swapSecurity);
    assertThat(resultPV01.isSuccess(), is((true)));

    ReferenceAmount<Pair<String,Currency>> pv01s = resultPV01.getValue();
    double pv01 = 0;
    for (final Map.Entry<Pair<String, Currency>, Double> entry : pv01s.getMap().entrySet()) {
      if (entry.getKey().getSecond().equals(Currency.USD)) {
        pv01 += entry.getValue();
      }
    }
    assertThat(pv01, is(closeTo(EXPECTED_PV01, STD_TOLERANCE_PV01)));
  }

}
