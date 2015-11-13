/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.model;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Data bundle related to the Hull-White one factor (extended Vasicek) model with piecewise constant volatility.
 */
@BeanDefinition(builderScope = "private")
public final class HullWhiteOneFactorPiecewiseConstantParameters implements ImmutableBean, Serializable {

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
  /**
   * The time used to represent infinity.
   * <p>
   * The last element of {@code volatilityTime} must be this value.
   */
  private static final double VOLATILITY_TIME_INFINITY = 1000d;

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
   * Creates a new instance with the volatility parameters replaced.
   * 
   * @param volatility  the replacing volatility parameters
   * @return the new instance
   */
  public HullWhiteOneFactorPiecewiseConstantParameters setVolatility(DoubleArray volatility) {
    return new HullWhiteOneFactorPiecewiseConstantParameters(meanReversion, volatility, volatilityTime);
  }

  /**
   * Gets the last volatility of the volatility parameters
   * 
   * @return the last volatility
   */
  public double getLastVolatility() {
    return volatility.get(volatility.size() - 1);
  }

  /**
   * Creates a new instance with the last volatility of the volatility parameters replaced.
   * 
   * @param volatility  the replacing volatility
   * @return the new instance
   */
  public HullWhiteOneFactorPiecewiseConstantParameters setLastVolatility(double volatility) {
    double[] volatilityArray = this.volatility.toArray();
    volatilityArray[volatilityArray.length - 1] = volatility;
    return new HullWhiteOneFactorPiecewiseConstantParameters(
        meanReversion, DoubleArray.copyOf(volatilityArray), volatilityTime);
  }

  /**
   * Creates a new instance with an extra volatility and volatility time added at the end of the respective arrays.
   * 
   * @param volatility  the volatility
   * @param volatilityTime  the times separating the constant volatility periods. Must be larger than the previous one
   * @return the new instance
   */
  public HullWhiteOneFactorPiecewiseConstantParameters addVolatility(double volatility, double volatilityTime) {
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
   * @return the meta-bean, not null
   */
  public static HullWhiteOneFactorPiecewiseConstantParameters.Meta meta() {
    return HullWhiteOneFactorPiecewiseConstantParameters.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(HullWhiteOneFactorPiecewiseConstantParameters.Meta.INSTANCE);
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
  public HullWhiteOneFactorPiecewiseConstantParameters.Meta metaBean() {
    return HullWhiteOneFactorPiecewiseConstantParameters.Meta.INSTANCE;
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

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code HullWhiteOneFactorPiecewiseConstantParameters}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code meanReversion} property.
     */
    private final MetaProperty<Double> meanReversion = DirectMetaProperty.ofImmutable(
        this, "meanReversion", HullWhiteOneFactorPiecewiseConstantParameters.class, Double.TYPE);
    /**
     * The meta-property for the {@code volatility} property.
     */
    private final MetaProperty<DoubleArray> volatility = DirectMetaProperty.ofImmutable(
        this, "volatility", HullWhiteOneFactorPiecewiseConstantParameters.class, DoubleArray.class);
    /**
     * The meta-property for the {@code volatilityTime} property.
     */
    private final MetaProperty<DoubleArray> volatilityTime = DirectMetaProperty.ofImmutable(
        this, "volatilityTime", HullWhiteOneFactorPiecewiseConstantParameters.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "meanReversion",
        "volatility",
        "volatilityTime");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2016560896:  // meanReversion
          return meanReversion;
        case -1917967323:  // volatility
          return volatility;
        case 70078610:  // volatilityTime
          return volatilityTime;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends HullWhiteOneFactorPiecewiseConstantParameters> builder() {
      return new HullWhiteOneFactorPiecewiseConstantParameters.Builder();
    }

    @Override
    public Class<? extends HullWhiteOneFactorPiecewiseConstantParameters> beanType() {
      return HullWhiteOneFactorPiecewiseConstantParameters.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code meanReversion} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> meanReversion() {
      return meanReversion;
    }

    /**
     * The meta-property for the {@code volatility} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> volatility() {
      return volatility;
    }

    /**
     * The meta-property for the {@code volatilityTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> volatilityTime() {
      return volatilityTime;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2016560896:  // meanReversion
          return ((HullWhiteOneFactorPiecewiseConstantParameters) bean).getMeanReversion();
        case -1917967323:  // volatility
          return ((HullWhiteOneFactorPiecewiseConstantParameters) bean).getVolatility();
        case 70078610:  // volatilityTime
          return ((HullWhiteOneFactorPiecewiseConstantParameters) bean).getVolatilityTime();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code HullWhiteOneFactorPiecewiseConstantParameters}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<HullWhiteOneFactorPiecewiseConstantParameters> {

    private double meanReversion;
    private DoubleArray volatility;
    private DoubleArray volatilityTime;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2016560896:  // meanReversion
          return meanReversion;
        case -1917967323:  // volatility
          return volatility;
        case 70078610:  // volatilityTime
          return volatilityTime;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -2016560896:  // meanReversion
          this.meanReversion = (Double) newValue;
          break;
        case -1917967323:  // volatility
          this.volatility = (DoubleArray) newValue;
          break;
        case 70078610:  // volatilityTime
          this.volatilityTime = (DoubleArray) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public HullWhiteOneFactorPiecewiseConstantParameters build() {
      return new HullWhiteOneFactorPiecewiseConstantParameters(
          meanReversion,
          volatility,
          volatilityTime);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("HullWhiteOneFactorPiecewiseConstantParameters.Builder{");
      buf.append("meanReversion").append('=').append(JodaBeanUtils.toString(meanReversion)).append(',').append(' ');
      buf.append("volatility").append('=').append(JodaBeanUtils.toString(volatility)).append(',').append(' ');
      buf.append("volatilityTime").append('=').append(JodaBeanUtils.toString(volatilityTime));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
