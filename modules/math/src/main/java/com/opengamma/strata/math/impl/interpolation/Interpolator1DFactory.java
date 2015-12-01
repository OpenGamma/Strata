/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public final class Interpolator1DFactory {
  /** Linear */
  public static final String LINEAR = "Linear";
  /** Exponential */
  public static final String EXPONENTIAL = "Exponential";
  /** Log-linear */
  public static final String LOG_LINEAR = "LogLinear";
  /** Natural cubic spline */
  public static final String NATURAL_CUBIC_SPLINE = "NaturalCubicSpline";
  /** Barycentric rational function */
  public static final String BARYCENTRIC_RATIONAL_FUNCTION = "BarycentricRationalFunction";
  /** Polynomial */
  public static final String POLYNOMIAL = "Polynomial";
  /** Rational function */
  public static final String RATIONAL_FUNCTION = "RationalFunction";
  /** Step */
  public static final String STEP = "Step";
  /** Step with the value in the interval equal to the value at the upper bound */
  public static final String STEP_UPPER = "StepUpper";
  /** Double quadratic */
  public static final String DOUBLE_QUADRATIC = "DoubleQuadratic";
  /**Monotonicity-Preserving-Cubic-Spline
   * @deprecated Use the name PCHIP instead 
   * */
  @Deprecated
  public static final String MONOTONIC_CUBIC = "MonotonicityPreservingCubicSpline";
  /**Piecewise Cubic Hermite Interpolating Polynomial (PCHIP)*/
  public static final String PCHIP = "PCHIP";
  /** Time square */
  public static final String TIME_SQUARE = "TimeSquare";
  /** Flat extrapolator */
  public static final String FLAT_EXTRAPOLATOR = FlatExtrapolator1D.NAME;
  /** Linear extrapolator */
  public static final String LINEAR_EXTRAPOLATOR = LinearExtrapolator1D.NAME;
  /** Log-linear extrapolator */
  public static final String LOG_LINEAR_EXTRAPOLATOR = LogLinearExtrapolator1D.NAME;
  /** Quadratic polynomial left extrapolator */
  public static final String QUADRATIC_LEFT_EXTRAPOLATOR = QuadraticPolynomialLeftExtrapolator.NAME;
  /** Linear extrapolator */
  public static final String EXPONENTIAL_EXTRAPOLATOR = ExponentialExtrapolator1D.NAME;
  /** Product polynomial extrapolator */
  public static final String PRODUCT_POLYNOMIAL_EXTRAPOLATOR = ProductPolynomialExtrapolator1D.NAME;
  /** Reciprocal extrapolator */
  public static final String RECIPROCAL_EXTRAPOLATOR = ReciprocalExtrapolator1D.NAME;
  /** Extrapolator that does no extrapolation itself and always delegates to the interpolator*/
  public static final String INTERPOLATOR_EXTRAPOLATOR = InterpolatorExtrapolator.NAME;
  /** ISDA interpolator */
  public static final String ISDA_INTERPOLATOR = "ISDAInterpolator";
  /** ISDA extrapolator */
  public static final String ISDA_EXTRAPOLATOR = "ISDAExtrapolator";
  /** Linear instance */
  public static final LinearInterpolator1D LINEAR_INSTANCE = new LinearInterpolator1D();
  /** Exponential instance */
  public static final ExponentialInterpolator1D EXPONENTIAL_INSTANCE = new ExponentialInterpolator1D();
  /** Log-linear instance */
  public static final LogLinearInterpolator1D LOG_LINEAR_INSTANCE = new LogLinearInterpolator1D();
  /** Natural cubic spline instance */
  public static final NaturalCubicSplineInterpolator1D NATURAL_CUBIC_SPLINE_INSTANCE = new NaturalCubicSplineInterpolator1D();
  /** Step instance */
  public static final StepInterpolator1D STEP_INSTANCE = new StepInterpolator1D();
  /** Step-Upper instance */
  public static final StepUpperInterpolator1D STEP_UPPER_INSTANCE = new StepUpperInterpolator1D();
  /** Double quadratic instance */
  public static final DoubleQuadraticInterpolator1D DOUBLE_QUADRATIC_INSTANCE = new DoubleQuadraticInterpolator1D();
  /** MonotonicityPreservingCubicSpline
   * @deprecated use PCHIP_INSTANCE instead 
   * */
  @Deprecated
  public static final PCHIPInterpolator1D MONOTONIC_CUBIC_INSTANCE = new PCHIPInterpolator1D();
  /**Piecewise Cubic Hermite Interpolating Polynomial (PCHIP)*/
  public static final PCHIPInterpolator1D PCHIP_INSTANCE = new PCHIPInterpolator1D();
  /** Time square instance */
  public static final TimeSquareInterpolator1D TIME_SQUARE_INSTANCE = new TimeSquareInterpolator1D();
  /** Flat extrapolator instance */
  public static final FlatExtrapolator1D FLAT_EXTRAPOLATOR_INSTANCE = new FlatExtrapolator1D();
  /** Linear extrapolator instance */
  public static final LinearExtrapolator1D LINEAR_EXTRAPOLATOR_INSTANCE = new LinearExtrapolator1D();
  /** Log linear extrapolator instance */
  public static final LogLinearExtrapolator1D LOG_LINEAR_EXTRAPOLATOR_INSTANCE = new LogLinearExtrapolator1D();
  /** Exponential extrapolator instance */
  public static final ExponentialExtrapolator1D EXPONENTIAL_EXTRAPOLATOR_INSTANCE = new ExponentialExtrapolator1D();

  /**Cubic spline with clamped endpoint conditions*/
  public static final String CLAMPED_CUBIC = "ClampedCubicSpline";
  /**Instance of cubic spline with clamped endpoint conditions*/
  public static final ClampedCubicSplineInterpolator1D CLAMPED_CUBIC_INSTANCE = new ClampedCubicSplineInterpolator1D();
  /**Cubic spline with clamped endpoint conditions and nonnegativity filter*/
  public static final String CLAMPED_CUBIC_NONNEGATIVE = "ClampedCubicSplineWithNonnegativity";
  /**Instance of cubic spline with clamped endpoint conditions and nonnegativity filter*/
  public static final NonnegativityPreservingCubicSplineInterpolator1D CLAMPED_CUBIC_NONNEGATIVE_INSTANCE = new NonnegativityPreservingCubicSplineInterpolator1D(new CubicSplineInterpolator());

  public static final String NATURAL_CUBIC_NONNEGATIVE = "NaturalCubicSplineWithNonnegativity";
  /**Instance of cubic spline with clamped endpoint conditions and nonnegativity filter*/
  public static final NonnegativityPreservingCubicSplineInterpolator1D NATURAL_CUBIC_NONNEGATIVE_INSTANCE = new NonnegativityPreservingCubicSplineInterpolator1D(new NaturalSplineInterpolator());

  /**Cubic spline with not-a-knot endpoint conditions and nonnegativity filter*/
  public static final String NOTAKNOT_CUBIC_NONNEGATIVE = "NotAKnotCubicSplineWithNonnegativity";
  /**Instance of quintic spline with not-a-knot endpoint conditions and nonnegativity filter*/
  public static final NonnegativityPreservingCubicSplineInterpolator1D NOTAKNOT_CUBIC_NONNEGATIVE_INSTANCE = new NonnegativityPreservingCubicSplineInterpolator1D(new CubicSplineInterpolator());

  /**Constrained cubic interpolation*/
  public static final String CONSTRAINED_CUBIC = "ConstrainedCubicSpline";
  /**Instance of constrained cubic interpolation*/
  public static final ConstrainedCubicSplineInterpolator1D CONSTRAINED_CUBIC_INSTANCE = new ConstrainedCubicSplineInterpolator1D();
  /**Constrained cubic interpolation with nonnegativity filter*/
  public static final String CONSTRAINED_CUBIC_NONNEGATIVE = "ConstrainedCubicSplineWithNonnegativity";
  /**Instance of constrained cubic interpolation with nonnegativity filter*/
  public static final NonnegativityPreservingCubicSplineInterpolator1D CONSTRAINED_CUBIC_NONNEGATIVE_INSTANCE = new NonnegativityPreservingCubicSplineInterpolator1D(
      new ConstrainedCubicSplineInterpolator());

  /**Akima cubic interpolation*/
  public static final String AKIMA_CUBIC = "AkimaCubicSpline";
  /**Instance of Akima cubic interpolation*/
  public static final SemiLocalCubicSplineInterpolator1D AKIMA_CUBIC_INSTANCE = new SemiLocalCubicSplineInterpolator1D();
  /**Akima cubic interpolation with nonnegativity filter*/
  public static final String AKIMA_CUBIC_NONNEGATIVE = "AkimaCubicSplineWithNonnegativity";
  /**Instance of Akima cubic interpolation with nonnegativity filter*/
  public static final NonnegativityPreservingCubicSplineInterpolator1D AKIMA_CUBIC_NONNEGATIVE_INSTANCE = new NonnegativityPreservingCubicSplineInterpolator1D(new SemiLocalCubicSplineInterpolator());

  /**Log natural cubic interpolation with monotonicity filter*/
  public static final String LOG_NATURAL_CUBIC_MONOTONE = "LogNaturalCubicWithMonotonicity";
  /**Instance of log natural cubic interpolation with monotonicity filter*/
  public static final LogNaturalCubicMonotonicityPreservingInterpolator1D LOG_NATURAL_CUBIC_MONOTONE_INSTANCE = new LogNaturalCubicMonotonicityPreservingInterpolator1D();

  /**Square linear interpolation*/
  public static final String SQUARE_LINEAR = "SquareLinear";
  /**Instance of square linear interpolation*/
  public static final SquareLinearInterpolator1D SQUARE_LINEAR_INSTANCE = new SquareLinearInterpolator1D();

  private static final Map<String, Interpolator1D> s_staticInstances;
  private static final Map<Class<?>, String> s_instanceNames;
  private static final Map<String, Extrapolator1D> extrapolators;

  static {
    Map<String, Interpolator1D> staticInstances = new HashMap<>();
    Map<Class<?>, String> instanceNames = new HashMap<>();
    Map<String, Extrapolator1D> extrapolatorMap = new HashMap<>();

    staticInstances.put(LINEAR, LINEAR_INSTANCE);
    instanceNames.put(LinearInterpolator1D.class, LINEAR);
    staticInstances.put(EXPONENTIAL, EXPONENTIAL_INSTANCE);
    instanceNames.put(ExponentialInterpolator1D.class, EXPONENTIAL);
    staticInstances.put(LOG_LINEAR, LOG_LINEAR_INSTANCE);
    instanceNames.put(LogLinearInterpolator1D.class, LOG_LINEAR);
    staticInstances.put(NATURAL_CUBIC_SPLINE, NATURAL_CUBIC_SPLINE_INSTANCE);
    instanceNames.put(NaturalCubicSplineInterpolator1D.class, NATURAL_CUBIC_SPLINE);
    staticInstances.put(STEP, STEP_INSTANCE);
    instanceNames.put(StepInterpolator1D.class, STEP);
    staticInstances.put(STEP_UPPER, STEP_UPPER_INSTANCE);
    instanceNames.put(StepUpperInterpolator1D.class, STEP_UPPER);
    staticInstances.put(DOUBLE_QUADRATIC, DOUBLE_QUADRATIC_INSTANCE);
    instanceNames.put(DoubleQuadraticInterpolator1D.class, DOUBLE_QUADRATIC);
    staticInstances.put(MONOTONIC_CUBIC, MONOTONIC_CUBIC_INSTANCE);
    instanceNames.put(PCHIPInterpolator1D.class, MONOTONIC_CUBIC);
    staticInstances.put(PCHIP, PCHIP_INSTANCE);
    instanceNames.put(PCHIPInterpolator1D.class, PCHIP);
    staticInstances.put(TIME_SQUARE, TIME_SQUARE_INSTANCE);
    instanceNames.put(TimeSquareInterpolator1D.class, TIME_SQUARE);

    staticInstances.put(CLAMPED_CUBIC, CLAMPED_CUBIC_INSTANCE);
    instanceNames.put(ClampedCubicSplineInterpolator1D.class, CLAMPED_CUBIC);
    staticInstances.put(CLAMPED_CUBIC_NONNEGATIVE, CLAMPED_CUBIC_NONNEGATIVE_INSTANCE);
    instanceNames.put(NonnegativityPreservingCubicSplineInterpolator1D.class, CLAMPED_CUBIC_NONNEGATIVE);

    staticInstances.put(NATURAL_CUBIC_NONNEGATIVE, NATURAL_CUBIC_NONNEGATIVE_INSTANCE);
    instanceNames.put(NonnegativityPreservingCubicSplineInterpolator1D.class, NATURAL_CUBIC_NONNEGATIVE);

    staticInstances.put(NOTAKNOT_CUBIC_NONNEGATIVE, NOTAKNOT_CUBIC_NONNEGATIVE_INSTANCE);
    instanceNames.put(NonnegativityPreservingCubicSplineInterpolator1D.class, NOTAKNOT_CUBIC_NONNEGATIVE);

    staticInstances.put(CONSTRAINED_CUBIC, CONSTRAINED_CUBIC_INSTANCE);
    instanceNames.put(ConstrainedCubicSplineInterpolator1D.class, CONSTRAINED_CUBIC);
    staticInstances.put(CONSTRAINED_CUBIC_NONNEGATIVE, CONSTRAINED_CUBIC_NONNEGATIVE_INSTANCE);
    instanceNames.put(NonnegativityPreservingCubicSplineInterpolator1D.class, CONSTRAINED_CUBIC_NONNEGATIVE);

    staticInstances.put(AKIMA_CUBIC, AKIMA_CUBIC_INSTANCE);
    instanceNames.put(SemiLocalCubicSplineInterpolator1D.class, AKIMA_CUBIC);
    staticInstances.put(AKIMA_CUBIC_NONNEGATIVE, AKIMA_CUBIC_NONNEGATIVE_INSTANCE);
    instanceNames.put(NonnegativityPreservingCubicSplineInterpolator1D.class, AKIMA_CUBIC_NONNEGATIVE);

    staticInstances.put(LOG_NATURAL_CUBIC_MONOTONE, LOG_NATURAL_CUBIC_MONOTONE_INSTANCE);
    instanceNames.put(LogNaturalCubicMonotonicityPreservingInterpolator1D.class, LOG_NATURAL_CUBIC_MONOTONE);

    staticInstances.put(SQUARE_LINEAR, SQUARE_LINEAR_INSTANCE);
    instanceNames.put(SquareLinearInterpolator1D.class, SQUARE_LINEAR);

    extrapolatorMap.put(LinearExtrapolator1D.NAME, new LinearExtrapolator1D());
    extrapolatorMap.put(LogLinearExtrapolator1D.NAME, new LogLinearExtrapolator1D());
    extrapolatorMap.put(QuadraticPolynomialLeftExtrapolator.NAME, new QuadraticPolynomialLeftExtrapolator());
    extrapolatorMap.put(ProductPolynomialExtrapolator1D.NAME, new ProductPolynomialExtrapolator1D());
    extrapolatorMap.put(ReciprocalExtrapolator1D.NAME, new ReciprocalExtrapolator1D());
    extrapolatorMap.put(FlatExtrapolator1D.NAME, new FlatExtrapolator1D());
    extrapolatorMap.put(InterpolatorExtrapolator.NAME, new InterpolatorExtrapolator());

    s_staticInstances = new HashMap<>(staticInstances);
    s_instanceNames = new HashMap<>(instanceNames);
    extrapolators = extrapolatorMap;
  }

  private Interpolator1DFactory() {
  }

  public static Interpolator1D getInterpolator(final String interpolatorName) {
    final Interpolator1D interpolator = s_staticInstances.get(interpolatorName);
    if (interpolator != null) {
      return interpolator;
    }
    // TODO Deal with degree for Barycentric, Polynomial, and RationalFunction
    throw new IllegalArgumentException("Interpolator not handled: " + interpolatorName);
  }

  public static String getInterpolatorName(final Interpolator1D interpolator) {
    if (interpolator == null) {
      return null;
    }
    return s_instanceNames.get(interpolator.getClass());
  }

  public static Interpolator1D getCurveInterpolator(final String interpolatorName) {
    Interpolator1D interpolator = s_staticInstances.get(interpolatorName);
    if (interpolator != null) {
      return (Interpolator1D) interpolator;
    }
    throw new IllegalArgumentException("Unknown interpolator: " + interpolatorName);
  }

  public static Extrapolator1D getCurveExtrapolator(final String extrapolatorName) {
    Extrapolator1D extrapolator = extrapolators.get(extrapolatorName);
    if (extrapolator != null) {
      return extrapolator;
    }
    throw new IllegalArgumentException("Unknown extrapolator: " + extrapolatorName);
  }

}
