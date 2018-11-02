/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

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

import com.opengamma.strata.collect.ArgChecker;

/**
 * A future value notional amount for a fixed swap leg .
 * <p>
 * The future value notional is calculated as the notional multiplied by one plus the fixed rate raised to
 * the power of the fixed rate day count fraction, i.e.
 * Future Value Notional = Notional Amount * (1 + Fixed Rate) ^ (Fixed Rate Day Count Fraction).
 * <p>
 * The future value notional is normally only required for Brazilian CDI Swaps.
 */
@BeanDefinition
public final class FutureValueNotional
    implements ImmutableBean, Serializable {

  /**
   * An empty instance that causes the future value notional to be automatically
   * calculated using the standard formula..
   */
  private static final FutureValueNotional AUTO = FutureValueNotional.builder().build();
  /**
   * The amount.
   * <p>
   * The future value notional amount, derived as per the standard formula.
   */
  @PropertyDefinition(get = "optional")
  private final Double value;
  /**
   * The value date.
   * <p>
   * This is the adjusted value date of the future value amount, which is the adjusted end date.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate valueDate;
  /**
   * The number of days in the calculation period.
   * <p>
   * This defines the number of days from the adjusted start date to the adjusted end date
   * as calculated by the day count.
   */
  @PropertyDefinition(get = "optional")
  private final Integer calculationPeriodNumberOfDays;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty instance, that causes the future value notional to be automatically
   * calculated using the standard formula.
   * 
   * @return the empty instance
   */
  public static FutureValueNotional auto() {
    return AUTO;
  }

  /**
   * Obtains an instance from the specified amount, date and number of days.
   * 
   * @param value  the amount
   * @param valueDate  the value date
   * @param calculationPeriodNumberOfDays  the number of days
   * @return the future value notional
   */
  public static FutureValueNotional of(double value, LocalDate valueDate, int calculationPeriodNumberOfDays) {
    ArgChecker.isTrue(valueDate != null);
    return new FutureValueNotional(value, valueDate, calculationPeriodNumberOfDays);
  }

  /**
   * Obtains an instance from the specified amount.
   * 
   * @param value  the amount
   * @return the future value notional
   */
  public static FutureValueNotional of(double value) {
    return new FutureValueNotional(value, null, null);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FutureValueNotional}.
   * @return the meta-bean, not null
   */
  public static FutureValueNotional.Meta meta() {
    return FutureValueNotional.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FutureValueNotional.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FutureValueNotional.Builder builder() {
    return new FutureValueNotional.Builder();
  }

  private FutureValueNotional(
      Double value,
      LocalDate valueDate,
      Integer calculationPeriodNumberOfDays) {
    this.value = value;
    this.valueDate = valueDate;
    this.calculationPeriodNumberOfDays = calculationPeriodNumberOfDays;
  }

  @Override
  public FutureValueNotional.Meta metaBean() {
    return FutureValueNotional.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the amount.
   * <p>
   * The future value notional amount, derived as per the standard formula.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getValue() {
    return value != null ? OptionalDouble.of(value) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value date.
   * <p>
   * This is the adjusted value date of the future value amount, which is the adjusted end date.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getValueDate() {
    return Optional.ofNullable(valueDate);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days in the calculation period.
   * <p>
   * This defines the number of days from the adjusted start date to the adjusted end date
   * as calculated by the day count.
   * @return the optional value of the property, not null
   */
  public OptionalInt getCalculationPeriodNumberOfDays() {
    return calculationPeriodNumberOfDays != null ? OptionalInt.of(calculationPeriodNumberOfDays) : OptionalInt.empty();
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
      FutureValueNotional other = (FutureValueNotional) obj;
      return JodaBeanUtils.equal(value, other.value) &&
          JodaBeanUtils.equal(valueDate, other.valueDate) &&
          JodaBeanUtils.equal(calculationPeriodNumberOfDays, other.calculationPeriodNumberOfDays);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(value);
    hash = hash * 31 + JodaBeanUtils.hashCode(valueDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(calculationPeriodNumberOfDays);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("FutureValueNotional{");
    buf.append("value").append('=').append(value).append(',').append(' ');
    buf.append("valueDate").append('=').append(valueDate).append(',').append(' ');
    buf.append("calculationPeriodNumberOfDays").append('=').append(JodaBeanUtils.toString(calculationPeriodNumberOfDays));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FutureValueNotional}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code value} property.
     */
    private final MetaProperty<Double> value = DirectMetaProperty.ofImmutable(
        this, "value", FutureValueNotional.class, Double.class);
    /**
     * The meta-property for the {@code valueDate} property.
     */
    private final MetaProperty<LocalDate> valueDate = DirectMetaProperty.ofImmutable(
        this, "valueDate", FutureValueNotional.class, LocalDate.class);
    /**
     * The meta-property for the {@code calculationPeriodNumberOfDays} property.
     */
    private final MetaProperty<Integer> calculationPeriodNumberOfDays = DirectMetaProperty.ofImmutable(
        this, "calculationPeriodNumberOfDays", FutureValueNotional.class, Integer.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "value",
        "valueDate",
        "calculationPeriodNumberOfDays");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return value;
        case -766192449:  // valueDate
          return valueDate;
        case -846977407:  // calculationPeriodNumberOfDays
          return calculationPeriodNumberOfDays;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FutureValueNotional.Builder builder() {
      return new FutureValueNotional.Builder();
    }

    @Override
    public Class<? extends FutureValueNotional> beanType() {
      return FutureValueNotional.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code value} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> value() {
      return value;
    }

    /**
     * The meta-property for the {@code valueDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valueDate() {
      return valueDate;
    }

    /**
     * The meta-property for the {@code calculationPeriodNumberOfDays} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> calculationPeriodNumberOfDays() {
      return calculationPeriodNumberOfDays;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return ((FutureValueNotional) bean).value;
        case -766192449:  // valueDate
          return ((FutureValueNotional) bean).valueDate;
        case -846977407:  // calculationPeriodNumberOfDays
          return ((FutureValueNotional) bean).calculationPeriodNumberOfDays;
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
   * The bean-builder for {@code FutureValueNotional}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FutureValueNotional> {

    private Double value;
    private LocalDate valueDate;
    private Integer calculationPeriodNumberOfDays;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FutureValueNotional beanToCopy) {
      this.value = beanToCopy.value;
      this.valueDate = beanToCopy.valueDate;
      this.calculationPeriodNumberOfDays = beanToCopy.calculationPeriodNumberOfDays;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return value;
        case -766192449:  // valueDate
          return valueDate;
        case -846977407:  // calculationPeriodNumberOfDays
          return calculationPeriodNumberOfDays;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          this.value = (Double) newValue;
          break;
        case -766192449:  // valueDate
          this.valueDate = (LocalDate) newValue;
          break;
        case -846977407:  // calculationPeriodNumberOfDays
          this.calculationPeriodNumberOfDays = (Integer) newValue;
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
    public FutureValueNotional build() {
      return new FutureValueNotional(
          value,
          valueDate,
          calculationPeriodNumberOfDays);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the amount.
     * <p>
     * The future value notional amount, derived as per the standard formula.
     * @param value  the new value
     * @return this, for chaining, not null
     */
    public Builder value(Double value) {
      this.value = value;
      return this;
    }

    /**
     * Sets the value date.
     * <p>
     * This is the adjusted value date of the future value amount, which is the adjusted end date.
     * @param valueDate  the new value
     * @return this, for chaining, not null
     */
    public Builder valueDate(LocalDate valueDate) {
      this.valueDate = valueDate;
      return this;
    }

    /**
     * Sets the number of days in the calculation period.
     * <p>
     * This defines the number of days from the adjusted start date to the adjusted end date
     * as calculated by the day count.
     * @param calculationPeriodNumberOfDays  the new value
     * @return this, for chaining, not null
     */
    public Builder calculationPeriodNumberOfDays(Integer calculationPeriodNumberOfDays) {
      this.calculationPeriodNumberOfDays = calculationPeriodNumberOfDays;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("FutureValueNotional.Builder{");
      buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ');
      buf.append("valueDate").append('=').append(JodaBeanUtils.toString(valueDate)).append(',').append(' ');
      buf.append("calculationPeriodNumberOfDays").append('=').append(JodaBeanUtils.toString(calculationPeriodNumberOfDays));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
