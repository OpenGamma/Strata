/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod;

/**
 * A map of double values keyed by overnight rate in arrears caplet/floorlet periods.
 * <p>
 * The double values should represent the same metric.
 */
@BeanDefinition
public final class OvernightInArrearsCapletFloorletPeriodAmounts
    implements ImmutableBean, Serializable {

  /**
   * The map of overnight rate in arrears caplet/floorlet periods to the double amount.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<OvernightInArrearsCapletFloorletPeriod, Double> amounts;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of double amounts.
   *
   * @param doubleMap the map of doubles
   * @return the instance
   */
  public static OvernightInArrearsCapletFloorletPeriodAmounts of(Map<OvernightInArrearsCapletFloorletPeriod, Double> doubleMap) {
    return new OvernightInArrearsCapletFloorletPeriodAmounts(doubleMap);
  }

  /**
   * Gets a double amount for the provided overnight rate in arrears caplet/floorlet.
   *
   * @param period the caplet/floorlet
   * @return the double amount, empty if missing
   */
  public Optional<Double> findAmount(OvernightInArrearsCapletFloorletPeriod period) {
    return Optional.ofNullable(amounts.get(period));
  }

  /**
   * Gets a double amount for the provided overnight rate in arrears caplet/floorlet.
   *
   * @param period the caplet/floorlet
   * @return the double amount
   * @throws IllegalArgumentException if the period is missing
   */
  public double getAmount(OvernightInArrearsCapletFloorletPeriod period) {
    if (!amounts.containsKey(period)) {
      throw new IllegalArgumentException("Could not find double amount for " + period);
    }
    return amounts.get(period);
  }


  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapletFloorletPeriodAmounts}.
   * @return the meta-bean, not null
   */
  public static OvernightInArrearsCapletFloorletPeriodAmounts.Meta meta() {
    return OvernightInArrearsCapletFloorletPeriodAmounts.Meta.INSTANCE;
  }

  static {
    MetaBean.register(OvernightInArrearsCapletFloorletPeriodAmounts.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightInArrearsCapletFloorletPeriodAmounts.Builder builder() {
    return new OvernightInArrearsCapletFloorletPeriodAmounts.Builder();
  }

  private OvernightInArrearsCapletFloorletPeriodAmounts(
      Map<OvernightInArrearsCapletFloorletPeriod, Double> amounts) {
    JodaBeanUtils.notNull(amounts, "amounts");
    this.amounts = ImmutableMap.copyOf(amounts);
  }

  @Override
  public OvernightInArrearsCapletFloorletPeriodAmounts.Meta metaBean() {
    return OvernightInArrearsCapletFloorletPeriodAmounts.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the map of overnight rate in arrears caplet/floorlet periods to the double amount.
   * @return the value of the property, not null
   */
  public ImmutableMap<OvernightInArrearsCapletFloorletPeriod, Double> getAmounts() {
    return amounts;
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
      OvernightInArrearsCapletFloorletPeriodAmounts other = (OvernightInArrearsCapletFloorletPeriodAmounts) obj;
      return JodaBeanUtils.equal(amounts, other.amounts);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(amounts);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("OvernightInArrearsCapletFloorletPeriodAmounts{");
    buf.append("amounts").append('=').append(JodaBeanUtils.toString(amounts));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapletFloorletPeriodAmounts}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code amounts} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<OvernightInArrearsCapletFloorletPeriod, Double>> amounts = DirectMetaProperty.ofImmutable(
        this, "amounts", OvernightInArrearsCapletFloorletPeriodAmounts.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "amounts");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -879772901:  // amounts
          return amounts;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightInArrearsCapletFloorletPeriodAmounts.Builder builder() {
      return new OvernightInArrearsCapletFloorletPeriodAmounts.Builder();
    }

    @Override
    public Class<? extends OvernightInArrearsCapletFloorletPeriodAmounts> beanType() {
      return OvernightInArrearsCapletFloorletPeriodAmounts.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code amounts} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<OvernightInArrearsCapletFloorletPeriod, Double>> amounts() {
      return amounts;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -879772901:  // amounts
          return ((OvernightInArrearsCapletFloorletPeriodAmounts) bean).getAmounts();
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
   * The bean-builder for {@code OvernightInArrearsCapletFloorletPeriodAmounts}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightInArrearsCapletFloorletPeriodAmounts> {

    private Map<OvernightInArrearsCapletFloorletPeriod, Double> amounts = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(OvernightInArrearsCapletFloorletPeriodAmounts beanToCopy) {
      this.amounts = beanToCopy.getAmounts();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -879772901:  // amounts
          return amounts;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -879772901:  // amounts
          this.amounts = (Map<OvernightInArrearsCapletFloorletPeriod, Double>) newValue;
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
    public OvernightInArrearsCapletFloorletPeriodAmounts build() {
      return new OvernightInArrearsCapletFloorletPeriodAmounts(
          amounts);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the map of overnight rate in arrears caplet/floorlet periods to the double amount.
     * @param amounts  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder amounts(Map<OvernightInArrearsCapletFloorletPeriod, Double> amounts) {
      JodaBeanUtils.notNull(amounts, "amounts");
      this.amounts = amounts;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("OvernightInArrearsCapletFloorletPeriodAmounts.Builder{");
      buf.append("amounts").append('=').append(JodaBeanUtils.toString(amounts));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
