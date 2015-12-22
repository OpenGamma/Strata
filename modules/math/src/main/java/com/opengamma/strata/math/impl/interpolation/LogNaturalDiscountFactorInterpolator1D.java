/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DLogPiecewisePoynomialDataBundle;

/**
 * Find a interpolant F(x) = exp( f(x) ) where f(x) is a Natural cubic spline {@link NaturalSplineInterpolator} and 
 * F(0) = 1 is satisified.
 * <p>
 * The natural end point condition is satisfied at the rightmost node point and x=0 rather than the leftmost node point. 
 */
@BeanDefinition(style = "light")
public final class LogNaturalDiscountFactorInterpolator1D extends
    PiecewisePolynomialInterpolator1D
    implements ImmutableBean {

  private static final PiecewisePolynomialWithSensitivityFunction1D FUNC = new PiecewisePolynomialWithSensitivityFunction1D();

  /**
   * Default constructor.
   */
  @ImmutableConstructor
  public LogNaturalDiscountFactorInterpolator1D() {
    super(new ClampedPiecewisePolynomialInterpolator(new NaturalSplineInterpolator(), new double[] {0d }, new double[] {0d }));
  }

  @Override
  public double interpolate(Interpolator1DDataBundle data, double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DLogPiecewisePoynomialDataBundle);
    Interpolator1DLogPiecewisePoynomialDataBundle polyData = (Interpolator1DLogPiecewisePoynomialDataBundle) data;
    DoubleArray res = FUNC.evaluate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    return Math.exp(res.get(0));
  }

  @Override
  public double firstDerivative( Interpolator1DDataBundle data, double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DLogPiecewisePoynomialDataBundle);
    Interpolator1DLogPiecewisePoynomialDataBundle polyData = (Interpolator1DLogPiecewisePoynomialDataBundle) data;
    DoubleArray resValue = FUNC.evaluate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    DoubleArray resDerivative = FUNC.differentiate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    return Math.exp(resValue.get(0)) * resDerivative.get(0);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DLogPiecewisePoynomialDataBundle);
    final Interpolator1DLogPiecewisePoynomialDataBundle polyData = (Interpolator1DLogPiecewisePoynomialDataBundle) data;
    double[] resSense = FUNC.nodeSensitivity(polyData.getPiecewisePolynomialResultsWithSensitivity(), value).toArray();
    double resValue = Math.exp(FUNC.evaluate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value).get(0));
    final double[] knotValues = data.getValues();
    final int nKnots = knotValues.length;
    final double[] res = new double[nKnots];
    for (int i = 0; i < nKnots; ++i) {
      res[i] = resSense[i + 1] * resValue / knotValues[i];
    }
    return res;
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(final double[] x, final double[] y) {
    ArgChecker.notNull(y, "y");
    final int nData = y.length;
    final double[] logY = new double[nData];
    for (int i = 0; i < nData; ++i) {
      ArgChecker.isTrue(x[i] > 0., "x should be positive");
      ArgChecker.isTrue(y[i] > 0., "y should be positive");
      logY[i] = Math.log(y[i]);
    }
    return new Interpolator1DLogPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, logY, false),
        getInterpolator());
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(final double[] x, final double[] y) {
    ArgChecker.notNull(y, "y");
    final int nData = y.length;
    final double[] logY = new double[nData];
    for (int i = 0; i < nData; ++i) {
      ArgChecker.isTrue(x[i] > 0., "x should be positive");
      ArgChecker.isTrue(y[i] > 0., "y should be positive");
      logY[i] = Math.log(y[i]);
    }
    return new Interpolator1DLogPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, logY, true),
        getInterpolator());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code LogNaturalDiscountFactorInterpolator1D}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(LogNaturalDiscountFactorInterpolator1D.class);

  /**
   * The meta-bean for {@code LogNaturalDiscountFactorInterpolator1D}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

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
    buf.append("LogNaturalDiscountFactorInterpolator1D{");
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
