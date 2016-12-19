/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.DerivedProperty;
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
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.common.PayReceive;

/**
 * A rate swap, resolved for pricing.
 * <p>
 * This is the resolved form of {@link Swap} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedSwap} from a {@code Swap}
 * using {@link Swap#resolve(ReferenceData)}.
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
 * Any combination of legs, payments and accrual periods is supported in the data model,
 * however there is no guarantee that exotic combinations will price sensibly.
 * <p>
 * A {@code ResolvedSwap} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedSwap
    implements ResolvedProduct, ImmutableBean, Serializable {

  /**
   * The legs of the swap.
   * <p>
   * A swap consists of one or more legs.
   * The legs of a swap are essentially unordered, however it is more efficient
   * and closer to user expectation to treat them as being ordered.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<ResolvedSwapLeg> legs;
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
   * Creates a swap from one or more swap legs.
   * <p>
   * While most swaps have two legs, other combinations are possible.
   * 
   * @param legs  the array of legs
   * @return the swap
   */
  public static ResolvedSwap of(ResolvedSwapLeg... legs) {
    ArgChecker.notEmpty(legs, "legs");
    return new ResolvedSwap(ImmutableList.copyOf(legs));
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private ResolvedSwap(List<ResolvedSwapLeg> legs) {
    JodaBeanUtils.notEmpty(legs, "legs");
    this.legs = ImmutableList.copyOf(legs);
    this.currencies = buildCurrencies(legs);
    this.indices = buildIndices(legs);
  }

  // trusted constructor
  ResolvedSwap(ImmutableList<ResolvedSwapLeg> legs, ImmutableSet<Currency> currencies, ImmutableSet<Index> indices) {
    this.legs = legs;
    this.currencies = currencies;
    this.indices = indices;
  }

  // collect the set of currencies
  private static ImmutableSet<Currency> buildCurrencies(List<ResolvedSwapLeg> legs) {
    // avoid streams as profiling showed a hotspot
    ImmutableSet.Builder<Currency> builder = ImmutableSet.builder();
    for (ResolvedSwapLeg leg : legs) {
      builder.add(leg.getCurrency());
    }
    return builder.build();
  }

  // collect the set of indices
  private static ImmutableSet<Index> buildIndices(List<ResolvedSwapLeg> legs) {
    // avoid streams as profiling showed a hotspot
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    for (ResolvedSwapLeg leg : legs) {
      leg.collectIndices(builder);
    }
    return builder.build();
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new ResolvedSwap(legs);
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
  public ImmutableList<ResolvedSwapLeg> getLegs(SwapLegType type) {
    return legs.stream().filter(leg -> leg.getType() == type).collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the first pay or receive leg of the swap.
   * <p>
   * This returns the first pay or receive leg of the swap, empty if no matching leg.
   * 
   * @param payReceive  the pay or receive flag
   * @return the first matching leg of the swap
   */
  public Optional<ResolvedSwapLeg> getLeg(PayReceive payReceive) {
    return legs.stream().filter(leg -> leg.getPayReceive() == payReceive).findFirst();
  }

  /**
   * Gets the first pay leg of the swap.
   * <p>
   * This returns the first pay leg of the swap, empty if no pay leg.
   * 
   * @return the first pay leg of the swap
   */
  public Optional<ResolvedSwapLeg> getPayLeg() {
    return getLeg(PayReceive.PAY);
  }

  /**
   * Gets the first receive leg of the swap.
   * <p>
   * This returns the first receive leg of the swap, empty if no receive leg.
   * 
   * @return the first receive leg of the swap
   */
  public Optional<ResolvedSwapLeg> getReceiveLeg() {
    return getLeg(PayReceive.RECEIVE);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the accrual start date of the swap.
   * <p>
   * This is the earliest accrual date of the legs, often known as the effective date.
   * This date has typically been adjusted to be a valid business day.
   * 
   * @return the start date of the swap
   */
  @DerivedProperty
  public LocalDate getStartDate() {
    return legs.stream()
        .map(ResolvedSwapLeg::getStartDate)
        .min(Comparator.naturalOrder())
        .get();  // always at least one leg, so get() is safe
  }

  /**
   * Gets the accrual end date of the swap.
   * <p>
   * This is the latest accrual date of the legs, often known as the termination date.
   * This date has typically been adjusted to be a valid business day.
   * 
   * @return the end date of the swap
   */
  @DerivedProperty
  public LocalDate getEndDate() {
    return legs.stream()
        .map(ResolvedSwapLeg::getEndDate)
        .max(Comparator.naturalOrder())
        .get();  // always at least one leg, so get() is safe
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
    return currencies.size() > 1;
  }

  /**
   * Returns the set of payment currencies referred to by the swap.
   * <p>
   * This returns the complete set of payment currencies for the swap.
   * This will typically return one or two currencies.
   * <p>
   * If there is an FX reset, then this set contains the currency of the payment,
   * not the currency of the notional. Note that in many cases, the currency of
   * the FX reset notional will be the currency of the other leg.
   * 
   * @return the currencies
   */
  public ImmutableSet<Currency> allPaymentCurrencies() {
    return currencies;
  }

  /**
   * Returns the set of indices referred to by the swap.
   * <p>
   * A swap will typically refer to at least one index, such as 'GBP-LIBOR-3M'.
   * Calling this method will return the complete list of indices, including
   * any associated with FX reset.
   * 
   * @return the set of indices referred to by this swap
   */
  public ImmutableSet<Index> allIndices() {
    return indices;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedSwap}.
   * @return the meta-bean, not null
   */
  public static ResolvedSwap.Meta meta() {
    return ResolvedSwap.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedSwap.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedSwap.Builder builder() {
    return new ResolvedSwap.Builder();
  }

  @Override
  public ResolvedSwap.Meta metaBean() {
    return ResolvedSwap.Meta.INSTANCE;
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
  public ImmutableList<ResolvedSwapLeg> getLegs() {
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
      ResolvedSwap other = (ResolvedSwap) obj;
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
    StringBuilder buf = new StringBuilder(128);
    buf.append("ResolvedSwap{");
    buf.append("legs").append('=').append(legs).append(',').append(' ');
    buf.append("startDate").append('=').append(getStartDate()).append(',').append(' ');
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(getEndDate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedSwap}.
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
    private final MetaProperty<ImmutableList<ResolvedSwapLeg>> legs = DirectMetaProperty.ofImmutable(
        this, "legs", ResolvedSwap.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofDerived(
        this, "startDate", ResolvedSwap.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofDerived(
        this, "endDate", ResolvedSwap.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "legs",
        "startDate",
        "endDate");

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
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedSwap.Builder builder() {
      return new ResolvedSwap.Builder();
    }

    @Override
    public Class<? extends ResolvedSwap> beanType() {
      return ResolvedSwap.class;
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
    public MetaProperty<ImmutableList<ResolvedSwapLeg>> legs() {
      return legs;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3317797:  // legs
          return ((ResolvedSwap) bean).getLegs();
        case -2129778896:  // startDate
          return ((ResolvedSwap) bean).getStartDate();
        case -1607727319:  // endDate
          return ((ResolvedSwap) bean).getEndDate();
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
   * The bean-builder for {@code ResolvedSwap}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedSwap> {

    private List<ResolvedSwapLeg> legs = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedSwap beanToCopy) {
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
          this.legs = (List<ResolvedSwapLeg>) newValue;
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
    public ResolvedSwap build() {
      return new ResolvedSwap(
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
    public Builder legs(List<ResolvedSwapLeg> legs) {
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
    public Builder legs(ResolvedSwapLeg... legs) {
      return legs(ImmutableList.copyOf(legs));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("ResolvedSwap.Builder{");
      buf.append("legs").append('=').append(JodaBeanUtils.toString(legs));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
