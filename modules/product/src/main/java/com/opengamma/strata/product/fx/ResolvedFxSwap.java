/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvedProduct;

/**
 * An FX Swap, resolved for pricing.
 * <p>
 * This is the resolved form of {@link FxSwap} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedFxSwap} from a {@code FxSwap}
 * using {@link FxSwap#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedFxSwap} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition(builderScope = "private")
public final class ResolvedFxSwap
    implements ResolvedProduct, ImmutableBean, Serializable {

  /**
   * The foreign exchange transaction at the earlier date.
   * <p>
   * This provides details of a single foreign exchange at a specific date.
   * The payment date of this transaction must be before that of the far leg.
   */
  @PropertyDefinition(validate = "notNull")
  private final ResolvedFxSingle nearLeg;
  /**
   * The foreign exchange transaction at the later date.
   * <p>
   * This provides details of a single foreign exchange at a specific date.
   * The payment date of this transaction must be after that of the near leg.
   */
  @PropertyDefinition(validate = "notNull")
  private final ResolvedFxSingle farLeg;

  //-------------------------------------------------------------------------
  /**
   * Creates a {@code ResolvedFxSwap} from two legs.
   * <p>
   * The transactions must be passed in with payment dates in the correct order.
   * The currency pair of each leg must match and have amounts flowing in opposite directions.
   * 
   * @param nearLeg  the earlier leg
   * @param farLeg  the later leg
   * @return the resolved FX swap
   */
  public static ResolvedFxSwap of(ResolvedFxSingle nearLeg, ResolvedFxSingle farLeg) {
    return new ResolvedFxSwap(nearLeg, farLeg);
  }

  /**
   * Creates a {@code ResolvedFxSwap} using forward points.
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
   * @return the resolved FX swap
   */
  public static ResolvedFxSwap ofForwardPoints(
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
    ResolvedFxSingle nearLeg = ResolvedFxSingle.of(amountCurrency1, FxRate.of(currency1, currency2, nearFxRate), nearDate);
    ResolvedFxSingle farLeg = ResolvedFxSingle.of(amountCurrency1.negated(), FxRate.of(currency1, currency2, farFxRate), farDate);
    return of(nearLeg, farLeg);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(
        nearLeg.getPaymentDate(), farLeg.getPaymentDate(), "nearLeg.paymentDate", "farLeg.paymentDate");
    if (!nearLeg.getBaseCurrencyPayment().getCurrency().equals(farLeg.getBaseCurrencyPayment().getCurrency()) ||
        !nearLeg.getCounterCurrencyPayment().getCurrency().equals(farLeg.getCounterCurrencyPayment().getCurrency())) {
      throw new IllegalArgumentException("Legs must have the same currency pair");
    }
    if (signum(nearLeg.getBaseCurrencyPayment().getAmount()) == signum(farLeg.getBaseCurrencyPayment().getAmount())) {
      throw new IllegalArgumentException("Legs must have payments flowing in opposite directions");
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedFxSwap}.
   * @return the meta-bean, not null
   */
  public static ResolvedFxSwap.Meta meta() {
    return ResolvedFxSwap.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedFxSwap.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ResolvedFxSwap(
      ResolvedFxSingle nearLeg,
      ResolvedFxSingle farLeg) {
    JodaBeanUtils.notNull(nearLeg, "nearLeg");
    JodaBeanUtils.notNull(farLeg, "farLeg");
    this.nearLeg = nearLeg;
    this.farLeg = farLeg;
    validate();
  }

  @Override
  public ResolvedFxSwap.Meta metaBean() {
    return ResolvedFxSwap.Meta.INSTANCE;
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
  public ResolvedFxSingle getNearLeg() {
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
  public ResolvedFxSingle getFarLeg() {
    return farLeg;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ResolvedFxSwap other = (ResolvedFxSwap) obj;
      return JodaBeanUtils.equal(nearLeg, other.nearLeg) &&
          JodaBeanUtils.equal(farLeg, other.farLeg);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(nearLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(farLeg);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ResolvedFxSwap{");
    buf.append("nearLeg").append('=').append(nearLeg).append(',').append(' ');
    buf.append("farLeg").append('=').append(JodaBeanUtils.toString(farLeg));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedFxSwap}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code nearLeg} property.
     */
    private final MetaProperty<ResolvedFxSingle> nearLeg = DirectMetaProperty.ofImmutable(
        this, "nearLeg", ResolvedFxSwap.class, ResolvedFxSingle.class);
    /**
     * The meta-property for the {@code farLeg} property.
     */
    private final MetaProperty<ResolvedFxSingle> farLeg = DirectMetaProperty.ofImmutable(
        this, "farLeg", ResolvedFxSwap.class, ResolvedFxSingle.class);
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
    public BeanBuilder<? extends ResolvedFxSwap> builder() {
      return new ResolvedFxSwap.Builder();
    }

    @Override
    public Class<? extends ResolvedFxSwap> beanType() {
      return ResolvedFxSwap.class;
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
    public MetaProperty<ResolvedFxSingle> nearLeg() {
      return nearLeg;
    }

    /**
     * The meta-property for the {@code farLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ResolvedFxSingle> farLeg() {
      return farLeg;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1825755334:  // nearLeg
          return ((ResolvedFxSwap) bean).getNearLeg();
        case -1281739913:  // farLeg
          return ((ResolvedFxSwap) bean).getFarLeg();
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
   * The bean-builder for {@code ResolvedFxSwap}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ResolvedFxSwap> {

    private ResolvedFxSingle nearLeg;
    private ResolvedFxSingle farLeg;

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
          this.nearLeg = (ResolvedFxSingle) newValue;
          break;
        case -1281739913:  // farLeg
          this.farLeg = (ResolvedFxSingle) newValue;
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
    public ResolvedFxSwap build() {
      return new ResolvedFxSwap(
          nearLeg,
          farLeg);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ResolvedFxSwap.Builder{");
      buf.append("nearLeg").append('=').append(JodaBeanUtils.toString(nearLeg)).append(',').append(' ');
      buf.append("farLeg").append('=').append(JodaBeanUtils.toString(farLeg));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
