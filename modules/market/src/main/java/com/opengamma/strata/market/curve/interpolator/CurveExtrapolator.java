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
 * Interface for extrapolators which extrapolate beyond the ends of a curve.
 */
public interface CurveExtrapolator extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CurveExtrapolator of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the extrapolator to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  public static ExtendedEnum<CurveExtrapolator> extendedEnum() {
    return CurveExtrapolators.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Binds this extrapolator to a curve.
   * <p>
   * The bind process takes the definition of the extrapolator and combines it with the x-y values.
   * This allows implementations to optimize extrapolation calculations.
   * <p>
   * This method is intended to be called from within
   * {@link CurveInterpolator#bind(DoubleArray, DoubleArray, CurveExtrapolator, CurveExtrapolator)}.
   * Callers should ensure that the interpolator instance passed in fully constructed.
   * For example, it is incorrect to call this method from a {@link BoundCurveInterpolator} constructor.
   *
   * @param xValues  the x-values of the curve, must be sorted from low to high
   * @param yValues  the y-values of the curve
   * @param interpolator  the interpolator
   * @return the bound extrapolator
   */
  public abstract BoundCurveExtrapolator bind(
      DoubleArray xValues,
      DoubleArray yValues,
      BoundCurveInterpolator interpolator);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this extrapolator.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
