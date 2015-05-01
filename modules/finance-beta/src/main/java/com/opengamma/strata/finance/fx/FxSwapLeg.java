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
import com.opengamma.strata.collect.ArgChecker;

/**
 * An FX swap leg.
 * <p>
 * An FX swap leg represents the exchange of an equivalent amount in two different currencies
 * between counterparties on a specific date.
 * For example, it might represent the payment of USD 1,000 and the receipt of EUR 932.
 * An {@link FxSwap} holds two swap legs.
 */
@BeanDefinition(builderScope = "private")
public final class FxSwapLeg
    implements ImmutableBean, Serializable {

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
   * The date that the leg settles.
   * <p>
   * On this date, the pay and receive amounts will be exchanged.
   * This date should be a valid business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate valueDate;

  //-------------------------------------------------------------------------
  /**
   * Creates an {@code FxSwapLeg} from two amounts and the value date.
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
   * @param valueDate  the value date
   * @return the FX swap leg
   */
  public static FxSwapLeg of(CurrencyAmount amount1, CurrencyAmount amount2, LocalDate valueDate) {
    CurrencyPair pair = CurrencyPair.of(amount1.getCurrency(), amount2.getCurrency());
    if (pair.isConventional()) {
      return new FxSwapLeg(amount2, amount1, valueDate);
    } else {
      return new FxSwapLeg(amount1, amount2, valueDate);
    }
  }

  /**
   * Creates an {@code FxSwapLeg} from an amount and the FX rate.
   * <p>
   * This creates a swap leg from the currencies, amount and rate.
   * The amount in the second currency is calculated as {@code (amountCurrency2 = -amountCurrency1 * fxRate)}.
   * <p>
   * This factory identifies the currency pair of the exchange and assigns the payments
   * to match the base or counter currency of the standardized currency pair.
   * For example, a EUR/USD exchange always has EUR as the base payment and USD as the counter payment.
   * <p>
   * The two currencies must not be equal.
   * No payment date adjustments apply.
   * 
   * @param amountCurrency1  the amount in the first currency
   * @param currency2  the second currency
   * @param fxRate  the FX rate, where {@code (1.0 * amountCurrency1 = fxRate * amountCurrency2)}
   * @param valueDate  the value date
   * @return the FX swap leg
   */
  public static FxSwapLeg of(CurrencyAmount amountCurrency1, Currency currency2, double fxRate, LocalDate valueDate) {
    ArgChecker.isTrue(amountCurrency1.getCurrency().equals(currency2), "Currencies must not be equal");
    ArgChecker.notNegativeOrZero(fxRate, "fxRate");
    CurrencyAmount amount2 = CurrencyAmount.of(currency2, -amountCurrency1.getAmount() * fxRate);
    return of(amountCurrency1, amount2, valueDate);
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
   * Expands this leg into an {@code ExpandedFx}.
   * 
   * @return the expanded form
   */
  ExpandedFx expand() {
    return ExpandedFx.of(
        FxPayment.of(valueDate, baseCurrencyAmount),
        FxPayment.of(valueDate, counterCurrencyAmount),
        valueDate);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxSwapLeg}.
   * @return the meta-bean, not null
   */
  public static FxSwapLeg.Meta meta() {
    return FxSwapLeg.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxSwapLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxSwapLeg(
      CurrencyAmount baseCurrencyAmount,
      CurrencyAmount counterCurrencyAmount,
      LocalDate valueDate) {
    JodaBeanUtils.notNull(baseCurrencyAmount, "baseCurrencyAmount");
    JodaBeanUtils.notNull(counterCurrencyAmount, "counterCurrencyAmount");
    JodaBeanUtils.notNull(valueDate, "valueDate");
    this.baseCurrencyAmount = baseCurrencyAmount;
    this.counterCurrencyAmount = counterCurrencyAmount;
    this.valueDate = valueDate;
    validate();
  }

  @Override
  public FxSwapLeg.Meta metaBean() {
    return FxSwapLeg.Meta.INSTANCE;
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
   * Gets the date that the leg settles.
   * <p>
   * On this date, the pay and receive amounts will be exchanged.
   * This date should be a valid business day.
   * @return the value of the property, not null
   */
  public LocalDate getValueDate() {
    return valueDate;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxSwapLeg other = (FxSwapLeg) obj;
      return JodaBeanUtils.equal(getBaseCurrencyAmount(), other.getBaseCurrencyAmount()) &&
          JodaBeanUtils.equal(getCounterCurrencyAmount(), other.getCounterCurrencyAmount()) &&
          JodaBeanUtils.equal(getValueDate(), other.getValueDate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getBaseCurrencyAmount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCounterCurrencyAmount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getValueDate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("FxSwapLeg{");
    buf.append("baseCurrencyAmount").append('=').append(getBaseCurrencyAmount()).append(',').append(' ');
    buf.append("counterCurrencyAmount").append('=').append(getCounterCurrencyAmount()).append(',').append(' ');
    buf.append("valueDate").append('=').append(JodaBeanUtils.toString(getValueDate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxSwapLeg}.
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
        this, "baseCurrencyAmount", FxSwapLeg.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code counterCurrencyAmount} property.
     */
    private final MetaProperty<CurrencyAmount> counterCurrencyAmount = DirectMetaProperty.ofImmutable(
        this, "counterCurrencyAmount", FxSwapLeg.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code valueDate} property.
     */
    private final MetaProperty<LocalDate> valueDate = DirectMetaProperty.ofImmutable(
        this, "valueDate", FxSwapLeg.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "baseCurrencyAmount",
        "counterCurrencyAmount",
        "valueDate");

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
        case -766192449:  // valueDate
          return valueDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxSwapLeg> builder() {
      return new FxSwapLeg.Builder();
    }

    @Override
    public Class<? extends FxSwapLeg> beanType() {
      return FxSwapLeg.class;
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
        case 714419450:  // baseCurrencyAmount
          return ((FxSwapLeg) bean).getBaseCurrencyAmount();
        case -446491419:  // counterCurrencyAmount
          return ((FxSwapLeg) bean).getCounterCurrencyAmount();
        case -766192449:  // valueDate
          return ((FxSwapLeg) bean).getValueDate();
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
   * The bean-builder for {@code FxSwapLeg}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<FxSwapLeg> {

    private CurrencyAmount baseCurrencyAmount;
    private CurrencyAmount counterCurrencyAmount;
    private LocalDate valueDate;

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
        case -766192449:  // valueDate
          return valueDate;
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
    public FxSwapLeg build() {
      preBuild(this);
      return new FxSwapLeg(
          baseCurrencyAmount,
          counterCurrencyAmount,
          valueDate);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("FxSwapLeg.Builder{");
      buf.append("baseCurrencyAmount").append('=').append(JodaBeanUtils.toString(baseCurrencyAmount)).append(',').append(' ');
      buf.append("counterCurrencyAmount").append('=').append(JodaBeanUtils.toString(counterCurrencyAmount)).append(',').append(' ');
      buf.append("valueDate").append('=').append(JodaBeanUtils.toString(valueDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
