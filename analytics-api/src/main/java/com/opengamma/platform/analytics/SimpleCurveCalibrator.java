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
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.math.curve.DoublesCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.analytics.math.interpolation.FlatExtrapolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.basics.date.BusinessDayCalendar;
import com.opengamma.basics.date.BusinessDayConvention;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.date.Tenor;
import com.opengamma.collect.Guavate;

/**
 * Builds yield curves from a set of zero-coupon rates. This provides a
 * very straightforward way to create yield curves in order to test
 * out other aspects of analytics.
 */
public class SimpleCurveCalibrator {

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

  private final DayCount dayCount;

  private final BusinessDayConvention businessDayConvention;

  private final BusinessDayCalendar businessDayCalendar;

  public SimpleCurveCalibrator() {

    // Don't worry about holidays yet
    this(DEFAULT_INTERPOLATOR_EXTRAPOLATOR, DayCount.DC_ACT_365F, BusinessDayConvention.FOLLOWING, BusinessDayCalendar.WEEKENDS);
  }

  private SimpleCurveCalibrator(Interpolator1D interpolatorExtrapolator, DayCount dayCount,
                                BusinessDayConvention businessDayConvention,
                                BusinessDayCalendar businessDayCalendar) {
    this.interpolatorExtrapolator = interpolatorExtrapolator;
    this.dayCount = dayCount;
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
  public SimpleCurveCalibrator withInterpolation(InterpolationMethod interpolationMethod) {
    return new SimpleCurveCalibrator(buildInterpolatorExtrapolator(interpolationMethod), dayCount,
        businessDayConvention, businessDayCalendar);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * day count.
   *
   * @param dayCount  the day count to be used
   * @return a new curve calibrator
   */
  public SimpleCurveCalibrator withDayCount(DayCount dayCount) {
    return new SimpleCurveCalibrator(interpolatorExtrapolator, dayCount,
        businessDayConvention, businessDayCalendar);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * business day convention.
   *
   * @param businessDayConvention  the business day convention to be used
   * @return a new curve calibrator
   */
  public SimpleCurveCalibrator withBusinessDayConvention(BusinessDayConvention businessDayConvention) {
    return new SimpleCurveCalibrator(interpolatorExtrapolator, dayCount,
        businessDayConvention, businessDayCalendar);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * business day convention.
   *
   * @param businessDayCalendar  the business day calendar to be used
   * @return a new curve calibrator
   */
  public SimpleCurveCalibrator withBusinessDayCalendar(BusinessDayCalendar businessDayCalendar) {
    return new SimpleCurveCalibrator(interpolatorExtrapolator, dayCount,
        businessDayConvention, businessDayCalendar);
  }

  private Interpolator1D buildInterpolatorExtrapolator(InterpolationMethod interpolationMethod) {
    return new CombinedInterpolatorExtrapolator(interpolationMethod.getInterpolator(), DEFAULT_EXTRAPOLATOR);
  }

  public YieldCurve buildYieldCurve(Map<Tenor, Double> zeroCouponRates, LocalDate valuationDate) {

    // Validate we have sensible tenors and order them (or ensure they're ordered)

    LocalDate startDate = businessDayCalendar.ensure(valuationDate, businessDayConvention);


    Function<Map.Entry<Tenor, Double>, Double> keyMapper = e -> {
      Period period = e.getKey().getPeriod();
      LocalDate rateDate = businessDayCalendar.ensure(startDate.plus(period), businessDayConvention);
      return dayCount.getDayCountFraction(startDate, rateDate);
    };

    SortedMap<Double, Double> rateMap = zeroCouponRates.entrySet().stream()
        .collect(Guavate.toImmutableSortedMap(keyMapper, Map.Entry::getValue));

    DoublesCurve curve = InterpolatedDoublesCurve.fromSorted(rateMap, interpolatorExtrapolator);
    YieldAndDiscountCurve calibratedCurve =
        com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve.from(curve);

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

}

