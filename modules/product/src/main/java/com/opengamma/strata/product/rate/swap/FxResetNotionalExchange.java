/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap;

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
   * The FX index used to obtain the FX reset rate.
   * <p>
   * This is the index of FX used to obtain the FX reset rate.
   * An FX index is a daily rate of exchange between two currencies.
   * Note that the order of the currencies in the index does not matter, as the
   * conversion direction is fully defined by the currency of the reference amount.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxIndex index;
  /**
   * The date of the FX reset fixing.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * Valid business days are defined by {@link FxIndex#getFixingCalendar()}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
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
      FxIndex index,
      LocalDate fixingDate) {
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    JodaBeanUtils.notNull(referenceCurrency, "referenceCurrency");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    this.paymentDate = paymentDate;
    this.referenceCurrency = referenceCurrency;
    this.notional = notional;
    this.index = index;
    this.fixingDate = fixingDate;
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
   * Gets the FX index used to obtain the FX reset rate.
   * <p>
   * This is the index of FX used to obtain the FX reset rate.
   * An FX index is a daily rate of exchange between two currencies.
   * Note that the order of the currencies in the index does not matter, as the
   * conversion direction is fully defined by the currency of the reference amount.
   * @return the value of the property, not null
   */
  public FxIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date of the FX reset fixing.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * Valid business days are defined by {@link FxIndex#getFixingCalendar()}.
   * @return the value of the property, not null
   */
  public LocalDate getFixingDate() {
    return fixingDate;
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
      return JodaBeanUtils.equal(getPaymentDate(), other.getPaymentDate()) &&
          JodaBeanUtils.equal(getReferenceCurrency(), other.getReferenceCurrency()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getFixingDate(), other.getFixingDate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixingDate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("FxResetNotionalExchange{");
    buf.append("paymentDate").append('=').append(getPaymentDate()).append(',').append(' ');
    buf.append("referenceCurrency").append('=').append(getReferenceCurrency()).append(',').append(' ');
    buf.append("notional").append('=').append(getNotional()).append(',').append(' ');
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(getFixingDate()));
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
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", FxResetNotionalExchange.class, FxIndex.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", FxResetNotionalExchange.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "paymentDate",
        "referenceCurrency",
        "notional",
        "index",
        "fixingDate");

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
        case 100346066:  // index
          return index;
        case 1255202043:  // fixingDate
          return fixingDate;
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
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code fixingDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> fixingDate() {
      return fixingDate;
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
        case 100346066:  // index
          return ((FxResetNotionalExchange) bean).getIndex();
        case 1255202043:  // fixingDate
          return ((FxResetNotionalExchange) bean).getFixingDate();
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
    private FxIndex index;
    private LocalDate fixingDate;

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
      this.index = beanToCopy.getIndex();
      this.fixingDate = beanToCopy.getFixingDate();
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
        case 100346066:  // index
          return index;
        case 1255202043:  // fixingDate
          return fixingDate;
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
        case 100346066:  // index
          this.index = (FxIndex) newValue;
          break;
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
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
          index,
          fixingDate);
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
     * Sets the FX index used to obtain the FX reset rate.
     * <p>
     * This is the index of FX used to obtain the FX reset rate.
     * An FX index is a daily rate of exchange between two currencies.
     * Note that the order of the currencies in the index does not matter, as the
     * conversion direction is fully defined by the currency of the reference amount.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(FxIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the date of the FX reset fixing.
     * <p>
     * This is an adjusted date with any business day rule applied.
     * Valid business days are defined by {@link FxIndex#getFixingCalendar()}.
     * @param fixingDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDate(LocalDate fixingDate) {
      JodaBeanUtils.notNull(fixingDate, "fixingDate");
      this.fixingDate = fixingDate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("FxResetNotionalExchange.Builder{");
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("referenceCurrency").append('=').append(JodaBeanUtils.toString(referenceCurrency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
