/**
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
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;

/**
 * An Ibor cap/floor, resolved for pricing.
 * <p>
 * This is the resolved form of {@link IborCapFloor} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedIborCapFloor} from a {@code IborCapFloor}
 * using {@link IborCapFloor#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedIborCapFloor} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition(builderScope = "private")
public final class ResolvedIborCapFloor
    implements ResolvedProduct, ImmutableBean, Serializable {

  /**
   * The Ibor cap/floor leg of the product.
   * <p>
   * This is associated with periodic payments based on Ibor rate.
   * The payments are Ibor caplets or Ibor floorlets.
   */
  @PropertyDefinition(validate = "notNull")
  private final ResolvedIborCapFloorLeg capFloorLeg;
  /**
   * The optional pay leg of the product.
   * <p>
   * These periodic payments are not made for typical cap/floor products. Instead the premium is paid upfront.
   */
  @PropertyDefinition(get = "optional")
  private final ResolvedSwapLeg payLeg;
  /**
   * The set of currencies.
   */
  private final transient ImmutableSet<Currency> currencies;  // not a property, derived and cached from input data
  /**
   * The set of indices.
   */
  private final transient ImmutableSet<Index> indices;  // not a property, derived and cached from input data

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a cap/floor leg with no pay leg.
   * <p>
   * The pay leg is absent in the resulting cap/floor.
   * 
   * @param capFloorLeg  the cap/floor leg
   * @return the cap/floor
   */
  public static ResolvedIborCapFloor of(ResolvedIborCapFloorLeg capFloorLeg) {
    ArgChecker.notNull(capFloorLeg, "capFloorLeg");
    return new ResolvedIborCapFloor(capFloorLeg, null);
  }

  /**
   * Obtains an instance from a cap/floor leg and a pay leg.
   * 
   * @param capFloorLeg  the cap/floor leg
   * @param payLeg  the pay leg
   * @return the cap/floor
   */
  public static ResolvedIborCapFloor of(ResolvedIborCapFloorLeg capFloorLeg, ResolvedSwapLeg payLeg) {
    ArgChecker.notNull(capFloorLeg, "capFloorLeg");
    ArgChecker.notNull(payLeg, "payLeg");
    return new ResolvedIborCapFloor(capFloorLeg, payLeg);
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private ResolvedIborCapFloor(ResolvedIborCapFloorLeg capFloorLeg, ResolvedSwapLeg payLeg) {
    JodaBeanUtils.notNull(capFloorLeg, "capFloorLeg");
    if (payLeg != null) {
      ArgChecker.isFalse(
          payLeg.getPayReceive().equals(capFloorLeg.getPayReceive()),
          "Legs must have different Pay/Receive flag, but both were {}", payLeg.getPayReceive());
    }
    this.capFloorLeg = capFloorLeg;
    this.payLeg = payLeg;
    this.currencies = buildCurrencies(capFloorLeg, payLeg);
    this.indices = buildIndices(capFloorLeg, payLeg);
  }

  // collect the set of currencies
  private static ImmutableSet<Currency> buildCurrencies(ResolvedIborCapFloorLeg capFloorLeg, ResolvedSwapLeg payLeg) {
    ImmutableSet.Builder<Currency> builder = ImmutableSet.builder();
    builder.add(capFloorLeg.getCurrency());
    if (payLeg != null) {
      builder.add(payLeg.getCurrency());
    }
    return builder.build();
  }

  // collect the set of indices
  private static ImmutableSet<Index> buildIndices(ResolvedIborCapFloorLeg capFloorLeg, ResolvedSwapLeg payLeg) {
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    builder.add(capFloorLeg.getIndex());
    if (payLeg != null) {
      payLeg.collectIndices(builder);
    }
    return builder.build();
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new ResolvedIborCapFloor(capFloorLeg, payLeg);
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
    return currencies;
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
    return indices;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedIborCapFloor}.
   * @return the meta-bean, not null
   */
  public static ResolvedIborCapFloor.Meta meta() {
    return ResolvedIborCapFloor.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedIborCapFloor.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public ResolvedIborCapFloor.Meta metaBean() {
    return ResolvedIborCapFloor.Meta.INSTANCE;
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
  public ResolvedIborCapFloorLeg getCapFloorLeg() {
    return capFloorLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional pay leg of the product.
   * <p>
   * These periodic payments are not made for typical cap/floor products. Instead the premium is paid upfront.
   * @return the optional value of the property, not null
   */
  public Optional<ResolvedSwapLeg> getPayLeg() {
    return Optional.ofNullable(payLeg);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ResolvedIborCapFloor other = (ResolvedIborCapFloor) obj;
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
    buf.append("ResolvedIborCapFloor{");
    buf.append("capFloorLeg").append('=').append(capFloorLeg).append(',').append(' ');
    buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedIborCapFloor}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code capFloorLeg} property.
     */
    private final MetaProperty<ResolvedIborCapFloorLeg> capFloorLeg = DirectMetaProperty.ofImmutable(
        this, "capFloorLeg", ResolvedIborCapFloor.class, ResolvedIborCapFloorLeg.class);
    /**
     * The meta-property for the {@code payLeg} property.
     */
    private final MetaProperty<ResolvedSwapLeg> payLeg = DirectMetaProperty.ofImmutable(
        this, "payLeg", ResolvedIborCapFloor.class, ResolvedSwapLeg.class);
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
    public BeanBuilder<? extends ResolvedIborCapFloor> builder() {
      return new ResolvedIborCapFloor.Builder();
    }

    @Override
    public Class<? extends ResolvedIborCapFloor> beanType() {
      return ResolvedIborCapFloor.class;
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
    public MetaProperty<ResolvedIborCapFloorLeg> capFloorLeg() {
      return capFloorLeg;
    }

    /**
     * The meta-property for the {@code payLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ResolvedSwapLeg> payLeg() {
      return payLeg;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 2124672084:  // capFloorLeg
          return ((ResolvedIborCapFloor) bean).getCapFloorLeg();
        case -995239866:  // payLeg
          return ((ResolvedIborCapFloor) bean).payLeg;
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
   * The bean-builder for {@code ResolvedIborCapFloor}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ResolvedIborCapFloor> {

    private ResolvedIborCapFloorLeg capFloorLeg;
    private ResolvedSwapLeg payLeg;

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
          this.capFloorLeg = (ResolvedIborCapFloorLeg) newValue;
          break;
        case -995239866:  // payLeg
          this.payLeg = (ResolvedSwapLeg) newValue;
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
    public ResolvedIborCapFloor build() {
      return new ResolvedIborCapFloor(
          capFloorLeg,
          payLeg);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ResolvedIborCapFloor.Builder{");
      buf.append("capFloorLeg").append('=').append(JodaBeanUtils.toString(capFloorLeg)).append(',').append(' ');
      buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
