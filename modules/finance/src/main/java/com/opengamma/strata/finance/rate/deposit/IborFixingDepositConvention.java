/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.Convention;
import com.opengamma.strata.finance.TradeInfo;

/**
 * A convention for Ibor fixing deposit trades.
 * <p>
 * This defines the convention for an Ibor fixing deposit against a particular index.
 * In most cases, the index contains sufficient information to fully define the convention.
 * As such, no other fields need to be specified when creating an instance.
 * The getters will default any missing information on the fly, avoiding both null and {@link Optional}.
 * <p>
 * The convention is defined by four dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Start date or spot date, the date on which the deposit starts, typically 2 business days after the trade date
 * <li>End date, the date on which deposit ends, typically a number of months after the start date
 * <li>Fixing date, the date on which the index is to be observed, typically 2 business days before the start date
 * </ul>
 * The period between the start date and end date is specified by {@link IborFixingDepositTemplate},
 * not by this convention. However, the period is typically equal to the tenor of the index. 
 */
@BeanDefinition
public final class IborFixingDepositConvention
    implements Convention, ImmutableBean, Serializable {

  /**
   * The Ibor index.
   * <p>
   * The floating rate to be paid or received is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
  /**
   * The primary currency, optional with defaulting getter.
   * <p>
   * This is the currency of the deposit and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the currency of the index if not specified.
   */
  @PropertyDefinition(get = "field")
  private final Currency currency;
  /**
   * The day count convention applicable, optional with defaulting getter.
   * <p>
   * This is used to convert dates to a numerical value.
   * The data model permits the day count to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the day count of the index if not specified.
   */
  @PropertyDefinition(get = "field")
  private final DayCount dayCount;
  /**
   * The offset of the spot value date from the trade date, optional with defaulting getter.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days.
   * The start date of the term deposit is equal to the spot date 
   * and the end date of the term deposit is relative to the start date.
   * <p>
   * This will default to the effective date offset of the index if not specified.
   */
  @PropertyDefinition(get = "field")
  private final DaysAdjustment spotDateOffset;
  /**
   * The business day adjustment to apply to the start and end date, optional with defaulting getter.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   * <p>
   * This will default to 'ModifiedFollowing' using the index fixing calendar if not specified.
   */
  @PropertyDefinition(get = "field")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The offset of the fixing date from the start date, optional with defaulting getter.
   * <p>
   * The offset is applied to the start date and is typically minus 2 business days.
   * The data model permits the offset to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the fixing date offset of the index if not specified.
   */
  @PropertyDefinition(get = "field")
  private final DaysAdjustment fixingDateOffset;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified index.
   * <p>
   * The standard convention for an Ibor fixing deposit is based exclusively on the index.
   * Use the {@linkplain #builder() builder} for unusual conventions.
   * 
   * @param index  the index, the standard convention values are extracted from the index
   * @return the convention
   */
  public static IborFixingDepositConvention of(IborIndex index) {
    return IborFixingDepositConvention.builder()
        .index(index)
        .build();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency,
   * providing a default result if no override specified.
   * <p>
   * This is the currency of the Ibor fixing deposit and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the currency of the index if not specified.
   * 
   * @return the currency, not null
   */
  public Currency getCurrency() {
    return currency != null ? currency : index.getCurrency();
  }

  /**
   * Gets the day count convention applicable,
   * providing a default result if no override specified.
   * <p>
   * This is used to convert dates to a numerical value.
   * The data model permits the day count to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the day count of the index if not specified.
   * 
   * @return the day count, not null
   */
  public DayCount getDayCount() {
    return dayCount != null ? dayCount : index.getDayCount();
  }

  /**
   * Gets the offset of the spot value date from the trade date,
   * providing a default result if no override specified.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days.
   * The start and end date of the term are relative to the spot date.
   * <p>
   * This will default to the effective date offset of the index if not specified.
   * 
   * @return the spot date offset, not null
   */
  public DaysAdjustment getSpotDateOffset() {
    return spotDateOffset != null ? spotDateOffset : index.getEffectiveDateOffset();
  }

  /**
   * Gets the business day adjustment to apply to the start and end date,
   * providing a default result if no override specified.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   * <p>
   * This will default to 'ModifiedFollowing' using the index fixing calendar if not specified.
   * 
   * @return the business day adjustment, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment != null ?
        businessDayAdjustment : BusinessDayAdjustment.of(MODIFIED_FOLLOWING, index.getFixingCalendar());
  }

  /**
   * Gets the offset of the fixing date from the start date,
   * providing a default result if no override specified.
   * <p>
   * The offset is applied to the start date and is typically minus 2 business days.
   * The data model permits the offset to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the fixing date offset of the index if not specified.
   * 
   * @return the fixing date offset, not null
   */
  public DaysAdjustment getFixingDateOffset() {
    return fixingDateOffset != null ? fixingDateOffset : index.getFixingDateOffset();
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this convention, returning an instance where all the optional fields are present.
   * <p>
   * This returns an equivalent instance where any empty optional have been filled in.
   * 
   * @return the expanded convention
   */
  public IborFixingDepositConvention expand() {
    return IborFixingDepositConvention.builder()
        .index(index)
        .currency(getCurrency())
        .spotDateOffset(getSpotDateOffset())
        .businessDayAdjustment(getBusinessDayAdjustment())
        .fixingDateOffset(getFixingDateOffset())
        .dayCount(getDayCount())
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a template based on this convention.
   * <p>
   * This returns a template based on this convention.
   * The period from the start date to the end date will be the tenor of the index.
   * 
   * @return the template
   */
  public IborFixingDepositTemplate toTemplate() {
    return toTemplate(index.getTenor().getPeriod());
  }

  /**
   * Creates a template based on this convention, specifying the period from start to end.
   * <p>
   * This returns a template based on this convention.
   * The period from the start date to the end date is specified.
   * 
   * @param depositPeriod  the period from the start date to the end date
   * @return the template
   */
  public IborFixingDepositTemplate toTemplate(Period depositPeriod) {
    return IborFixingDepositTemplate.of(depositPeriod, this);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified deposit period.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the Ibor fixing deposit, the floating rate is paid from the counterparty, with the fixed rate being received.
   * If selling the Ibor fixing deposit, the floating received is paid to the counterparty, with the fixed rate being paid.
   * 
   * @param tradeDate  the date of the trade
   * @param depositPeriod  the period between the start date and the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public IborFixingDepositTrade toTrade(
      LocalDate tradeDate,
      Period depositPeriod,
      BuySell buySell,
      double notional,
      double fixedRate) {

    LocalDate startDate = getSpotDateOffset().adjust(tradeDate);
    LocalDate endDate = startDate.plus(depositPeriod);
    return toTrade(tradeDate, startDate, endDate, buySell, notional, fixedRate);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the Ibor fixing deposit, the floating rate is paid from the counterparty, with the fixed rate being received.
   * If selling the Ibor fixing deposit, the floating received is paid to the counterparty, with the fixed rate being paid.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public IborFixingDepositTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate) {

    ArgChecker.inOrderOrEqual(tradeDate, startDate, "tradeDate", "startDate");
    return IborFixingDepositTrade.builder()
        .tradeInfo(TradeInfo.builder()
            .tradeDate(tradeDate)
            .build())
        .product(IborFixingDeposit.builder()
            .buySell(buySell)
            .currency(getCurrency())
            .notional(notional)
            .startDate(startDate)
            .endDate(endDate)
            .businessDayAdjustment(getBusinessDayAdjustment())
            .fixedRate(fixedRate)
            .index(index)
            .fixingDateOffset(getFixingDateOffset())
            .dayCount(getDayCount())
            .build())
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFixingDepositConvention}.
   * @return the meta-bean, not null
   */
  public static IborFixingDepositConvention.Meta meta() {
    return IborFixingDepositConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFixingDepositConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFixingDepositConvention.Builder builder() {
    return new IborFixingDepositConvention.Builder();
  }

  private IborFixingDepositConvention(
      IborIndex index,
      Currency currency,
      DayCount dayCount,
      DaysAdjustment spotDateOffset,
      BusinessDayAdjustment businessDayAdjustment,
      DaysAdjustment fixingDateOffset) {
    JodaBeanUtils.notNull(index, "index");
    this.index = index;
    this.currency = currency;
    this.dayCount = dayCount;
    this.spotDateOffset = spotDateOffset;
    this.businessDayAdjustment = businessDayAdjustment;
    this.fixingDateOffset = fixingDateOffset;
  }

  @Override
  public IborFixingDepositConvention.Meta metaBean() {
    return IborFixingDepositConvention.Meta.INSTANCE;
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
   * Gets the Ibor index.
   * <p>
   * The floating rate to be paid or received is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public IborIndex getIndex() {
    return index;
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
      IborFixingDepositConvention other = (IborFixingDepositConvention) obj;
      return JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(fixingDateOffset, other.fixingDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDateOffset);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("IborFixingDepositConvention{");
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("spotDateOffset").append('=').append(spotDateOffset).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFixingDepositConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", IborFixingDepositConvention.class, IborIndex.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IborFixingDepositConvention.class, Currency.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", IborFixingDepositConvention.class, DayCount.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", IborFixingDepositConvention.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", IborFixingDepositConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code fixingDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> fixingDateOffset = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffset", IborFixingDepositConvention.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "currency",
        "dayCount",
        "spotDateOffset",
        "businessDayAdjustment",
        "fixingDateOffset");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 575402001:  // currency
          return currency;
        case 1905311443:  // dayCount
          return dayCount;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFixingDepositConvention.Builder builder() {
      return new IborFixingDepositConvention.Builder();
    }

    @Override
    public Class<? extends IborFixingDepositConvention> beanType() {
      return IborFixingDepositConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code spotDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> spotDateOffset() {
      return spotDateOffset;
    }

    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code fixingDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> fixingDateOffset() {
      return fixingDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((IborFixingDepositConvention) bean).getIndex();
        case 575402001:  // currency
          return ((IborFixingDepositConvention) bean).currency;
        case 1905311443:  // dayCount
          return ((IborFixingDepositConvention) bean).dayCount;
        case 746995843:  // spotDateOffset
          return ((IborFixingDepositConvention) bean).spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return ((IborFixingDepositConvention) bean).businessDayAdjustment;
        case 873743726:  // fixingDateOffset
          return ((IborFixingDepositConvention) bean).fixingDateOffset;
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
   * The bean-builder for {@code IborFixingDepositConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFixingDepositConvention> {

    private IborIndex index;
    private Currency currency;
    private DayCount dayCount;
    private DaysAdjustment spotDateOffset;
    private BusinessDayAdjustment businessDayAdjustment;
    private DaysAdjustment fixingDateOffset;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborFixingDepositConvention beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.currency = beanToCopy.currency;
      this.dayCount = beanToCopy.dayCount;
      this.spotDateOffset = beanToCopy.spotDateOffset;
      this.businessDayAdjustment = beanToCopy.businessDayAdjustment;
      this.fixingDateOffset = beanToCopy.fixingDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 575402001:  // currency
          return currency;
        case 1905311443:  // dayCount
          return dayCount;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 746995843:  // spotDateOffset
          this.spotDateOffset = (DaysAdjustment) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 873743726:  // fixingDateOffset
          this.fixingDateOffset = (DaysAdjustment) newValue;
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
    public IborFixingDepositConvention build() {
      return new IborFixingDepositConvention(
          index,
          currency,
          dayCount,
          spotDateOffset,
          businessDayAdjustment,
          fixingDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Ibor index.
     * <p>
     * The floating rate to be paid or received is based on this index
     * It will be a well known market index such as 'GBP-LIBOR-3M'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the primary currency, optional with defaulting getter.
     * <p>
     * This is the currency of the deposit and the currency that payment is made in.
     * The data model permits this currency to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * This will default to the currency of the index if not specified.
     * @param currency  the new value
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      this.currency = currency;
      return this;
    }

    /**
     * Sets the day count convention applicable, optional with defaulting getter.
     * <p>
     * This is used to convert dates to a numerical value.
     * The data model permits the day count to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * This will default to the day count of the index if not specified.
     * @param dayCount  the new value
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the offset of the spot value date from the trade date, optional with defaulting getter.
     * <p>
     * The offset is applied to the trade date and is typically plus 2 business days.
     * The start date of the term deposit is equal to the spot date
     * and the end date of the term deposit is relative to the start date.
     * <p>
     * This will default to the effective date offset of the index if not specified.
     * @param spotDateOffset  the new value
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start and end date, optional with defaulting getter.
     * <p>
     * The start and end date are typically defined as valid business days and thus
     * do not need to be adjusted. If this optional property is present, then the
     * start and end date will be adjusted as defined here.
     * <p>
     * This will default to 'ModifiedFollowing' using the index fixing calendar if not specified.
     * @param businessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the offset of the fixing date from the start date, optional with defaulting getter.
     * <p>
     * The offset is applied to the start date and is typically minus 2 business days.
     * The data model permits the offset to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * This will default to the fixing date offset of the index if not specified.
     * @param fixingDateOffset  the new value
     * @return this, for chaining, not null
     */
    public Builder fixingDateOffset(DaysAdjustment fixingDateOffset) {
      this.fixingDateOffset = fixingDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("IborFixingDepositConvention.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
