/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.util.Set;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.index.FxIndex;
import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
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
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.basics.currency.CurrencyPair;

/**
 * A binary FX option.
 * <p>
 * A binary FX option is a financial instrument that provides a payoff based on the future value of
 * a foreign exchange index. The currency unit of the payoff can be in either currency unit of the foreign exchange.
 * The option is European, exercised only on the exercise date.
 * <p>
 * For example, a binary call with a strike rate 'EUR 1.00 / USD 1.1' and a payment amount USD 100 pays
 * this amount at expiry if the future value of the foreign exchange index is at least as great as the
 * strike rate.
 */
@BeanDefinition
public final class FxBinaryOption
        implements Product, Resolvable<ResolvedFxBinaryOption>, ImmutableBean, Serializable {

    /**
     * Whether the option is long or short.
     * <p>
     * At expiry, the long party will receive the payment amount in the specified currency if the level of the
     * foreign exchange index is at or above the strike rate; in this case, the short party will have to deliver
     * the payment amount in the specified currency to the long party.
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
     * The reference FX Index of the option.
     * <p>
     * The reference FX Index is used to determine whether or not a payment event has occurred at the expiry
     * of the option.
     */
    @PropertyDefinition(validate = "notNull")
    private final FxIndex underlying;
    /**
     * The amount and currency of the option payment.
     * <p>
     * The payment amount that will be made to the long party in the specified currency, if at expiry, a payment
     * event has occurred.
     */
    @PropertyDefinition(validate = "notNull")
    private final Payment paymentCurrencyAmount;

    //-------------------------------------------------------------------------
    @ImmutableValidator
    private void validate() {
        inOrderOrEqual(expiryDate, paymentCurrencyAmount.getDate(), "expiryDate", "underlying.paymentDate");
    }

    //-------------------------------------------------------------------------
    /**
     * Gets currency pair of the base currency and counter currency.
     * <p>
     * This currency pair is conventional, thus indifferent to the direction of FX.
     *
     * @return the currency pair
     */
    public CurrencyPair getCurrencyPair() { return underlying.getCurrencyPair(); }

    /**
     * Gets the expiry date-time.
     * <p>
     * The option expires at this date and time.
     * <p>
     * The result is returned by combining the expiry date, time and time-zone.
     *
     * @return the expiry date and time
     */
    public ZonedDateTime getExpiry() {
        return expiryDate.atTime(expiryTime).atZone(expiryZone);
    }

    //-------------------------------------------------------------------------
    @Override
    public ResolvedFxBinaryOption resolve(ReferenceData refData) {
        return ResolvedFxBinaryOption.builder()
                .longShort(longShort)
                .expiry(getExpiry())
                .underlying(underlying)
                .build();
    }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxBinaryOption}.
   * @return the meta-bean, not null
   */
  public static FxBinaryOption.Meta meta() {
    return FxBinaryOption.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxBinaryOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxBinaryOption.Builder builder() {
    return new FxBinaryOption.Builder();
  }

  private FxBinaryOption(
      LongShort longShort,
      LocalDate expiryDate,
      LocalTime expiryTime,
      ZoneId expiryZone,
      FxIndex underlying,
      Payment paymentCurrencyAmount) {
    JodaBeanUtils.notNull(longShort, "longShort");
    JodaBeanUtils.notNull(expiryDate, "expiryDate");
    JodaBeanUtils.notNull(expiryTime, "expiryTime");
    JodaBeanUtils.notNull(expiryZone, "expiryZone");
    JodaBeanUtils.notNull(underlying, "underlying");
    JodaBeanUtils.notNull(paymentCurrencyAmount, "paymentCurrencyAmount");
    this.longShort = longShort;
    this.expiryDate = expiryDate;
    this.expiryTime = expiryTime;
    this.expiryZone = expiryZone;
    this.underlying = underlying;
    this.paymentCurrencyAmount = paymentCurrencyAmount;
    validate();
  }

  @Override
  public FxBinaryOption.Meta metaBean() {
    return FxBinaryOption.Meta.INSTANCE;
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
   * Gets whether the option is long or short.
   * <p>
   * At expiry, the long party will receive the payment amount in the specified currency if the level of the
   * foreign exchange index is at or above the strike rate; in this case, the short party will have to deliver
   * the payment amount in the specified currency to the long party.
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
   * Gets the reference FX Index of the option.
   * <p>
   * The reference FX Index is used to determine whether or not a payment event has occurred at the expiry
   * of the option.
   * @return the value of the property, not null
   */
  public FxIndex getUnderlying() {
    return underlying;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the amount and currency of the option payment.
   * <p>
   * The payment amount that will be made to the long party in the specified currency, if at expiry, a payment
   * event has occurred.
   * @return the value of the property, not null
   */
  public Payment getPaymentCurrencyAmount() {
    return paymentCurrencyAmount;
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
      FxBinaryOption other = (FxBinaryOption) obj;
      return JodaBeanUtils.equal(longShort, other.longShort) &&
          JodaBeanUtils.equal(expiryDate, other.expiryDate) &&
          JodaBeanUtils.equal(expiryTime, other.expiryTime) &&
          JodaBeanUtils.equal(expiryZone, other.expiryZone) &&
          JodaBeanUtils.equal(underlying, other.underlying) &&
          JodaBeanUtils.equal(paymentCurrencyAmount, other.paymentCurrencyAmount);
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
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentCurrencyAmount);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("FxBinaryOption{");
    buf.append("longShort").append('=').append(longShort).append(',').append(' ');
    buf.append("expiryDate").append('=').append(expiryDate).append(',').append(' ');
    buf.append("expiryTime").append('=').append(expiryTime).append(',').append(' ');
    buf.append("expiryZone").append('=').append(expiryZone).append(',').append(' ');
    buf.append("underlying").append('=').append(underlying).append(',').append(' ');
    buf.append("paymentCurrencyAmount").append('=').append(JodaBeanUtils.toString(paymentCurrencyAmount));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxBinaryOption}.
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
        this, "longShort", FxBinaryOption.class, LongShort.class);
    /**
     * The meta-property for the {@code expiryDate} property.
     */
    private final MetaProperty<LocalDate> expiryDate = DirectMetaProperty.ofImmutable(
        this, "expiryDate", FxBinaryOption.class, LocalDate.class);
    /**
     * The meta-property for the {@code expiryTime} property.
     */
    private final MetaProperty<LocalTime> expiryTime = DirectMetaProperty.ofImmutable(
        this, "expiryTime", FxBinaryOption.class, LocalTime.class);
    /**
     * The meta-property for the {@code expiryZone} property.
     */
    private final MetaProperty<ZoneId> expiryZone = DirectMetaProperty.ofImmutable(
        this, "expiryZone", FxBinaryOption.class, ZoneId.class);
    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<FxIndex> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", FxBinaryOption.class, FxIndex.class);
    /**
     * The meta-property for the {@code paymentCurrencyAmount} property.
     */
    private final MetaProperty<Payment> paymentCurrencyAmount = DirectMetaProperty.ofImmutable(
        this, "paymentCurrencyAmount", FxBinaryOption.class, Payment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "longShort",
        "expiryDate",
        "expiryTime",
        "expiryZone",
        "underlying",
        "paymentCurrencyAmount");

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
        case 944314223:  // paymentCurrencyAmount
          return paymentCurrencyAmount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxBinaryOption.Builder builder() {
      return new FxBinaryOption.Builder();
    }

    @Override
    public Class<? extends FxBinaryOption> beanType() {
      return FxBinaryOption.class;
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
    public MetaProperty<FxIndex> underlying() {
      return underlying;
    }

    /**
     * The meta-property for the {@code paymentCurrencyAmount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Payment> paymentCurrencyAmount() {
      return paymentCurrencyAmount;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return ((FxBinaryOption) bean).getLongShort();
        case -816738431:  // expiryDate
          return ((FxBinaryOption) bean).getExpiryDate();
        case -816254304:  // expiryTime
          return ((FxBinaryOption) bean).getExpiryTime();
        case -816069761:  // expiryZone
          return ((FxBinaryOption) bean).getExpiryZone();
        case -1770633379:  // underlying
          return ((FxBinaryOption) bean).getUnderlying();
        case 944314223:  // paymentCurrencyAmount
          return ((FxBinaryOption) bean).getPaymentCurrencyAmount();
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
   * The bean-builder for {@code FxBinaryOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxBinaryOption> {

    private LongShort longShort;
    private LocalDate expiryDate;
    private LocalTime expiryTime;
    private ZoneId expiryZone;
    private FxIndex underlying;
    private Payment paymentCurrencyAmount;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FxBinaryOption beanToCopy) {
      this.longShort = beanToCopy.getLongShort();
      this.expiryDate = beanToCopy.getExpiryDate();
      this.expiryTime = beanToCopy.getExpiryTime();
      this.expiryZone = beanToCopy.getExpiryZone();
      this.underlying = beanToCopy.getUnderlying();
      this.paymentCurrencyAmount = beanToCopy.getPaymentCurrencyAmount();
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
        case 944314223:  // paymentCurrencyAmount
          return paymentCurrencyAmount;
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
          this.underlying = (FxIndex) newValue;
          break;
        case 944314223:  // paymentCurrencyAmount
          this.paymentCurrencyAmount = (Payment) newValue;
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
    public FxBinaryOption build() {
      return new FxBinaryOption(
          longShort,
          expiryDate,
          expiryTime,
          expiryZone,
          underlying,
          paymentCurrencyAmount);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the option is long or short.
     * <p>
     * At expiry, the long party will receive the payment amount in the specified currency if the level of the
     * foreign exchange index is at or above the strike rate; in this case, the short party will have to deliver
     * the payment amount in the specified currency to the long party.
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
     * Sets the reference FX Index of the option.
     * <p>
     * The reference FX Index is used to determine whether or not a payment event has occurred at the expiry
     * of the option.
     * @param underlying  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlying(FxIndex underlying) {
      JodaBeanUtils.notNull(underlying, "underlying");
      this.underlying = underlying;
      return this;
    }

    /**
     * Sets the amount and currency of the option payment.
     * <p>
     * The payment amount that will be made to the long party in the specified currency, if at expiry, a payment
     * event has occurred.
     * @param paymentCurrencyAmount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentCurrencyAmount(Payment paymentCurrencyAmount) {
      JodaBeanUtils.notNull(paymentCurrencyAmount, "paymentCurrencyAmount");
      this.paymentCurrencyAmount = paymentCurrencyAmount;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("FxBinaryOption.Builder{");
      buf.append("longShort").append('=').append(JodaBeanUtils.toString(longShort)).append(',').append(' ');
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
      buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
      buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
      buf.append("paymentCurrencyAmount").append('=').append(JodaBeanUtils.toString(paymentCurrencyAmount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
