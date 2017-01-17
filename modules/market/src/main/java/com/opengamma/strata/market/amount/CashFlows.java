/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;

/**
 * A collection of cash flows.
 * <p>
 * Contains a list of {@linkplain CashFlow cash flow} objects, each referring to a single cash flow.
 * The list is can be {@linkplain #sorted() sorted} by date and value if desired.
 */
@BeanDefinition(builderScope = "private")
public final class CashFlows
    implements FxConvertible<CashFlows>, ImmutableBean, Serializable {

  /**
   * A cash flows instance to be used when there is no cash flow.
   */
  public static final CashFlows NONE = new CashFlows(ImmutableList.of());

  /**
   * The cash flows.
   * <p>
   * Each entry includes details of a single cash flow.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<CashFlow> cashFlows;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a single cash flow.
   * 
   * @param cashFlow The cash flow
   * @return the cash flows instance
   */
  public static CashFlows of(CashFlow cashFlow) {
    return CashFlows.of(ImmutableList.of(cashFlow));
  }

  /**
   * Obtains an instance from a list of cash flows.
   * 
   * @param cashFlows the list of cash flows
   * @return the cash flows instance
   */
  public static CashFlows of(List<CashFlow> cashFlows) {
    return new CashFlows(cashFlows);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the cash flow by index.
   * 
   * @param index  the index to get
   * @return the cash flow
   */
  public CashFlow getCashFlow(int index) {
    return cashFlows.get(index);
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this cash flows instance with another cash flow.
   * <p>
   * This returns a new cash flows instance with a combined list of cash flow instances.
   * This instance is immutable and unaffected by this method.
   * The result may contain duplicate cash flows.
   * 
   * @param cashFlow  the additional single cash flow
   * @return the new instance of {@code CashFlows} based on this instance, with the additional single cash flow added
   */
  public CashFlows combinedWith(CashFlow cashFlow) {
    return new CashFlows(ImmutableList.<CashFlow>builder()
        .addAll(cashFlows)
        .add(cashFlow)
        .build());
  }

  /**
   * Combines this cash flows instance with another one.
   * <p>
   * This returns a new cash flows instance with a combined list of cash flow instances.
   * This instance is immutable and unaffected by this method.
   * The result may contain duplicate cash flows.
   * 
   * @param other  the other cash flows
   * @return the new instance of {@code CashFlows} based on this instance, with the other instance added
   */
  public CashFlows combinedWith(CashFlows other) {
    return new CashFlows(ImmutableList.<CashFlow>builder()
        .addAll(cashFlows)
        .addAll(other.cashFlows)
        .build());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance that is sorted.
   * <p>
   * The sort is by date, then value.
   * 
   * @return the sorted instance
   */
  public CashFlows sorted() {
    if (Ordering.natural().isOrdered(cashFlows)) {
      return this;
    } else {
      return new CashFlows(Ordering.natural().immutableSortedCopy(cashFlows));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this collection of cash flows to an equivalent amount in the specified currency.
   * <p>
   * This ensures that the result will have all currency amounts expressed in terms of the given currency.
   * If conversion is needed, the provider will be used to supply the FX rate.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the converted instance, in the specified currency
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public CashFlows convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return CashFlows.of(cashFlows.stream()
        .map(cf -> cf.convertedTo(resultCurrency, rateProvider))
        .collect(toImmutableList()));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CashFlows}.
   * @return the meta-bean, not null
   */
  public static CashFlows.Meta meta() {
    return CashFlows.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CashFlows.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CashFlows(
      List<CashFlow> cashFlows) {
    JodaBeanUtils.notNull(cashFlows, "cashFlows");
    this.cashFlows = ImmutableList.copyOf(cashFlows);
  }

  @Override
  public CashFlows.Meta metaBean() {
    return CashFlows.Meta.INSTANCE;
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
   * Gets the cash flows.
   * <p>
   * Each entry includes details of a single cash flow.
   * @return the value of the property, not null
   */
  public ImmutableList<CashFlow> getCashFlows() {
    return cashFlows;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CashFlows other = (CashFlows) obj;
      return JodaBeanUtils.equal(cashFlows, other.cashFlows);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(cashFlows);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("CashFlows{");
    buf.append("cashFlows").append('=').append(JodaBeanUtils.toString(cashFlows));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CashFlows}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code cashFlows} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CashFlow>> cashFlows = DirectMetaProperty.ofImmutable(
        this, "cashFlows", CashFlows.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "cashFlows");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 733659538:  // cashFlows
          return cashFlows;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CashFlows> builder() {
      return new CashFlows.Builder();
    }

    @Override
    public Class<? extends CashFlows> beanType() {
      return CashFlows.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code cashFlows} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<CashFlow>> cashFlows() {
      return cashFlows;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 733659538:  // cashFlows
          return ((CashFlows) bean).getCashFlows();
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
   * The bean-builder for {@code CashFlows}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<CashFlows> {

    private List<CashFlow> cashFlows = ImmutableList.of();

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
        case 733659538:  // cashFlows
          return cashFlows;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 733659538:  // cashFlows
          this.cashFlows = (List<CashFlow>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public CashFlows build() {
      return new CashFlows(
          cashFlows);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("CashFlows.Builder{");
      buf.append("cashFlows").append('=').append(JodaBeanUtils.toString(cashFlows));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
