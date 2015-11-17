/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A single fixing of an index that is observed by {@code IborAveragedRateObservation}.
 * <p>
 * The interest rate is determined for each reset period, with the weight used
 * to create a weighted average.
 */
@BeanDefinition
public final class IborAveragedFixing
    implements ImmutableBean, Serializable {

  /**
   * The fixing date to use to determine a rate for the reset period.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * Valid business days are defined by {@link IborIndex#getFixingCalendar()}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;
  /**
   * The fixed rate for the fixing date, optional.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * In certain circumstances two counterparties agree the rate of a fixing when the contract starts.
   * It is used in place of an observed fixing.
   * Other calculation elements, such as gearing or spread, still apply.
   * <p>
   * If the value not present, which is the normal case, then the rate is
   * observed via the normal fixing process.
   */
  @PropertyDefinition(get = "optional")
  private final Double fixedRate;
  /**
   * The weight to apply to this fixing.
   * <p>
   * If the averaging is unweighted, then all weights must be one.
   */
  @PropertyDefinition
  private final double weight;

  //-------------------------------------------------------------------------
  /**
   * Creates a {@code IborAveragedFixing} from the fixing date with a weight of 1.
   * 
   * @param fixingDate  the fixing date
   * @return the fixing information
   */
  public static IborAveragedFixing of(LocalDate fixingDate) {
    return of(fixingDate, null);
  }

  /**
   * Creates a {@code IborAveragedFixing} from the fixing date with a weight of 1.
   * 
   * @param fixingDate  the fixing date
   * @param fixedRate  the fixed rate for the fixing date, optional, may be null
   * @return the fixing information
   */
  public static IborAveragedFixing of(LocalDate fixingDate, Double fixedRate) {
    return IborAveragedFixing.builder()
        .fixingDate(fixingDate)
        .fixedRate(fixedRate)
        .build();
  }

  /**
   * Creates a {@code IborAveragedFixing} from the fixing date, calculating the weight
   * from the number of days in the reset period.
   * <p>
   * This implements the standard approach to average weights, which is to set each
   * weight to the actual number of days between the start and end of the reset period.
   * 
   * @param fixingDate  the fixing date
   * @param startDate  the start date of the reset period
   * @param endDate  the end date of the reset period
   * @return the fixing information
   */
  public static IborAveragedFixing ofDaysInResetPeriod(
      LocalDate fixingDate,
      LocalDate startDate,
      LocalDate endDate) {
    return ofDaysInResetPeriod(fixingDate, startDate, endDate, null);
  }

  /**
   * Creates a {@code IborAveragedFixing} from the fixing date, calculating the weight
   * from the number of days in the reset period.
   * <p>
   * This implements the standard approach to average weights, which is to set each
   * weight to the actual number of days between the start and end of the reset period.
   * 
   * @param fixingDate  the fixing date
   * @param startDate  the start date of the reset period
   * @param endDate  the end date of the reset period
   * @param fixedRate  the fixed rate for the fixing date, optional, may be null
   * @return the fixing information
   */
  public static IborAveragedFixing ofDaysInResetPeriod(
      LocalDate fixingDate,
      LocalDate startDate,
      LocalDate endDate,
      Double fixedRate) {
    ArgChecker.notNull(fixingDate, "fixingDate");
    ArgChecker.notNull(startDate, "startDate");
    ArgChecker.notNull(endDate, "endDate");
    return IborAveragedFixing.builder()
        .fixingDate(fixingDate)
        .fixedRate(fixedRate)
        .weight(endDate.toEpochDay() - startDate.toEpochDay())
        .build();
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.weight(1d);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborAveragedFixing}.
   * @return the meta-bean, not null
   */
  public static IborAveragedFixing.Meta meta() {
    return IborAveragedFixing.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborAveragedFixing.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborAveragedFixing.Builder builder() {
    return new IborAveragedFixing.Builder();
  }

  private IborAveragedFixing(
      LocalDate fixingDate,
      Double fixedRate,
      double weight) {
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    this.fixingDate = fixingDate;
    this.fixedRate = fixedRate;
    this.weight = weight;
  }

  @Override
  public IborAveragedFixing.Meta metaBean() {
    return IborAveragedFixing.Meta.INSTANCE;
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
   * Gets the fixing date to use to determine a rate for the reset period.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * Valid business days are defined by {@link IborIndex#getFixingCalendar()}.
   * @return the value of the property, not null
   */
  public LocalDate getFixingDate() {
    return fixingDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed rate for the fixing date, optional.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * In certain circumstances two counterparties agree the rate of a fixing when the contract starts.
   * It is used in place of an observed fixing.
   * Other calculation elements, such as gearing or spread, still apply.
   * <p>
   * If the value not present, which is the normal case, then the rate is
   * observed via the normal fixing process.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getFixedRate() {
    return fixedRate != null ? OptionalDouble.of(fixedRate) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the weight to apply to this fixing.
   * <p>
   * If the averaging is unweighted, then all weights must be one.
   * @return the value of the property
   */
  public double getWeight() {
    return weight;
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
      IborAveragedFixing other = (IborAveragedFixing) obj;
      return JodaBeanUtils.equal(fixingDate, other.fixingDate) &&
          JodaBeanUtils.equal(fixedRate, other.fixedRate) &&
          JodaBeanUtils.equal(weight, other.weight);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(weight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("IborAveragedFixing{");
    buf.append("fixingDate").append('=').append(fixingDate).append(',').append(' ');
    buf.append("fixedRate").append('=').append(fixedRate).append(',').append(' ');
    buf.append("weight").append('=').append(JodaBeanUtils.toString(weight));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborAveragedFixing}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", IborAveragedFixing.class, LocalDate.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", IborAveragedFixing.class, Double.class);
    /**
     * The meta-property for the {@code weight} property.
     */
    private final MetaProperty<Double> weight = DirectMetaProperty.ofImmutable(
        this, "weight", IborAveragedFixing.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "fixingDate",
        "fixedRate",
        "weight");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1255202043:  // fixingDate
          return fixingDate;
        case 747425396:  // fixedRate
          return fixedRate;
        case -791592328:  // weight
          return weight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborAveragedFixing.Builder builder() {
      return new IborAveragedFixing.Builder();
    }

    @Override
    public Class<? extends IborAveragedFixing> beanType() {
      return IborAveragedFixing.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code fixingDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> fixingDate() {
      return fixingDate;
    }

    /**
     * The meta-property for the {@code fixedRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> fixedRate() {
      return fixedRate;
    }

    /**
     * The meta-property for the {@code weight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> weight() {
      return weight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1255202043:  // fixingDate
          return ((IborAveragedFixing) bean).getFixingDate();
        case 747425396:  // fixedRate
          return ((IborAveragedFixing) bean).fixedRate;
        case -791592328:  // weight
          return ((IborAveragedFixing) bean).getWeight();
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
   * The bean-builder for {@code IborAveragedFixing}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborAveragedFixing> {

    private LocalDate fixingDate;
    private Double fixedRate;
    private double weight;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborAveragedFixing beanToCopy) {
      this.fixingDate = beanToCopy.getFixingDate();
      this.fixedRate = beanToCopy.fixedRate;
      this.weight = beanToCopy.getWeight();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1255202043:  // fixingDate
          return fixingDate;
        case 747425396:  // fixedRate
          return fixedRate;
        case -791592328:  // weight
          return weight;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
          break;
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
          break;
        case -791592328:  // weight
          this.weight = (Double) newValue;
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
    public IborAveragedFixing build() {
      return new IborAveragedFixing(
          fixingDate,
          fixedRate,
          weight);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the fixing date to use to determine a rate for the reset period.
     * <p>
     * This is an adjusted date with any business day rule applied.
     * Valid business days are defined by {@link IborIndex#getFixingCalendar()}.
     * @param fixingDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDate(LocalDate fixingDate) {
      JodaBeanUtils.notNull(fixingDate, "fixingDate");
      this.fixingDate = fixingDate;
      return this;
    }

    /**
     * Sets the fixed rate for the fixing date, optional.
     * A 5% rate will be expressed as 0.05.
     * <p>
     * In certain circumstances two counterparties agree the rate of a fixing when the contract starts.
     * It is used in place of an observed fixing.
     * Other calculation elements, such as gearing or spread, still apply.
     * <p>
     * If the value not present, which is the normal case, then the rate is
     * observed via the normal fixing process.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(Double fixedRate) {
      this.fixedRate = fixedRate;
      return this;
    }

    /**
     * Sets the weight to apply to this fixing.
     * <p>
     * If the averaging is unweighted, then all weights must be one.
     * @param weight  the new value
     * @return this, for chaining, not null
     */
    public Builder weight(double weight) {
      this.weight = weight;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("IborAveragedFixing.Builder{");
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate)).append(',').append(' ');
      buf.append("weight").append('=').append(JodaBeanUtils.toString(weight));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
