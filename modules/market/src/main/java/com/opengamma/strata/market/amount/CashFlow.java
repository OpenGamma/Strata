/*
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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;

/**
 * A single cash flow of a currency amount on a specific date.
 */
@BeanDefinition(builderScope = "private")
public final class CashFlow
    implements FxConvertible<CashFlow>, Comparable<CashFlow>, ImmutableBean, Serializable {

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
   * The forecast value of the cash flow.
   * <p>
   * The forecast value is signed.
   * A negative value indicates a payment while a positive value indicates receipt.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount forecastValue;
  /**
   * The discount factor.
   * <p>
   * This is the discount factor between valuation date and the payment date.
   * Thus present value is the forecast value multiplied by the discount factor.
   */
  @PropertyDefinition
  private final double discountFactor;

  //-------------------------------------------------------------------------
  /**
   * Creates a {@code CashFlow} representing a single cash flow from
   * payment date, present value and discount factor.
   * 
   * @param paymentDate  the payment date
   * @param presentValue  the present value as a currency amount
   * @param discountFactor  the discount factor
   * @return the cash flow instance
   */
  public static CashFlow ofPresentValue(LocalDate paymentDate, CurrencyAmount presentValue, double discountFactor) {
    return new CashFlow(paymentDate, presentValue, presentValue.multipliedBy(1d / discountFactor), discountFactor);
  }

  /**
   * Creates a {@code CashFlow} representing a single cash flow from payment date, present value amount, 
   * discount factor and currency.
   * 
   * @param paymentDate  the payment date
   * @param currency  the currency
   * @param presentValue  the amount of the present value
   * @param discountFactor  the discount factor
   * @return the cash flow instance
   */
  public static CashFlow ofPresentValue(LocalDate paymentDate, Currency currency, double presentValue, double discountFactor) {
    return ofPresentValue(paymentDate, CurrencyAmount.of(currency, presentValue), discountFactor);
  }

  /**
   * Creates a {@code CashFlow} representing a single cash flow from
   * payment date, forecast value and discount factor.
   * 
   * @param paymentDate  the payment date
   * @param forecastValue  the forecast value as a currency amount
   * @param discountFactor  the discount factor
   * @return the cash flow instance
   */
  public static CashFlow ofForecastValue(LocalDate paymentDate, CurrencyAmount forecastValue, double discountFactor) {
    return new CashFlow(paymentDate, forecastValue.multipliedBy(discountFactor), forecastValue, discountFactor);
  }

  /**
   * Creates a {@code CashFlow} representing a single cash flow from payment date, forecast value amount,
   * discount factor and currency.
   * 
   * @param paymentDate  the payment date
   * @param currency  the currency
   * @param forecastValue  the amount of the forecast value
   * @param discountFactor  the discount factor
   * @return the cash flow instance
   */
  public static CashFlow ofForecastValue(LocalDate paymentDate, Currency currency, double forecastValue, double discountFactor) {
    return ofForecastValue(paymentDate, CurrencyAmount.of(currency, forecastValue), discountFactor);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this cash flow to an equivalent amount in the specified currency.
   * <p>
   * The result will have both the present and forecast value expressed in terms of the given currency.
   * If conversion is needed, the provider will be used to supply the FX rate.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the converted instance, in the specified currency
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public CashFlow convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    if (presentValue.getCurrency().equals(resultCurrency) && forecastValue.getCurrency().equals(resultCurrency)) {
      return this;
    }
    CurrencyAmount pv = presentValue.convertedTo(resultCurrency, rateProvider);
    CurrencyAmount fv = forecastValue.convertedTo(resultCurrency, rateProvider);
    return new CashFlow(paymentDate, pv, fv, discountFactor);
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
        .compare(forecastValue, other.forecastValue)
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
      CurrencyAmount forecastValue,
      double discountFactor) {
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    JodaBeanUtils.notNull(presentValue, "presentValue");
    JodaBeanUtils.notNull(forecastValue, "forecastValue");
    this.paymentDate = paymentDate;
    this.presentValue = presentValue;
    this.forecastValue = forecastValue;
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
   * Gets the forecast value of the cash flow.
   * <p>
   * The forecast value is signed.
   * A negative value indicates a payment while a positive value indicates receipt.
   * @return the value of the property, not null
   */
  public CurrencyAmount getForecastValue() {
    return forecastValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount factor.
   * <p>
   * This is the discount factor between valuation date and the payment date.
   * Thus present value is the forecast value multiplied by the discount factor.
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
      return JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(presentValue, other.presentValue) &&
          JodaBeanUtils.equal(forecastValue, other.forecastValue) &&
          JodaBeanUtils.equal(discountFactor, other.discountFactor);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(presentValue);
    hash = hash * 31 + JodaBeanUtils.hashCode(forecastValue);
    hash = hash * 31 + JodaBeanUtils.hashCode(discountFactor);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("CashFlow{");
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("presentValue").append('=').append(presentValue).append(',').append(' ');
    buf.append("forecastValue").append('=').append(forecastValue).append(',').append(' ');
    buf.append("discountFactor").append('=').append(JodaBeanUtils.toString(discountFactor));
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
     * The meta-property for the {@code forecastValue} property.
     */
    private final MetaProperty<CurrencyAmount> forecastValue = DirectMetaProperty.ofImmutable(
        this, "forecastValue", CashFlow.class, CurrencyAmount.class);
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
        "forecastValue",
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
        case 1310579766:  // forecastValue
          return forecastValue;
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
     * The meta-property for the {@code forecastValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> forecastValue() {
      return forecastValue;
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
        case 1310579766:  // forecastValue
          return ((CashFlow) bean).getForecastValue();
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
  private static final class Builder extends DirectPrivateBeanBuilder<CashFlow> {

    private LocalDate paymentDate;
    private CurrencyAmount presentValue;
    private CurrencyAmount forecastValue;
    private double discountFactor;

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
        case -1540873516:  // paymentDate
          return paymentDate;
        case 686253430:  // presentValue
          return presentValue;
        case 1310579766:  // forecastValue
          return forecastValue;
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
        case 1310579766:  // forecastValue
          this.forecastValue = (CurrencyAmount) newValue;
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
    public CashFlow build() {
      return new CashFlow(
          paymentDate,
          presentValue,
          forecastValue,
          discountFactor);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("CashFlow.Builder{");
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("presentValue").append('=').append(JodaBeanUtils.toString(presentValue)).append(',').append(' ');
      buf.append("forecastValue").append('=').append(JodaBeanUtils.toString(forecastValue)).append(',').append(' ');
      buf.append("discountFactor").append('=').append(JodaBeanUtils.toString(discountFactor));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
