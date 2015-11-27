/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

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
   * Obtains a {@code CurveInterpolator} from a unique name.
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
   * This helper allows instances of {@code CurveInterpolator} to be lookup up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  public static ExtendedEnum<CurveInterpolator> extendedEnum() {
    return CurveInterpolators.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Binds this interpolator to a curve specifying the extrapolators to use.
   * <p>
   * The bind process takes the definition of the interpolator and combines it with the x-y values.
   * This allows implementations to optimize interpolation calculations.
   *
   * @param xValues  the x-values
   * @param yValues  the y-values
   * @param extrapolatorLeft  the extrapolator for x-values on the left
   * @param extrapolatorRight  the extrapolator for x-values on the right
   * @return the bound interpolator
   */
  public abstract BoundCurveInterpolator bind(
      DoubleArray xValues,
      DoubleArray yValues,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight);

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
