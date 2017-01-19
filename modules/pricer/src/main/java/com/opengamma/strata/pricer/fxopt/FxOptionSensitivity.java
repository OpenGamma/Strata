/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Point sensitivity to an implied volatility for a FX option model.
 * <p>
 * Holds the sensitivity to a specific volatility point.
 */
@BeanDefinition(builderScope = "private")
public final class FxOptionSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The name of the volatilities.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxOptionVolatilitiesName volatilitiesName;
  /**
   * The currency pair for which the sensitivity is presented.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyPair currencyPair;
  /**
   * The time to expiry of the option as a year fraction.
   */
  @PropertyDefinition(validate = "notNull")
  private final double expiry;
  /**
   * The strike rate.
   */
  @PropertyDefinition
  private final double strike;
  /**
   * The forward rate.
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
   * Obtains an instance, specifying sensitivity currency.
   * 
   * @param volatilitiesName  the name of the volatilities
   * @param currencyPair  the currency pair
   * @param expiry  the time to expiry of the option as a year fraction
   * @param strike  the strike rate
   * @param forward  the forward rate
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static FxOptionSensitivity of(
      FxOptionVolatilitiesName volatilitiesName,
      CurrencyPair currencyPair,
      double expiry,
      double strike,
      double forward,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new FxOptionSensitivity(
        volatilitiesName, currencyPair, expiry, strike, forward, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxOptionSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new FxOptionSensitivity(volatilitiesName, currencyPair, expiry, strike, forward, currency, sensitivity);
  }

  @Override
  public FxOptionSensitivity withSensitivity(double sensitivity) {
    return new FxOptionSensitivity(volatilitiesName, currencyPair, expiry, strike, forward, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof FxOptionSensitivity) {
      FxOptionSensitivity otherOption = (FxOptionSensitivity) other;
      return ComparisonChain.start()
          .compare(volatilitiesName, otherOption.volatilitiesName)
          .compare(currencyPair.toString(), otherOption.currencyPair.toString())
          .compare(expiry, otherOption.expiry)
          .compare(strike, otherOption.strike)
          .compare(forward, otherOption.forward)
          .compare(currency, otherOption.currency)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public FxOptionSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (FxOptionSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxOptionSensitivity multipliedBy(double factor) {
    return new FxOptionSensitivity(
        volatilitiesName, currencyPair, expiry, strike, forward, currency, sensitivity * factor);
  }

  @Override
  public FxOptionSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new FxOptionSensitivity(
        volatilitiesName, currencyPair, expiry, strike, forward, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public FxOptionSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public FxOptionSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxOptionSensitivity}.
   * @return the meta-bean, not null
   */
  public static FxOptionSensitivity.Meta meta() {
    return FxOptionSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxOptionSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxOptionSensitivity(
      FxOptionVolatilitiesName volatilitiesName,
      CurrencyPair currencyPair,
      double expiry,
      double strike,
      double forward,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(volatilitiesName, "volatilitiesName");
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(currency, "currency");
    this.volatilitiesName = volatilitiesName;
    this.currencyPair = currencyPair;
    this.expiry = expiry;
    this.strike = strike;
    this.forward = forward;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public FxOptionSensitivity.Meta metaBean() {
    return FxOptionSensitivity.Meta.INSTANCE;
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
   * Gets the name of the volatilities.
   * @return the value of the property, not null
   */
  public FxOptionVolatilitiesName getVolatilitiesName() {
    return volatilitiesName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency pair for which the sensitivity is presented.
   * @return the value of the property, not null
   */
  public CurrencyPair getCurrencyPair() {
    return currencyPair;
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
   * Gets the strike rate.
   * @return the value of the property
   */
  public double getStrike() {
    return strike;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the forward rate.
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
      FxOptionSensitivity other = (FxOptionSensitivity) obj;
      return JodaBeanUtils.equal(volatilitiesName, other.volatilitiesName) &&
          JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(strike);
    hash = hash * 31 + JodaBeanUtils.hashCode(forward);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("FxOptionSensitivity{");
    buf.append("volatilitiesName").append('=').append(volatilitiesName).append(',').append(' ');
    buf.append("currencyPair").append('=').append(currencyPair).append(',').append(' ');
    buf.append("expiry").append('=').append(expiry).append(',').append(' ');
    buf.append("strike").append('=').append(strike).append(',').append(' ');
    buf.append("forward").append('=').append(forward).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxOptionSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code volatilitiesName} property.
     */
    private final MetaProperty<FxOptionVolatilitiesName> volatilitiesName = DirectMetaProperty.ofImmutable(
        this, "volatilitiesName", FxOptionSensitivity.class, FxOptionVolatilitiesName.class);
    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", FxOptionSensitivity.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<Double> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", FxOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<Double> strike = DirectMetaProperty.ofImmutable(
        this, "strike", FxOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code forward} property.
     */
    private final MetaProperty<Double> forward = DirectMetaProperty.ofImmutable(
        this, "forward", FxOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", FxOptionSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", FxOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "volatilitiesName",
        "currencyPair",
        "expiry",
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
        case 1005147787:  // currencyPair
          return currencyPair;
        case -1289159373:  // expiry
          return expiry;
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
    public BeanBuilder<? extends FxOptionSensitivity> builder() {
      return new FxOptionSensitivity.Builder();
    }

    @Override
    public Class<? extends FxOptionSensitivity> beanType() {
      return FxOptionSensitivity.class;
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
    public MetaProperty<FxOptionVolatilitiesName> volatilitiesName() {
      return volatilitiesName;
    }

    /**
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return currencyPair;
    }

    /**
     * The meta-property for the {@code expiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> expiry() {
      return expiry;
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
          return ((FxOptionSensitivity) bean).getVolatilitiesName();
        case 1005147787:  // currencyPair
          return ((FxOptionSensitivity) bean).getCurrencyPair();
        case -1289159373:  // expiry
          return ((FxOptionSensitivity) bean).getExpiry();
        case -891985998:  // strike
          return ((FxOptionSensitivity) bean).getStrike();
        case -677145915:  // forward
          return ((FxOptionSensitivity) bean).getForward();
        case 575402001:  // currency
          return ((FxOptionSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((FxOptionSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code FxOptionSensitivity}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<FxOptionSensitivity> {

    private FxOptionVolatilitiesName volatilitiesName;
    private CurrencyPair currencyPair;
    private double expiry;
    private double strike;
    private double forward;
    private Currency currency;
    private double sensitivity;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2100884654:  // volatilitiesName
          return volatilitiesName;
        case 1005147787:  // currencyPair
          return currencyPair;
        case -1289159373:  // expiry
          return expiry;
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
          this.volatilitiesName = (FxOptionVolatilitiesName) newValue;
          break;
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
          break;
        case -1289159373:  // expiry
          this.expiry = (Double) newValue;
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
    public FxOptionSensitivity build() {
      return new FxOptionSensitivity(
          volatilitiesName,
          currencyPair,
          expiry,
          strike,
          forward,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("FxOptionSensitivity.Builder{");
      buf.append("volatilitiesName").append('=').append(JodaBeanUtils.toString(volatilitiesName)).append(',').append(' ');
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike)).append(',').append(' ');
      buf.append("forward").append('=').append(JodaBeanUtils.toString(forward)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
