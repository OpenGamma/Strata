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
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.rate.OvernightRateComputation;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * A futures contract based on an Overnight index.
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
@BeanDefinition(constructorScope = "package")
public final class OvernightFuture
    implements SecuritizedProduct, Resolvable<ResolvedOvernightFuture>, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityId securityId;
  /**
   * The currency that the future is traded in, defaulted from the index if not set.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The notional amount.
   * <p>
   * This is the full notional of the deposit, such as 5 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
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
  @PropertyDefinition(validate = "notNull")
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
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.rounding(Rounding.none());
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null) {
      if (builder.currency == null) {
        builder.currency = builder.index.getCurrency();
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedOvernightFuture resolve(ReferenceData refData) {
    OvernightRateComputation overnightAveragedRate = OvernightRateComputation.of(
        index, startDate, endDate, 0, accrualMethod, refData);
    return ResolvedOvernightFuture.builder()
        .securityId(securityId)
        .accrualFactor(accrualFactor)
        .currency(currency)
        .notional(notional)
        .lastTradeDate(lastTradeDate)
        .overnightRate(overnightAveragedRate)
        .rounding(rounding)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code OvernightFuture}.
   * @return the meta-bean, not null
   */
  public static OvernightFuture.Meta meta() {
    return OvernightFuture.Meta.INSTANCE;
  }

  static {
    MetaBean.register(OvernightFuture.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightFuture.Builder builder() {
    return new OvernightFuture.Builder();
  }

  /**
   * Creates an instance.
   * @param securityId  the value of the property, not null
   * @param currency  the value of the property, not null
   * @param notional  the value of the property
   * @param accrualFactor  the value of the property
   * @param lastTradeDate  the value of the property, not null
   * @param startDate  the value of the property, not null
   * @param endDate  the value of the property, not null
   * @param index  the value of the property, not null
   * @param accrualMethod  the value of the property, not null
   * @param rounding  the value of the property, not null
   */
  OvernightFuture(
      SecurityId securityId,
      Currency currency,
      double notional,
      double accrualFactor,
      LocalDate lastTradeDate,
      LocalDate startDate,
      LocalDate endDate,
      OvernightIndex index,
      OvernightAccrualMethod accrualMethod,
      Rounding rounding) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegativeOrZero(notional, "notional");
    ArgChecker.notNegativeOrZero(accrualFactor, "accrualFactor");
    JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(accrualMethod, "accrualMethod");
    JodaBeanUtils.notNull(rounding, "rounding");
    this.securityId = securityId;
    this.currency = currency;
    this.notional = notional;
    this.accrualFactor = accrualFactor;
    this.lastTradeDate = lastTradeDate;
    this.startDate = startDate;
    this.endDate = endDate;
    this.index = index;
    this.accrualMethod = accrualMethod;
    this.rounding = rounding;
    validate();
  }

  @Override
  public OvernightFuture.Meta metaBean() {
    return OvernightFuture.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * @return the value of the property, not null
   */
  @Override
  public SecurityId getSecurityId() {
    return securityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency that the future is traded in, defaulted from the index if not set.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount.
   * <p>
   * This is the full notional of the deposit, such as 5 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
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
      OvernightFuture other = (OvernightFuture) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(currency, other.currency) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(securityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
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
    StringBuilder buf = new StringBuilder(352);
    buf.append("OvernightFuture{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
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
   * The meta-bean for {@code OvernightFuture}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code securityId} property.
     */
    private final MetaProperty<SecurityId> securityId = DirectMetaProperty.ofImmutable(
        this, "securityId", OvernightFuture.class, SecurityId.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", OvernightFuture.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", OvernightFuture.class, Double.TYPE);
    /**
     * The meta-property for the {@code accrualFactor} property.
     */
    private final MetaProperty<Double> accrualFactor = DirectMetaProperty.ofImmutable(
        this, "accrualFactor", OvernightFuture.class, Double.TYPE);
    /**
     * The meta-property for the {@code lastTradeDate} property.
     */
    private final MetaProperty<LocalDate> lastTradeDate = DirectMetaProperty.ofImmutable(
        this, "lastTradeDate", OvernightFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", OvernightFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", OvernightFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<OvernightIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", OvernightFuture.class, OvernightIndex.class);
    /**
     * The meta-property for the {@code accrualMethod} property.
     */
    private final MetaProperty<OvernightAccrualMethod> accrualMethod = DirectMetaProperty.ofImmutable(
        this, "accrualMethod", OvernightFuture.class, OvernightAccrualMethod.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", OvernightFuture.class, Rounding.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "currency",
        "notional",
        "accrualFactor",
        "lastTradeDate",
        "startDate",
        "endDate",
        "index",
        "accrualMethod",
        "rounding");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case 575402001:  // currency
          return currency;
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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightFuture.Builder builder() {
      return new OvernightFuture.Builder();
    }

    @Override
    public Class<? extends OvernightFuture> beanType() {
      return OvernightFuture.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code securityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityId> securityId() {
      return securityId;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return ((OvernightFuture) bean).getSecurityId();
        case 575402001:  // currency
          return ((OvernightFuture) bean).getCurrency();
        case 1585636160:  // notional
          return ((OvernightFuture) bean).getNotional();
        case -1540322338:  // accrualFactor
          return ((OvernightFuture) bean).getAccrualFactor();
        case -1041950404:  // lastTradeDate
          return ((OvernightFuture) bean).getLastTradeDate();
        case -2129778896:  // startDate
          return ((OvernightFuture) bean).getStartDate();
        case -1607727319:  // endDate
          return ((OvernightFuture) bean).getEndDate();
        case 100346066:  // index
          return ((OvernightFuture) bean).getIndex();
        case -1335729296:  // accrualMethod
          return ((OvernightFuture) bean).getAccrualMethod();
        case -142444:  // rounding
          return ((OvernightFuture) bean).getRounding();
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
   * The bean-builder for {@code OvernightFuture}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightFuture> {

    private SecurityId securityId;
    private Currency currency;
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
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(OvernightFuture beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.currency = beanToCopy.getCurrency();
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
        case 1574023291:  // securityId
          return securityId;
        case 575402001:  // currency
          return currency;
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
        case 1574023291:  // securityId
          this.securityId = (SecurityId) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
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
    public OvernightFuture build() {
      preBuild(this);
      return new OvernightFuture(
          securityId,
          currency,
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
     * Sets the security identifier.
     * <p>
     * This identifier uniquely identifies the security within the system.
     * @param securityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder securityId(SecurityId securityId) {
      JodaBeanUtils.notNull(securityId, "securityId");
      this.securityId = securityId;
      return this;
    }

    /**
     * Sets the currency that the future is traded in, defaulted from the index if not set.
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
     * This is the full notional of the deposit, such as 5 million dollars.
     * The notional expressed here must be positive.
     * The currency of the notional is specified by {@code currency}.
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
      StringBuilder buf = new StringBuilder(352);
      buf.append("OvernightFuture.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
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
