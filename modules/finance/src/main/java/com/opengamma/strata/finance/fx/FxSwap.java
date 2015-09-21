/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import static java.lang.Math.signum;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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
import com.opengamma.strata.collect.ArgChecker;

/**
 * An FX swap.
 * <p>
 * An FX swap is a financial instrument that represents the exchange of an equivalent amount
 * in two different currencies between counterparties on two different dates.
 * <p>
 * The two exchanges are based on the same currency pair, with the two payment flows in the opposite directions.
 * <p>
 * For example, an FX swap might represent the payment of USD 1,000 and the receipt of EUR 932
 * on one date, and the payment of EUR 941 and the receipt of USD 1,000 at a later date.
 */
@BeanDefinition(builderScope = "private")
public final class FxSwap
    implements FxSwapProduct, ImmutableBean, Serializable {

  /**
   * The foreign exchange transaction at the earlier date.
   * <p>
   * This provides details of a single foreign exchange at a specific date.
   * The payment date of this transaction must be before that of the far leg.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxSingle nearLeg;
  /**
   * The foreign exchange transaction at the later date.
   * <p>
   * This provides details of a single foreign exchange at a specific date.
   * The payment date of this transaction must be after that of the near leg.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxSingle farLeg;

  //-------------------------------------------------------------------------
  /**
   * Creates an {@code FxSwap} from two transactions.
   * <p>
   * The transactions must be passed in with value dates in the correct order.
   * The currency pair of each leg must match and have amounts flowing in opposite directions.
   * 
   * @param nearLeg  the earlier leg
   * @param farLeg  the later leg
   * @return the FX swap
   */
  public static FxSwap of(FxSingle nearLeg, FxSingle farLeg) {
    return new FxSwap(nearLeg, farLeg);
  }

  /**
   * Creates an {@code FxSwap} using forward points.
   * <p>
   * The FX rate at the near date is specified as {@code fxRate}.
   * The FX rate at the far date is equal to {@code fxRate + forwardPoints}
   * <p>
   * The two currencies must not be equal.
   * The near date must be before the far date.
   * Conventions will be used to determine the base and counter currency.
   * 
   * @param amountCurrency1  the amount of the near leg in the first currency
   * @param currency2  the second currency
   * @param nearFxRate  the near FX rate, where {@code (1.0 * amountCurrency1 = fxRate * amountCurrency2)}
   * @param forwardPoints  the forward points, where the far FX rate is {@code (fxRate + forwardPoints)}
   * @param nearDate  the near value date
   * @param farDate  the far value date
   * @return the FX swap
   */
  public static FxSwap ofForwardPoints(
      CurrencyAmount amountCurrency1,
      Currency currency2,
      double nearFxRate,
      double forwardPoints,
      LocalDate nearDate,
      LocalDate farDate) {

    Currency currency1 = amountCurrency1.getCurrency();
    ArgChecker.isFalse(currency1.equals(currency2), "Currencies must not be equal");
    ArgChecker.notNegativeOrZero(nearFxRate, "fxRate");
    double farFxRate = nearFxRate + forwardPoints;
    FxSingle nearLeg = FxSingle.of(amountCurrency1, FxRate.of(currency1, currency2, nearFxRate), nearDate);
    FxSingle farLeg = FxSingle.of(amountCurrency1.negated(), FxRate.of(currency1, currency2, farFxRate), farDate);
    return of(nearLeg, farLeg);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(
        nearLeg.getPaymentDate(), farLeg.getPaymentDate(), "nearLeg.paymentDate", "farLeg.paymentDate");
    if (!nearLeg.getBaseCurrencyAmount().getCurrency().equals(farLeg.getBaseCurrencyAmount().getCurrency()) ||
        !nearLeg.getCounterCurrencyAmount().getCurrency().equals(farLeg.getCounterCurrencyAmount().getCurrency())) {
      throw new IllegalArgumentException("Legs must have the same currency pair");
    }
    if (signum(nearLeg.getBaseCurrencyAmount().getAmount()) == signum(farLeg.getBaseCurrencyAmount().getAmount())) {
      throw new IllegalArgumentException("Legs must have payments flowing in opposite directions");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this FX swap into {@code ExpandedFxSwap}.
   * 
   * @return the expanded FX swap
   */
  @Override
  public ExpandedFxSwap expand() {
    return ExpandedFxSwap.of(nearLeg.expand(), farLeg.expand());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxSwap}.
   * @return the meta-bean, not null
   */
  public static FxSwap.Meta meta() {
    return FxSwap.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxSwap.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxSwap(
      FxSingle nearLeg,
      FxSingle farLeg) {
    JodaBeanUtils.notNull(nearLeg, "nearLeg");
    JodaBeanUtils.notNull(farLeg, "farLeg");
    this.nearLeg = nearLeg;
    this.farLeg = farLeg;
    validate();
  }

  @Override
  public FxSwap.Meta metaBean() {
    return FxSwap.Meta.INSTANCE;
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
   * Gets the foreign exchange transaction at the earlier date.
   * <p>
   * This provides details of a single foreign exchange at a specific date.
   * The payment date of this transaction must be before that of the far leg.
   * @return the value of the property, not null
   */
  public FxSingle getNearLeg() {
    return nearLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the foreign exchange transaction at the later date.
   * <p>
   * This provides details of a single foreign exchange at a specific date.
   * The payment date of this transaction must be after that of the near leg.
   * @return the value of the property, not null
   */
  public FxSingle getFarLeg() {
    return farLeg;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxSwap other = (FxSwap) obj;
      return JodaBeanUtils.equal(getNearLeg(), other.getNearLeg()) &&
          JodaBeanUtils.equal(getFarLeg(), other.getFarLeg());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getNearLeg());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFarLeg());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("FxSwap{");
    buf.append("nearLeg").append('=').append(getNearLeg()).append(',').append(' ');
    buf.append("farLeg").append('=').append(JodaBeanUtils.toString(getFarLeg()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxSwap}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code nearLeg} property.
     */
    private final MetaProperty<FxSingle> nearLeg = DirectMetaProperty.ofImmutable(
        this, "nearLeg", FxSwap.class, FxSingle.class);
    /**
     * The meta-property for the {@code farLeg} property.
     */
    private final MetaProperty<FxSingle> farLeg = DirectMetaProperty.ofImmutable(
        this, "farLeg", FxSwap.class, FxSingle.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "nearLeg",
        "farLeg");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1825755334:  // nearLeg
          return nearLeg;
        case -1281739913:  // farLeg
          return farLeg;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxSwap> builder() {
      return new FxSwap.Builder();
    }

    @Override
    public Class<? extends FxSwap> beanType() {
      return FxSwap.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code nearLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxSingle> nearLeg() {
      return nearLeg;
    }

    /**
     * The meta-property for the {@code farLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxSingle> farLeg() {
      return farLeg;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1825755334:  // nearLeg
          return ((FxSwap) bean).getNearLeg();
        case -1281739913:  // farLeg
          return ((FxSwap) bean).getFarLeg();
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
   * The bean-builder for {@code FxSwap}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<FxSwap> {

    private FxSingle nearLeg;
    private FxSingle farLeg;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1825755334:  // nearLeg
          return nearLeg;
        case -1281739913:  // farLeg
          return farLeg;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1825755334:  // nearLeg
          this.nearLeg = (FxSingle) newValue;
          break;
        case -1281739913:  // farLeg
          this.farLeg = (FxSingle) newValue;
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
    public FxSwap build() {
      return new FxSwap(
          nearLeg,
          farLeg);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("FxSwap.Builder{");
      buf.append("nearLeg").append('=').append(JodaBeanUtils.toString(nearLeg)).append(',').append(' ');
      buf.append("farLeg").append('=').append(JodaBeanUtils.toString(farLeg));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
