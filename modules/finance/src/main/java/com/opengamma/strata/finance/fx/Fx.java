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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.ArgChecker;

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
public final class Fx
    implements FxProduct, ImmutableBean, Serializable {

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
   * This date should be a valid business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;

  //-------------------------------------------------------------------------
  /**
   * Creates an {@code Fx} from two amounts and the value date.
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
  public static Fx of(CurrencyAmount amount1, CurrencyAmount amount2, LocalDate paymentDate) {
    CurrencyPair pair = CurrencyPair.of(amount2.getCurrency(), amount1.getCurrency());
    if (pair.isConventional()) {
      return new Fx(amount2, amount1, paymentDate);
    } else {
      return new Fx(amount1, amount2, paymentDate);
    }
  }

  /**
   * Creates an {@code Fx} using a rate.
   * <p>
   * This create an FX specifying a value date, notional in one currency, the second currency
   * and the FX rate between the two.
   * The currencies of the payments must differ.
   * <p>
   * This factory identifies the currency pair of the exchange and assigns the payments
   * to match the base or counter currency of the standardized currency pair.
   * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
   * <p>
   * No payment date adjustments apply.
   * 
   * @param amountCurrency1  the amount of the near leg in the first currency
   * @param fxRate  the near FX rate
   * @param paymentDate  date that the FX settles
   * @return the FX
   */
  public static Fx of(CurrencyAmount amountCurrency1, FxRate fxRate, LocalDate paymentDate) {
    CurrencyPair pair = fxRate.getPair();
    ArgChecker.isTrue(pair.contains(amountCurrency1.getCurrency()));
    Currency currency2 = pair.getBase().equals(amountCurrency1.getCurrency()) ? pair.getCounter() : pair.getBase();
    CurrencyAmount amountCurrency2 = amountCurrency1.convertedTo(currency2, fxRate).negated();
    return of(amountCurrency1, amountCurrency2, paymentDate);
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
    // this handled deserialization where the base/counter rules differ from those applicable at serialization
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
  /**
   * Expands this FX into an {@code ExpandedFx}.
   * 
   * @return the transaction
   */
  @Override
  public ExpandedFx expand() {
    return ExpandedFx.of(
        FxPayment.of(baseCurrencyAmount, paymentDate),
        FxPayment.of(counterCurrencyAmount, paymentDate));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Fx}.
   * @return the meta-bean, not null
   */
  public static Fx.Meta meta() {
    return Fx.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Fx.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private Fx(
      CurrencyAmount baseCurrencyAmount,
      CurrencyAmount counterCurrencyAmount,
      LocalDate paymentDate) {
    JodaBeanUtils.notNull(baseCurrencyAmount, "baseCurrencyAmount");
    JodaBeanUtils.notNull(counterCurrencyAmount, "counterCurrencyAmount");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    this.baseCurrencyAmount = baseCurrencyAmount;
    this.counterCurrencyAmount = counterCurrencyAmount;
    this.paymentDate = paymentDate;
    validate();
  }

  @Override
  public Fx.Meta metaBean() {
    return Fx.Meta.INSTANCE;
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
   * This date should be a valid business day.
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Fx other = (Fx) obj;
      return JodaBeanUtils.equal(getBaseCurrencyAmount(), other.getBaseCurrencyAmount()) &&
          JodaBeanUtils.equal(getCounterCurrencyAmount(), other.getCounterCurrencyAmount()) &&
          JodaBeanUtils.equal(getPaymentDate(), other.getPaymentDate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getBaseCurrencyAmount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCounterCurrencyAmount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentDate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("Fx{");
    buf.append("baseCurrencyAmount").append('=').append(getBaseCurrencyAmount()).append(',').append(' ');
    buf.append("counterCurrencyAmount").append('=').append(getCounterCurrencyAmount()).append(',').append(' ');
    buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(getPaymentDate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Fx}.
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
        this, "baseCurrencyAmount", Fx.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code counterCurrencyAmount} property.
     */
    private final MetaProperty<CurrencyAmount> counterCurrencyAmount = DirectMetaProperty.ofImmutable(
        this, "counterCurrencyAmount", Fx.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", Fx.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "baseCurrencyAmount",
        "counterCurrencyAmount",
        "paymentDate");

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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends Fx> builder() {
      return new Fx.Builder();
    }

    @Override
    public Class<? extends Fx> beanType() {
      return Fx.class;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 714419450:  // baseCurrencyAmount
          return ((Fx) bean).getBaseCurrencyAmount();
        case -446491419:  // counterCurrencyAmount
          return ((Fx) bean).getCounterCurrencyAmount();
        case -1540873516:  // paymentDate
          return ((Fx) bean).getPaymentDate();
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
   * The bean-builder for {@code Fx}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<Fx> {

    private CurrencyAmount baseCurrencyAmount;
    private CurrencyAmount counterCurrencyAmount;
    private LocalDate paymentDate;

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
    public Fx build() {
      preBuild(this);
      return new Fx(
          baseCurrencyAmount,
          counterCurrencyAmount,
          paymentDate);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("Fx.Builder{");
      buf.append("baseCurrencyAmount").append('=').append(JodaBeanUtils.toString(baseCurrencyAmount)).append(',').append(' ');
      buf.append("counterCurrencyAmount").append('=').append(JodaBeanUtils.toString(counterCurrencyAmount)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
