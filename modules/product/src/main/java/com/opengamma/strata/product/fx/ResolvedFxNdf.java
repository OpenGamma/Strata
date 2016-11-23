/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.product.ResolvedProduct;

/**
 * A Non-Deliverable Forward (NDF), resolved for pricing.
 * <p>
 * This is the resolved form of {@link FxNdf} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedFxNdf} from a {@code FxNdf}
 * using {@link FxNdf#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedFxNdf} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedFxNdf
    implements ResolvedProduct, ImmutableBean, Serializable {

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
   * The FX index observation.
   * <p>
   * This defines the observation of the index used to settle the trade.
   * The value of the trade is based on the difference between the actual rate and the agreed rate.
   * <p>
   * An FX index is a daily rate of exchange between two currencies.
   * Note that the order of the currencies in the index does not matter, as the
   * conversion direction is fully defined by the currency of the reference amount.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxIndexObservation observation;
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
    CurrencyPair pair = observation.getIndex().getCurrencyPair();
    if (!pair.contains(settlementCurrencyNotional.getCurrency())) {
      throw new IllegalArgumentException("FxIndex and settlement notional currency are incompatible");
    }
    if (!(pair.equals(agreedFxRate.getPair()) || pair.isInverse(agreedFxRate.getPair()))) {
      throw new IllegalArgumentException("FxIndex and agreed FX rate are incompatible");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the FX index.
   * 
   * @return the FX index
   */
  public FxIndex getIndex() {
    return observation.getIndex();
  }

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
    FxIndex index = getIndex();
    return index.getCurrencyPair().getBase().equals(getSettlementCurrency()) ?
        index.getCurrencyPair().getCounter() :
        index.getCurrencyPair().getBase();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedFxNdf}.
   * @return the meta-bean, not null
   */
  public static ResolvedFxNdf.Meta meta() {
    return ResolvedFxNdf.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedFxNdf.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedFxNdf.Builder builder() {
    return new ResolvedFxNdf.Builder();
  }

  private ResolvedFxNdf(
      CurrencyAmount settlementCurrencyNotional,
      FxRate agreedFxRate,
      FxIndexObservation observation,
      LocalDate paymentDate) {
    JodaBeanUtils.notNull(settlementCurrencyNotional, "settlementCurrencyNotional");
    JodaBeanUtils.notNull(agreedFxRate, "agreedFxRate");
    JodaBeanUtils.notNull(observation, "observation");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    this.settlementCurrencyNotional = settlementCurrencyNotional;
    this.agreedFxRate = agreedFxRate;
    this.observation = observation;
    this.paymentDate = paymentDate;
    validate();
  }

  @Override
  public ResolvedFxNdf.Meta metaBean() {
    return ResolvedFxNdf.Meta.INSTANCE;
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
   * Gets the FX index observation.
   * <p>
   * This defines the observation of the index used to settle the trade.
   * The value of the trade is based on the difference between the actual rate and the agreed rate.
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
      ResolvedFxNdf other = (ResolvedFxNdf) obj;
      return JodaBeanUtils.equal(settlementCurrencyNotional, other.settlementCurrencyNotional) &&
          JodaBeanUtils.equal(agreedFxRate, other.agreedFxRate) &&
          JodaBeanUtils.equal(observation, other.observation) &&
          JodaBeanUtils.equal(paymentDate, other.paymentDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementCurrencyNotional);
    hash = hash * 31 + JodaBeanUtils.hashCode(agreedFxRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(observation);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("ResolvedFxNdf{");
    buf.append("settlementCurrencyNotional").append('=').append(settlementCurrencyNotional).append(',').append(' ');
    buf.append("agreedFxRate").append('=').append(agreedFxRate).append(',').append(' ');
    buf.append("observation").append('=').append(observation).append(',').append(' ');
    buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedFxNdf}.
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
        this, "settlementCurrencyNotional", ResolvedFxNdf.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code agreedFxRate} property.
     */
    private final MetaProperty<FxRate> agreedFxRate = DirectMetaProperty.ofImmutable(
        this, "agreedFxRate", ResolvedFxNdf.class, FxRate.class);
    /**
     * The meta-property for the {@code observation} property.
     */
    private final MetaProperty<FxIndexObservation> observation = DirectMetaProperty.ofImmutable(
        this, "observation", ResolvedFxNdf.class, FxIndexObservation.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", ResolvedFxNdf.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "settlementCurrencyNotional",
        "agreedFxRate",
        "observation",
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
        case 122345516:  // observation
          return observation;
        case -1540873516:  // paymentDate
          return paymentDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedFxNdf.Builder builder() {
      return new ResolvedFxNdf.Builder();
    }

    @Override
    public Class<? extends ResolvedFxNdf> beanType() {
      return ResolvedFxNdf.class;
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
     * The meta-property for the {@code observation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndexObservation> observation() {
      return observation;
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
          return ((ResolvedFxNdf) bean).getSettlementCurrencyNotional();
        case 1040357930:  // agreedFxRate
          return ((ResolvedFxNdf) bean).getAgreedFxRate();
        case 122345516:  // observation
          return ((ResolvedFxNdf) bean).getObservation();
        case -1540873516:  // paymentDate
          return ((ResolvedFxNdf) bean).getPaymentDate();
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
   * The bean-builder for {@code ResolvedFxNdf}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedFxNdf> {

    private CurrencyAmount settlementCurrencyNotional;
    private FxRate agreedFxRate;
    private FxIndexObservation observation;
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
    private Builder(ResolvedFxNdf beanToCopy) {
      this.settlementCurrencyNotional = beanToCopy.getSettlementCurrencyNotional();
      this.agreedFxRate = beanToCopy.getAgreedFxRate();
      this.observation = beanToCopy.getObservation();
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
        case 122345516:  // observation
          return observation;
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
        case 122345516:  // observation
          this.observation = (FxIndexObservation) newValue;
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
    public ResolvedFxNdf build() {
      return new ResolvedFxNdf(
          settlementCurrencyNotional,
          agreedFxRate,
          observation,
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
     * Sets the FX index observation.
     * <p>
     * This defines the observation of the index used to settle the trade.
     * The value of the trade is based on the difference between the actual rate and the agreed rate.
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
      buf.append("ResolvedFxNdf.Builder{");
      buf.append("settlementCurrencyNotional").append('=').append(JodaBeanUtils.toString(settlementCurrencyNotional)).append(',').append(' ');
      buf.append("agreedFxRate").append('=').append(JodaBeanUtils.toString(agreedFxRate)).append(',').append(' ');
      buf.append("observation").append('=').append(JodaBeanUtils.toString(observation)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
