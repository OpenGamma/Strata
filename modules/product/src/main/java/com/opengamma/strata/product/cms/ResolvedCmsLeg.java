/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.SwapIndex;

/**
 * A CMS leg of a constant maturity swap (CMS) product, resolved for pricing.
 * <p>
 * This is the resolved form of {@link CmsLeg} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedCmLegs} from a {@code CmsLeg}
 * using {@link CmsLeg#resolve(ReferenceData)}.
 * <p>
 * This defines a single leg for a CMS product and is formed from a number of periods.
 * Each period may be a CMS coupon, caplet or floorlet.
 * <p>
 * A {@code ResolvedCmsLeg} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedCmsLeg
    implements ImmutableBean, Serializable {

  /**
   * Whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative swap rates can result in a payment in the opposite direction
   * to that implied by this indicator.
   * <p>
   * The value of this flag should match the signs of the payment period notionals.
   */
  @PropertyDefinition(validate = "notNull")
  private final PayReceive payReceive;
  /**
   * The periodic payments based on the successive observed values of a swap index.
   * <p>
   * Each payment period represents part of the life-time of the leg.
   * In most cases, the periods do not overlap. However, since each payment period
   * is essentially independent the data model allows overlapping periods.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<CmsPeriod> cmsPeriods;

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private ResolvedCmsLeg(
      PayReceive payReceive,
      List<CmsPeriod> cmsPeriods) {

    this.payReceive = ArgChecker.notNull(payReceive, "payReceive");
    this.cmsPeriods = ImmutableList.copyOf(cmsPeriods);
    Set<Currency> currencies = this.cmsPeriods.stream().map(CmsPeriod::getCurrency).collect(Collectors.toSet());
    ArgChecker.isTrue(currencies.size() == 1, "Leg must have a single currency, found: " + currencies);
    Set<SwapIndex> swapIndices = this.cmsPeriods.stream().map(CmsPeriod::getIndex).collect(Collectors.toSet());
    ArgChecker.isTrue(swapIndices.size() == 1, "Leg must have a single swap index: " + swapIndices);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the leg
   */
  public LocalDate getStartDate() {
    return cmsPeriods.get(0).getStartDate();
  }

  /**
   * Gets the end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the maturity date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date of the leg
   */
  public LocalDate getEndDate() {
    return cmsPeriods.get(cmsPeriods.size() - 1).getEndDate();
  }

  /**
   * Gets the currency of the leg.
   * <p>
   * All periods in the leg will have this currency.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return cmsPeriods.get(0).getCurrency();
  }

  /**
   * Gets the swap index of the leg.
   * <p>
   * All periods in the leg will have this index.
   * 
   * @return the index
   */
  public SwapIndex getIndex() {
    return cmsPeriods.get(0).getIndex();
  }

  /**
   * Gets the underlying Ibor index that the leg is based on.
   * <p>
   * All periods in the leg will have this index.
   * 
   * @return the index
   */
  public IborIndex getUnderlyingIndex() {
    return getIndex().getTemplate().getConvention().getFloatingLeg().getIndex();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedCmsLeg}.
   * @return the meta-bean, not null
   */
  public static ResolvedCmsLeg.Meta meta() {
    return ResolvedCmsLeg.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedCmsLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedCmsLeg.Builder builder() {
    return new ResolvedCmsLeg.Builder();
  }

  @Override
  public ResolvedCmsLeg.Meta metaBean() {
    return ResolvedCmsLeg.Meta.INSTANCE;
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
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative swap rates can result in a payment in the opposite direction
   * to that implied by this indicator.
   * <p>
   * The value of this flag should match the signs of the payment period notionals.
   * @return the value of the property, not null
   */
  public PayReceive getPayReceive() {
    return payReceive;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the periodic payments based on the successive observed values of a swap index.
   * <p>
   * Each payment period represents part of the life-time of the leg.
   * In most cases, the periods do not overlap. However, since each payment period
   * is essentially independent the data model allows overlapping periods.
   * @return the value of the property, not empty
   */
  public ImmutableList<CmsPeriod> getCmsPeriods() {
    return cmsPeriods;
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
      ResolvedCmsLeg other = (ResolvedCmsLeg) obj;
      return JodaBeanUtils.equal(payReceive, other.payReceive) &&
          JodaBeanUtils.equal(cmsPeriods, other.cmsPeriods);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(payReceive);
    hash = hash * 31 + JodaBeanUtils.hashCode(cmsPeriods);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ResolvedCmsLeg{");
    buf.append("payReceive").append('=').append(payReceive).append(',').append(' ');
    buf.append("cmsPeriods").append('=').append(JodaBeanUtils.toString(cmsPeriods));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedCmsLeg}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code payReceive} property.
     */
    private final MetaProperty<PayReceive> payReceive = DirectMetaProperty.ofImmutable(
        this, "payReceive", ResolvedCmsLeg.class, PayReceive.class);
    /**
     * The meta-property for the {@code cmsPeriods} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CmsPeriod>> cmsPeriods = DirectMetaProperty.ofImmutable(
        this, "cmsPeriods", ResolvedCmsLeg.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "payReceive",
        "cmsPeriods");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case 2121598281:  // cmsPeriods
          return cmsPeriods;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedCmsLeg.Builder builder() {
      return new ResolvedCmsLeg.Builder();
    }

    @Override
    public Class<? extends ResolvedCmsLeg> beanType() {
      return ResolvedCmsLeg.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code payReceive} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PayReceive> payReceive() {
      return payReceive;
    }

    /**
     * The meta-property for the {@code cmsPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<CmsPeriod>> cmsPeriods() {
      return cmsPeriods;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return ((ResolvedCmsLeg) bean).getPayReceive();
        case 2121598281:  // cmsPeriods
          return ((ResolvedCmsLeg) bean).getCmsPeriods();
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
   * The bean-builder for {@code ResolvedCmsLeg}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedCmsLeg> {

    private PayReceive payReceive;
    private List<CmsPeriod> cmsPeriods = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedCmsLeg beanToCopy) {
      this.payReceive = beanToCopy.getPayReceive();
      this.cmsPeriods = beanToCopy.getCmsPeriods();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case 2121598281:  // cmsPeriods
          return cmsPeriods;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          this.payReceive = (PayReceive) newValue;
          break;
        case 2121598281:  // cmsPeriods
          this.cmsPeriods = (List<CmsPeriod>) newValue;
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
    public ResolvedCmsLeg build() {
      return new ResolvedCmsLeg(
          payReceive,
          cmsPeriods);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the leg is pay or receive.
     * <p>
     * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
     * A value of 'Receive' implies that the resulting amount is received from the counterparty.
     * Note that negative swap rates can result in a payment in the opposite direction
     * to that implied by this indicator.
     * <p>
     * The value of this flag should match the signs of the payment period notionals.
     * @param payReceive  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payReceive(PayReceive payReceive) {
      JodaBeanUtils.notNull(payReceive, "payReceive");
      this.payReceive = payReceive;
      return this;
    }

    /**
     * Sets the periodic payments based on the successive observed values of a swap index.
     * <p>
     * Each payment period represents part of the life-time of the leg.
     * In most cases, the periods do not overlap. However, since each payment period
     * is essentially independent the data model allows overlapping periods.
     * @param cmsPeriods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder cmsPeriods(List<CmsPeriod> cmsPeriods) {
      JodaBeanUtils.notEmpty(cmsPeriods, "cmsPeriods");
      this.cmsPeriods = cmsPeriods;
      return this;
    }

    /**
     * Sets the {@code cmsPeriods} property in the builder
     * from an array of objects.
     * @param cmsPeriods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder cmsPeriods(CmsPeriod... cmsPeriods) {
      return cmsPeriods(ImmutableList.copyOf(cmsPeriods));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ResolvedCmsLeg.Builder{");
      buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
      buf.append("cmsPeriods").append('=').append(JodaBeanUtils.toString(cmsPeriods));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
