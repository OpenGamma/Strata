/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A period over which a fixed coupon is paid.
 * <p>
 * A single payment period within a CDS, {@link ResolvedCds}.
 * The payments of the CDS consist of periodic coupon payments and protection payment on default.
 * This class represents a single payment of the periodic payments.
 */
@BeanDefinition
public final class CreditCouponPaymentPeriod
    implements ImmutableBean, Serializable {

  /**
   * The primary currency of the payment period.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount, must be positive.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double notional;
  /**
   * The start date of the accrual period.
   * <p>
   * This is the first accrual date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date of the accrual period.
   * <p>
   * This is the last accrual date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The unadjusted start date.
   * <p>
   * The start date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the start date if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedStartDate;
  /**
   * The unadjusted end date.
   * <p>
   * The end date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the end date if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedEndDate;
  /**
   * The effective protection start date of the period.
   * <p>
   * This is the first date in the protection period associated with the payment period.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate effectiveStartDate;
  /**
   * The effective protection end date of the period.
   * <p>
   * This is the last date in the protection period associated with the payment period.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate effectiveEndDate;
  /**
   * The payment date.
   * <p>
   * The fixed rate is paid on this date.
   * This is not necessarily the same as {@code endDate}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;
  /**
   * The fixed coupon rate.
   * <p>
   * The single payment is based on this fixed coupon rate.
   * The coupon must be represented in fraction.
   */
  @PropertyDefinition
  private final double fixedRate;
  /**
   * The year fraction that the accrual period represents.
   * <p>
   * The year fraction of a period is based on {@code startDate} and {@code endDate}.
   * The value is usually calculated using a specific {@link DayCount}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double yearFraction;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.unadjustedStartDate == null) {
      builder.unadjustedStartDate = builder.startDate;
    }
    if (builder.unadjustedEndDate == null) {
      builder.unadjustedEndDate = builder.endDate;
    }
  }

  //-------------------------------------------------------------------------
  // does this period contain the date
  boolean contains(LocalDate date) {
    return !date.isBefore(startDate) && date.isBefore(endDate);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CreditCouponPaymentPeriod}.
   * @return the meta-bean, not null
   */
  public static CreditCouponPaymentPeriod.Meta meta() {
    return CreditCouponPaymentPeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CreditCouponPaymentPeriod.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CreditCouponPaymentPeriod.Builder builder() {
    return new CreditCouponPaymentPeriod.Builder();
  }

  private CreditCouponPaymentPeriod(
      Currency currency,
      double notional,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      LocalDate effectiveStartDate,
      LocalDate effectiveEndDate,
      LocalDate paymentDate,
      double fixedRate,
      double yearFraction) {
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegative(notional, "notional");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
    JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
    JodaBeanUtils.notNull(effectiveStartDate, "effectiveStartDate");
    JodaBeanUtils.notNull(effectiveEndDate, "effectiveEndDate");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    ArgChecker.notNegative(yearFraction, "yearFraction");
    this.currency = currency;
    this.notional = notional;
    this.startDate = startDate;
    this.endDate = endDate;
    this.unadjustedStartDate = unadjustedStartDate;
    this.unadjustedEndDate = unadjustedEndDate;
    this.effectiveStartDate = effectiveStartDate;
    this.effectiveEndDate = effectiveEndDate;
    this.paymentDate = paymentDate;
    this.fixedRate = fixedRate;
    this.yearFraction = yearFraction;
  }

  @Override
  public CreditCouponPaymentPeriod.Meta metaBean() {
    return CreditCouponPaymentPeriod.Meta.INSTANCE;
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
   * Gets the primary currency of the payment period.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount, must be positive.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of the accrual period.
   * <p>
   * This is the first accrual date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the accrual period.
   * <p>
   * This is the last accrual date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted start date.
   * <p>
   * The start date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the start date if not specified.
   * @return the value of the property, not null
   */
  public LocalDate getUnadjustedStartDate() {
    return unadjustedStartDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted end date.
   * <p>
   * The end date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the end date if not specified.
   * @return the value of the property, not null
   */
  public LocalDate getUnadjustedEndDate() {
    return unadjustedEndDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the effective protection start date of the period.
   * <p>
   * This is the first date in the protection period associated with the payment period.
   * @return the value of the property, not null
   */
  public LocalDate getEffectiveStartDate() {
    return effectiveStartDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the effective protection end date of the period.
   * <p>
   * This is the last date in the protection period associated with the payment period.
   * @return the value of the property, not null
   */
  public LocalDate getEffectiveEndDate() {
    return effectiveEndDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment date.
   * <p>
   * The fixed rate is paid on this date.
   * This is not necessarily the same as {@code endDate}.
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed coupon rate.
   * <p>
   * The single payment is based on this fixed coupon rate.
   * The coupon must be represented in fraction.
   * @return the value of the property
   */
  public double getFixedRate() {
    return fixedRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction that the accrual period represents.
   * <p>
   * The year fraction of a period is based on {@code startDate} and {@code endDate}.
   * The value is usually calculated using a specific {@link DayCount}.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
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
      CreditCouponPaymentPeriod other = (CreditCouponPaymentPeriod) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(unadjustedStartDate, other.unadjustedStartDate) &&
          JodaBeanUtils.equal(unadjustedEndDate, other.unadjustedEndDate) &&
          JodaBeanUtils.equal(effectiveStartDate, other.effectiveStartDate) &&
          JodaBeanUtils.equal(effectiveEndDate, other.effectiveEndDate) &&
          JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(fixedRate, other.fixedRate) &&
          JodaBeanUtils.equal(yearFraction, other.yearFraction);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedStartDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedEndDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(effectiveStartDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(effectiveEndDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(384);
    buf.append("CreditCouponPaymentPeriod{");
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("unadjustedStartDate").append('=').append(unadjustedStartDate).append(',').append(' ');
    buf.append("unadjustedEndDate").append('=').append(unadjustedEndDate).append(',').append(' ');
    buf.append("effectiveStartDate").append('=').append(effectiveStartDate).append(',').append(' ');
    buf.append("effectiveEndDate").append('=').append(effectiveEndDate).append(',').append(' ');
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("fixedRate").append('=').append(fixedRate).append(',').append(' ');
    buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CreditCouponPaymentPeriod}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", CreditCouponPaymentPeriod.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", CreditCouponPaymentPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", CreditCouponPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", CreditCouponPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedStartDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedStartDate", CreditCouponPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedEndDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedEndDate", CreditCouponPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code effectiveStartDate} property.
     */
    private final MetaProperty<LocalDate> effectiveStartDate = DirectMetaProperty.ofImmutable(
        this, "effectiveStartDate", CreditCouponPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code effectiveEndDate} property.
     */
    private final MetaProperty<LocalDate> effectiveEndDate = DirectMetaProperty.ofImmutable(
        this, "effectiveEndDate", CreditCouponPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", CreditCouponPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", CreditCouponPaymentPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", CreditCouponPaymentPeriod.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "notional",
        "startDate",
        "endDate",
        "unadjustedStartDate",
        "unadjustedEndDate",
        "effectiveStartDate",
        "effectiveEndDate",
        "paymentDate",
        "fixedRate",
        "yearFraction");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -1815017143:  // effectiveStartDate
          return effectiveStartDate;
        case -566060158:  // effectiveEndDate
          return effectiveEndDate;
        case -1540873516:  // paymentDate
          return paymentDate;
        case 747425396:  // fixedRate
          return fixedRate;
        case -1731780257:  // yearFraction
          return yearFraction;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CreditCouponPaymentPeriod.Builder builder() {
      return new CreditCouponPaymentPeriod.Builder();
    }

    @Override
    public Class<? extends CreditCouponPaymentPeriod> beanType() {
      return CreditCouponPaymentPeriod.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
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
     * The meta-property for the {@code unadjustedStartDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> unadjustedStartDate() {
      return unadjustedStartDate;
    }

    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> unadjustedEndDate() {
      return unadjustedEndDate;
    }

    /**
     * The meta-property for the {@code effectiveStartDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> effectiveStartDate() {
      return effectiveStartDate;
    }

    /**
     * The meta-property for the {@code effectiveEndDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> effectiveEndDate() {
      return effectiveEndDate;
    }

    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    /**
     * The meta-property for the {@code fixedRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> fixedRate() {
      return fixedRate;
    }

    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((CreditCouponPaymentPeriod) bean).getCurrency();
        case 1585636160:  // notional
          return ((CreditCouponPaymentPeriod) bean).getNotional();
        case -2129778896:  // startDate
          return ((CreditCouponPaymentPeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((CreditCouponPaymentPeriod) bean).getEndDate();
        case 1457691881:  // unadjustedStartDate
          return ((CreditCouponPaymentPeriod) bean).getUnadjustedStartDate();
        case 31758114:  // unadjustedEndDate
          return ((CreditCouponPaymentPeriod) bean).getUnadjustedEndDate();
        case -1815017143:  // effectiveStartDate
          return ((CreditCouponPaymentPeriod) bean).getEffectiveStartDate();
        case -566060158:  // effectiveEndDate
          return ((CreditCouponPaymentPeriod) bean).getEffectiveEndDate();
        case -1540873516:  // paymentDate
          return ((CreditCouponPaymentPeriod) bean).getPaymentDate();
        case 747425396:  // fixedRate
          return ((CreditCouponPaymentPeriod) bean).getFixedRate();
        case -1731780257:  // yearFraction
          return ((CreditCouponPaymentPeriod) bean).getYearFraction();
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
   * The bean-builder for {@code CreditCouponPaymentPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CreditCouponPaymentPeriod> {

    private Currency currency;
    private double notional;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate unadjustedStartDate;
    private LocalDate unadjustedEndDate;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private LocalDate paymentDate;
    private double fixedRate;
    private double yearFraction;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CreditCouponPaymentPeriod beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.unadjustedStartDate = beanToCopy.getUnadjustedStartDate();
      this.unadjustedEndDate = beanToCopy.getUnadjustedEndDate();
      this.effectiveStartDate = beanToCopy.getEffectiveStartDate();
      this.effectiveEndDate = beanToCopy.getEffectiveEndDate();
      this.paymentDate = beanToCopy.getPaymentDate();
      this.fixedRate = beanToCopy.getFixedRate();
      this.yearFraction = beanToCopy.getYearFraction();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -1815017143:  // effectiveStartDate
          return effectiveStartDate;
        case -566060158:  // effectiveEndDate
          return effectiveEndDate;
        case -1540873516:  // paymentDate
          return paymentDate;
        case 747425396:  // fixedRate
          return fixedRate;
        case -1731780257:  // yearFraction
          return yearFraction;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case 1457691881:  // unadjustedStartDate
          this.unadjustedStartDate = (LocalDate) newValue;
          break;
        case 31758114:  // unadjustedEndDate
          this.unadjustedEndDate = (LocalDate) newValue;
          break;
        case -1815017143:  // effectiveStartDate
          this.effectiveStartDate = (LocalDate) newValue;
          break;
        case -566060158:  // effectiveEndDate
          this.effectiveEndDate = (LocalDate) newValue;
          break;
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
          break;
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
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

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    /**
     * @deprecated Loop in application code
     */
    @Override
    @Deprecated
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public CreditCouponPaymentPeriod build() {
      preBuild(this);
      return new CreditCouponPaymentPeriod(
          currency,
          notional,
          startDate,
          endDate,
          unadjustedStartDate,
          unadjustedEndDate,
          effectiveStartDate,
          effectiveEndDate,
          paymentDate,
          fixedRate,
          yearFraction);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the primary currency of the payment period.
     * <p>
     * The amounts of the notional are usually expressed in terms of this currency,
     * however they can be converted from amounts in a different currency.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the notional amount, must be positive.
     * <p>
     * The notional amount applicable during the period.
     * The currency of the notional is specified by {@code currency}.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegative(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the start date of the accrual period.
     * <p>
     * This is the first accrual date in the period.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date of the accrual period.
     * <p>
     * This is the last accrual date in the period.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the unadjusted start date.
     * <p>
     * The start date before any business day adjustment is applied.
     * <p>
     * When building, this will default to the start date if not specified.
     * @param unadjustedStartDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedStartDate(LocalDate unadjustedStartDate) {
      JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
      this.unadjustedStartDate = unadjustedStartDate;
      return this;
    }

    /**
     * Sets the unadjusted end date.
     * <p>
     * The end date before any business day adjustment is applied.
     * <p>
     * When building, this will default to the end date if not specified.
     * @param unadjustedEndDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedEndDate(LocalDate unadjustedEndDate) {
      JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
      this.unadjustedEndDate = unadjustedEndDate;
      return this;
    }

    /**
     * Sets the effective protection start date of the period.
     * <p>
     * This is the first date in the protection period associated with the payment period.
     * @param effectiveStartDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder effectiveStartDate(LocalDate effectiveStartDate) {
      JodaBeanUtils.notNull(effectiveStartDate, "effectiveStartDate");
      this.effectiveStartDate = effectiveStartDate;
      return this;
    }

    /**
     * Sets the effective protection end date of the period.
     * <p>
     * This is the last date in the protection period associated with the payment period.
     * @param effectiveEndDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder effectiveEndDate(LocalDate effectiveEndDate) {
      JodaBeanUtils.notNull(effectiveEndDate, "effectiveEndDate");
      this.effectiveEndDate = effectiveEndDate;
      return this;
    }

    /**
     * Sets the payment date.
     * <p>
     * The fixed rate is paid on this date.
     * This is not necessarily the same as {@code endDate}.
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

    /**
     * Sets the fixed coupon rate.
     * <p>
     * The single payment is based on this fixed coupon rate.
     * The coupon must be represented in fraction.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(double fixedRate) {
      this.fixedRate = fixedRate;
      return this;
    }

    /**
     * Sets the year fraction that the accrual period represents.
     * <p>
     * The year fraction of a period is based on {@code startDate} and {@code endDate}.
     * The value is usually calculated using a specific {@link DayCount}.
     * @param yearFraction  the new value
     * @return this, for chaining, not null
     */
    public Builder yearFraction(double yearFraction) {
      ArgChecker.notNegative(yearFraction, "yearFraction");
      this.yearFraction = yearFraction;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(384);
      buf.append("CreditCouponPaymentPeriod.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ');
      buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate)).append(',').append(' ');
      buf.append("effectiveStartDate").append('=').append(JodaBeanUtils.toString(effectiveStartDate)).append(',').append(' ');
      buf.append("effectiveEndDate").append('=').append(JodaBeanUtils.toString(effectiveEndDate)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
