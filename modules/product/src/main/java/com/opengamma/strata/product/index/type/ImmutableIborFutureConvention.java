/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;

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
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DateSequence;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.IborFuture;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * A market convention for Ibor Future trades.
 * <p>
 * This defines the market convention for a future against a particular index.
 * In most cases, the index contains sufficient information to fully define the convention.
 */
@BeanDefinition
public final class ImmutableIborFutureConvention
    implements IborFutureConvention, ImmutableBean, Serializable {

  /**
   * The Ibor index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborIndex index;
  /**
   * The convention name, such as 'USD-LIBOR-3M-Quarterly-IMM'.
   * <p>
   * This will default to the name of the index suffixed by the name of the date sequence if not specified.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String name;
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
   * The convention name will default to the name of the index suffixed by the
   * name of the date sequence.
   * 
   * @param index  the index, the calendar for the adjustment is extracted from the index
   * @param dateSequence  the sequence of dates
   * @return the convention
   */
  public static ImmutableIborFutureConvention of(IborIndex index, DateSequence dateSequence) {
    return ImmutableIborFutureConvention.builder()
        .index(index)
        .dateSequence(dateSequence)
        .build();
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null) {
      if (builder.name == null && builder.dateSequence != null) {
        builder.name = builder.index.getName() + "-" + builder.dateSequence.getName();
      }
      if (builder.businessDayAdjustment == null) {
        builder.businessDayAdjustment = BusinessDayAdjustment.of(
            FOLLOWING, builder.index.getEffectiveDateOffset().getCalendar());
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public IborFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      Period minimumPeriod,
      int sequenceNumber,
      double quantity,
      double notional,
      double price,
      ReferenceData refData) {

    LocalDate referenceDate = calculateReferenceDateFromTradeDate(tradeDate, minimumPeriod, sequenceNumber, refData);
    LocalDate lastTradeDate = index.calculateFixingFromEffective(referenceDate, refData);
    YearMonth yearMonth = YearMonth.from(lastTradeDate);
    return createTrade(tradeDate, securityId, quantity, notional, price, yearMonth, lastTradeDate, referenceDate);
  }

  @Override
  public IborFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      YearMonth yearMonth,
      double quantity,
      double notional,
      double price,
      ReferenceData refData) {

    LocalDate referenceDate = calculateReferenceDateFromTradeDate(tradeDate, yearMonth, refData);
    LocalDate lastTradeDate = index.calculateFixingFromEffective(referenceDate, refData);
    return createTrade(tradeDate, securityId, quantity, notional, price, yearMonth, lastTradeDate, referenceDate);
  }

  private IborFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      double quantity,
      double notional,
      double price,
      YearMonth yearMonth,
      LocalDate lastTradeDate,
      LocalDate referenceDate) {

    double accrualFactor = index.getTenor().get(ChronoUnit.MONTHS) / 12.0;
    IborFuture product = IborFuture.builder()
        .securityId(securityId)
        .index(index)
        .accrualFactor(accrualFactor)
        .lastTradeDate(lastTradeDate)
        .notional(notional)
        .build();
    TradeInfo info = TradeInfo.of(tradeDate);
    return IborFutureTrade.builder()
        .info(info)
        .product(product)
        .quantity(quantity)
        .price(price)
        .build();
  }

  @Override
  public LocalDate calculateReferenceDateFromTradeDate(
      LocalDate tradeDate,
      Period minimumPeriod,
      int sequenceNumber,
      ReferenceData refData) {

    LocalDate earliestDate = tradeDate.plus(minimumPeriod);
    LocalDate referenceDate = dateSequence.nthOrSame(earliestDate, sequenceNumber);
    return businessDayAdjustment.adjust(referenceDate, refData);
  }

  @Override
  public LocalDate calculateReferenceDateFromTradeDate(
      LocalDate tradeDate,
      YearMonth yearMonth,
      ReferenceData refData) {

    LocalDate referenceDate = dateSequence.dateMatching(yearMonth);
    return businessDayAdjustment.adjust(referenceDate, refData);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableIborFutureConvention}.
   * @return the meta-bean, not null
   */
  public static ImmutableIborFutureConvention.Meta meta() {
    return ImmutableIborFutureConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableIborFutureConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableIborFutureConvention.Builder builder() {
    return new ImmutableIborFutureConvention.Builder();
  }

  private ImmutableIborFutureConvention(
      IborIndex index,
      String name,
      DateSequence dateSequence,
      BusinessDayAdjustment businessDayAdjustment) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(dateSequence, "dateSequence");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    this.index = index;
    this.name = name;
    this.dateSequence = dateSequence;
    this.businessDayAdjustment = businessDayAdjustment;
  }

  @Override
  public ImmutableIborFutureConvention.Meta metaBean() {
    return ImmutableIborFutureConvention.Meta.INSTANCE;
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
   * Gets the convention name, such as 'USD-LIBOR-3M-Quarterly-IMM'.
   * <p>
   * This will default to the name of the index suffixed by the name of the date sequence if not specified.
   * @return the value of the property, not null
   */
  @Override
  public String getName() {
    return name;
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
      ImmutableIborFutureConvention other = (ImmutableIborFutureConvention) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(dateSequence, other.dateSequence) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(dateSequence);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableIborFutureConvention}.
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
        this, "index", ImmutableIborFutureConvention.class, IborIndex.class);
    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", ImmutableIborFutureConvention.class, String.class);
    /**
     * The meta-property for the {@code dateSequence} property.
     */
    private final MetaProperty<DateSequence> dateSequence = DirectMetaProperty.ofImmutable(
        this, "dateSequence", ImmutableIborFutureConvention.class, DateSequence.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", ImmutableIborFutureConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "name",
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
        case 3373707:  // name
          return name;
        case -258065009:  // dateSequence
          return dateSequence;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableIborFutureConvention.Builder builder() {
      return new ImmutableIborFutureConvention.Builder();
    }

    @Override
    public Class<? extends ImmutableIborFutureConvention> beanType() {
      return ImmutableIborFutureConvention.class;
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
          return ((ImmutableIborFutureConvention) bean).getIndex();
        case 3373707:  // name
          return ((ImmutableIborFutureConvention) bean).getName();
        case -258065009:  // dateSequence
          return ((ImmutableIborFutureConvention) bean).getDateSequence();
        case -1065319863:  // businessDayAdjustment
          return ((ImmutableIborFutureConvention) bean).getBusinessDayAdjustment();
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
   * The bean-builder for {@code ImmutableIborFutureConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableIborFutureConvention> {

    private IborIndex index;
    private String name;
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
    private Builder(ImmutableIborFutureConvention beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.name = beanToCopy.getName();
      this.dateSequence = beanToCopy.getDateSequence();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 3373707:  // name
          return name;
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
        case 3373707:  // name
          this.name = (String) newValue;
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
    public ImmutableIborFutureConvention build() {
      preBuild(this);
      return new ImmutableIborFutureConvention(
          index,
          name,
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
     * Sets the convention name, such as 'USD-LIBOR-3M-Quarterly-IMM'.
     * <p>
     * This will default to the name of the index suffixed by the name of the date sequence if not specified.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
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
      StringBuilder buf = new StringBuilder(160);
      buf.append("ImmutableIborFutureConvention.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("dateSequence").append('=').append(JodaBeanUtils.toString(dateSequence)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
