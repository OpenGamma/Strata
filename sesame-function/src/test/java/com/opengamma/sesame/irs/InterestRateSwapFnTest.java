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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fudgemsg.FudgeMsg;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opengamma.analytics.financial.instrument.annuity.CompoundingMethod;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.financial.analytics.DoubleLabelledMatrix1D;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.analytics.curve.exposure.InstrumentExposuresProvider;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.financial.analytics.model.fixedincome.FixedLegCashFlows;
import com.opengamma.financial.analytics.model.fixedincome.FloatingLegCashFlows;
import com.opengamma.financial.analytics.model.fixedincome.SwapLegCashFlows;
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
import com.opengamma.sesame.CurveNodeConverterFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveNodeConverterFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleResolverFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.DiscountingMulticurveBundleResolverFn;
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
import com.opengamma.util.GUIDGenerator;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

@Test(groups = TestGroup.UNIT)
public class InterestRateSwapFnTest {

  private static final double STD_TOLERANCE_PV = 1.0E-3;
  private static final double STD_TOLERANCE_RATE = 1.0E-8;
  private static final double STD_TOLERANCE_PV01 = 1.0E-4;
  private static final double STD_TOLERANCE_AMOUNT = 1.0E-3;

  private static final double EXPECTED_ON_PV = -9723.264518929138;
  private static final double EXPECTED_ON_PAR_RATE =  6.560723881400023E-4;
  private static final double EXPECTED_ON_PV01 = 0.0000; //TODO What is the correct PV here

  private static final double EXPECTED_3M_PV = 7170391.798257509;
  private static final double EXPECTED_3M_PAR_RATE = 0.025894715668195054;
  private static final Map<Pair<String, Currency>, DoubleMatrix1D> EXPECTED_3M_BUCKETED_PV01 =
      ImmutableMap.<Pair<String, Currency>, DoubleMatrix1D>builder().
        put(Pairs.of(InterestRateMockSources.USD_OIS_CURVE_NAME, Currency.USD),
            new DoubleMatrix1D(-2.006128288990294, -2.0061296821289525, -8.674474462036337E-5, 0.0011745459584018116,
                               1.4847039748268902, -56.94910798756204, 1.1272953905008014, -86.07354103199485,
                               -166.96224130769323, -242.22201141057718, -314.1940601301674, -385.90291779006617,
                               -463.27621838407424, -979.7315579072819, -243.35533454746522, 243.5314116731397,
                               139.99052668955744)
        ).
        put(Pairs.of(InterestRateMockSources.USD_LIBOR3M_CURVE_NAME, Currency.USD),
            new DoubleMatrix1D(-2604.935862485561, -2632.099517240145, -1176.1264079088776, 27.132459445836727,
                               -34.13622855060674, -8.299063014960922, -10.516911339441654, 0.5088197267155414,
                               56648.04062946332, 15520.134985387911, 1.1803422588258187E-10, -2.746886018748557E-10,
                               1.2282414271537867E-10, 2.5897954580662176E-11, -1.309540899491159E-10)
        ).

        build();

  private static final double EXPECTED_FIXING_PV = -2434664.6871909914;

  private static final double NOTIONAL = 100000000; //100m

  private static final ZonedDateTime VALUATION_TIME = DateUtils.getUTCDate(2014, 1, 22);

  private static final Environment ENV = new SimpleEnvironment(VALUATION_TIME,
                                                               InterestRateMockSources.createMarketDataSource(
                                                                   LocalDate.of(2014, 2, 18)));
  private InterestRateSwapFn _swapFunction;
  private InterestRateSwapSecurity _fixedVsOnCompoundedSwapSecurity = createFixedVsOnCompoundedSwap();
  private InterestRateSwapSecurity _fixedVsLibor3mSwapSecurity = createFixedVsLibor3mSwap();
  private InterestRateSwapSecurity _fixedVsLiborWithFixingSwapSecurity = createFixedVsLiborWithFixingSwap();

  @BeforeClass
  public void setUpClass() throws IOException {
    FunctionModelConfig config =
        config(
            arguments(
                function(ConfigDbMarketExposureSelectorFn.class,
                         argument("exposureConfig",
                                  ConfigLink.resolved(InterestRateMockSources.mockExposureFunctions()))),
                function(RootFinderConfiguration.class,
                         argument("rootFinderAbsoluteTolerance", 1e-10),
                         argument("rootFinderRelativeTolerance", 1e-10),
                         argument("rootFinderMaxIterations", 5000)),
                function(DefaultCurrencyPairsFn.class,
                         argument("currencyPairs", ImmutableSet.of(/*no pairs*/))),
                function(DefaultHistoricalTimeSeriesFn.class,
                         argument("resolutionKey", "DEFAULT_TSS"),
                         argument("htsRetrievalPeriod",  RetrievalPeriod.of(Period.ofYears(1)))),
                function(DefaultCurveNodeConverterFn.class,
                         argument("timeSeriesDuration", RetrievalPeriod.of(Period.ofYears(1)))),
                function(DefaultHistoricalMarketDataFn.class,
                         argument("dataSource", "BLOOMBERG")),
                function(DefaultDiscountingMulticurveBundleFn.class,
                         argument("impliedCurveNames", StringSet.of()))),
            implementations(InterestRateSwapFn.class, DiscountingInterestRateSwapFn.class,
                            CurrencyPairsFn.class, DefaultCurrencyPairsFn.class,
                            InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class,
                            InterestRateSwapCalculatorFactory.class, DiscountingInterestRateSwapCalculatorFactory.class,
                            InterestRateSwapCalculator.class, DiscountingInterestRateSwapCalculator.class,
                            CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
                            CurveNodeConverterFn.class, DefaultCurveNodeConverterFn.class,
                            FXMatrixFn.class, DefaultFXMatrixFn.class,
                            DiscountingMulticurveCombinerFn.class, ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                            CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
                            DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
                            DiscountingMulticurveBundleResolverFn.class, DefaultDiscountingMulticurveBundleResolverFn.class,
                            CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
                            CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class,
                            HistoricalTimeSeriesFn.class, DefaultHistoricalTimeSeriesFn.class,
                            HistoricalMarketDataFn.class, DefaultHistoricalMarketDataFn.class,
                            MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class,
                            MarketDataFn.class, DefaultMarketDataFn.class));

    ImmutableMap<Class<?>, Object> components = InterestRateMockSources.generateBaseComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    _swapFunction = FunctionModel.build(InterestRateSwapFn.class, config, ComponentMap.of(components));
  }

  private InterestRateSwapSecurity createFixedVsLiborWithFixingSwap() {

    InterestRateSwapNotional notional = new InterestRateSwapNotional(Currency.USD, NOTIONAL);
    List<InterestRateSwapLeg> legs = new ArrayList<>();
    PeriodFrequency freq6m = PeriodFrequency.of(Period.ofMonths(6));
    PeriodFrequency freq3m = PeriodFrequency.of(Period.ofMonths(3));
    Set<ExternalId> calendarUsNy = Sets.newHashSet(ExternalId.of(ExternalSchemes.ISDA_HOLIDAY, "USNY"));

    FixedInterestRateSwapLeg payLeg = new FixedInterestRateSwapLeg();
    payLeg.setNotional(notional);
    payLeg.setDayCountConvention(DayCounts.THIRTY_U_360);
    payLeg.setPaymentDateFrequency(freq6m);
    payLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setPaymentDateCalendars(calendarUsNy);
    payLeg.setAccrualPeriodFrequency(freq6m);
    payLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setAccrualPeriodCalendars(calendarUsNy);
    payLeg.setRate(new Rate(0.02));
    payLeg.setPayReceiveType(PayReceiveType.PAY);
    legs.add(payLeg);

    FloatingInterestRateSwapLeg receiveLeg = new FloatingInterestRateSwapLeg();
    receiveLeg.setNotional(notional);
    receiveLeg.setDayCountConvention(DayCounts.ACT_360);
    receiveLeg.setPaymentDateFrequency(freq3m);
    receiveLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setPaymentDateCalendars(calendarUsNy);
    receiveLeg.setAccrualPeriodFrequency(freq3m);
    receiveLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setAccrualPeriodCalendars(calendarUsNy);
    receiveLeg.setResetPeriodFrequency(freq3m);
    receiveLeg.setResetPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setResetPeriodCalendars(calendarUsNy);
    receiveLeg.setFixingDateBusinessDayConvention(BusinessDayConventions.PRECEDING);
    receiveLeg.setFixingDateCalendars(calendarUsNy);
    receiveLeg.setFixingDateOffset(-2);
    receiveLeg.setFloatingRateType(FloatingRateType.IBOR);
    receiveLeg.setFloatingReferenceRateId(InterestRateMockSources.getLiborIndexId());
    receiveLeg.setPayReceiveType(PayReceiveType.RECEIVE);
    receiveLeg.setRollConvention(RollConvention.EOM);
    legs.add(receiveLeg);

    return new InterestRateSwapSecurity(
        ExternalIdBundle.of(ExternalId.of("UUID", GUIDGenerator.generate().toString())),
        "Fixed vs Libor with fixing",
        LocalDate.of(2013, 3, 19), // effective date
        LocalDate.of(2015, 3, 18), // maturity date,
        legs);
  }

  private InterestRateSwapSecurity createFixedVsOnCompoundedSwap() {

    InterestRateSwapNotional notional = new InterestRateSwapNotional(Currency.USD, NOTIONAL);
    List<InterestRateSwapLeg> legs = new ArrayList<>();
    PeriodFrequency freq1y = PeriodFrequency.of(Period.ofYears(1));
    Set<ExternalId> calendarUSNY = Sets.newHashSet(ExternalId.of(ExternalSchemes.ISDA_HOLIDAY, "USNY"));

    FixedInterestRateSwapLeg payLeg = new FixedInterestRateSwapLeg();
    payLeg.setNotional(notional);
    payLeg.setDayCountConvention(DayCounts.ACT_360);
    payLeg.setPaymentDateFrequency(freq1y);
    payLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setPaymentDateCalendars(calendarUSNY);
    payLeg.setAccrualPeriodFrequency(freq1y);
    payLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setAccrualPeriodCalendars(calendarUSNY);
    payLeg.setPaymentOffset(2);
    payLeg.setRate(new Rate(0.00123));
    payLeg.setPayReceiveType(PayReceiveType.PAY);
    legs.add(payLeg);

    FloatingInterestRateSwapLeg receiveLeg = new FloatingInterestRateSwapLeg();
    receiveLeg.setNotional(notional);
    receiveLeg.setDayCountConvention(DayCounts.ACT_360);
    receiveLeg.setPaymentDateFrequency(freq1y);
    receiveLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setPaymentDateCalendars(calendarUSNY);
    receiveLeg.setAccrualPeriodFrequency(PeriodFrequency.DAILY);
    receiveLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setAccrualPeriodCalendars(calendarUSNY);
    receiveLeg.setResetPeriodFrequency(freq1y);
    receiveLeg.setResetPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setResetPeriodCalendars(calendarUSNY);
    receiveLeg.setFixingDateBusinessDayConvention(BusinessDayConventions.PRECEDING);
    receiveLeg.setFixingDateCalendars(calendarUSNY);
    receiveLeg.setFixingDateOffset(-2);
    receiveLeg.setPaymentOffset(2);
    receiveLeg.setFloatingRateType(FloatingRateType.OIS);
    receiveLeg.setFloatingReferenceRateId(InterestRateMockSources.getOvernightIndexId());
    receiveLeg.setPayReceiveType(PayReceiveType.RECEIVE);
    receiveLeg.setCompoundingMethod(CompoundingMethod.FLAT);
    legs.add(receiveLeg);

    return new InterestRateSwapSecurity(
        ExternalIdBundle.of(ExternalId.of("UUID", GUIDGenerator.generate().toString())),
        "Fixed vs ON compounded",
        LocalDate.of(2014, 2, 5),
        LocalDate.of(2014, 4, 7), // maturity date,
        legs);
  }

  private InterestRateSwapSecurity createFixedVsLibor3mSwap() {

    InterestRateSwapNotional notional = new InterestRateSwapNotional(Currency.USD, NOTIONAL);
    PeriodFrequency freq6m = PeriodFrequency.of(Period.ofMonths(6));
    PeriodFrequency freq3m = PeriodFrequency.of(Period.ofMonths(3));
    Set<ExternalId> calendarUSNY = Sets.newHashSet(ExternalId.of(ExternalSchemes.ISDA_HOLIDAY, "USNY"));
    List<InterestRateSwapLeg> legs = new ArrayList<>();

    FixedInterestRateSwapLeg payLeg = new FixedInterestRateSwapLeg();
    payLeg.setNotional(notional);
    payLeg.setDayCountConvention(DayCounts.THIRTY_U_360);
    payLeg.setPaymentDateFrequency(freq6m);
    payLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setPaymentDateCalendars(calendarUSNY);
    payLeg.setAccrualPeriodFrequency(freq6m);
    payLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setAccrualPeriodCalendars(calendarUSNY);
    payLeg.setRate(new Rate(0.0150));
    payLeg.setPayReceiveType(PayReceiveType.PAY);
    legs.add(payLeg);

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
    receiveLeg.setFloatingReferenceRateId(InterestRateMockSources.getLiborIndexId());
    receiveLeg.setPayReceiveType(PayReceiveType.RECEIVE);

    legs.add(receiveLeg);

    return new InterestRateSwapSecurity(
        ExternalIdBundle.of(ExternalId.of("UUID", GUIDGenerator.generate().toString())),
        "Fixed vs Libor 3m",
        LocalDate.of(2014, 9, 12), // effective date
        LocalDate.of(2021, 9, 12), // maturity date,
        legs);
  }

  @Test
  public void fixedVsLibor3mSwapLegDetails() {
    Result<SwapLegCashFlows> payResult = _swapFunction.calculatePayLegCashFlows(ENV,_fixedVsLibor3mSwapSecurity);
    assertThat(payResult.isSuccess(), is((true)));
    SwapLegCashFlows payDetails = payResult.getValue();

    assertThat(payDetails, is((instanceOf(FixedLegCashFlows.class))));
    FixedLegCashFlows fixedLegCashFlows = (FixedLegCashFlows) payDetails;

    FudgeMsg fixedFudge = OpenGammaFudgeContext.getInstance().toFudgeMsg(fixedLegCashFlows).getMessage();
    OpenGammaFudgeContext.getInstance().fromFudgeMsg(fixedFudge);

    List<CurrencyAmount> actualPaymentAmounts = fixedLegCashFlows.getPaymentAmounts();
    List<CurrencyAmount> expectedPaymentAmounts =
        Lists.newArrayList(
            CurrencyAmount.of("USD", -750000.0),
            CurrencyAmount.of("USD", -758333.3333333333),
            CurrencyAmount.of("USD", -750000.0),
            CurrencyAmount.of("USD", -741666.6666666667),
            CurrencyAmount.of("USD", -754166.6666666666),
            CurrencyAmount.of("USD", -745833.3333333334),
            CurrencyAmount.of("USD", -750000.0),
            CurrencyAmount.of("USD", -750000.0),
            CurrencyAmount.of("USD", -750000.0),
            CurrencyAmount.of("USD", -750000.0),
            CurrencyAmount.of("USD", -750000.0),
            CurrencyAmount.of("USD", -758333.3333333333),
            CurrencyAmount.of("USD", -741666.6666666667),
            CurrencyAmount.of("USD", -754166.6666666666)
        );

    int i = 0;
    for(CurrencyAmount amount : expectedPaymentAmounts) {
      assertThat(amount.getAmount(), is(closeTo(actualPaymentAmounts.get(i).getAmount(), STD_TOLERANCE_AMOUNT)));
      i++;
    }

    List<LocalDate> actualAccrualStart = fixedLegCashFlows.getAccrualStart();
    List<LocalDate> expectedAccrualStart =
        Lists.newArrayList(
            LocalDate.of(2014,9,12),
            LocalDate.of(2015,3,12),
            LocalDate.of(2015,9,14),
            LocalDate.of(2016,3,14),
            LocalDate.of(2016,9,12),
            LocalDate.of(2017,3,13),
            LocalDate.of(2017,9,12),
            LocalDate.of(2018,3,12),
            LocalDate.of(2018,9,12),
            LocalDate.of(2019,3,12),
            LocalDate.of(2019,9,12),
            LocalDate.of(2020,3,12),
            LocalDate.of(2020,9,14),
            LocalDate.of(2021,3,12)
        );

    i = 0;
    for(LocalDate date : expectedAccrualStart) {
      assertThat(date, is(actualAccrualStart.get(i)));
      i++;
    }

    Result<SwapLegCashFlows> receiveResult = _swapFunction.calculateReceiveLegCashFlows(ENV,_fixedVsLibor3mSwapSecurity);
    assertThat(receiveResult.isSuccess(), is((true)));
    SwapLegCashFlows receiveDetails = receiveResult.getValue();

    assertThat(receiveDetails, is((instanceOf(FloatingLegCashFlows.class))));
    FloatingLegCashFlows floatingLegCashFlows = (FloatingLegCashFlows) receiveDetails;

    FudgeMsg floatingFudge = OpenGammaFudgeContext.getInstance().toFudgeMsg(floatingLegCashFlows).getMessage();
    OpenGammaFudgeContext.getInstance().fromFudgeMsg(floatingFudge);

    List<Double> expectedForwardRates = floatingLegCashFlows.getForwardRates();
    List<Double> actualForwardRates = Lists.newArrayList(
        0.002830776043127479, 0.003889725581109005, 0.00534820864384384, 0.006272357544524368, 0.007191464409902153,
        0.01010956533411408, 0.013521579956778747, 0.015318507913146722, 0.01710597237713002, 0.019924700484298726,
        0.02285929744200523, 0.024965464562180496, 0.027083323913778736, 0.028774171062606158, 0.03044395193791545,
        0.03248279019018542, 0.03450994398725367, 0.03441329882138877, 0.0342129941075707, 0.03585838813458806,
        0.03749357371138418, 0.03903671874199058, 0.04064520492792625, 0.04230705788571235, 0.04394811214893466,
        0.04127235310120026, 0.038531753356691775, 0.03959796966266573);

    i = 0;
    for(Double rate : expectedForwardRates) {
      assertThat(rate, is(closeTo(actualForwardRates.get(i), STD_TOLERANCE_RATE)));
      i++;
    }

    List<LocalDate> actualAccrualEnd  = floatingLegCashFlows.getAccrualEnd();
    List<LocalDate> expectedAccrualEnd =
        Lists.newArrayList(
            LocalDate.of(2014,12,12),
            LocalDate.of(2015,3,12),
            LocalDate.of(2015,6,12),
            LocalDate.of(2015,9,14),
            LocalDate.of(2015,12,14),
            LocalDate.of(2016,3,14),
            LocalDate.of(2016,6,13),
            LocalDate.of(2016,9,12),
            LocalDate.of(2016,12,12),
            LocalDate.of(2017,3,13),
            LocalDate.of(2017,6,12),
            LocalDate.of(2017,9,12),
            LocalDate.of(2017,12,12),
            LocalDate.of(2018,3,12),
            LocalDate.of(2018,6,12),
            LocalDate.of(2018,9,12),
            LocalDate.of(2018,12,12),
            LocalDate.of(2019,3,12),
            LocalDate.of(2019,6,12),
            LocalDate.of(2019,9,12),
            LocalDate.of(2019,12,12),
            LocalDate.of(2020,3,12),
            LocalDate.of(2020,6,12),
            LocalDate.of(2020,9,14),
            LocalDate.of(2020,12,14),
            LocalDate.of(2021,3,12),
            LocalDate.of(2021,6,14),
            LocalDate.of(2021,9,13)
        );

    i = 0;
    for(LocalDate date : expectedAccrualEnd) {
      assertThat(date, is(actualAccrualEnd.get(i)));
      i++;
    }

  }

  public void fixedVsOnCompoundedSwapLegDetails() {
    Result<SwapLegCashFlows> payResult = _swapFunction.calculatePayLegCashFlows(ENV,_fixedVsOnCompoundedSwapSecurity);
    assertThat(payResult.isSuccess(), is((true)));
    SwapLegCashFlows payDetails = payResult.getValue();

    assertThat(payDetails, is((instanceOf(FixedLegCashFlows.class))));

    Result<SwapLegCashFlows> receiveResult = _swapFunction.calculateReceiveLegCashFlows(ENV,_fixedVsOnCompoundedSwapSecurity);
    assertThat(receiveResult.isSuccess(), is((true)));
    SwapLegCashFlows receiveDetails = receiveResult.getValue();

    assertThat(receiveDetails, is((instanceOf(FloatingLegCashFlows.class))));
  }

  public void fixedVsLiborWithFixingSwapLegDetails() {
    Result<SwapLegCashFlows> payResult = _swapFunction.calculatePayLegCashFlows(ENV,_fixedVsLiborWithFixingSwapSecurity);
    assertThat(payResult.isSuccess(), is((true)));
    SwapLegCashFlows payDetails = payResult.getValue();

    assertThat(payDetails, is((instanceOf(FixedLegCashFlows.class))));

    Result<SwapLegCashFlows> receiveResult = _swapFunction.calculateReceiveLegCashFlows(ENV,_fixedVsLiborWithFixingSwapSecurity);
    assertThat(receiveResult.isSuccess(), is((true)));
    SwapLegCashFlows receiveDetails = receiveResult.getValue();

    assertThat(receiveDetails, is((instanceOf(FloatingLegCashFlows.class))));
  }

  @Test
  public void fixedVsOnCompoundedSwapPv() {
    Result<MultipleCurrencyAmount> resultPv = _swapFunction.calculatePV(ENV, _fixedVsOnCompoundedSwapSecurity);
    assertThat(resultPv.isSuccess(), is((true)));

    MultipleCurrencyAmount mca = resultPv.getValue();
    assertThat(mca.getCurrencyAmount(Currency.USD).getAmount(), is(closeTo(EXPECTED_ON_PV, STD_TOLERANCE_PV)));
  }

  @Test
  public void fixedVsLibor3mSwapPv() {
    Result<MultipleCurrencyAmount> resultPv = _swapFunction.calculatePV(ENV, _fixedVsLibor3mSwapSecurity);
    assertThat(resultPv.isSuccess(), is((true)));

    MultipleCurrencyAmount mca = resultPv.getValue();
    assertThat(mca.getCurrencyAmount(Currency.USD).getAmount(), is(closeTo(EXPECTED_3M_PV, STD_TOLERANCE_PV)));
  }

  //TODO when converting a swap to derivative and then getting the par rate via the ParRateDiscounting Calculator,
  //par rate is not available as the converted swap is not of type SwapFixedCoupon
  @Test(enabled = false)
  public void fixedVsOnCompoundedSwapParRate() {
    Result<Double> resultParRate = _swapFunction.calculateParRate(ENV, _fixedVsOnCompoundedSwapSecurity);
    assertThat(resultParRate.isSuccess(), is((true)));

    Double parRate = resultParRate.getValue();
    assertThat(parRate, is(closeTo(EXPECTED_ON_PAR_RATE, STD_TOLERANCE_RATE)));
  }

  @Test
  public void fixedVsLibor3mWithFixingSwapPv() {
    Result<MultipleCurrencyAmount> resultPv = _swapFunction.calculatePV(ENV, _fixedVsLiborWithFixingSwapSecurity);
    assertThat(resultPv.isSuccess(), is((true)));

    MultipleCurrencyAmount mca = resultPv.getValue();
    assertThat(mca.getCurrencyAmount(Currency.USD).getAmount(), is(closeTo(EXPECTED_FIXING_PV, STD_TOLERANCE_PV)));
  }

  @Test
  public void fixedVsLibor3mSwapParRate() {
    Result<Double> resultParRate = _swapFunction.calculateParRate(ENV, _fixedVsLibor3mSwapSecurity);
    assertThat(resultParRate.isSuccess(), is((true)));

    Double parRate = resultParRate.getValue();
    assertThat(parRate, is(closeTo(EXPECTED_3M_PAR_RATE, STD_TOLERANCE_RATE)));
  }

  //TODO enable test when PV01 expected is available
  @Test(enabled = false)
  public void interestRateSwapPv01() {
    Result<ReferenceAmount<Pair<String,Currency>>> resultPv01 =
        _swapFunction.calculatePV01(ENV, _fixedVsOnCompoundedSwapSecurity);
    assertThat(resultPv01.isSuccess(), is(true));

    ReferenceAmount<Pair<String,Currency>> pv01s = resultPv01.getValue();
    double pv01 = 0;
    for (final Map.Entry<Pair<String, Currency>, Double> entry : pv01s.getMap().entrySet()) {
      if (entry.getKey().getSecond().equals(Currency.USD)) {
        pv01 += entry.getValue();
      }
    }
    assertThat(pv01, is(closeTo(EXPECTED_ON_PV01, STD_TOLERANCE_PV01)));
  }

  @Test
  public void interestRateSwapBucketedPv01() {
    Result<BucketedCurveSensitivities> resultPv01 = _swapFunction.calculateBucketedPV01(ENV, _fixedVsLibor3mSwapSecurity);
    assertThat(resultPv01.isSuccess(), is(true));

    Map<Pair<String, Currency>, DoubleLabelledMatrix1D> pv01s = resultPv01.getValue().getSensitivities();
    assertThat(pv01s.size(), is(EXPECTED_3M_BUCKETED_PV01.size()));
    for (Map.Entry<Pair<String, Currency>, DoubleLabelledMatrix1D> sensitivity : pv01s.entrySet()) {
      DoubleMatrix1D expectedSensitivities = EXPECTED_3M_BUCKETED_PV01.get(sensitivity.getKey());
      assertThat(sensitivity.getKey() + " not an expected sensitivity", expectedSensitivities, is(notNullValue()));
      assertThat(sensitivity.getValue().size(), is(expectedSensitivities.getNumberOfElements()));
      for (int i = 0; i < expectedSensitivities.getNumberOfElements(); i++) {
        assertThat(sensitivity.getValue().getValues()[i], is(closeTo(expectedSensitivities.getEntry(i), STD_TOLERANCE_PV01)));
      }
    }
  }

}
