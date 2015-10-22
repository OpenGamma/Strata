/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
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

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.Convention;
import com.opengamma.strata.finance.TradeInfo;

/**
 * A market convention for term deposit trades.
 * <p>
 * This defines the market convention for a term deposit.
 * <p>
 * The convention is defined by three dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Start date or spot date, the date on which the deposit starts, typically 2 business days after the trade date
 * <li>End date, the date on which the deposit ends, typically a number of months after the start date
 * </ul>
 * The period between the start date and the end date is specified by {@link TermDepositTemplate},
 * not by this convention.
 */
@BeanDefinition
public final class TermDepositConvention
    implements Convention, ImmutableBean, Serializable {

  /**
   * The primary currency.
   * <p>
   * This is the currency of the term deposit and the currency that payment is made in.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The business day adjustment to apply to the start and end date.
   * <p>
   * The start and end date will be adjusted as defined here.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days.
   * The start date of the term deposit is equal to the spot date 
   * and the end date of the term deposit is relative to the start date.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment spotDateOffset;

  //-----------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified currency, business day adjustment,
   * day count convention and spot date offset.
   * 
   * @param currency  the currency, in which the payments are made
   * @param businessDayAdjustment the business day adjustment to apply to the start and end date
   * @param dayCount the day count convention, used to convert dates to a numerical value
   * @param spotDateOffset the offset of the spot value date from the trade date
   * @return the convention
   */
  public static TermDepositConvention of(
      Currency currency,
      BusinessDayAdjustment businessDayAdjustment,
      DayCount dayCount,
      DaysAdjustment spotDateOffset) {
    
    return TermDepositConvention.builder()
        .currency(currency)
        .businessDayAdjustment(businessDayAdjustment)
        .dayCount(dayCount)
        .spotDateOffset(spotDateOffset)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a template based on this convention, specifying the period from start to end.
   * <p>
   * This returns a template based on this convention.
   * The period from the start date to the end date is specified.
   * 
   * @param depositPeriod  the period from the start date to the end date
   * @return the template
   */
  public TermDepositTemplate toTemplate(Period depositPeriod) {
    return TermDepositTemplate.of(depositPeriod, this);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified deposit period.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the term deposit, the principal is paid at the start date and the
   * principal plus interest is received at the end date.
   * If selling the term deposit, the principal is received at the start date and the
   * principal plus interest is paid at the end date.
   * 
   * @param tradeDate  the date of the trade
   * @param depositPeriod  the period between the start date and the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param rate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public TermDepositTrade toTrade(
      LocalDate tradeDate,
      Period depositPeriod,
      BuySell buySell,
      double notional,
      double rate) {

    LocalDate startDate = getSpotDateOffset().adjust(tradeDate);
    LocalDate endDate = startDate.plus(depositPeriod);
    return toTrade(tradeDate, startDate, endDate, buySell, notional, rate);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the term deposit, the principal is paid at the start date and the
   * principal plus interest is received at the end date.
   * If selling the term deposit, the principal is received at the start date and the
   * principal plus interest is paid at the end date.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param rate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public TermDepositTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double rate) {

    ArgChecker.inOrderOrEqual(tradeDate, startDate, "tradeDate", "startDate");
    return TermDepositTrade.builder()
        .tradeInfo(TradeInfo.builder()
            .tradeDate(tradeDate)
            .build())
        .product(TermDeposit.builder()
            .buySell(buySell)
            .currency(currency)
            .notional(notional)
            .startDate(startDate)
            .endDate(endDate)
            .businessDayAdjustment(businessDayAdjustment)
            .rate(rate)
            .dayCount(dayCount)
            .build())
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code TermDepositConvention}.
   * @return the meta-bean, not null
   */
  public static TermDepositConvention.Meta meta() {
    return TermDepositConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(TermDepositConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static TermDepositConvention.Builder builder() {
    return new TermDepositConvention.Builder();
  }

  private TermDepositConvention(
      Currency currency,
      BusinessDayAdjustment businessDayAdjustment,
      DayCount dayCount,
      DaysAdjustment spotDateOffset) {
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
    this.currency = currency;
    this.businessDayAdjustment = businessDayAdjustment;
    this.dayCount = dayCount;
    this.spotDateOffset = spotDateOffset;
  }

  @Override
  public TermDepositConvention.Meta metaBean() {
    return TermDepositConvention.Meta.INSTANCE;
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
   * Gets the primary currency.
   * <p>
   * This is the currency of the term deposit and the currency that payment is made in.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the start and end date.
   * <p>
   * The start and end date will be adjusted as defined here.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days.
   * The start date of the term deposit is equal to the spot date
   * and the end date of the term deposit is relative to the start date.
   * @return the value of the property, not null
   */
  public DaysAdjustment getSpotDateOffset() {
    return spotDateOffset;
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
      TermDepositConvention other = (TermDepositConvention) obj;
      return JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getBusinessDayAdjustment(), other.getBusinessDayAdjustment()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getSpotDateOffset(), other.getSpotDateOffset());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBusinessDayAdjustment());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSpotDateOffset());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("TermDepositConvention{");
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(getBusinessDayAdjustment()).append(',').append(' ');
    buf.append("dayCount").append('=').append(getDayCount()).append(',').append(' ');
    buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(getSpotDateOffset()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code TermDepositConvention}.
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
        this, "currency", TermDepositConvention.class, Currency.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", TermDepositConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", TermDepositConvention.class, DayCount.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", TermDepositConvention.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "businessDayAdjustment",
        "dayCount",
        "spotDateOffset");

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
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public TermDepositConvention.Builder builder() {
      return new TermDepositConvention.Builder();
    }

    @Override
    public Class<? extends TermDepositConvention> beanType() {
      return TermDepositConvention.class;
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
     * The meta-property for the {@code spotDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> spotDateOffset() {
      return spotDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((TermDepositConvention) bean).getCurrency();
        case -1065319863:  // businessDayAdjustment
          return ((TermDepositConvention) bean).getBusinessDayAdjustment();
        case 1905311443:  // dayCount
          return ((TermDepositConvention) bean).getDayCount();
        case 746995843:  // spotDateOffset
          return ((TermDepositConvention) bean).getSpotDateOffset();
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
   * The bean-builder for {@code TermDepositConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<TermDepositConvention> {

    private Currency currency;
    private BusinessDayAdjustment businessDayAdjustment;
    private DayCount dayCount;
    private DaysAdjustment spotDateOffset;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(TermDepositConvention beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
      this.dayCount = beanToCopy.getDayCount();
      this.spotDateOffset = beanToCopy.getSpotDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
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
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 746995843:  // spotDateOffset
          this.spotDateOffset = (DaysAdjustment) newValue;
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
    public TermDepositConvention build() {
      return new TermDepositConvention(
          currency,
          businessDayAdjustment,
          dayCount,
          spotDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the primary currency.
     * <p>
     * This is the currency of the term deposit and the currency that payment is made in.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start and end date.
     * <p>
     * The start and end date will be adjusted as defined here.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the day count convention.
     * <p>
     * This is used to convert dates to a numerical value.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the offset of the spot value date from the trade date.
     * <p>
     * The offset is applied to the trade date and is typically plus 2 business days.
     * The start date of the term deposit is equal to the spot date
     * and the end date of the term deposit is relative to the start date.
     * @param spotDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("TermDepositConvention.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
