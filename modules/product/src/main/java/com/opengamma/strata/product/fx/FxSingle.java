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
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.Product;

/**
 * A single foreign exchange, such as an FX forward or FX spot.
 * <p>
 * An FX is a financial instrument that represents the exchange of an equivalent amount
 * in two different currencies between counterparties on a specific date.
 * For example, it might represent the payment of USD 1,000 and the receipt of EUR 932.
 * <p>
 * FX spot and FX forward are essentially equivalent, simply with a different way to obtain the payment date; 
 * they are both represented using this class.
 */
@BeanDefinition(builderScope = "private")
public final class FxSingle
    implements Product, Resolvable<ResolvedFxSingle>, ImmutableBean, Serializable {

  /**
   * The amount in the base currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount baseCurrencyAmount;
  /**
   * The amount in the counter currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount counterCurrencyAmount;
  /**
   * The date that the FX settles.
   * <p>
   * On this date, the pay and receive amounts will be exchanged.
   * This date is typically a valid business day, however the {@code businessDayAdjustment}
   * property may be used to adjust it.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;
  /**
   * The payment date adjustment, optional.
   * <p>
   * If present, the adjustment will be applied to the payment date.
   */
  @PropertyDefinition(get = "optional")
  private final BusinessDayAdjustment paymentDateAdjustment;

  //-------------------------------------------------------------------------
  /**
   * Creates an {@code FxSingle} from two amounts and the value date.
   * <p>
   * The amounts must be of the correct type, one pay and one receive.
   * The currencies of the payments must differ.
   * <p>
   * This factory identifies the currency pair of the exchange and assigns the payments
   * to match the base or counter currency of the standardized currency pair.
   * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
   * <p>
   * No payment date adjustments apply.
   * 
   * @param amount1  the amount in the first currency
   * @param amount2  the amount in the second currency
   * @param paymentDate  the date that the FX settles
   * @return the FX
   */
  public static FxSingle of(CurrencyAmount amount1, CurrencyAmount amount2, LocalDate paymentDate) {
    return create(amount1, amount2, paymentDate, null);
  }

  /**
   * Creates an {@code FxSingle} from two amounts and the value date, specifying a date adjustment.
   * <p>
   * The amounts must be of the correct type, one pay and one receive.
   * The currencies of the payments must differ.
   * <p>
   * This factory identifies the currency pair of the exchange and assigns the payments
   * to match the base or counter currency of the standardized currency pair.
   * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
   * 
   * @param amount1  the amount in the first currency
   * @param amount2  the amount in the second currency
   * @param paymentDate  the date that the FX settles
   * @param paymentDateAdjustment  the adjustment to apply to the payment date
   * @return the FX
   */
  public static FxSingle of(
      CurrencyAmount amount1,
      CurrencyAmount amount2,
      LocalDate paymentDate,
      BusinessDayAdjustment paymentDateAdjustment) {

    ArgChecker.notNull(paymentDateAdjustment, "paymentDateAdjustment");
    return create(amount1, amount2, paymentDate, paymentDateAdjustment);
  }

  /**
   * Creates an {@code FxSingle} using a rate.
   * <p>
   * This creates a single foreign exchange specifying the amount, FX rate and value date.
   * The amount must be specified using one of the currencies of the FX rate.
   * <p>
   * This factory identifies the currency pair of the exchange and assigns the payments
   * to match the base or counter currency of the standardized currency pair.
   * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
   * <p>
   * No payment date adjustments apply.
   * 
   * @param amount  the amount being exchanged, positive if being received, negative if being paid
   * @param fxRate  the FX rate
   * @param paymentDate  the date that the FX settles
   * @return the FX
   */
  public static FxSingle of(CurrencyAmount amount, FxRate fxRate, LocalDate paymentDate) {
    return create(amount, fxRate, paymentDate, null);
  }

  /**
   * Creates an {@code FxSingle} using a rate, specifying a date adjustment.
   * <p>
   * This creates a single foreign exchange specifying the amount, FX rate and value date.
   * The amount must be specified using one of the currencies of the FX rate.
   * <p>
   * This factory identifies the currency pair of the exchange and assigns the payments
   * to match the base or counter currency of the standardized currency pair.
   * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
   * 
   * @param amount  the amount being exchanged, positive if being received, negative if being paid
   * @param fxRate  the FX rate
   * @param paymentDate  the date that the FX settles
   * @param paymentDateAdjustment  the adjustment to apply to the payment date
   * @return the FX
   */
  public static FxSingle of(
      CurrencyAmount amount,
      FxRate fxRate,
      LocalDate paymentDate,
      BusinessDayAdjustment paymentDateAdjustment) {

    ArgChecker.notNull(paymentDateAdjustment, "paymentDateAdjustment");
    return create(amount, fxRate, paymentDate, paymentDateAdjustment);
  }

  // internal method where adjustment may be null
  private static FxSingle create(
      CurrencyAmount amount,
      FxRate fxRate,
      LocalDate paymentDate,
      BusinessDayAdjustment paymentDateAdjustment) {

    CurrencyPair pair = fxRate.getPair();
    ArgChecker.isTrue(pair.contains(amount.getCurrency()));
    Currency currency2 = pair.getBase().equals(amount.getCurrency()) ? pair.getCounter() : pair.getBase();
    CurrencyAmount amountCurrency2 = amount.convertedTo(currency2, fxRate).negated();
    return create(amount, amountCurrency2, paymentDate, paymentDateAdjustment);
  }

  // internal method where adjustment may be null
  private static FxSingle create(
      CurrencyAmount amount1,
      CurrencyAmount amount2,
      LocalDate paymentDate,
      BusinessDayAdjustment paymentDateAdjustment) {

    CurrencyPair pair = CurrencyPair.of(amount2.getCurrency(), amount1.getCurrency());
    if (pair.isConventional()) {
      return new FxSingle(amount2, amount1, paymentDate, paymentDateAdjustment);
    } else {
      return new FxSingle(amount1, amount2, paymentDate, paymentDateAdjustment);
    }
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (baseCurrencyAmount.getCurrency().equals(counterCurrencyAmount.getCurrency())) {
      throw new IllegalArgumentException("Amounts must have different currencies");
    }
    if ((baseCurrencyAmount.getAmount() != 0d || counterCurrencyAmount.getAmount() != 0d) &&
        Math.signum(baseCurrencyAmount.getAmount()) != -Math.signum(counterCurrencyAmount.getAmount())) {
      throw new IllegalArgumentException("Amounts must have different signs");
    }
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    // swap order to be base/counter if reverse is conventional
    // this handles deserialization where the base/counter rules differ from those applicable at serialization
    CurrencyAmount base = builder.baseCurrencyAmount;
    CurrencyAmount counter = builder.counterCurrencyAmount;
    CurrencyPair pair = CurrencyPair.of(counter.getCurrency(), base.getCurrency());
    if (pair.isConventional()) {
      builder.baseCurrencyAmount = counter;
      builder.counterCurrencyAmount = base;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets currency pair of the base currency and counter currency.
   * <p>
   * This currency pair is conventional, thus indifferent to the direction of FX.
   * 
   * @return the currency pair
   */
  public CurrencyPair getCurrencyPair() {
    return CurrencyPair.of(baseCurrencyAmount.getCurrency(), counterCurrencyAmount.getCurrency());
  }

  /**
   * Gets the currency amount in which the amount is received.
   * <p>
   * This returns the currency amount whose amount is non-negative.
   * If both are zero, {@code counterCurrencyAmount} is returned.
   * 
   * @return the receive currency amount
   */
  public CurrencyAmount getReceiveCurrencyAmount() {
    if (baseCurrencyAmount.getAmount() > 0d) {
      return baseCurrencyAmount;
    }
    return counterCurrencyAmount;
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedFxSingle resolve(ReferenceData refData) {
    LocalDate date = paymentDateAdjustment != null ? paymentDateAdjustment.adjust(paymentDate, refData) : paymentDate;
    return ResolvedFxSingle.of(
        Payment.of(baseCurrencyAmount, date),
        Payment.of(counterCurrencyAmount, date));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxSingle}.
   * @return the meta-bean, not null
   */
  public static FxSingle.Meta meta() {
    return FxSingle.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxSingle.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxSingle(
      CurrencyAmount baseCurrencyAmount,
      CurrencyAmount counterCurrencyAmount,
      LocalDate paymentDate,
      BusinessDayAdjustment paymentDateAdjustment) {
    JodaBeanUtils.notNull(baseCurrencyAmount, "baseCurrencyAmount");
    JodaBeanUtils.notNull(counterCurrencyAmount, "counterCurrencyAmount");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    this.baseCurrencyAmount = baseCurrencyAmount;
    this.counterCurrencyAmount = counterCurrencyAmount;
    this.paymentDate = paymentDate;
    this.paymentDateAdjustment = paymentDateAdjustment;
    validate();
  }

  @Override
  public FxSingle.Meta metaBean() {
    return FxSingle.Meta.INSTANCE;
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
   * Gets the amount in the base currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * @return the value of the property, not null
   */
  public CurrencyAmount getBaseCurrencyAmount() {
    return baseCurrencyAmount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the amount in the counter currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * @return the value of the property, not null
   */
  public CurrencyAmount getCounterCurrencyAmount() {
    return counterCurrencyAmount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that the FX settles.
   * <p>
   * On this date, the pay and receive amounts will be exchanged.
   * This date is typically a valid business day, however the {@code businessDayAdjustment}
   * property may be used to adjust it.
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment date adjustment, optional.
   * <p>
   * If present, the adjustment will be applied to the payment date.
   * @return the optional value of the property, not null
   */
  public Optional<BusinessDayAdjustment> getPaymentDateAdjustment() {
    return Optional.ofNullable(paymentDateAdjustment);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxSingle other = (FxSingle) obj;
      return JodaBeanUtils.equal(baseCurrencyAmount, other.baseCurrencyAmount) &&
          JodaBeanUtils.equal(counterCurrencyAmount, other.counterCurrencyAmount) &&
          JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(paymentDateAdjustment, other.paymentDateAdjustment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(baseCurrencyAmount);
    hash = hash * 31 + JodaBeanUtils.hashCode(counterCurrencyAmount);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDateAdjustment);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("FxSingle{");
    buf.append("baseCurrencyAmount").append('=').append(baseCurrencyAmount).append(',').append(' ');
    buf.append("counterCurrencyAmount").append('=').append(counterCurrencyAmount).append(',').append(' ');
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("paymentDateAdjustment").append('=').append(JodaBeanUtils.toString(paymentDateAdjustment));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxSingle}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code baseCurrencyAmount} property.
     */
    private final MetaProperty<CurrencyAmount> baseCurrencyAmount = DirectMetaProperty.ofImmutable(
        this, "baseCurrencyAmount", FxSingle.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code counterCurrencyAmount} property.
     */
    private final MetaProperty<CurrencyAmount> counterCurrencyAmount = DirectMetaProperty.ofImmutable(
        this, "counterCurrencyAmount", FxSingle.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", FxSingle.class, LocalDate.class);
    /**
     * The meta-property for the {@code paymentDateAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> paymentDateAdjustment = DirectMetaProperty.ofImmutable(
        this, "paymentDateAdjustment", FxSingle.class, BusinessDayAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "baseCurrencyAmount",
        "counterCurrencyAmount",
        "paymentDate",
        "paymentDateAdjustment");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 714419450:  // baseCurrencyAmount
          return baseCurrencyAmount;
        case -446491419:  // counterCurrencyAmount
          return counterCurrencyAmount;
        case -1540873516:  // paymentDate
          return paymentDate;
        case 737375073:  // paymentDateAdjustment
          return paymentDateAdjustment;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxSingle> builder() {
      return new FxSingle.Builder();
    }

    @Override
    public Class<? extends FxSingle> beanType() {
      return FxSingle.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code baseCurrencyAmount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> baseCurrencyAmount() {
      return baseCurrencyAmount;
    }

    /**
     * The meta-property for the {@code counterCurrencyAmount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> counterCurrencyAmount() {
      return counterCurrencyAmount;
    }

    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    /**
     * The meta-property for the {@code paymentDateAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> paymentDateAdjustment() {
      return paymentDateAdjustment;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 714419450:  // baseCurrencyAmount
          return ((FxSingle) bean).getBaseCurrencyAmount();
        case -446491419:  // counterCurrencyAmount
          return ((FxSingle) bean).getCounterCurrencyAmount();
        case -1540873516:  // paymentDate
          return ((FxSingle) bean).getPaymentDate();
        case 737375073:  // paymentDateAdjustment
          return ((FxSingle) bean).paymentDateAdjustment;
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
   * The bean-builder for {@code FxSingle}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<FxSingle> {

    private CurrencyAmount baseCurrencyAmount;
    private CurrencyAmount counterCurrencyAmount;
    private LocalDate paymentDate;
    private BusinessDayAdjustment paymentDateAdjustment;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 714419450:  // baseCurrencyAmount
          return baseCurrencyAmount;
        case -446491419:  // counterCurrencyAmount
          return counterCurrencyAmount;
        case -1540873516:  // paymentDate
          return paymentDate;
        case 737375073:  // paymentDateAdjustment
          return paymentDateAdjustment;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 714419450:  // baseCurrencyAmount
          this.baseCurrencyAmount = (CurrencyAmount) newValue;
          break;
        case -446491419:  // counterCurrencyAmount
          this.counterCurrencyAmount = (CurrencyAmount) newValue;
          break;
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case 737375073:  // paymentDateAdjustment
          this.paymentDateAdjustment = (BusinessDayAdjustment) newValue;
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
    public FxSingle build() {
      preBuild(this);
      return new FxSingle(
          baseCurrencyAmount,
          counterCurrencyAmount,
          paymentDate,
          paymentDateAdjustment);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("FxSingle.Builder{");
      buf.append("baseCurrencyAmount").append('=').append(JodaBeanUtils.toString(baseCurrencyAmount)).append(',').append(' ');
      buf.append("counterCurrencyAmount").append('=').append(JodaBeanUtils.toString(counterCurrencyAmount)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("paymentDateAdjustment").append('=').append(JodaBeanUtils.toString(paymentDateAdjustment));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
