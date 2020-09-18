/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
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
import com.opengamma.strata.basics.date.SequenceDate;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.IborFuture;
import com.opengamma.strata.product.index.IborFuturePosition;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * A contract specification for exchange traded Ibor Futures.
 * <p>
 * The contract specification defines how the future is traded.
 * A specific future is created by specifying the year-month.
 */
@BeanDefinition(style = "minimal")
public final class ImmutableIborFutureContractSpec
    implements IborFutureContractSpec, ImmutableBean, Serializable {

  /**
   * The name, such as 'USD-LIBOR-3M-IMM-CME'.
   */
  @PropertyDefinition(validate = "notBlank", overrideGet = true)
  private final String name;
  /**
   * The Ibor index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
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
      SequenceDate sequenceDate,
      double quantity,
      double price,
      ReferenceData refData) {

    LocalDate referenceDate = calculateReferenceDate(tradeDate, sequenceDate, refData);
    LocalDate lastTradeDate = index.calculateFixingFromEffective(referenceDate, refData);
    return createTrade(tradeDate, securityId, quantity, price, lastTradeDate);
  }

  // creates the trade
  private IborFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      double quantity,
      double price,
      LocalDate lastTradeDate) {

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
  public LocalDate calculateReferenceDate(LocalDate tradeDate, SequenceDate sequenceDate, ReferenceData refData) {
    LocalDate referenceDate = dateSequence.selectDate(tradeDate, sequenceDate);
    return businessDayAdjustment.adjust(referenceDate, refData);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborFuturePosition createPosition(
      SecurityId securityId,
      YearMonth expiry,
      double quantity,
      ReferenceData refData) {

    LocalDate effectiveDate = dateSequence.dateMatching(expiry);
    LocalDate lastTradeDate = index.calculateFixingFromEffective(effectiveDate, refData);
    return createPosition(securityId, quantity, lastTradeDate);
  }

  // creates the position
  private IborFuturePosition createPosition(
      SecurityId securityId,
      double quantity,
      LocalDate lastTradeDate) {

    double accrualFactor = index.getTenor().get(ChronoUnit.MONTHS) / 12.0;
    IborFuture product = IborFuture.builder()
        .securityId(securityId)
        .index(index)
        .accrualFactor(accrualFactor)
        .lastTradeDate(lastTradeDate)
        .notional(notional)
        .build();
    return IborFuturePosition.ofNet(PositionInfo.empty(), product, quantity);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ImmutableIborFutureContractSpec}.
   */
  private static final TypedMetaBean<ImmutableIborFutureContractSpec> META_BEAN =
      MinimalMetaBean.of(
          ImmutableIborFutureContractSpec.class,
          new String[] {
              "name",
              "index",
              "dateSequence",
              "businessDayAdjustment",
              "notional"},
          () -> new ImmutableIborFutureContractSpec.Builder(),
          b -> b.getName(),
          b -> b.getIndex(),
          b -> b.getDateSequence(),
          b -> b.getBusinessDayAdjustment(),
          b -> b.getNotional());

  /**
   * The meta-bean for {@code ImmutableIborFutureContractSpec}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<ImmutableIborFutureContractSpec> meta() {
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
  public static ImmutableIborFutureContractSpec.Builder builder() {
    return new ImmutableIborFutureContractSpec.Builder();
  }

  private ImmutableIborFutureContractSpec(
      String name,
      IborIndex index,
      DateSequence dateSequence,
      BusinessDayAdjustment businessDayAdjustment,
      double notional) {
    JodaBeanUtils.notBlank(name, "name");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(dateSequence, "dateSequence");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    ArgChecker.notNegativeOrZero(notional, "notional");
    this.name = name;
    this.index = index;
    this.dateSequence = dateSequence;
    this.businessDayAdjustment = businessDayAdjustment;
    this.notional = notional;
  }

  @Override
  public TypedMetaBean<ImmutableIborFutureContractSpec> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name, such as 'USD-LIBOR-3M-IMM-CME'.
   * @return the value of the property, not blank
   */
  @Override
  public String getName() {
    return name;
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
      ImmutableIborFutureContractSpec other = (ImmutableIborFutureContractSpec) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(dateSequence, other.dateSequence) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code ImmutableIborFutureContractSpec}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableIborFutureContractSpec> {

    private String name;
    private IborIndex index;
    private DateSequence dateSequence;
    private BusinessDayAdjustment businessDayAdjustment;
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
    private Builder(ImmutableIborFutureContractSpec beanToCopy) {
      this.name = beanToCopy.getName();
      this.index = beanToCopy.getIndex();
      this.dateSequence = beanToCopy.getDateSequence();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
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
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
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
          this.index = (IborIndex) newValue;
          break;
        case -258065009:  // dateSequence
          this.dateSequence = (DateSequence) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
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
    public ImmutableIborFutureContractSpec build() {
      preBuild(this);
      return new ImmutableIborFutureContractSpec(
          name,
          index,
          dateSequence,
          businessDayAdjustment,
          notional);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the name, such as 'USD-LIBOR-3M-IMM-CME'.
     * @param name  the new value, not blank
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notBlank(name, "name");
      this.name = name;
      return this;
    }

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
      StringBuilder buf = new StringBuilder(192);
      buf.append("ImmutableIborFutureContractSpec.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("dateSequence").append('=').append(JodaBeanUtils.toString(dateSequence)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
