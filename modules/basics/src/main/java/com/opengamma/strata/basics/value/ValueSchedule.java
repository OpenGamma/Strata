/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * A value that can vary over time.
 * <p>
 * This represents a single initial value and any adjustments over the lifetime of a trade.
 * Adjustments may be specified in absolute or relative terms.
 * <p>
 * The value is specified as a {@code double} with the context adding additional meaning.
 * If the value represents an amount of money then the currency is specified separately.
 * If the value represents a rate then a 5% rate is expressed as 0.05.
 */
@BeanDefinition
public final class ValueSchedule
    implements ImmutableBean, Serializable {

  /**
   * A value schedule that always has the value zero.
   */
  public static final ValueSchedule ALWAYS_0 = ValueSchedule.of(0);
  /**
   * A value schedule that always has the value one.
   */
  public static final ValueSchedule ALWAYS_1 = ValueSchedule.of(1);

  /**
   * The initial value.
   * <p>
   * This is used for the lifetime of the trade unless specifically varied.
   */
  @PropertyDefinition
  private final double initialValue;
  /**
   * The steps defining the change in the value.
   * <p>
   * Each step consists of a key locating the date of the change and the adjustment that occurs.
   */
  @PropertyDefinition(validate = "notNull")
  private final List<ValueStep> steps;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a single value that does not change over time.
   * 
   * @param value  a single value that does not change over time
   * @return the value schedule
   */
  public static ValueSchedule of(double value) {
    return new ValueSchedule(value, ImmutableList.of());
  }

  /**
   * Obtains an instance from an initial value and a list of changes.
   * <p>
   * Each step fully defines a single change in the value.
   * The date of each change can be specified as an absolute date or in relative terms.
   * 
   * @param initialValue  the initial value used for the first period
   * @param steps  the full definition of how the value changes over time
   * @return the value schedule
   */
  public static ValueSchedule of(double initialValue, ValueStep... steps) {
    return new ValueSchedule(initialValue, ImmutableList.copyOf(steps));
  }

  /**
   * Obtains an instance from an initial value and a list of changes.
   * <p>
   * Each step fully defines a single change in the value.
   * The date of each change can be specified as an absolute date or in relative terms.
   * 
   * @param initialValue  the initial value used for the first period
   * @param steps  the full definition of how the value changes over time
   * @return the value schedule
   */
  public static ValueSchedule of(double initialValue, List<ValueStep> steps) {
    return new ValueSchedule(initialValue, ImmutableList.copyOf(steps));
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves the value and adjustments against a specific schedule.
   * <p>
   * This converts a schedule into a list of values, one for each schedule period.
   * <p>
   * The output list is immutable and matches the input list.
   * Use {@link Doubles#toArray} to efficiently access the list as a {@code double[]}
   * (without breaking encapsulation or immutability).
   * 
   * @param periods  the list of schedule periods
   * @return the values, one for each schedule period
   */
  public List<Double> resolveValues(List<SchedulePeriod> periods) {
    int size = periods.size();
    double[] result = new double[size];
    // handle simple case
    if (steps.size() == 0) {
      Arrays.fill(result, initialValue);
    } else {
      // expand ValueStep to array of adjustments matching the periods
      // the steps are not sorted, so use fixed size array to absorb incoming data
      ValueAdjustment[] expandedSteps = new ValueAdjustment[size];
      for (ValueStep step : steps) {
        int index = step.findIndex(periods);
        if (expandedSteps[index] != null && !expandedSteps[index].equals(step.getValue())) {
          throw new IllegalArgumentException("Two ValueStep instances resolve to the same schedule period");
        }
        expandedSteps[index] = step.getValue();
      }
      // apply each adjustment
      double value = initialValue;
      for (int i = 0; i < size; i++) {
        if (expandedSteps[i] != null) {
          value = expandedSteps[i].adjust(value);
        }
        result[i] = value;
      }
    }
    // result array is wrapped, not copied, which is OK as scope of result ends here
    return Doubles.asList(result);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ValueSchedule}.
   * @return the meta-bean, not null
   */
  public static ValueSchedule.Meta meta() {
    return ValueSchedule.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ValueSchedule.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ValueSchedule.Builder builder() {
    return new ValueSchedule.Builder();
  }

  private ValueSchedule(
      double initialValue,
      List<ValueStep> steps) {
    JodaBeanUtils.notNull(steps, "steps");
    this.initialValue = initialValue;
    this.steps = ImmutableList.copyOf(steps);
  }

  @Override
  public ValueSchedule.Meta metaBean() {
    return ValueSchedule.Meta.INSTANCE;
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
   * Gets the initial value.
   * <p>
   * This is used for the lifetime of the trade unless specifically varied.
   * @return the value of the property
   */
  public double getInitialValue() {
    return initialValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the steps defining the change in the value.
   * <p>
   * Each step consists of a key locating the date of the change and the adjustment that occurs.
   * @return the value of the property, not null
   */
  public List<ValueStep> getSteps() {
    return steps;
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
      ValueSchedule other = (ValueSchedule) obj;
      return JodaBeanUtils.equal(initialValue, other.initialValue) &&
          JodaBeanUtils.equal(steps, other.steps);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(initialValue);
    hash = hash * 31 + JodaBeanUtils.hashCode(steps);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ValueSchedule{");
    buf.append("initialValue").append('=').append(initialValue).append(',').append(' ');
    buf.append("steps").append('=').append(JodaBeanUtils.toString(steps));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ValueSchedule}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code initialValue} property.
     */
    private final MetaProperty<Double> initialValue = DirectMetaProperty.ofImmutable(
        this, "initialValue", ValueSchedule.class, Double.TYPE);
    /**
     * The meta-property for the {@code steps} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<ValueStep>> steps = DirectMetaProperty.ofImmutable(
        this, "steps", ValueSchedule.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "initialValue",
        "steps");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -418368371:  // initialValue
          return initialValue;
        case 109761319:  // steps
          return steps;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ValueSchedule.Builder builder() {
      return new ValueSchedule.Builder();
    }

    @Override
    public Class<? extends ValueSchedule> beanType() {
      return ValueSchedule.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code initialValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> initialValue() {
      return initialValue;
    }

    /**
     * The meta-property for the {@code steps} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<ValueStep>> steps() {
      return steps;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -418368371:  // initialValue
          return ((ValueSchedule) bean).getInitialValue();
        case 109761319:  // steps
          return ((ValueSchedule) bean).getSteps();
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
   * The bean-builder for {@code ValueSchedule}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ValueSchedule> {

    private double initialValue;
    private List<ValueStep> steps = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ValueSchedule beanToCopy) {
      this.initialValue = beanToCopy.getInitialValue();
      this.steps = ImmutableList.copyOf(beanToCopy.getSteps());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -418368371:  // initialValue
          return initialValue;
        case 109761319:  // steps
          return steps;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -418368371:  // initialValue
          this.initialValue = (Double) newValue;
          break;
        case 109761319:  // steps
          this.steps = (List<ValueStep>) newValue;
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
    public ValueSchedule build() {
      return new ValueSchedule(
          initialValue,
          steps);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the initial value.
     * <p>
     * This is used for the lifetime of the trade unless specifically varied.
     * @param initialValue  the new value
     * @return this, for chaining, not null
     */
    public Builder initialValue(double initialValue) {
      this.initialValue = initialValue;
      return this;
    }

    /**
     * Sets the steps defining the change in the value.
     * <p>
     * Each step consists of a key locating the date of the change and the adjustment that occurs.
     * @param steps  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder steps(List<ValueStep> steps) {
      JodaBeanUtils.notNull(steps, "steps");
      this.steps = steps;
      return this;
    }

    /**
     * Sets the {@code steps} property in the builder
     * from an array of objects.
     * @param steps  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder steps(ValueStep... steps) {
      return steps(ImmutableList.copyOf(steps));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ValueSchedule.Builder{");
      buf.append("initialValue").append('=').append(JodaBeanUtils.toString(initialValue)).append(',').append(' ');
      buf.append("steps").append('=').append(JodaBeanUtils.toString(steps));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
