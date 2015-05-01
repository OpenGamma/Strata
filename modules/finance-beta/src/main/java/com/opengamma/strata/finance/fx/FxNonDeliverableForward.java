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
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndex;

/**
 * A Non-Deliverable Forward (NDF).
 * <p>
 * An NDF is a financial instrument that returns the difference between the spot rate
 * at the inception of the trade and the FX rate at maturity.
 * It is primarily used to handle FX requirements for currencies that cannot be easily traded.
 * For example, the forward may be between USD and CNY (Chinese Yuan).
 */
@BeanDefinition
public final class FxNonDeliverableForward
    implements FxNonDeliverableForwardProduct, ImmutableBean, Serializable {
  // TODO is there a need to handle settlement in a third currency?
  // requires an additional index, and allowing notional to be in one of 3 currencies
  // allow for business day adjustments to value/payment/fixing dates?

  /**
   * The settlement currency.
   * <p>
   * The settlement currency is the currency that payment will be made in.
   * It must be one of the two currencies of the forward.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency settlementCurrency;
  /**
   * The notional amount, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * <p>
   * This must be specified in one of the two currencies of the forward.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount notional;
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
   * The date that the forward settles.
   * <p>
   * On this date, the settlement amount will be exchanged.
   * This date should be a valid business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate valueDate;
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
   * The fixing date, when the FX rate is determined.
   * <p>
   * This is typically 2 business days before the value date.
   * This date should be a valid business day for the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (!index.getCurrencyPair().contains(settlementCurrency)) {
      throw new IllegalArgumentException("FxIndex and settlement currency are incompatible");
    }
    if (!index.getCurrencyPair().contains(notional.getCurrency())) {
      throw new IllegalArgumentException("FxIndex and notional are incompatible");
    }
    if (!index.getCurrencyPair().isRelated(agreedFxRate.getPair())) {
      throw new IllegalArgumentException("FxIndex and agreed FX rate are incompatible");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the non-deliverable currency.
   * <p>
   * Returns the currency that is not the settlement currency.
   * 
   * @return the currency that is not to be settled
   */
  public Currency getNonDeliverableCurrency() {
    Currency base = agreedFxRate.getPair().getBase();
    return base.equals(settlementCurrency) ? agreedFxRate.getPair().getCounter() : base;
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this FX forward into an {@code ExpandedFxNonDeliverableForward}.
   * 
   * @return the transaction
   */
  @Override
  public ExpandedFxNonDeliverableForward expand() {
    return ExpandedFxNonDeliverableForward.builder()
        .settlementCurrencyNotional(notional.convertedTo(settlementCurrency, agreedFxRate))
        .nonDeliverableCurrencyNotional(notional.convertedTo(getNonDeliverableCurrency(), agreedFxRate).negated())
        .valueDate(valueDate)
        .index(index)
        .fixingDate(fixingDate)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxNonDeliverableForward}.
   * @return the meta-bean, not null
   */
  public static FxNonDeliverableForward.Meta meta() {
    return FxNonDeliverableForward.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxNonDeliverableForward.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxNonDeliverableForward.Builder builder() {
    return new FxNonDeliverableForward.Builder();
  }

  private FxNonDeliverableForward(
      Currency settlementCurrency,
      CurrencyAmount notional,
      FxRate agreedFxRate,
      LocalDate valueDate,
      FxIndex index,
      LocalDate fixingDate) {
    JodaBeanUtils.notNull(settlementCurrency, "settlementCurrency");
    JodaBeanUtils.notNull(notional, "notional");
    JodaBeanUtils.notNull(agreedFxRate, "agreedFxRate");
    JodaBeanUtils.notNull(valueDate, "valueDate");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    this.settlementCurrency = settlementCurrency;
    this.notional = notional;
    this.agreedFxRate = agreedFxRate;
    this.valueDate = valueDate;
    this.index = index;
    this.fixingDate = fixingDate;
    validate();
  }

  @Override
  public FxNonDeliverableForward.Meta metaBean() {
    return FxNonDeliverableForward.Meta.INSTANCE;
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
   * Gets the settlement currency.
   * <p>
   * The settlement currency is the currency that payment will be made in.
   * It must be one of the two currencies of the forward.
   * @return the value of the property, not null
   */
  public Currency getSettlementCurrency() {
    return settlementCurrency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * <p>
   * This must be specified in one of the two currencies of the forward.
   * @return the value of the property, not null
   */
  public CurrencyAmount getNotional() {
    return notional;
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
   * Gets the date that the forward settles.
   * <p>
   * On this date, the settlement amount will be exchanged.
   * This date should be a valid business day.
   * @return the value of the property, not null
   */
  public LocalDate getValueDate() {
    return valueDate;
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
   * Gets the fixing date, when the FX rate is determined.
   * <p>
   * This is typically 2 business days before the value date.
   * This date should be a valid business day for the index.
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
      FxNonDeliverableForward other = (FxNonDeliverableForward) obj;
      return JodaBeanUtils.equal(getSettlementCurrency(), other.getSettlementCurrency()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getAgreedFxRate(), other.getAgreedFxRate()) &&
          JodaBeanUtils.equal(getValueDate(), other.getValueDate()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getFixingDate(), other.getFixingDate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getSettlementCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getAgreedFxRate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValueDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixingDate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("FxNonDeliverableForward{");
    buf.append("settlementCurrency").append('=').append(getSettlementCurrency()).append(',').append(' ');
    buf.append("notional").append('=').append(getNotional()).append(',').append(' ');
    buf.append("agreedFxRate").append('=').append(getAgreedFxRate()).append(',').append(' ');
    buf.append("valueDate").append('=').append(getValueDate()).append(',').append(' ');
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(getFixingDate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxNonDeliverableForward}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code settlementCurrency} property.
     */
    private final MetaProperty<Currency> settlementCurrency = DirectMetaProperty.ofImmutable(
        this, "settlementCurrency", FxNonDeliverableForward.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<CurrencyAmount> notional = DirectMetaProperty.ofImmutable(
        this, "notional", FxNonDeliverableForward.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code agreedFxRate} property.
     */
    private final MetaProperty<FxRate> agreedFxRate = DirectMetaProperty.ofImmutable(
        this, "agreedFxRate", FxNonDeliverableForward.class, FxRate.class);
    /**
     * The meta-property for the {@code valueDate} property.
     */
    private final MetaProperty<LocalDate> valueDate = DirectMetaProperty.ofImmutable(
        this, "valueDate", FxNonDeliverableForward.class, LocalDate.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", FxNonDeliverableForward.class, FxIndex.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", FxNonDeliverableForward.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "settlementCurrency",
        "notional",
        "agreedFxRate",
        "valueDate",
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
        case -1024875430:  // settlementCurrency
          return settlementCurrency;
        case 1585636160:  // notional
          return notional;
        case 1040357930:  // agreedFxRate
          return agreedFxRate;
        case -766192449:  // valueDate
          return valueDate;
        case 100346066:  // index
          return index;
        case 1255202043:  // fixingDate
          return fixingDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxNonDeliverableForward.Builder builder() {
      return new FxNonDeliverableForward.Builder();
    }

    @Override
    public Class<? extends FxNonDeliverableForward> beanType() {
      return FxNonDeliverableForward.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code settlementCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> settlementCurrency() {
      return settlementCurrency;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code agreedFxRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxRate> agreedFxRate() {
      return agreedFxRate;
    }

    /**
     * The meta-property for the {@code valueDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valueDate() {
      return valueDate;
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
        case -1024875430:  // settlementCurrency
          return ((FxNonDeliverableForward) bean).getSettlementCurrency();
        case 1585636160:  // notional
          return ((FxNonDeliverableForward) bean).getNotional();
        case 1040357930:  // agreedFxRate
          return ((FxNonDeliverableForward) bean).getAgreedFxRate();
        case -766192449:  // valueDate
          return ((FxNonDeliverableForward) bean).getValueDate();
        case 100346066:  // index
          return ((FxNonDeliverableForward) bean).getIndex();
        case 1255202043:  // fixingDate
          return ((FxNonDeliverableForward) bean).getFixingDate();
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
   * The bean-builder for {@code FxNonDeliverableForward}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxNonDeliverableForward> {

    private Currency settlementCurrency;
    private CurrencyAmount notional;
    private FxRate agreedFxRate;
    private LocalDate valueDate;
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
    private Builder(FxNonDeliverableForward beanToCopy) {
      this.settlementCurrency = beanToCopy.getSettlementCurrency();
      this.notional = beanToCopy.getNotional();
      this.agreedFxRate = beanToCopy.getAgreedFxRate();
      this.valueDate = beanToCopy.getValueDate();
      this.index = beanToCopy.getIndex();
      this.fixingDate = beanToCopy.getFixingDate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1024875430:  // settlementCurrency
          return settlementCurrency;
        case 1585636160:  // notional
          return notional;
        case 1040357930:  // agreedFxRate
          return agreedFxRate;
        case -766192449:  // valueDate
          return valueDate;
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
        case -1024875430:  // settlementCurrency
          this.settlementCurrency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (CurrencyAmount) newValue;
          break;
        case 1040357930:  // agreedFxRate
          this.agreedFxRate = (FxRate) newValue;
          break;
        case -766192449:  // valueDate
          this.valueDate = (LocalDate) newValue;
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
    public FxNonDeliverableForward build() {
      return new FxNonDeliverableForward(
          settlementCurrency,
          notional,
          agreedFxRate,
          valueDate,
          index,
          fixingDate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code settlementCurrency} property in the builder.
     * @param settlementCurrency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder settlementCurrency(Currency settlementCurrency) {
      JodaBeanUtils.notNull(settlementCurrency, "settlementCurrency");
      this.settlementCurrency = settlementCurrency;
      return this;
    }

    /**
     * Sets the {@code notional} property in the builder.
     * @param notional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notional(CurrencyAmount notional) {
      JodaBeanUtils.notNull(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the {@code agreedFxRate} property in the builder.
     * @param agreedFxRate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder agreedFxRate(FxRate agreedFxRate) {
      JodaBeanUtils.notNull(agreedFxRate, "agreedFxRate");
      this.agreedFxRate = agreedFxRate;
      return this;
    }

    /**
     * Sets the {@code valueDate} property in the builder.
     * @param valueDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valueDate(LocalDate valueDate) {
      JodaBeanUtils.notNull(valueDate, "valueDate");
      this.valueDate = valueDate;
      return this;
    }

    /**
     * Sets the {@code index} property in the builder.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(FxIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the {@code fixingDate} property in the builder.
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
      StringBuilder buf = new StringBuilder(224);
      buf.append("FxNonDeliverableForward.Builder{");
      buf.append("settlementCurrency").append('=').append(JodaBeanUtils.toString(settlementCurrency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("agreedFxRate").append('=').append(JodaBeanUtils.toString(agreedFxRate)).append(',').append(' ');
      buf.append("valueDate").append('=').append(JodaBeanUtils.toString(valueDate)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
