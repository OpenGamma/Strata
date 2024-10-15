/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import java.io.Serializable;
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
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * An overnight rate in arrears cap/floor product.
 * <p>
 * The overnight rate in arrears cap/floor product consists of two legs, a cap/floor leg and a pay leg.
 * The cap/floor leg involves a set of call/put options on successive compounded overnight index rates.
 * The pay leg is any swap leg from a standard interest rate swap.
 * The pay leg is absent for typical overnight rate in arrears cap/floor products,
 * with the premium paid upfront instead, as defined in {@link OvernightInArrearsCapFloorTrade}.
 */
@BeanDefinition(builderScope = "private")
public final class OvernightInArrearsCapFloor
    implements Product, Resolvable<ResolvedOvernightInArrearsCapFloor>, ImmutableBean, Serializable {

  /**
   * The cap/floor leg of the product.
   * <p>
   * This is associated with periodic payments based on overnight index rate.
   * The payments are caplets or floorlets.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightInArrearsCapFloorLeg capFloorLeg;
  /**
   * The optional pay leg of the product.
   * <p>
   * These periodic payments are not made for typical cap/floor products.
   * Instead the premium is paid upfront.
   */
  @PropertyDefinition(get = "optional")
  private final SwapLeg payLeg;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a cap/floor leg with no pay leg.
   * <p>
   * The pay leg is absent in the resulting cap/floor.
   *
   * @param capFloorLeg the cap/floor leg
   * @return the cap/floor
   */
  public static OvernightInArrearsCapFloor of(OvernightInArrearsCapFloorLeg capFloorLeg) {
    return new OvernightInArrearsCapFloor(capFloorLeg, null);
  }

  /**
   * Obtains an instance from a cap/floor leg and a pay leg.
   *
   * @param capFloorLeg the cap/floor leg
   * @param payLeg the pay leg
   * @return the cap/floor
   */
  public static OvernightInArrearsCapFloor of(OvernightInArrearsCapFloorLeg capFloorLeg, SwapLeg payLeg) {
    return new OvernightInArrearsCapFloor(capFloorLeg, payLeg);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (payLeg != null) {
      ArgChecker.isFalse(
          payLeg.getPayReceive().equals(capFloorLeg.getPayReceive()),
          "Legs must have different Pay/Receive flag, but both were {}", payLeg.getPayReceive());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<Currency> allPaymentCurrencies() {
    if (payLeg == null) {
      return ImmutableSet.of(capFloorLeg.getCurrency());
    }
    return ImmutableSet.of(capFloorLeg.getCurrency(), payLeg.getCurrency());
  }

  @Override
  public ImmutableSet<Currency> allCurrencies() {
    if (payLeg == null) {
      return ImmutableSet.of(capFloorLeg.getCurrency());
    }
    ImmutableSet.Builder<Currency> builder = ImmutableSet.builder();
    builder.add(capFloorLeg.getCurrency());
    builder.addAll(payLeg.allCurrencies());
    return builder.build();
  }

  /**
   * Returns the set of indices referred to by the cap/floor.
   * <p>
   * A cap/floor will typically refer to one index, such as 'GBP-SONIA'.
   * Calling this method will return the complete list of indices.
   *
   * @return the set of indices referred to by this cap/floor
   */
  public ImmutableSet<Index> allIndices() {
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    builder.add(capFloorLeg.getCalculation().getIndex());
    if (payLeg != null) {
      payLeg.collectIndices(builder);
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedOvernightInArrearsCapFloor resolve(ReferenceData refData) {
    if (payLeg == null) {
      return ResolvedOvernightInArrearsCapFloor.of(capFloorLeg.resolve(refData));
    }
    return ResolvedOvernightInArrearsCapFloor.of(capFloorLeg.resolve(refData), payLeg.resolve(refData));
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapFloor}.
   * @return the meta-bean, not null
   */
  public static OvernightInArrearsCapFloor.Meta meta() {
    return OvernightInArrearsCapFloor.Meta.INSTANCE;
  }

  static {
    MetaBean.register(OvernightInArrearsCapFloor.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private OvernightInArrearsCapFloor(
      OvernightInArrearsCapFloorLeg capFloorLeg,
      SwapLeg payLeg) {
    JodaBeanUtils.notNull(capFloorLeg, "capFloorLeg");
    this.capFloorLeg = capFloorLeg;
    this.payLeg = payLeg;
    validate();
  }

  @Override
  public OvernightInArrearsCapFloor.Meta metaBean() {
    return OvernightInArrearsCapFloor.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the cap/floor leg of the product.
   * <p>
   * This is associated with periodic payments based on overnight index rate.
   * The payments are caplets or floorlets.
   * @return the value of the property, not null
   */
  public OvernightInArrearsCapFloorLeg getCapFloorLeg() {
    return capFloorLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional pay leg of the product.
   * <p>
   * These periodic payments are not made for typical cap/floor products.
   * Instead the premium is paid upfront.
   * @return the optional value of the property, not null
   */
  public Optional<SwapLeg> getPayLeg() {
    return Optional.ofNullable(payLeg);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      OvernightInArrearsCapFloor other = (OvernightInArrearsCapFloor) obj;
      return JodaBeanUtils.equal(capFloorLeg, other.capFloorLeg) &&
          JodaBeanUtils.equal(payLeg, other.payLeg);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(capFloorLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(payLeg);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("OvernightInArrearsCapFloor{");
    buf.append("capFloorLeg").append('=').append(JodaBeanUtils.toString(capFloorLeg)).append(',').append(' ');
    buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapFloor}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code capFloorLeg} property.
     */
    private final MetaProperty<OvernightInArrearsCapFloorLeg> capFloorLeg = DirectMetaProperty.ofImmutable(
        this, "capFloorLeg", OvernightInArrearsCapFloor.class, OvernightInArrearsCapFloorLeg.class);
    /**
     * The meta-property for the {@code payLeg} property.
     */
    private final MetaProperty<SwapLeg> payLeg = DirectMetaProperty.ofImmutable(
        this, "payLeg", OvernightInArrearsCapFloor.class, SwapLeg.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "capFloorLeg",
        "payLeg");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2124672084:  // capFloorLeg
          return capFloorLeg;
        case -995239866:  // payLeg
          return payLeg;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends OvernightInArrearsCapFloor> builder() {
      return new OvernightInArrearsCapFloor.Builder();
    }

    @Override
    public Class<? extends OvernightInArrearsCapFloor> beanType() {
      return OvernightInArrearsCapFloor.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code capFloorLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightInArrearsCapFloorLeg> capFloorLeg() {
      return capFloorLeg;
    }

    /**
     * The meta-property for the {@code payLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwapLeg> payLeg() {
      return payLeg;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 2124672084:  // capFloorLeg
          return ((OvernightInArrearsCapFloor) bean).getCapFloorLeg();
        case -995239866:  // payLeg
          return ((OvernightInArrearsCapFloor) bean).payLeg;
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
   * The bean-builder for {@code OvernightInArrearsCapFloor}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<OvernightInArrearsCapFloor> {

    private OvernightInArrearsCapFloorLeg capFloorLeg;
    private SwapLeg payLeg;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2124672084:  // capFloorLeg
          return capFloorLeg;
        case -995239866:  // payLeg
          return payLeg;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 2124672084:  // capFloorLeg
          this.capFloorLeg = (OvernightInArrearsCapFloorLeg) newValue;
          break;
        case -995239866:  // payLeg
          this.payLeg = (SwapLeg) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public OvernightInArrearsCapFloor build() {
      return new OvernightInArrearsCapFloor(
          capFloorLeg,
          payLeg);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("OvernightInArrearsCapFloor.Builder{");
      buf.append("capFloorLeg").append('=').append(JodaBeanUtils.toString(capFloorLeg)).append(',').append(' ');
      buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
