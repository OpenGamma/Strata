/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate.fra;

import static com.opengamma.platform.finance.rate.fra.FraDiscountingMethod.AFMA;
import static com.opengamma.platform.finance.rate.fra.FraDiscountingMethod.ISDA;
import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.NZD;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.platform.finance.rate.IborInterpolatedRateObservation;
import com.opengamma.platform.finance.rate.IborRateObservation;
import com.opengamma.platform.finance.rate.RateObservation;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A forward rate agreement (FRA).
 * <p>
 * A FRA is a financial instrument that represents the one off exchange of a fixed
 * rate of interest for a floating rate at a future date.
 * <p>
 * For example, a FRA might involve an agreement to exchange the difference between
 * the fixed rate of 1% and the 'GBP-LIBOR-3M' rate in 2 months time.
 */
@BeanDefinition
public final class Fra
    implements FraProduct, ImmutableBean, Serializable {

  /**
   * Whether the FRA is buy or sell.
   * <p>
   * A value of 'Buy' implies that the floating rate is received from the counterparty,
   * with the fixed rate being paid. A value of 'Sell' implies that the floating rate
   * is paid to the counterparty, with the fixed rate being received.
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySell;
  /**
   * The date that payment occurs.
   * <p>
   * The date that payment is made when settling the FRA with support for business day adjustments.
   */
  @PropertyDefinition(validate = "notNull")
  private final AdjustableDate paymentDate;
  /**
   * The start date, which is the effective date of the FRA.
   * <p>
   * This is the first date that interest accrues.
   * <p>
   * This date is typically set to be a valid business day
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date, which is the termination date of the FRA.
   * <p>
   * This is the last day that interest accrues.
   * This date must be after the start date.
   * <p>
   * This date is typically set to be a valid business day
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The business day adjustment to apply to the start and end date, optional.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   */
  @PropertyDefinition(get = "optional")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The day count convention applicable, defaulted to the day count of the index.
   * <p>
   * This is used to convert dates to a numerical value.
   * The data model permits the day count to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * When building, this will default to the day count of the index if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The fixed rate of interest.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * See {@code buySell} to determine whether this rate is paid or received.
   */
  @PropertyDefinition
  private final double fixedRate;
  /**
   * The IBOR-like index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * This will be used throughout unless {@code indexInterpolated} is present.
   * <p>
   * See {@code buySell} to determine whether this rate is paid or received.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
  /**
   * The second IBOR-like index to be used for linear interpolation, optional.
   * <p>
   * This will be used with {@code index} to linearly interpolate the rate.
   * It will be a well known market index such as 'GBP-LIBOR-6M'.
   * This index may be shorter or longer than {@code index}, but not the same.
   */
  @PropertyDefinition(get = "optional")
  private final IborIndex indexInterpolated;
  /**
   * The offset of the fixing date from the start date.
   * <p>
   * The offset is applied to the start date of the FRA.
   * The offset is typically a negative number of business days.
   * The data model permits the fixing offset to differ from that of the index,
   * however the two are typically the same.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment fixingOffset;
  /**
   * The primary currency, defaulted to the currency of the index.
   * <p>
   * This is the currency of the FRA and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * When building, this will default to the currency of the index if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount.
   * <p>
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double notional;
  /**
   * The method to use for discounting, defaulted to 'ISDA' or 'AFMA'.
   * <p>
   * There are different approaches FRA pricing in the area of discounting.
   * This method specifies the approach for this FRA.
   * <p>
   * When building, this will default 'AFMA' if the index has the currency
   * 'AUD' or 'NZD' and to 'ISDA' otherwise.
   */
  @PropertyDefinition(validate = "notNull")
  private final FraDiscountingMethod discounting;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null) {
      if (builder.dayCount == null) {
        builder.dayCount = builder.index.getDayCount();
      }
      if (builder.currency == null) {
        builder.currency = builder.index.getCurrency();
      }
      if (builder.discounting == null) {
        Currency curr = builder.index.getCurrency();
        builder.discounting = (curr.equals(AUD) || curr.equals(NZD) ? AFMA : ISDA);
      }
    }
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    if (index.equals(indexInterpolated)) {
      throw new IllegalArgumentException("Interpolation requires two different indices");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this FRA.
   * <p>
   * Expanding a FRA causes the dates to be adjusted according to the relevant
   * holiday calendar. Other one-off calculations may also be performed.
   * 
   * @return the equivalent expanded FRA
   * @throws RuntimeException if unable to expand due to an invalid definition
   */
  @Override
  public ExpandedFra expand() {
    LocalDate start = getBusinessDayAdjustment().orElse(BusinessDayAdjustment.NONE).adjust(startDate);
    LocalDate end = getBusinessDayAdjustment().orElse(BusinessDayAdjustment.NONE).adjust(endDate);
    return ExpandedFra.builder()
        .paymentDate(paymentDate.adjusted())
        .startDate(start)
        .endDate(end)
        .yearFraction(dayCount.yearFraction(start, end))
        .fixedRate(fixedRate)
        .floatingRate(createRateObservation())
        .currency(currency)
        .notional(buySell.normalize(notional))
        .discounting(discounting)
        .build();
  }

  // creates an Ibor or IborInterpolated observation
  private RateObservation createRateObservation() {
    LocalDate fixingDate = fixingOffset.adjust(startDate);
    if (indexInterpolated != null) {
      return IborInterpolatedRateObservation.of(index, indexInterpolated, fixingDate);
    } else {
      return IborRateObservation.of(index, fixingDate);
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Fra}.
   * @return the meta-bean, not null
   */
  public static Fra.Meta meta() {
    return Fra.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Fra.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Fra.Builder builder() {
    return new Fra.Builder();
  }

  private Fra(
      BuySell buySell,
      AdjustableDate paymentDate,
      LocalDate startDate,
      LocalDate endDate,
      BusinessDayAdjustment businessDayAdjustment,
      DayCount dayCount,
      double fixedRate,
      IborIndex index,
      IborIndex indexInterpolated,
      DaysAdjustment fixingOffset,
      Currency currency,
      double notional,
      FraDiscountingMethod discounting) {
    JodaBeanUtils.notNull(buySell, "buySell");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingOffset, "fixingOffset");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegative(notional, "notional");
    JodaBeanUtils.notNull(discounting, "discounting");
    this.buySell = buySell;
    this.paymentDate = paymentDate;
    this.startDate = startDate;
    this.endDate = endDate;
    this.businessDayAdjustment = businessDayAdjustment;
    this.dayCount = dayCount;
    this.fixedRate = fixedRate;
    this.index = index;
    this.indexInterpolated = indexInterpolated;
    this.fixingOffset = fixingOffset;
    this.currency = currency;
    this.notional = notional;
    this.discounting = discounting;
    validate();
  }

  @Override
  public Fra.Meta metaBean() {
    return Fra.Meta.INSTANCE;
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
   * Gets whether the FRA is buy or sell.
   * <p>
   * A value of 'Buy' implies that the floating rate is received from the counterparty,
   * with the fixed rate being paid. A value of 'Sell' implies that the floating rate
   * is paid to the counterparty, with the fixed rate being received.
   * @return the value of the property, not null
   */
  public BuySell getBuySell() {
    return buySell;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that payment occurs.
   * <p>
   * The date that payment is made when settling the FRA with support for business day adjustments.
   * @return the value of the property, not null
   */
  public AdjustableDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date, which is the effective date of the FRA.
   * <p>
   * This is the first date that interest accrues.
   * <p>
   * This date is typically set to be a valid business day
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date, which is the termination date of the FRA.
   * <p>
   * This is the last day that interest accrues.
   * This date must be after the start date.
   * <p>
   * This date is typically set to be a valid business day
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the start and end date, optional.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   * @return the optional value of the property, not null
   */
  public Optional<BusinessDayAdjustment> getBusinessDayAdjustment() {
    return Optional.ofNullable(businessDayAdjustment);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention applicable, defaulted to the day count of the index.
   * <p>
   * This is used to convert dates to a numerical value.
   * The data model permits the day count to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * When building, this will default to the day count of the index if not specified.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed rate of interest.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * See {@code buySell} to determine whether this rate is paid or received.
   * @return the value of the property
   */
  public double getFixedRate() {
    return fixedRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the IBOR-like index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * This will be used throughout unless {@code indexInterpolated} is present.
   * <p>
   * See {@code buySell} to determine whether this rate is paid or received.
   * @return the value of the property, not null
   */
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the second IBOR-like index to be used for linear interpolation, optional.
   * <p>
   * This will be used with {@code index} to linearly interpolate the rate.
   * It will be a well known market index such as 'GBP-LIBOR-6M'.
   * This index may be shorter or longer than {@code index}, but not the same.
   * @return the optional value of the property, not null
   */
  public Optional<IborIndex> getIndexInterpolated() {
    return Optional.ofNullable(indexInterpolated);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the fixing date from the start date.
   * <p>
   * The offset is applied to the start date of the FRA.
   * The offset is typically a negative number of business days.
   * The data model permits the fixing offset to differ from that of the index,
   * however the two are typically the same.
   * @return the value of the property, not null
   */
  public DaysAdjustment getFixingOffset() {
    return fixingOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency, defaulted to the currency of the index.
   * <p>
   * This is the currency of the FRA and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * When building, this will default to the currency of the index if not specified.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount.
   * <p>
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method to use for discounting, defaulted to 'ISDA' or 'AFMA'.
   * <p>
   * There are different approaches FRA pricing in the area of discounting.
   * This method specifies the approach for this FRA.
   * <p>
   * When building, this will default 'AFMA' if the index has the currency
   * 'AUD' or 'NZD' and to 'ISDA' otherwise.
   * @return the value of the property, not null
   */
  public FraDiscountingMethod getDiscounting() {
    return discounting;
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
      Fra other = (Fra) obj;
      return JodaBeanUtils.equal(getBuySell(), other.getBuySell()) &&
          JodaBeanUtils.equal(getPaymentDate(), other.getPaymentDate()) &&
          JodaBeanUtils.equal(getStartDate(), other.getStartDate()) &&
          JodaBeanUtils.equal(getEndDate(), other.getEndDate()) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getFixedRate(), other.getFixedRate()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(indexInterpolated, other.indexInterpolated) &&
          JodaBeanUtils.equal(getFixingOffset(), other.getFixingOffset()) &&
          JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getDiscounting(), other.getDiscounting());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getBuySell());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStartDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getEndDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixedRate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(indexInterpolated);
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixingOffset());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDiscounting());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(448);
    buf.append("Fra{");
    buf.append("buySell").append('=').append(getBuySell()).append(',').append(' ');
    buf.append("paymentDate").append('=').append(getPaymentDate()).append(',').append(' ');
    buf.append("startDate").append('=').append(getStartDate()).append(',').append(' ');
    buf.append("endDate").append('=').append(getEndDate()).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("dayCount").append('=').append(getDayCount()).append(',').append(' ');
    buf.append("fixedRate").append('=').append(getFixedRate()).append(',').append(' ');
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("indexInterpolated").append('=').append(indexInterpolated).append(',').append(' ');
    buf.append("fixingOffset").append('=').append(getFixingOffset()).append(',').append(' ');
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("notional").append('=').append(getNotional()).append(',').append(' ');
    buf.append("discounting").append('=').append(JodaBeanUtils.toString(getDiscounting()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Fra}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code buySell} property.
     */
    private final MetaProperty<BuySell> buySell = DirectMetaProperty.ofImmutable(
        this, "buySell", Fra.class, BuySell.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<AdjustableDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", Fra.class, AdjustableDate.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", Fra.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", Fra.class, LocalDate.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", Fra.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", Fra.class, DayCount.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", Fra.class, Double.TYPE);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", Fra.class, IborIndex.class);
    /**
     * The meta-property for the {@code indexInterpolated} property.
     */
    private final MetaProperty<IborIndex> indexInterpolated = DirectMetaProperty.ofImmutable(
        this, "indexInterpolated", Fra.class, IborIndex.class);
    /**
     * The meta-property for the {@code fixingOffset} property.
     */
    private final MetaProperty<DaysAdjustment> fixingOffset = DirectMetaProperty.ofImmutable(
        this, "fixingOffset", Fra.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", Fra.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", Fra.class, Double.TYPE);
    /**
     * The meta-property for the {@code discounting} property.
     */
    private final MetaProperty<FraDiscountingMethod> discounting = DirectMetaProperty.ofImmutable(
        this, "discounting", Fra.class, FraDiscountingMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "buySell",
        "paymentDate",
        "startDate",
        "endDate",
        "businessDayAdjustment",
        "dayCount",
        "fixedRate",
        "index",
        "indexInterpolated",
        "fixingOffset",
        "currency",
        "notional",
        "discounting");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return buySell;
        case -1540873516:  // paymentDate
          return paymentDate;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 747425396:  // fixedRate
          return fixedRate;
        case 100346066:  // index
          return index;
        case -1934091915:  // indexInterpolated
          return indexInterpolated;
        case -317508960:  // fixingOffset
          return fixingOffset;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -536441087:  // discounting
          return discounting;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Fra.Builder builder() {
      return new Fra.Builder();
    }

    @Override
    public Class<? extends Fra> beanType() {
      return Fra.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code buySell} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BuySell> buySell() {
      return buySell;
    }

    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDate> paymentDate() {
      return paymentDate;
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
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code fixedRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> fixedRate() {
      return fixedRate;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code indexInterpolated} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> indexInterpolated() {
      return indexInterpolated;
    }

    /**
     * The meta-property for the {@code fixingOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> fixingOffset() {
      return fixingOffset;
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
     * The meta-property for the {@code discounting} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FraDiscountingMethod> discounting() {
      return discounting;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return ((Fra) bean).getBuySell();
        case -1540873516:  // paymentDate
          return ((Fra) bean).getPaymentDate();
        case -2129778896:  // startDate
          return ((Fra) bean).getStartDate();
        case -1607727319:  // endDate
          return ((Fra) bean).getEndDate();
        case -1065319863:  // businessDayAdjustment
          return ((Fra) bean).businessDayAdjustment;
        case 1905311443:  // dayCount
          return ((Fra) bean).getDayCount();
        case 747425396:  // fixedRate
          return ((Fra) bean).getFixedRate();
        case 100346066:  // index
          return ((Fra) bean).getIndex();
        case -1934091915:  // indexInterpolated
          return ((Fra) bean).indexInterpolated;
        case -317508960:  // fixingOffset
          return ((Fra) bean).getFixingOffset();
        case 575402001:  // currency
          return ((Fra) bean).getCurrency();
        case 1585636160:  // notional
          return ((Fra) bean).getNotional();
        case -536441087:  // discounting
          return ((Fra) bean).getDiscounting();
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
   * The bean-builder for {@code Fra}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Fra> {

    private BuySell buySell;
    private AdjustableDate paymentDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BusinessDayAdjustment businessDayAdjustment;
    private DayCount dayCount;
    private double fixedRate;
    private IborIndex index;
    private IborIndex indexInterpolated;
    private DaysAdjustment fixingOffset;
    private Currency currency;
    private double notional;
    private FraDiscountingMethod discounting;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Fra beanToCopy) {
      this.buySell = beanToCopy.getBuySell();
      this.paymentDate = beanToCopy.getPaymentDate();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.businessDayAdjustment = beanToCopy.businessDayAdjustment;
      this.dayCount = beanToCopy.getDayCount();
      this.fixedRate = beanToCopy.getFixedRate();
      this.index = beanToCopy.getIndex();
      this.indexInterpolated = beanToCopy.indexInterpolated;
      this.fixingOffset = beanToCopy.getFixingOffset();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.discounting = beanToCopy.getDiscounting();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return buySell;
        case -1540873516:  // paymentDate
          return paymentDate;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 747425396:  // fixedRate
          return fixedRate;
        case 100346066:  // index
          return index;
        case -1934091915:  // indexInterpolated
          return indexInterpolated;
        case -317508960:  // fixingOffset
          return fixingOffset;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -536441087:  // discounting
          return discounting;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          this.buySell = (BuySell) newValue;
          break;
        case -1540873516:  // paymentDate
          this.paymentDate = (AdjustableDate) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
          break;
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case -1934091915:  // indexInterpolated
          this.indexInterpolated = (IborIndex) newValue;
          break;
        case -317508960:  // fixingOffset
          this.fixingOffset = (DaysAdjustment) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -536441087:  // discounting
          this.discounting = (FraDiscountingMethod) newValue;
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
    public Fra build() {
      preBuild(this);
      return new Fra(
          buySell,
          paymentDate,
          startDate,
          endDate,
          businessDayAdjustment,
          dayCount,
          fixedRate,
          index,
          indexInterpolated,
          fixingOffset,
          currency,
          notional,
          discounting);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code buySell} property in the builder.
     * @param buySell  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySell(BuySell buySell) {
      JodaBeanUtils.notNull(buySell, "buySell");
      this.buySell = buySell;
      return this;
    }

    /**
     * Sets the {@code paymentDate} property in the builder.
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(AdjustableDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

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
     * Sets the {@code businessDayAdjustment} property in the builder.
     * @param businessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the {@code dayCount} property in the builder.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the {@code fixedRate} property in the builder.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(double fixedRate) {
      this.fixedRate = fixedRate;
      return this;
    }

    /**
     * Sets the {@code index} property in the builder.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the {@code indexInterpolated} property in the builder.
     * @param indexInterpolated  the new value
     * @return this, for chaining, not null
     */
    public Builder indexInterpolated(IborIndex indexInterpolated) {
      this.indexInterpolated = indexInterpolated;
      return this;
    }

    /**
     * Sets the {@code fixingOffset} property in the builder.
     * @param fixingOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingOffset(DaysAdjustment fixingOffset) {
      JodaBeanUtils.notNull(fixingOffset, "fixingOffset");
      this.fixingOffset = fixingOffset;
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
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegative(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the {@code discounting} property in the builder.
     * @param discounting  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder discounting(FraDiscountingMethod discounting) {
      JodaBeanUtils.notNull(discounting, "discounting");
      this.discounting = discounting;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(448);
      buf.append("Fra.Builder{");
      buf.append("buySell").append('=').append(JodaBeanUtils.toString(buySell)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("indexInterpolated").append('=').append(JodaBeanUtils.toString(indexInterpolated)).append(',').append(' ');
      buf.append("fixingOffset").append('=').append(JodaBeanUtils.toString(fixingOffset)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("discounting").append('=').append(JodaBeanUtils.toString(discounting));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
