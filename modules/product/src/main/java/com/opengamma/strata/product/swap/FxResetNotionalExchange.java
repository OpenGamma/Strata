/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.collect.Messages;

/**
 * An exchange of notionals between two counterparties where FX reset applies.
 * <p>
 * In most swaps, the notional amount is not exchanged, with only the interest being exchanged.
 * However, in the case of an FX reset swap, the notional is exchanged.
 * The swap contract will define a notional, which may vary over time, in one currency
 * however payments are defined to occur in a different currency.
 * An FX conversion is used to convert the amount.
 * <p>
 * For example, a swap may have a notional of GBP 1,000,000 but be paid in USD.
 * At the start of the first swap period, there is a notional exchange at the prevailing
 * FX rate, say of USD 1,520,000. At the end of the first swap period, that amount is repaid
 * and the new FX rate is used to determine the exchange for the second period, say of USD 1,610,000.
 * In general, only the net difference due to FX will be exchanged at intermediate swap period boundaries.
 * <p>
 * The reference currency is the currency in which the notional is actually defined.
 * ISDA refers to the payment currency as the <i>variable currency</i> and the reference
 * currency as the <i>constant currency</i>.
 * An FX reset swap is also known as a <i>Mark-to-market currency swap</i>.
 * <p>
 * Defined by the 2006 ISDA definitions article 10.
 */
@BeanDefinition
public final class FxResetNotionalExchange
    implements PaymentEvent, ImmutableBean, Serializable {

  /**
   * The date that the payment is made.
   * <p>
   * Each payment event has a single payment date.
   * This date has been adjusted to be a valid business day.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate paymentDate;
  /**
   * The currency of the notional amount defined in the contract.
   * <p>
   * This is the currency of notional amount as defined in the contract.
   * The notional will be converted from this currency to the payment currency using the specified index.
   * ISDA refers to this as the <i>constant currency</i>.
   * <p>
   * The reference currency must be one of the two currencies of the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency referenceCurrency;
  /**
   * The notional amount, positive if receiving, negative if paying.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code referenceCurrency} but will
   * be paid after FX conversion using the index.
   */
  @PropertyDefinition
  private final double notional;
  /**
   * The FX index observation.
   * <p>
   * This defines the observation of the index used to obtain the FX reset rate.
   * <p>
   * An FX index is a daily rate of exchange between two currencies.
   * Note that the order of the currencies in the index does not matter, as the
   * conversion direction is fully defined by the currency of the reference amount.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxIndexObservation observation;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    FxIndex index = observation.getIndex();
    if (!index.getCurrencyPair().contains(referenceCurrency)) {
      throw new IllegalArgumentException(
          Messages.format(
              "Reference currency {} must be one of those in the FxIndex {}", referenceCurrency, index));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the payment currency.
   * <p>
   * This returns the currency that the payment is made in.
   * ISDA refers to this as the <i>variable currency</i>.
   * 
   * @return the payment currency
   */
  @Override
  public Currency getCurrency() {
    FxIndex index = observation.getIndex();
    Currency indexBase = index.getCurrencyPair().getBase();
    Currency indexCounter = index.getCurrencyPair().getCounter();
    return (referenceCurrency.equals(indexBase) ? indexCounter : indexBase);
  }

  /**
   * Gets the notional as a {@code CurrencyAmount}.
   * <p>
   * The notional is expressed in the reference currency, prior to FX conversion.
   * 
   * @return the notional as a  {@code CurrencyAmount}
   */
  public CurrencyAmount getNotionalAmount() {
    return CurrencyAmount.of(referenceCurrency, notional);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxResetNotionalExchange adjustPaymentDate(TemporalAdjuster adjuster) {
    LocalDate adjusted = paymentDate.with(adjuster);
    return adjusted.equals(paymentDate) ? this : toBuilder().paymentDate(adjusted).build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxResetNotionalExchange}.
   * @return the meta-bean, not null
   */
  public static FxResetNotionalExchange.Meta meta() {
    return FxResetNotionalExchange.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxResetNotionalExchange.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxResetNotionalExchange.Builder builder() {
    return new FxResetNotionalExchange.Builder();
  }

  private FxResetNotionalExchange(
      LocalDate paymentDate,
      Currency referenceCurrency,
      double notional,
      FxIndexObservation observation) {
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    JodaBeanUtils.notNull(referenceCurrency, "referenceCurrency");
    JodaBeanUtils.notNull(observation, "observation");
    this.paymentDate = paymentDate;
    this.referenceCurrency = referenceCurrency;
    this.notional = notional;
    this.observation = observation;
    validate();
  }

  @Override
  public FxResetNotionalExchange.Meta metaBean() {
    return FxResetNotionalExchange.Meta.INSTANCE;
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
   * Gets the date that the payment is made.
   * <p>
   * Each payment event has a single payment date.
   * This date has been adjusted to be a valid business day.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the notional amount defined in the contract.
   * <p>
   * This is the currency of notional amount as defined in the contract.
   * The notional will be converted from this currency to the payment currency using the specified index.
   * ISDA refers to this as the <i>constant currency</i>.
   * <p>
   * The reference currency must be one of the two currencies of the index.
   * @return the value of the property, not null
   */
  public Currency getReferenceCurrency() {
    return referenceCurrency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount, positive if receiving, negative if paying.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code referenceCurrency} but will
   * be paid after FX conversion using the index.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the FX index observation.
   * <p>
   * This defines the observation of the index used to obtain the FX reset rate.
   * <p>
   * An FX index is a daily rate of exchange between two currencies.
   * Note that the order of the currencies in the index does not matter, as the
   * conversion direction is fully defined by the currency of the reference amount.
   * @return the value of the property, not null
   */
  public FxIndexObservation getObservation() {
    return observation;
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
      FxResetNotionalExchange other = (FxResetNotionalExchange) obj;
      return JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(referenceCurrency, other.referenceCurrency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(observation, other.observation);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceCurrency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(observation);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("FxResetNotionalExchange{");
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("referenceCurrency").append('=').append(referenceCurrency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("observation").append('=').append(JodaBeanUtils.toString(observation));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxResetNotionalExchange}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", FxResetNotionalExchange.class, LocalDate.class);
    /**
     * The meta-property for the {@code referenceCurrency} property.
     */
    private final MetaProperty<Currency> referenceCurrency = DirectMetaProperty.ofImmutable(
        this, "referenceCurrency", FxResetNotionalExchange.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", FxResetNotionalExchange.class, Double.TYPE);
    /**
     * The meta-property for the {@code observation} property.
     */
    private final MetaProperty<FxIndexObservation> observation = DirectMetaProperty.ofImmutable(
        this, "observation", FxResetNotionalExchange.class, FxIndexObservation.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "paymentDate",
        "referenceCurrency",
        "notional",
        "observation");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return paymentDate;
        case 727652476:  // referenceCurrency
          return referenceCurrency;
        case 1585636160:  // notional
          return notional;
        case 122345516:  // observation
          return observation;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxResetNotionalExchange.Builder builder() {
      return new FxResetNotionalExchange.Builder();
    }

    @Override
    public Class<? extends FxResetNotionalExchange> beanType() {
      return FxResetNotionalExchange.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    /**
     * The meta-property for the {@code referenceCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> referenceCurrency() {
      return referenceCurrency;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code observation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndexObservation> observation() {
      return observation;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return ((FxResetNotionalExchange) bean).getPaymentDate();
        case 727652476:  // referenceCurrency
          return ((FxResetNotionalExchange) bean).getReferenceCurrency();
        case 1585636160:  // notional
          return ((FxResetNotionalExchange) bean).getNotional();
        case 122345516:  // observation
          return ((FxResetNotionalExchange) bean).getObservation();
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
   * The bean-builder for {@code FxResetNotionalExchange}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxResetNotionalExchange> {

    private LocalDate paymentDate;
    private Currency referenceCurrency;
    private double notional;
    private FxIndexObservation observation;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FxResetNotionalExchange beanToCopy) {
      this.paymentDate = beanToCopy.getPaymentDate();
      this.referenceCurrency = beanToCopy.getReferenceCurrency();
      this.notional = beanToCopy.getNotional();
      this.observation = beanToCopy.getObservation();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return paymentDate;
        case 727652476:  // referenceCurrency
          return referenceCurrency;
        case 1585636160:  // notional
          return notional;
        case 122345516:  // observation
          return observation;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case 727652476:  // referenceCurrency
          this.referenceCurrency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case 122345516:  // observation
          this.observation = (FxIndexObservation) newValue;
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
    public FxResetNotionalExchange build() {
      return new FxResetNotionalExchange(
          paymentDate,
          referenceCurrency,
          notional,
          observation);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the date that the payment is made.
     * <p>
     * Each payment event has a single payment date.
     * This date has been adjusted to be a valid business day.
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

    /**
     * Sets the currency of the notional amount defined in the contract.
     * <p>
     * This is the currency of notional amount as defined in the contract.
     * The notional will be converted from this currency to the payment currency using the specified index.
     * ISDA refers to this as the <i>constant currency</i>.
     * <p>
     * The reference currency must be one of the two currencies of the index.
     * @param referenceCurrency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceCurrency(Currency referenceCurrency) {
      JodaBeanUtils.notNull(referenceCurrency, "referenceCurrency");
      this.referenceCurrency = referenceCurrency;
      return this;
    }

    /**
     * Sets the notional amount, positive if receiving, negative if paying.
     * <p>
     * The notional amount applicable during the period.
     * The currency of the notional is specified by {@code referenceCurrency} but will
     * be paid after FX conversion using the index.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      this.notional = notional;
      return this;
    }

    /**
     * Sets the FX index observation.
     * <p>
     * This defines the observation of the index used to obtain the FX reset rate.
     * <p>
     * An FX index is a daily rate of exchange between two currencies.
     * Note that the order of the currencies in the index does not matter, as the
     * conversion direction is fully defined by the currency of the reference amount.
     * @param observation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder observation(FxIndexObservation observation) {
      JodaBeanUtils.notNull(observation, "observation");
      this.observation = observation;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("FxResetNotionalExchange.Builder{");
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("referenceCurrency").append('=').append(JodaBeanUtils.toString(referenceCurrency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("observation").append('=').append(JodaBeanUtils.toString(observation));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
