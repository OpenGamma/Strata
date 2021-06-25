/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.fx.FxOptionProduct;
import com.opengamma.strata.product.option.Barrier;

/**
 * FX (European) single barrier option.
 * <p>
 * An FX option is a financial instrument that provides an option to exchange two currencies at a specified future time
 * only when barrier event occurs (knock-in option) or does not occur (knock-out option).
 * <p>
 * Depending on the barrier defined in {@link Barrier}, the options are classified into four types: up-and-in,
 * down-and-in, up-and-out and down-and-out.
 * <p>
 * For example, an up-and-out call on a 'EUR 1.00 / USD -1.41' exchange with barrier of 1.5 is the option to
 * perform a foreign exchange on the expiry date, where USD 1.41 is paid to receive EUR 1.00, only when EUR/USD rate does
 * not exceed 1.5 during the barrier event observation period.
 * <p>
 * In case of the occurrence (non-occurrence for knock-in options) of the barrier event, the option becomes worthless,
 * or alternatively, a rebate is made.
 */
@BeanDefinition
public final class FxSingleBarrierOption
    implements FxOptionProduct, Resolvable<ResolvedFxSingleBarrierOption>, ImmutableBean, Serializable {

  /**
   * The underlying FX vanilla option.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxVanillaOption underlyingOption;
  /**
   * The barrier description.
   * <p>
   * The barrier level stored in this field must be represented based on the direction of the currency pair in the
   * underlying FX transaction.
   * <p>
   * For example, if the underlying option is an option on EUR/GBP, the barrier should be a certain level of EUR/GBP rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final Barrier barrier;
  /**
   * For a 'out' option, the amount is paid when the barrier is reached;
   * for a 'in' option, the amount is paid at expiry if the barrier is not reached.
   * <p>
   * This is the notional amount represented in one of the currency pair.
   * The amount should be positive.
   */
  @PropertyDefinition(get = "optional")
  private final CurrencyAmount rebate;

  //-------------------------------------------------------------------------
  /**
   * Obtains FX single barrier option with rebate.
   *
   * @param underlyingOption  the underlying FX vanilla option
   * @param barrier  the barrier
   * @param rebate  the rebate
   * @return the instance
   */
  public static FxSingleBarrierOption of(FxVanillaOption underlyingOption, Barrier barrier, CurrencyAmount rebate) {
    return new FxSingleBarrierOption(underlyingOption, barrier, rebate);
  }

  /**
   * Obtains FX single barrier option without rebate.
   *
   * @param underlyingOption  the underlying FX vanilla option
   * @param barrier  the barrier
   * @return the instance
   */
  public static FxSingleBarrierOption of(FxVanillaOption underlyingOption, Barrier barrier) {
    return new FxSingleBarrierOption(underlyingOption, barrier, null);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (rebate != null) {
      ArgChecker.isTrue(rebate.getAmount() > 0d, "rebate must be positive");
      ArgChecker.isTrue(underlyingOption.getUnderlying().getCurrencyPair().contains(rebate.getCurrency()),
          "The rebate currency must be one of underlying currency pair");
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
    return underlyingOption.getCurrencyPair();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying Fx vanilla option's expiry.
   * @return the expiry
   */
  @Override
  public ZonedDateTime getExpiry() {
    return underlyingOption.getExpiry();
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedFxSingleBarrierOption resolve(ReferenceData refData) {
    if (rebate != null) {
      return ResolvedFxSingleBarrierOption.of(underlyingOption.resolve(refData), barrier, rebate);
    }
    return ResolvedFxSingleBarrierOption.of(underlyingOption.resolve(refData), barrier);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FxSingleBarrierOption}.
   * @return the meta-bean, not null
   */
  public static FxSingleBarrierOption.Meta meta() {
    return FxSingleBarrierOption.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FxSingleBarrierOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxSingleBarrierOption.Builder builder() {
    return new FxSingleBarrierOption.Builder();
  }

  private FxSingleBarrierOption(
      FxVanillaOption underlyingOption,
      Barrier barrier,
      CurrencyAmount rebate) {
    JodaBeanUtils.notNull(underlyingOption, "underlyingOption");
    JodaBeanUtils.notNull(barrier, "barrier");
    this.underlyingOption = underlyingOption;
    this.barrier = barrier;
    this.rebate = rebate;
    validate();
  }

  @Override
  public FxSingleBarrierOption.Meta metaBean() {
    return FxSingleBarrierOption.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying FX vanilla option.
   * @return the value of the property, not null
   */
  public FxVanillaOption getUnderlyingOption() {
    return underlyingOption;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the barrier description.
   * <p>
   * The barrier level stored in this field must be represented based on the direction of the currency pair in the
   * underlying FX transaction.
   * <p>
   * For example, if the underlying option is an option on EUR/GBP, the barrier should be a certain level of EUR/GBP rate.
   * @return the value of the property, not null
   */
  public Barrier getBarrier() {
    return barrier;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets for a 'out' option, the amount is paid when the barrier is reached;
   * for a 'in' option, the amount is paid at expiry if the barrier is not reached.
   * <p>
   * This is the notional amount represented in one of the currency pair.
   * The amount should be positive.
   * @return the optional value of the property, not null
   */
  public Optional<CurrencyAmount> getRebate() {
    return Optional.ofNullable(rebate);
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
      FxSingleBarrierOption other = (FxSingleBarrierOption) obj;
      return JodaBeanUtils.equal(underlyingOption, other.underlyingOption) &&
          JodaBeanUtils.equal(barrier, other.barrier) &&
          JodaBeanUtils.equal(rebate, other.rebate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingOption);
    hash = hash * 31 + JodaBeanUtils.hashCode(barrier);
    hash = hash * 31 + JodaBeanUtils.hashCode(rebate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("FxSingleBarrierOption{");
    buf.append("underlyingOption").append('=').append(JodaBeanUtils.toString(underlyingOption)).append(',').append(' ');
    buf.append("barrier").append('=').append(JodaBeanUtils.toString(barrier)).append(',').append(' ');
    buf.append("rebate").append('=').append(JodaBeanUtils.toString(rebate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxSingleBarrierOption}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code underlyingOption} property.
     */
    private final MetaProperty<FxVanillaOption> underlyingOption = DirectMetaProperty.ofImmutable(
        this, "underlyingOption", FxSingleBarrierOption.class, FxVanillaOption.class);
    /**
     * The meta-property for the {@code barrier} property.
     */
    private final MetaProperty<Barrier> barrier = DirectMetaProperty.ofImmutable(
        this, "barrier", FxSingleBarrierOption.class, Barrier.class);
    /**
     * The meta-property for the {@code rebate} property.
     */
    private final MetaProperty<CurrencyAmount> rebate = DirectMetaProperty.ofImmutable(
        this, "rebate", FxSingleBarrierOption.class, CurrencyAmount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "underlyingOption",
        "barrier",
        "rebate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 87556658:  // underlyingOption
          return underlyingOption;
        case -333143113:  // barrier
          return barrier;
        case -934952029:  // rebate
          return rebate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxSingleBarrierOption.Builder builder() {
      return new FxSingleBarrierOption.Builder();
    }

    @Override
    public Class<? extends FxSingleBarrierOption> beanType() {
      return FxSingleBarrierOption.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code underlyingOption} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxVanillaOption> underlyingOption() {
      return underlyingOption;
    }

    /**
     * The meta-property for the {@code barrier} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Barrier> barrier() {
      return barrier;
    }

    /**
     * The meta-property for the {@code rebate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> rebate() {
      return rebate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 87556658:  // underlyingOption
          return ((FxSingleBarrierOption) bean).getUnderlyingOption();
        case -333143113:  // barrier
          return ((FxSingleBarrierOption) bean).getBarrier();
        case -934952029:  // rebate
          return ((FxSingleBarrierOption) bean).rebate;
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
   * The bean-builder for {@code FxSingleBarrierOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxSingleBarrierOption> {

    private FxVanillaOption underlyingOption;
    private Barrier barrier;
    private CurrencyAmount rebate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FxSingleBarrierOption beanToCopy) {
      this.underlyingOption = beanToCopy.getUnderlyingOption();
      this.barrier = beanToCopy.getBarrier();
      this.rebate = beanToCopy.rebate;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 87556658:  // underlyingOption
          return underlyingOption;
        case -333143113:  // barrier
          return barrier;
        case -934952029:  // rebate
          return rebate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 87556658:  // underlyingOption
          this.underlyingOption = (FxVanillaOption) newValue;
          break;
        case -333143113:  // barrier
          this.barrier = (Barrier) newValue;
          break;
        case -934952029:  // rebate
          this.rebate = (CurrencyAmount) newValue;
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
    public FxSingleBarrierOption build() {
      return new FxSingleBarrierOption(
          underlyingOption,
          barrier,
          rebate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the underlying FX vanilla option.
     * @param underlyingOption  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlyingOption(FxVanillaOption underlyingOption) {
      JodaBeanUtils.notNull(underlyingOption, "underlyingOption");
      this.underlyingOption = underlyingOption;
      return this;
    }

    /**
     * Sets the barrier description.
     * <p>
     * The barrier level stored in this field must be represented based on the direction of the currency pair in the
     * underlying FX transaction.
     * <p>
     * For example, if the underlying option is an option on EUR/GBP, the barrier should be a certain level of EUR/GBP rate.
     * @param barrier  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder barrier(Barrier barrier) {
      JodaBeanUtils.notNull(barrier, "barrier");
      this.barrier = barrier;
      return this;
    }

    /**
     * Sets for a 'out' option, the amount is paid when the barrier is reached;
     * for a 'in' option, the amount is paid at expiry if the barrier is not reached.
     * <p>
     * This is the notional amount represented in one of the currency pair.
     * The amount should be positive.
     * @param rebate  the new value
     * @return this, for chaining, not null
     */
    public Builder rebate(CurrencyAmount rebate) {
      this.rebate = rebate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("FxSingleBarrierOption.Builder{");
      buf.append("underlyingOption").append('=').append(JodaBeanUtils.toString(underlyingOption)).append(',').append(' ');
      buf.append("barrier").append('=').append(JodaBeanUtils.toString(barrier)).append(',').append(' ');
      buf.append("rebate").append('=').append(JodaBeanUtils.toString(rebate));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
