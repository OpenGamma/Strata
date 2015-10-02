/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.io.Serializable;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DLogPiecewisePoynomialDataBundle;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * Find a interpolant F(x) = exp( f(x) ) where f(x) is a Natural cubic spline with Monotonicity cubic fileter. 
 * 
 * The natural cubic spline is determined by {@link LogNaturalSplineHelper}, where the tridiagonal
 * algorithm is used to solve a linear system. Since {@link PiecewisePolynomialResultsWithSensitivity}
 * in {@link Interpolator1DLogPiecewisePoynomialDataBundle} contains information on f(x) (NOT F(x)), 
 * computation done by {@link PiecewisePolynomialWithSensitivityFunction1D} MUST be exponentiated.
 */
@BeanDefinition(style = "light")
public final class LogNaturalCubicMonotonicityPreservingInterpolator1D
    extends PiecewisePolynomialInterpolator1D
    implements CurveInterpolator, ImmutableBean, Serializable {

  private static final PiecewisePolynomialWithSensitivityFunction1D FUNC = new PiecewisePolynomialWithSensitivityFunction1D();

  /** The interpolator name. */
  private static final String NAME = "LogNaturalCubicWithMonotonicity";

  /**
   * Creates an instance.
   */
  @ImmutableConstructor
  public LogNaturalCubicMonotonicityPreservingInterpolator1D() {
    super(new MonotonicityPreservingCubicSplineInterpolator(new LogNaturalSplineHelper()));
  }

  //-------------------------------------------------------------------------
  @Override
  public Double interpolate(Interpolator1DDataBundle data, Double value) {
    JodaBeanUtils.notNull(value, "value");
    JodaBeanUtils.notNull(data, "data");
    ArgChecker.isTrue(data instanceof Interpolator1DLogPiecewisePoynomialDataBundle);
    Interpolator1DLogPiecewisePoynomialDataBundle polyData = (Interpolator1DLogPiecewisePoynomialDataBundle) data;
    DoubleMatrix1D res = FUNC.evaluate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    return Math.exp(res.getEntry(0));
  }

  @Override
  public double firstDerivative(Interpolator1DDataBundle data, Double value) {
    JodaBeanUtils.notNull(value, "value");
    JodaBeanUtils.notNull(data, "data");
    ArgChecker.isTrue(data instanceof Interpolator1DLogPiecewisePoynomialDataBundle);
    Interpolator1DLogPiecewisePoynomialDataBundle polyData = (Interpolator1DLogPiecewisePoynomialDataBundle) data;
    DoubleMatrix1D resValue = FUNC.evaluate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    DoubleMatrix1D resDerivative = FUNC.differentiate(
        polyData.getPiecewisePolynomialResultsWithSensitivity(),
        value);
    return Math.exp(resValue.getEntry(0)) * resDerivative.getEntry(0);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, Double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DLogPiecewisePoynomialDataBundle);
    Interpolator1DLogPiecewisePoynomialDataBundle polyData = (Interpolator1DLogPiecewisePoynomialDataBundle) data;
    double[] resSense = FUNC.nodeSensitivity(polyData.getPiecewisePolynomialResultsWithSensitivity(), value).getData();
    double resValue = Math.exp(FUNC.evaluate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value).getEntry(0));
    double[] knotValues = data.getValues();
    int nKnots = knotValues.length;
    double[] res = new double[nKnots];
    for (int i = 0; i < nKnots; ++i) {
      res[i] = resSense[i] * resValue / knotValues[i];
    }
    return res;
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(double[] x, double[] y) {
    ArgChecker.notNull(y, "y");
    int nData = y.length;
    double[] logY = new double[nData];
    for (int i = 0; i < nData; ++i) {
      ArgChecker.isTrue(y[i] > 0., "y should be positive");
      logY[i] = Math.log(y[i]);
    }
    return new Interpolator1DLogPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, logY, false), new MonotonicityPreservingCubicSplineInterpolator(new LogNaturalSplineHelper()));
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(double[] x, double[] y) {
    ArgChecker.notNull(y, "y");
    int nData = y.length;
    double[] logY = new double[nData];
    for (int i = 0; i < nData; ++i) {
      ArgChecker.isTrue(y[i] > 0., "y should be positive");
      logY[i] = Math.log(y[i]);
    }
    return new Interpolator1DLogPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, logY, true), new MonotonicityPreservingCubicSplineInterpolator(new LogNaturalSplineHelper()));
  }

  @Override
  public String getName() {
    return NAME;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code LogNaturalCubicMonotonicityPreservingInterpolator1D}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(LogNaturalCubicMonotonicityPreservingInterpolator1D.class);

  /**
   * The meta-bean for {@code LogNaturalCubicMonotonicityPreservingInterpolator1D}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(32);
    buf.append("LogNaturalCubicMonotonicityPreservingInterpolator1D{");
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
