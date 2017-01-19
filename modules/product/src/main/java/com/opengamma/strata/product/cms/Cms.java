/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

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
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * A constant maturity swap (CMS) or CMS cap/floor.
 * <p>
 * The CMS product consists of two legs, a CMS leg and a pay leg.
 * The CMS leg of CMS periodically pays coupons based on swap rate, which is the observed
 * value of a {@linkplain SwapIndex swap index}.
 * The pay leg is any swap leg from a standard interest rate swap. The pay leg may be absent
 * for certain CMS products, with the premium paid upfront instead, as defined on {@link CmsTrade}.
 * <p>
 * CMS cap/floor instruments can be created. These are defined as a set of call/put options
 * on successive swap rates, creating CMS caplets/floorlets.
 * <p>
 * For example, a CMS trade might involve an agreement to exchange the difference between
 * the fixed rate of 1% and the swap rate of 5-year 'GBP-FIXED-6M-LIBOR-6M' swaps every 6 months for 2 years.
 */
@BeanDefinition(builderScope = "private")
public final class Cms
    implements Product, Resolvable<ResolvedCms>, ImmutableBean, Serializable {

  /**
   * The CMS leg of the product.
   * <p>
   * This is associated with periodic payments based on swap rate.
   * The payments are CMS coupons, CMS caplets or CMS floors.
   */
  @PropertyDefinition(validate = "notNull")
  private final CmsLeg cmsLeg;
  /**
   * The optional pay leg of the product.
   * <p>
   * Typically this is associated with periodic fixed or Ibor rate payments without compounding or notional exchange.
   * <p>
   * These periodic payments are not made over the lifetime of the product for certain CMS products.
   * Instead the premium is paid upfront.
   */
  @PropertyDefinition(get = "optional")
  private final SwapLeg payLeg;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a CMS leg with no pay leg.
   * <p>
   * The pay leg is absent in the resulting CMS.
   * 
   * @param cmsLeg  the CMS leg
   * @return the CMS
   */
  public static Cms of(CmsLeg cmsLeg) {
    return new Cms(cmsLeg, null);
  }

  /**
   * Obtains an instance from a CMS leg and a pay leg.
   * 
   * @param cmsLeg  the CMS leg
   * @param payLeg  the pay leg
   * @return the CMS
   */
  public static Cms of(CmsLeg cmsLeg, SwapLeg payLeg) {
    return new Cms(cmsLeg, payLeg);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (getPayLeg().isPresent()) {
      ArgChecker.isFalse(payLeg.getPayReceive().equals(cmsLeg.getPayReceive()),
          "Two legs should have different Pay/Receive flags");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedCms resolve(ReferenceData refData) {
    if (payLeg == null) {
      return ResolvedCms.of(cmsLeg.resolve(refData));
    }
    return ResolvedCms.of(cmsLeg.resolve(refData), payLeg.resolve(refData));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the set of currencies referred to by the CMS.
   * <p>
   * This returns the complete set of payment currencies for the CMS.
   * This will typically return one currency, but could return two.
   * 
   * @return the set of payment currencies referred to by this swap
   */
  public ImmutableSet<Currency> allPaymentCurrencies() {
    if (payLeg == null) {
      return ImmutableSet.of(cmsLeg.getCurrency());
    }
    return ImmutableSet.of(cmsLeg.getCurrency(), payLeg.getCurrency());
  }

  /**
   * Returns the set of rate indices referred to by the CMS.
   * <p>
   * The CMS leg will refer to one index, such as 'GBP-LIBOR-3M'.
   * The pay leg may refer to a different index.
   * The swap index will not be included.
   * 
   * @return the set of indices referred to by this CMS
   */
  public ImmutableSet<Index> allRateIndices() {
    IborIndex cmsIndex = cmsLeg.getUnderlyingIndex();
    if (payLeg == null) {
      return ImmutableSet.of(cmsIndex);
    }
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    payLeg.collectIndices(builder);
    builder.add(cmsIndex);
    return builder.build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Cms}.
   * @return the meta-bean, not null
   */
  public static Cms.Meta meta() {
    return Cms.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Cms.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private Cms(
      CmsLeg cmsLeg,
      SwapLeg payLeg) {
    JodaBeanUtils.notNull(cmsLeg, "cmsLeg");
    this.cmsLeg = cmsLeg;
    this.payLeg = payLeg;
    validate();
  }

  @Override
  public Cms.Meta metaBean() {
    return Cms.Meta.INSTANCE;
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
   * Gets the CMS leg of the product.
   * <p>
   * This is associated with periodic payments based on swap rate.
   * The payments are CMS coupons, CMS caplets or CMS floors.
   * @return the value of the property, not null
   */
  public CmsLeg getCmsLeg() {
    return cmsLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional pay leg of the product.
   * <p>
   * Typically this is associated with periodic fixed or Ibor rate payments without compounding or notional exchange.
   * <p>
   * These periodic payments are not made over the lifetime of the product for certain CMS products.
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
      Cms other = (Cms) obj;
      return JodaBeanUtils.equal(cmsLeg, other.cmsLeg) &&
          JodaBeanUtils.equal(payLeg, other.payLeg);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(cmsLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(payLeg);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("Cms{");
    buf.append("cmsLeg").append('=').append(cmsLeg).append(',').append(' ');
    buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Cms}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code cmsLeg} property.
     */
    private final MetaProperty<CmsLeg> cmsLeg = DirectMetaProperty.ofImmutable(
        this, "cmsLeg", Cms.class, CmsLeg.class);
    /**
     * The meta-property for the {@code payLeg} property.
     */
    private final MetaProperty<SwapLeg> payLeg = DirectMetaProperty.ofImmutable(
        this, "payLeg", Cms.class, SwapLeg.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "cmsLeg",
        "payLeg");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1356515323:  // cmsLeg
          return cmsLeg;
        case -995239866:  // payLeg
          return payLeg;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends Cms> builder() {
      return new Cms.Builder();
    }

    @Override
    public Class<? extends Cms> beanType() {
      return Cms.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code cmsLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CmsLeg> cmsLeg() {
      return cmsLeg;
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
        case -1356515323:  // cmsLeg
          return ((Cms) bean).getCmsLeg();
        case -995239866:  // payLeg
          return ((Cms) bean).payLeg;
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
   * The bean-builder for {@code Cms}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<Cms> {

    private CmsLeg cmsLeg;
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
        case -1356515323:  // cmsLeg
          return cmsLeg;
        case -995239866:  // payLeg
          return payLeg;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1356515323:  // cmsLeg
          this.cmsLeg = (CmsLeg) newValue;
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
    public Cms build() {
      return new Cms(
          cmsLeg,
          payLeg);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("Cms.Builder{");
      buf.append("cmsLeg").append('=').append(JodaBeanUtils.toString(cmsLeg)).append(',').append(' ');
      buf.append("payLeg").append('=').append(JodaBeanUtils.toString(payLeg));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
