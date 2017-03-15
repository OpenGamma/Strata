/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.date.AdjustableDate;

/**
 * A single payment of a known amount on a date, with business day adjustment rules.
 * <p>
 * This class represents a payment, where the payment date and amount are known.
 * The date is specified using an {@link AdjustableDate} which allows holidays and weekends to be handled.
 * A negative value indicates the amount is to be paid while a positive value indicates the amount is received.
 */
@BeanDefinition(builderScope = "private")
public final class AdjustablePayment
    implements Resolvable<Payment>, ImmutableBean, Serializable {

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
  private final AdjustableDate date;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance representing an amount where the date is fixed.
   * <p>
   * Whether the payment is pay or receive is determined by the sign of the specified amount.
   * 
   * @param currency  the currency of the payment
   * @param amount  the amount of the payment
   * @param date  the date that the payment is made
   * @return the adjustable payment instance
   */
  public static AdjustablePayment of(Currency currency, double amount, LocalDate date) {
    return new AdjustablePayment(CurrencyAmount.of(currency, amount), AdjustableDate.of(date));
  }

  /**
   * Obtains an instance representing an amount where the date is adjustable.
   * <p>
   * Whether the payment is pay or receive is determined by the sign of the specified amount.
   * 
   * @param currency  the currency of the payment
   * @param amount  the amount of the payment
   * @param date  the date that the payment is made
   * @return the adjustable payment instance
   */
  public static AdjustablePayment of(Currency currency, double amount, AdjustableDate date) {
    return new AdjustablePayment(CurrencyAmount.of(currency, amount), date);
  }

  /**
   * Obtains an instance representing an amount where the date is fixed.
   * <p>
   * Whether the payment is pay or receive is determined by the sign of the specified amount.
   * 
   * @param value  the amount of the payment
   * @param date  the date that the payment is made
   * @return the adjustable payment instance
   */
  public static AdjustablePayment of(CurrencyAmount value, LocalDate date) {
    return new AdjustablePayment(value, AdjustableDate.of(date));
  }

  /**
   * Obtains an instance representing an amount where the date is adjustable.
   * <p>
   * Whether the payment is pay or receive is determined by the sign of the specified amount.
   * 
   * @param value  the amount of the payment
   * @param date  the date that the payment is made
   * @return the adjustable payment instance
   */
  public static AdjustablePayment of(CurrencyAmount value, AdjustableDate date) {
    return new AdjustablePayment(value, date);
  }

  /**
   * Obtains an instance representing an amount to be paid where the date is fixed.
   * <p>
   * The sign of the amount will be normalized to be negative, indicating a payment.
   * 
   * @param value  the amount of the payment
   * @param date  the date that the payment is made
   * @return the adjustable payment instance
   */
  public static AdjustablePayment ofPay(CurrencyAmount value, LocalDate date) {
    return new AdjustablePayment(value.negative(), AdjustableDate.of(date));
  }

  /**
   * Obtains an instance representing an amount to be paid where the date is adjustable.
   * <p>
   * The sign of the amount will be normalized to be negative, indicating a payment.
   * 
   * @param value  the amount of the payment
   * @param date  the date that the payment is made
   * @return the adjustable payment instance
   */
  public static AdjustablePayment ofPay(CurrencyAmount value, AdjustableDate date) {
    return new AdjustablePayment(value.negative(), date);
  }

  /**
   * Obtains an instance representing an amount to be received where the date is fixed.
   * <p>
   * The sign of the amount will be normalized to be positive, indicating receipt.
   * 
   * @param value  the amount of the payment
   * @param date  the date that the payment is made
   * @return the adjustable payment instance
   */
  public static AdjustablePayment ofReceive(CurrencyAmount value, LocalDate date) {
    return new AdjustablePayment(value.positive(), AdjustableDate.of(date));
  }

  /**
   * Obtains an instance representing an amount to be received where the date is adjustable.
   * <p>
   * The sign of the amount will be normalized to be positive, indicating receipt.
   * 
   * @param value  the amount of the payment
   * @param date  the date that the payment is made
   * @return the adjustable payment instance
   */
  public static AdjustablePayment ofReceive(CurrencyAmount value, AdjustableDate date) {
    return new AdjustablePayment(value.positive(), date);
  }

  /**
   * Obtains an instance based on a {@code Payment}.
   * 
   * @param payment  the fixed payment
   * @return the adjustable payment instance
   */
  public static AdjustablePayment of(Payment payment) {
    return of(payment.getValue(), payment.getDate());
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
   * Resolves the date on this payment, returning a payment with a fixed date.
   * <p>
   * This returns a {@link Payment} with the same amount and resolved date.
   * 
   * @param refData  the reference data, used to find the holiday calendar
   * @return the resolved payment
   */
  @Override
  public Payment resolve(ReferenceData refData) {
    return Payment.of(value, date.adjusted(refData));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this payment with the value negated.
   * <p>
   * This takes this payment and negates it.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return a payment based on this with the value negated
   */
  public AdjustablePayment negated() {
    return AdjustablePayment.of(value.negated(), date);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code AdjustablePayment}.
   * @return the meta-bean, not null
   */
  public static AdjustablePayment.Meta meta() {
    return AdjustablePayment.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(AdjustablePayment.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private AdjustablePayment(
      CurrencyAmount value,
      AdjustableDate date) {
    JodaBeanUtils.notNull(value, "value");
    JodaBeanUtils.notNull(date, "date");
    this.value = value;
    this.date = date;
  }

  @Override
  public AdjustablePayment.Meta metaBean() {
    return AdjustablePayment.Meta.INSTANCE;
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
  public AdjustableDate getDate() {
    return date;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      AdjustablePayment other = (AdjustablePayment) obj;
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
    buf.append("AdjustablePayment{");
    buf.append("value").append('=').append(value).append(',').append(' ');
    buf.append("date").append('=').append(JodaBeanUtils.toString(date));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code AdjustablePayment}.
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
        this, "value", AdjustablePayment.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<AdjustableDate> date = DirectMetaProperty.ofImmutable(
        this, "date", AdjustablePayment.class, AdjustableDate.class);
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
    public BeanBuilder<? extends AdjustablePayment> builder() {
      return new AdjustablePayment.Builder();
    }

    @Override
    public Class<? extends AdjustablePayment> beanType() {
      return AdjustablePayment.class;
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
    public MetaProperty<AdjustableDate> date() {
      return date;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return ((AdjustablePayment) bean).getValue();
        case 3076014:  // date
          return ((AdjustablePayment) bean).getDate();
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
   * The bean-builder for {@code AdjustablePayment}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<AdjustablePayment> {

    private CurrencyAmount value;
    private AdjustableDate date;

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
          this.date = (AdjustableDate) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public AdjustablePayment build() {
      return new AdjustablePayment(
          value,
          date);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("AdjustablePayment.Builder{");
      buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
