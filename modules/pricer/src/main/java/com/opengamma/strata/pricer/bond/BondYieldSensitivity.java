/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.DoubleUnaryOperator;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Point sensitivity to a bond yield implied parameter point.
 * <p>
 * Holds the sensitivity to the bond yield grid point.
 */
@BeanDefinition(builderScope = "private")
public final class BondYieldSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The name of the volatilities.
   */
  @PropertyDefinition(validate = "notNull")
  private final BondVolatilitiesName volatilitiesName;
  /**
   * The time to expiry of the option as a year fraction.
   */
  @PropertyDefinition(validate = "notNull")
  private final double expiry;
  /**
   * The underlying duration.
   */
  @PropertyDefinition
  private final double duration;
  /**
   * The strike yield.
   */
  @PropertyDefinition
  private final double strike;
  /**
   * The underlying bond forward yield.
   */
  @PropertyDefinition
  private final double forward;
  /**
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The value of the sensitivity.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified elements.
   * 
   * @param volatilitiesName  the name of the volatilities
   * @param expiry  the time to expiry of the option as a year fraction
   * @param duration  the underlying bond duration
   * @param strike  the bond strike yield
   * @param forward  the underlying bond forward yield
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static BondYieldSensitivity of(
      BondVolatilitiesName volatilitiesName,
      double expiry,
      double duration,
      double strike,
      double forward,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new BondYieldSensitivity(volatilitiesName, expiry, duration, strike, forward, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public BondYieldSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new BondYieldSensitivity(volatilitiesName, expiry, duration, strike, forward, currency, sensitivity);
  }

  @Override
  public BondYieldSensitivity withSensitivity(double value) {
    return new BondYieldSensitivity(volatilitiesName, expiry, duration, strike, forward, currency, value);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof BondYieldSensitivity) {
      BondYieldSensitivity otherSwpt = (BondYieldSensitivity) other;
      return ComparisonChain.start()
          .compare(volatilitiesName, otherSwpt.volatilitiesName)
          .compare(currency, otherSwpt.currency)
          .compare(expiry, otherSwpt.expiry)
          .compare(duration, otherSwpt.duration)
          .compare(strike, otherSwpt.strike)
          .compare(forward, otherSwpt.forward)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public BondYieldSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (BondYieldSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public BondYieldSensitivity multipliedBy(double factor) {
    return new BondYieldSensitivity(volatilitiesName, expiry, duration, strike, forward, currency, sensitivity * factor);
  }

  @Override
  public BondYieldSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new BondYieldSensitivity(
        volatilitiesName, expiry, duration, strike, forward, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public BondYieldSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public BondYieldSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code BondYieldSensitivity}.
   * @return the meta-bean, not null
   */
  public static BondYieldSensitivity.Meta meta() {
    return BondYieldSensitivity.Meta.INSTANCE;
  }

  static {
    MetaBean.register(BondYieldSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private BondYieldSensitivity(
      BondVolatilitiesName volatilitiesName,
      double expiry,
      double duration,
      double strike,
      double forward,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(volatilitiesName, "volatilitiesName");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(currency, "currency");
    this.volatilitiesName = volatilitiesName;
    this.expiry = expiry;
    this.duration = duration;
    this.strike = strike;
    this.forward = forward;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public BondYieldSensitivity.Meta metaBean() {
    return BondYieldSensitivity.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name of the volatilities.
   * @return the value of the property, not null
   */
  public BondVolatilitiesName getVolatilitiesName() {
    return volatilitiesName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time to expiry of the option as a year fraction.
   * @return the value of the property, not null
   */
  public double getExpiry() {
    return expiry;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying duration.
   * @return the value of the property
   */
  public double getDuration() {
    return duration;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike yield.
   * @return the value of the property
   */
  public double getStrike() {
    return strike;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying bond forward yield.
   * @return the value of the property
   */
  public double getForward() {
    return forward;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the sensitivity.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value of the sensitivity.
   * @return the value of the property
   */
  @Override
  public double getSensitivity() {
    return sensitivity;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      BondYieldSensitivity other = (BondYieldSensitivity) obj;
      return JodaBeanUtils.equal(volatilitiesName, other.volatilitiesName) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
          JodaBeanUtils.equal(duration, other.duration) &&
          JodaBeanUtils.equal(strike, other.strike) &&
          JodaBeanUtils.equal(forward, other.forward) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(volatilitiesName);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(duration);
    hash = hash * 31 + JodaBeanUtils.hashCode(strike);
    hash = hash * 31 + JodaBeanUtils.hashCode(forward);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("BondYieldSensitivity{");
    buf.append("volatilitiesName").append('=').append(JodaBeanUtils.toString(volatilitiesName)).append(',').append(' ');
    buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
    buf.append("duration").append('=').append(JodaBeanUtils.toString(duration)).append(',').append(' ');
    buf.append("strike").append('=').append(JodaBeanUtils.toString(strike)).append(',').append(' ');
    buf.append("forward").append('=').append(JodaBeanUtils.toString(forward)).append(',').append(' ');
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BondYieldSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code volatilitiesName} property.
     */
    private final MetaProperty<BondVolatilitiesName> volatilitiesName = DirectMetaProperty.ofImmutable(
        this, "volatilitiesName", BondYieldSensitivity.class, BondVolatilitiesName.class);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<Double> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", BondYieldSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code duration} property.
     */
    private final MetaProperty<Double> duration = DirectMetaProperty.ofImmutable(
        this, "duration", BondYieldSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<Double> strike = DirectMetaProperty.ofImmutable(
        this, "strike", BondYieldSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code forward} property.
     */
    private final MetaProperty<Double> forward = DirectMetaProperty.ofImmutable(
        this, "forward", BondYieldSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", BondYieldSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", BondYieldSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "volatilitiesName",
        "expiry",
        "duration",
        "strike",
        "forward",
        "currency",
        "sensitivity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2100884654:  // volatilitiesName
          return volatilitiesName;
        case -1289159373:  // expiry
          return expiry;
        case -1992012396:  // duration
          return duration;
        case -891985998:  // strike
          return strike;
        case -677145915:  // forward
          return forward;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends BondYieldSensitivity> builder() {
      return new BondYieldSensitivity.Builder();
    }

    @Override
    public Class<? extends BondYieldSensitivity> beanType() {
      return BondYieldSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code volatilitiesName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BondVolatilitiesName> volatilitiesName() {
      return volatilitiesName;
    }

    /**
     * The meta-property for the {@code expiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> expiry() {
      return expiry;
    }

    /**
     * The meta-property for the {@code duration} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> duration() {
      return duration;
    }

    /**
     * The meta-property for the {@code strike} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strike() {
      return strike;
    }

    /**
     * The meta-property for the {@code forward} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> forward() {
      return forward;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code sensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> sensitivity() {
      return sensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 2100884654:  // volatilitiesName
          return ((BondYieldSensitivity) bean).getVolatilitiesName();
        case -1289159373:  // expiry
          return ((BondYieldSensitivity) bean).getExpiry();
        case -1992012396:  // duration
          return ((BondYieldSensitivity) bean).getDuration();
        case -891985998:  // strike
          return ((BondYieldSensitivity) bean).getStrike();
        case -677145915:  // forward
          return ((BondYieldSensitivity) bean).getForward();
        case 575402001:  // currency
          return ((BondYieldSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((BondYieldSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code BondYieldSensitivity}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<BondYieldSensitivity> {

    private BondVolatilitiesName volatilitiesName;
    private double expiry;
    private double duration;
    private double strike;
    private double forward;
    private Currency currency;
    private double sensitivity;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2100884654:  // volatilitiesName
          return volatilitiesName;
        case -1289159373:  // expiry
          return expiry;
        case -1992012396:  // duration
          return duration;
        case -891985998:  // strike
          return strike;
        case -677145915:  // forward
          return forward;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 2100884654:  // volatilitiesName
          this.volatilitiesName = (BondVolatilitiesName) newValue;
          break;
        case -1289159373:  // expiry
          this.expiry = (Double) newValue;
          break;
        case -1992012396:  // duration
          this.duration = (Double) newValue;
          break;
        case -891985998:  // strike
          this.strike = (Double) newValue;
          break;
        case -677145915:  // forward
          this.forward = (Double) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 564403871:  // sensitivity
          this.sensitivity = (Double) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public BondYieldSensitivity build() {
      return new BondYieldSensitivity(
          volatilitiesName,
          expiry,
          duration,
          strike,
          forward,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("BondYieldSensitivity.Builder{");
      buf.append("volatilitiesName").append('=').append(JodaBeanUtils.toString(volatilitiesName)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("duration").append('=').append(JodaBeanUtils.toString(duration)).append(',').append(' ');
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike)).append(',').append(' ');
      buf.append("forward").append('=').append(JodaBeanUtils.toString(forward)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
