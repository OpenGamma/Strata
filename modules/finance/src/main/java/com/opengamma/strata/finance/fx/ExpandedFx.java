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

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An expanded single FX transaction, the low level representation of a simple foreign exchange.
 * <p>
 * This represents a single foreign exchange on a specific date.
 * The two payments are identified as the base and counter currencies in a standardized currency pair.
 * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
 * See {@link CurrencyPair} for details of the configuration that determines the ordering.
 * <p>
 * An {@code ExpandedFx} may contain information based on holiday calendars.
 * If a holiday calendar changes, the adjusted dates may no longer be correct.
 * Care must be taken when placing the expanded form in a cache or persistence layer.
 * Application code should use {@link Fx}, not this class.
 */
@BeanDefinition(builderScope = "private")
public final class ExpandedFx
    implements FxProduct, ImmutableBean, Serializable {

  /**
   * The payment in the base currency, positive if receiving, negative if paying.
   * <p>
   * The payment amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxPayment baseCurrencyPayment;
  /**
   * The payment in the counter currency, positive if receiving, negative if paying.
   * <p>
   * The payment amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxPayment counterCurrencyPayment;

  //-------------------------------------------------------------------------
  /**
   * Creates an {@code ExpandedFx} from two equivalent payments in different currencies.
   * <p>
   * The payments must be of the correct type, one pay and one receive.
   * The currencies of the payments must differ.
   * <p>
   * This factory identifies the currency pair of the exchange and assigns the payments
   * to match the base or counter currency of the standardized currency pair.
   * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
   * 
   * @param payment1  the first payment
   * @param payment2  the second payment
   * @return the expanded foreign exchange transaction
   */
  public static ExpandedFx of(FxPayment payment1, FxPayment payment2) {
    CurrencyPair pair = CurrencyPair.of(payment2.getCurrency(), payment1.getCurrency());
    if (pair.isConventional()) {
      return new ExpandedFx(payment2, payment1);
    } else {
      return new ExpandedFx(payment1, payment2);
    }
  }

  /**
   * Creates an {@code ExpandedFx} from two amounts and the value date.
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
   * @param valueDate  the value date
   * @return the expanded foreign exchange transaction
   */
  public static ExpandedFx of(CurrencyAmount amount1, CurrencyAmount amount2, LocalDate valueDate) {
    return ExpandedFx.of(FxPayment.of(amount1, valueDate), FxPayment.of(amount2, valueDate));
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (baseCurrencyPayment.getCurrency().equals(counterCurrencyPayment.getCurrency())) {
      throw new IllegalArgumentException("Payments must have different currencies");
    }
    if ((baseCurrencyPayment.getAmount() != 0d || counterCurrencyPayment.getAmount() != 0d) &&
        Math.signum(baseCurrencyPayment.getAmount()) != -Math.signum(counterCurrencyPayment.getAmount())) {
      throw new IllegalArgumentException("Payments must have different signs");
    }
    ArgChecker.inOrderOrEqual(baseCurrencyPayment.getPaymentDate(), counterCurrencyPayment.getPaymentDate(),
        "baseCurrencyPayment.date", "counterCurrencyPayment.date");
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    // swap order to be base/counter if reverse is conventional
    // this handled deserialization where the base/counter rules differ from those applicable at serialization
    FxPayment base = builder.baseCurrencyPayment;
    FxPayment counter = builder.counterCurrencyPayment;
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
  public CurrencyPair getCurrencyPair() {
    return CurrencyPair.of(baseCurrencyPayment.getCurrency(), counterCurrencyPayment.getCurrency());
  }

  /**
   * Gets the currency amount in which the amount is received.
   * <p>
   * This returns the currency amount whose amount is non-negative. 
   * If both are zero, the currency amount of {@code counterCurrencyPayment} is returned. 
   * 
   * @return the receive currency amount
   */
  public CurrencyAmount getReceiveCurrencyAmount() {
    if (baseCurrencyPayment.getAmount() > 0d) {
      return CurrencyAmount.of(baseCurrencyPayment.getCurrency(), baseCurrencyPayment.getAmount());
    }
    return CurrencyAmount.of(counterCurrencyPayment.getCurrency(), counterCurrencyPayment.getAmount());
  }

  /**
   * Returns the date that the transaction settles.
   * <p>
   * This returns the settlement date of the base currency.
   * 
   * @return the value date
   */
  public LocalDate getPaymentDate() {
    return baseCurrencyPayment.getPaymentDate();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the inverse transaction.
   * <p>
   * The result has the base and counter payments negated.
   * 
   * @return the inverse transaction
   */
  public ExpandedFx inverse() {
    return new ExpandedFx(baseCurrencyPayment.inverse(), counterCurrencyPayment.inverse());
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this transaction, trivially returning {@code this}.
   * 
   * @return this transaction
   */
  @Override
  public ExpandedFx expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExpandedFx}.
   * @return the meta-bean, not null
   */
  public static ExpandedFx.Meta meta() {
    return ExpandedFx.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExpandedFx.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ExpandedFx(
      FxPayment baseCurrencyPayment,
      FxPayment counterCurrencyPayment) {
    JodaBeanUtils.notNull(baseCurrencyPayment, "baseCurrencyPayment");
    JodaBeanUtils.notNull(counterCurrencyPayment, "counterCurrencyPayment");
    this.baseCurrencyPayment = baseCurrencyPayment;
    this.counterCurrencyPayment = counterCurrencyPayment;
    validate();
  }

  @Override
  public ExpandedFx.Meta metaBean() {
    return ExpandedFx.Meta.INSTANCE;
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
   * Gets the payment in the base currency, positive if receiving, negative if paying.
   * <p>
   * The payment amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * @return the value of the property, not null
   */
  public FxPayment getBaseCurrencyPayment() {
    return baseCurrencyPayment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment in the counter currency, positive if receiving, negative if paying.
   * <p>
   * The payment amount is signed.
   * A positive amount indicates the payment is to be received.
   * A negative amount indicates the payment is to be paid.
   * @return the value of the property, not null
   */
  public FxPayment getCounterCurrencyPayment() {
    return counterCurrencyPayment;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ExpandedFx other = (ExpandedFx) obj;
      return JodaBeanUtils.equal(getBaseCurrencyPayment(), other.getBaseCurrencyPayment()) &&
          JodaBeanUtils.equal(getCounterCurrencyPayment(), other.getCounterCurrencyPayment());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getBaseCurrencyPayment());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCounterCurrencyPayment());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ExpandedFx{");
    buf.append("baseCurrencyPayment").append('=').append(getBaseCurrencyPayment()).append(',').append(' ');
    buf.append("counterCurrencyPayment").append('=').append(JodaBeanUtils.toString(getCounterCurrencyPayment()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExpandedFx}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code baseCurrencyPayment} property.
     */
    private final MetaProperty<FxPayment> baseCurrencyPayment = DirectMetaProperty.ofImmutable(
        this, "baseCurrencyPayment", ExpandedFx.class, FxPayment.class);
    /**
     * The meta-property for the {@code counterCurrencyPayment} property.
     */
    private final MetaProperty<FxPayment> counterCurrencyPayment = DirectMetaProperty.ofImmutable(
        this, "counterCurrencyPayment", ExpandedFx.class, FxPayment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "baseCurrencyPayment",
        "counterCurrencyPayment");

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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ExpandedFx> builder() {
      return new ExpandedFx.Builder();
    }

    @Override
    public Class<? extends ExpandedFx> beanType() {
      return ExpandedFx.class;
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
    public MetaProperty<FxPayment> baseCurrencyPayment() {
      return baseCurrencyPayment;
    }

    /**
     * The meta-property for the {@code counterCurrencyPayment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxPayment> counterCurrencyPayment() {
      return counterCurrencyPayment;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 765258148:  // baseCurrencyPayment
          return ((ExpandedFx) bean).getBaseCurrencyPayment();
        case -863240423:  // counterCurrencyPayment
          return ((ExpandedFx) bean).getCounterCurrencyPayment();
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
   * The bean-builder for {@code ExpandedFx}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ExpandedFx> {

    private FxPayment baseCurrencyPayment;
    private FxPayment counterCurrencyPayment;

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
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 765258148:  // baseCurrencyPayment
          this.baseCurrencyPayment = (FxPayment) newValue;
          break;
        case -863240423:  // counterCurrencyPayment
          this.counterCurrencyPayment = (FxPayment) newValue;
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
    public ExpandedFx build() {
      preBuild(this);
      return new ExpandedFx(
          baseCurrencyPayment,
          counterCurrencyPayment);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ExpandedFx.Builder{");
      buf.append("baseCurrencyPayment").append('=').append(JodaBeanUtils.toString(baseCurrencyPayment)).append(',').append(' ');
      buf.append("counterCurrencyPayment").append('=').append(JodaBeanUtils.toString(counterCurrencyPayment));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
