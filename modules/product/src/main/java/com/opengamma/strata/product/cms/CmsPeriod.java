/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapIndex;

/**
 * A period over which a CMS coupon or CMS caplet/floorlet payoff is paid.
 * <p>
 * This represents a single payment period within a CMS leg.
 * This class specifies the data necessary to calculate the value of the period.
 * The payment period contains the unique accrual period.
 * The value of the period is based on the observed value of {@code SwapIndex}.
 * <p>
 * The payment is a CMS coupon, CMS caplet or CMS floorlet.
 * The pay-offs are, for a swap index on the fixingDate of 'S' and an year fraction 'a'<br>
 * CMS Coupon: a * S<br>
 * CMS Caplet: a * (S-K)^+ ; K=caplet<br>
 * CMS Floorlet: a * (K-S)^+ ; K=floorlet
 * <p>
 * If {@code caplet} ({@code floorlet}) is not null, the payment is a caplet (floorlet).
 * If both of {@code caplet} and {@code floorlet} are null, this class represents a CMS coupon payment.
 * Thus at least one of the fields must be null.
 * <p>
 * A {@code CmsPeriod} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class CmsPeriod
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
   * The notional amount, positive if receiving, negative if paying.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition
  private final double notional;
  /**
   * The start date of the payment period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date of the payment period.
   * <p>
   * This is the last date in the period.
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
   * The year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount} which may be different to that of the index.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double yearFraction;
  /**
   * The date that payment occurs.
   * <p>
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;
  /**
   * The date of the index fixing.
   * <p>
   * This is an adjusted date with any business day applied.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;
  /**
   * The optional caplet strike.
   * <p>
   * This defines the strike value of a caplet.
   * <p>
   * If the period is not a caplet, this field will be absent.
   */
  @PropertyDefinition(get = "optional")
  private final Double caplet;
  /**
   * The optional floorlet strike.
   * <p>
   * This defines the strike value of a floorlet.
   * <p>
   * If the period is not a floorlet, this field will be absent.
   */
  @PropertyDefinition(get = "optional")
  private final Double floorlet;
  /**
   * The day count of the period.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The swap index.
   * <p>
   * The swap rate to be paid is the observed value of this index.
   */
  @PropertyDefinition(validate = "notNull")
  private final SwapIndex index;
  /**
   * The underlying swap.
   * <p>
   * The interest rate swap for which the swap rate is referred.
   */
  @PropertyDefinition(validate = "notNull")
  private final ResolvedSwap underlyingSwap;

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private CmsPeriod(
      Currency currency,
      double notional,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      double yearFraction,
      LocalDate paymentDate,
      LocalDate fixingDate,
      Double caplet,
      Double floorlet,
      DayCount dayCount,
      SwapIndex index,
      ResolvedSwap underlyingSwap) {

    this.index = ArgChecker.notNull(index, "index");
    this.currency = ArgChecker.notNull(currency, "currency");
    this.notional = notional;
    this.startDate = ArgChecker.notNull(startDate, "startDate");
    this.endDate = ArgChecker.notNull(endDate, "endDate");
    this.unadjustedStartDate = firstNonNull(unadjustedStartDate, startDate);
    this.unadjustedEndDate = firstNonNull(unadjustedEndDate, endDate);
    this.yearFraction = ArgChecker.notNegative(yearFraction, "yearFraction");
    this.paymentDate = ArgChecker.notNull(paymentDate, "paymentDate");
    this.fixingDate = ArgChecker.notNull(fixingDate, "fixingDate");
    this.caplet = caplet;
    this.floorlet = floorlet;
    this.dayCount = ArgChecker.notNull(dayCount, "dayCount");
    this.underlyingSwap = ArgChecker.notNull(underlyingSwap, "underlyingSwap");
    ArgChecker.inOrderNotEqual(this.startDate, this.endDate, "startDate", "endDate");
    ArgChecker.inOrderNotEqual(
        this.unadjustedStartDate, this.unadjustedEndDate, "unadjustedStartDate", "unadjustedEndDate");
    ArgChecker.isFalse(this.getCaplet().isPresent() && this.getFloorlet().isPresent(),
        "At least one of cap and floor should be null");
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the type of the CMS period.
   * <p>
   * The period type is caplet, floorlet or coupon.
   * 
   * @return the CMS period type
   */
  public CmsPeriodType getCmsPeriodType() {
    if (getCaplet().isPresent()) {
      return CmsPeriodType.CAPLET;
    } else if (getFloorlet().isPresent()) {
      return CmsPeriodType.FLOORLET;
    } else {
      return CmsPeriodType.COUPON;
    }
  }

  /**
   * Obtains the strike value.
   * <p>
   * If the CMS period type is coupon, 0 is returned.
   * 
   * @return the strike value
   */
  public double getStrike() {
    CmsPeriodType type = getCmsPeriodType();
    if (type.equals(CmsPeriodType.CAPLET)) {
      return caplet;
    }
    if (type.equals(CmsPeriodType.FLOORLET)) {
      return floorlet;
    }
    return 0d;
  }

  /**
   * Return the CMS coupon equivalent to the period.
   * <p>
   * For cap or floor the result is the coupon with the same dates and index but with no cap or floor strike.
   * 
   * @return  the CMS coupon
   */
  public CmsPeriod toCouponEquivalent() {
    return this.toBuilder().floorlet(null).caplet(null).build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CmsPeriod}.
   * @return the meta-bean, not null
   */
  public static CmsPeriod.Meta meta() {
    return CmsPeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CmsPeriod.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CmsPeriod.Builder builder() {
    return new CmsPeriod.Builder();
  }

  @Override
  public CmsPeriod.Meta metaBean() {
    return CmsPeriod.Meta.INSTANCE;
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
   * Gets the notional amount, positive if receiving, negative if paying.
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
   * Gets the year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount} which may be different to that of the index.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that payment occurs.
   * <p>
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date of the index fixing.
   * <p>
   * This is an adjusted date with any business day applied.
   * @return the value of the property, not null
   */
  public LocalDate getFixingDate() {
    return fixingDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional caplet strike.
   * <p>
   * This defines the strike value of a caplet.
   * <p>
   * If the period is not a caplet, this field will be absent.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getCaplet() {
    return caplet != null ? OptionalDouble.of(caplet) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional floorlet strike.
   * <p>
   * This defines the strike value of a floorlet.
   * <p>
   * If the period is not a floorlet, this field will be absent.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getFloorlet() {
    return floorlet != null ? OptionalDouble.of(floorlet) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count of the period.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the swap index.
   * <p>
   * The swap rate to be paid is the observed value of this index.
   * @return the value of the property, not null
   */
  public SwapIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying swap.
   * <p>
   * The interest rate swap for which the swap rate is referred.
   * @return the value of the property, not null
   */
  public ResolvedSwap getUnderlyingSwap() {
    return underlyingSwap;
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
      CmsPeriod other = (CmsPeriod) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(unadjustedStartDate, other.unadjustedStartDate) &&
          JodaBeanUtils.equal(unadjustedEndDate, other.unadjustedEndDate) &&
          JodaBeanUtils.equal(yearFraction, other.yearFraction) &&
          JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(fixingDate, other.fixingDate) &&
          JodaBeanUtils.equal(caplet, other.caplet) &&
          JodaBeanUtils.equal(floorlet, other.floorlet) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(underlyingSwap, other.underlyingSwap);
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
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(caplet);
    hash = hash * 31 + JodaBeanUtils.hashCode(floorlet);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingSwap);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(480);
    buf.append("CmsPeriod{");
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("unadjustedStartDate").append('=').append(unadjustedStartDate).append(',').append(' ');
    buf.append("unadjustedEndDate").append('=').append(unadjustedEndDate).append(',').append(' ');
    buf.append("yearFraction").append('=').append(yearFraction).append(',').append(' ');
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("fixingDate").append('=').append(fixingDate).append(',').append(' ');
    buf.append("caplet").append('=').append(caplet).append(',').append(' ');
    buf.append("floorlet").append('=').append(floorlet).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("underlyingSwap").append('=').append(JodaBeanUtils.toString(underlyingSwap));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CmsPeriod}.
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
        this, "currency", CmsPeriod.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", CmsPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", CmsPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", CmsPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedStartDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedStartDate", CmsPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedEndDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedEndDate", CmsPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", CmsPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", CmsPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", CmsPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code caplet} property.
     */
    private final MetaProperty<Double> caplet = DirectMetaProperty.ofImmutable(
        this, "caplet", CmsPeriod.class, Double.class);
    /**
     * The meta-property for the {@code floorlet} property.
     */
    private final MetaProperty<Double> floorlet = DirectMetaProperty.ofImmutable(
        this, "floorlet", CmsPeriod.class, Double.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", CmsPeriod.class, DayCount.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<SwapIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", CmsPeriod.class, SwapIndex.class);
    /**
     * The meta-property for the {@code underlyingSwap} property.
     */
    private final MetaProperty<ResolvedSwap> underlyingSwap = DirectMetaProperty.ofImmutable(
        this, "underlyingSwap", CmsPeriod.class, ResolvedSwap.class);
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
        "yearFraction",
        "paymentDate",
        "fixingDate",
        "caplet",
        "floorlet",
        "dayCount",
        "index",
        "underlyingSwap");

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
        case -1731780257:  // yearFraction
          return yearFraction;
        case -1540873516:  // paymentDate
          return paymentDate;
        case 1255202043:  // fixingDate
          return fixingDate;
        case -1367656183:  // caplet
          return caplet;
        case 2022994575:  // floorlet
          return floorlet;
        case 1905311443:  // dayCount
          return dayCount;
        case 100346066:  // index
          return index;
        case 1497421456:  // underlyingSwap
          return underlyingSwap;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CmsPeriod.Builder builder() {
      return new CmsPeriod.Builder();
    }

    @Override
    public Class<? extends CmsPeriod> beanType() {
      return CmsPeriod.class;
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
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    /**
     * The meta-property for the {@code fixingDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> fixingDate() {
      return fixingDate;
    }

    /**
     * The meta-property for the {@code caplet} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> caplet() {
      return caplet;
    }

    /**
     * The meta-property for the {@code floorlet} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> floorlet() {
      return floorlet;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwapIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code underlyingSwap} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ResolvedSwap> underlyingSwap() {
      return underlyingSwap;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((CmsPeriod) bean).getCurrency();
        case 1585636160:  // notional
          return ((CmsPeriod) bean).getNotional();
        case -2129778896:  // startDate
          return ((CmsPeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((CmsPeriod) bean).getEndDate();
        case 1457691881:  // unadjustedStartDate
          return ((CmsPeriod) bean).getUnadjustedStartDate();
        case 31758114:  // unadjustedEndDate
          return ((CmsPeriod) bean).getUnadjustedEndDate();
        case -1731780257:  // yearFraction
          return ((CmsPeriod) bean).getYearFraction();
        case -1540873516:  // paymentDate
          return ((CmsPeriod) bean).getPaymentDate();
        case 1255202043:  // fixingDate
          return ((CmsPeriod) bean).getFixingDate();
        case -1367656183:  // caplet
          return ((CmsPeriod) bean).caplet;
        case 2022994575:  // floorlet
          return ((CmsPeriod) bean).floorlet;
        case 1905311443:  // dayCount
          return ((CmsPeriod) bean).getDayCount();
        case 100346066:  // index
          return ((CmsPeriod) bean).getIndex();
        case 1497421456:  // underlyingSwap
          return ((CmsPeriod) bean).getUnderlyingSwap();
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
   * The bean-builder for {@code CmsPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CmsPeriod> {

    private Currency currency;
    private double notional;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate unadjustedStartDate;
    private LocalDate unadjustedEndDate;
    private double yearFraction;
    private LocalDate paymentDate;
    private LocalDate fixingDate;
    private Double caplet;
    private Double floorlet;
    private DayCount dayCount;
    private SwapIndex index;
    private ResolvedSwap underlyingSwap;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CmsPeriod beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.unadjustedStartDate = beanToCopy.getUnadjustedStartDate();
      this.unadjustedEndDate = beanToCopy.getUnadjustedEndDate();
      this.yearFraction = beanToCopy.getYearFraction();
      this.paymentDate = beanToCopy.getPaymentDate();
      this.fixingDate = beanToCopy.getFixingDate();
      this.caplet = beanToCopy.caplet;
      this.floorlet = beanToCopy.floorlet;
      this.dayCount = beanToCopy.getDayCount();
      this.index = beanToCopy.getIndex();
      this.underlyingSwap = beanToCopy.getUnderlyingSwap();
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
        case -1731780257:  // yearFraction
          return yearFraction;
        case -1540873516:  // paymentDate
          return paymentDate;
        case 1255202043:  // fixingDate
          return fixingDate;
        case -1367656183:  // caplet
          return caplet;
        case 2022994575:  // floorlet
          return floorlet;
        case 1905311443:  // dayCount
          return dayCount;
        case 100346066:  // index
          return index;
        case 1497421456:  // underlyingSwap
          return underlyingSwap;
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
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
          break;
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
          break;
        case -1367656183:  // caplet
          this.caplet = (Double) newValue;
          break;
        case 2022994575:  // floorlet
          this.floorlet = (Double) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 100346066:  // index
          this.index = (SwapIndex) newValue;
          break;
        case 1497421456:  // underlyingSwap
          this.underlyingSwap = (ResolvedSwap) newValue;
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
    public CmsPeriod build() {
      return new CmsPeriod(
          currency,
          notional,
          startDate,
          endDate,
          unadjustedStartDate,
          unadjustedEndDate,
          yearFraction,
          paymentDate,
          fixingDate,
          caplet,
          floorlet,
          dayCount,
          index,
          underlyingSwap);
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
     * Sets the notional amount, positive if receiving, negative if paying.
     * <p>
     * The notional amount applicable during the period.
     * The currency of the notional is specified by {@code currency}.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
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
     * Sets the year fraction that the accrual period represents.
     * <p>
     * The value is usually calculated using a {@link DayCount} which may be different to that of the index.
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

    /**
     * Sets the date that payment occurs.
     * <p>
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

    /**
     * Sets the date of the index fixing.
     * <p>
     * This is an adjusted date with any business day applied.
     * @param fixingDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDate(LocalDate fixingDate) {
      JodaBeanUtils.notNull(fixingDate, "fixingDate");
      this.fixingDate = fixingDate;
      return this;
    }

    /**
     * Sets the optional caplet strike.
     * <p>
     * This defines the strike value of a caplet.
     * <p>
     * If the period is not a caplet, this field will be absent.
     * @param caplet  the new value
     * @return this, for chaining, not null
     */
    public Builder caplet(Double caplet) {
      this.caplet = caplet;
      return this;
    }

    /**
     * Sets the optional floorlet strike.
     * <p>
     * This defines the strike value of a floorlet.
     * <p>
     * If the period is not a floorlet, this field will be absent.
     * @param floorlet  the new value
     * @return this, for chaining, not null
     */
    public Builder floorlet(Double floorlet) {
      this.floorlet = floorlet;
      return this;
    }

    /**
     * Sets the day count of the period.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the swap index.
     * <p>
     * The swap rate to be paid is the observed value of this index.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(SwapIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the underlying swap.
     * <p>
     * The interest rate swap for which the swap rate is referred.
     * @param underlyingSwap  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlyingSwap(ResolvedSwap underlyingSwap) {
      JodaBeanUtils.notNull(underlyingSwap, "underlyingSwap");
      this.underlyingSwap = underlyingSwap;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(480);
      buf.append("CmsPeriod.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ');
      buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate)).append(',').append(' ');
      buf.append("caplet").append('=').append(JodaBeanUtils.toString(caplet)).append(',').append(' ');
      buf.append("floorlet").append('=').append(JodaBeanUtils.toString(floorlet)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("underlyingSwap").append('=').append(JodaBeanUtils.toString(underlyingSwap));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
