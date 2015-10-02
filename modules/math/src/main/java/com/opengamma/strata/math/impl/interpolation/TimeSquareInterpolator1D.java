/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.io.Serializable;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.InterpolationBoundedValues;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * A one-dimensional interpolator.
 * The interpolation is linear on x y^2. The interpolator is used for interpolation on integrated variance for options.
 * All values of y must be positive. 
 */
@BeanDefinition(style = "light", constructorScope = "public")
public final class TimeSquareInterpolator1D
    extends Interpolator1D
    implements CurveInterpolator, ImmutableBean, Serializable {

  /* Level below which the value is consider to be 0. */
  private static final double EPS = 1.0E-10;

  /** Interpolator name. */
  private static final String NAME = "TimeSquare";

  //-------------------------------------------------------------------------
  @Override
  public Double interpolate(Interpolator1DDataBundle data, Double value) {
    JodaBeanUtils.notNull(value, "value");
    JodaBeanUtils.notNull(data, "data");
    ArgChecker.isTrue(value > 0, "Value should be stricly positive");
    InterpolationBoundedValues boundedValues = data.getBoundedValues(value);
    double x1 = boundedValues.getLowerBoundKey();
    double y1 = boundedValues.getLowerBoundValue();
    if (boundedValues.getLowerBoundIndex() == data.size() - 1) {
      return y1;
    }
    double x2 = boundedValues.getHigherBoundKey();
    double y2 = boundedValues.getHigherBoundValue();
    double w = (x2 - value) / (x2 - x1);
    double xy21 = x1 * y1 * y1;
    double xy22 = x2 * y2 * y2;
    double xy2 = w * xy21 + (1 - w) * xy22;
    return Math.sqrt(xy2 / value);
  }

  @Override
  public double firstDerivative(Interpolator1DDataBundle data, Double value) {
    JodaBeanUtils.notNull(value, "value");
    JodaBeanUtils.notNull(data, "data");
    ArgChecker.isTrue(value > 0, "Value should be stricly positive");
    int lowerIndex = data.getLowerBoundIndex(value);
    int index;
    if (lowerIndex == data.size() - 1) {
      index = data.size() - 2;
    } else {
      index = lowerIndex;
    }
    double x1 = data.getKeys()[index];
    double y1 = data.getValues()[index];
    double x2 = data.getKeys()[index + 1];
    double y2 = data.getValues()[index + 1];
    if ((y1 < EPS) || (y2 < EPS)) {
      throw new UnsupportedOperationException("node sensitivity not implemented when one node is 0 value");
    }
    double w = (x2 - value) / (x2 - x1);
    double xy21 = x1 * y1 * y1;
    double xy22 = x2 * y2 * y2;
    double xy2 = w * xy21 + (1 - w) * xy22;
    return 0.5 * (-Math.sqrt(xy2 / value) + (-xy21 + xy22) / (x2 - x1) / Math.sqrt(xy2 / value)) / value;
  }

  @Override
  public double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, Double value) {
    ArgChecker.notNull(value, "Value to be interpolated must not be null");
    ArgChecker.notNull(data, "Data bundle must not be null");
    int n = data.size();
    double[] resultSensitivity = new double[n];
    InterpolationBoundedValues boundedValues = data.getBoundedValues(value);
    double x1 = boundedValues.getLowerBoundKey();
    double y1 = boundedValues.getLowerBoundValue();
    int index = boundedValues.getLowerBoundIndex();
    if (index == n - 1) {
      resultSensitivity[n - 1] = 1.0;
      return resultSensitivity;
    }
    double x2 = boundedValues.getHigherBoundKey();
    double y2 = boundedValues.getHigherBoundValue();
    if ((y1 < EPS) || (y2 < EPS)) {
      throw new UnsupportedOperationException("node sensitivity not implemented when one node is 0 value");
    }
    double w = (x2 - value) / (x2 - x1);
    double xy21 = x1 * y1 * y1;
    double xy22 = x2 * y2 * y2;
    double xy2 = w * xy21 + (1 - w) * xy22;
    double resultValue = Math.sqrt(xy2 / value);
    double resultValueBar = 1.0;
    double xy2Bar = 0.5 / resultValue / value * resultValueBar;
    double xy21Bar = w * xy2Bar;
    double xy22Bar = (1 - w) * xy2Bar;
    double y2Bar = 2 * x2 * y2 * xy22Bar;
    double y1Bar = 2 * x1 * y1 * xy21Bar;
    resultSensitivity[index] = y1Bar;
    resultSensitivity[index + 1] = y2Bar;
    return resultSensitivity;
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(double[] x, double[] y) {
    ArgChecker.notNull(y, "y");
    int nY = y.length;
    for (int i = 0; i < nY; ++i) {
      ArgChecker.isTrue(y[i] >= 0.0, "All values in y must be positive");
    }
    return new ArrayInterpolator1DDataBundle(x, y);
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(double[] x, double[] y) {
    ArgChecker.notNull(y, "y");
    int nY = y.length;
    for (int i = 0; i < nY; ++i) {
      ArgChecker.isTrue(y[i] >= 0.0, "All values in y must be positive");
    }
    return new ArrayInterpolator1DDataBundle(x, y, true);
  }

  @Override
  public String getName() {
    return NAME;
  }
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code TimeSquareInterpolator1D}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(TimeSquareInterpolator1D.class);

  /**
   * The meta-bean for {@code TimeSquareInterpolator1D}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   */
  public TimeSquareInterpolator1D() {
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
    buf.append("TimeSquareInterpolator1D{");
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
