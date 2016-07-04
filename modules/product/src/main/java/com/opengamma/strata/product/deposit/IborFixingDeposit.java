/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * An Ibor fixing deposit.
 * <p>
 * An Ibor fixing deposit is a fictitious financial instrument that provides a floating rate of interest on
 * notional amount for a specific term, which is effectively an exchange of a fixed rate and a floating rate 
 * based on an Ibor index on the term end date.
 * <p>
 * For example, an Ibor fixing deposit involves the exchange of the difference between
 * the fixed rate of 1% and the 'GBP-LIBOR-3M' rate for the principal in 3 months time.
 */
@BeanDefinition
public final class IborFixingDeposit
    implements Product, Resolvable<ResolvedIborFixingDeposit>, ImmutableBean, Serializable {

  /**
   * Whether the Ibor fixing deposit is 'Buy' or 'Sell'.
   * <p>
   * A value of 'Buy' implies that the floating rate is paid to the counterparty, with the fixed rate being received.
   * A value of 'Sell' implies that the floating rate is received from the counterparty, with the fixed rate being paid.
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySell;
  /**
   * The primary currency, defaulted to the currency of the index.
   * <p>
   * This is the currency of the deposit and the currency that payment is made in.
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
   * The notional expressed here must be non-negative.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double notional;
  /**
   * The start date of the deposit.
   * <p>
   * Interest accrues from this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date of the deposit.
   * <p>
   * Interest accrues until this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * This date must be after the start date.
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
   * The Ibor index.
   * <p>
   * The floating rate to be paid or received is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * <p>
   * See {@code buySell} to determine whether this rate is paid or received.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
  /**
   * The offset of the fixing date from the start date.
   * <p>
   * The offset is applied to the start date and is typically minus 2 business days.
   * The data model permits the offset to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * When building, this will default to the fixing date offset of the index if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment fixingDateOffset;
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
   * The fixed interest rate to be paid.
   * A 5% rate will be expressed as 0.05.
   */
  @PropertyDefinition
  private final double fixedRate;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null) {
      if (builder.dayCount == null) {
        builder.dayCount = builder.index.getDayCount();
      }
      if (builder.fixingDateOffset == null) {
        builder.fixingDateOffset = builder.index.getFixingDateOffset();
      }
      if (builder.currency == null) {
        builder.currency = builder.index.getCurrency();
      }
    }
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedIborFixingDeposit resolve(ReferenceData refData) {
    DateAdjuster bda = getBusinessDayAdjustment().orElse(BusinessDayAdjustment.NONE).resolve(refData);
    LocalDate start = bda.adjust(startDate);
    LocalDate end = bda.adjust(endDate);
    double yearFraction = dayCount.yearFraction(start, end);
    LocalDate fixingDate = fixingDateOffset.adjust(startDate, refData);
    return ResolvedIborFixingDeposit.builder()
        .startDate(start)
        .endDate(end)
        .yearFraction(yearFraction)
        .currency(getCurrency())
        .notional(buySell.normalize(notional))
        .floatingRate(IborRateComputation.of(index, fixingDate, refData))
        .fixedRate(fixedRate)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFixingDeposit}.
   * @return the meta-bean, not null
   */
  public static IborFixingDeposit.Meta meta() {
    return IborFixingDeposit.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFixingDeposit.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFixingDeposit.Builder builder() {
    return new IborFixingDeposit.Builder();
  }

  private IborFixingDeposit(
      BuySell buySell,
      Currency currency,
      double notional,
      LocalDate startDate,
      LocalDate endDate,
      BusinessDayAdjustment businessDayAdjustment,
      IborIndex index,
      DaysAdjustment fixingDateOffset,
      DayCount dayCount,
      double fixedRate) {
    JodaBeanUtils.notNull(buySell, "buySell");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegative(notional, "notional");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.buySell = buySell;
    this.currency = currency;
    this.notional = notional;
    this.startDate = startDate;
    this.endDate = endDate;
    this.businessDayAdjustment = businessDayAdjustment;
    this.index = index;
    this.fixingDateOffset = fixingDateOffset;
    this.dayCount = dayCount;
    this.fixedRate = fixedRate;
    validate();
  }

  @Override
  public IborFixingDeposit.Meta metaBean() {
    return IborFixingDeposit.Meta.INSTANCE;
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
   * Gets whether the Ibor fixing deposit is 'Buy' or 'Sell'.
   * <p>
   * A value of 'Buy' implies that the floating rate is paid to the counterparty, with the fixed rate being received.
   * A value of 'Sell' implies that the floating rate is received from the counterparty, with the fixed rate being paid.
   * @return the value of the property, not null
   */
  public BuySell getBuySell() {
    return buySell;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency, defaulted to the currency of the index.
   * <p>
   * This is the currency of the deposit and the currency that payment is made in.
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
   * The notional expressed here must be non-negative.
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of the deposit.
   * <p>
   * Interest accrues from this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the deposit.
   * <p>
   * Interest accrues until this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * This date must be after the start date.
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
   * Gets the Ibor index.
   * <p>
   * The floating rate to be paid or received is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * <p>
   * See {@code buySell} to determine whether this rate is paid or received.
   * @return the value of the property, not null
   */
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the fixing date from the start date.
   * <p>
   * The offset is applied to the start date and is typically minus 2 business days.
   * The data model permits the offset to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * When building, this will default to the fixing date offset of the index if not specified.
   * @return the value of the property, not null
   */
  public DaysAdjustment getFixingDateOffset() {
    return fixingDateOffset;
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
   * Gets the fixed interest rate to be paid.
   * A 5% rate will be expressed as 0.05.
   * @return the value of the property
   */
  public double getFixedRate() {
    return fixedRate;
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
      IborFixingDeposit other = (IborFixingDeposit) obj;
      return JodaBeanUtils.equal(buySell, other.buySell) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(fixingDateOffset, other.fixingDateOffset) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(fixedRate, other.fixedRate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(buySell);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedRate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("IborFixingDeposit{");
    buf.append("buySell").append('=').append(buySell).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("fixingDateOffset").append('=').append(fixingDateOffset).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFixingDeposit}.
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
        this, "buySell", IborFixingDeposit.class, BuySell.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IborFixingDeposit.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", IborFixingDeposit.class, Double.TYPE);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", IborFixingDeposit.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", IborFixingDeposit.class, LocalDate.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", IborFixingDeposit.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", IborFixingDeposit.class, IborIndex.class);
    /**
     * The meta-property for the {@code fixingDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> fixingDateOffset = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffset", IborFixingDeposit.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", IborFixingDeposit.class, DayCount.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", IborFixingDeposit.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "buySell",
        "currency",
        "notional",
        "startDate",
        "endDate",
        "businessDayAdjustment",
        "index",
        "fixingDateOffset",
        "dayCount",
        "fixedRate");

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
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 100346066:  // index
          return index;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
        case 747425396:  // fixedRate
          return fixedRate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFixingDeposit.Builder builder() {
      return new IborFixingDeposit.Builder();
    }

    @Override
    public Class<? extends IborFixingDeposit> beanType() {
      return IborFixingDeposit.class;
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
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code fixingDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> fixingDateOffset() {
      return fixingDateOffset;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return ((IborFixingDeposit) bean).getBuySell();
        case 575402001:  // currency
          return ((IborFixingDeposit) bean).getCurrency();
        case 1585636160:  // notional
          return ((IborFixingDeposit) bean).getNotional();
        case -2129778896:  // startDate
          return ((IborFixingDeposit) bean).getStartDate();
        case -1607727319:  // endDate
          return ((IborFixingDeposit) bean).getEndDate();
        case -1065319863:  // businessDayAdjustment
          return ((IborFixingDeposit) bean).businessDayAdjustment;
        case 100346066:  // index
          return ((IborFixingDeposit) bean).getIndex();
        case 873743726:  // fixingDateOffset
          return ((IborFixingDeposit) bean).getFixingDateOffset();
        case 1905311443:  // dayCount
          return ((IborFixingDeposit) bean).getDayCount();
        case 747425396:  // fixedRate
          return ((IborFixingDeposit) bean).getFixedRate();
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
   * The bean-builder for {@code IborFixingDeposit}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFixingDeposit> {

    private BuySell buySell;
    private Currency currency;
    private double notional;
    private LocalDate startDate;
    private LocalDate endDate;
    private BusinessDayAdjustment businessDayAdjustment;
    private IborIndex index;
    private DaysAdjustment fixingDateOffset;
    private DayCount dayCount;
    private double fixedRate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborFixingDeposit beanToCopy) {
      this.buySell = beanToCopy.getBuySell();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.businessDayAdjustment = beanToCopy.businessDayAdjustment;
      this.index = beanToCopy.getIndex();
      this.fixingDateOffset = beanToCopy.getFixingDateOffset();
      this.dayCount = beanToCopy.getDayCount();
      this.fixedRate = beanToCopy.getFixedRate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return buySell;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 100346066:  // index
          return index;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
        case 747425396:  // fixedRate
          return fixedRate;
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
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case 873743726:  // fixingDateOffset
          this.fixingDateOffset = (DaysAdjustment) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
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
    public IborFixingDeposit build() {
      preBuild(this);
      return new IborFixingDeposit(
          buySell,
          currency,
          notional,
          startDate,
          endDate,
          businessDayAdjustment,
          index,
          fixingDateOffset,
          dayCount,
          fixedRate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the Ibor fixing deposit is 'Buy' or 'Sell'.
     * <p>
     * A value of 'Buy' implies that the floating rate is paid to the counterparty, with the fixed rate being received.
     * A value of 'Sell' implies that the floating rate is received from the counterparty, with the fixed rate being paid.
     * @param buySell  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySell(BuySell buySell) {
      JodaBeanUtils.notNull(buySell, "buySell");
      this.buySell = buySell;
      return this;
    }

    /**
     * Sets the primary currency, defaulted to the currency of the index.
     * <p>
     * This is the currency of the deposit and the currency that payment is made in.
     * The data model permits this currency to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * When building, this will default to the currency of the index if not specified.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the notional amount.
     * <p>
     * The notional expressed here must be non-negative.
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
     * Sets the start date of the deposit.
     * <p>
     * Interest accrues from this date.
     * This date is typically set to be a valid business day.
     * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date of the deposit.
     * <p>
     * Interest accrues until this date.
     * This date is typically set to be a valid business day.
     * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
     * This date must be after the start date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start and end date, optional.
     * <p>
     * The start and end date are typically defined as valid business days and thus
     * do not need to be adjusted. If this optional property is present, then the
     * start and end date will be adjusted as defined here.
     * @param businessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the Ibor index.
     * <p>
     * The floating rate to be paid or received is based on this index
     * It will be a well known market index such as 'GBP-LIBOR-3M'.
     * <p>
     * See {@code buySell} to determine whether this rate is paid or received.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the offset of the fixing date from the start date.
     * <p>
     * The offset is applied to the start date and is typically minus 2 business days.
     * The data model permits the offset to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * When building, this will default to the fixing date offset of the index if not specified.
     * @param fixingDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDateOffset(DaysAdjustment fixingDateOffset) {
      JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset");
      this.fixingDateOffset = fixingDateOffset;
      return this;
    }

    /**
     * Sets the day count convention applicable, defaulted to the day count of the index.
     * <p>
     * This is used to convert dates to a numerical value.
     * The data model permits the day count to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * When building, this will default to the day count of the index if not specified.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the fixed interest rate to be paid.
     * A 5% rate will be expressed as 0.05.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(double fixedRate) {
      this.fixedRate = fixedRate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("IborFixingDeposit.Builder{");
      buf.append("buySell").append('=').append(JodaBeanUtils.toString(buySell)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
