/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

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
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An FX digital option.
 * <p>
 * An FX digital option is a financial instrument that pays one unit of a currency based on the future value of
 * a foreign exchange. The option is European, exercised only on the exercise date.
 * <p>
 * For a call, the option holder receives one unit of base/counter currency if the exchange spot rate is above 
 * the strike rate at expiry, and the payoff is zero otherwise. 
 * For a put, the option holder receives one unit of base/counter currency if the exchange spot rate is below 
 * the strike rate at expiry, and the payoff is zero otherwise. 
 * <p>
 * For example, a call on an EUR/USD exchange with strike 1.4 and EUR delivery pays one unit of EUR if the spot 
 * at expiry is above 1.4. A put on the same exchange pays one unit of EUR if the spot at expiry is below 1.4.
 */
@BeanDefinition
public final class FxDigitalOption
    implements FxDigitalOptionProduct, ImmutableBean, Serializable {

  /**
   * Whether the option is put or call.
   * <p>
   * A call pays one unit of base/counter currency if the exchange spot rate is above the strike rate at expiry.
   * A put pays one unit of base/counter currency if the exchange spot rate is below the strike rate at expiry.
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

  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double notional;
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
   * The date that option's payoff settles.
   * <p>
   * This should not be before expiry date. 
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;
  /**
   * The currency in which the unit amount is delivered. 
   * <p>
   * This is be one of the currency pair of the FX index.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency payoffCurrency;
  /**
   * The index defining the FX rate to observe on the expiry date.
   * <p>
   * The index is used to decide the exercisability of the option by providing the actual FX rate on the expiry date.
   * The value of the trade is based on the relative magnitude between the actual rate and the agreed rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxIndex index;
  /**
   * The strike of the option.
   * <p>
   * The moneyness of the option is determined based on this strike in terms of direction and value. 
   * The currency pair of the strike should match the currency pair (or the inverse of the pair) of {@code index}. 
   */
  @PropertyDefinition(validate = "notNull")
  private final FxRate strike;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    CurrencyPair underlyingPair = index.getCurrencyPair();
    ArgChecker.isTrue(strike.getPair().equals(underlyingPair) || strike.getPair().isInverse(underlyingPair),
        "currency pair mismatch between strike and index");
    ArgChecker.isTrue(underlyingPair.contains(payoffCurrency));
    inOrderOrEqual(expiryDate, paymentDate, "expiryDate", "paymentDate");
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null && builder.expiryDate != null && builder.paymentDate == null) {
      builder.paymentDate = builder.index.calculateMaturityFromFixing(builder.expiryDate);
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
   * Get the base currency of {@code strike}. 
   * 
   * @return the base currency
   */
  public Currency getStrikeBaseCurrency() {
    return strike.getPair().getBase();
  }

  /**
   * Get the counter currency of {@code strike}.
   * 
   * @return the counter currency
   */
  public Currency getStrikeCounterCurrency() {
    return strike.getPair().getCounter();
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this FX option, trivially returning {@code this}.
   * 
   * @return this
   */
  @Override
  public FxDigitalOption expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxDigitalOption}.
   * @return the meta-bean, not null
   */
  public static FxDigitalOption.Meta meta() {
    return FxDigitalOption.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxDigitalOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxDigitalOption.Builder builder() {
    return new FxDigitalOption.Builder();
  }

  private FxDigitalOption(
      PutCall putCall,
      LongShort longShort,
      double notional,
      LocalDate expiryDate,
      LocalTime expiryTime,
      ZoneId expiryZone,
      LocalDate paymentDate,
      Currency payoffCurrency,
      FxIndex index,
      FxRate strike) {
    JodaBeanUtils.notNull(putCall, "putCall");
    JodaBeanUtils.notNull(longShort, "longShort");
    ArgChecker.notNegative(notional, "notional");
    JodaBeanUtils.notNull(expiryDate, "expiryDate");
    JodaBeanUtils.notNull(expiryTime, "expiryTime");
    JodaBeanUtils.notNull(expiryZone, "expiryZone");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    JodaBeanUtils.notNull(payoffCurrency, "payoffCurrency");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(strike, "strike");
    this.putCall = putCall;
    this.longShort = longShort;
    this.notional = notional;
    this.expiryDate = expiryDate;
    this.expiryTime = expiryTime;
    this.expiryZone = expiryZone;
    this.paymentDate = paymentDate;
    this.payoffCurrency = payoffCurrency;
    this.index = index;
    this.strike = strike;
    validate();
  }

  @Override
  public FxDigitalOption.Meta metaBean() {
    return FxDigitalOption.Meta.INSTANCE;
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
   * A call pays one unit of base/counter currency if the exchange spot rate is above the strike rate at expiry.
   * A put pays one unit of base/counter currency if the exchange spot rate is below the strike rate at expiry.
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
   * Gets the notional.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
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
   * Gets the date that option's payoff settles.
   * <p>
   * This should not be before expiry date.
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency in which the unit amount is delivered.
   * <p>
   * This is be one of the currency pair of the FX index.
   * @return the value of the property, not null
   */
  public Currency getPayoffCurrency() {
    return payoffCurrency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index defining the FX rate to observe on the expiry date.
   * <p>
   * The index is used to decide the exercisability of the option by providing the actual FX rate on the expiry date.
   * The value of the trade is based on the relative magnitude between the actual rate and the agreed rate.
   * @return the value of the property, not null
   */
  public FxIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike of the option.
   * <p>
   * The moneyness of the option is determined based on this strike in terms of direction and value.
   * The currency pair of the strike should match the currency pair (or the inverse of the pair) of {@code index}.
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
      FxDigitalOption other = (FxDigitalOption) obj;
      return JodaBeanUtils.equal(getPutCall(), other.getPutCall()) &&
          JodaBeanUtils.equal(getLongShort(), other.getLongShort()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getExpiryDate(), other.getExpiryDate()) &&
          JodaBeanUtils.equal(getExpiryTime(), other.getExpiryTime()) &&
          JodaBeanUtils.equal(getExpiryZone(), other.getExpiryZone()) &&
          JodaBeanUtils.equal(getPaymentDate(), other.getPaymentDate()) &&
          JodaBeanUtils.equal(getPayoffCurrency(), other.getPayoffCurrency()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getStrike(), other.getStrike());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getPutCall());
    hash = hash * 31 + JodaBeanUtils.hashCode(getLongShort());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExpiryDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExpiryTime());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExpiryZone());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPayoffCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStrike());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("FxDigitalOption{");
    buf.append("putCall").append('=').append(getPutCall()).append(',').append(' ');
    buf.append("longShort").append('=').append(getLongShort()).append(',').append(' ');
    buf.append("notional").append('=').append(getNotional()).append(',').append(' ');
    buf.append("expiryDate").append('=').append(getExpiryDate()).append(',').append(' ');
    buf.append("expiryTime").append('=').append(getExpiryTime()).append(',').append(' ');
    buf.append("expiryZone").append('=').append(getExpiryZone()).append(',').append(' ');
    buf.append("paymentDate").append('=').append(getPaymentDate()).append(',').append(' ');
    buf.append("payoffCurrency").append('=').append(getPayoffCurrency()).append(',').append(' ');
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("strike").append('=').append(JodaBeanUtils.toString(getStrike()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxDigitalOption}.
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
        this, "putCall", FxDigitalOption.class, PutCall.class);
    /**
     * The meta-property for the {@code longShort} property.
     */
    private final MetaProperty<LongShort> longShort = DirectMetaProperty.ofImmutable(
        this, "longShort", FxDigitalOption.class, LongShort.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", FxDigitalOption.class, Double.TYPE);
    /**
     * The meta-property for the {@code expiryDate} property.
     */
    private final MetaProperty<LocalDate> expiryDate = DirectMetaProperty.ofImmutable(
        this, "expiryDate", FxDigitalOption.class, LocalDate.class);
    /**
     * The meta-property for the {@code expiryTime} property.
     */
    private final MetaProperty<LocalTime> expiryTime = DirectMetaProperty.ofImmutable(
        this, "expiryTime", FxDigitalOption.class, LocalTime.class);
    /**
     * The meta-property for the {@code expiryZone} property.
     */
    private final MetaProperty<ZoneId> expiryZone = DirectMetaProperty.ofImmutable(
        this, "expiryZone", FxDigitalOption.class, ZoneId.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", FxDigitalOption.class, LocalDate.class);
    /**
     * The meta-property for the {@code payoffCurrency} property.
     */
    private final MetaProperty<Currency> payoffCurrency = DirectMetaProperty.ofImmutable(
        this, "payoffCurrency", FxDigitalOption.class, Currency.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", FxDigitalOption.class, FxIndex.class);
    /**
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<FxRate> strike = DirectMetaProperty.ofImmutable(
        this, "strike", FxDigitalOption.class, FxRate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "putCall",
        "longShort",
        "notional",
        "expiryDate",
        "expiryTime",
        "expiryZone",
        "paymentDate",
        "payoffCurrency",
        "index",
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
        case 1585636160:  // notional
          return notional;
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
        case -1540873516:  // paymentDate
          return paymentDate;
        case -243533576:  // payoffCurrency
          return payoffCurrency;
        case 100346066:  // index
          return index;
        case -891985998:  // strike
          return strike;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxDigitalOption.Builder builder() {
      return new FxDigitalOption.Builder();
    }

    @Override
    public Class<? extends FxDigitalOption> beanType() {
      return FxDigitalOption.class;
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
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
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
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    /**
     * The meta-property for the {@code payoffCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> payoffCurrency() {
      return payoffCurrency;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndex> index() {
      return index;
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
          return ((FxDigitalOption) bean).getPutCall();
        case 116685664:  // longShort
          return ((FxDigitalOption) bean).getLongShort();
        case 1585636160:  // notional
          return ((FxDigitalOption) bean).getNotional();
        case -816738431:  // expiryDate
          return ((FxDigitalOption) bean).getExpiryDate();
        case -816254304:  // expiryTime
          return ((FxDigitalOption) bean).getExpiryTime();
        case -816069761:  // expiryZone
          return ((FxDigitalOption) bean).getExpiryZone();
        case -1540873516:  // paymentDate
          return ((FxDigitalOption) bean).getPaymentDate();
        case -243533576:  // payoffCurrency
          return ((FxDigitalOption) bean).getPayoffCurrency();
        case 100346066:  // index
          return ((FxDigitalOption) bean).getIndex();
        case -891985998:  // strike
          return ((FxDigitalOption) bean).getStrike();
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
   * The bean-builder for {@code FxDigitalOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxDigitalOption> {

    private PutCall putCall;
    private LongShort longShort;
    private double notional;
    private LocalDate expiryDate;
    private LocalTime expiryTime;
    private ZoneId expiryZone;
    private LocalDate paymentDate;
    private Currency payoffCurrency;
    private FxIndex index;
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
    private Builder(FxDigitalOption beanToCopy) {
      this.putCall = beanToCopy.getPutCall();
      this.longShort = beanToCopy.getLongShort();
      this.notional = beanToCopy.getNotional();
      this.expiryDate = beanToCopy.getExpiryDate();
      this.expiryTime = beanToCopy.getExpiryTime();
      this.expiryZone = beanToCopy.getExpiryZone();
      this.paymentDate = beanToCopy.getPaymentDate();
      this.payoffCurrency = beanToCopy.getPayoffCurrency();
      this.index = beanToCopy.getIndex();
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
        case 1585636160:  // notional
          return notional;
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
        case -1540873516:  // paymentDate
          return paymentDate;
        case -243533576:  // payoffCurrency
          return payoffCurrency;
        case 100346066:  // index
          return index;
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
        case 1585636160:  // notional
          this.notional = (Double) newValue;
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
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case -243533576:  // payoffCurrency
          this.payoffCurrency = (Currency) newValue;
          break;
        case 100346066:  // index
          this.index = (FxIndex) newValue;
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
    public FxDigitalOption build() {
      preBuild(this);
      return new FxDigitalOption(
          putCall,
          longShort,
          notional,
          expiryDate,
          expiryTime,
          expiryZone,
          paymentDate,
          payoffCurrency,
          index,
          strike);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the option is put or call.
     * <p>
     * A call pays one unit of base/counter currency if the exchange spot rate is above the strike rate at expiry.
     * A put pays one unit of base/counter currency if the exchange spot rate is below the strike rate at expiry.
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
     * Sets the notional.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegative(notional, "notional");
      this.notional = notional;
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
     * Sets the date that option's payoff settles.
     * <p>
     * This should not be before expiry date.
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

    /**
     * Sets the currency in which the unit amount is delivered.
     * <p>
     * This is be one of the currency pair of the FX index.
     * @param payoffCurrency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payoffCurrency(Currency payoffCurrency) {
      JodaBeanUtils.notNull(payoffCurrency, "payoffCurrency");
      this.payoffCurrency = payoffCurrency;
      return this;
    }

    /**
     * Sets the index defining the FX rate to observe on the expiry date.
     * <p>
     * The index is used to decide the exercisability of the option by providing the actual FX rate on the expiry date.
     * The value of the trade is based on the relative magnitude between the actual rate and the agreed rate.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(FxIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the strike of the option.
     * <p>
     * The moneyness of the option is determined based on this strike in terms of direction and value.
     * The currency pair of the strike should match the currency pair (or the inverse of the pair) of {@code index}.
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
      StringBuilder buf = new StringBuilder(352);
      buf.append("FxDigitalOption.Builder{");
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("longShort").append('=').append(JodaBeanUtils.toString(longShort)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
      buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
      buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("payoffCurrency").append('=').append(JodaBeanUtils.toString(payoffCurrency)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
