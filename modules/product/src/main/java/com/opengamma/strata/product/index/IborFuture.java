/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
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
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * A futures contract based on an Ibor index.
 * <p>
 * An Ibor future is a financial instrument that is based on the future value of
 * an Ibor index interest rate. The profit or loss of an Ibor future is settled daily.
 * An Ibor future is also known as a <i>STIR future</i> (Short Term Interest Rate).
 * This class represents the structure of a single futures contract.
 * <p>
 * For example, the widely traded "CME Eurodollar futures contract" has a notional
 * of 1 million USD, is based on the USD Libor 3 month rate 'USD-LIBOR-3M', expiring
 * two business days before an IMM date (the 3rd Wednesday of the month).
 * 
 * <h4>Price</h4>
 * The price of an Ibor future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 */
@BeanDefinition(constructorScope = "package")
public final class IborFuture
    implements SecuritizedProduct, Resolvable<ResolvedIborFuture>, ImmutableBean, Serializable {

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
   * This is the full notional of the deposit, such as 1 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double notional;
  /**
   * The accrual factor, defaulted from the index if not set.
   * <p>
   * This is the year fraction of the contract, typically 0.25 for a 3 month deposit.
   * <p>
   * When building, this will default to the number of months in the index divided by 12
   * if not specified. However, if the index is not month-based, no defaulting will occur.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double accrualFactor;
  /**
   * The last date of trading.
   * This date is also the fixing date for the Ibor index.
   * This is typically 2 business days before the IMM date (3rd Wednesday of the month).
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate lastTradeDate;
  /**
   * The underlying Ibor index.
   * <p>
   * The future is based on this index.
   * It will be a well known market index such as 'USD-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
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
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.rounding(Rounding.none());
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null) {
      if (builder.accrualFactor == 0d && builder.index.getTenor().isMonthBased()) {
        builder.accrualFactor(builder.index.getTenor().getPeriod().toTotalMonths() / 12d);
      }
      if (builder.currency == null) {
        builder.currency = builder.index.getCurrency();
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the applicable fixing date.
   * <p>
   * This returns the fixing date of the contract.
   * This implementation simply returns the last trade date.
   * By including this method, it allows for the possibility of a future where the fixing date
   * and last trade date differ.
   * 
   * @return the fixing date
   */
  public LocalDate getFixingDate() {
    return lastTradeDate;
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedIborFuture resolve(ReferenceData refData) {
    IborRateComputation iborRate = IborRateComputation.of(index, lastTradeDate, refData);
    return new ResolvedIborFuture(securityId, currency, notional, accrualFactor, iborRate, rounding);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFuture}.
   * @return the meta-bean, not null
   */
  public static IborFuture.Meta meta() {
    return IborFuture.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFuture.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFuture.Builder builder() {
    return new IborFuture.Builder();
  }

  /**
   * Creates an instance.
   * @param securityId  the value of the property, not null
   * @param currency  the value of the property, not null
   * @param notional  the value of the property
   * @param accrualFactor  the value of the property
   * @param lastTradeDate  the value of the property, not null
   * @param index  the value of the property, not null
   * @param rounding  the value of the property, not null
   */
  IborFuture(
      SecurityId securityId,
      Currency currency,
      double notional,
      double accrualFactor,
      LocalDate lastTradeDate,
      IborIndex index,
      Rounding rounding) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegativeOrZero(notional, "notional");
    ArgChecker.notNegativeOrZero(accrualFactor, "accrualFactor");
    JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(rounding, "rounding");
    this.securityId = securityId;
    this.currency = currency;
    this.notional = notional;
    this.accrualFactor = accrualFactor;
    this.lastTradeDate = lastTradeDate;
    this.index = index;
    this.rounding = rounding;
  }

  @Override
  public IborFuture.Meta metaBean() {
    return IborFuture.Meta.INSTANCE;
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
   * This is the full notional of the deposit, such as 1 million dollars.
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
   * This is the year fraction of the contract, typically 0.25 for a 3 month deposit.
   * <p>
   * When building, this will default to the number of months in the index divided by 12
   * if not specified. However, if the index is not month-based, no defaulting will occur.
   * @return the value of the property
   */
  public double getAccrualFactor() {
    return accrualFactor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last date of trading.
   * This date is also the fixing date for the Ibor index.
   * This is typically 2 business days before the IMM date (3rd Wednesday of the month).
   * @return the value of the property, not null
   */
  public LocalDate getLastTradeDate() {
    return lastTradeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying Ibor index.
   * <p>
   * The future is based on this index.
   * It will be a well known market index such as 'USD-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public IborIndex getIndex() {
    return index;
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
      IborFuture other = (IborFuture) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(accrualFactor, other.accrualFactor) &&
          JodaBeanUtils.equal(lastTradeDate, other.lastTradeDate) &&
          JodaBeanUtils.equal(index, other.index) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(rounding);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("IborFuture{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("accrualFactor").append('=').append(accrualFactor).append(',').append(' ');
    buf.append("lastTradeDate").append('=').append(lastTradeDate).append(',').append(' ');
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFuture}.
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
        this, "securityId", IborFuture.class, SecurityId.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IborFuture.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", IborFuture.class, Double.TYPE);
    /**
     * The meta-property for the {@code accrualFactor} property.
     */
    private final MetaProperty<Double> accrualFactor = DirectMetaProperty.ofImmutable(
        this, "accrualFactor", IborFuture.class, Double.TYPE);
    /**
     * The meta-property for the {@code lastTradeDate} property.
     */
    private final MetaProperty<LocalDate> lastTradeDate = DirectMetaProperty.ofImmutable(
        this, "lastTradeDate", IborFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", IborFuture.class, IborIndex.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", IborFuture.class, Rounding.class);
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
        "index",
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
        case 100346066:  // index
          return index;
        case -142444:  // rounding
          return rounding;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFuture.Builder builder() {
      return new IborFuture.Builder();
    }

    @Override
    public Class<? extends IborFuture> beanType() {
      return IborFuture.class;
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
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> index() {
      return index;
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
          return ((IborFuture) bean).getSecurityId();
        case 575402001:  // currency
          return ((IborFuture) bean).getCurrency();
        case 1585636160:  // notional
          return ((IborFuture) bean).getNotional();
        case -1540322338:  // accrualFactor
          return ((IborFuture) bean).getAccrualFactor();
        case -1041950404:  // lastTradeDate
          return ((IborFuture) bean).getLastTradeDate();
        case 100346066:  // index
          return ((IborFuture) bean).getIndex();
        case -142444:  // rounding
          return ((IborFuture) bean).getRounding();
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
   * The bean-builder for {@code IborFuture}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFuture> {

    private SecurityId securityId;
    private Currency currency;
    private double notional;
    private double accrualFactor;
    private LocalDate lastTradeDate;
    private IborIndex index;
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
    private Builder(IborFuture beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.accrualFactor = beanToCopy.getAccrualFactor();
      this.lastTradeDate = beanToCopy.getLastTradeDate();
      this.index = beanToCopy.getIndex();
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
        case 100346066:  // index
          return index;
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
        case 100346066:  // index
          this.index = (IborIndex) newValue;
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

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    /**
     * @deprecated Loop in application code
     */
    @Override
    @Deprecated
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public IborFuture build() {
      preBuild(this);
      return new IborFuture(
          securityId,
          currency,
          notional,
          accrualFactor,
          lastTradeDate,
          index,
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
     * This is the full notional of the deposit, such as 1 million dollars.
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
     * This is the year fraction of the contract, typically 0.25 for a 3 month deposit.
     * <p>
     * When building, this will default to the number of months in the index divided by 12
     * if not specified. However, if the index is not month-based, no defaulting will occur.
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
     * This date is also the fixing date for the Ibor index.
     * This is typically 2 business days before the IMM date (3rd Wednesday of the month).
     * @param lastTradeDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder lastTradeDate(LocalDate lastTradeDate) {
      JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
      this.lastTradeDate = lastTradeDate;
      return this;
    }

    /**
     * Sets the underlying Ibor index.
     * <p>
     * The future is based on this index.
     * It will be a well known market index such as 'USD-LIBOR-3M'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
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
      StringBuilder buf = new StringBuilder(256);
      buf.append("IborFuture.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("accrualFactor").append('=').append(JodaBeanUtils.toString(accrualFactor)).append(',').append(' ');
      buf.append("lastTradeDate").append('=').append(JodaBeanUtils.toString(lastTradeDate)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
