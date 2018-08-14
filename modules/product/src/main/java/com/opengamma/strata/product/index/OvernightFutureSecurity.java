/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.DerivedProperty;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * A security representing a futures contract based on an Overnight rate index.
 * <p>
 * An Overnight rate future is a financial instrument that is based on the future value of
 * an Overnight index interest rate. The profit or loss of an Overnight rate future is settled daily.
 * This class represents the structure of a single futures contract.
 * <p>
 * For example, the widely traded "30-Day Federal Funds futures contract" has a notional
 * of 5 million USD, is based on the US Federal Funds Effective Rate 'USD-FED-FUND',
 * expiring the last business day of each month.
 * 
 * <h4>Price</h4>
 * The price of an Overnight rate future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Overnight rate futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 */
@BeanDefinition
public final class OvernightFutureSecurity
    implements RateIndexSecurity, ImmutableBean, Serializable {

  /**
   * The standard security information.
   * <p>
   * This includes the security identifier.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityInfo info;
  /**
   * The notional amount.
   * <p>
   * This is the full notional of the deposit, such as 5 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional the same as the currency of the index.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double notional;
  /**
   * The accrual factor, defaulted from the index if not set.
   * <p>
   * This is the year fraction of the contract, typically 1/12 for a 30-day future.
   * As such, it is often unrelated to the day count of the index.
   * The year fraction must be positive.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double accrualFactor;
  /**
   * The last date of trading.
   * <p>
   * This must be a valid business day on the fixing calendar of {@code index}.
   * For example, the last trade date is often the last business day of the month.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate lastTradeDate;
  /**
   * The first date of the rate calculation period.
   * <p>
   * This is not necessarily a valid business day on the fixing calendar of {@code index}.
   * However, it will be adjusted in {@code OvernightRateComputation} if needed.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The last date of the rate calculation period. 
   * <p>
   * This is not necessarily a valid business day on the fixing calendar of {@code index}.
   * However, it will be adjusted in {@code OvernightRateComputation} if needed.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The underlying Overnight index.
   * <p>
   * The future is based on this index.
   * It will be a well known market index such as 'USD-FED-FUND'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final OvernightIndex index;
  /**
   * The method of accruing Overnight interest.
   * <p>
   * The average rate is calculated based on this method over the period between {@code startDate} and {@code endDate}.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightAccrualMethod accrualMethod;
  /**
   * The definition of how to round the futures price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   * For example, the common market price of 99.7125 for a 0.2875% rate is
   * represented as 0.997125 which has 6 decimal places.
   */
  @PropertyDefinition(validate = "notNull")
  private final Rounding rounding;

  //-------------------------------------------------------------------------
  @Override
  @DerivedProperty
  public Currency getCurrency() {
    return index.getCurrency();
  }

  @Override
  public ImmutableSet<SecurityId> getUnderlyingIds() {
    return ImmutableSet.of();
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightFutureSecurity withInfo(SecurityInfo info) {
    return toBuilder().info(info).build();
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightFuture createProduct(ReferenceData refData) {
    return OvernightFuture.builder()
        .securityId(getSecurityId())
        .notional(notional)
        .accrualFactor(accrualFactor)
        .index(index)
        .accrualMethod(accrualMethod)
        .lastTradeDate(lastTradeDate)
        .startDate(startDate)
        .endDate(endDate)
        .rounding(rounding)
        .build();
  }

  @Override
  public OvernightFutureTrade createTrade(
      TradeInfo info,
      double quantity,
      double tradePrice,
      ReferenceData refData) {

    return new OvernightFutureTrade(info, createProduct(refData), quantity, tradePrice);
  }

  @Override
  public OvernightFuturePosition createPosition(PositionInfo positionInfo, double quantity, ReferenceData refData) {
    return OvernightFuturePosition.ofNet(positionInfo, createProduct(refData), quantity);
  }

  @Override
  public OvernightFuturePosition createPosition(
      PositionInfo positionInfo,
      double longQuantity,
      double shortQuantity,
      ReferenceData refData) {

    return OvernightFuturePosition.ofLongShort(positionInfo, createProduct(refData), longQuantity, shortQuantity);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code OvernightFutureSecurity}.
   * @return the meta-bean, not null
   */
  public static OvernightFutureSecurity.Meta meta() {
    return OvernightFutureSecurity.Meta.INSTANCE;
  }

  static {
    MetaBean.register(OvernightFutureSecurity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightFutureSecurity.Builder builder() {
    return new OvernightFutureSecurity.Builder();
  }

  private OvernightFutureSecurity(
      SecurityInfo info,
      double notional,
      double accrualFactor,
      LocalDate lastTradeDate,
      LocalDate startDate,
      LocalDate endDate,
      OvernightIndex index,
      OvernightAccrualMethod accrualMethod,
      Rounding rounding) {
    JodaBeanUtils.notNull(info, "info");
    ArgChecker.notNegativeOrZero(notional, "notional");
    ArgChecker.notNegativeOrZero(accrualFactor, "accrualFactor");
    JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(accrualMethod, "accrualMethod");
    JodaBeanUtils.notNull(rounding, "rounding");
    this.info = info;
    this.notional = notional;
    this.accrualFactor = accrualFactor;
    this.lastTradeDate = lastTradeDate;
    this.startDate = startDate;
    this.endDate = endDate;
    this.index = index;
    this.accrualMethod = accrualMethod;
    this.rounding = rounding;
  }

  @Override
  public OvernightFutureSecurity.Meta metaBean() {
    return OvernightFutureSecurity.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the standard security information.
   * <p>
   * This includes the security identifier.
   * @return the value of the property, not null
   */
  @Override
  public SecurityInfo getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount.
   * <p>
   * This is the full notional of the deposit, such as 5 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional the same as the currency of the index.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accrual factor, defaulted from the index if not set.
   * <p>
   * This is the year fraction of the contract, typically 1/12 for a 30-day future.
   * As such, it is often unrelated to the day count of the index.
   * The year fraction must be positive.
   * @return the value of the property
   */
  public double getAccrualFactor() {
    return accrualFactor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last date of trading.
   * <p>
   * This must be a valid business day on the fixing calendar of {@code index}.
   * For example, the last trade date is often the last business day of the month.
   * @return the value of the property, not null
   */
  public LocalDate getLastTradeDate() {
    return lastTradeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first date of the rate calculation period.
   * <p>
   * This is not necessarily a valid business day on the fixing calendar of {@code index}.
   * However, it will be adjusted in {@code OvernightRateComputation} if needed.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last date of the rate calculation period.
   * <p>
   * This is not necessarily a valid business day on the fixing calendar of {@code index}.
   * However, it will be adjusted in {@code OvernightRateComputation} if needed.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying Overnight index.
   * <p>
   * The future is based on this index.
   * It will be a well known market index such as 'USD-FED-FUND'.
   * @return the value of the property, not null
   */
  @Override
  public OvernightIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method of accruing Overnight interest.
   * <p>
   * The average rate is calculated based on this method over the period between {@code startDate} and {@code endDate}.
   * @return the value of the property, not null
   */
  public OvernightAccrualMethod getAccrualMethod() {
    return accrualMethod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the definition of how to round the futures price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   * For example, the common market price of 99.7125 for a 0.2875% rate is
   * represented as 0.997125 which has 6 decimal places.
   * @return the value of the property, not null
   */
  public Rounding getRounding() {
    return rounding;
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
      OvernightFutureSecurity other = (OvernightFutureSecurity) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(accrualFactor, other.accrualFactor) &&
          JodaBeanUtils.equal(lastTradeDate, other.lastTradeDate) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(accrualMethod, other.accrualMethod) &&
          JodaBeanUtils.equal(rounding, other.rounding);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualFactor);
    hash = hash * 31 + JodaBeanUtils.hashCode(lastTradeDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualMethod);
    hash = hash * 31 + JodaBeanUtils.hashCode(rounding);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("OvernightFutureSecurity{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("accrualFactor").append('=').append(accrualFactor).append(',').append(' ');
    buf.append("lastTradeDate").append('=').append(lastTradeDate).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("accrualMethod").append('=').append(accrualMethod).append(',').append(' ');
    buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightFutureSecurity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<SecurityInfo> info = DirectMetaProperty.ofImmutable(
        this, "info", OvernightFutureSecurity.class, SecurityInfo.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", OvernightFutureSecurity.class, Double.TYPE);
    /**
     * The meta-property for the {@code accrualFactor} property.
     */
    private final MetaProperty<Double> accrualFactor = DirectMetaProperty.ofImmutable(
        this, "accrualFactor", OvernightFutureSecurity.class, Double.TYPE);
    /**
     * The meta-property for the {@code lastTradeDate} property.
     */
    private final MetaProperty<LocalDate> lastTradeDate = DirectMetaProperty.ofImmutable(
        this, "lastTradeDate", OvernightFutureSecurity.class, LocalDate.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", OvernightFutureSecurity.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", OvernightFutureSecurity.class, LocalDate.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<OvernightIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", OvernightFutureSecurity.class, OvernightIndex.class);
    /**
     * The meta-property for the {@code accrualMethod} property.
     */
    private final MetaProperty<OvernightAccrualMethod> accrualMethod = DirectMetaProperty.ofImmutable(
        this, "accrualMethod", OvernightFutureSecurity.class, OvernightAccrualMethod.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", OvernightFutureSecurity.class, Rounding.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofDerived(
        this, "currency", OvernightFutureSecurity.class, Currency.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "notional",
        "accrualFactor",
        "lastTradeDate",
        "startDate",
        "endDate",
        "index",
        "accrualMethod",
        "rounding",
        "currency");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 1585636160:  // notional
          return notional;
        case -1540322338:  // accrualFactor
          return accrualFactor;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 100346066:  // index
          return index;
        case -1335729296:  // accrualMethod
          return accrualMethod;
        case -142444:  // rounding
          return rounding;
        case 575402001:  // currency
          return currency;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightFutureSecurity.Builder builder() {
      return new OvernightFutureSecurity.Builder();
    }

    @Override
    public Class<? extends OvernightFutureSecurity> beanType() {
      return OvernightFutureSecurity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityInfo> info() {
      return info;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code accrualFactor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> accrualFactor() {
      return accrualFactor;
    }

    /**
     * The meta-property for the {@code lastTradeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> lastTradeDate() {
      return lastTradeDate;
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
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code accrualMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightAccrualMethod> accrualMethod() {
      return accrualMethod;
    }

    /**
     * The meta-property for the {@code rounding} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Rounding> rounding() {
      return rounding;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((OvernightFutureSecurity) bean).getInfo();
        case 1585636160:  // notional
          return ((OvernightFutureSecurity) bean).getNotional();
        case -1540322338:  // accrualFactor
          return ((OvernightFutureSecurity) bean).getAccrualFactor();
        case -1041950404:  // lastTradeDate
          return ((OvernightFutureSecurity) bean).getLastTradeDate();
        case -2129778896:  // startDate
          return ((OvernightFutureSecurity) bean).getStartDate();
        case -1607727319:  // endDate
          return ((OvernightFutureSecurity) bean).getEndDate();
        case 100346066:  // index
          return ((OvernightFutureSecurity) bean).getIndex();
        case -1335729296:  // accrualMethod
          return ((OvernightFutureSecurity) bean).getAccrualMethod();
        case -142444:  // rounding
          return ((OvernightFutureSecurity) bean).getRounding();
        case 575402001:  // currency
          return ((OvernightFutureSecurity) bean).getCurrency();
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
   * The bean-builder for {@code OvernightFutureSecurity}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightFutureSecurity> {

    private SecurityInfo info;
    private double notional;
    private double accrualFactor;
    private LocalDate lastTradeDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private OvernightIndex index;
    private OvernightAccrualMethod accrualMethod;
    private Rounding rounding;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(OvernightFutureSecurity beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.notional = beanToCopy.getNotional();
      this.accrualFactor = beanToCopy.getAccrualFactor();
      this.lastTradeDate = beanToCopy.getLastTradeDate();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.index = beanToCopy.getIndex();
      this.accrualMethod = beanToCopy.getAccrualMethod();
      this.rounding = beanToCopy.getRounding();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 1585636160:  // notional
          return notional;
        case -1540322338:  // accrualFactor
          return accrualFactor;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 100346066:  // index
          return index;
        case -1335729296:  // accrualMethod
          return accrualMethod;
        case -142444:  // rounding
          return rounding;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (SecurityInfo) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -1540322338:  // accrualFactor
          this.accrualFactor = (Double) newValue;
          break;
        case -1041950404:  // lastTradeDate
          this.lastTradeDate = (LocalDate) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case 100346066:  // index
          this.index = (OvernightIndex) newValue;
          break;
        case -1335729296:  // accrualMethod
          this.accrualMethod = (OvernightAccrualMethod) newValue;
          break;
        case -142444:  // rounding
          this.rounding = (Rounding) newValue;
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
    public OvernightFutureSecurity build() {
      return new OvernightFutureSecurity(
          info,
          notional,
          accrualFactor,
          lastTradeDate,
          startDate,
          endDate,
          index,
          accrualMethod,
          rounding);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the standard security information.
     * <p>
     * This includes the security identifier.
     * @param info  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder info(SecurityInfo info) {
      JodaBeanUtils.notNull(info, "info");
      this.info = info;
      return this;
    }

    /**
     * Sets the notional amount.
     * <p>
     * This is the full notional of the deposit, such as 5 million dollars.
     * The notional expressed here must be positive.
     * The currency of the notional the same as the currency of the index.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegativeOrZero(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the accrual factor, defaulted from the index if not set.
     * <p>
     * This is the year fraction of the contract, typically 1/12 for a 30-day future.
     * As such, it is often unrelated to the day count of the index.
     * The year fraction must be positive.
     * @param accrualFactor  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualFactor(double accrualFactor) {
      ArgChecker.notNegativeOrZero(accrualFactor, "accrualFactor");
      this.accrualFactor = accrualFactor;
      return this;
    }

    /**
     * Sets the last date of trading.
     * <p>
     * This must be a valid business day on the fixing calendar of {@code index}.
     * For example, the last trade date is often the last business day of the month.
     * @param lastTradeDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder lastTradeDate(LocalDate lastTradeDate) {
      JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
      this.lastTradeDate = lastTradeDate;
      return this;
    }

    /**
     * Sets the first date of the rate calculation period.
     * <p>
     * This is not necessarily a valid business day on the fixing calendar of {@code index}.
     * However, it will be adjusted in {@code OvernightRateComputation} if needed.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the last date of the rate calculation period.
     * <p>
     * This is not necessarily a valid business day on the fixing calendar of {@code index}.
     * However, it will be adjusted in {@code OvernightRateComputation} if needed.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the underlying Overnight index.
     * <p>
     * The future is based on this index.
     * It will be a well known market index such as 'USD-FED-FUND'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(OvernightIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the method of accruing Overnight interest.
     * <p>
     * The average rate is calculated based on this method over the period between {@code startDate} and {@code endDate}.
     * @param accrualMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualMethod(OvernightAccrualMethod accrualMethod) {
      JodaBeanUtils.notNull(accrualMethod, "accrualMethod");
      this.accrualMethod = accrualMethod;
      return this;
    }

    /**
     * Sets the definition of how to round the futures price, defaulted to no rounding.
     * <p>
     * The price is represented in decimal form, not percentage form.
     * As such, the decimal places expressed by the rounding refers to this decimal form.
     * For example, the common market price of 99.7125 for a 0.2875% rate is
     * represented as 0.997125 which has 6 decimal places.
     * @param rounding  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rounding(Rounding rounding) {
      JodaBeanUtils.notNull(rounding, "rounding");
      this.rounding = rounding;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("OvernightFutureSecurity.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("accrualFactor").append('=').append(JodaBeanUtils.toString(accrualFactor)).append(',').append(' ');
      buf.append("lastTradeDate").append('=').append(JodaBeanUtils.toString(lastTradeDate)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("accrualMethod").append('=').append(JodaBeanUtils.toString(accrualMethod)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
