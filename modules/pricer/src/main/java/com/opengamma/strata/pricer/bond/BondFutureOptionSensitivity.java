/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.io.Serializable;
import java.time.LocalDate;
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
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Point sensitivity to an implied volatility for a bond future option model.
 * <p>
 * Holds the sensitivity to a specific volatility point.
 */
@BeanDefinition(builderScope = "private")
public final class BondFutureOptionSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The name of the volatilities.
   */
  @PropertyDefinition(validate = "notNull")
  private final BondFutureVolatilitiesName volatilitiesName;
  /**
   * The expiry date-time of the option.
   */
  @PropertyDefinition(validate = "notNull")
  private final double expiry;
  /**
   * The expiry date of the underlying future.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate futureExpiryDate;
  /**
   * The option strike price.
   */
  @PropertyDefinition
  private final double strikePrice;
  /**
   * The underlying future price.
   */
  @PropertyDefinition
  private final double futurePrice;
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
   * Obtains an instance based on the security ID.
   * 
   * @param volatilitiesName  the name of the volatilities
   * @param expiry  the time to expiry of the option as a year fraction
   * @param futureExpiryDate  the expiry date of the underlying future
   * @param strikePrice  the strike price of the option
   * @param futurePrice  the price of the underlying future
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static BondFutureOptionSensitivity of(
      BondFutureVolatilitiesName volatilitiesName,
      double expiry,
      LocalDate futureExpiryDate,
      double strikePrice,
      double futurePrice,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new BondFutureOptionSensitivity(
        volatilitiesName, expiry, futureExpiryDate, strikePrice, futurePrice, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public BondFutureOptionSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new BondFutureOptionSensitivity(
        volatilitiesName, expiry, futureExpiryDate, strikePrice, futurePrice, currency, sensitivity);
  }

  @Override
  public BondFutureOptionSensitivity withSensitivity(double sensitivity) {
    return new BondFutureOptionSensitivity(
        volatilitiesName, expiry, futureExpiryDate, strikePrice, futurePrice, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof BondFutureOptionSensitivity) {
      BondFutureOptionSensitivity otherOption = (BondFutureOptionSensitivity) other;
      return ComparisonChain.start()
          .compare(volatilitiesName.toString(), otherOption.volatilitiesName.toString())
          .compare(expiry, otherOption.expiry)
          .compare(futureExpiryDate, otherOption.futureExpiryDate)
          .compare(strikePrice, otherOption.strikePrice)
          .compare(futurePrice, otherOption.futurePrice)
          .compare(currency, otherOption.currency)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public BondFutureOptionSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (BondFutureOptionSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public BondFutureOptionSensitivity multipliedBy(double factor) {
    return new BondFutureOptionSensitivity(
        volatilitiesName, expiry, futureExpiryDate, strikePrice, futurePrice, currency, sensitivity * factor);
  }

  @Override
  public BondFutureOptionSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new BondFutureOptionSensitivity(
        volatilitiesName, expiry, futureExpiryDate, strikePrice, futurePrice, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public BondFutureOptionSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public BondFutureOptionSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BondFutureOptionSensitivity}.
   * @return the meta-bean, not null
   */
  public static BondFutureOptionSensitivity.Meta meta() {
    return BondFutureOptionSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BondFutureOptionSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private BondFutureOptionSensitivity(
      BondFutureVolatilitiesName volatilitiesName,
      double expiry,
      LocalDate futureExpiryDate,
      double strikePrice,
      double futurePrice,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(volatilitiesName, "volatilitiesName");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(futureExpiryDate, "futureExpiryDate");
    JodaBeanUtils.notNull(currency, "currency");
    this.volatilitiesName = volatilitiesName;
    this.expiry = expiry;
    this.futureExpiryDate = futureExpiryDate;
    this.strikePrice = strikePrice;
    this.futurePrice = futurePrice;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public BondFutureOptionSensitivity.Meta metaBean() {
    return BondFutureOptionSensitivity.Meta.INSTANCE;
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
  public BondFutureVolatilitiesName getVolatilitiesName() {
    return volatilitiesName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry date-time of the option.
   * @return the value of the property, not null
   */
  public double getExpiry() {
    return expiry;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry date of the underlying future.
   * @return the value of the property, not null
   */
  public LocalDate getFutureExpiryDate() {
    return futureExpiryDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the option strike price.
   * @return the value of the property
   */
  public double getStrikePrice() {
    return strikePrice;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying future price.
   * @return the value of the property
   */
  public double getFuturePrice() {
    return futurePrice;
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
      BondFutureOptionSensitivity other = (BondFutureOptionSensitivity) obj;
      return JodaBeanUtils.equal(volatilitiesName, other.volatilitiesName) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
          JodaBeanUtils.equal(futureExpiryDate, other.futureExpiryDate) &&
          JodaBeanUtils.equal(strikePrice, other.strikePrice) &&
          JodaBeanUtils.equal(futurePrice, other.futurePrice) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(futureExpiryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikePrice);
    hash = hash * 31 + JodaBeanUtils.hashCode(futurePrice);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("BondFutureOptionSensitivity{");
    buf.append("volatilitiesName").append('=').append(volatilitiesName).append(',').append(' ');
    buf.append("expiry").append('=').append(expiry).append(',').append(' ');
    buf.append("futureExpiryDate").append('=').append(futureExpiryDate).append(',').append(' ');
    buf.append("strikePrice").append('=').append(strikePrice).append(',').append(' ');
    buf.append("futurePrice").append('=').append(futurePrice).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BondFutureOptionSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code volatilitiesName} property.
     */
    private final MetaProperty<BondFutureVolatilitiesName> volatilitiesName = DirectMetaProperty.ofImmutable(
        this, "volatilitiesName", BondFutureOptionSensitivity.class, BondFutureVolatilitiesName.class);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<Double> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", BondFutureOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code futureExpiryDate} property.
     */
    private final MetaProperty<LocalDate> futureExpiryDate = DirectMetaProperty.ofImmutable(
        this, "futureExpiryDate", BondFutureOptionSensitivity.class, LocalDate.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", BondFutureOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code futurePrice} property.
     */
    private final MetaProperty<Double> futurePrice = DirectMetaProperty.ofImmutable(
        this, "futurePrice", BondFutureOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", BondFutureOptionSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", BondFutureOptionSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "volatilitiesName",
        "expiry",
        "futureExpiryDate",
        "strikePrice",
        "futurePrice",
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
        case -1119821404:  // futureExpiryDate
          return futureExpiryDate;
        case 50946231:  // strikePrice
          return strikePrice;
        case -518499002:  // futurePrice
          return futurePrice;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends BondFutureOptionSensitivity> builder() {
      return new BondFutureOptionSensitivity.Builder();
    }

    @Override
    public Class<? extends BondFutureOptionSensitivity> beanType() {
      return BondFutureOptionSensitivity.class;
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
    public MetaProperty<BondFutureVolatilitiesName> volatilitiesName() {
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
     * The meta-property for the {@code futureExpiryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> futureExpiryDate() {
      return futureExpiryDate;
    }

    /**
     * The meta-property for the {@code strikePrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strikePrice() {
      return strikePrice;
    }

    /**
     * The meta-property for the {@code futurePrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> futurePrice() {
      return futurePrice;
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
          return ((BondFutureOptionSensitivity) bean).getVolatilitiesName();
        case -1289159373:  // expiry
          return ((BondFutureOptionSensitivity) bean).getExpiry();
        case -1119821404:  // futureExpiryDate
          return ((BondFutureOptionSensitivity) bean).getFutureExpiryDate();
        case 50946231:  // strikePrice
          return ((BondFutureOptionSensitivity) bean).getStrikePrice();
        case -518499002:  // futurePrice
          return ((BondFutureOptionSensitivity) bean).getFuturePrice();
        case 575402001:  // currency
          return ((BondFutureOptionSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((BondFutureOptionSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code BondFutureOptionSensitivity}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<BondFutureOptionSensitivity> {

    private BondFutureVolatilitiesName volatilitiesName;
    private double expiry;
    private LocalDate futureExpiryDate;
    private double strikePrice;
    private double futurePrice;
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
        case -1289159373:  // expiry
          return expiry;
        case -1119821404:  // futureExpiryDate
          return futureExpiryDate;
        case 50946231:  // strikePrice
          return strikePrice;
        case -518499002:  // futurePrice
          return futurePrice;
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
          this.volatilitiesName = (BondFutureVolatilitiesName) newValue;
          break;
        case -1289159373:  // expiry
          this.expiry = (Double) newValue;
          break;
        case -1119821404:  // futureExpiryDate
          this.futureExpiryDate = (LocalDate) newValue;
          break;
        case 50946231:  // strikePrice
          this.strikePrice = (Double) newValue;
          break;
        case -518499002:  // futurePrice
          this.futurePrice = (Double) newValue;
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
    public BondFutureOptionSensitivity build() {
      return new BondFutureOptionSensitivity(
          volatilitiesName,
          expiry,
          futureExpiryDate,
          strikePrice,
          futurePrice,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("BondFutureOptionSensitivity.Builder{");
      buf.append("volatilitiesName").append('=').append(JodaBeanUtils.toString(volatilitiesName)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("futureExpiryDate").append('=').append(JodaBeanUtils.toString(futureExpiryDate)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
      buf.append("futurePrice").append('=').append(JodaBeanUtils.toString(futurePrice)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
