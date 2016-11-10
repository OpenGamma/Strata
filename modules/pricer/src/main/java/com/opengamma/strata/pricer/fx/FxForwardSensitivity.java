/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

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
import org.joda.beans.ImmutableValidator;
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
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Point sensitivity to a forward rate of an FX rate for a currency pair.
 * <p>
 * Holds the sensitivity to the curves associated with {@link CurrencyPair} at a reference date.
 */
@BeanDefinition(builderScope = "private")
public final class FxForwardSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The currency pair for which the sensitivity is computed.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyPair currencyPair;
  /**
   * The reference currency.
   * <p>
   * This is the base currency of the FX conversion that occurs using the currency pair.
   * The reference currency must be one of the two currencies of the currency pair.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency referenceCurrency;
  /**
   * The date to query the rate for.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate referenceDate;
  /**
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The value of the sensitivity.
   * This is the amount that is converted from the base currency to the counter currency.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from currency pair, reference currency, reference date and sensitivity value.
   * <p>
   * The sensitivity currency is defaulted to be a currency of the currency pair that is not the reference currency.
   * 
   * @param currencyPair  the currency pair
   * @param referenceCurrency  the reference currency
   * @param referenceDate  the reference date
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static FxForwardSensitivity of(
      CurrencyPair currencyPair,
      Currency referenceCurrency,
      LocalDate referenceDate,
      double sensitivity) {
    boolean inverse = referenceCurrency.equals(currencyPair.getCounter());
    CurrencyPair pair = inverse ? currencyPair.inverse() : currencyPair;
    Currency sensitivityCurrency = pair.getCounter();
    return new FxForwardSensitivity(currencyPair, referenceCurrency, referenceDate, sensitivityCurrency, sensitivity);
  }

  /**
   * Obtains an instance from currency pair, reference currency, reference date
   * sensitivity currency and sensitivity value.
   * 
   * @param currencyPair  the currency pair
   * @param referenceCurrency  the reference currency
   * @param referenceDate  the reference date
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static FxForwardSensitivity of(
      CurrencyPair currencyPair,
      Currency referenceCurrency,
      LocalDate referenceDate,
      Currency sensitivityCurrency,
      double sensitivity) {
    return new FxForwardSensitivity(currencyPair, referenceCurrency, referenceDate, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (!currencyPair.contains(referenceCurrency)) {
      throw new IllegalArgumentException(Messages.format(
          "Reference currency {} must be one of those in the currency pair {}", referenceCurrency, currencyPair));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency counter to the reference currency.
   * <p>
   * The currency pair contains two currencies. One is the reference currency.
   * This method returns the other.
   * 
   * @return the counter currency
   */
  public Currency getReferenceCounterCurrency() {
    boolean inverse = referenceCurrency.equals(currencyPair.getBase());
    return inverse ? currencyPair.getCounter() : currencyPair.getBase();
  }

  //-------------------------------------------------------------------------
  @Override
  public FxForwardSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new FxForwardSensitivity(currencyPair, referenceCurrency, referenceDate, currency, sensitivity);
  }

  @Override
  public FxForwardSensitivity withSensitivity(double sensitivity) {
    return new FxForwardSensitivity(currencyPair, referenceCurrency, referenceDate, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof FxForwardSensitivity) {
      FxForwardSensitivity otherFx = (FxForwardSensitivity) other;
      return ComparisonChain.start()
          .compare(currencyPair.toString(), otherFx.currencyPair.toString())
          .compare(currency, otherFx.currency)
          .compare(referenceCurrency, otherFx.referenceCurrency)
          .compare(referenceDate, otherFx.referenceDate)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public FxForwardSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (FxForwardSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxForwardSensitivity multipliedBy(double factor) {
    return new FxForwardSensitivity(currencyPair, referenceCurrency, referenceDate, currency, sensitivity * factor);
  }

  @Override
  public FxForwardSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new FxForwardSensitivity(
        currencyPair, referenceCurrency, referenceDate, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public FxForwardSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public FxForwardSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxForwardSensitivity}.
   * @return the meta-bean, not null
   */
  public static FxForwardSensitivity.Meta meta() {
    return FxForwardSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxForwardSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxForwardSensitivity(
      CurrencyPair currencyPair,
      Currency referenceCurrency,
      LocalDate referenceDate,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(referenceCurrency, "referenceCurrency");
    JodaBeanUtils.notNull(referenceDate, "referenceDate");
    JodaBeanUtils.notNull(currency, "currency");
    this.currencyPair = currencyPair;
    this.referenceCurrency = referenceCurrency;
    this.referenceDate = referenceDate;
    this.currency = currency;
    this.sensitivity = sensitivity;
    validate();
  }

  @Override
  public FxForwardSensitivity.Meta metaBean() {
    return FxForwardSensitivity.Meta.INSTANCE;
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
   * Gets the currency pair for which the sensitivity is computed.
   * @return the value of the property, not null
   */
  public CurrencyPair getCurrencyPair() {
    return currencyPair;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference currency.
   * <p>
   * This is the base currency of the FX conversion that occurs using the currency pair.
   * The reference currency must be one of the two currencies of the currency pair.
   * @return the value of the property, not null
   */
  public Currency getReferenceCurrency() {
    return referenceCurrency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date to query the rate for.
   * @return the value of the property, not null
   */
  public LocalDate getReferenceDate() {
    return referenceDate;
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
   * This is the amount that is converted from the base currency to the counter currency.
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
      FxForwardSensitivity other = (FxForwardSensitivity) obj;
      return JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(referenceCurrency, other.referenceCurrency) &&
          JodaBeanUtils.equal(referenceDate, other.referenceDate) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceCurrency);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("FxForwardSensitivity{");
    buf.append("currencyPair").append('=').append(currencyPair).append(',').append(' ');
    buf.append("referenceCurrency").append('=').append(referenceCurrency).append(',').append(' ');
    buf.append("referenceDate").append('=').append(referenceDate).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxForwardSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", FxForwardSensitivity.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code referenceCurrency} property.
     */
    private final MetaProperty<Currency> referenceCurrency = DirectMetaProperty.ofImmutable(
        this, "referenceCurrency", FxForwardSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code referenceDate} property.
     */
    private final MetaProperty<LocalDate> referenceDate = DirectMetaProperty.ofImmutable(
        this, "referenceDate", FxForwardSensitivity.class, LocalDate.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", FxForwardSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", FxForwardSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currencyPair",
        "referenceCurrency",
        "referenceDate",
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
        case 1005147787:  // currencyPair
          return currencyPair;
        case 727652476:  // referenceCurrency
          return referenceCurrency;
        case 1600456089:  // referenceDate
          return referenceDate;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxForwardSensitivity> builder() {
      return new FxForwardSensitivity.Builder();
    }

    @Override
    public Class<? extends FxForwardSensitivity> beanType() {
      return FxForwardSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return currencyPair;
    }

    /**
     * The meta-property for the {@code referenceCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> referenceCurrency() {
      return referenceCurrency;
    }

    /**
     * The meta-property for the {@code referenceDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> referenceDate() {
      return referenceDate;
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
        case 1005147787:  // currencyPair
          return ((FxForwardSensitivity) bean).getCurrencyPair();
        case 727652476:  // referenceCurrency
          return ((FxForwardSensitivity) bean).getReferenceCurrency();
        case 1600456089:  // referenceDate
          return ((FxForwardSensitivity) bean).getReferenceDate();
        case 575402001:  // currency
          return ((FxForwardSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((FxForwardSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code FxForwardSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<FxForwardSensitivity> {

    private CurrencyPair currencyPair;
    private Currency referenceCurrency;
    private LocalDate referenceDate;
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
        case 1005147787:  // currencyPair
          return currencyPair;
        case 727652476:  // referenceCurrency
          return referenceCurrency;
        case 1600456089:  // referenceDate
          return referenceDate;
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
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
          break;
        case 727652476:  // referenceCurrency
          this.referenceCurrency = (Currency) newValue;
          break;
        case 1600456089:  // referenceDate
          this.referenceDate = (LocalDate) newValue;
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
    public FxForwardSensitivity build() {
      return new FxForwardSensitivity(
          currencyPair,
          referenceCurrency,
          referenceDate,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("FxForwardSensitivity.Builder{");
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("referenceCurrency").append('=').append(JodaBeanUtils.toString(referenceCurrency)).append(',').append(' ');
      buf.append("referenceDate").append('=').append(JodaBeanUtils.toString(referenceDate)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
