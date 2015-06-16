/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

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
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * A single cash flow of a currency amount on a specific date.
 */
@BeanDefinition(builderScope = "private")
public final class CashFlow
    implements Comparable<CashFlow>, ImmutableBean, Serializable {

  /**
   * The payment date.
   * <p>
   * This is the date on which the cash flow occurs. 
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;
  /**
   * The present value of the cash flow. 
   * <p>
   * The present value is signed.
   * A negative value indicates a payment while a positive value indicates receipt.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount presentValue;
  /**
   * The future value of the cash flow. 
   * <p>
   * The future value is signed.
   * A negative value indicates a payment while a positive value indicates receipt.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount futureValue;
  /**
   * The discount factor. 
   * <p>
   * This is the discount factor between valuation date and the payment date.
   * Thus present value is the future value multiplied by the discount factor. 
   */
  @PropertyDefinition
  private final double discountFactor;

  //-------------------------------------------------------------------------
  /**
   * Creates a {@code CashFlow} representing a single cash flow from
   * payment date, future value and discount factor. 
   * 
   * @param paymentDate  the payment date
   * @param presentValue  the future value as a currency amount
   * @param discountFactor  the discount factor
   * @return the cash flow instance
   */
  public static CashFlow ofPresentValue(LocalDate paymentDate, CurrencyAmount presentValue, double discountFactor) {
    return new CashFlow(paymentDate, presentValue, presentValue.multipliedBy(1d / discountFactor), discountFactor);
  }

  /**
   * Creates a {@code CashFlow} representing a single cash flow from payment date, future value amount, 
   * discount factor and currency. 
   * 
   * @param paymentDate  the payment date
   * @param currency  the currency
   * @param presentValue  the amount of the future value
   * @param discountFactor  the discount factor
   * @return the cash flow instance
   */
  public static CashFlow ofPresentValue(LocalDate paymentDate, Currency currency, double presentValue, double discountFactor) {
    return ofPresentValue(paymentDate, CurrencyAmount.of(currency, presentValue), discountFactor);
  }

  /**
   * Creates a {@code CashFlow} representing a single cash flow from
   * payment date, future value and discount factor. 
   * 
   * @param paymentDate  the payment date
   * @param futureValue  the future value as a currency amount
   * @param discountFactor  the discount factor
   * @return the cash flow instance
   */
  public static CashFlow ofFutureValue(LocalDate paymentDate, CurrencyAmount futureValue, double discountFactor) {
    return new CashFlow(paymentDate, futureValue.multipliedBy(discountFactor), futureValue, discountFactor);
  }

  /**
   * Creates a {@code CashFlow} representing a single cash flow from payment date, future value amount, 
   * discount factor and currency. 
   * 
   * @param paymentDate  the payment date
   * @param currency  the currency
   * @param futureValue  the amount of the future value
   * @param discountFactor  the discount factor
   * @return the cash flow instance
   */
  public static CashFlow ofFutureValue(LocalDate paymentDate, Currency currency, double futureValue, double discountFactor) {
    return ofFutureValue(paymentDate, CurrencyAmount.of(currency, futureValue), discountFactor);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this cash flow to another, first by date, then value.
   * 
   * @param other  the other instance
   * @return the comparison
   */
  @Override
  public int compareTo(CashFlow other) {
    return ComparisonChain.start()
        .compare(paymentDate, other.paymentDate)
        .compare(presentValue, other.presentValue)
        .compare(futureValue, other.futureValue)
        .compare(discountFactor, other.discountFactor)
        .result();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CashFlow}.
   * @return the meta-bean, not null
   */
  public static CashFlow.Meta meta() {
    return CashFlow.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CashFlow.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CashFlow(
      LocalDate paymentDate,
      CurrencyAmount presentValue,
      CurrencyAmount futureValue,
      double discountFactor) {
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    JodaBeanUtils.notNull(presentValue, "presentValue");
    JodaBeanUtils.notNull(futureValue, "futureValue");
    this.paymentDate = paymentDate;
    this.presentValue = presentValue;
    this.futureValue = futureValue;
    this.discountFactor = discountFactor;
  }

  @Override
  public CashFlow.Meta metaBean() {
    return CashFlow.Meta.INSTANCE;
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
   * Gets the payment date.
   * <p>
   * This is the date on which the cash flow occurs.
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the present value of the cash flow.
   * <p>
   * The present value is signed.
   * A negative value indicates a payment while a positive value indicates receipt.
   * @return the value of the property, not null
   */
  public CurrencyAmount getPresentValue() {
    return presentValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the future value of the cash flow.
   * <p>
   * The future value is signed.
   * A negative value indicates a payment while a positive value indicates receipt.
   * @return the value of the property, not null
   */
  public CurrencyAmount getFutureValue() {
    return futureValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount factor.
   * <p>
   * This is the discount factor between valuation date and the payment date.
   * Thus present value is the future value multiplied by the discount factor.
   * @return the value of the property
   */
  public double getDiscountFactor() {
    return discountFactor;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CashFlow other = (CashFlow) obj;
      return JodaBeanUtils.equal(getPaymentDate(), other.getPaymentDate()) &&
          JodaBeanUtils.equal(getPresentValue(), other.getPresentValue()) &&
          JodaBeanUtils.equal(getFutureValue(), other.getFutureValue()) &&
          JodaBeanUtils.equal(getDiscountFactor(), other.getDiscountFactor());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPresentValue());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFutureValue());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDiscountFactor());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("CashFlow{");
    buf.append("paymentDate").append('=').append(getPaymentDate()).append(',').append(' ');
    buf.append("presentValue").append('=').append(getPresentValue()).append(',').append(' ');
    buf.append("futureValue").append('=').append(getFutureValue()).append(',').append(' ');
    buf.append("discountFactor").append('=').append(JodaBeanUtils.toString(getDiscountFactor()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CashFlow}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", CashFlow.class, LocalDate.class);
    /**
     * The meta-property for the {@code presentValue} property.
     */
    private final MetaProperty<CurrencyAmount> presentValue = DirectMetaProperty.ofImmutable(
        this, "presentValue", CashFlow.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code futureValue} property.
     */
    private final MetaProperty<CurrencyAmount> futureValue = DirectMetaProperty.ofImmutable(
        this, "futureValue", CashFlow.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code discountFactor} property.
     */
    private final MetaProperty<Double> discountFactor = DirectMetaProperty.ofImmutable(
        this, "discountFactor", CashFlow.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "paymentDate",
        "presentValue",
        "futureValue",
        "discountFactor");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return paymentDate;
        case 686253430:  // presentValue
          return presentValue;
        case -513460882:  // futureValue
          return futureValue;
        case -557144592:  // discountFactor
          return discountFactor;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CashFlow> builder() {
      return new CashFlow.Builder();
    }

    @Override
    public Class<? extends CashFlow> beanType() {
      return CashFlow.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    /**
     * The meta-property for the {@code presentValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> presentValue() {
      return presentValue;
    }

    /**
     * The meta-property for the {@code futureValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> futureValue() {
      return futureValue;
    }

    /**
     * The meta-property for the {@code discountFactor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> discountFactor() {
      return discountFactor;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return ((CashFlow) bean).getPaymentDate();
        case 686253430:  // presentValue
          return ((CashFlow) bean).getPresentValue();
        case -513460882:  // futureValue
          return ((CashFlow) bean).getFutureValue();
        case -557144592:  // discountFactor
          return ((CashFlow) bean).getDiscountFactor();
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
   * The bean-builder for {@code CashFlow}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CashFlow> {

    private LocalDate paymentDate;
    private CurrencyAmount presentValue;
    private CurrencyAmount futureValue;
    private double discountFactor;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return paymentDate;
        case 686253430:  // presentValue
          return presentValue;
        case -513460882:  // futureValue
          return futureValue;
        case -557144592:  // discountFactor
          return discountFactor;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case 686253430:  // presentValue
          this.presentValue = (CurrencyAmount) newValue;
          break;
        case -513460882:  // futureValue
          this.futureValue = (CurrencyAmount) newValue;
          break;
        case -557144592:  // discountFactor
          this.discountFactor = (Double) newValue;
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
    public CashFlow build() {
      return new CashFlow(
          paymentDate,
          presentValue,
          futureValue,
          discountFactor);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("CashFlow.Builder{");
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("presentValue").append('=').append(JodaBeanUtils.toString(presentValue)).append(',').append(' ');
      buf.append("futureValue").append('=').append(JodaBeanUtils.toString(futureValue)).append(',').append(' ');
      buf.append("discountFactor").append('=').append(JodaBeanUtils.toString(discountFactor));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
