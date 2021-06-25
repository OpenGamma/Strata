/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.collect.ArgChecker.inOrderOrEqual;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.fx.FxOptionProduct;
import com.opengamma.strata.product.fx.FxSingle;

/**
 * A vanilla FX option.
 * <p>
 * An FX option is a financial instrument that provides an option based on the future value of a foreign exchange. The
 * option is European, exercised only on the exercise date.
 * <p>
 * For example, a call on a 'EUR 1.00 / USD -1.41' exchange is the option to perform a foreign exchange on the expiry
 * date, where USD 1.41 is paid to receive EUR 1.00.
 */
@BeanDefinition
public final class FxVanillaOption
    implements FxOptionProduct, Resolvable<ResolvedFxVanillaOption>, ImmutableBean, Serializable {

  /**
   * Whether the option is long or short.
   * <p>
   * At expiry, the long party will have the option to enter in this transaction; the short party will, at the option of
   * the long party, potentially enter into the inverse transaction.
   */
  @PropertyDefinition(validate = "notNull")
  private final LongShort longShort;
  /**
   * The expiry date of the option.
   * <p>
   * The option is European, and can only be exercised on the expiry date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate expiryDate;
  /**
   * The expiry time of the option.
   * <p>
   * The expiry time is related to the expiry date and time-zone.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalTime expiryTime;
  /**
   * The time-zone of the expiry time.
   * <p>
   * The expiry time-zone is related to the expiry date and time.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZoneId expiryZone;
  /**
   * The underlying foreign exchange transaction.
   * <p>
   * At expiry, if the option is in the money, this foreign exchange will occur.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxSingle underlying;

  /**
   * Creates an equivalent {@code FxVanillaOption} using currency pair, option expiry, call/put flag, strike, base
   * currency notional, and underlying payment date.
   *
   * @param longShort the long/short flag of the option
   * @param expiry the option expiry
   * @param currencyPair the FX currency pair
   * @param putCall the put/call flag of the option
   * @param strike the FX strike
   * @param baseNotional the base currency notional amount: should always be positive
   * @param paymentDate the payment date of the underlying FX cash flows
   * @return an equivalent fx vanilla option
   */
  public static FxVanillaOption of(
      LongShort longShort,
      ZonedDateTime expiry,
      CurrencyPair currencyPair,
      PutCall putCall,
      double strike,
      double baseNotional,
      LocalDate paymentDate) {

    ArgChecker.isTrue(baseNotional > 0, "Base notional must be positive");
    ArgChecker.isTrue(strike > 0, "FX strike must be positive");

    // for a vanilla call, will be long the base currency and short the counter currency
    // for a vanilla put, will be short the base currency and long the counter currency
    double baseAmount = putCall.isCall() ? baseNotional : -baseNotional;
    double counterNotional = strike * baseNotional;
    double counterAmount = putCall.isCall() ? -counterNotional : counterNotional;
    FxSingle equivalentUnderlying = FxSingle.of(
        CurrencyAmount.of(currencyPair.getBase(), baseAmount),
        CurrencyAmount.of(currencyPair.getCounter(), counterAmount),
        paymentDate);

    return FxVanillaOption.builder()
        .longShort(longShort)
        .expiryDate(expiry.toLocalDate())
        .expiryTime(expiry.toLocalTime())
        .expiryZone(expiry.getZone())
        .underlying(equivalentUnderlying)
        .build();
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    inOrderOrEqual(expiryDate, underlying.getPaymentDate(), "expiryDate", "underlying.paymentDate");
  }

  //-------------------------------------------------------------------------

  /**
   * Gets currency pair of the base currency and counter currency.
   * <p>
   * This currency pair is conventional, thus indifferent to the direction of FX.
   *
   * @return the currency pair
   */
  @Override
  public CurrencyPair getCurrencyPair() {
    return underlying.getCurrencyPair();
  }

  /**
   * Gets the expiry date-time.
   * <p>
   * The option expires at this date and time.
   * <p>
   * The result is returned by combining the expiry date, time and time-zone.
   *
   * @return the expiry date and time
   */
  @Override
  public ZonedDateTime getExpiry() {
    return expiryDate.atTime(expiryTime).atZone(expiryZone);
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedFxVanillaOption resolve(ReferenceData refData) {
    return ResolvedFxVanillaOption.builder()
        .longShort(longShort)
        .expiry(getExpiry())
        .underlying(underlying.resolve(refData))
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FxVanillaOption}.
   * @return the meta-bean, not null
   */
  public static FxVanillaOption.Meta meta() {
    return FxVanillaOption.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FxVanillaOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxVanillaOption.Builder builder() {
    return new FxVanillaOption.Builder();
  }

  private FxVanillaOption(
      LongShort longShort,
      LocalDate expiryDate,
      LocalTime expiryTime,
      ZoneId expiryZone,
      FxSingle underlying) {
    JodaBeanUtils.notNull(longShort, "longShort");
    JodaBeanUtils.notNull(expiryDate, "expiryDate");
    JodaBeanUtils.notNull(expiryTime, "expiryTime");
    JodaBeanUtils.notNull(expiryZone, "expiryZone");
    JodaBeanUtils.notNull(underlying, "underlying");
    this.longShort = longShort;
    this.expiryDate = expiryDate;
    this.expiryTime = expiryTime;
    this.expiryZone = expiryZone;
    this.underlying = underlying;
    validate();
  }

  @Override
  public FxVanillaOption.Meta metaBean() {
    return FxVanillaOption.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the option is long or short.
   * <p>
   * At expiry, the long party will have the option to enter in this transaction; the short party will, at the option of
   * the long party, potentially enter into the inverse transaction.
   * @return the value of the property, not null
   */
  public LongShort getLongShort() {
    return longShort;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry date of the option.
   * <p>
   * The option is European, and can only be exercised on the expiry date.
   * @return the value of the property, not null
   */
  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry time of the option.
   * <p>
   * The expiry time is related to the expiry date and time-zone.
   * @return the value of the property, not null
   */
  public LocalTime getExpiryTime() {
    return expiryTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-zone of the expiry time.
   * <p>
   * The expiry time-zone is related to the expiry date and time.
   * @return the value of the property, not null
   */
  public ZoneId getExpiryZone() {
    return expiryZone;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying foreign exchange transaction.
   * <p>
   * At expiry, if the option is in the money, this foreign exchange will occur.
   * @return the value of the property, not null
   */
  public FxSingle getUnderlying() {
    return underlying;
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
      FxVanillaOption other = (FxVanillaOption) obj;
      return JodaBeanUtils.equal(longShort, other.longShort) &&
          JodaBeanUtils.equal(expiryDate, other.expiryDate) &&
          JodaBeanUtils.equal(expiryTime, other.expiryTime) &&
          JodaBeanUtils.equal(expiryZone, other.expiryZone) &&
          JodaBeanUtils.equal(underlying, other.underlying);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(longShort);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryZone);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("FxVanillaOption{");
    buf.append("longShort").append('=').append(JodaBeanUtils.toString(longShort)).append(',').append(' ');
    buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
    buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
    buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
    buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxVanillaOption}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code longShort} property.
     */
    private final MetaProperty<LongShort> longShort = DirectMetaProperty.ofImmutable(
        this, "longShort", FxVanillaOption.class, LongShort.class);
    /**
     * The meta-property for the {@code expiryDate} property.
     */
    private final MetaProperty<LocalDate> expiryDate = DirectMetaProperty.ofImmutable(
        this, "expiryDate", FxVanillaOption.class, LocalDate.class);
    /**
     * The meta-property for the {@code expiryTime} property.
     */
    private final MetaProperty<LocalTime> expiryTime = DirectMetaProperty.ofImmutable(
        this, "expiryTime", FxVanillaOption.class, LocalTime.class);
    /**
     * The meta-property for the {@code expiryZone} property.
     */
    private final MetaProperty<ZoneId> expiryZone = DirectMetaProperty.ofImmutable(
        this, "expiryZone", FxVanillaOption.class, ZoneId.class);
    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<FxSingle> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", FxVanillaOption.class, FxSingle.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "longShort",
        "expiryDate",
        "expiryTime",
        "expiryZone",
        "underlying");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return longShort;
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
        case -1770633379:  // underlying
          return underlying;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxVanillaOption.Builder builder() {
      return new FxVanillaOption.Builder();
    }

    @Override
    public Class<? extends FxVanillaOption> beanType() {
      return FxVanillaOption.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code longShort} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LongShort> longShort() {
      return longShort;
    }

    /**
     * The meta-property for the {@code expiryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> expiryDate() {
      return expiryDate;
    }

    /**
     * The meta-property for the {@code expiryTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalTime> expiryTime() {
      return expiryTime;
    }

    /**
     * The meta-property for the {@code expiryZone} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZoneId> expiryZone() {
      return expiryZone;
    }

    /**
     * The meta-property for the {@code underlying} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxSingle> underlying() {
      return underlying;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return ((FxVanillaOption) bean).getLongShort();
        case -816738431:  // expiryDate
          return ((FxVanillaOption) bean).getExpiryDate();
        case -816254304:  // expiryTime
          return ((FxVanillaOption) bean).getExpiryTime();
        case -816069761:  // expiryZone
          return ((FxVanillaOption) bean).getExpiryZone();
        case -1770633379:  // underlying
          return ((FxVanillaOption) bean).getUnderlying();
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
   * The bean-builder for {@code FxVanillaOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxVanillaOption> {

    private LongShort longShort;
    private LocalDate expiryDate;
    private LocalTime expiryTime;
    private ZoneId expiryZone;
    private FxSingle underlying;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FxVanillaOption beanToCopy) {
      this.longShort = beanToCopy.getLongShort();
      this.expiryDate = beanToCopy.getExpiryDate();
      this.expiryTime = beanToCopy.getExpiryTime();
      this.expiryZone = beanToCopy.getExpiryZone();
      this.underlying = beanToCopy.getUnderlying();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return longShort;
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
        case -1770633379:  // underlying
          return underlying;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          this.longShort = (LongShort) newValue;
          break;
        case -816738431:  // expiryDate
          this.expiryDate = (LocalDate) newValue;
          break;
        case -816254304:  // expiryTime
          this.expiryTime = (LocalTime) newValue;
          break;
        case -816069761:  // expiryZone
          this.expiryZone = (ZoneId) newValue;
          break;
        case -1770633379:  // underlying
          this.underlying = (FxSingle) newValue;
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
    public FxVanillaOption build() {
      return new FxVanillaOption(
          longShort,
          expiryDate,
          expiryTime,
          expiryZone,
          underlying);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the option is long or short.
     * <p>
     * At expiry, the long party will have the option to enter in this transaction; the short party will, at the option of
     * the long party, potentially enter into the inverse transaction.
     * @param longShort  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder longShort(LongShort longShort) {
      JodaBeanUtils.notNull(longShort, "longShort");
      this.longShort = longShort;
      return this;
    }

    /**
     * Sets the expiry date of the option.
     * <p>
     * The option is European, and can only be exercised on the expiry date.
     * @param expiryDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryDate(LocalDate expiryDate) {
      JodaBeanUtils.notNull(expiryDate, "expiryDate");
      this.expiryDate = expiryDate;
      return this;
    }

    /**
     * Sets the expiry time of the option.
     * <p>
     * The expiry time is related to the expiry date and time-zone.
     * @param expiryTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryTime(LocalTime expiryTime) {
      JodaBeanUtils.notNull(expiryTime, "expiryTime");
      this.expiryTime = expiryTime;
      return this;
    }

    /**
     * Sets the time-zone of the expiry time.
     * <p>
     * The expiry time-zone is related to the expiry date and time.
     * @param expiryZone  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryZone(ZoneId expiryZone) {
      JodaBeanUtils.notNull(expiryZone, "expiryZone");
      this.expiryZone = expiryZone;
      return this;
    }

    /**
     * Sets the underlying foreign exchange transaction.
     * <p>
     * At expiry, if the option is in the money, this foreign exchange will occur.
     * @param underlying  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlying(FxSingle underlying) {
      JodaBeanUtils.notNull(underlying, "underlying");
      this.underlying = underlying;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("FxVanillaOption.Builder{");
      buf.append("longShort").append('=').append(JodaBeanUtils.toString(longShort)).append(',').append(' ');
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
      buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
      buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
