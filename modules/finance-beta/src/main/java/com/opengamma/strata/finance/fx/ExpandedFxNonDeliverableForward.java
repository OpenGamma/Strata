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
import com.opengamma.strata.basics.index.FxIndex;

/**
 * An expanded Non-Deliverable Forward (NDF), the low level representation of an NDF.
 * <p>
 * An NDF is a financial instrument that returns the difference between the spot rate
 * at the inception of the trade and the FX rate at maturity.
 * It is primarily used to handle FX requirements for currencies that cannot be easily traded.
 * For example, the forward may be between USD and CNY (Chinese Yuan).
 * <p>
 * An {@code ExpandedFxNonDeliverableForward} may contain information based on holiday calendars.
 * If a holiday calendar changes, the adjusted dates may no longer be correct.
 * Care must be taken when placing the expanded form in a cache or persistence layer.
 * Application code should use {@link FxNonDeliverableForward}, not this class.
 */
@BeanDefinition
public final class ExpandedFxNonDeliverableForward
    implements FxNonDeliverableForwardProduct, ImmutableBean, Serializable {

  /**
   * The notional amount in the settlement currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount settlementCurrencyNotional;
  /**
   * The notional amount in the non-deliverable currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount nonDeliverableCurrencyNotional;
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
  /**
   * The date that the forward settles.
   * <p>
   * On this date, the settlement amount will be exchanged.
   * This date should be a valid business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate valueDate;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (settlementCurrencyNotional.getCurrency().equals(nonDeliverableCurrencyNotional.getCurrency())) {
      throw new IllegalArgumentException("Notionals must have different currencies");
    }
    if (!index.getCurrencyPair().contains(settlementCurrencyNotional.getCurrency())) {
      throw new IllegalArgumentException("FxIndex and settlement notional currency are incompatible");
    }
    if (!index.getCurrencyPair().contains(nonDeliverableCurrencyNotional.getCurrency())) {
      throw new IllegalArgumentException("FxIndex and non-deliverable notional currency are incompatible");
    }
    if ((settlementCurrencyNotional.getAmount() != 0d || nonDeliverableCurrencyNotional.getAmount() != 0d) &&
        Math.signum(settlementCurrencyNotional.getAmount()) != -Math.signum(nonDeliverableCurrencyNotional.getAmount())) {
      throw new IllegalArgumentException("Notionals must have different signs");
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
   * Gets the non-deliverable currency.
   * <p>
   * Returns the currency that is not the settlement currency.
   * 
   * @return the currency that is not to be settled
   */
  public Currency getNonDeliverableCurrency() {
    return nonDeliverableCurrencyNotional.getCurrency();
  }

  /**
   * Gets the agreed FX rate, from the settlement currency to the non-deliverable currency.
   * 
   * @return the rate from the settlement currency to the non-deliverable currency
   */
  public double getAgreedFxRate() {
    return -nonDeliverableCurrencyNotional.getAmount() / settlementCurrencyNotional.getAmount();
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this forward, trivially returning {@code this}.
   * 
   * @return this transaction
   */
  @Override
  public ExpandedFxNonDeliverableForward expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExpandedFxNonDeliverableForward}.
   * @return the meta-bean, not null
   */
  public static ExpandedFxNonDeliverableForward.Meta meta() {
    return ExpandedFxNonDeliverableForward.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExpandedFxNonDeliverableForward.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ExpandedFxNonDeliverableForward.Builder builder() {
    return new ExpandedFxNonDeliverableForward.Builder();
  }

  private ExpandedFxNonDeliverableForward(
      CurrencyAmount settlementCurrencyNotional,
      CurrencyAmount nonDeliverableCurrencyNotional,
      FxIndex index,
      LocalDate fixingDate,
      LocalDate valueDate) {
    JodaBeanUtils.notNull(settlementCurrencyNotional, "settlementCurrencyNotional");
    JodaBeanUtils.notNull(nonDeliverableCurrencyNotional, "nonDeliverableCurrencyNotional");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    JodaBeanUtils.notNull(valueDate, "valueDate");
    this.settlementCurrencyNotional = settlementCurrencyNotional;
    this.nonDeliverableCurrencyNotional = nonDeliverableCurrencyNotional;
    this.index = index;
    this.fixingDate = fixingDate;
    this.valueDate = valueDate;
    validate();
  }

  @Override
  public ExpandedFxNonDeliverableForward.Meta metaBean() {
    return ExpandedFxNonDeliverableForward.Meta.INSTANCE;
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
   * @return the value of the property, not null
   */
  public CurrencyAmount getSettlementCurrencyNotional() {
    return settlementCurrencyNotional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount in the non-deliverable currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * @return the value of the property, not null
   */
  public CurrencyAmount getNonDeliverableCurrencyNotional() {
    return nonDeliverableCurrencyNotional;
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
      ExpandedFxNonDeliverableForward other = (ExpandedFxNonDeliverableForward) obj;
      return JodaBeanUtils.equal(getSettlementCurrencyNotional(), other.getSettlementCurrencyNotional()) &&
          JodaBeanUtils.equal(getNonDeliverableCurrencyNotional(), other.getNonDeliverableCurrencyNotional()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getFixingDate(), other.getFixingDate()) &&
          JodaBeanUtils.equal(getValueDate(), other.getValueDate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getSettlementCurrencyNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNonDeliverableCurrencyNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixingDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValueDate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("ExpandedFxNonDeliverableForward{");
    buf.append("settlementCurrencyNotional").append('=').append(getSettlementCurrencyNotional()).append(',').append(' ');
    buf.append("nonDeliverableCurrencyNotional").append('=').append(getNonDeliverableCurrencyNotional()).append(',').append(' ');
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("fixingDate").append('=').append(getFixingDate()).append(',').append(' ');
    buf.append("valueDate").append('=').append(JodaBeanUtils.toString(getValueDate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExpandedFxNonDeliverableForward}.
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
        this, "settlementCurrencyNotional", ExpandedFxNonDeliverableForward.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code nonDeliverableCurrencyNotional} property.
     */
    private final MetaProperty<CurrencyAmount> nonDeliverableCurrencyNotional = DirectMetaProperty.ofImmutable(
        this, "nonDeliverableCurrencyNotional", ExpandedFxNonDeliverableForward.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", ExpandedFxNonDeliverableForward.class, FxIndex.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", ExpandedFxNonDeliverableForward.class, LocalDate.class);
    /**
     * The meta-property for the {@code valueDate} property.
     */
    private final MetaProperty<LocalDate> valueDate = DirectMetaProperty.ofImmutable(
        this, "valueDate", ExpandedFxNonDeliverableForward.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "settlementCurrencyNotional",
        "nonDeliverableCurrencyNotional",
        "index",
        "fixingDate",
        "valueDate");

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
        case -1127062141:  // nonDeliverableCurrencyNotional
          return nonDeliverableCurrencyNotional;
        case 100346066:  // index
          return index;
        case 1255202043:  // fixingDate
          return fixingDate;
        case -766192449:  // valueDate
          return valueDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ExpandedFxNonDeliverableForward.Builder builder() {
      return new ExpandedFxNonDeliverableForward.Builder();
    }

    @Override
    public Class<? extends ExpandedFxNonDeliverableForward> beanType() {
      return ExpandedFxNonDeliverableForward.class;
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
     * The meta-property for the {@code nonDeliverableCurrencyNotional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> nonDeliverableCurrencyNotional() {
      return nonDeliverableCurrencyNotional;
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

    /**
     * The meta-property for the {@code valueDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valueDate() {
      return valueDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 594670010:  // settlementCurrencyNotional
          return ((ExpandedFxNonDeliverableForward) bean).getSettlementCurrencyNotional();
        case -1127062141:  // nonDeliverableCurrencyNotional
          return ((ExpandedFxNonDeliverableForward) bean).getNonDeliverableCurrencyNotional();
        case 100346066:  // index
          return ((ExpandedFxNonDeliverableForward) bean).getIndex();
        case 1255202043:  // fixingDate
          return ((ExpandedFxNonDeliverableForward) bean).getFixingDate();
        case -766192449:  // valueDate
          return ((ExpandedFxNonDeliverableForward) bean).getValueDate();
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
   * The bean-builder for {@code ExpandedFxNonDeliverableForward}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ExpandedFxNonDeliverableForward> {

    private CurrencyAmount settlementCurrencyNotional;
    private CurrencyAmount nonDeliverableCurrencyNotional;
    private FxIndex index;
    private LocalDate fixingDate;
    private LocalDate valueDate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ExpandedFxNonDeliverableForward beanToCopy) {
      this.settlementCurrencyNotional = beanToCopy.getSettlementCurrencyNotional();
      this.nonDeliverableCurrencyNotional = beanToCopy.getNonDeliverableCurrencyNotional();
      this.index = beanToCopy.getIndex();
      this.fixingDate = beanToCopy.getFixingDate();
      this.valueDate = beanToCopy.getValueDate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 594670010:  // settlementCurrencyNotional
          return settlementCurrencyNotional;
        case -1127062141:  // nonDeliverableCurrencyNotional
          return nonDeliverableCurrencyNotional;
        case 100346066:  // index
          return index;
        case 1255202043:  // fixingDate
          return fixingDate;
        case -766192449:  // valueDate
          return valueDate;
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
        case -1127062141:  // nonDeliverableCurrencyNotional
          this.nonDeliverableCurrencyNotional = (CurrencyAmount) newValue;
          break;
        case 100346066:  // index
          this.index = (FxIndex) newValue;
          break;
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
          break;
        case -766192449:  // valueDate
          this.valueDate = (LocalDate) newValue;
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
    public ExpandedFxNonDeliverableForward build() {
      return new ExpandedFxNonDeliverableForward(
          settlementCurrencyNotional,
          nonDeliverableCurrencyNotional,
          index,
          fixingDate,
          valueDate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code settlementCurrencyNotional} property in the builder.
     * @param settlementCurrencyNotional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder settlementCurrencyNotional(CurrencyAmount settlementCurrencyNotional) {
      JodaBeanUtils.notNull(settlementCurrencyNotional, "settlementCurrencyNotional");
      this.settlementCurrencyNotional = settlementCurrencyNotional;
      return this;
    }

    /**
     * Sets the {@code nonDeliverableCurrencyNotional} property in the builder.
     * @param nonDeliverableCurrencyNotional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder nonDeliverableCurrencyNotional(CurrencyAmount nonDeliverableCurrencyNotional) {
      JodaBeanUtils.notNull(nonDeliverableCurrencyNotional, "nonDeliverableCurrencyNotional");
      this.nonDeliverableCurrencyNotional = nonDeliverableCurrencyNotional;
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

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ExpandedFxNonDeliverableForward.Builder{");
      buf.append("settlementCurrencyNotional").append('=').append(JodaBeanUtils.toString(settlementCurrencyNotional)).append(',').append(' ');
      buf.append("nonDeliverableCurrencyNotional").append('=').append(JodaBeanUtils.toString(nonDeliverableCurrencyNotional)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate)).append(',').append(' ');
      buf.append("valueDate").append('=').append(JodaBeanUtils.toString(valueDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
