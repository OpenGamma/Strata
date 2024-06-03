/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * A sequence of steps that vary a value over time.
 * <p>
 * A financial value, such as the notional or interest rate, may vary over time.
 * This class represents a sequence of changes in the value within {@link ValueSchedule}.
 * <p>
 * The sequence is defined by a start date, end date and frequency.
 * The adjustment at each step is defined using {@link ValueAdjustment}.
 */
@BeanDefinition(builderScope = "private")
public final class ValueStepSequence
    implements ImmutableBean, Serializable {

  /**
   * The first date in the sequence.
   * <p>
   * This sequence will change the value on this date, but not before.
   * This must be one of the unadjusted dates in the schedule period schedule.
   * <p>
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate firstStepDate;
  /**
   * The last date in the sequence.
   * <p>
   * This sequence will change the value on this date, but not after.
   * This must be one of the unadjusted dates in the schedule period schedule.
   * <p>
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * The date '2015-02-01' is an unadjusted schedule period boundary, and so may be specified here.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate lastStepDate;
  /**
   * The frequency of the sequence.
   * <p>
   * This sequence will change the value on each date between the start and end defined by this frequency.
   * The frequency is interpreted relative to the frequency of a {@link Schedule}.
   * It must be equal or greater than the related schedule.
   */
  @PropertyDefinition(validate = "notNull")
  private final Frequency frequency;
  /**
   * The adjustment representing the change that occurs at each step.
   * <p>
   * The adjustment type must not be 'Replace'.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueAdjustment adjustment;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the dates, frequency and change.
   * 
   * @param firstStepDate  the first date of the sequence
   * @param lastStepDate  the last date of the sequence
   * @param frequency  the frequency of changes
   * @param adjustment  the adjustment at each step
   * @return the varying step
   */
  public static ValueStepSequence of(
      LocalDate firstStepDate,
      LocalDate lastStepDate,
      Frequency frequency,
      ValueAdjustment adjustment) {

    return new ValueStepSequence(firstStepDate, lastStepDate, frequency, adjustment);
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderOrEqual(firstStepDate, lastStepDate, "firstStepDate", "lastStepDate");
    ArgChecker.isTrue(adjustment.getType() != ValueAdjustmentType.REPLACE, "ValueAdjustmentType must not be 'Replace'");
  }

  //-------------------------------------------------------------------------
  /**
   * Resolves the sequence to a list of steps.
   * 
   * @param existingSteps  the existing list of steps
   * @param rollConv  the roll convention
   * @return the steps
   */
  List<ValueStep> resolve(List<ValueStep> existingSteps, RollConvention rollConv) {
    ImmutableList.Builder<ValueStep> steps = ImmutableList.builder();
    steps.addAll(existingSteps);
    LocalDate prev = rollConv.adjust(firstStepDate);
    LocalDate date = rollConv.adjust(firstStepDate);
    LocalDate adjustedLastStepDate = rollConv.adjust(lastStepDate);
    while (!date.isAfter(adjustedLastStepDate)) {
      steps.add(ValueStep.of(date, adjustment));
      prev = date;
      date = rollConv.next(date, frequency);
    }
    if (!prev.equals(adjustedLastStepDate)) {
      throw new IllegalArgumentException(Messages.format(
          "ValueStepSequence lastStepDate did not match frequency '{}' using roll convention '{}', {} != {}",
          frequency, rollConv, adjustedLastStepDate, prev));
    }
    return steps.build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ValueStepSequence}.
   * @return the meta-bean, not null
   */
  public static ValueStepSequence.Meta meta() {
    return ValueStepSequence.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ValueStepSequence.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ValueStepSequence(
      LocalDate firstStepDate,
      LocalDate lastStepDate,
      Frequency frequency,
      ValueAdjustment adjustment) {
    JodaBeanUtils.notNull(firstStepDate, "firstStepDate");
    JodaBeanUtils.notNull(lastStepDate, "lastStepDate");
    JodaBeanUtils.notNull(frequency, "frequency");
    JodaBeanUtils.notNull(adjustment, "adjustment");
    this.firstStepDate = firstStepDate;
    this.lastStepDate = lastStepDate;
    this.frequency = frequency;
    this.adjustment = adjustment;
    validate();
  }

  @Override
  public ValueStepSequence.Meta metaBean() {
    return ValueStepSequence.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first date in the sequence.
   * <p>
   * This sequence will change the value on this date, but not before.
   * This must be one of the unadjusted dates in the schedule period schedule.
   * <p>
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * The date '2013-02-01' is an unadjusted schedule period boundary, and so may be specified here.
   * @return the value of the property, not null
   */
  public LocalDate getFirstStepDate() {
    return firstStepDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last date in the sequence.
   * <p>
   * This sequence will change the value on this date, but not after.
   * This must be one of the unadjusted dates in the schedule period schedule.
   * <p>
   * For example, consider a 5 year swap from 2012-02-01 to 2017-02-01 with 6 month frequency.
   * The date '2015-02-01' is an unadjusted schedule period boundary, and so may be specified here.
   * @return the value of the property, not null
   */
  public LocalDate getLastStepDate() {
    return lastStepDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the frequency of the sequence.
   * <p>
   * This sequence will change the value on each date between the start and end defined by this frequency.
   * The frequency is interpreted relative to the frequency of a {@link Schedule}.
   * It must be equal or greater than the related schedule.
   * @return the value of the property, not null
   */
  public Frequency getFrequency() {
    return frequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment representing the change that occurs at each step.
   * <p>
   * The adjustment type must not be 'Replace'.
   * @return the value of the property, not null
   */
  public ValueAdjustment getAdjustment() {
    return adjustment;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ValueStepSequence other = (ValueStepSequence) obj;
      return JodaBeanUtils.equal(firstStepDate, other.firstStepDate) &&
          JodaBeanUtils.equal(lastStepDate, other.lastStepDate) &&
          JodaBeanUtils.equal(frequency, other.frequency) &&
          JodaBeanUtils.equal(adjustment, other.adjustment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(firstStepDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(lastStepDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(frequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustment);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("ValueStepSequence{");
    buf.append("firstStepDate").append('=').append(JodaBeanUtils.toString(firstStepDate)).append(',').append(' ');
    buf.append("lastStepDate").append('=').append(JodaBeanUtils.toString(lastStepDate)).append(',').append(' ');
    buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
    buf.append("adjustment").append('=').append(JodaBeanUtils.toString(adjustment));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ValueStepSequence}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code firstStepDate} property.
     */
    private final MetaProperty<LocalDate> firstStepDate = DirectMetaProperty.ofImmutable(
        this, "firstStepDate", ValueStepSequence.class, LocalDate.class);
    /**
     * The meta-property for the {@code lastStepDate} property.
     */
    private final MetaProperty<LocalDate> lastStepDate = DirectMetaProperty.ofImmutable(
        this, "lastStepDate", ValueStepSequence.class, LocalDate.class);
    /**
     * The meta-property for the {@code frequency} property.
     */
    private final MetaProperty<Frequency> frequency = DirectMetaProperty.ofImmutable(
        this, "frequency", ValueStepSequence.class, Frequency.class);
    /**
     * The meta-property for the {@code adjustment} property.
     */
    private final MetaProperty<ValueAdjustment> adjustment = DirectMetaProperty.ofImmutable(
        this, "adjustment", ValueStepSequence.class, ValueAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "firstStepDate",
        "lastStepDate",
        "frequency",
        "adjustment");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1025397910:  // firstStepDate
          return firstStepDate;
        case -292412080:  // lastStepDate
          return lastStepDate;
        case -70023844:  // frequency
          return frequency;
        case 1977085293:  // adjustment
          return adjustment;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ValueStepSequence> builder() {
      return new ValueStepSequence.Builder();
    }

    @Override
    public Class<? extends ValueStepSequence> beanType() {
      return ValueStepSequence.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code firstStepDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> firstStepDate() {
      return firstStepDate;
    }

    /**
     * The meta-property for the {@code lastStepDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> lastStepDate() {
      return lastStepDate;
    }

    /**
     * The meta-property for the {@code frequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> frequency() {
      return frequency;
    }

    /**
     * The meta-property for the {@code adjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueAdjustment> adjustment() {
      return adjustment;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1025397910:  // firstStepDate
          return ((ValueStepSequence) bean).getFirstStepDate();
        case -292412080:  // lastStepDate
          return ((ValueStepSequence) bean).getLastStepDate();
        case -70023844:  // frequency
          return ((ValueStepSequence) bean).getFrequency();
        case 1977085293:  // adjustment
          return ((ValueStepSequence) bean).getAdjustment();
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
   * The bean-builder for {@code ValueStepSequence}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<ValueStepSequence> {

    private LocalDate firstStepDate;
    private LocalDate lastStepDate;
    private Frequency frequency;
    private ValueAdjustment adjustment;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1025397910:  // firstStepDate
          return firstStepDate;
        case -292412080:  // lastStepDate
          return lastStepDate;
        case -70023844:  // frequency
          return frequency;
        case 1977085293:  // adjustment
          return adjustment;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1025397910:  // firstStepDate
          this.firstStepDate = (LocalDate) newValue;
          break;
        case -292412080:  // lastStepDate
          this.lastStepDate = (LocalDate) newValue;
          break;
        case -70023844:  // frequency
          this.frequency = (Frequency) newValue;
          break;
        case 1977085293:  // adjustment
          this.adjustment = (ValueAdjustment) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public ValueStepSequence build() {
      return new ValueStepSequence(
          firstStepDate,
          lastStepDate,
          frequency,
          adjustment);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ValueStepSequence.Builder{");
      buf.append("firstStepDate").append('=').append(JodaBeanUtils.toString(firstStepDate)).append(',').append(' ');
      buf.append("lastStepDate").append('=').append(JodaBeanUtils.toString(lastStepDate)).append(',').append(' ');
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
      buf.append("adjustment").append('=').append(JodaBeanUtils.toString(adjustment));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
