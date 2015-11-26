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
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.interpolator.CurveExtrapolator;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * This left extrapolator is designed for extrapolating a discount factor where the
 * trivial point (0.,1.) is NOT involved in the data.
 * The extrapolation is completed by applying a quadratic extrapolant on the discount
 * factor (not log of the discount factor), where the point (0.,1.) is inserted and
 * the first derivative value is assumed to be continuous at firstKey.
 */
@BeanDefinition(style = "light", constructorScope = "public")
public final class QuadraticPolynomialLeftExtrapolator
    implements CurveExtrapolator, Extrapolator1D, ImmutableBean, Serializable {

  /** The extrapolator name. */
  public static final String NAME = "QuadraticLeft";

  /**
   * The bump parameter of finite difference approximation for the first derivative value.
   */
  @PropertyDefinition
  private final double eps;

  /**
   * Creates an instance.
   */
  public QuadraticPolynomialLeftExtrapolator() {
    this(1e-8);
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public double extrapolate(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    if (data.firstKey() == 0.) {
      throw new IllegalArgumentException("The trivial point at key=0. is already included");
    }
    if (value < data.firstKey()) {
      return leftExtrapolate(data, value, interpolator);
    } else if (value > data.lastKey()) {
      throw new IllegalArgumentException("Value " + value + " was greater than data range");
    }
    throw new IllegalArgumentException("Value " + value + " was within data range");
  }

  @Override
  public double firstDerivative(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    if (data.firstKey() == 0.) {
      throw new IllegalArgumentException("The trivial point at key=0. is already included");
    }
    if (value < data.firstKey()) {
      return leftExtrapolateDerivative(data, value, interpolator);
    } else if (value > data.lastKey()) {
      throw new IllegalArgumentException("Value " + value + " was greater than data range");
    }
    throw new IllegalArgumentException("Value " + value + " was within data range");
  }

  @Override
  public double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    if (data.firstKey() == 0.) {
      throw new IllegalArgumentException("The trivial point at key=0. is already included");
    }
    if (value < data.firstKey()) {
      return getLeftSensitivities(data, value, interpolator);
    } else if (value > data.lastKey()) {
      throw new IllegalArgumentException("Value " + value + " was greater than data range");
    }
    throw new IllegalArgumentException("Value " + value + " was within data range");
  }

  private double leftExtrapolate(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    double x = data.firstKey();
    double y = data.firstValue();
    double m = interpolator.firstDerivative(data, x);
    double quadCoef = m / x - (y - 1.) / x / x;
    double linCoef = -m + 2. * (y - 1.) / x;
    return quadCoef * value * value + linCoef * value + 1.;
  }

  private double leftExtrapolateDerivative(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    double x = data.firstKey();
    double y = data.firstValue();
    double m = interpolator.firstDerivative(data, x);
    double quadCoef = m / x - (y - 1.) / x / x;
    double linCoef = -m + 2. * (y - 1.) / x;
    return 2. * quadCoef * value + linCoef;
  }

  private double[] getLeftSensitivities(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    double eps = this.eps * (data.lastKey() - data.firstKey());
    double x = data.firstKey();
    double[] result = interpolator.getNodeSensitivitiesForValue(data, x + eps);

    int n = result.length;
    for (int i = 1; i < n; i++) {
      double tmp = result[i] * value / eps;
      result[i] = tmp / x * value - tmp;
    }
    double tmp = (result[0] - 1.) / eps;
    result[0] = (tmp / x - 1. / x / x) * value * value + (2. / x - tmp) * value;
    return result;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code QuadraticPolynomialLeftExtrapolator}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(QuadraticPolynomialLeftExtrapolator.class);

  /**
   * The meta-bean for {@code QuadraticPolynomialLeftExtrapolator}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param eps  the value of the property
   */
  public QuadraticPolynomialLeftExtrapolator(
      double eps) {
    this.eps = eps;
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
  /**
   * Gets the bump parameter of finite difference approximation for the first derivative value.
   * @return the value of the property
   */
  public double getEps() {
    return eps;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      QuadraticPolynomialLeftExtrapolator other = (QuadraticPolynomialLeftExtrapolator) obj;
      return JodaBeanUtils.equal(eps, other.eps);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(eps);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("QuadraticPolynomialLeftExtrapolator{");
    buf.append("eps").append('=').append(JodaBeanUtils.toString(eps));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
