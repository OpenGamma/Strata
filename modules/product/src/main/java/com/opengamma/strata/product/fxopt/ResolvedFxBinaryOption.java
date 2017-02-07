/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.collect.ArgChecker.inOrderOrEqual;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.fx.FxSingle;
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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.fx.ResolvedFxSingle;

/**
 * A vanilla FX option, resolved for pricing.
 * <p>
 * This is the resolved form of {@link FxVanillaOption} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedFxVanillaOption} from a {@code FxVanillaOption}
 * using {@link FxVanillaOption#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedFxVanillaOption} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedFxBinaryOption
        implements ResolvedProduct, ImmutableBean, Serializable {

    /**
     * Whether the option is long or short.
     * <p>
     * At expiry, the long party will have the option to enter in this transaction;
     * the short party will, at the option of the long party, potentially enter into the inverse transaction.
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

    @PropertyDefinition(validate = "notNull")
    private final double strikePrice;

    @PropertyDefinition(validate = "notNull")
    private final Payment payoff;

    //-------------------------------------------------------------------------
    @ImmutableValidator
    private void validate() {
        inOrderOrEqual(expiry.toLocalDate(), underlying.getPaymentDate(), "expiry.date", "underlying.paymentDate");
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
     * Gets the expiry date of the option.
     *
     * @return the expiry date
     */
    public LocalDate getExpiryDate() {
        return expiry.toLocalDate();
    }

    /**
     * Gets the strike rate.
     *
     * @return the strike
     */
    public double getStrike() {
        return Math.abs(underlying.getCounterCurrencyPayment().getAmount() /
                underlying.getBaseCurrencyPayment().getAmount());
    }

    /**
     * Returns the put/call flag.
     * <p>
     * This is the put/call for the base currency.
     * If the amount for the base currency is positive, the option is a call on the base currency (put on counter currency).
     * If the amount for the base currency is negative, the option is a put on the base currency (call on counter currency).
     *
     * @return the put or call
     */
    public PutCall getPutCall() {
        return underlying.getCounterCurrencyPayment().getAmount() > 0d ? PutCall.PUT : PutCall.CALL;
    }

    /**
     * Get the counter currency of the underlying FX transaction.
     *
     * @return the counter currency
     */
    public Currency getCounterCurrency() {
        return underlying.getCounterCurrencyPayment().getCurrency();
    }
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedFxBinaryOption}.
   * @return the meta-bean, not null
   */
  public static ResolvedFxBinaryOption.Meta meta() {
    return ResolvedFxBinaryOption.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedFxBinaryOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedFxBinaryOption.Builder builder() {
    return new ResolvedFxBinaryOption.Builder();
  }

  private ResolvedFxBinaryOption(
      LongShort longShort,
      ZonedDateTime expiry,
      ResolvedFxSingle underlying,
      double strikePrice,
      Payment payoff) {
    JodaBeanUtils.notNull(longShort, "longShort");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(underlying, "underlying");
    JodaBeanUtils.notNull(strikePrice, "strikePrice");
    JodaBeanUtils.notNull(payoff, "payoff");
    this.longShort = longShort;
    this.expiry = expiry;
    this.underlying = underlying;
    this.strikePrice = strikePrice;
    this.payoff = payoff;
    validate();
  }

  @Override
  public ResolvedFxBinaryOption.Meta metaBean() {
    return ResolvedFxBinaryOption.Meta.INSTANCE;
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
   * At expiry, the long party will have the option to enter in this transaction;
   * the short party will, at the option of the long party, potentially enter into the inverse transaction.
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
   * Gets the strikePrice.
   * @return the value of the property, not null
   */
  public double getStrikePrice() {
    return strikePrice;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payoff.
   * @return the value of the property, not null
   */
  public Payment getPayoff() {
    return payoff;
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
      ResolvedFxBinaryOption other = (ResolvedFxBinaryOption) obj;
      return JodaBeanUtils.equal(longShort, other.longShort) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
          JodaBeanUtils.equal(underlying, other.underlying) &&
          JodaBeanUtils.equal(strikePrice, other.strikePrice) &&
          JodaBeanUtils.equal(payoff, other.payoff);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(longShort);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikePrice);
    hash = hash * 31 + JodaBeanUtils.hashCode(payoff);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("ResolvedFxBinaryOption{");
    buf.append("longShort").append('=').append(longShort).append(',').append(' ');
    buf.append("expiry").append('=').append(expiry).append(',').append(' ');
    buf.append("underlying").append('=').append(underlying).append(',').append(' ');
    buf.append("strikePrice").append('=').append(strikePrice).append(',').append(' ');
    buf.append("payoff").append('=').append(JodaBeanUtils.toString(payoff));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedFxBinaryOption}.
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
        this, "longShort", ResolvedFxBinaryOption.class, LongShort.class);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<ZonedDateTime> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", ResolvedFxBinaryOption.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<ResolvedFxSingle> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", ResolvedFxBinaryOption.class, ResolvedFxSingle.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", ResolvedFxBinaryOption.class, Double.TYPE);
    /**
     * The meta-property for the {@code payoff} property.
     */
    private final MetaProperty<Payment> payoff = DirectMetaProperty.ofImmutable(
        this, "payoff", ResolvedFxBinaryOption.class, Payment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "longShort",
        "expiry",
        "underlying",
        "strikePrice",
        "payoff");

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
        case -1289159373:  // expiry
          return expiry;
        case -1770633379:  // underlying
          return underlying;
        case 50946231:  // strikePrice
          return strikePrice;
        case -995206201:  // payoff
          return payoff;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedFxBinaryOption.Builder builder() {
      return new ResolvedFxBinaryOption.Builder();
    }

    @Override
    public Class<? extends ResolvedFxBinaryOption> beanType() {
      return ResolvedFxBinaryOption.class;
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
     * The meta-property for the {@code strikePrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strikePrice() {
      return strikePrice;
    }

    /**
     * The meta-property for the {@code payoff} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Payment> payoff() {
      return payoff;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return ((ResolvedFxBinaryOption) bean).getLongShort();
        case -1289159373:  // expiry
          return ((ResolvedFxBinaryOption) bean).getExpiry();
        case -1770633379:  // underlying
          return ((ResolvedFxBinaryOption) bean).getUnderlying();
        case 50946231:  // strikePrice
          return ((ResolvedFxBinaryOption) bean).getStrikePrice();
        case -995206201:  // payoff
          return ((ResolvedFxBinaryOption) bean).getPayoff();
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
   * The bean-builder for {@code ResolvedFxBinaryOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedFxBinaryOption> {

    private LongShort longShort;
    private ZonedDateTime expiry;
    private ResolvedFxSingle underlying;
    private double strikePrice;
    private Payment payoff;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedFxBinaryOption beanToCopy) {
      this.longShort = beanToCopy.getLongShort();
      this.expiry = beanToCopy.getExpiry();
      this.underlying = beanToCopy.getUnderlying();
      this.strikePrice = beanToCopy.getStrikePrice();
      this.payoff = beanToCopy.getPayoff();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return longShort;
        case -1289159373:  // expiry
          return expiry;
        case -1770633379:  // underlying
          return underlying;
        case 50946231:  // strikePrice
          return strikePrice;
        case -995206201:  // payoff
          return payoff;
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
        case -1289159373:  // expiry
          this.expiry = (ZonedDateTime) newValue;
          break;
        case -1770633379:  // underlying
          this.underlying = (ResolvedFxSingle) newValue;
          break;
        case 50946231:  // strikePrice
          this.strikePrice = (Double) newValue;
          break;
        case -995206201:  // payoff
          this.payoff = (Payment) newValue;
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
    public ResolvedFxBinaryOption build() {
      return new ResolvedFxBinaryOption(
          longShort,
          expiry,
          underlying,
          strikePrice,
          payoff);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the option is long or short.
     * <p>
     * At expiry, the long party will have the option to enter in this transaction;
     * the short party will, at the option of the long party, potentially enter into the inverse transaction.
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
     * Sets the strikePrice.
     * @param strikePrice  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder strikePrice(double strikePrice) {
      JodaBeanUtils.notNull(strikePrice, "strikePrice");
      this.strikePrice = strikePrice;
      return this;
    }

    /**
     * Sets the payoff.
     * @param payoff  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payoff(Payment payoff) {
      JodaBeanUtils.notNull(payoff, "payoff");
      this.payoff = payoff;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ResolvedFxBinaryOption.Builder{");
      buf.append("longShort").append('=').append(JodaBeanUtils.toString(longShort)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
      buf.append("payoff").append('=').append(JodaBeanUtils.toString(payoff));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
