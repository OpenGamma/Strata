/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
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

import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDoubleQuadraticDataBundle;

/**
 *
 */
@BeanDefinition(style = "light", constructorScope = "public")
public final class DoubleQuadraticInterpolator1D
    extends Interpolator1D
    implements CurveInterpolator, ImmutableBean, Serializable {

  private static final WeightingFunction DEFAULT_WEIGHT_FUNCTION = WeightingFunctions.LINEAR;

  /** The name of the interpolator. */
  private static final String NAME = "DoubleQuadratic";

  @PropertyDefinition(validate = "notNull")
  private final WeightingFunction weightFunction;

  /**
   * Creates an instance.
   */
  public DoubleQuadraticInterpolator1D() {
    this.weightFunction = DEFAULT_WEIGHT_FUNCTION;
  }

  //-------------------------------------------------------------------------
  @Override
  public double interpolate(Interpolator1DDataBundle data, double value) {
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DDoubleQuadraticDataBundle, "data bundle is of wrong type");
    Interpolator1DDoubleQuadraticDataBundle quadraticData = (Interpolator1DDoubleQuadraticDataBundle) data;
    int low = data.getLowerBoundIndex(value);
    int high = low + 1;
    int n = data.size() - 1;
    double[] xData = data.getKeys();
    double[] yData = data.getValues();
    if (low == n) {
      return yData[n];
    } else if (low == 0) {
      RealPolynomialFunction1D quadratic = quadraticData.getQuadratic(0);
      double x = value - xData[1];
      return quadratic.applyAsDouble(x);
    } else if (high == n) {
      RealPolynomialFunction1D quadratic = quadraticData.getQuadratic(n - 2);
      double x = value - xData[n - 1];
      return quadratic.applyAsDouble(x);
    }
    RealPolynomialFunction1D quadratic1 = quadraticData.getQuadratic(low - 1);
    RealPolynomialFunction1D quadratic2 = quadraticData.getQuadratic(high - 1);
    double w = weightFunction.getWeight((xData[high] - value) / (xData[high] - xData[low]));
    return w * quadratic1.applyAsDouble(value - xData[low]) + (1 - w) * quadratic2.applyAsDouble(value - xData[high]);
  }

  @Override
  public double firstDerivative(Interpolator1DDataBundle data, double value) {
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DDoubleQuadraticDataBundle, "data bundle is of wrong type");
    Interpolator1DDoubleQuadraticDataBundle quadraticData = (Interpolator1DDoubleQuadraticDataBundle) data;
    int low = data.getLowerBoundIndex(value);
    int high = low + 1;
    int n = data.size() - 1;
    double[] xData = data.getKeys();

    if (n == 0) {
      return 0.0; // Special case of single knot
    }
    if (low == 0 || n == 1) { //second case handles two knots 
      RealPolynomialFunction1D quadraticFirstDerivative = quadraticData.getQuadraticFirstDerivative(0);
      double x = value - xData[1];
      return quadraticFirstDerivative.applyAsDouble(x);
    } else if (high >= n) {
      RealPolynomialFunction1D quadraticFirstDerivative = quadraticData.getQuadraticFirstDerivative(n - 2);
      double x = value - xData[n - 1];
      return quadraticFirstDerivative.applyAsDouble(x);
    }
    RealPolynomialFunction1D quadratic1 = quadraticData.getQuadratic(low - 1);
    RealPolynomialFunction1D quadratic2 = quadraticData.getQuadratic(high - 1);
    RealPolynomialFunction1D quadratic1FirstDerivative = quadraticData.getQuadraticFirstDerivative(low - 1);
    RealPolynomialFunction1D quadratic2FirstDerivative = quadraticData.getQuadraticFirstDerivative(high - 1);
    double w = weightFunction.getWeight((xData[high] - value) / (xData[high] - xData[low]));
    return w * quadratic1FirstDerivative.applyAsDouble(value - xData[low]) +
        (1 - w) * quadratic2FirstDerivative.applyAsDouble(value - xData[high]) +
        (quadratic2.applyAsDouble(value - xData[high]) - quadratic1.applyAsDouble(value - xData[low])) / (xData[high] - xData[low]);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, double value) {
    ArgChecker.notNull(data, "data");
    ArgChecker.isTrue(data instanceof Interpolator1DDoubleQuadraticDataBundle, "data bundle is of wrong type");
    Interpolator1DDoubleQuadraticDataBundle quadraticData = (Interpolator1DDoubleQuadraticDataBundle) data;
    int low = quadraticData.getLowerBoundIndex(value);
    int high = low + 1;
    int n = quadraticData.size();
    double[] xData = data.getKeys();
    double[] result = new double[n];
    if (low == 0) {
      double[] temp = getQuadraticSensitivities(xData, value, 1);
      result[0] = temp[0];
      result[1] = temp[1];
      result[2] = temp[2];
      return result;
    } else if (high == n - 1) {
      double[] temp = getQuadraticSensitivities(xData, value, n - 2);
      result[n - 3] = temp[0];
      result[n - 2] = temp[1];
      result[n - 1] = temp[2];
      return result;
    } else if (high == n) {
      result[n - 1] = 1;
      return result;
    }
    double[] temp1 = getQuadraticSensitivities(xData, value, low);
    double[] temp2 = getQuadraticSensitivities(xData, value, high);
    double w = weightFunction.getWeight((xData[high] - value) / (xData[high] - xData[low]));
    result[low - 1] = w * temp1[0];
    result[low] = w * temp1[1] + (1 - w) * temp2[0];
    result[high] = w * temp1[2] + (1 - w) * temp2[1];
    result[high + 1] = (1 - w) * temp2[2];
    return result;
  }

  @Override
  public Interpolator1DDoubleQuadraticDataBundle getDataBundle(double[] x, double[] y) {
    return new Interpolator1DDoubleQuadraticDataBundle(new ArrayInterpolator1DDataBundle(x, y));
  }

  @Override
  public Interpolator1DDoubleQuadraticDataBundle getDataBundleFromSortedArrays(double[] x, double[] y) {
    return new Interpolator1DDoubleQuadraticDataBundle(new ArrayInterpolator1DDataBundle(x, y, true));
  }

  private double[] getQuadraticSensitivities(double[] xData, double x, int i) {
    double[] res = new double[3];
    double deltaX = x - xData[i];
    double h1 = xData[i] - xData[i - 1];
    double h2 = xData[i + 1] - xData[i];
    res[0] = deltaX * (deltaX - h2) / h1 / (h1 + h2);
    res[1] = 1 + deltaX * (h2 - h1 - deltaX) / h1 / h2;
    res[2] = deltaX * (h1 + deltaX) / (h1 + h2) / h2;
    return res;
  }

  @Override
  public String getName() {
    return NAME;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DoubleQuadraticInterpolator1D}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(DoubleQuadraticInterpolator1D.class);

  /**
   * The meta-bean for {@code DoubleQuadraticInterpolator1D}.
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
   * @param weightFunction  the value of the property, not null
   */
  public DoubleQuadraticInterpolator1D(
      WeightingFunction weightFunction) {
    JodaBeanUtils.notNull(weightFunction, "weightFunction");
    this.weightFunction = weightFunction;
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
   * Gets the weightFunction.
   * @return the value of the property, not null
   */
  public WeightingFunction getWeightFunction() {
    return weightFunction;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DoubleQuadraticInterpolator1D other = (DoubleQuadraticInterpolator1D) obj;
      return JodaBeanUtils.equal(weightFunction, other.weightFunction);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(weightFunction);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("DoubleQuadraticInterpolator1D{");
    buf.append("weightFunction").append('=').append(JodaBeanUtils.toString(weightFunction));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
