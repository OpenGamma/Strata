/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Log-linear extrapolator: the extrapolant is exp(f(x)) where f(x) is a linear function
 * which is smoothly connected with a log-interpolator exp(F(x)), such as
 * {@link LogNaturalCubicMonotonicityPreservingInterpolator1D},
 * i.e., F'(x) = f'(x) at a respectivie endpoint.
 */
@BeanDefinition(style = "light", constructorScope = "public")
public final class LogLinearExtrapolator1D
    implements Extrapolator1D, ImmutableBean {

  /** The extrapolator name. */
  public static final String NAME = "LogLinear";

  /**
   * The bump parameter of finite difference approximation for the first derivative value.
   */
  @PropertyDefinition
  private final double eps;

  /**
   * Creates an instance.
   */
  public LogLinearExtrapolator1D() {
    this(1e-8);
  }

  //-------------------------------------------------------------------------
  @Override
  public double extrapolate(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    if (value < data.firstKey()) {
      return leftExtrapolate(data, value, interpolator);
    } else if (value > data.lastKey()) {
      return rightExtrapolate(data, value, interpolator);
    }
    throw new IllegalArgumentException("Value " + value + " was within data range");
  }

  @Override
  public double firstDerivative(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    if (value < data.firstKey()) {
      return leftExtrapolateDerivative(data, value, interpolator);
    } else if (value > data.lastKey()) {
      return rightExtrapolateDerivative(data, value, interpolator);
    }
    throw new IllegalArgumentException("Value " + value + " was within data range");
  }

  @Override
  public double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    if (value < data.firstKey()) {
      return getLeftSensitivities(data, value, interpolator);
    } else if (value > data.lastKey()) {
      return getRightSensitivities(data, value, interpolator);
    }
    throw new IllegalArgumentException("Value " + value + " was within data range");
  }

  private double leftExtrapolate(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    double x = data.firstKey();
    double y = Math.log(data.firstValue());
    double m = interpolator.firstDerivative(data, x) / interpolator.interpolate(data, x);
    return Math.exp(y + (value - x) * m);
  }

  private double rightExtrapolate(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    double x = data.lastKey();
    double y = Math.log(data.lastValue());
    double m = interpolator.firstDerivative(data, x) / interpolator.interpolate(data, x);
    return Math.exp(y + (value - x) * m);
  }

  private double leftExtrapolateDerivative(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    double x = data.firstKey();
    double y = Math.log(data.firstValue());
    double m = interpolator.firstDerivative(data, x) / interpolator.interpolate(data, x);
    return m * Math.exp(y + (value - x) * m);
  }

  private double rightExtrapolateDerivative(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    ArgChecker.notNull(data, "data");
    double x = data.lastKey();
    double y = Math.log(data.lastValue());
    double m = interpolator.firstDerivative(data, x) / interpolator.interpolate(data, x);
    return m * Math.exp(y + (value - x) * m);
  }

  private double[] getLeftSensitivities(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    double eps = this.eps * (data.lastKey() - data.firstKey());
    double x = data.firstKey();
    double resValueInterpolator = interpolator.interpolate(data, x + eps);
    double resValueExtrapolator = leftExtrapolate(data, value, interpolator);
    double[] result = interpolator.getNodeSensitivitiesForValue(data, x + eps);
    double factor1 = (value - x) / eps;
    double factor2 = factor1 * resValueExtrapolator / resValueInterpolator;

    int n = result.length;
    for (int i = 1; i < n; i++) {
      result[i] *= factor2;
    }
    result[0] = result[0] * factor2 + (1. - factor1) * resValueExtrapolator / data.firstValue();
    return result;
  }

  private double[] getRightSensitivities(Interpolator1DDataBundle data, double value, Interpolator1D interpolator) {
    double eps = this.eps * (data.lastKey() - data.firstKey());
    double x = data.lastKey();
    double resValueInterpolator = interpolator.interpolate(data, x - eps);
    double resValueExtrapolator = rightExtrapolate(data, value, interpolator);
    double[] result = interpolator.getNodeSensitivitiesForValue(data, x - eps);
    double factor1 = (value - x) / eps;
    double factor2 = factor1 * resValueExtrapolator / resValueInterpolator;

    int n = result.length;
    for (int i = 0; i < n - 1; i++) {
      result[i] *= -factor2;
    }
    result[n - 1] = (1. + factor1) * resValueExtrapolator / data.lastValue() - result[n - 1] * factor2;
    return result;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code LogLinearExtrapolator1D}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(LogLinearExtrapolator1D.class);

  /**
   * The meta-bean for {@code LogLinearExtrapolator1D}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * Creates an instance.
   * @param eps  the value of the property
   */
  public LogLinearExtrapolator1D(
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
      LogLinearExtrapolator1D other = (LogLinearExtrapolator1D) obj;
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
    buf.append("LogLinearExtrapolator1D{");
    buf.append("eps").append('=').append(JodaBeanUtils.toString(eps));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
