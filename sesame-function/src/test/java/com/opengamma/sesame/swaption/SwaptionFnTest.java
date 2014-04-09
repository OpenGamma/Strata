/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.swaption;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.util.money.Currency.USD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZoneOffset;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.instrument.index.GeneratorSwapFixedIbor;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.interestrate.PresentValueSABRSensitivityDataBundle;
import com.opengamma.analytics.financial.model.option.definition.SABRInterestRateParameters;
import com.opengamma.analytics.financial.model.volatility.smile.function.SABRHaganVolatilityFunction;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.GridInterpolator2D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.link.SecurityLink;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.analytics.curve.exposure.InstrumentExposuresProvider;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.convention.frequency.PeriodFrequency;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.irs.FixedInterestRateSwapLeg;
import com.opengamma.financial.security.irs.FloatingInterestRateSwapLeg;
import com.opengamma.financial.security.irs.InterestRateSwapNotional;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.financial.security.irs.PayReceiveType;
import com.opengamma.financial.security.irs.Rate;
import com.opengamma.financial.security.option.ExerciseType;
import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.financial.security.swap.FloatingRateType;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
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
import com.opengamma.util.GUIDGenerator;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Expiry;
import com.opengamma.util.tuple.DoublesPair;
import com.opengamma.util.tuple.ObjectsPair;
import com.opengamma.util.tuple.Pair;

/**
 * Tests the swaption analytics functions with expected values taken
 * from SwaptionPhysicalFixedIborSABRMethodE2ETest in og-analytics.
 */
public class SwaptionFnTest {

  private static final double TOLERANCE_PV = 1.0E-3;

  private static final InterestRateMockSources _interestRateMockSources = new InterestRateMockSources();

  private static final Environment ENV =
      new SimpleEnvironment(DateUtils.getUTCDate(2014, 1, 22),
                            _interestRateMockSources.createMarketDataSource(LocalDate.of(2014, 2, 18)));

  private SwaptionFn _swaptionFn;

  private SwaptionSecurity _swaptionSecurity = createSingleSwaption();

  @BeforeClass
  public void setUp() {
    FunctionModelConfig config = config(
        arguments(
            function(ConfigDbMarketExposureSelectorFn.class,
                     argument("exposureConfig", ConfigLink.of("Test USD",
                                                              _interestRateMockSources.mockExposureFunctions())) ),
            function(RootFinderConfiguration.class,
                     argument("rootFinderAbsoluteTolerance", 1e-12),
                     argument("rootFinderRelativeTolerance", 1e-12),
                     argument("rootFinderMaxIterations", 5000)),
            function(DefaultHistoricalTimeSeriesFn.class,
                     argument("resolutionKey", "DEFAULT_TSS"),
                     argument("htsRetrievalPeriod", RetrievalPeriod.of(Period.ofYears(1)))),
            function(DefaultDiscountingMulticurveBundleFn.class,
                     argument("impliedCurveNames", StringSet.of()))
        ),
        implementations(SwaptionFn.class, SABRSwaptionFn.class,
                        InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class,
                        SwaptionCalculatorFactory.class, SABRSwaptionCalculatorFactory.class,
                        SABRInterestRateParametersFn.class, MockSabrProvider.class,
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

    ImmutableMap<Class<?>, Object> components = _interestRateMockSources.generateBaseComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    _swaptionFn = FunctionModel.build(SwaptionFn.class, config, ComponentMap.of(components));
  }

  @Test
  public void testPresentValue() {
    Result<MultipleCurrencyAmount> pvComputed = _swaptionFn.calculatePV(ENV, _swaptionSecurity);
    assertThat(pvComputed.isSuccess(), is(true));
    assertThat(pvComputed.getValue().getAmount(USD), is(closeTo(3156216.48957, TOLERANCE_PV)));
  }

  @Test
  public void testImpliedVolatility() {
    Result<Double> pvComputed = _swaptionFn.calculateImpliedVolatility(ENV, _swaptionSecurity);
    assertThat(pvComputed.isSuccess(), is(true));
    assertThat(pvComputed.getValue(), is(closeTo(0.298092262, 1E-8)));
  }

  @Test
  public void testBucketedPV01() {

    double[] deltaDsc = {
        -0.8970521909327039, -0.8970528138871251, 2.0679726864123788E-5, -2.800077859468568E-4,
        0.020545355340195248, -28.660344224880443, 1.0311659235333974, -101.02574104758263, -162.90022502561072,
        -34.11856047817592, -41.87866271284144, -47.20852985708558, -52.64477419064427,
        -193.55488041593657, -379.8195117988651, 26.793804732157106, 259.3051035445537 };

    double[] deltaFwd3 = {
        0.6768377111533482, -0.013861472263779616, -0.00815248053117034, 28.045784074714817,
        -10296.86232676286, -9.445439010985615, -12.048126446934697, -60.09929275115254,
        14090.425121330405, 28748.01823487962, 0.00, 0.00, 0.00, 0.00, 0.00 };

    LinkedHashMap<Pair<String, Currency>, DoubleMatrix1D> sensitivity = new LinkedHashMap<>();
    sensitivity.put(ObjectsPair.of("USD-ON-OIS", USD), new DoubleMatrix1D(deltaDsc));
    sensitivity.put(ObjectsPair.of("USD-LIBOR3M-FRAIRS", USD), new DoubleMatrix1D(deltaFwd3));
    MultipleCurrencyParameterSensitivity pvpsExpected = new MultipleCurrencyParameterSensitivity(sensitivity);

    Result<MultipleCurrencyParameterSensitivity> sensitivityResult = _swaptionFn.calculateBucketedPV01(ENV, _swaptionSecurity);
    assertThat(sensitivityResult.isSuccess(), is(true));

    Map<Pair<String, Currency>, DoubleMatrix1D> sensitivities = sensitivityResult.getValue().getSensitivities();
    Map<Pair<String, Currency>, DoubleMatrix1D> expected = pvpsExpected.getSensitivities();

    assertThat(sensitivities.size(), is(expected.size()));
    assertThat(sensitivities.keySet(), is(expected.keySet()));

    for (Pair<String, Currency> key : sensitivities.keySet()) {

      double[] data = sensitivities.get(key).getData();
      double[] expectedData = expected.get(key).getData();

      assertThat(data.length, is(expectedData.length));

      for (int i = 0; i < data.length; i++) {
        assertThat(data[i], is(closeTo(expectedData[i], 1E-4)));
      }
    }
  }

  @Test
  public void testBucketedSABRRisk() {

    Result<PresentValueSABRSensitivityDataBundle> result =
        _swaptionFn.calculateBucketedSABRRisk(ENV, _swaptionSecurity);
    assertThat(result.isSuccess(), is(true));
    PresentValueSABRSensitivityDataBundle sabrRisk = result.getValue();

    Map<DoublesPair, Double> alphaRiskExpected = ImmutableMap.of(
        DoublesPair.of(1.0, 5.0), 6204.475194599176,
        DoublesPair.of(2.0, 5.0), 3.946312129841228E7,
        DoublesPair.of(1.0, 10.0), 4136.961894403856,
        DoublesPair.of(2.0, 10.0), 2.6312850632053435E7);
    Map<DoublesPair, Double> alphaRiskComputed = sabrRisk.getAlpha().getMap();
    checkSabrRiskValues(alphaRiskExpected, alphaRiskComputed);

    Map<DoublesPair, Double> betaRiskExpected = ImmutableMap.of(
        DoublesPair.of(1.0, 5.0), -1135.9264046809967,
        DoublesPair.of(2.0, 5.0), -7224978.7593665235,
        DoublesPair.of(1.0, 10.0), -757.402375482628,
        DoublesPair.of(2.0, 10.0), -4817403.709083163);
    Map<DoublesPair, Double> betaRiskComputed = sabrRisk.getBeta().getMap();
    checkSabrRiskValues(betaRiskExpected, betaRiskComputed);

    Map<DoublesPair, Double> rhoRiskExpected = ImmutableMap.of(
        DoublesPair.of(1.0, 5.0), 25.108219123928023,
        DoublesPair.of(2.0, 5.0), 159699.0342933747,
        DoublesPair.of(1.0, 10.0), 16.74142332657722,
        DoublesPair.of(2.0, 10.0), 106482.62725264493);
    Map<DoublesPair, Double> rhoRiskComputed = sabrRisk.getRho().getMap();
    checkSabrRiskValues(rhoRiskExpected, rhoRiskComputed);

    Map<DoublesPair, Double> nuRiskExpected = ImmutableMap.of(
        DoublesPair.of(1.0, 5.0), 37.75195237231597,
        DoublesPair.of(2.0, 5.0), 240118.59649586905,
        DoublesPair.of(1.0, 10.0), 25.17189343259352,
        DoublesPair.of(2.0, 10.0), 160104.0301854763);
    Map<DoublesPair, Double> nuRiskComputed = sabrRisk.getNu().getMap();
    checkSabrRiskValues(nuRiskExpected, nuRiskComputed);

  }

  private void checkSabrRiskValues(Map<DoublesPair, Double> alphaRiskExpected,
                                   Map<DoublesPair, Double> alphaRiskComputed) {
    for (Map.Entry<DoublesPair, Double> entry : alphaRiskExpected.entrySet()) {
      assertThat(alphaRiskComputed.get(entry.getKey()), is(closeTo(entry.getValue(), 1E-4)));
    }
  }

  private SwaptionSecurity createSingleSwaption() {

    InterestRateSwapNotional notional = new InterestRateSwapNotional(Currency.USD, 100_000_000);

    PeriodFrequency freq6m = PeriodFrequency.of(Period.ofMonths(6));
    Set<ExternalId> calendarUSNY = ImmutableSet.of(ExternalId.of(ExternalSchemes.ISDA_HOLIDAY, "USNY"));

    FixedInterestRateSwapLeg payLeg = new FixedInterestRateSwapLeg();
    payLeg.setNotional(notional);
    payLeg.setDayCountConvention(DayCounts.THIRTY_U_360);
    payLeg.setPaymentDateFrequency(freq6m);
    payLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setPaymentDateCalendars(calendarUSNY);
    payLeg.setAccrualPeriodFrequency(freq6m);
    payLeg.setAccrualPeriodBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    payLeg.setAccrualPeriodCalendars(calendarUSNY);
    payLeg.setRate(new Rate(0.035));
    payLeg.setPayReceiveType(PayReceiveType.PAY);

    PeriodFrequency freq3m = PeriodFrequency.of(Period.ofMonths(3));

    FloatingInterestRateSwapLeg receiveLeg = new FloatingInterestRateSwapLeg();
    receiveLeg.setNotional(notional);
    receiveLeg.setDayCountConvention(DayCounts.ACT_360);
    receiveLeg.setPaymentDateFrequency(freq3m);
    receiveLeg.setPaymentDateBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    receiveLeg.setPaymentDateCalendars(calendarUSNY);
    receiveLeg.setPaymentOffset(0);
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
    receiveLeg.setFloatingReferenceRateId(ExternalId.of("CONVENTION", "USD 3M IRS Ibor Leg"));
    receiveLeg.setPayReceiveType(PayReceiveType.RECEIVE);
    //receiveLeg.setRollConvention(RollConvention.EOM);

    FinancialSecurity swap = new InterestRateSwapSecurity(
        ExternalIdBundle.of(ExternalId.of("UUID", GUIDGenerator.generate().toString())),
        "test swap",
        LocalDate.of(2016, 1, 26), // effective date
        LocalDate.of(2023, 1, 26), // maturity date,
        ImmutableSet.of(payLeg, receiveLeg));

    SecurityLink<FinancialSecurity> swapLink = SecurityLink.of(swap, null, null);

    return new SwaptionSecurity(true, swapLink, true, new Expiry(LocalDate.of(2016, 1, 22).atStartOfDay(ZoneOffset.UTC)), false, USD, 100_000_000d,
                                ExerciseType.of("European"), LocalDate.of(2016, 1, 26).atStartOfDay(ZoneOffset.UTC));
  }

  public static class MockSabrProvider implements SABRInterestRateParametersFn {

    private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.LINEAR,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);

    private final InterpolatedDoublesSurface alphaSurface = InterpolatedDoublesSurface.from(
        new double[]{0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10},
        new double[]{1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 10},
        new double[]{0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.06, 0.06, 0.06, 0.06, 0.06, 0.06},
        INTERPOLATOR_2D);
    private final InterpolatedDoublesSurface betaSurface = InterpolatedDoublesSurface.from(
        new double[]{0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10},
        new double[]{1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 10},
        new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5},
        INTERPOLATOR_2D);
    private final InterpolatedDoublesSurface rhoSurface = InterpolatedDoublesSurface.from(
        new double[]{0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10},
        new double[]{1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 10},
        new double[]{-0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, -0.25, 0.00, 0.00, 0.00, 0.00},
        INTERPOLATOR_2D);
    private final InterpolatedDoublesSurface nuSurface = InterpolatedDoublesSurface.from(
        new double[]{0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10, 0.0, 0.5, 1, 2, 5, 10},
        new double[]{1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 10},
        new double[]{0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.30, 0.30, 0.30, 0.30},
        INTERPOLATOR_2D);

    @Override
    public Result<SABRParametersConfig> getSabrParameters(Environment env, SwaptionSecurity security) {
      SABRInterestRateParameters sabrInterestRateParameters =
          new SABRInterestRateParameters(alphaSurface, betaSurface, rhoSurface, nuSurface,
                                         new SABRHaganVolatilityFunction());


      IborIndex iborIndex =
          new IborIndex(Currency.USD, Period.ofMonths(3), 2, DayCounts.ACT_360,
                        BusinessDayConventions.MODIFIED_FOLLOWING, true, "USDLIBOR3M");

      GeneratorSwapFixedIbor swapConvention =
          new GeneratorSwapFixedIbor("USD6MLIBOR3M", Period.ofMonths(6),
                                     DayCounts.THIRTY_U_360, iborIndex, new NYCalendar());

      return Result.success(new SABRParametersConfig(sabrInterestRateParameters, swapConvention));
    }
  }

  private static final class NYCalendar extends MondayToFridayCalendar {

    public NYCalendar() {
      super("NYC");

      final int startYear = 2013;
      final int endYear = 2063;
      for (int i = startYear; i <= endYear; i++) {
        addNonWorkingDay(LocalDate.of(i, 1, 1));
        addNonWorkingDay(LocalDate.of(i, 7, 4));
        addNonWorkingDay(LocalDate.of(i, 12, 25));
      }
      addNonWorkingDay(LocalDate.of(2015, 1, 19));
      addNonWorkingDay(LocalDate.of(2015, 2, 16));
      addNonWorkingDay(LocalDate.of(2015, 5, 25));
      addNonWorkingDay(LocalDate.of(2015, 9, 7));
      addNonWorkingDay(LocalDate.of(2015, 10, 12));
      addNonWorkingDay(LocalDate.of(2015, 11, 11));
      addNonWorkingDay(LocalDate.of(2015, 11, 26));
      addNonWorkingDay(LocalDate.of(2016, 1, 18));
      addNonWorkingDay(LocalDate.of(2016, 2, 15));
      addNonWorkingDay(LocalDate.of(2016, 5, 30));
      addNonWorkingDay(LocalDate.of(2016, 9, 5));
      addNonWorkingDay(LocalDate.of(2016, 10, 10));
      addNonWorkingDay(LocalDate.of(2016, 11, 11));
      addNonWorkingDay(LocalDate.of(2016, 11, 24));
      addNonWorkingDay(LocalDate.of(2016, 12, 26));
      addNonWorkingDay(LocalDate.of(2017, 1, 2));
      addNonWorkingDay(LocalDate.of(2017, 1, 16));
      addNonWorkingDay(LocalDate.of(2017, 2, 20));
      addNonWorkingDay(LocalDate.of(2017, 5, 29));
      addNonWorkingDay(LocalDate.of(2017, 9, 4));
      addNonWorkingDay(LocalDate.of(2017, 10, 9));
      addNonWorkingDay(LocalDate.of(2017, 11, 23));
    }
  }
}
