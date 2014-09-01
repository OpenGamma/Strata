/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.analytics;

import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE;
import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.NATURAL_CUBIC_SPLINE_INSTANCE;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.threeten.bp.ZoneOffset;

import com.google.common.primitives.Doubles;
import com.opengamma.analytics.financial.curve.interestrate.generator.GeneratorCurveYieldInterpolated;
import com.opengamma.analytics.financial.curve.interestrate.generator.GeneratorYDCurve;
import com.opengamma.analytics.financial.instrument.annuity.AnnuityDefinition;
import com.opengamma.analytics.financial.instrument.annuity.FixedAnnuityDefinitionBuilder;
import com.opengamma.analytics.financial.instrument.annuity.FloatingAnnuityDefinitionBuilder;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.annuity.derivative.Annuity;
import com.opengamma.analytics.financial.interestrate.cash.derivative.Cash;
import com.opengamma.analytics.financial.interestrate.payments.derivative.Payment;
import com.opengamma.analytics.financial.interestrate.swap.derivative.Swap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.LastTimeCalculator;
import com.opengamma.analytics.financial.provider.curve.multicurve.GeneratorMulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.curve.multicurve.MulticurveDiscountBuildingData;
import com.opengamma.analytics.financial.provider.curve.multicurve.MulticurveDiscountFinderFunction;
import com.opengamma.analytics.financial.provider.curve.multicurve.MulticurveDiscountFinderJacobian;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.ParameterSensitivityMulticurveUnderlyingMatrixCalculator;
import com.opengamma.analytics.math.function.Function1D;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.analytics.math.interpolation.FlatExtrapolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.linearalgebra.DecompositionFactory;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.analytics.math.rootfinding.newton.BroydenVectorRootFinder;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.BusinessDayCalendar;
import com.opengamma.basics.date.BusinessDayConvention;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.date.Tenor;
import com.opengamma.financial.convention.businessday.BusinessDayConventionFactory;
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.financial.convention.frequency.SimpleFrequency;

/**
 * Builds yield curves from a set of instruments and rates. Sensible
 * defaults are used but these can be changed by creating new instances
 * using the with* methods.
 */
public class InstrumentCurveCalibrator {

  private static final ParSpreadMarketQuoteDiscountingCalculator DISCOUNTING_CALCULATOR =
      ParSpreadMarketQuoteDiscountingCalculator.getInstance();

  private static final ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator CURVE_SENSITIVITY_CALCULATOR =
      ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator.getInstance();

  /**
   * The default extrapolator to be used.
   */
  public static final FlatExtrapolator1D DEFAULT_EXTRAPOLATOR = FLAT_EXTRAPOLATOR_INSTANCE;

  /**
   * The default interpolator/extrapolator to be used.
   */
  public static final Interpolator1D DEFAULT_INTERPOLATOR_EXTRAPOLATOR =
      new CombinedInterpolatorExtrapolator(NATURAL_CUBIC_SPLINE_INSTANCE, DEFAULT_EXTRAPOLATOR);

  private final Interpolator1D interpolatorExtrapolator;

  private final DayCount cashDayCount;

  private final DayCount swapDayCount;

  private final SimpleFrequency swapFrequency;

  private final BusinessDayConvention businessDayConvention;

  private final BusinessDayCalendar businessDayCalendar;


  /**
   * The root finder used for curve calibration.
   */
  private final BroydenVectorRootFinder rootFinder = new BroydenVectorRootFinder(
      1e-9, 1e-9, 1000, DecompositionFactory.getDecomposition(DecompositionFactory.SV_COLT_NAME));

  public InstrumentCurveCalibrator() {
    this(DEFAULT_INTERPOLATOR_EXTRAPOLATOR, DayCount.DC_ACT_365F, DayCount.DC_ACT_365F, SimpleFrequency.SEMI_ANNUAL,
        BusinessDayConvention.FOLLOWING, BusinessDayCalendar.WEEKENDS);
  }

  private InstrumentCurveCalibrator(Interpolator1D interpolatorExtrapolator, DayCount cashDayCount,
                                    DayCount swapDayCount, SimpleFrequency swapFrequency,
                                    BusinessDayConvention businessDayConvention,
                                    BusinessDayCalendar businessDayCalendar) {
    this.interpolatorExtrapolator = interpolatorExtrapolator;
    this.cashDayCount = cashDayCount;
    this.swapDayCount = swapDayCount;
    this.swapFrequency = swapFrequency;
    this.businessDayConvention = businessDayConvention;
    this.businessDayCalendar = businessDayCalendar;
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * interpolation method.
   *
   * @param interpolationMethod  the interpolation method to be used
   * @return a new curve calibrator
   */
  public InstrumentCurveCalibrator withInterpolation(InterpolationMethod interpolationMethod) {
    return new InstrumentCurveCalibrator(buildInterpolatorExtrapolator(interpolationMethod),
        cashDayCount, swapDayCount, swapFrequency, businessDayConvention, businessDayCalendar);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * day count for the cash instruments.
   *
   * @param cashDayCount  the day count to be used for the cash instruments
   * @return a new curve calibrator
   */
  public InstrumentCurveCalibrator withCashDayCount(DayCount cashDayCount) {
    return new InstrumentCurveCalibrator(interpolatorExtrapolator, cashDayCount, swapDayCount, swapFrequency,
        businessDayConvention, businessDayCalendar);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * day count for the swap instruments.
   *
   * @param swapDayCount  the day count to be used for the swap instruments
   * @return a new curve calibrator
   */
  public InstrumentCurveCalibrator withSwapDayCount(DayCount swapDayCount) {
    return new InstrumentCurveCalibrator(interpolatorExtrapolator, cashDayCount, swapDayCount, swapFrequency,
        businessDayConvention, businessDayCalendar);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * frequency for the swap instruments.
   *
   * @param swapFrequency  the frequency of the swap instruments
   * @return a new curve calibrator
   */
  public InstrumentCurveCalibrator withSwapFrequency(SimpleFrequency swapFrequency) {
    return new InstrumentCurveCalibrator(interpolatorExtrapolator, cashDayCount, swapDayCount, swapFrequency,
        businessDayConvention, businessDayCalendar);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * business day convention.
   *
   * @param businessDayConvention  the business day convention to be used
   * @return a new curve calibrator
   */
  public InstrumentCurveCalibrator withBusinessDayConvention(BusinessDayConvention businessDayConvention) {
    return new InstrumentCurveCalibrator(interpolatorExtrapolator, cashDayCount, swapDayCount, swapFrequency,
        businessDayConvention, businessDayCalendar);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * business day convention.
   *
   * @param businessDayCalendar  the business day calendar to be used
   * @return a new curve calibrator
   */
  public InstrumentCurveCalibrator withBusinessDayCalendar(BusinessDayCalendar businessDayCalendar) {
    return new InstrumentCurveCalibrator(interpolatorExtrapolator, cashDayCount, swapDayCount, swapFrequency,
        businessDayConvention, businessDayCalendar);
  }

  private Interpolator1D buildInterpolatorExtrapolator(InterpolationMethod interpolationMethod) {
    return new CombinedInterpolatorExtrapolator(interpolationMethod.getInterpolator(), DEFAULT_EXTRAPOLATOR);
  }

  // TODO - maybe we should introduce a CurveDefinition class (why are all the best names already used?)
  YieldCurve buildYieldCurve(Map<Tenor, CurveNodeInstrumentType> instruments, Map<Tenor, Double> rates,
                             Currency currency, LocalDate valuationDate) {

    // Validate we have sensible tenors and order them (or ensure they're ordered)

    LocalDate startDate = businessDayCalendar.ensure(valuationDate, businessDayConvention);

    // We need to build an array containing the dates as year fractions
    // and one containing InstrumentDefinitions/Derivatives. These can then
    // be passed to



//



    // Do date -> year fraction conversions


    // Build doubles curve using default interpolator


    InstrumentDerivative[] derivatives = createDerivatives(instruments, rates, startDate, currency);

    // If we are using fx forwards or futures, the market value does
    // not provide a good initial guess


    // TODO - this needs ordering!!
    double[] initGuess = Doubles.toArray(rates.values());

    GeneratorYDCurve initialGenerator = new GeneratorCurveYieldInterpolated(
        LastTimeCalculator.getInstance(), interpolatorExtrapolator);

    // This potentially changes the x-values (times) depending on the
    // instruments in the curve. Do we need to account for this as we
    // request values on the curve?
    GeneratorYDCurve finalGenerator = initialGenerator.finalGenerator(derivatives);
    initGuess = finalGenerator.initialGuess(initGuess);

    LinkedHashMap<String, com.opengamma.util.money.Currency> discountingMap = new LinkedHashMap<>();
    discountingMap.put("name", convertCurrency(currency));

    LinkedHashMap<String, GeneratorYDCurve> generatorsMap = new LinkedHashMap<>();
    generatorsMap.put("name", finalGenerator);

    // This basically replicates the makeUnit method from MulticurveDiscountBuildingRepository
    GeneratorMulticurveProviderDiscount generator = new GeneratorMulticurveProviderDiscount(
        new MulticurveProviderDiscount(), discountingMap, new LinkedHashMap<>(), new LinkedHashMap<>(), generatorsMap);

    MulticurveDiscountBuildingData data = new MulticurveDiscountBuildingData(derivatives, generator);

    Function1D<DoubleMatrix1D, DoubleMatrix1D> curveCalculator = new MulticurveDiscountFinderFunction(DISCOUNTING_CALCULATOR, data);

    Function1D<DoubleMatrix1D, DoubleMatrix2D> jacobianCalculator = new MulticurveDiscountFinderJacobian(
        new ParameterSensitivityMulticurveUnderlyingMatrixCalculator(CURVE_SENSITIVITY_CALCULATOR), data);

    DoubleMatrix1D nodeValues = rootFinder.getRoot(curveCalculator, jacobianCalculator, new DoubleMatrix1D(initGuess));


    final MulticurveProviderDiscount newCurves = data.getGeneratorMarket().evaluate(nodeValues);

    YieldAndDiscountCurve calibratedCurve = newCurves.getCurve("name");

    // For calculating year fractions for retrieving discount factors from the
    // calibrated curve, we always use ACT_365
    DayCount dayCount = DayCount.DC_ACT_365F;

    return new YieldCurve() {
      @Override
      public YieldAndDiscountCurve getCalibratedCurve() {
        return calibratedCurve;
      }

      @Override
      public double getDiscountFactor(LocalDate date) {
        return calibratedCurve.getDiscountFactor(dayCount.getDayCountFraction(startDate, date));
      }

      @Override
      public double getDiscountFactor(Tenor tenor) {
        return getDiscountFactor(startDate.plus(tenor.getPeriod()));
      }

      @Override
      public double getForwardRate(Tenor startTenor, Tenor endTenor) {

        double forwardLength = dayCount.getDayCountFraction(startDate.plus(startTenor.getPeriod()), startDate.plus(endTenor.getPeriod()));
        return (getDiscountFactor(startTenor) / getDiscountFactor(endTenor) - 1) / forwardLength;
      }

      public double getInterestRate(LocalDate date) {
        return calibratedCurve.getInterestRate(dayCount.getDayCountFraction(startDate, date));
      }
    };
  }

  private com.opengamma.util.money.Currency convertCurrency(Currency currency) {
    return com.opengamma.util.money.Currency.of(currency.toString());
  }

  private InstrumentDerivative[] createDerivatives(Map<Tenor, CurveNodeInstrumentType> instruments,
                                                   Map<Tenor, Double> rates, LocalDate startDate, Currency currency) {

    Function<Map.Entry<Tenor, Double>, InstrumentDerivative> valueMapper = e -> {
      Tenor tenor = e.getKey();
      double rate = e.getValue();
      CurveNodeInstrumentType instrumentType = instruments.get(tenor);
      switch (instrumentType) {
        case CASH:
          return convertCash(startDate, tenor, rate, currency);
        case SWAP:
          return convertSwap(startDate, tenor, rate, currency);
        default:
          throw new RuntimeException("Unable to handle instrument type: " + instrumentType);
      }
    };

    return rates.entrySet().stream()
        .sorted((o1, o2) -> {
          Period tenor1 = o1.getKey().getPeriod();
          Period tenor2 = o2.getKey().getPeriod();
          return startDate.plus(tenor1).compareTo(startDate.plus(tenor2));
        })
        .map(valueMapper)
        .toArray(InstrumentDerivative[]::new);
  }

  private InstrumentDerivative convertSwap(LocalDate startDate, Tenor tenor, Double rate, Currency currency) {

    LocalDate endDate = businessDayCalendar.ensure(startDate.plus(tenor.getPeriod()), businessDayConvention);

    AnnuityDefinition<?> fixedAnnuity = new FixedAnnuityDefinitionBuilder()
        .rate(rate)
        .startDate(convertDate(startDate))
        .endDate(convertDate(endDate))
        .currency(convertCurrency(currency))
        .accrualPeriodFrequency(swapFrequency.toPeriodFrequency().getPeriod())
        .dayCount(DayCountFactory.of(swapDayCount.getName()))
        .notional(date -> 1)
        // todo - add something to ensure a calendar is created
        .build();

    IborIndex iborIndex = new IborIndex(convertCurrency(currency), convertPeriod(tenor.getPeriod()), 0,
        convertDayCount(swapDayCount), convertBDC(businessDayConvention), false, "ix name");

    AnnuityDefinition<?> floatingAnnuity = new FloatingAnnuityDefinitionBuilder()
        .startDate(convertDate(startDate))
        .endDate(convertDate(endDate))
        .index(iborIndex)
        .notional(date -> 1)
        // todo - add something to ensure a calendar is created
        .build();


    Annuity<? extends Payment> fixed = fixedAnnuity.toDerivative(convertDate(startDate).atStartOfDay(ZoneOffset.UTC));
    Annuity<? extends Payment> floating = floatingAnnuity.toDerivative(convertDate(startDate).atStartOfDay(ZoneOffset.UTC));
    return new Swap(fixed, floating);
  }

  private com.opengamma.financial.convention.businessday.BusinessDayConvention convertBDC(
      BusinessDayConvention businessDayConvention) {
    return BusinessDayConventionFactory.of(businessDayConvention.getName());
  }

  private com.opengamma.financial.convention.daycount.DayCount convertDayCount(DayCount dayCount) {
    return DayCountFactory.of(dayCount.getName());
  }

  private org.threeten.bp.Period convertPeriod(Period period) {
    return org.threeten.bp.Period.of(period.getYears(), period.getMonths(), period.getDays());
  }

  private InstrumentDerivative convertCash(LocalDate startDate, Tenor tenor, Double rate, Currency currency) {

    LocalDate endDate = businessDayCalendar.ensure(startDate.plus(tenor.getPeriod()), businessDayConvention);

    double start = 0; // In future may need offset starts (e.g. TomNext etc)
    double end = start + DayCount.DC_ACT_365F.getDayCountFraction(startDate, endDate);

    // Accrual factor needs to be calculated in terms of the instrument's daycount
    double accrualFactor = cashDayCount.getDayCountFraction(startDate, endDate);
    return new Cash(convertCurrency(currency), start, end, 1, rate, accrualFactor);
  }

  private org.threeten.bp.LocalDate convertDate(LocalDate startDate) {
    return org.threeten.bp.LocalDate.ofEpochDay(startDate.toEpochDay());
  }

}

