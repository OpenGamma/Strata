/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.NoSuchElementException;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.MinimalMetaBean;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DateSequence;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.SequenceDate;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.OvernightFuture;
import com.opengamma.strata.product.index.OvernightFuturePosition;
import com.opengamma.strata.product.index.OvernightFutureTrade;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * A contract specification for exchange traded Overnight Futures.
 * <p>
 * The contract specification defines how the future is traded.
 * A specific future is created by specifying the year-month.
 */
@BeanDefinition(style = "minimal")
public final class ImmutableOvernightFutureContractSpec
    implements OvernightFutureContractSpec, ImmutableBean, Serializable {

  /**
   * The name, such as 'GBP-SONIA-3M-IMM-ICE'.
   */
  @PropertyDefinition(validate = "notBlank", overrideGet = true)
  private final String name;
  /**
   * The Overnight index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-SONIA'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final OvernightIndex index;
  /**
   * The sequence of dates that the future is based on.
   * <p>
   * This is used to calculate the reference date of the future that is the start
   * date of the underlying synthetic deposit.
   */
  @PropertyDefinition(validate = "notNull")
  private final DateSequence dateSequence;
  /**
   * The method of accruing Overnight interest.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightAccrualMethod accrualMethod;
  /**
   * The business day adjustment to apply to get the start date.
   * <p>
   * The start date is obtained by applying this adjustment to the reference date from the date sequence.
   * The reference date is often the third Wednesday of the month or the start of the month.
   * This defaults to accepting the date from the sequence without applying a holiday calendar.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment startDateAdjustment;
  /**
   * The days adjustment to apply to get the end date.
   * <p>
   * The end date is obtained by applying this adjustment to the next date in sequence from the start date.
   * This defaults to minus one without applying a holiday calendar.
   */
  @PropertyDefinition
  private final DaysAdjustment endDateAdjustment;
  /**
   * The days adjustment to apply to get the last trade date.
   * <p>
   * The last trade date is obtained by applying this adjustment to the next date in sequence from the start date.
   * This defaults to the previous business day in the fixing calendar (minus one calendar day and preceding).
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment lastTradeDateAdjustment;
  /**
   * The notional deposit that the contract models.
   * <p>
   * This is the full notional of the deposit, such as 1 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional is specified by the index.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero", overrideGet = true)
  private final double notional;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null) {
      if (builder.lastTradeDateAdjustment == null) {
        BusinessDayAdjustment bda = BusinessDayAdjustment.of(PRECEDING, builder.index.getFixingCalendar());
        builder.lastTradeDateAdjustment = DaysAdjustment.ofCalendarDays(-1, bda);
      }
    }
    if (builder.startDateAdjustment == null) {
      builder.startDateAdjustment = BusinessDayAdjustment.NONE;
    }
    if (builder.endDateAdjustment == null) {
      builder.endDateAdjustment = DaysAdjustment.ofCalendarDays(-1);
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      SequenceDate sequenceDate,
      double quantity,
      double price,
      ReferenceData refData) {

    LocalDate startDate = calculateReferenceDate(tradeDate, sequenceDate, refData);
    return createTrade(tradeDate, securityId, quantity, price, startDate, refData);
  }

  // creates the trade
  private OvernightFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      double quantity,
      double price,
      LocalDate startDate,
      ReferenceData refData) {

    LocalDate nextReferenceDate = dateSequence.baseSequence().next(startDate);
    double accrualFactor = startDate.withDayOfMonth(1).until(nextReferenceDate.withDayOfMonth(1), MONTHS) / 12d;
    LocalDate endDate = endDateAdjustment.adjust(nextReferenceDate, refData);
    LocalDate lastTradeDate = lastTradeDateAdjustment.adjust(nextReferenceDate, refData);
    OvernightFuture product = OvernightFuture.builder()
        .securityId(securityId)
        .index(index)
        .accrualMethod(accrualMethod)
        .accrualFactor(accrualFactor)
        .startDate(startDate)
        .endDate(endDate)
        .lastTradeDate(lastTradeDate)
        .notional(notional)
        .build();
    TradeInfo info = TradeInfo.of(tradeDate);
    return OvernightFutureTrade.builder()
        .info(info)
        .product(product)
        .quantity(quantity)
        .price(price)
        .build();
  }

  @Override
  public OvernightFuturePosition createPosition(
      SecurityId securityId,
      YearMonth expiry,
      double quantity,
      ReferenceData refData) {

    LocalDate startDate = dateSequence.dateMatching(expiry);
    return createPosition(securityId, quantity, startDate, refData);
  }

  // creates the position
  private OvernightFuturePosition createPosition(
      SecurityId securityId,
      double quantity,
      LocalDate startDate,
      ReferenceData refData) {

    LocalDate nextReferenceDate = dateSequence.baseSequence().next(startDate);
    double accrualFactor = startDate.withDayOfMonth(1).until(nextReferenceDate.withDayOfMonth(1), MONTHS) / 12d;
    LocalDate endDate = endDateAdjustment.adjust(nextReferenceDate, refData);
    LocalDate lastTradeDate = lastTradeDateAdjustment.adjust(nextReferenceDate, refData);
    OvernightFuture product = OvernightFuture.builder()
        .securityId(securityId)
        .index(index)
        .accrualMethod(accrualMethod)
        .accrualFactor(accrualFactor)
        .startDate(startDate)
        .endDate(endDate)
        .lastTradeDate(lastTradeDate)
        .notional(notional)
        .build();
    return OvernightFuturePosition.ofNet(PositionInfo.empty(), product, quantity);
  }

  @Override
  public LocalDate calculateReferenceDate(LocalDate tradeDate, SequenceDate sequenceDate, ReferenceData refData) {
    LocalDate referenceDate = dateSequence.selectDate(tradeDate, sequenceDate);
    return startDateAdjustment.adjust(referenceDate, refData);
  }

  @Override
  public LocalDate calculateLastFixingDate(LocalDate referenceDate, ReferenceData refData) {
    LocalDate nextReferenceDate = dateSequence.baseSequence().next(referenceDate);
    return endDateAdjustment.adjust(nextReferenceDate, refData);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ImmutableOvernightFutureContractSpec}.
   */
  private static final TypedMetaBean<ImmutableOvernightFutureContractSpec> META_BEAN =
      MinimalMetaBean.of(
          ImmutableOvernightFutureContractSpec.class,
          new String[] {
              "name",
              "index",
              "dateSequence",
              "accrualMethod",
              "startDateAdjustment",
              "endDateAdjustment",
              "lastTradeDateAdjustment",
              "notional"},
          () -> new ImmutableOvernightFutureContractSpec.Builder(),
          b -> b.getName(),
          b -> b.getIndex(),
          b -> b.getDateSequence(),
          b -> b.getAccrualMethod(),
          b -> b.getStartDateAdjustment(),
          b -> b.getEndDateAdjustment(),
          b -> b.getLastTradeDateAdjustment(),
          b -> b.getNotional());

  /**
   * The meta-bean for {@code ImmutableOvernightFutureContractSpec}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<ImmutableOvernightFutureContractSpec> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableOvernightFutureContractSpec.Builder builder() {
    return new ImmutableOvernightFutureContractSpec.Builder();
  }

  private ImmutableOvernightFutureContractSpec(
      String name,
      OvernightIndex index,
      DateSequence dateSequence,
      OvernightAccrualMethod accrualMethod,
      BusinessDayAdjustment startDateAdjustment,
      DaysAdjustment endDateAdjustment,
      DaysAdjustment lastTradeDateAdjustment,
      double notional) {
    JodaBeanUtils.notBlank(name, "name");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(dateSequence, "dateSequence");
    JodaBeanUtils.notNull(accrualMethod, "accrualMethod");
    JodaBeanUtils.notNull(startDateAdjustment, "startDateAdjustment");
    JodaBeanUtils.notNull(lastTradeDateAdjustment, "lastTradeDateAdjustment");
    ArgChecker.notNegativeOrZero(notional, "notional");
    this.name = name;
    this.index = index;
    this.dateSequence = dateSequence;
    this.accrualMethod = accrualMethod;
    this.startDateAdjustment = startDateAdjustment;
    this.endDateAdjustment = endDateAdjustment;
    this.lastTradeDateAdjustment = lastTradeDateAdjustment;
    this.notional = notional;
  }

  @Override
  public TypedMetaBean<ImmutableOvernightFutureContractSpec> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name, such as 'GBP-SONIA-3M-IMM-ICE'.
   * @return the value of the property, not blank
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Overnight index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-SONIA'.
   * @return the value of the property, not null
   */
  @Override
  public OvernightIndex getIndex() {
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
   * Gets the method of accruing Overnight interest.
   * @return the value of the property, not null
   */
  public OvernightAccrualMethod getAccrualMethod() {
    return accrualMethod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to get the start date.
   * <p>
   * The start date is obtained by applying this adjustment to the reference date from the date sequence.
   * The reference date is often the third Wednesday of the month or the start of the month.
   * This defaults to accepting the date from the sequence without applying a holiday calendar.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getStartDateAdjustment() {
    return startDateAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the days adjustment to apply to get the end date.
   * <p>
   * The end date is obtained by applying this adjustment to the next date in sequence from the start date.
   * This defaults to minus one without applying a holiday calendar.
   * @return the value of the property
   */
  public DaysAdjustment getEndDateAdjustment() {
    return endDateAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the days adjustment to apply to get the last trade date.
   * <p>
   * The last trade date is obtained by applying this adjustment to the next date in sequence from the start date.
   * This defaults to the previous business day in the fixing calendar (minus one calendar day and preceding).
   * @return the value of the property, not null
   */
  public DaysAdjustment getLastTradeDateAdjustment() {
    return lastTradeDateAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional deposit that the contract models.
   * <p>
   * This is the full notional of the deposit, such as 1 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional is specified by the index.
   * @return the value of the property
   */
  @Override
  public double getNotional() {
    return notional;
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
      ImmutableOvernightFutureContractSpec other = (ImmutableOvernightFutureContractSpec) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(dateSequence, other.dateSequence) &&
          JodaBeanUtils.equal(accrualMethod, other.accrualMethod) &&
          JodaBeanUtils.equal(startDateAdjustment, other.startDateAdjustment) &&
          JodaBeanUtils.equal(endDateAdjustment, other.endDateAdjustment) &&
          JodaBeanUtils.equal(lastTradeDateAdjustment, other.lastTradeDateAdjustment) &&
          JodaBeanUtils.equal(notional, other.notional);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(dateSequence);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualMethod);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDateAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDateAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(lastTradeDateAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code ImmutableOvernightFutureContractSpec}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableOvernightFutureContractSpec> {

    private String name;
    private OvernightIndex index;
    private DateSequence dateSequence;
    private OvernightAccrualMethod accrualMethod;
    private BusinessDayAdjustment startDateAdjustment;
    private DaysAdjustment endDateAdjustment;
    private DaysAdjustment lastTradeDateAdjustment;
    private double notional;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableOvernightFutureContractSpec beanToCopy) {
      this.name = beanToCopy.getName();
      this.index = beanToCopy.getIndex();
      this.dateSequence = beanToCopy.getDateSequence();
      this.accrualMethod = beanToCopy.getAccrualMethod();
      this.startDateAdjustment = beanToCopy.getStartDateAdjustment();
      this.endDateAdjustment = beanToCopy.getEndDateAdjustment();
      this.lastTradeDateAdjustment = beanToCopy.getLastTradeDateAdjustment();
      this.notional = beanToCopy.getNotional();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 100346066:  // index
          return index;
        case -258065009:  // dateSequence
          return dateSequence;
        case -1335729296:  // accrualMethod
          return accrualMethod;
        case -1235962691:  // startDateAdjustment
          return startDateAdjustment;
        case 1599713654:  // endDateAdjustment
          return endDateAdjustment;
        case -1889695799:  // lastTradeDateAdjustment
          return lastTradeDateAdjustment;
        case 1585636160:  // notional
          return notional;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case 100346066:  // index
          this.index = (OvernightIndex) newValue;
          break;
        case -258065009:  // dateSequence
          this.dateSequence = (DateSequence) newValue;
          break;
        case -1335729296:  // accrualMethod
          this.accrualMethod = (OvernightAccrualMethod) newValue;
          break;
        case -1235962691:  // startDateAdjustment
          this.startDateAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 1599713654:  // endDateAdjustment
          this.endDateAdjustment = (DaysAdjustment) newValue;
          break;
        case -1889695799:  // lastTradeDateAdjustment
          this.lastTradeDateAdjustment = (DaysAdjustment) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
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
    public ImmutableOvernightFutureContractSpec build() {
      preBuild(this);
      return new ImmutableOvernightFutureContractSpec(
          name,
          index,
          dateSequence,
          accrualMethod,
          startDateAdjustment,
          endDateAdjustment,
          lastTradeDateAdjustment,
          notional);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the name, such as 'GBP-SONIA-3M-IMM-ICE'.
     * @param name  the new value, not blank
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notBlank(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the Overnight index.
     * <p>
     * The floating rate to be paid is based on this index
     * It will be a well known market index such as 'GBP-SONIA'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(OvernightIndex index) {
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
     * Sets the method of accruing Overnight interest.
     * @param accrualMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualMethod(OvernightAccrualMethod accrualMethod) {
      JodaBeanUtils.notNull(accrualMethod, "accrualMethod");
      this.accrualMethod = accrualMethod;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to get the start date.
     * <p>
     * The start date is obtained by applying this adjustment to the reference date from the date sequence.
     * The reference date is often the third Wednesday of the month or the start of the month.
     * This defaults to accepting the date from the sequence without applying a holiday calendar.
     * @param startDateAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDateAdjustment(BusinessDayAdjustment startDateAdjustment) {
      JodaBeanUtils.notNull(startDateAdjustment, "startDateAdjustment");
      this.startDateAdjustment = startDateAdjustment;
      return this;
    }

    /**
     * Sets the days adjustment to apply to get the end date.
     * <p>
     * The end date is obtained by applying this adjustment to the next date in sequence from the start date.
     * This defaults to minus one without applying a holiday calendar.
     * @param endDateAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder endDateAdjustment(DaysAdjustment endDateAdjustment) {
      this.endDateAdjustment = endDateAdjustment;
      return this;
    }

    /**
     * Sets the days adjustment to apply to get the last trade date.
     * <p>
     * The last trade date is obtained by applying this adjustment to the next date in sequence from the start date.
     * This defaults to the previous business day in the fixing calendar (minus one calendar day and preceding).
     * @param lastTradeDateAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder lastTradeDateAdjustment(DaysAdjustment lastTradeDateAdjustment) {
      JodaBeanUtils.notNull(lastTradeDateAdjustment, "lastTradeDateAdjustment");
      this.lastTradeDateAdjustment = lastTradeDateAdjustment;
      return this;
    }

    /**
     * Sets the notional deposit that the contract models.
     * <p>
     * This is the full notional of the deposit, such as 1 million dollars.
     * The notional expressed here must be positive.
     * The currency of the notional is specified by the index.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegativeOrZero(notional, "notional");
      this.notional = notional;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("ImmutableOvernightFutureContractSpec.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("dateSequence").append('=').append(JodaBeanUtils.toString(dateSequence)).append(',').append(' ');
      buf.append("accrualMethod").append('=').append(JodaBeanUtils.toString(accrualMethod)).append(',').append(' ');
      buf.append("startDateAdjustment").append('=').append(JodaBeanUtils.toString(startDateAdjustment)).append(',').append(' ');
      buf.append("endDateAdjustment").append('=').append(JodaBeanUtils.toString(endDateAdjustment)).append(',').append(' ');
      buf.append("lastTradeDateAdjustment").append('=').append(JodaBeanUtils.toString(lastTradeDateAdjustment)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
