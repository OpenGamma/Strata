/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Point sensitivity to a rate from an Ibor index curve.
 * <p>
 * Holds the sensitivity to the {@link IborIndex} curve at a fixing date.
 */
@BeanDefinition(builderScope = "private")
public final class IborRateSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The Ibor index observation.
   * <p>
   * This includes the index and fixing date.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndexObservation observation;
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
   * Obtains an instance from the observation and sensitivity value.
   * <p>
   * The currency is defaulted from the index.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static IborRateSensitivity of(IborIndexObservation observation, double sensitivity) {
    return new IborRateSensitivity(observation, observation.getIndex().getCurrency(), sensitivity);
  }

  /**
   * Obtains an instance from the observation and sensitivity value,
   * specifying the currency of the value.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static IborRateSensitivity of(
      IborIndexObservation observation,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new IborRateSensitivity(observation, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Ibor index that the sensitivity refers to.
   * 
   * @return the Ibor index
   */
  public IborIndex getIndex() {
    return observation.getIndex();
  }

  //-------------------------------------------------------------------------
  @Override
  public IborRateSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new IborRateSensitivity(observation, currency, sensitivity);
  }

  @Override
  public IborRateSensitivity withSensitivity(double sensitivity) {
    return new IborRateSensitivity(observation, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof IborRateSensitivity) {
      IborRateSensitivity otherIbor = (IborRateSensitivity) other;
      return ComparisonChain.start()
          .compare(getIndex().toString(), otherIbor.getIndex().toString())
          .compare(currency, otherIbor.currency)
          .compare(observation.getFixingDate(), otherIbor.observation.getFixingDate())
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public IborRateSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (IborRateSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborRateSensitivity multipliedBy(double factor) {
    return new IborRateSensitivity(observation, currency, sensitivity * factor);
  }

  @Override
  public IborRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new IborRateSensitivity(observation, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public IborRateSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public IborRateSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborRateSensitivity}.
   * @return the meta-bean, not null
   */
  public static IborRateSensitivity.Meta meta() {
    return IborRateSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborRateSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IborRateSensitivity(
      IborIndexObservation observation,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(observation, "observation");
    JodaBeanUtils.notNull(currency, "currency");
    this.observation = observation;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public IborRateSensitivity.Meta metaBean() {
    return IborRateSensitivity.Meta.INSTANCE;
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
   * Gets the Ibor index observation.
   * <p>
   * This includes the index and fixing date.
   * @return the value of the property, not null
   */
  public IborIndexObservation getObservation() {
    return observation;
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
      IborRateSensitivity other = (IborRateSensitivity) obj;
      return JodaBeanUtils.equal(observation, other.observation) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(observation);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("IborRateSensitivity{");
    buf.append("observation").append('=').append(observation).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborRateSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code observation} property.
     */
    private final MetaProperty<IborIndexObservation> observation = DirectMetaProperty.ofImmutable(
        this, "observation", IborRateSensitivity.class, IborIndexObservation.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IborRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", IborRateSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "observation",
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
        case 122345516:  // observation
          return observation;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IborRateSensitivity> builder() {
      return new IborRateSensitivity.Builder();
    }

    @Override
    public Class<? extends IborRateSensitivity> beanType() {
      return IborRateSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code observation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndexObservation> observation() {
      return observation;
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
        case 122345516:  // observation
          return ((IborRateSensitivity) bean).getObservation();
        case 575402001:  // currency
          return ((IborRateSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((IborRateSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code IborRateSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<IborRateSensitivity> {

    private IborIndexObservation observation;
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
        case 122345516:  // observation
          return observation;
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
        case 122345516:  // observation
          this.observation = (IborIndexObservation) newValue;
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
    public IborRateSensitivity build() {
      return new IborRateSensitivity(
          observation,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("IborRateSensitivity.Builder{");
      buf.append("observation").append('=').append(JodaBeanUtils.toString(observation)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
