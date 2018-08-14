/*
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

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;
import org.joda.beans.ser.SerDeserializer;

import com.google.common.collect.Ordering;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

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
    implements FxProduct, Resolvable<ResolvedFxSingle>, ImmutableBean, Serializable {

  /**
   * The deserializer, for compatibility.
   */
  public static final SerDeserializer DESERIALIZER = new FxSingleDeserializer();

  /**
   * The payment in the base currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * <p>
   * The payment date is usually the same as {@code counterCurrencyPayment}.
   * It is typically a valid business day, however the {@code businessDayAdjustment}
   * property may be used to adjust it.
   */
  @PropertyDefinition(validate = "notNull")
  private final Payment baseCurrencyPayment;
  /**
   * The payment in the counter currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * <p>
   * The payment date is usually the same as {@code baseCurrencyPayment}.
   * It is typically a valid business day, however the {@code businessDayAdjustment}
   * property may be used to adjust it.
   */
  @PropertyDefinition(validate = "notNull")
  private final Payment counterCurrencyPayment;
  /**
   * The payment date adjustment, optional.
   * <p>
   * If present, the adjustment will be applied to the payment date.
   */
  @PropertyDefinition(get = "optional")
  private final BusinessDayAdjustment paymentDateAdjustment;

  //-------------------------------------------------------------------------
  /**
   * Creates an {@code FxSingle} from two payments.
   * <p>
   * The payments must be of the correct type, one pay and one receive.
   * The currencies of the payments must differ.
   * The payment dates may differ.
   * <p>
   * This factory identifies the currency pair of the exchange and assigns the payments
   * to match the base or counter currency of the standardized currency pair.
   * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
   * <p>
   * No payment date adjustments apply.
   * 
   * @param payment1  the payment in the first currency
   * @param payment2  the payment in the second currency
   * @return the FX
   */
  public static FxSingle of(Payment payment1, Payment payment2) {
    return create(payment1, payment2, null);
  }

  /**
   * Creates an {@code FxSingle} from two payments, specifying a date adjustment.
   * <p>
   * The payments must be of the correct type, one pay and one receive.
   * The currencies of the payments must differ.
   * The payment dates may differ.
   * <p>
   * This factory identifies the currency pair of the exchange and assigns the payments
   * to match the base or counter currency of the standardized currency pair.
   * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
   * 
   * @param payment1  the payment in the first currency
   * @param payment2  the payment in the second currency
   * @param paymentDateAdjustment  the adjustment to apply to the payment date
   * @return the FX
   */
  public static FxSingle of(Payment payment1, Payment payment2, BusinessDayAdjustment paymentDateAdjustment) {
    ArgChecker.notNull(paymentDateAdjustment, "paymentDateAdjustment");
    return create(payment1, payment2, paymentDateAdjustment);
  }

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
   * @throws IllegalArgumentException if the FX rate and amount do not have a currency in common
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
   * @throws IllegalArgumentException if the FX rate and amount do not have a currency in common
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

    ArgChecker.notNull(amount, "amount");
    ArgChecker.notNull(fxRate, "fxRate");
    ArgChecker.notNull(paymentDate, "paymentDate");
    CurrencyPair pair = fxRate.getPair();
    if (!pair.contains(amount.getCurrency())) {
      throw new IllegalArgumentException(Messages.format(
          "FxRate '{}' and CurrencyAmount '{}' must have a currency in common", fxRate, amount));
    }
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

    ArgChecker.notNull(amount1, "amount1");
    ArgChecker.notNull(amount2, "amount2");
    ArgChecker.notNull(paymentDate, "paymentDate");
    return create(Payment.of(amount1, paymentDate), Payment.of(amount2, paymentDate), paymentDateAdjustment);
  }

  // internal method where adjustment may be null
  private static FxSingle create(
      Payment payment1,
      Payment payment2,
      BusinessDayAdjustment paymentDateAdjustment) {

    ArgChecker.notNull(payment1, "payment1");
    ArgChecker.notNull(payment2, "payment2");
    CurrencyPair pair = CurrencyPair.of(payment1.getCurrency(), payment2.getCurrency());
    if (pair.isConventional()) {
      return new FxSingle(payment1, payment2, paymentDateAdjustment);
    } else {
      return new FxSingle(payment2, payment1, paymentDateAdjustment);
    }
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (baseCurrencyPayment.getCurrency().equals(counterCurrencyPayment.getCurrency())) {
      throw new IllegalArgumentException("Amounts must have different currencies");
    }
    if ((baseCurrencyPayment.getAmount() != 0d || counterCurrencyPayment.getAmount() != 0d) &&
        Math.signum(baseCurrencyPayment.getAmount()) != -Math.signum(counterCurrencyPayment.getAmount())) {
      throw new IllegalArgumentException("Amounts must have different signs");
    }
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    // swap order to be base/counter if reverse is conventional
    // this handles deserialization where the base/counter rules differ from those applicable at serialization
    Payment base = builder.baseCurrencyPayment;
    Payment counter = builder.counterCurrencyPayment;
    CurrencyPair pair = CurrencyPair.of(counter.getCurrency(), base.getCurrency());
    if (pair.isConventional()) {
      builder.baseCurrencyPayment = counter;
      builder.counterCurrencyPayment = base;
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
  @Override
  public CurrencyPair getCurrencyPair() {
    return CurrencyPair.of(baseCurrencyPayment.getCurrency(), counterCurrencyPayment.getCurrency());
  }

  /**
   * Gets the amount in the base currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * 
   * @return the amount
   */
  public CurrencyAmount getBaseCurrencyAmount() {
    return baseCurrencyPayment.getValue();
  }

  /**
   * Gets the amount in the counter currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * 
   * @return the amount
   */
  public CurrencyAmount getCounterCurrencyAmount() {
    return counterCurrencyPayment.getValue();
  }

  /**
   * Gets the currency amount in which the amount is paid.
   * <p>
   * This returns the currency amount whose amount is negative or zero.
   * 
   * @return the pay currency amount
   */
  public CurrencyAmount getPayCurrencyAmount() {
    if (baseCurrencyPayment.getAmount() <= 0d) {
      return baseCurrencyPayment.getValue();
    }
    return counterCurrencyPayment.getValue();
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
    if (baseCurrencyPayment.getAmount() > 0d) {
      return baseCurrencyPayment.getValue();
    }
    return counterCurrencyPayment.getValue();
  }

  /**
   * Gets the last payment date.
   * <p>
   * The payment date is normally the same for the base and counter currencies.
   * If it differs, this method returns the latest of the two dates.
   * 
   * @return the latest payment date
   */
  public LocalDate getPaymentDate() {
    return Ordering.natural().max(baseCurrencyPayment.getDate(), counterCurrencyPayment.getDate());
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedFxSingle resolve(ReferenceData refData) {
    if (paymentDateAdjustment == null) {
      return ResolvedFxSingle.of(baseCurrencyPayment, counterCurrencyPayment);
    }
    DateAdjuster adjuster = paymentDateAdjustment.resolve(refData);
    return ResolvedFxSingle.of(baseCurrencyPayment.adjustDate(adjuster), counterCurrencyPayment.adjustDate(adjuster));
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FxSingle}.
   * @return the meta-bean, not null
   */
  public static FxSingle.Meta meta() {
    return FxSingle.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FxSingle.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxSingle(
      Payment baseCurrencyPayment,
      Payment counterCurrencyPayment,
      BusinessDayAdjustment paymentDateAdjustment) {
    JodaBeanUtils.notNull(baseCurrencyPayment, "baseCurrencyPayment");
    JodaBeanUtils.notNull(counterCurrencyPayment, "counterCurrencyPayment");
    this.baseCurrencyPayment = baseCurrencyPayment;
    this.counterCurrencyPayment = counterCurrencyPayment;
    this.paymentDateAdjustment = paymentDateAdjustment;
    validate();
  }

  @Override
  public FxSingle.Meta metaBean() {
    return FxSingle.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment in the base currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * <p>
   * The payment date is usually the same as {@code counterCurrencyPayment}.
   * It is typically a valid business day, however the {@code businessDayAdjustment}
   * property may be used to adjust it.
   * @return the value of the property, not null
   */
  public Payment getBaseCurrencyPayment() {
    return baseCurrencyPayment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment in the counter currency, positive if receiving, negative if paying.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * <p>
   * The payment date is usually the same as {@code baseCurrencyPayment}.
   * It is typically a valid business day, however the {@code businessDayAdjustment}
   * property may be used to adjust it.
   * @return the value of the property, not null
   */
  public Payment getCounterCurrencyPayment() {
    return counterCurrencyPayment;
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
      return JodaBeanUtils.equal(baseCurrencyPayment, other.baseCurrencyPayment) &&
          JodaBeanUtils.equal(counterCurrencyPayment, other.counterCurrencyPayment) &&
          JodaBeanUtils.equal(paymentDateAdjustment, other.paymentDateAdjustment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(baseCurrencyPayment);
    hash = hash * 31 + JodaBeanUtils.hashCode(counterCurrencyPayment);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDateAdjustment);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("FxSingle{");
    buf.append("baseCurrencyPayment").append('=').append(baseCurrencyPayment).append(',').append(' ');
    buf.append("counterCurrencyPayment").append('=').append(counterCurrencyPayment).append(',').append(' ');
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
     * The meta-property for the {@code baseCurrencyPayment} property.
     */
    private final MetaProperty<Payment> baseCurrencyPayment = DirectMetaProperty.ofImmutable(
        this, "baseCurrencyPayment", FxSingle.class, Payment.class);
    /**
     * The meta-property for the {@code counterCurrencyPayment} property.
     */
    private final MetaProperty<Payment> counterCurrencyPayment = DirectMetaProperty.ofImmutable(
        this, "counterCurrencyPayment", FxSingle.class, Payment.class);
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
        "baseCurrencyPayment",
        "counterCurrencyPayment",
        "paymentDateAdjustment");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 765258148:  // baseCurrencyPayment
          return baseCurrencyPayment;
        case -863240423:  // counterCurrencyPayment
          return counterCurrencyPayment;
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
     * The meta-property for the {@code baseCurrencyPayment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Payment> baseCurrencyPayment() {
      return baseCurrencyPayment;
    }

    /**
     * The meta-property for the {@code counterCurrencyPayment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Payment> counterCurrencyPayment() {
      return counterCurrencyPayment;
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
        case 765258148:  // baseCurrencyPayment
          return ((FxSingle) bean).getBaseCurrencyPayment();
        case -863240423:  // counterCurrencyPayment
          return ((FxSingle) bean).getCounterCurrencyPayment();
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
  private static final class Builder extends DirectPrivateBeanBuilder<FxSingle> {

    private Payment baseCurrencyPayment;
    private Payment counterCurrencyPayment;
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
        case 765258148:  // baseCurrencyPayment
          return baseCurrencyPayment;
        case -863240423:  // counterCurrencyPayment
          return counterCurrencyPayment;
        case 737375073:  // paymentDateAdjustment
          return paymentDateAdjustment;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 765258148:  // baseCurrencyPayment
          this.baseCurrencyPayment = (Payment) newValue;
          break;
        case -863240423:  // counterCurrencyPayment
          this.counterCurrencyPayment = (Payment) newValue;
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
    public FxSingle build() {
      preBuild(this);
      return new FxSingle(
          baseCurrencyPayment,
          counterCurrencyPayment,
          paymentDateAdjustment);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("FxSingle.Builder{");
      buf.append("baseCurrencyPayment").append('=').append(JodaBeanUtils.toString(baseCurrencyPayment)).append(',').append(' ');
      buf.append("counterCurrencyPayment").append('=').append(JodaBeanUtils.toString(counterCurrencyPayment)).append(',').append(' ');
      buf.append("paymentDateAdjustment").append('=').append(JodaBeanUtils.toString(paymentDateAdjustment));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
