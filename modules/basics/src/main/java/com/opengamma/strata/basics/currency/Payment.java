/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * A single payment of a known amount on a specific date.
 * <p>
 * This class represents a payment, where the payment date and amount are known.
 * A negative value indicates the amount is to be paid while a positive value indicates the amount is received.
 */
@BeanDefinition
public final class Payment
    implements FxConvertible<Payment>, ImmutableBean, Serializable {

  /**
   * The amount of the payment.
   * <p>
   * The amount is signed.
   * A negative value indicates the amount is to be paid while a positive value indicates the amount is received.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount value;
  /**
   * The date that the payment is made.
   * <p>
   * This date should normally be a valid business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate date;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance representing an amount.
   * <p>
   * Whether the payment is pay or receive is determined by the sign of the specified amount.
   * 
   * @param currency  the currency of the payment
   * @param amount  the amount of the payment
   * @param date  the date that the payment is made
   * @return the payment instance
   */
  public static Payment of(Currency currency, double amount, LocalDate date) {
    return new Payment(CurrencyAmount.of(currency, amount), date);
  }

  /**
   * Obtains an instance representing an amount.
   * <p>
   * Whether the payment is pay or receive is determined by the sign of the specified amount.
   * 
   * @param value  the amount of the payment
   * @param date  the date that the payment is made
   * @return the payment instance
   */
  public static Payment of(CurrencyAmount value, LocalDate date) {
    return new Payment(value, date);
  }

  /**
   * Obtains an instance representing an amount to be paid.
   * <p>
   * The sign of the amount will be normalized to be negative, indicating a payment.
   * 
   * @param value  the amount of the payment
   * @param date  the date that the payment is made
   * @return the payment instance
   */
  public static Payment ofPay(CurrencyAmount value, LocalDate date) {
    return new Payment(value.negative(), date);
  }

  /**
   * Obtains an instance representing an amount to be received.
   * <p>
   * The sign of the amount will be normalized to be positive, indicating receipt.
   * 
   * @param value  the amount of the payment
   * @param date  the date that the payment is made
   * @return the payment instance
   */
  public static Payment ofReceive(CurrencyAmount value, LocalDate date) {
    return new Payment(value.positive(), date);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency of the payment.
   * <p>
   * This simply returns {@code getValue().getCurrency()}.
   * 
   * @return the currency of the payment
   */
  public Currency getCurrency() {
    return value.getCurrency();
  }

  /**
   * Gets the amount of the payment.
   * <p>
   * The payment value is signed.
   * A negative value indicates a payment while a positive value indicates receipt.
   * <p>
   * This simply returns {@code getValue().getAmount()}.
   * 
   * @return the amount of the payment
   */
  public double getAmount() {
    return value.getAmount();
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the payment date using the rules of the specified adjuster.
   * <p>
   * The adjuster is typically an instance of {@link BusinessDayAdjustment}.
   * If the date is unchanged by the adjuster, {@code this} payment will be returned.
   * 
   * @param adjuster  the adjuster to apply to the payment date
   * @return the adjusted payment
   */
  public Payment adjustDate(TemporalAdjuster adjuster) {
    LocalDate adjusted = date.with(adjuster);
    return adjusted.equals(date) ? this : toBuilder().date(adjusted).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this {@code Payment} with the value negated.
   * <p>
   * This takes this payment and negates it.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return a payment based on this with the value negated
   */
  public Payment negated() {
    return Payment.of(value.negated(), date);
  }

  /**
   * Converts this payment to an equivalent payment in the specified currency.
   * <p>
   * The result will be expressed in terms of the given currency.
   * If conversion is needed, the provider will be used to supply the FX rate.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the converted instance, in the specified currency
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public Payment convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    if (getCurrency().equals(resultCurrency)) {
      return this;
    }
    return Payment.of(value.convertedTo(resultCurrency, rateProvider), date);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Payment}.
   * @return the meta-bean, not null
   */
  public static Payment.Meta meta() {
    return Payment.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Payment.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Payment.Builder builder() {
    return new Payment.Builder();
  }

  private Payment(
      CurrencyAmount value,
      LocalDate date) {
    JodaBeanUtils.notNull(value, "value");
    JodaBeanUtils.notNull(date, "date");
    this.value = value;
    this.date = date;
  }

  @Override
  public Payment.Meta metaBean() {
    return Payment.Meta.INSTANCE;
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
   * Gets the amount of the payment.
   * <p>
   * The amount is signed.
   * A negative value indicates the amount is to be paid while a positive value indicates the amount is received.
   * @return the value of the property, not null
   */
  public CurrencyAmount getValue() {
    return value;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that the payment is made.
   * <p>
   * This date should normally be a valid business day.
   * @return the value of the property, not null
   */
  public LocalDate getDate() {
    return date;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Payment other = (Payment) obj;
      return JodaBeanUtils.equal(value, other.value) &&
          JodaBeanUtils.equal(date, other.date);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(value);
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("Payment{");
    buf.append("value").append('=').append(value).append(',').append(' ');
    buf.append("date").append('=').append(JodaBeanUtils.toString(date));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Payment}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code value} property.
     */
    private final MetaProperty<CurrencyAmount> value = DirectMetaProperty.ofImmutable(
        this, "value", Payment.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<LocalDate> date = DirectMetaProperty.ofImmutable(
        this, "date", Payment.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "value",
        "date");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return value;
        case 3076014:  // date
          return date;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Payment.Builder builder() {
      return new Payment.Builder();
    }

    @Override
    public Class<? extends Payment> beanType() {
      return Payment.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code value} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> value() {
      return value;
    }

    /**
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> date() {
      return date;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return ((Payment) bean).getValue();
        case 3076014:  // date
          return ((Payment) bean).getDate();
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
   * The bean-builder for {@code Payment}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Payment> {

    private CurrencyAmount value;
    private LocalDate date;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Payment beanToCopy) {
      this.value = beanToCopy.getValue();
      this.date = beanToCopy.getDate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return value;
        case 3076014:  // date
          return date;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          this.value = (CurrencyAmount) newValue;
          break;
        case 3076014:  // date
          this.date = (LocalDate) newValue;
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
    public Payment build() {
      return new Payment(
          value,
          date);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the amount of the payment.
     * <p>
     * The amount is signed.
     * A negative value indicates the amount is to be paid while a positive value indicates the amount is received.
     * @param value  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder value(CurrencyAmount value) {
      JodaBeanUtils.notNull(value, "value");
      this.value = value;
      return this;
    }

    /**
     * Sets the date that the payment is made.
     * <p>
     * This date should normally be a valid business day.
     * @param date  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder date(LocalDate date) {
      JodaBeanUtils.notNull(date, "date");
      this.date = date;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("Payment.Builder{");
      buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
