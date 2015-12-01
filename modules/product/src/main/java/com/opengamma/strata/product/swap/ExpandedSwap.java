/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An expanded rate swap, with dates calculated ready for pricing.
 * <p>
 * A rate swap is a financial instrument that represents the exchange of streams of payments.
 * The swap is formed of legs, where each leg typically represents the obligations
 * of the seller or buyer of the swap. In the simplest vanilla interest rate swap,
 * there are two legs, one with a fixed rate and the other a floating rate.
 * Many other more complex swaps can also be represented.
 * <p>
 * This class defines a swap as a set of legs, each of which contains a list of payment periods.
 * Each payment period typically consists of one or more accrual periods.
 * Additional payment events may also be specified.
 * <p>
 * An {@code ExpandedSwap} contains information based on holiday calendars.
 * If a holiday calendar changes, the adjusted dates may no longer be correct.
 * Care must be taken when placing the expanded form in a cache or persistence layer.
 * <p>
 * Any combination of legs, payments and accrual periods is supported in the data model,
 * however there is no guarantee that exotic combinations will price sensibly.
 */
@BeanDefinition
public final class ExpandedSwap
    implements SwapProduct, ImmutableBean, Serializable {

  /**
   * The legs of the swap.
   * <p>
   * A swap consists of one or more legs.
   * The legs of a swap are essentially unordered, however it is more efficient
   * and closer to user expectation to treat them as being ordered.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<ExpandedSwapLeg> legs;
  /**
   * Whether the swap is cross currency or not.
   */
  private final boolean crossCurrency;  // not a property, derived and cached from input data

  //-------------------------------------------------------------------------
  /**
   * Creates a swap from one or more swap legs.
   * <p>
   * While most swaps have two legs, other combinations are possible.
   * 
   * @param legs  the array of legs
   * @return the swap
   */
  public static ExpandedSwap of(ExpandedSwapLeg... legs) {
    ArgChecker.notEmpty(legs, "legs");
    return new ExpandedSwap(ImmutableList.copyOf(legs));
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private ExpandedSwap(List<ExpandedSwapLeg> legs) {
    JodaBeanUtils.notEmpty(legs, "legs");
    this.legs = ImmutableList.copyOf(legs);
    this.crossCurrency = checkIfCrossCurrency(legs);
  }

  // profiling showed a hotspot when using streams, removed when using this approach
  private static boolean checkIfCrossCurrency(List<ExpandedSwapLeg> legs) {
    Iterator<ExpandedSwapLeg> it = legs.iterator();
    Currency currency = it.next().getCurrency();
    while (it.hasNext()) {
      if (!currency.equals(it.next().getCurrency())) {
        return true;
      }
    }
    return false;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this trade is cross-currency.
   * <p>
   * A cross currency swap is defined as one with legs in two different currencies.
   * 
   * @return true if cross currency
   */
  public boolean isCrossCurrency() {
    return crossCurrency;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the legs of the swap with the specified type.
   * <p>
   * This returns all the legs with the given type.
   * 
   * @param type  the type to find
   * @return the matching legs of the swap
   */
  public ImmutableList<ExpandedSwapLeg> getLegs(SwapLegType type) {
    return legs.stream().filter(leg -> leg.getType() == type).collect(toImmutableList());
  }

  /**
   * Gets the first pay or receive leg of the swap.
   * <p>
   * This returns the first pay or receive leg of the swap, empty if no matching leg.
   * 
   * @param payReceive  the pay or receive flag
   * @return the first matching leg of the swap
   */
  public Optional<ExpandedSwapLeg> getLeg(PayReceive payReceive) {
    return legs.stream().filter(leg -> leg.getPayReceive() == payReceive).findFirst();
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this swap, trivially returning {@code this}.
   * 
   * @return this swap
   */
  @Override
  public ExpandedSwap expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExpandedSwap}.
   * @return the meta-bean, not null
   */
  public static ExpandedSwap.Meta meta() {
    return ExpandedSwap.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExpandedSwap.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ExpandedSwap.Builder builder() {
    return new ExpandedSwap.Builder();
  }

  @Override
  public ExpandedSwap.Meta metaBean() {
    return ExpandedSwap.Meta.INSTANCE;
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
   * Gets the legs of the swap.
   * <p>
   * A swap consists of one or more legs.
   * The legs of a swap are essentially unordered, however it is more efficient
   * and closer to user expectation to treat them as being ordered.
   * @return the value of the property, not empty
   */
  public ImmutableList<ExpandedSwapLeg> getLegs() {
    return legs;
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
      ExpandedSwap other = (ExpandedSwap) obj;
      return JodaBeanUtils.equal(legs, other.legs);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(legs);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("ExpandedSwap{");
    buf.append("legs").append('=').append(JodaBeanUtils.toString(legs));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExpandedSwap}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code legs} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ExpandedSwapLeg>> legs = DirectMetaProperty.ofImmutable(
        this, "legs", ExpandedSwap.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "legs");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3317797:  // legs
          return legs;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ExpandedSwap.Builder builder() {
      return new ExpandedSwap.Builder();
    }

    @Override
    public Class<? extends ExpandedSwap> beanType() {
      return ExpandedSwap.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code legs} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ExpandedSwapLeg>> legs() {
      return legs;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3317797:  // legs
          return ((ExpandedSwap) bean).getLegs();
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
   * The bean-builder for {@code ExpandedSwap}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ExpandedSwap> {

    private List<ExpandedSwapLeg> legs = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ExpandedSwap beanToCopy) {
      this.legs = beanToCopy.getLegs();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3317797:  // legs
          return legs;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3317797:  // legs
          this.legs = (List<ExpandedSwapLeg>) newValue;
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
    public ExpandedSwap build() {
      return new ExpandedSwap(
          legs);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the legs of the swap.
     * <p>
     * A swap consists of one or more legs.
     * The legs of a swap are essentially unordered, however it is more efficient
     * and closer to user expectation to treat them as being ordered.
     * @param legs  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder legs(List<ExpandedSwapLeg> legs) {
      JodaBeanUtils.notEmpty(legs, "legs");
      this.legs = legs;
      return this;
    }

    /**
     * Sets the {@code legs} property in the builder
     * from an array of objects.
     * @param legs  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder legs(ExpandedSwapLeg... legs) {
      return legs(ImmutableList.copyOf(legs));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("ExpandedSwap.Builder{");
      buf.append("legs").append('=').append(JodaBeanUtils.toString(legs));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
