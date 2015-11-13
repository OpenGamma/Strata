/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.collect.ArgChecker.inOrderOrEqual;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A vanilla FX option.
 * <p>
 * An FX option is a financial instrument that provides an option based on the future value of
 * a foreign exchange. The option is European, exercised only on the exercise date.
 * <p>
 * If the option is a call, the option holder has the right to enter into the specified exchange.
 * If the option is a put, the option holder has the right to enter into the opposite of the specified exchange.
 * <p>
 * For example, a call on a 'EUR 1.00 / USD -1.41' exchange is the option to
 * perform a foreign exchange on the expiry date, where USD 1.41 is paid to receive EUR 1.00.
 * A put on the same exchange is the option to pay EUR 1.00 and receive USD 1.41.
 */
@BeanDefinition
public final class FxVanillaOption
    implements FxVanillaOptionProduct, ImmutableBean, Serializable {

  /**
   * Whether the option is put or call.
   * <p>
   * A call gives the owner the right, but not obligation, to exercise the underlying foreign exchange.
   * A put gives a similar option to exercise the inverse of the underlying.
   */
  @PropertyDefinition(validate = "notNull")
  private final PutCall putCall;
  /**
   * Whether the option is long or short.
   * <p>
   * Long indicates that the owner wants the option to be in the money at expiry.
   * Short indicates that the owner wants the option to be out of the money at expiry.
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
   * A call option permits the transaction as specified to occur.
   * A put option permits the inverse transaction to occur.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxSingle underlying;
  /**
   * The strike of the option.
   * <p>
   * The moneyness of the option is determined based on this strike. 
   */
  @PropertyDefinition(validate = "notNull")
  private final FxRate strike;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    CurrencyPair underlyingPair = underlying.getCurrencyPair();
    ArgChecker.isTrue(strike.getPair().equals(underlyingPair) || strike.getPair().isInverse(underlyingPair),
        "currency pair mismatch between strike and underlying");
    inOrderOrEqual(expiryDate, underlying.getPaymentDate(), "expiryDate", "underlying.paymentDate");
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    // set the direction of the strike to be the same as the underlying.
    if (!builder.strike.getPair().getBase().equals(builder.underlying.getReceiveCurrencyAmount().getCurrency())) {
      builder.strike = builder.strike.inverse();
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the expiry date-time.
   * <p>
   * The option expires at this date and time.
   * <p>
   * The result is returned by combining the expiry date, time and time-zone.
   * 
   * @return the expiry date and time
   */
  public ZonedDateTime getExpiryDateTime() {
    return expiryDate.atTime(expiryTime).atZone(expiryZone);
  }

  /**
   * Gets the currency on which the payoff occurs. 
   * 
   * @return the payoff currency
   */
  public Currency getPayoffCurrency() {
    return strike.getPair().getCounter();
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this FX option, trivially returning {@code this}.
   * 
   * @return this
   */
  @Override
  public FxVanillaOption expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxVanillaOption}.
   * @return the meta-bean, not null
   */
  public static FxVanillaOption.Meta meta() {
    return FxVanillaOption.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxVanillaOption.Meta.INSTANCE);
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
      PutCall putCall,
      LongShort longShort,
      LocalDate expiryDate,
      LocalTime expiryTime,
      ZoneId expiryZone,
      FxSingle underlying,
      FxRate strike) {
    JodaBeanUtils.notNull(putCall, "putCall");
    JodaBeanUtils.notNull(longShort, "longShort");
    JodaBeanUtils.notNull(expiryDate, "expiryDate");
    JodaBeanUtils.notNull(expiryTime, "expiryTime");
    JodaBeanUtils.notNull(expiryZone, "expiryZone");
    JodaBeanUtils.notNull(underlying, "underlying");
    JodaBeanUtils.notNull(strike, "strike");
    this.putCall = putCall;
    this.longShort = longShort;
    this.expiryDate = expiryDate;
    this.expiryTime = expiryTime;
    this.expiryZone = expiryZone;
    this.underlying = underlying;
    this.strike = strike;
    validate();
  }

  @Override
  public FxVanillaOption.Meta metaBean() {
    return FxVanillaOption.Meta.INSTANCE;
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
   * Gets whether the option is put or call.
   * <p>
   * A call gives the owner the right, but not obligation, to exercise the underlying foreign exchange.
   * A put gives a similar option to exercise the inverse of the underlying.
   * @return the value of the property, not null
   */
  public PutCall getPutCall() {
    return putCall;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the option is long or short.
   * <p>
   * Long indicates that the owner wants the option to be in the money at expiry.
   * Short indicates that the owner wants the option to be out of the money at expiry.
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
   * A call option permits the transaction as specified to occur.
   * A put option permits the inverse transaction to occur.
   * @return the value of the property, not null
   */
  public FxSingle getUnderlying() {
    return underlying;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike of the option.
   * <p>
   * The moneyness of the option is determined based on this strike.
   * @return the value of the property, not null
   */
  public FxRate getStrike() {
    return strike;
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
      return JodaBeanUtils.equal(putCall, other.putCall) &&
          JodaBeanUtils.equal(longShort, other.longShort) &&
          JodaBeanUtils.equal(expiryDate, other.expiryDate) &&
          JodaBeanUtils.equal(expiryTime, other.expiryTime) &&
          JodaBeanUtils.equal(expiryZone, other.expiryZone) &&
          JodaBeanUtils.equal(underlying, other.underlying) &&
          JodaBeanUtils.equal(strike, other.strike);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(putCall);
    hash = hash * 31 + JodaBeanUtils.hashCode(longShort);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryZone);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    hash = hash * 31 + JodaBeanUtils.hashCode(strike);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("FxVanillaOption{");
    buf.append("putCall").append('=').append(putCall).append(',').append(' ');
    buf.append("longShort").append('=').append(longShort).append(',').append(' ');
    buf.append("expiryDate").append('=').append(expiryDate).append(',').append(' ');
    buf.append("expiryTime").append('=').append(expiryTime).append(',').append(' ');
    buf.append("expiryZone").append('=').append(expiryZone).append(',').append(' ');
    buf.append("underlying").append('=').append(underlying).append(',').append(' ');
    buf.append("strike").append('=').append(JodaBeanUtils.toString(strike));
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
     * The meta-property for the {@code putCall} property.
     */
    private final MetaProperty<PutCall> putCall = DirectMetaProperty.ofImmutable(
        this, "putCall", FxVanillaOption.class, PutCall.class);
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
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<FxRate> strike = DirectMetaProperty.ofImmutable(
        this, "strike", FxVanillaOption.class, FxRate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "putCall",
        "longShort",
        "expiryDate",
        "expiryTime",
        "expiryZone",
        "underlying",
        "strike");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -219971059:  // putCall
          return putCall;
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
        case -891985998:  // strike
          return strike;
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
     * The meta-property for the {@code putCall} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PutCall> putCall() {
      return putCall;
    }

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

    /**
     * The meta-property for the {@code strike} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxRate> strike() {
      return strike;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -219971059:  // putCall
          return ((FxVanillaOption) bean).getPutCall();
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
        case -891985998:  // strike
          return ((FxVanillaOption) bean).getStrike();
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

    private PutCall putCall;
    private LongShort longShort;
    private LocalDate expiryDate;
    private LocalTime expiryTime;
    private ZoneId expiryZone;
    private FxSingle underlying;
    private FxRate strike;

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
      this.putCall = beanToCopy.getPutCall();
      this.longShort = beanToCopy.getLongShort();
      this.expiryDate = beanToCopy.getExpiryDate();
      this.expiryTime = beanToCopy.getExpiryTime();
      this.expiryZone = beanToCopy.getExpiryZone();
      this.underlying = beanToCopy.getUnderlying();
      this.strike = beanToCopy.getStrike();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -219971059:  // putCall
          return putCall;
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
        case -891985998:  // strike
          return strike;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -219971059:  // putCall
          this.putCall = (PutCall) newValue;
          break;
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
        case -891985998:  // strike
          this.strike = (FxRate) newValue;
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
    public FxVanillaOption build() {
      preBuild(this);
      return new FxVanillaOption(
          putCall,
          longShort,
          expiryDate,
          expiryTime,
          expiryZone,
          underlying,
          strike);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the option is put or call.
     * <p>
     * A call gives the owner the right, but not obligation, to exercise the underlying foreign exchange.
     * A put gives a similar option to exercise the inverse of the underlying.
     * @param putCall  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder putCall(PutCall putCall) {
      JodaBeanUtils.notNull(putCall, "putCall");
      this.putCall = putCall;
      return this;
    }

    /**
     * Sets whether the option is long or short.
     * <p>
     * Long indicates that the owner wants the option to be in the money at expiry.
     * Short indicates that the owner wants the option to be out of the money at expiry.
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
     * A call option permits the transaction as specified to occur.
     * A put option permits the inverse transaction to occur.
     * @param underlying  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlying(FxSingle underlying) {
      JodaBeanUtils.notNull(underlying, "underlying");
      this.underlying = underlying;
      return this;
    }

    /**
     * Sets the strike of the option.
     * <p>
     * The moneyness of the option is determined based on this strike.
     * @param strike  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder strike(FxRate strike) {
      JodaBeanUtils.notNull(strike, "strike");
      this.strike = strike;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("FxVanillaOption.Builder{");
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("longShort").append('=').append(JodaBeanUtils.toString(longShort)).append(',').append(' ');
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
      buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
      buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
