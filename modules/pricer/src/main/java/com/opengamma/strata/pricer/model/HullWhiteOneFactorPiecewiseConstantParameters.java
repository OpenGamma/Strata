/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.model;

import java.io.Serializable;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Data bundle related to the Hull-White one factor (extended Vasicek) model with piecewise constant volatility.
 */
@BeanDefinition(style = "light")
public final class HullWhiteOneFactorPiecewiseConstantParameters
    implements ImmutableBean, Serializable {

  /**
   * The time used to represent infinity.
   * <p>
   * The last element of {@code volatilityTime} must be this value.
   */
  private static final double VOLATILITY_TIME_INFINITY = 1000d;

  /**
   * The mean reversion speed parameter.
   */
  @PropertyDefinition(validate = "notNull")
  private final double meanReversion;
  /**
   * The volatility parameters.
   * <p>
   * The volatility is constant between the volatility times, i.e., volatility value at t is {@code volatility.get(i)} 
   * for any t between {@code volatilityTime.get(i)} and {@code volatilityTime.get(i+1)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray volatility;
  /**
   * The times separating the constant volatility periods.
   * <p>
   * The time should be sorted by increasing order. The first time is 0 and the last time is 1000 (represents infinity).
   * These extra times are added in {@link #of(double, DoubleArray, DoubleArray)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray volatilityTime;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the model parameters.
   * <p>
   * {@code volatilityTime} should be sorted in increasing order. The first time (0) and the last time (1000) will be 
   * added within this method. Thus the size of {@code volatility} should be greater than that of {@code volatilityTime}
   * by one.
   * 
   * @param meanReversion  the mean reversion speed (a) parameter
   * @param volatility  the volatility parameters
   * @param volatilityTime  the times separating the constant volatility periods
   * @return the instance
   */
  public static HullWhiteOneFactorPiecewiseConstantParameters of(
      double meanReversion,
      DoubleArray volatility,
      DoubleArray volatilityTime) {

    double[] volatilityTimeArray = new double[volatilityTime.size() + 2];
    volatilityTimeArray[0] = 0d;
    volatilityTimeArray[volatilityTime.size() + 1] = VOLATILITY_TIME_INFINITY;
    System.arraycopy(volatilityTime.toArray(), 0, volatilityTimeArray, 1, volatilityTime.size());
    return new HullWhiteOneFactorPiecewiseConstantParameters(
        meanReversion, volatility, DoubleArray.copyOf(volatilityTimeArray));
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    int sizeTime = volatilityTime.size();
    ArgChecker.isTrue(sizeTime == volatility.size() + 1, "size mismatch between volatility and volatilityTime");
    for (int i = 1; i < sizeTime; ++i) {
      ArgChecker.isTrue(volatilityTime.get(i - 1) < volatilityTime.get(i), "volatility times should be increasing");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy with the volatility parameters changed.
   * 
   * @param volatility  the new volatility parameters
   * @return the new instance
   */
  public HullWhiteOneFactorPiecewiseConstantParameters withVolatility(DoubleArray volatility) {
    return new HullWhiteOneFactorPiecewiseConstantParameters(meanReversion, volatility, volatilityTime);
  }

  /**
   * Gets the last volatility of the volatility parameters.
   * 
   * @return the last volatility
   */
  public double getLastVolatility() {
    return volatility.get(volatility.size() - 1);
  }

  /**
   * Returns a copy with the last volatility of the volatility parameters changed.
   * 
   * @param volatility  the new volatility
   * @return the new instance
   */
  public HullWhiteOneFactorPiecewiseConstantParameters withLastVolatility(double volatility) {
    double[] volatilityArray = this.volatility.toArray();
    volatilityArray[volatilityArray.length - 1] = volatility;
    return new HullWhiteOneFactorPiecewiseConstantParameters(
        meanReversion, DoubleArray.copyOf(volatilityArray), volatilityTime);
  }

  /**
   * Returns a copy with an extra volatility and volatility time added at the end of the respective arrays.
   * 
   * @param volatility  the volatility
   * @param volatilityTime  the times separating the constant volatility periods. Must be larger than the previous one
   * @return the new instance
   */
  public HullWhiteOneFactorPiecewiseConstantParameters withVolatilityAdded(double volatility, double volatilityTime) {
    double[] volatilityArray = this.volatility.toArray();
    double[] volatilityTimeArray = this.volatilityTime.toArray();
    ArgChecker.isTrue(volatilityTime > volatilityTimeArray[volatilityTimeArray.length - 2],
        "volatility times should be increasing");
    double[] newVolatilityArray = new double[volatilityArray.length + 1];
    double[] newVolatilityTimeArray = new double[volatilityTimeArray.length + 1];
    System.arraycopy(volatilityArray, 0, newVolatilityArray, 0, volatilityArray.length);
    System.arraycopy(volatilityTimeArray, 0, newVolatilityTimeArray, 0, volatilityTimeArray.length - 1);
    newVolatilityArray[volatilityArray.length] = volatility;
    newVolatilityTimeArray[volatilityTimeArray.length - 1] = volatilityTime;
    newVolatilityTimeArray[volatilityTimeArray.length] = VOLATILITY_TIME_INFINITY;
    return new HullWhiteOneFactorPiecewiseConstantParameters(
        meanReversion, DoubleArray.copyOf(newVolatilityArray), DoubleArray.copyOf(newVolatilityTimeArray));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code HullWhiteOneFactorPiecewiseConstantParameters}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(HullWhiteOneFactorPiecewiseConstantParameters.class);

  /**
   * The meta-bean for {@code HullWhiteOneFactorPiecewiseConstantParameters}.
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

  private HullWhiteOneFactorPiecewiseConstantParameters(
      double meanReversion,
      DoubleArray volatility,
      DoubleArray volatilityTime) {
    JodaBeanUtils.notNull(meanReversion, "meanReversion");
    JodaBeanUtils.notNull(volatility, "volatility");
    JodaBeanUtils.notNull(volatilityTime, "volatilityTime");
    this.meanReversion = meanReversion;
    this.volatility = volatility;
    this.volatilityTime = volatilityTime;
    validate();
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
   * Gets the mean reversion speed parameter.
   * @return the value of the property, not null
   */
  public double getMeanReversion() {
    return meanReversion;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the volatility parameters.
   * <p>
   * The volatility is constant between the volatility times, i.e., volatility value at t is {@code volatility.get(i)}
   * for any t between {@code volatilityTime.get(i)} and {@code volatilityTime.get(i+1)}.
   * @return the value of the property, not null
   */
  public DoubleArray getVolatility() {
    return volatility;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the times separating the constant volatility periods.
   * <p>
   * The time should be sorted by increasing order. The first time is 0 and the last time is 1000 (represents infinity).
   * These extra times are added in {@link #of(double, DoubleArray, DoubleArray)}.
   * @return the value of the property, not null
   */
  public DoubleArray getVolatilityTime() {
    return volatilityTime;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      HullWhiteOneFactorPiecewiseConstantParameters other = (HullWhiteOneFactorPiecewiseConstantParameters) obj;
      return JodaBeanUtils.equal(meanReversion, other.meanReversion) &&
          JodaBeanUtils.equal(volatility, other.volatility) &&
          JodaBeanUtils.equal(volatilityTime, other.volatilityTime);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(meanReversion);
    hash = hash * 31 + JodaBeanUtils.hashCode(volatility);
    hash = hash * 31 + JodaBeanUtils.hashCode(volatilityTime);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("HullWhiteOneFactorPiecewiseConstantParameters{");
    buf.append("meanReversion").append('=').append(meanReversion).append(',').append(' ');
    buf.append("volatility").append('=').append(volatility).append(',').append(' ');
    buf.append("volatilityTime").append('=').append(JodaBeanUtils.toString(volatilityTime));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
