/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import java.io.Serializable;
import java.time.LocalDate;
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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndex;

/**
 * An expanded Non-Deliverable Forward (NDF), the low level representation of an NDF.
 * <p>
 * An NDF is a financial instrument that returns the difference between a fixed FX rate 
 * agreed at the inception of the trade and the FX rate at maturity. 
 * It is primarily used to handle FX requirements for currencies that have settlement restrictions.
 * For example, the forward may be between USD and CNY (Chinese Yuan).
 * <p>
 * An {@code ExpandedFxNdf} may contain information based on holiday calendars.
 * If a holiday calendar changes, the adjusted dates may no longer be correct.
 * Care must be taken when placing the expanded form in a cache or persistence layer.
 * Application code should use {@link FxNdf}, not this class.
 */
@BeanDefinition
public final class ExpandedFxNdf
    implements FxNdfProduct, ImmutableBean, Serializable {

  /**
   * The notional amount in the settlement currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * <p>
   * This must be specified in one of the two currencies of the forward.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount settlementCurrencyNotional;
  /**
   * The FX rate agreed for the value date at the inception of the trade.
   * <p>
   * The settlement amount is based on the difference between this rate and the
   * rate observed on the fixing date using the {@code index}.
   * <p>
   * The forward is between the two currencies defined by the rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxRate agreedFxRate;
  /**
   * The index defining the FX rate to observe on the fixing date.
   * <p>
   * The index is used to settle the trade by providing the actual FX rate on the fixing date.
   * The value of the trade is based on the difference between the actual rate and the agreed rate.
   * <p>
   * The forward is between the two currencies defined by the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxIndex index;
  /**
   * The date that the forward settles.
   * <p>
   * On this date, the settlement amount will be exchanged.
   * This date should be a valid business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    CurrencyPair pair = index.getCurrencyPair();
    if (!pair.contains(settlementCurrencyNotional.getCurrency())) {
      throw new IllegalArgumentException("FxIndex and settlement notional currency are incompatible");
    }
    if (!(pair.equals(agreedFxRate.getPair()) || pair.isInverse(agreedFxRate.getPair()))) {
      throw new IllegalArgumentException("FxIndex and agreed FX rate are incompatible");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the settlement currency.
   * 
   * @return the currency that is to be settled
   */
  public Currency getSettlementCurrency() {
    return settlementCurrencyNotional.getCurrency();
  }

  /**
   * Gets the settlement notional.
   * <p>
   * Returns the signed notional amount that is to be settled in the settlement currency. 
   * 
   * @return the notional
   */
  public double getSettlementNotional() {
    return settlementCurrencyNotional.getAmount();
  }

  /**
   * Gets the non-deliverable currency.
   * <p>
   * Returns the currency that is not the settlement currency.
   * 
   * @return the currency that is not to be settled
   */
  public Currency getNonDeliverableCurrency() {
    return index.getCurrencyPair().getBase().equals(getSettlementCurrency()) ?
        index.getCurrencyPair().getCounter() : index.getCurrencyPair().getBase();
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this forward, trivially returning {@code this}.
   * 
   * @return this NDF
   */
  @Override
  public ExpandedFxNdf expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExpandedFxNdf}.
   * @return the meta-bean, not null
   */
  public static ExpandedFxNdf.Meta meta() {
    return ExpandedFxNdf.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExpandedFxNdf.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ExpandedFxNdf.Builder builder() {
    return new ExpandedFxNdf.Builder();
  }

  private ExpandedFxNdf(
      CurrencyAmount settlementCurrencyNotional,
      FxRate agreedFxRate,
      FxIndex index,
      LocalDate paymentDate) {
    JodaBeanUtils.notNull(settlementCurrencyNotional, "settlementCurrencyNotional");
    JodaBeanUtils.notNull(agreedFxRate, "agreedFxRate");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    this.settlementCurrencyNotional = settlementCurrencyNotional;
    this.agreedFxRate = agreedFxRate;
    this.index = index;
    this.paymentDate = paymentDate;
    validate();
  }

  @Override
  public ExpandedFxNdf.Meta metaBean() {
    return ExpandedFxNdf.Meta.INSTANCE;
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
   * Gets the notional amount in the settlement currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * <p>
   * This must be specified in one of the two currencies of the forward.
   * @return the value of the property, not null
   */
  public CurrencyAmount getSettlementCurrencyNotional() {
    return settlementCurrencyNotional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the FX rate agreed for the value date at the inception of the trade.
   * <p>
   * The settlement amount is based on the difference between this rate and the
   * rate observed on the fixing date using the {@code index}.
   * <p>
   * The forward is between the two currencies defined by the rate.
   * @return the value of the property, not null
   */
  public FxRate getAgreedFxRate() {
    return agreedFxRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index defining the FX rate to observe on the fixing date.
   * <p>
   * The index is used to settle the trade by providing the actual FX rate on the fixing date.
   * The value of the trade is based on the difference between the actual rate and the agreed rate.
   * <p>
   * The forward is between the two currencies defined by the index.
   * @return the value of the property, not null
   */
  public FxIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that the forward settles.
   * <p>
   * On this date, the settlement amount will be exchanged.
   * This date should be a valid business day.
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
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
      ExpandedFxNdf other = (ExpandedFxNdf) obj;
      return JodaBeanUtils.equal(getSettlementCurrencyNotional(), other.getSettlementCurrencyNotional()) &&
          JodaBeanUtils.equal(getAgreedFxRate(), other.getAgreedFxRate()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getPaymentDate(), other.getPaymentDate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getSettlementCurrencyNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getAgreedFxRate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentDate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("ExpandedFxNdf{");
    buf.append("settlementCurrencyNotional").append('=').append(getSettlementCurrencyNotional()).append(',').append(' ');
    buf.append("agreedFxRate").append('=').append(getAgreedFxRate()).append(',').append(' ');
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(getPaymentDate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExpandedFxNdf}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code settlementCurrencyNotional} property.
     */
    private final MetaProperty<CurrencyAmount> settlementCurrencyNotional = DirectMetaProperty.ofImmutable(
        this, "settlementCurrencyNotional", ExpandedFxNdf.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code agreedFxRate} property.
     */
    private final MetaProperty<FxRate> agreedFxRate = DirectMetaProperty.ofImmutable(
        this, "agreedFxRate", ExpandedFxNdf.class, FxRate.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", ExpandedFxNdf.class, FxIndex.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", ExpandedFxNdf.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "settlementCurrencyNotional",
        "agreedFxRate",
        "index",
        "paymentDate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 594670010:  // settlementCurrencyNotional
          return settlementCurrencyNotional;
        case 1040357930:  // agreedFxRate
          return agreedFxRate;
        case 100346066:  // index
          return index;
        case -1540873516:  // paymentDate
          return paymentDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ExpandedFxNdf.Builder builder() {
      return new ExpandedFxNdf.Builder();
    }

    @Override
    public Class<? extends ExpandedFxNdf> beanType() {
      return ExpandedFxNdf.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code settlementCurrencyNotional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> settlementCurrencyNotional() {
      return settlementCurrencyNotional;
    }

    /**
     * The meta-property for the {@code agreedFxRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxRate> agreedFxRate() {
      return agreedFxRate;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 594670010:  // settlementCurrencyNotional
          return ((ExpandedFxNdf) bean).getSettlementCurrencyNotional();
        case 1040357930:  // agreedFxRate
          return ((ExpandedFxNdf) bean).getAgreedFxRate();
        case 100346066:  // index
          return ((ExpandedFxNdf) bean).getIndex();
        case -1540873516:  // paymentDate
          return ((ExpandedFxNdf) bean).getPaymentDate();
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
   * The bean-builder for {@code ExpandedFxNdf}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ExpandedFxNdf> {

    private CurrencyAmount settlementCurrencyNotional;
    private FxRate agreedFxRate;
    private FxIndex index;
    private LocalDate paymentDate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ExpandedFxNdf beanToCopy) {
      this.settlementCurrencyNotional = beanToCopy.getSettlementCurrencyNotional();
      this.agreedFxRate = beanToCopy.getAgreedFxRate();
      this.index = beanToCopy.getIndex();
      this.paymentDate = beanToCopy.getPaymentDate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 594670010:  // settlementCurrencyNotional
          return settlementCurrencyNotional;
        case 1040357930:  // agreedFxRate
          return agreedFxRate;
        case 100346066:  // index
          return index;
        case -1540873516:  // paymentDate
          return paymentDate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 594670010:  // settlementCurrencyNotional
          this.settlementCurrencyNotional = (CurrencyAmount) newValue;
          break;
        case 1040357930:  // agreedFxRate
          this.agreedFxRate = (FxRate) newValue;
          break;
        case 100346066:  // index
          this.index = (FxIndex) newValue;
          break;
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
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
    public ExpandedFxNdf build() {
      return new ExpandedFxNdf(
          settlementCurrencyNotional,
          agreedFxRate,
          index,
          paymentDate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the notional amount in the settlement currency, positive if receiving, negative if paying.
     * <p>
     * The amount is signed.
     * A positive amount indicates the payment is to be received.
     * A negative amount indicates the payment is to be paid.
     * <p>
     * This must be specified in one of the two currencies of the forward.
     * @param settlementCurrencyNotional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder settlementCurrencyNotional(CurrencyAmount settlementCurrencyNotional) {
      JodaBeanUtils.notNull(settlementCurrencyNotional, "settlementCurrencyNotional");
      this.settlementCurrencyNotional = settlementCurrencyNotional;
      return this;
    }

    /**
     * Sets the FX rate agreed for the value date at the inception of the trade.
     * <p>
     * The settlement amount is based on the difference between this rate and the
     * rate observed on the fixing date using the {@code index}.
     * <p>
     * The forward is between the two currencies defined by the rate.
     * @param agreedFxRate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder agreedFxRate(FxRate agreedFxRate) {
      JodaBeanUtils.notNull(agreedFxRate, "agreedFxRate");
      this.agreedFxRate = agreedFxRate;
      return this;
    }

    /**
     * Sets the index defining the FX rate to observe on the fixing date.
     * <p>
     * The index is used to settle the trade by providing the actual FX rate on the fixing date.
     * The value of the trade is based on the difference between the actual rate and the agreed rate.
     * <p>
     * The forward is between the two currencies defined by the index.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(FxIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the date that the forward settles.
     * <p>
     * On this date, the settlement amount will be exchanged.
     * This date should be a valid business day.
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ExpandedFxNdf.Builder{");
      buf.append("settlementCurrencyNotional").append('=').append(JodaBeanUtils.toString(settlementCurrencyNotional)).append(',').append(' ');
      buf.append("agreedFxRate").append('=').append(JodaBeanUtils.toString(agreedFxRate)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
