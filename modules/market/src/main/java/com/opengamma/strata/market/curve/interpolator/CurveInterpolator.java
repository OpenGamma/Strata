/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * Interface for interpolators that interpolate between points on a curve.
 */
public interface CurveInterpolator extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CurveInterpolator of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the interpolator to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  public static ExtendedEnum<CurveInterpolator> extendedEnum() {
    return CurveInterpolators.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Binds this interpolator to a curve where no extrapolation is permitted.
   * <p>
   * The bound interpolator provides methods to interpolate the y-value for a x-value.
   * If an attempt is made to interpolate an x-value outside the range defined by
   * the first and last nodes, an exception will be thrown.
   * <p>
   * The bind process takes the definition of the interpolator and combines it with the x-y values.
   * This allows implementations to optimize interpolation calculations.
   *
   * @param xValues  the x-values of the curve, must be sorted from low to high
   * @param yValues  the y-values of the curve
   * @return the bound interpolator
   */
  public abstract BoundCurveInterpolator bind(DoubleArray xValues, DoubleArray yValues);

  /**
   * Binds this interpolator to a curve specifying the extrapolators to use.
   * <p>
   * The bound interpolator provides methods to interpolate the y-value for a x-value.
   * If an attempt is made to interpolate an x-value outside the range defined by
   * the first and last nodes, the appropriate extrapolator will be used.
   * <p>
   * The bind process takes the definition of the interpolator and combines it with the x-y values.
   * This allows implementations to optimize interpolation calculations.
   *
   * @param xValues  the x-values of the curve, must be sorted from low to high
   * @param yValues  the y-values of the curve
   * @param extrapolatorLeft  the extrapolator for x-values on the left
   * @param extrapolatorRight  the extrapolator for x-values on the right
   * @return the bound interpolator
   */
  public default BoundCurveInterpolator bind(
      DoubleArray xValues,
      DoubleArray yValues,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight) {

    // interpolators depend on extrapolators and vice versa
    // this makes it hard to satisfy the Java Memory Model for immutability
    // handle this by creating an interpolator instance that cannot extrapolate
    // use that interpolator to bind the extrapolators
    // finally, create the bound interpolator for the caller
    BoundCurveInterpolator interpolatorOnly = bind(xValues, yValues);
    BoundCurveExtrapolator boundLeft = extrapolatorLeft.bind(xValues, yValues, interpolatorOnly);
    BoundCurveExtrapolator boundRight = extrapolatorRight.bind(xValues, yValues, interpolatorOnly);
    return interpolatorOnly.bind(boundLeft, boundRight);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this interpolator.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
