/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.interpolation.Extrapolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * The standard immutable curve extrapolator implementation based on strata-math.
 */
final class StandardCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The name.
   */
  private final String name;
  /**
   * The underlying extrapolator.
   */
  private transient final Extrapolator1D underlying;

  /**
   * Creates an instance.
   * 
   * @param name  the name of the extrapolator
   * @param underlying  the underlying extrapolator
   */
  StandardCurveExtrapolator(String name, Extrapolator1D underlying) {
    this.name = name;
    this.underlying = underlying;
  }

  // resolve instance using name
  private Object readResolve() {
    return CurveExtrapolator.of(name);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name.
   * 
   * @return the name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Gets the underlying extrapolator.
   * 
   * @return the underlying extrapolator
   */
  Extrapolator1D getUnderlying() {
    return underlying;
  }

  //-------------------------------------------------------------------------
  @Override
  public BoundCurveExtrapolator bind(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
    ArgChecker.isTrue(interpolator instanceof StandardBoundCurveInterpolator,
        "Interpolator must be StandardBoundCurveInterpolator");
    Interpolator1D interp = ((StandardBoundCurveInterpolator) interpolator).getInterpolator();
    Interpolator1DDataBundle data = ((StandardBoundCurveInterpolator) interpolator).getDataBundle();
    return new Bound(interp, data, underlying);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      StandardCurveExtrapolator other = (StandardCurveExtrapolator) obj;
      return name.equals(other.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

  //-------------------------------------------------------------------------
  /**
   * Bound extrapolator.
   */
  static class Bound implements BoundCurveExtrapolator {
    private final Interpolator1D underlyingInterpolator;
    private final Interpolator1DDataBundle underlyingData;
    private final Extrapolator1D underlyingExtrapolator;

    Bound(Interpolator1D interpolator, Interpolator1DDataBundle data, Extrapolator1D extrapolator) {
      this.underlyingInterpolator = interpolator;
      this.underlyingData = data;
      this.underlyingExtrapolator = extrapolator;
    }

    @Override
    public double leftExtrapolate(double xValue) {
      return underlyingExtrapolator.extrapolate(underlyingData, xValue, underlyingInterpolator);
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      return underlyingExtrapolator.firstDerivative(underlyingData, xValue, underlyingInterpolator);
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      double[] sens = underlyingExtrapolator.getNodeSensitivitiesForValue(underlyingData, xValue, underlyingInterpolator);
      return DoubleArray.ofUnsafe(sens);
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      return underlyingExtrapolator.extrapolate(underlyingData, xValue, underlyingInterpolator);
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      return underlyingExtrapolator.firstDerivative(underlyingData, xValue, underlyingInterpolator);
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      double[] sens = underlyingExtrapolator.getNodeSensitivitiesForValue(underlyingData, xValue, underlyingInterpolator);
      return DoubleArray.ofUnsafe(sens);
    }
  }

}
