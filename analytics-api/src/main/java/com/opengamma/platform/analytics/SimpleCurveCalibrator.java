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
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.BusinessDayConventions;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.date.DayCounts;
import com.opengamma.basics.date.HolidayCalendars;
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

  /**
   * The interpolator/extrapolator to be used for calibrating the curve.
   */
  private final Interpolator1D interpolatorExtrapolator;

  /**
   * The day count to be used when converting to year fractions.
   */
  private final DayCount dayCount;

  /**
   * The business day adjustment to be performed.
   */
  private final BusinessDayAdjustment businessDayAdjustment;

  /**
   * Creates a curve calibrator with default values for day count, interpolation
   * and business day adjustments. To change the default values use the with...
   * methods.
   */
  public SimpleCurveCalibrator() {

    // TODO -when available use inbuilt holiday information
    this(DEFAULT_INTERPOLATOR_EXTRAPOLATOR, DayCounts.ACT_365,
        BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN));
  }

  private SimpleCurveCalibrator(Interpolator1D interpolatorExtrapolator, DayCount dayCount,
                                BusinessDayAdjustment businessDayAdjustment) {
    this.interpolatorExtrapolator = interpolatorExtrapolator;
    this.dayCount = dayCount;
    this.businessDayAdjustment = businessDayAdjustment;
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * interpolation method.
   *
   * @param interpolationMethod  the interpolation method to be used
   * @return a new curve calibrator
   */
  public SimpleCurveCalibrator withInterpolation(InterpolationMethod interpolationMethod) {
    return new SimpleCurveCalibrator(
        buildInterpolatorExtrapolator(interpolationMethod),
        dayCount, businessDayAdjustment);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * day count.
   *
   * @param dayCount  the day count to be used
   * @return a new curve calibrator
   */
  public SimpleCurveCalibrator withDayCount(DayCount dayCount) {
    return new SimpleCurveCalibrator(interpolatorExtrapolator, dayCount, businessDayAdjustment);
  }

  /**
   * Create a copy of this curve calibrator with the specified
   * business day convention.
   *
   * @param businessDayAdjustment  the business day adjustment to be used
   * @return a new curve calibrator
   */
  public SimpleCurveCalibrator withBusinessDayConvention(BusinessDayAdjustment businessDayAdjustment) {
    return new SimpleCurveCalibrator(interpolatorExtrapolator, dayCount, businessDayAdjustment);
  }

  private Interpolator1D buildInterpolatorExtrapolator(InterpolationMethod interpolationMethod) {
    return new CombinedInterpolatorExtrapolator(interpolationMethod.getInterpolator(), DEFAULT_EXTRAPOLATOR);
  }

  /**
   * Build a yield curve using the provided zero coupon rates and valuation date.
   *
   * @param zeroCouponRates  map of zero coupon rates to be used
   * @param valuationDate  the valuation date for the curve
   * @return a calibrated yield curve
   */
  public YieldCurve buildYieldCurve(Map<Tenor, Double> zeroCouponRates, LocalDate valuationDate) {

    // TODO - Validate we have sensible tenors and order them (or ensure they're ordered)

    LocalDate startDate = businessDayAdjustment.adjust(valuationDate);

    Function<Map.Entry<Tenor, Double>, Double> keyMapper = e -> {
      Period period = e.getKey().getPeriod();
      LocalDate rateDate = businessDayAdjustment.adjust(startDate.plus(period));
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

