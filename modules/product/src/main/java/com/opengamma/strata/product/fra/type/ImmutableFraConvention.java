/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra.type;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.NZD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.product.fra.FraDiscountingMethod.AFMA;
import static com.opengamma.strata.product.fra.FraDiscountingMethod.ISDA;

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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraDiscountingMethod;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * A market convention for forward rate agreement (FRA) trades.
 * <p>
 * This defines the market convention for a FRA against a particular index.
 * In most cases, the index contains sufficient information to fully define the convention.
 * As such, no other fields need to be specified when creating an instance.
 * The name of the convention is the same as the name of the index by default.
 * The getters will default any missing information on the fly, avoiding both null and {@link Optional}.
 * <p>
 * The convention is defined by six dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Spot date, the base for date calculations, typically 2 business days after the trade date
 * <li>Start date, the date on which the implied deposit starts, typically a number of months after the spot date
 * <li>End date, the date on which the implied deposit ends, typically a number of months after the spot date
 * <li>Fixing date, the date on which the index is to be observed, typically 2 business days before the start date
 * <li>Payment date, the date on which payment is made, typically the same as the start date
 * </ul>
 * The period between the spot date and the start/end date is specified by {@link FraTemplate}, not by this convention.
 */
@BeanDefinition
public final class ImmutableFraConvention
    implements FraConvention, ImmutableBean, Serializable {

  /**
   * The Ibor index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborIndex index;
  /**
   * The convention name, such as 'GBP-LIBOR-3M', optional with defaulting getter.
   * <p>
   * This will default to the name of the index if not specified.
   */
  @PropertyDefinition(get = "field")
  private final String name;
  /**
   * The primary currency, optional with defaulting getter.
   * <p>
   * This is the currency of the FRA and the currency that payment is made in.
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
   * The start and end date of the FRA term are relative to the spot date.
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
  /**
   * The offset of the payment date from the start date, optional with defaulting getter.
   * <p>
   * Defines the offset from the start date to the payment date.
   * In most cases, the payment date is the same as the start date, so the default of zero is appropriate.
   * <p>
   * This will default to zero if not specified.
   */
  @PropertyDefinition(get = "field")
  private final DaysAdjustment paymentDateOffset;
  /**
   * The method to use for discounting, optional with defaulting getter.
   * <p>
   * There are different approaches FRA pricing in the area of discounting.
   * This method specifies the approach for this FRA.
   * <p>
   * This will default 'AFMA' if the index has the currency
   * 'AUD' or 'NZD' and to 'ISDA' otherwise.
   */
  @PropertyDefinition(get = "field")
  private final FraDiscountingMethod discounting;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified index.
   * <p>
   * The standard market convention for a FRA is based exclusively on the index.
   * This creates an instance that contains the index.
   * The instance is not dereferenced using the {@code FraConvention} name, as such
   * the result of this method and {@link FraConvention#of(IborIndex)} can differ.
   * <p>
   * Use the {@linkplain #builder() builder} for unusual conventions.
   * 
   * @param index  the index, the market convention values are extracted from the index
   * @return the convention
   */
  public static ImmutableFraConvention of(IborIndex index) {
    return ImmutableFraConvention.builder()
        .index(index)
        .build();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the convention name, such as 'GBP-LIBOR-3M'.
   * This is the same as the name of the index by default.
   * <p>
   * This will default to the name of the index if not specified.
   * 
   * @return the convention name
   */
  @Override
  public String getName() {
    return name != null ? name : index.getName();
  }

  /**
   * Gets the primary currency,
   * providing a default result if no override specified.
   * <p>
   * This is the currency of the FRA and the currency that payment is made in.
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
   * The start and end date of the FRA term are relative to the spot date.
   * <p>
   * This will default to the effective date offset of the index if not specified.
   * 
   * @return the spot date offset, not null
   */
  @Override
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
        businessDayAdjustment :
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, index.getFixingCalendar());
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

  /**
   * Gets the offset of the payment date from the start date,
   * providing a default result if no override specified.
   * <p>
   * Defines the offset from the start date to the payment date.
   * In most cases, the payment date is the same as the start date, so the default of zero is appropriate.
   * <p>
   * This will default to zero if not specified.
   * 
   * @return the payment date offset, not null
   */
  public DaysAdjustment getPaymentDateOffset() {
    return paymentDateOffset != null ? paymentDateOffset : DaysAdjustment.NONE;
  }

  /**
   * Gets the method to use for discounting,
   * providing a default result if no override specified.
   * <p>
   * There are different approaches FRA pricing in the area of discounting.
   * This method specifies the approach for this FRA.
   * <p>
   * This will default 'AFMA' if the index has the currency
   * 'AUD' or 'NZD' and to 'ISDA' otherwise.
   * 
   * @return the discounting method, not null
   */
  public FraDiscountingMethod getDiscounting() {
    if (discounting != null) {
      return discounting;
    }
    Currency indexCcy = index.getCurrency();
    return indexCcy.equals(AUD) || indexCcy.equals(NZD) ? AFMA : ISDA;
  }

  //-------------------------------------------------------------------------
  @Override
  public FraTrade createTrade(
      LocalDate tradeDate,
      Period periodToStart,
      Period periodToEnd,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    LocalDate spotValue = calculateSpotDateFromTradeDate(tradeDate, refData);
    LocalDate startDate = spotValue.plus(periodToStart);
    LocalDate endDate = spotValue.plus(periodToEnd);
    DateAdjuster bda = getBusinessDayAdjustment().resolve(refData);
    // start/end dates are adjusted when FRA trade is created and not adjusted later
    // payment date is adjusted when FRA trade is created and potentially adjusted again when resolving
    LocalDate adjustedStart = bda.adjust(startDate);
    LocalDate adjustedEnd = bda.adjust(endDate);
    LocalDate adjustedPay = getPaymentDateOffset().adjust(adjustedStart, refData);
    return toTrade(tradeDate, adjustedStart, adjustedEnd, adjustedPay, buySell, notional, fixedRate);
  }

  @Override
  public FraTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate paymentDate,
      BuySell buySell,
      double notional,
      double fixedRate) {

    Optional<LocalDate> tradeDate = tradeInfo.getTradeDate();
    if (tradeDate.isPresent()) {
      ArgChecker.inOrderOrEqual(tradeDate.get(), startDate, "tradeDate", "startDate");
    }
    // business day adjustment is not passed through as start/end date are fixed at
    // trade creation and should not be adjusted later
    return FraTrade.builder()
        .info(tradeInfo)
        .product(Fra.builder()
            .buySell(buySell)
            .currency(getCurrency())
            .notional(notional)
            .startDate(startDate)
            .endDate(endDate)
            .paymentDate(AdjustableDate.of(paymentDate, getPaymentDateOffset().getAdjustment()))
            .fixedRate(fixedRate)
            .index(index)
            .fixingDateOffset(getFixingDateOffset())
            .dayCount(getDayCount())
            .discounting(getDiscounting())
            .build())
        .build();
  }

  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableFraConvention}.
   * @return the meta-bean, not null
   */
  public static ImmutableFraConvention.Meta meta() {
    return ImmutableFraConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableFraConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableFraConvention.Builder builder() {
    return new ImmutableFraConvention.Builder();
  }

  private ImmutableFraConvention(
      IborIndex index,
      String name,
      Currency currency,
      DayCount dayCount,
      DaysAdjustment spotDateOffset,
      BusinessDayAdjustment businessDayAdjustment,
      DaysAdjustment fixingDateOffset,
      DaysAdjustment paymentDateOffset,
      FraDiscountingMethod discounting) {
    JodaBeanUtils.notNull(index, "index");
    this.index = index;
    this.name = name;
    this.currency = currency;
    this.dayCount = dayCount;
    this.spotDateOffset = spotDateOffset;
    this.businessDayAdjustment = businessDayAdjustment;
    this.fixingDateOffset = fixingDateOffset;
    this.paymentDateOffset = paymentDateOffset;
    this.discounting = discounting;
  }

  @Override
  public ImmutableFraConvention.Meta metaBean() {
    return ImmutableFraConvention.Meta.INSTANCE;
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
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  @Override
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
      ImmutableFraConvention other = (ImmutableFraConvention) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(fixingDateOffset, other.fixingDateOffset) &&
          JodaBeanUtils.equal(paymentDateOffset, other.paymentDateOffset) &&
          JodaBeanUtils.equal(discounting, other.discounting);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(discounting);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableFraConvention}.
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
        this, "index", ImmutableFraConvention.class, IborIndex.class);
    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", ImmutableFraConvention.class, String.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ImmutableFraConvention.class, Currency.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ImmutableFraConvention.class, DayCount.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", ImmutableFraConvention.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", ImmutableFraConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code fixingDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> fixingDateOffset = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffset", ImmutableFraConvention.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code paymentDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> paymentDateOffset = DirectMetaProperty.ofImmutable(
        this, "paymentDateOffset", ImmutableFraConvention.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code discounting} property.
     */
    private final MetaProperty<FraDiscountingMethod> discounting = DirectMetaProperty.ofImmutable(
        this, "discounting", ImmutableFraConvention.class, FraDiscountingMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "name",
        "currency",
        "dayCount",
        "spotDateOffset",
        "businessDayAdjustment",
        "fixingDateOffset",
        "paymentDateOffset",
        "discounting");

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
        case 3373707:  // name
          return name;
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
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
        case -536441087:  // discounting
          return discounting;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableFraConvention.Builder builder() {
      return new ImmutableFraConvention.Builder();
    }

    @Override
    public Class<? extends ImmutableFraConvention> beanType() {
      return ImmutableFraConvention.class;
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
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
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

    /**
     * The meta-property for the {@code paymentDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> paymentDateOffset() {
      return paymentDateOffset;
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
        case 100346066:  // index
          return ((ImmutableFraConvention) bean).getIndex();
        case 3373707:  // name
          return ((ImmutableFraConvention) bean).name;
        case 575402001:  // currency
          return ((ImmutableFraConvention) bean).currency;
        case 1905311443:  // dayCount
          return ((ImmutableFraConvention) bean).dayCount;
        case 746995843:  // spotDateOffset
          return ((ImmutableFraConvention) bean).spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return ((ImmutableFraConvention) bean).businessDayAdjustment;
        case 873743726:  // fixingDateOffset
          return ((ImmutableFraConvention) bean).fixingDateOffset;
        case -716438393:  // paymentDateOffset
          return ((ImmutableFraConvention) bean).paymentDateOffset;
        case -536441087:  // discounting
          return ((ImmutableFraConvention) bean).discounting;
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
   * The bean-builder for {@code ImmutableFraConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableFraConvention> {

    private IborIndex index;
    private String name;
    private Currency currency;
    private DayCount dayCount;
    private DaysAdjustment spotDateOffset;
    private BusinessDayAdjustment businessDayAdjustment;
    private DaysAdjustment fixingDateOffset;
    private DaysAdjustment paymentDateOffset;
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
    private Builder(ImmutableFraConvention beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.name = beanToCopy.name;
      this.currency = beanToCopy.currency;
      this.dayCount = beanToCopy.dayCount;
      this.spotDateOffset = beanToCopy.spotDateOffset;
      this.businessDayAdjustment = beanToCopy.businessDayAdjustment;
      this.fixingDateOffset = beanToCopy.fixingDateOffset;
      this.paymentDateOffset = beanToCopy.paymentDateOffset;
      this.discounting = beanToCopy.discounting;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 3373707:  // name
          return name;
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
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
        case -536441087:  // discounting
          return discounting;
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
        case 3373707:  // name
          this.name = (String) newValue;
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
        case -716438393:  // paymentDateOffset
          this.paymentDateOffset = (DaysAdjustment) newValue;
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
    public ImmutableFraConvention build() {
      return new ImmutableFraConvention(
          index,
          name,
          currency,
          dayCount,
          spotDateOffset,
          businessDayAdjustment,
          fixingDateOffset,
          paymentDateOffset,
          discounting);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Ibor index.
     * <p>
     * The floating rate to be paid is based on this index
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
     * Sets the convention name, such as 'GBP-LIBOR-3M', optional with defaulting getter.
     * <p>
     * This will default to the name of the index if not specified.
     * @param name  the new value
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the primary currency, optional with defaulting getter.
     * <p>
     * This is the currency of the FRA and the currency that payment is made in.
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
     * The start and end date of the FRA term are relative to the spot date.
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

    /**
     * Sets the offset of the payment date from the start date, optional with defaulting getter.
     * <p>
     * Defines the offset from the start date to the payment date.
     * In most cases, the payment date is the same as the start date, so the default of zero is appropriate.
     * <p>
     * This will default to zero if not specified.
     * @param paymentDateOffset  the new value
     * @return this, for chaining, not null
     */
    public Builder paymentDateOffset(DaysAdjustment paymentDateOffset) {
      this.paymentDateOffset = paymentDateOffset;
      return this;
    }

    /**
     * Sets the method to use for discounting, optional with defaulting getter.
     * <p>
     * There are different approaches FRA pricing in the area of discounting.
     * This method specifies the approach for this FRA.
     * <p>
     * This will default 'AFMA' if the index has the currency
     * 'AUD' or 'NZD' and to 'ISDA' otherwise.
     * @param discounting  the new value
     * @return this, for chaining, not null
     */
    public Builder discounting(FraDiscountingMethod discounting) {
      this.discounting = discounting;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("ImmutableFraConvention.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset)).append(',').append(' ');
      buf.append("paymentDateOffset").append('=').append(JodaBeanUtils.toString(paymentDateOffset)).append(',').append(' ');
      buf.append("discounting").append('=').append(JodaBeanUtils.toString(discounting));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
