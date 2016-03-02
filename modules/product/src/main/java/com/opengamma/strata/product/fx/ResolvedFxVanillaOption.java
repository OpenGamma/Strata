/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.collect.ArgChecker.inOrderOrEqual;

import java.io.Serializable;
import java.time.LocalDate;
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
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvedProduct;

/**
 * A vanilla FX option, resolved for pricing.
 * <p>
 * This is the resolved form of {@link FxVanillaOption} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedFxVanillaOption} from a {@code FxVanillaOption}
 * using {@link FxVanillaOption#resolve(ReferenceData)}.
 * <p>
 * If the option is a call, the option holder has the right to enter into the specified exchange.
 * If the option is a put, the option holder has the right to enter into the opposite of the specified exchange.
 * <p>
 * A {@code ResolvedFxVanillaOption} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedFxVanillaOption
    implements ResolvedProduct, ImmutableBean, Serializable {

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
   * The expiry date-time of the option.
   * <p>
   * The option is European, and can only be exercised on the expiry date.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZonedDateTime expiry;
  /**
   * The underlying foreign exchange transaction.
   * <p>
   * At expiry, if the option is in the money, this foreign exchange will occur.
   * A call option permits the transaction as specified to occur.
   * A put option permits the inverse transaction to occur.
   */
  @PropertyDefinition(validate = "notNull")
  private final ResolvedFxSingle underlying;
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
    inOrderOrEqual(expiry.toLocalDate(), underlying.getPaymentDate(), "expiry.date", "underlying.paymentDate");
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
   * Gets the expiry date of the option.
   * 
   * @return the expiry date
   */
  public LocalDate getExpiryDate() {
    return expiry.toLocalDate();
  }

  /**
   * Gets the currency on which the payoff occurs. 
   * 
   * @return the payoff currency
   */
  public Currency getPayoffCurrency() {
    return strike.getPair().getCounter();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedFxVanillaOption}.
   * @return the meta-bean, not null
   */
  public static ResolvedFxVanillaOption.Meta meta() {
    return ResolvedFxVanillaOption.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedFxVanillaOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedFxVanillaOption.Builder builder() {
    return new ResolvedFxVanillaOption.Builder();
  }

  private ResolvedFxVanillaOption(
      PutCall putCall,
      LongShort longShort,
      ZonedDateTime expiry,
      ResolvedFxSingle underlying,
      FxRate strike) {
    JodaBeanUtils.notNull(putCall, "putCall");
    JodaBeanUtils.notNull(longShort, "longShort");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(underlying, "underlying");
    JodaBeanUtils.notNull(strike, "strike");
    this.putCall = putCall;
    this.longShort = longShort;
    this.expiry = expiry;
    this.underlying = underlying;
    this.strike = strike;
    validate();
  }

  @Override
  public ResolvedFxVanillaOption.Meta metaBean() {
    return ResolvedFxVanillaOption.Meta.INSTANCE;
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
   * Gets the expiry date-time of the option.
   * <p>
   * The option is European, and can only be exercised on the expiry date.
   * @return the value of the property, not null
   */
  public ZonedDateTime getExpiry() {
    return expiry;
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
  public ResolvedFxSingle getUnderlying() {
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
      ResolvedFxVanillaOption other = (ResolvedFxVanillaOption) obj;
      return JodaBeanUtils.equal(putCall, other.putCall) &&
          JodaBeanUtils.equal(longShort, other.longShort) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    hash = hash * 31 + JodaBeanUtils.hashCode(strike);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("ResolvedFxVanillaOption{");
    buf.append("putCall").append('=').append(putCall).append(',').append(' ');
    buf.append("longShort").append('=').append(longShort).append(',').append(' ');
    buf.append("expiry").append('=').append(expiry).append(',').append(' ');
    buf.append("underlying").append('=').append(underlying).append(',').append(' ');
    buf.append("strike").append('=').append(JodaBeanUtils.toString(strike));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedFxVanillaOption}.
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
        this, "putCall", ResolvedFxVanillaOption.class, PutCall.class);
    /**
     * The meta-property for the {@code longShort} property.
     */
    private final MetaProperty<LongShort> longShort = DirectMetaProperty.ofImmutable(
        this, "longShort", ResolvedFxVanillaOption.class, LongShort.class);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<ZonedDateTime> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", ResolvedFxVanillaOption.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<ResolvedFxSingle> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", ResolvedFxVanillaOption.class, ResolvedFxSingle.class);
    /**
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<FxRate> strike = DirectMetaProperty.ofImmutable(
        this, "strike", ResolvedFxVanillaOption.class, FxRate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "putCall",
        "longShort",
        "expiry",
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
        case -1289159373:  // expiry
          return expiry;
        case -1770633379:  // underlying
          return underlying;
        case -891985998:  // strike
          return strike;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedFxVanillaOption.Builder builder() {
      return new ResolvedFxVanillaOption.Builder();
    }

    @Override
    public Class<? extends ResolvedFxVanillaOption> beanType() {
      return ResolvedFxVanillaOption.class;
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
     * The meta-property for the {@code expiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> expiry() {
      return expiry;
    }

    /**
     * The meta-property for the {@code underlying} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ResolvedFxSingle> underlying() {
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
          return ((ResolvedFxVanillaOption) bean).getPutCall();
        case 116685664:  // longShort
          return ((ResolvedFxVanillaOption) bean).getLongShort();
        case -1289159373:  // expiry
          return ((ResolvedFxVanillaOption) bean).getExpiry();
        case -1770633379:  // underlying
          return ((ResolvedFxVanillaOption) bean).getUnderlying();
        case -891985998:  // strike
          return ((ResolvedFxVanillaOption) bean).getStrike();
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
   * The bean-builder for {@code ResolvedFxVanillaOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedFxVanillaOption> {

    private PutCall putCall;
    private LongShort longShort;
    private ZonedDateTime expiry;
    private ResolvedFxSingle underlying;
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
    private Builder(ResolvedFxVanillaOption beanToCopy) {
      this.putCall = beanToCopy.getPutCall();
      this.longShort = beanToCopy.getLongShort();
      this.expiry = beanToCopy.getExpiry();
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
        case -1289159373:  // expiry
          return expiry;
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
        case -1289159373:  // expiry
          this.expiry = (ZonedDateTime) newValue;
          break;
        case -1770633379:  // underlying
          this.underlying = (ResolvedFxSingle) newValue;
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
    public ResolvedFxVanillaOption build() {
      preBuild(this);
      return new ResolvedFxVanillaOption(
          putCall,
          longShort,
          expiry,
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
     * Sets the expiry date-time of the option.
     * <p>
     * The option is European, and can only be exercised on the expiry date.
     * @param expiry  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiry(ZonedDateTime expiry) {
      JodaBeanUtils.notNull(expiry, "expiry");
      this.expiry = expiry;
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
    public Builder underlying(ResolvedFxSingle underlying) {
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
      StringBuilder buf = new StringBuilder(192);
      buf.append("ResolvedFxVanillaOption.Builder{");
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("longShort").append('=').append(JodaBeanUtils.toString(longShort)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
