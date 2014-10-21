/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.trade.swap;

import java.io.Serializable;
import java.time.LocalDate;
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

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.schedule.SchedulePeriod;
import com.opengamma.collect.ArgChecker;

/**
 * An accrual period based on a fixed interest rate.
 * <p>
 * A fixed rate swap leg consists of one or more periods that are the basis of accrual.
 * This class represents one such period.
 */
@BeanDefinition
public final class FixedRateAccrualPeriod
    implements AccrualPeriod, ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The start date of the period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate startDate;
  /**
   * The end date of the period.
   * <p>
   * This is the last date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate endDate;
  /**
   * The primary currency of this period.
   * <p>
   * This is the currency of the swap leg and the currency that interest calculation is made in.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * See the optional {@code fxReset} property.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount.
   * <p>
   * The notional amount applicable during the period.
   * Positive if receiving, negative if paying.
   */
  @PropertyDefinition(validate = "notNull")
  private final double notional;
  /**
   * The year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount}.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   */
  @PropertyDefinition
  private final double yearFraction;
  /**
   * The rate to be paid, with a 5% rate expressed as 0.05.
   * <p>
   * The fixed rate applicable during the period, may be negative.
   */
  @PropertyDefinition
  private final double rate;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of the fixed rate period.
   * <p>
   * This factory will calculate the accrued interest using the standard formula.
   * 
   * @param schedulePeriod  the schedule period
   * @param dayCount  the day count convention
   * @param currency  the currency of the notional
   * @param notional  the notional amount, which does not vary over time
   * @param rate  the rate, which does not vary over time
   * @return the calculation period
   */
  public static FixedRateAccrualPeriod of(
      SchedulePeriod schedulePeriod,
      DayCount dayCount,
      Currency currency,
      double notional,
      double rate) {
    ArgChecker.notNull(schedulePeriod, "schedulePeriod");
    ArgChecker.notNull(dayCount, "dayCount");
    ArgChecker.notNull(notional, "notional");
    double yearFraction = dayCount.getDayCountFraction(
        schedulePeriod.getStartDate(), schedulePeriod.getEndDate(), schedulePeriod);
    return FixedRateAccrualPeriod.builder()
        .startDate(schedulePeriod.getStartDate())
        .endDate(schedulePeriod.getEndDate())
        .currency(currency)
        .notional(notional)
        .yearFraction(yearFraction)
        .rate(rate)
        .build();
  }

  //-----------------------------------------------------------------------
  /**
   * Calculates the fixed amount that the period generates.
   * <p>
   * The amount is calculated as {@code (notional * rate * yearFraction)}.
   * 
   * @param notional  the notional amount
   * @param rate  the rate applicable to the notional
   * @param yearFraction  the year fraction, determined from a day count
   * @return the calculated amount of the period
   */
  public static CurrencyAmount calculateAmount(CurrencyAmount notional, double rate, double yearFraction) {
    double amount = notional.getAmount() * rate * yearFraction;
    return CurrencyAmount.of(notional.getCurrency(), amount);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FixedRateAccrualPeriod}.
   * @return the meta-bean, not null
   */
  public static FixedRateAccrualPeriod.Meta meta() {
    return FixedRateAccrualPeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FixedRateAccrualPeriod.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FixedRateAccrualPeriod.Builder builder() {
    return new FixedRateAccrualPeriod.Builder();
  }

  private FixedRateAccrualPeriod(
      LocalDate startDate,
      LocalDate endDate,
      Currency currency,
      double notional,
      double yearFraction,
      double rate) {
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(notional, "notional");
    this.startDate = startDate;
    this.endDate = endDate;
    this.currency = currency;
    this.notional = notional;
    this.yearFraction = yearFraction;
    this.rate = rate;
  }

  @Override
  public FixedRateAccrualPeriod.Meta metaBean() {
    return FixedRateAccrualPeriod.Meta.INSTANCE;
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
   * Gets the start date of the period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the period.
   * <p>
   * This is the last date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency of this period.
   * <p>
   * This is the currency of the swap leg and the currency that interest calculation is made in.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * See the optional {@code fxReset} property.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount.
   * <p>
   * The notional amount applicable during the period.
   * Positive if receiving, negative if paying.
   * @return the value of the property, not null
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount}.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate to be paid, with a 5% rate expressed as 0.05.
   * <p>
   * The fixed rate applicable during the period, may be negative.
   * @return the value of the property
   */
  public double getRate() {
    return rate;
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
      FixedRateAccrualPeriod other = (FixedRateAccrualPeriod) obj;
      return JodaBeanUtils.equal(getStartDate(), other.getStartDate()) &&
          JodaBeanUtils.equal(getEndDate(), other.getEndDate()) &&
          JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getYearFraction(), other.getYearFraction()) &&
          JodaBeanUtils.equal(getRate(), other.getRate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getStartDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getEndDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash += hash * 31 + JodaBeanUtils.hashCode(getYearFraction());
    hash += hash * 31 + JodaBeanUtils.hashCode(getRate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("FixedRateAccrualPeriod{");
    buf.append("startDate").append('=').append(getStartDate()).append(',').append(' ');
    buf.append("endDate").append('=').append(getEndDate()).append(',').append(' ');
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("notional").append('=').append(getNotional()).append(',').append(' ');
    buf.append("yearFraction").append('=').append(getYearFraction()).append(',').append(' ');
    buf.append("rate").append('=').append(JodaBeanUtils.toString(getRate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FixedRateAccrualPeriod}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", FixedRateAccrualPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", FixedRateAccrualPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", FixedRateAccrualPeriod.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", FixedRateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", FixedRateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code rate} property.
     */
    private final MetaProperty<Double> rate = DirectMetaProperty.ofImmutable(
        this, "rate", FixedRateAccrualPeriod.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "currency",
        "notional",
        "yearFraction",
        "rate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 3493088:  // rate
          return rate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FixedRateAccrualPeriod.Builder builder() {
      return new FixedRateAccrualPeriod.Builder();
    }

    @Override
    public Class<? extends FixedRateAccrualPeriod> beanType() {
      return FixedRateAccrualPeriod.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    /**
     * The meta-property for the {@code rate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> rate() {
      return rate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((FixedRateAccrualPeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((FixedRateAccrualPeriod) bean).getEndDate();
        case 575402001:  // currency
          return ((FixedRateAccrualPeriod) bean).getCurrency();
        case 1585636160:  // notional
          return ((FixedRateAccrualPeriod) bean).getNotional();
        case -1731780257:  // yearFraction
          return ((FixedRateAccrualPeriod) bean).getYearFraction();
        case 3493088:  // rate
          return ((FixedRateAccrualPeriod) bean).getRate();
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
   * The bean-builder for {@code FixedRateAccrualPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FixedRateAccrualPeriod> {

    private LocalDate startDate;
    private LocalDate endDate;
    private Currency currency;
    private double notional;
    private double yearFraction;
    private double rate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FixedRateAccrualPeriod beanToCopy) {
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.yearFraction = beanToCopy.getYearFraction();
      this.rate = beanToCopy.getRate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 3493088:  // rate
          return rate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
          break;
        case 3493088:  // rate
          this.rate = (Double) newValue;
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
    public FixedRateAccrualPeriod build() {
      return new FixedRateAccrualPeriod(
          startDate,
          endDate,
          currency,
          notional,
          yearFraction,
          rate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code startDate} property in the builder.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the {@code endDate} property in the builder.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the {@code currency} property in the builder.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the {@code notional} property in the builder.
     * @param notional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      JodaBeanUtils.notNull(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the {@code yearFraction} property in the builder.
     * @param yearFraction  the new value
     * @return this, for chaining, not null
     */
    public Builder yearFraction(double yearFraction) {
      this.yearFraction = yearFraction;
      return this;
    }

    /**
     * Sets the {@code rate} property in the builder.
     * @param rate  the new value
     * @return this, for chaining, not null
     */
    public Builder rate(double rate) {
      this.rate = rate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("FixedRateAccrualPeriod.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("rate").append('=').append(JodaBeanUtils.toString(rate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
