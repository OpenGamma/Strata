/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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
 * An Ibor cap/floor product.
 * <p>
 * The Ibor cap/floor product consists of two legs, a cap/floor leg and a pay leg.
 * The cap/floor leg involves a set of call/put options on successive Ibor index rates,
 * known as Ibor caplets/floorlets.
 * The pay leg is any swap leg from a standard interest rate swap. The pay leg is absent for typical
 * Ibor cap/floor products, with the premium paid upfront instead, as defined in {@link IborCapFloorTrade}.
 */
@BeanDefinition(builderScope = "private")
public final class IborCapFloor
    implements Product, Resolvable<ResolvedIborCapFloor>, ImmutableBean, Serializable {

  /**
   * The Ibor cap/floor leg of the product.
   * <p>
   * This is associated with periodic payments based on Ibor rate.
   * The payments are Ibor caplets or Ibor floorlets.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborCapFloorLeg capFloorLeg;
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
   * @param capFloorLeg  the cap/floor leg
   * @return the cap/floor
   */
  public static IborCapFloor of(IborCapFloorLeg capFloorLeg) {
    return new IborCapFloor(capFloorLeg, null);
  }

  /**
   * Obtains an instance from a cap/floor leg and a pay leg.
   * 
   * @param capFloorLeg  the cap/floor leg
   * @param payLeg  the pay leg
   * @return the cap/floor
   */
  public static IborCapFloor of(IborCapFloorLeg capFloorLeg, SwapLeg payLeg) {
    return new IborCapFloor(capFloorLeg, payLeg);
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
  /**
   * Returns the set of payment currencies referred to by the cap/floor.
   * <p>
   * This returns the complete set of payment currencies for the cap/floor.
   * This will typically return one currency, but could return two.
   * 
   * @return the set of payment currencies referred to by this swap
   */
  public ImmutableSet<Currency> allPaymentCurrencies() {
    ImmutableSet.Builder<Currency> builder = ImmutableSet.builder();
    builder.add(capFloorLeg.getCurrency());
    if (payLeg != null) {
      builder.add(payLeg.getCurrency());
    }
    return builder.build();
  }

  /**
   * Returns the set of indices referred to by the cap/floor.
   * <p>
   * A cap/floor will typically refer to one index, such as 'GBP-LIBOR-3M'.
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
  public ResolvedIborCapFloor resolve(ReferenceData refData) {
    if (payLeg == null) {
      return ResolvedIborCapFloor.of(capFloorLeg.resolve(refData));
    }
    return ResolvedIborCapFloor.of(capFloorLeg.resolve(refData), payLeg.resolve(refData));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborCapFloor}.
   * @return the meta-bean, not null
   */
  public static IborCapFloor.Meta meta() {
    return IborCapFloor.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborCapFloor.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IborCapFloor(
      IborCapFloorLeg capFloorLeg,
      SwapLeg payLeg) {
    JodaBeanUtils.notNull(capFloorLeg, "capFloorLeg");
    this.capFloorLeg = capFloorLeg;
    this.payLeg = payLeg;
    validate();
  }

  @Override
  public IborCapFloor.Meta metaBean() {
    return IborCapFloor.Meta.INSTANCE;
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
   * Gets the Ibor cap/floor leg of the product.
   * <p>
   * This is associated with periodic payments based on Ibor rate.
   * The payments are Ibor caplets or Ibor floorlets.
   * @return the value of the property, not null
   */
  public IborCapFloorLeg getCapFloorLeg() {
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
      IborCapFloor other = (IborCapFloor) obj;
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
    buf.append("IborCapFloor{");
    buf.append("capFloorLeg").append('=').append(capFloorLeg).append(',').append(' ');
    buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborCapFloor}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code capFloorLeg} property.
     */
    private final MetaProperty<IborCapFloorLeg> capFloorLeg = DirectMetaProperty.ofImmutable(
        this, "capFloorLeg", IborCapFloor.class, IborCapFloorLeg.class);
    /**
     * The meta-property for the {@code payLeg} property.
     */
    private final MetaProperty<SwapLeg> payLeg = DirectMetaProperty.ofImmutable(
        this, "payLeg", IborCapFloor.class, SwapLeg.class);
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
    public BeanBuilder<? extends IborCapFloor> builder() {
      return new IborCapFloor.Builder();
    }

    @Override
    public Class<? extends IborCapFloor> beanType() {
      return IborCapFloor.class;
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
    public MetaProperty<IborCapFloorLeg> capFloorLeg() {
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
          return ((IborCapFloor) bean).getCapFloorLeg();
        case -995239866:  // payLeg
          return ((IborCapFloor) bean).payLeg;
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
   * The bean-builder for {@code IborCapFloor}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<IborCapFloor> {

    private IborCapFloorLeg capFloorLeg;
    private SwapLeg payLeg;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
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
          this.capFloorLeg = (IborCapFloorLeg) newValue;
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
    public IborCapFloor build() {
      return new IborCapFloor(
          capFloorLeg,
          payLeg);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("IborCapFloor.Builder{");
      buf.append("capFloorLeg").append('=').append(JodaBeanUtils.toString(capFloorLeg)).append(',').append(' ');
      buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
