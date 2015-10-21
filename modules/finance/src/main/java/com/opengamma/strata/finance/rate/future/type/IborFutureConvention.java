/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.future.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
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

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DateSequence;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Convention;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.UnitSecurity;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.finance.rate.future.IborFutureTrade;

/**
 * A market convention for Ibor Future trades.
 * <p>
 * This defines the market convention for a future against a particular index.
 * In most cases, the index contains sufficient information to fully define the convention.
 */
@BeanDefinition
public final class IborFutureConvention
    implements Convention, ImmutableBean, Serializable {

  /**
   * The Ibor index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
  /**
   * The sequence of dates that the future is based on.
   * <p>
   * This is used to calculate the reference date of the future that is the start
   * date of the underlying synthetic deposit.
   */
  @PropertyDefinition(validate = "notNull")
  private final DateSequence dateSequence;
  /**
   * The business day adjustment to apply to the reference date.
   * <p>
   * The reference date, which is often the third Wednesday of the month, will be adjusted as defined here.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;

  //-------------------------------------------------------------------------
  /**
   * Creates a convention based on the specified index and the sequence of dates.
   * <p>
   * The standard market convention is based on the index.
   * The business day adjustment is set to be 'Following' using the effective date calendar from the index.
   * 
   * @param index  the index, the calendar for the adjustment is extracted from the index
   * @param dateSequence  the sequence of dates
   * @return the convention
   */
  public static IborFutureConvention of(IborIndex index, DateSequence dateSequence) {
    return IborFutureConvention.builder()
        .index(index)
        .businessDayAdjustment(
            BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, index.getEffectiveDateOffset().getCalendar()))
        .dateSequence(dateSequence)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * 
   * @param tradeDate  the trade date
   * @param minimumPeriod  minimum period between the value date and the first future
   * @param sequenceNumber  the 1-based sequence number of the futures
   * @param quantity  the quantity of contract traded
   * @param notional  the notional amount of one future contract
   * @param price  the trade price of the future
   * @return the trade
   */
  public IborFutureTrade toTrade(
      LocalDate tradeDate,
      Period minimumPeriod,
      int sequenceNumber,
      long quantity,
      double notional,
      double price) {

    LocalDate referenceDate = referenceDate(tradeDate, minimumPeriod, sequenceNumber);
    double accrualFactor = index.getTenor().get(ChronoUnit.MONTHS) / 12.0;
    LocalDate lastTradeDate = index.calculateFixingFromEffective(referenceDate);
    IborFuture underlying = IborFuture.builder()
        .index(index)
        .accrualFactor(accrualFactor)
        .lastTradeDate(lastTradeDate)
        .notional(notional).build();
    YearMonth m = YearMonth.from(lastTradeDate);
    Security<IborFuture> security = UnitSecurity.builder(underlying)
        .standardId(StandardId.of("OG-Ticker", "IborFuture-" + index.getName() + "-" + m.toString()))
        .build();
    SecurityLink<IborFuture> securityLink = SecurityLink.resolved(security);
    TradeInfo info = TradeInfo.builder().tradeDate(tradeDate).build();
    return IborFutureTrade.builder()
        .quantity(quantity).initialPrice(price).securityLink(securityLink).tradeInfo(info).build();
  }

  // determines the reference date from the trade date
  LocalDate referenceDate(LocalDate tradeDate, Period minimumPeriod, int sequenceNumber) {
    LocalDate earliestDate = tradeDate.plus(minimumPeriod);
    LocalDate referenceDate = dateSequence.nthOrSame(earliestDate, sequenceNumber);
    return businessDayAdjustment.adjust(referenceDate);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFutureConvention}.
   * @return the meta-bean, not null
   */
  public static IborFutureConvention.Meta meta() {
    return IborFutureConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFutureConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFutureConvention.Builder builder() {
    return new IborFutureConvention.Builder();
  }

  private IborFutureConvention(
      IborIndex index,
      DateSequence dateSequence,
      BusinessDayAdjustment businessDayAdjustment) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(dateSequence, "dateSequence");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    this.index = index;
    this.dateSequence = dateSequence;
    this.businessDayAdjustment = businessDayAdjustment;
  }

  @Override
  public IborFutureConvention.Meta metaBean() {
    return IborFutureConvention.Meta.INSTANCE;
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
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the sequence of dates that the future is based on.
   * <p>
   * This is used to calculate the reference date of the future that is the start
   * date of the underlying synthetic deposit.
   * @return the value of the property, not null
   */
  public DateSequence getDateSequence() {
    return dateSequence;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the reference date.
   * <p>
   * The reference date, which is often the third Wednesday of the month, will be adjusted as defined here.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
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
      IborFutureConvention other = (IborFutureConvention) obj;
      return JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getDateSequence(), other.getDateSequence()) &&
          JodaBeanUtils.equal(getBusinessDayAdjustment(), other.getBusinessDayAdjustment());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDateSequence());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBusinessDayAdjustment());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("IborFutureConvention{");
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("dateSequence").append('=').append(getDateSequence()).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(getBusinessDayAdjustment()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFutureConvention}.
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
        this, "index", IborFutureConvention.class, IborIndex.class);
    /**
     * The meta-property for the {@code dateSequence} property.
     */
    private final MetaProperty<DateSequence> dateSequence = DirectMetaProperty.ofImmutable(
        this, "dateSequence", IborFutureConvention.class, DateSequence.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", IborFutureConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "dateSequence",
        "businessDayAdjustment");

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
        case -258065009:  // dateSequence
          return dateSequence;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFutureConvention.Builder builder() {
      return new IborFutureConvention.Builder();
    }

    @Override
    public Class<? extends IborFutureConvention> beanType() {
      return IborFutureConvention.class;
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
     * The meta-property for the {@code dateSequence} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DateSequence> dateSequence() {
      return dateSequence;
    }

    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((IborFutureConvention) bean).getIndex();
        case -258065009:  // dateSequence
          return ((IborFutureConvention) bean).getDateSequence();
        case -1065319863:  // businessDayAdjustment
          return ((IborFutureConvention) bean).getBusinessDayAdjustment();
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
   * The bean-builder for {@code IborFutureConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFutureConvention> {

    private IborIndex index;
    private DateSequence dateSequence;
    private BusinessDayAdjustment businessDayAdjustment;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborFutureConvention beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.dateSequence = beanToCopy.getDateSequence();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -258065009:  // dateSequence
          return dateSequence;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
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
        case -258065009:  // dateSequence
          this.dateSequence = (DateSequence) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
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
    public IborFutureConvention build() {
      return new IborFutureConvention(
          index,
          dateSequence,
          businessDayAdjustment);
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
     * Sets the sequence of dates that the future is based on.
     * <p>
     * This is used to calculate the reference date of the future that is the start
     * date of the underlying synthetic deposit.
     * @param dateSequence  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dateSequence(DateSequence dateSequence) {
      JodaBeanUtils.notNull(dateSequence, "dateSequence");
      this.dateSequence = dateSequence;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the reference date.
     * <p>
     * The reference date, which is often the third Wednesday of the month, will be adjusted as defined here.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("IborFutureConvention.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("dateSequence").append('=').append(JodaBeanUtils.toString(dateSequence)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
