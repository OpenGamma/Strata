/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A period over which a fixed coupon is paid.
 * <p>
 * A single payment period within a fixed coupon bond, {@link ResolvedFixedCouponBond}.
 * The payments of the fixed coupon bond consist periodic coupon payments and nominal payment.
 * This class represents a single payment of the periodic payments.
 */
@BeanDefinition
public final class FixedCouponBondPaymentPeriod
    implements BondPaymentPeriod, ImmutableBean, Serializable {

  /**
   * The primary currency of the payment period.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
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
   * The start date of the payment period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate startDate;
  /**
   * The end date of the payment period.
   * <p>
   * This is the last date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
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
   * The detachment date.
   * <p>
   * Some bonds trade ex-coupon before the coupon payment.
   * The coupon is paid not to the owner of the bond on the payment date but to the
   * owner of the bond on the detachment date.
   * <p>
   * When building, this will default to the end date if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate detachmentDate;
  /**
   * The fixed coupon rate.
   * <p>
   * The single payment is based on this fixed coupon rate.
   */
  @PropertyDefinition
  private final double fixedRate;
  /**
   * The year fraction that the accrual period represents.
   * <p>
   * The year fraction of a bond period is based on the unadjusted dates.
   * <p>
   * The value is usually calculated using a {@link DayCount}.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double yearFraction;

  //-------------------------------------------------------------------------
  // could use @ImmutablePreBuild and @ImmutableValidate but faster inline
  @ImmutableConstructor
  private FixedCouponBondPaymentPeriod(
      Currency currency,
      double notional,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      LocalDate detachmentDate,
      double fixedRate,
      double yearFraction) {
    this.currency = ArgChecker.notNull(currency, "currency");
    this.notional = notional;
    this.startDate = ArgChecker.notNull(startDate, "startDate");
    this.endDate = ArgChecker.notNull(endDate, "endDate");
    this.unadjustedStartDate = firstNonNull(unadjustedStartDate, startDate);
    this.unadjustedEndDate = firstNonNull(unadjustedEndDate, endDate);
    this.detachmentDate = firstNonNull(detachmentDate, endDate);
    this.fixedRate = fixedRate;
    this.yearFraction = yearFraction;
    // check for unadjusted must be after firstNonNull
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.inOrderNotEqual(
        this.unadjustedStartDate, this.unadjustedEndDate, "unadjustedStartDate", "unadjustedEndDate");
    ArgChecker.inOrderOrEqual(this.detachmentDate, this.endDate, "detachmentDate", "endDate");
  }

  //-------------------------------------------------------------------------
  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    // no index
  }

  @Override
  public FixedCouponBondPaymentPeriod adjustPaymentDate(TemporalAdjuster adjuster) {
    return this;
  }

  @Override
  public LocalDate getPaymentDate() {
    return getEndDate();
  }

  /**
   * Checks if there is an ex-coupon period.
   * 
   * @return true if has an ex-coupon period
   */
  public boolean hasExCouponPeriod() {
    return !detachmentDate.equals(endDate);
  }

  /**
   * Checks if this period contains the specified date, based on unadjusted dates.
   * <p>
   * The unadjusted start and end dates are used in the comparison.
   * The unadjusted start date is included, the unadjusted end date is excluded.
   * 
   * @param date  the date to check
   * @return true if this period contains the date
   */
  boolean contains(LocalDate date) {
    return !date.isBefore(unadjustedStartDate) && date.isBefore(unadjustedEndDate);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FixedCouponBondPaymentPeriod}.
   * @return the meta-bean, not null
   */
  public static FixedCouponBondPaymentPeriod.Meta meta() {
    return FixedCouponBondPaymentPeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FixedCouponBondPaymentPeriod.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FixedCouponBondPaymentPeriod.Builder builder() {
    return new FixedCouponBondPaymentPeriod.Builder();
  }

  @Override
  public FixedCouponBondPaymentPeriod.Meta metaBean() {
    return FixedCouponBondPaymentPeriod.Meta.INSTANCE;
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
  @Override
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
   * Gets the start date of the payment period.
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
   * Gets the end date of the payment period.
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
   * Gets the detachment date.
   * <p>
   * Some bonds trade ex-coupon before the coupon payment.
   * The coupon is paid not to the owner of the bond on the payment date but to the
   * owner of the bond on the detachment date.
   * <p>
   * When building, this will default to the end date if not specified.
   * @return the value of the property, not null
   */
  public LocalDate getDetachmentDate() {
    return detachmentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed coupon rate.
   * <p>
   * The single payment is based on this fixed coupon rate.
   * @return the value of the property
   */
  public double getFixedRate() {
    return fixedRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction that the accrual period represents.
   * <p>
   * The year fraction of a bond period is based on the unadjusted dates.
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
      FixedCouponBondPaymentPeriod other = (FixedCouponBondPaymentPeriod) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(unadjustedStartDate, other.unadjustedStartDate) &&
          JodaBeanUtils.equal(unadjustedEndDate, other.unadjustedEndDate) &&
          JodaBeanUtils.equal(detachmentDate, other.detachmentDate) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(detachmentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("FixedCouponBondPaymentPeriod{");
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("unadjustedStartDate").append('=').append(unadjustedStartDate).append(',').append(' ');
    buf.append("unadjustedEndDate").append('=').append(unadjustedEndDate).append(',').append(' ');
    buf.append("detachmentDate").append('=').append(detachmentDate).append(',').append(' ');
    buf.append("fixedRate").append('=').append(fixedRate).append(',').append(' ');
    buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FixedCouponBondPaymentPeriod}.
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
        this, "currency", FixedCouponBondPaymentPeriod.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", FixedCouponBondPaymentPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", FixedCouponBondPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", FixedCouponBondPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedStartDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedStartDate", FixedCouponBondPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedEndDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedEndDate", FixedCouponBondPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code detachmentDate} property.
     */
    private final MetaProperty<LocalDate> detachmentDate = DirectMetaProperty.ofImmutable(
        this, "detachmentDate", FixedCouponBondPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", FixedCouponBondPaymentPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", FixedCouponBondPaymentPeriod.class, Double.TYPE);
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
        "detachmentDate",
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
        case -878940481:  // detachmentDate
          return detachmentDate;
        case 747425396:  // fixedRate
          return fixedRate;
        case -1731780257:  // yearFraction
          return yearFraction;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FixedCouponBondPaymentPeriod.Builder builder() {
      return new FixedCouponBondPaymentPeriod.Builder();
    }

    @Override
    public Class<? extends FixedCouponBondPaymentPeriod> beanType() {
      return FixedCouponBondPaymentPeriod.class;
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
     * The meta-property for the {@code detachmentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> detachmentDate() {
      return detachmentDate;
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
          return ((FixedCouponBondPaymentPeriod) bean).getCurrency();
        case 1585636160:  // notional
          return ((FixedCouponBondPaymentPeriod) bean).getNotional();
        case -2129778896:  // startDate
          return ((FixedCouponBondPaymentPeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((FixedCouponBondPaymentPeriod) bean).getEndDate();
        case 1457691881:  // unadjustedStartDate
          return ((FixedCouponBondPaymentPeriod) bean).getUnadjustedStartDate();
        case 31758114:  // unadjustedEndDate
          return ((FixedCouponBondPaymentPeriod) bean).getUnadjustedEndDate();
        case -878940481:  // detachmentDate
          return ((FixedCouponBondPaymentPeriod) bean).getDetachmentDate();
        case 747425396:  // fixedRate
          return ((FixedCouponBondPaymentPeriod) bean).getFixedRate();
        case -1731780257:  // yearFraction
          return ((FixedCouponBondPaymentPeriod) bean).getYearFraction();
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
   * The bean-builder for {@code FixedCouponBondPaymentPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FixedCouponBondPaymentPeriod> {

    private Currency currency;
    private double notional;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate unadjustedStartDate;
    private LocalDate unadjustedEndDate;
    private LocalDate detachmentDate;
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
    private Builder(FixedCouponBondPaymentPeriod beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.unadjustedStartDate = beanToCopy.getUnadjustedStartDate();
      this.unadjustedEndDate = beanToCopy.getUnadjustedEndDate();
      this.detachmentDate = beanToCopy.getDetachmentDate();
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
        case -878940481:  // detachmentDate
          return detachmentDate;
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
        case -878940481:  // detachmentDate
          this.detachmentDate = (LocalDate) newValue;
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
    public FixedCouponBondPaymentPeriod build() {
      return new FixedCouponBondPaymentPeriod(
          currency,
          notional,
          startDate,
          endDate,
          unadjustedStartDate,
          unadjustedEndDate,
          detachmentDate,
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
     * Sets the start date of the payment period.
     * <p>
     * This is the first date in the period.
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
     * Sets the end date of the payment period.
     * <p>
     * This is the last date in the period.
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
     * Sets the detachment date.
     * <p>
     * Some bonds trade ex-coupon before the coupon payment.
     * The coupon is paid not to the owner of the bond on the payment date but to the
     * owner of the bond on the detachment date.
     * <p>
     * When building, this will default to the end date if not specified.
     * @param detachmentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder detachmentDate(LocalDate detachmentDate) {
      JodaBeanUtils.notNull(detachmentDate, "detachmentDate");
      this.detachmentDate = detachmentDate;
      return this;
    }

    /**
     * Sets the fixed coupon rate.
     * <p>
     * The single payment is based on this fixed coupon rate.
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
     * The year fraction of a bond period is based on the unadjusted dates.
     * <p>
     * The value is usually calculated using a {@link DayCount}.
     * Typically the value will be close to 1 for one year and close to 0.5 for six months.
     * The fraction may be greater than 1, but not less than 0.
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
      StringBuilder buf = new StringBuilder(320);
      buf.append("FixedCouponBondPaymentPeriod.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ');
      buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate)).append(',').append(' ');
      buf.append("detachmentDate").append('=').append(JodaBeanUtils.toString(detachmentDate)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
