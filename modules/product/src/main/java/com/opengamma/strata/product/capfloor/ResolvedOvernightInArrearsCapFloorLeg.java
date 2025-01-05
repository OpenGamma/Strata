/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.PayReceive;

/**
 * A cap/floor leg of an overnight rate in arrears cap/floor product, resolved for pricing.
 * <p>
 * This is the resolved form of {@link OvernightInArrearsCapFloorLeg} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedOvernightInArrearsCapFloorLeg}
 * from a {@code OvernightInArrearsCapFloorLeg} using {@link OvernightInArrearsCapFloorLeg#resolve(ReferenceData)}.
 * <p>
 * This defines a single leg for an overnight rate in arrears cap/floor product and is formed from a number of periods.
 * Each period may be a caplet or floorlet.
 * The cap/floor instruments are defined as a set of call/put options on successive compounded overnight index rates.
 * <p>
 * A {@code ResolvedOvernightInArrearsCapFloorLeg} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedOvernightInArrearsCapFloorLeg
    implements ImmutableBean, Serializable {

  /**
   * Whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * <p>
   * The value of this flag should match the signs of the payment period notionals.
   */
  @PropertyDefinition(validate = "notNull")
  private final PayReceive payReceive;
  /**
   * The periodic payments based on the successive observed values of compounded overnight index rates.
   * <p>
   * Each payment period represents part of the life-time of the leg.
   * In most cases, the periods do not overlap. However, since each payment period
   * is essentially independent the data model allows overlapping periods.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<OvernightInArrearsCapletFloorletPeriod> capletFloorletPeriods;

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private ResolvedOvernightInArrearsCapFloorLeg(
      PayReceive payReceive,
      List<OvernightInArrearsCapletFloorletPeriod> capletFloorletPeriods) {

    this.payReceive = ArgChecker.notNull(payReceive, "payReceive");
    this.capletFloorletPeriods = ImmutableList.copyOf(capletFloorletPeriods);
    Set<Currency> currencies =
        this.capletFloorletPeriods.stream().map(OvernightInArrearsCapletFloorletPeriod::getCurrency).collect(Collectors.toSet());
    ArgChecker.isTrue(currencies.size() == 1, "Leg must have a single currency, found: " + currencies);
    Set<OvernightIndex> indices =
        this.capletFloorletPeriods.stream().map(OvernightInArrearsCapletFloorletPeriod::getIndex).collect(Collectors.toSet());
    ArgChecker.isTrue(indices.size() == 1, "Leg must have a single overnight index: " + indices);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the accrual start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   * This date has typically been adjusted to be a valid business day.
   *
   * @return the start date of the leg
   */
  public LocalDate getStartDate() {
    return capletFloorletPeriods.get(0).getStartDate();
  }

  /**
   * Gets the accrual end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the termination date.
   * This date has typically been adjusted to be a valid business day.
   *
   * @return the end date of the leg
   */
  public LocalDate getEndDate() {
    return capletFloorletPeriods.get(capletFloorletPeriods.size() - 1).getEndDate();
  }

  /**
   * Gets the final caplet/floorlet period.
   *
   * @return the final period
   */
  public OvernightInArrearsCapletFloorletPeriod getFinalPeriod() {
    return capletFloorletPeriods.get(capletFloorletPeriods.size() - 1);
  }

  /**
   * Gets the currency of the leg.
   * <p>
   * All periods in the leg will have this currency.
   *
   * @return the currency
   */
  public Currency getCurrency() {
    return capletFloorletPeriods.get(0).getCurrency();
  }

  /**
   * Gets the overnight index of the leg.
   * <p>
   * All periods in the leg will have this index.
   *
   * @return the index
   */
  public OvernightIndex getIndex() {
    return capletFloorletPeriods.get(0).getIndex();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ResolvedOvernightInArrearsCapFloorLeg}.
   * @return the meta-bean, not null
   */
  public static ResolvedOvernightInArrearsCapFloorLeg.Meta meta() {
    return ResolvedOvernightInArrearsCapFloorLeg.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ResolvedOvernightInArrearsCapFloorLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedOvernightInArrearsCapFloorLeg.Builder builder() {
    return new ResolvedOvernightInArrearsCapFloorLeg.Builder();
  }

  @Override
  public ResolvedOvernightInArrearsCapFloorLeg.Meta metaBean() {
    return ResolvedOvernightInArrearsCapFloorLeg.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * <p>
   * The value of this flag should match the signs of the payment period notionals.
   * @return the value of the property, not null
   */
  public PayReceive getPayReceive() {
    return payReceive;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the periodic payments based on the successive observed values of compounded overnight index rates.
   * <p>
   * Each payment period represents part of the life-time of the leg.
   * In most cases, the periods do not overlap. However, since each payment period
   * is essentially independent the data model allows overlapping periods.
   * @return the value of the property, not empty
   */
  public ImmutableList<OvernightInArrearsCapletFloorletPeriod> getCapletFloorletPeriods() {
    return capletFloorletPeriods;
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
      ResolvedOvernightInArrearsCapFloorLeg other = (ResolvedOvernightInArrearsCapFloorLeg) obj;
      return JodaBeanUtils.equal(payReceive, other.payReceive) &&
          JodaBeanUtils.equal(capletFloorletPeriods, other.capletFloorletPeriods);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(payReceive);
    hash = hash * 31 + JodaBeanUtils.hashCode(capletFloorletPeriods);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ResolvedOvernightInArrearsCapFloorLeg{");
    buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
    buf.append("capletFloorletPeriods").append('=').append(JodaBeanUtils.toString(capletFloorletPeriods));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedOvernightInArrearsCapFloorLeg}.
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
        this, "payReceive", ResolvedOvernightInArrearsCapFloorLeg.class, PayReceive.class);
    /**
     * The meta-property for the {@code capletFloorletPeriods} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<OvernightInArrearsCapletFloorletPeriod>> capletFloorletPeriods = DirectMetaProperty.ofImmutable(
        this, "capletFloorletPeriods", ResolvedOvernightInArrearsCapFloorLeg.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "payReceive",
        "capletFloorletPeriods");

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
        case 1504863482:  // capletFloorletPeriods
          return capletFloorletPeriods;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedOvernightInArrearsCapFloorLeg.Builder builder() {
      return new ResolvedOvernightInArrearsCapFloorLeg.Builder();
    }

    @Override
    public Class<? extends ResolvedOvernightInArrearsCapFloorLeg> beanType() {
      return ResolvedOvernightInArrearsCapFloorLeg.class;
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
     * The meta-property for the {@code capletFloorletPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<OvernightInArrearsCapletFloorletPeriod>> capletFloorletPeriods() {
      return capletFloorletPeriods;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return ((ResolvedOvernightInArrearsCapFloorLeg) bean).getPayReceive();
        case 1504863482:  // capletFloorletPeriods
          return ((ResolvedOvernightInArrearsCapFloorLeg) bean).getCapletFloorletPeriods();
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
   * The bean-builder for {@code ResolvedOvernightInArrearsCapFloorLeg}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedOvernightInArrearsCapFloorLeg> {

    private PayReceive payReceive;
    private List<OvernightInArrearsCapletFloorletPeriod> capletFloorletPeriods = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedOvernightInArrearsCapFloorLeg beanToCopy) {
      this.payReceive = beanToCopy.getPayReceive();
      this.capletFloorletPeriods = beanToCopy.getCapletFloorletPeriods();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case 1504863482:  // capletFloorletPeriods
          return capletFloorletPeriods;
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
        case 1504863482:  // capletFloorletPeriods
          this.capletFloorletPeriods = (List<OvernightInArrearsCapletFloorletPeriod>) newValue;
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
    public ResolvedOvernightInArrearsCapFloorLeg build() {
      return new ResolvedOvernightInArrearsCapFloorLeg(
          payReceive,
          capletFloorletPeriods);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the leg is pay or receive.
     * <p>
     * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
     * A value of 'Receive' implies that the resulting amount is received from the counterparty.
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
     * Sets the periodic payments based on the successive observed values of compounded overnight index rates.
     * <p>
     * Each payment period represents part of the life-time of the leg.
     * In most cases, the periods do not overlap. However, since each payment period
     * is essentially independent the data model allows overlapping periods.
     * @param capletFloorletPeriods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder capletFloorletPeriods(List<OvernightInArrearsCapletFloorletPeriod> capletFloorletPeriods) {
      JodaBeanUtils.notEmpty(capletFloorletPeriods, "capletFloorletPeriods");
      this.capletFloorletPeriods = capletFloorletPeriods;
      return this;
    }

    /**
     * Sets the {@code capletFloorletPeriods} property in the builder
     * from an array of objects.
     * @param capletFloorletPeriods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder capletFloorletPeriods(OvernightInArrearsCapletFloorletPeriod... capletFloorletPeriods) {
      return capletFloorletPeriods(ImmutableList.copyOf(capletFloorletPeriods));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ResolvedOvernightInArrearsCapFloorLeg.Builder{");
      buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
      buf.append("capletFloorletPeriods").append('=').append(JodaBeanUtils.toString(capletFloorletPeriods));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
