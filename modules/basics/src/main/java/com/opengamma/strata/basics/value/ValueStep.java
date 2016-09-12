/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * A single step in the variation of a value over time.
 * <p>
 * A financial value, such as the notional or interest rate, may vary over time.
 * This class represents a single change in the value within {@link ValueSchedule}.
 * <p>
 * The date of the change is either specified explicitly, or in relative terms via an index.
 * The adjustment to the value can also be specified absolutely, or in relative terms.
 */
@BeanDefinition
public final class ValueStep
    implements ImmutableBean, Serializable {

  /**
   * The index of the schedule period boundary at which the change occurs.
   * <p>
   * This property is used to define the date that the step occurs in relative terms.
   * The date is identified by specifying the zero-based index of the schedule period boundary.
   * The change will occur at the start of the specified period.
   * Thus an index of zero is the start of the first period or initial stub.
   * The index must be one or greater, as a change is not permitted at the start of the first period.
   * <p>
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * A zero-based index of '2' would refer to start of the 3rd period, which would be 2013-02-01.
   */
  @PropertyDefinition(get = "optional")
  private final Integer periodIndex;
  /**
   * The date of the schedule period boundary at which the change occurs.
   * <p>
   * This property is used to define the date that the step occurs in absolute terms.
   * This must be one of the unadjusted dates in the schedule period schedule.
   * This is an unadjusted date and calculation period business day adjustments will apply.
   * <p>
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate date;
  /**
   * The value representing the change that occurs.
   * <p>
   * The adjustment can be an absolute value, or various kinds of relative values.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueAdjustment value;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that applies at the specified schedule period index.
   * <p>
   * This factory is used to define the date that the step occurs in relative terms.
   * The date is identified by specifying the zero-based index of the schedule period boundary.
   * The change will occur at the start of the specified period.
   * Thus an index of zero is the start of the first period or initial stub.
   * The index must be one or greater, as a change is not permitted at the start of the first period.
   * <p>
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * A zero-based index of '2' would refer to start of the 3rd period, which would be 2013-02-01.
   * <p>
   * The value may be absolute or relative, as per {@link ValueAdjustment}.
   * 
   * @param periodIndex  the index of the period of the value change
   * @param value  the adjustment to make to the value
   * @return the varying step
   */
  public static ValueStep of(int periodIndex, ValueAdjustment value) {
    return new ValueStep(periodIndex, null, value);
  }

  /**
   * Obtains an instance that applies at the specified date.
   * <p>
   * This factory obtains a step that causes the value to change at the specified date.
   * <p>
   * The value may be absolute or relative, as per {@link ValueAdjustment}.
   * 
   * @param date  the start date of the value change
   * @param value  the adjustment to make to the value
   * @return the varying step
   */
  public static ValueStep of(LocalDate date, ValueAdjustment value) {
    return new ValueStep(null, date, value);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the index of this value step in the schedule.
   * 
   * @param periods  the list of schedule periods
   * @return the index of the schedule period
   */
  int findIndex(List<SchedulePeriod> periods) {
    // either periodIndex or date is non-null, not both
    if (periodIndex != null) {
      // index based
      if (periodIndex >= periods.size()) {
        throw new IllegalArgumentException("ValueStep index is beyond last schedule period");
      }
      return periodIndex;
    } else {
      // date based, match one of the unadjusted period boundaries
      for (int i = 0; i < periods.size(); i++) {
        SchedulePeriod period = periods.get(i);
        if (period.getUnadjustedStartDate().equals(date)) {
          return i;
        }
      }
      // try adjusted boundaries instead of unadjusted ones
      for (int i = 0; i < periods.size(); i++) {
        SchedulePeriod period = periods.get(i);
        if (period.getStartDate().equals(date)) {
          return i;
        }
      }
      throw new IllegalArgumentException("ValueStep date does not match a period boundary: " + date);
    }
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (periodIndex == null && date == null) {
      throw new IllegalArgumentException("Either the 'periodIndex' or 'date' must be set");
    }
    if (periodIndex != null) {
      if (date != null) {
        throw new IllegalArgumentException("Either the 'periodIndex' or 'date' must be set, not both");
      }
      if (periodIndex < 1) {
        throw new IllegalArgumentException("The 'periodIndex' must not be zero or negative");
      }
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ValueStep}.
   * @return the meta-bean, not null
   */
  public static ValueStep.Meta meta() {
    return ValueStep.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ValueStep.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ValueStep.Builder builder() {
    return new ValueStep.Builder();
  }

  private ValueStep(
      Integer periodIndex,
      LocalDate date,
      ValueAdjustment value) {
    JodaBeanUtils.notNull(value, "value");
    this.periodIndex = periodIndex;
    this.date = date;
    this.value = value;
    validate();
  }

  @Override
  public ValueStep.Meta metaBean() {
    return ValueStep.Meta.INSTANCE;
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
   * Gets the index of the schedule period boundary at which the change occurs.
   * <p>
   * This property is used to define the date that the step occurs in relative terms.
   * The date is identified by specifying the zero-based index of the schedule period boundary.
   * The change will occur at the start of the specified period.
   * Thus an index of zero is the start of the first period or initial stub.
   * The index must be one or greater, as a change is not permitted at the start of the first period.
   * <p>
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * A zero-based index of '2' would refer to start of the 3rd period, which would be 2013-02-01.
   * @return the optional value of the property, not null
   */
  public OptionalInt getPeriodIndex() {
    return periodIndex != null ? OptionalInt.of(periodIndex) : OptionalInt.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date of the schedule period boundary at which the change occurs.
   * <p>
   * This property is used to define the date that the step occurs in absolute terms.
   * This must be one of the unadjusted dates in the schedule period schedule.
   * This is an unadjusted date and calculation period business day adjustments will apply.
   * <p>
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getDate() {
    return Optional.ofNullable(date);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value representing the change that occurs.
   * <p>
   * The adjustment can be an absolute value, or various kinds of relative values.
   * @return the value of the property, not null
   */
  public ValueAdjustment getValue() {
    return value;
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
      ValueStep other = (ValueStep) obj;
      return JodaBeanUtils.equal(periodIndex, other.periodIndex) &&
          JodaBeanUtils.equal(date, other.date) &&
          JodaBeanUtils.equal(value, other.value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(periodIndex);
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    hash = hash * 31 + JodaBeanUtils.hashCode(value);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ValueStep{");
    buf.append("periodIndex").append('=').append(periodIndex).append(',').append(' ');
    buf.append("date").append('=').append(date).append(',').append(' ');
    buf.append("value").append('=').append(JodaBeanUtils.toString(value));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ValueStep}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code periodIndex} property.
     */
    private final MetaProperty<Integer> periodIndex = DirectMetaProperty.ofImmutable(
        this, "periodIndex", ValueStep.class, Integer.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<LocalDate> date = DirectMetaProperty.ofImmutable(
        this, "date", ValueStep.class, LocalDate.class);
    /**
     * The meta-property for the {@code value} property.
     */
    private final MetaProperty<ValueAdjustment> value = DirectMetaProperty.ofImmutable(
        this, "value", ValueStep.class, ValueAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "periodIndex",
        "date",
        "value");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -980601967:  // periodIndex
          return periodIndex;
        case 3076014:  // date
          return date;
        case 111972721:  // value
          return value;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ValueStep.Builder builder() {
      return new ValueStep.Builder();
    }

    @Override
    public Class<? extends ValueStep> beanType() {
      return ValueStep.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code periodIndex} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> periodIndex() {
      return periodIndex;
    }

    /**
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> date() {
      return date;
    }

    /**
     * The meta-property for the {@code value} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueAdjustment> value() {
      return value;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -980601967:  // periodIndex
          return ((ValueStep) bean).periodIndex;
        case 3076014:  // date
          return ((ValueStep) bean).date;
        case 111972721:  // value
          return ((ValueStep) bean).getValue();
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
   * The bean-builder for {@code ValueStep}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ValueStep> {

    private Integer periodIndex;
    private LocalDate date;
    private ValueAdjustment value;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ValueStep beanToCopy) {
      this.periodIndex = beanToCopy.periodIndex;
      this.date = beanToCopy.date;
      this.value = beanToCopy.getValue();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -980601967:  // periodIndex
          return periodIndex;
        case 3076014:  // date
          return date;
        case 111972721:  // value
          return value;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -980601967:  // periodIndex
          this.periodIndex = (Integer) newValue;
          break;
        case 3076014:  // date
          this.date = (LocalDate) newValue;
          break;
        case 111972721:  // value
          this.value = (ValueAdjustment) newValue;
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
    public ValueStep build() {
      return new ValueStep(
          periodIndex,
          date,
          value);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index of the schedule period boundary at which the change occurs.
     * <p>
     * This property is used to define the date that the step occurs in relative terms.
     * The date is identified by specifying the zero-based index of the schedule period boundary.
     * The change will occur at the start of the specified period.
     * Thus an index of zero is the start of the first period or initial stub.
     * The index must be one or greater, as a change is not permitted at the start of the first period.
     * <p>
     * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
     * A zero-based index of '2' would refer to start of the 3rd period, which would be 2013-02-01.
     * @param periodIndex  the new value
     * @return this, for chaining, not null
     */
    public Builder periodIndex(Integer periodIndex) {
      this.periodIndex = periodIndex;
      return this;
    }

    /**
     * Sets the date of the schedule period boundary at which the change occurs.
     * <p>
     * This property is used to define the date that the step occurs in absolute terms.
     * This must be one of the unadjusted dates in the schedule period schedule.
     * This is an unadjusted date and calculation period business day adjustments will apply.
     * <p>
     * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
     * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
     * @param date  the new value
     * @return this, for chaining, not null
     */
    public Builder date(LocalDate date) {
      this.date = date;
      return this;
    }

    /**
     * Sets the value representing the change that occurs.
     * <p>
     * The adjustment can be an absolute value, or various kinds of relative values.
     * @param value  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder value(ValueAdjustment value) {
      JodaBeanUtils.notNull(value, "value");
      this.value = value;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ValueStep.Builder{");
      buf.append("periodIndex").append('=').append(JodaBeanUtils.toString(periodIndex)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ');
      buf.append("value").append('=').append(JodaBeanUtils.toString(value));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
