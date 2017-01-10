/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Point sensitivity to a forward rate of an FX rate for an FX index.
 * <p>
 * Holds the sensitivity to the {@link FxIndex} curve at a fixing date.
 */
@BeanDefinition(builderScope = "private")
public final class FxIndexSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The FX rate observation.
   * <p>
   * This includes the index and fixing date.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxIndexObservation observation;
  /**
   * The reference currency.
   * <p>
   * This is the base currency of the FX conversion that occurs using the index.
   * The reference currency must be one of the two currencies of the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency referenceCurrency;
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
   * Obtains an instance from the observation, reference currency and sensitivity value.
   * <p>
   * The sensitivity currency is defaulted to be the counter currency of queried currency pair.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param referenceCurrency  the reference currency
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static FxIndexSensitivity of(FxIndexObservation observation, Currency referenceCurrency, double sensitivity) {
    CurrencyPair obsPair = observation.getCurrencyPair();
    boolean inverse = referenceCurrency.equals(obsPair.getCounter());
    CurrencyPair queriedPair = inverse ? obsPair.inverse() : obsPair;
    Currency sensiCurrency = queriedPair.getCounter();
    return new FxIndexSensitivity(observation, referenceCurrency, sensiCurrency, sensitivity);
  }

  /**
   * Obtains an instance from the observation, reference currency and sensitivity value,
   * specifying the currency of the value.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param referenceCurrency  the reference currency
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static FxIndexSensitivity of(
      FxIndexObservation observation,
      Currency referenceCurrency,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new FxIndexSensitivity(observation, referenceCurrency, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this sensitivity to an {@code FxForwardSensitivity}.
   * <p>
   * The time series, fixing date and FX index are lost by this conversion.
   * Instead, maturity date and currency pair are contained in {@link FxForwardSensitivity}.
   * 
   * @return the FX forward sensitivity
   */
  public FxForwardSensitivity toFxForwardSensitivity() {
    return FxForwardSensitivity.of(
        observation.getCurrencyPair(),
        referenceCurrency,
        observation.getMaturityDate(),
        currency,
        sensitivity);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the FX index that the sensitivity refers to.
   * 
   * @return the FX index
   */
  public FxIndex getIndex() {
    return observation.getIndex();
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new FxIndexSensitivity(observation, referenceCurrency, currency, sensitivity);
  }

  @Override
  public FxIndexSensitivity withSensitivity(double sensitivity) {
    return new FxIndexSensitivity(observation, referenceCurrency, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof FxIndexSensitivity) {
      FxIndexSensitivity otherFx = (FxIndexSensitivity) other;
      return ComparisonChain.start()
          .compare(getIndex().toString(), otherFx.getIndex().toString())
          .compare(currency, otherFx.currency)
          .compare(referenceCurrency, otherFx.referenceCurrency)
          .compare(observation.getFixingDate(), otherFx.observation.getFixingDate())
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public FxIndexSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (FxIndexSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexSensitivity multipliedBy(double factor) {
    return new FxIndexSensitivity(observation, referenceCurrency, currency, sensitivity * factor);
  }

  @Override
  public FxIndexSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new FxIndexSensitivity(observation, referenceCurrency, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public FxIndexSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public FxIndexSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxIndexSensitivity}.
   * @return the meta-bean, not null
   */
  public static FxIndexSensitivity.Meta meta() {
    return FxIndexSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxIndexSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxIndexSensitivity(
      FxIndexObservation observation,
      Currency referenceCurrency,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(observation, "observation");
    JodaBeanUtils.notNull(referenceCurrency, "referenceCurrency");
    JodaBeanUtils.notNull(currency, "currency");
    this.observation = observation;
    this.referenceCurrency = referenceCurrency;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public FxIndexSensitivity.Meta metaBean() {
    return FxIndexSensitivity.Meta.INSTANCE;
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
   * Gets the FX rate observation.
   * <p>
   * This includes the index and fixing date.
   * @return the value of the property, not null
   */
  public FxIndexObservation getObservation() {
    return observation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference currency.
   * <p>
   * This is the base currency of the FX conversion that occurs using the index.
   * The reference currency must be one of the two currencies of the index.
   * @return the value of the property, not null
   */
  public Currency getReferenceCurrency() {
    return referenceCurrency;
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
      FxIndexSensitivity other = (FxIndexSensitivity) obj;
      return JodaBeanUtils.equal(observation, other.observation) &&
          JodaBeanUtils.equal(referenceCurrency, other.referenceCurrency) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(observation);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceCurrency);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("FxIndexSensitivity{");
    buf.append("observation").append('=').append(observation).append(',').append(' ');
    buf.append("referenceCurrency").append('=').append(referenceCurrency).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxIndexSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code observation} property.
     */
    private final MetaProperty<FxIndexObservation> observation = DirectMetaProperty.ofImmutable(
        this, "observation", FxIndexSensitivity.class, FxIndexObservation.class);
    /**
     * The meta-property for the {@code referenceCurrency} property.
     */
    private final MetaProperty<Currency> referenceCurrency = DirectMetaProperty.ofImmutable(
        this, "referenceCurrency", FxIndexSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", FxIndexSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", FxIndexSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "observation",
        "referenceCurrency",
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
        case 727652476:  // referenceCurrency
          return referenceCurrency;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxIndexSensitivity> builder() {
      return new FxIndexSensitivity.Builder();
    }

    @Override
    public Class<? extends FxIndexSensitivity> beanType() {
      return FxIndexSensitivity.class;
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
    public MetaProperty<FxIndexObservation> observation() {
      return observation;
    }

    /**
     * The meta-property for the {@code referenceCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> referenceCurrency() {
      return referenceCurrency;
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
          return ((FxIndexSensitivity) bean).getObservation();
        case 727652476:  // referenceCurrency
          return ((FxIndexSensitivity) bean).getReferenceCurrency();
        case 575402001:  // currency
          return ((FxIndexSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((FxIndexSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code FxIndexSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<FxIndexSensitivity> {

    private FxIndexObservation observation;
    private Currency referenceCurrency;
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
        case 727652476:  // referenceCurrency
          return referenceCurrency;
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
          this.observation = (FxIndexObservation) newValue;
          break;
        case 727652476:  // referenceCurrency
          this.referenceCurrency = (Currency) newValue;
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
    public FxIndexSensitivity build() {
      return new FxIndexSensitivity(
          observation,
          referenceCurrency,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("FxIndexSensitivity.Builder{");
      buf.append("observation").append('=').append(JodaBeanUtils.toString(observation)).append(',').append(' ');
      buf.append("referenceCurrency").append('=').append(JodaBeanUtils.toString(referenceCurrency)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
