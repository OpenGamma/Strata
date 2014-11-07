/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
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

import com.google.common.collect.ComparisonChain;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.date.DayCount.ScheduleInfo;
import com.opengamma.collect.ArgChecker;

/**
 * A period in a schedule.
 * <p>
 * This consists of a single period (date range) within a schedule.
 * This is typically used as the basis for financial calculations, such as accrual of interest.
 * <p>
 * Two pairs of dates are provided, start/end and unadjustedStart/unadjustedEnd.
 * The period itself runs from {@code startDate} to {@code endDate}.
 * The {@code unadjustedStartDate} and {@code unadjustedEndDate} are the dates used to
 * calculate the {@code startDate} and {@code endDate} when applying business day adjustment.
 * <p>
 * For example, consider a schedule that has periods every three months on the 10th of the month.
 * From time to time, the scheduled date will be a weekend or holiday.
 * In this case, a rule may apply moving the date to a valid business day.
 * If this happens, then the "unadjusted" date is the original date in the periodic schedule
 * and the "adjusted" date is the related valid business day.
 * Note that not all schedules apply a business day adjustment.
 */
@BeanDefinition
public final class SchedulePeriod
    implements ScheduleInfo, ImmutableBean, Comparable<SchedulePeriod>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The type of this period.
   * <p>
   * This defines whether this period is an initial, final, term or normal period.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SchedulePeriodType type;
  /**
   * The start date of this period, used for financial calculations such as interest accrual.
   * <p>
   * The first date in the schedule period, typically treated as inclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date of this period, used for financial calculations such as interest accrual.
   * <p>
   * The last date in the schedule period, typically treated as exclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate endDate;
  /**
   * The unadjusted start date.
   * <p>
   * The start date before any business day adjustment.
   * If the schedule adjusts for business days, then this is typically the regular periodic date.
   * If the schedule does not adjust for business days, then this is the same as the start date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedStartDate;
  /**
   * The unadjusted end date.
   * <p>
   * The end date before any business day adjustment.
   * If the schedule adjusts for business days, then this is typically the regular periodic date.
   * If the schedule does not adjust for business days, then this is the same as the end date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedEndDate;
  /**
   * The periodic frequency used when building the schedule.
   * <p>
   * If the schedule was not built from a regular periodic frequency, then the frequency should
   * be the period between the unadjusted start and end date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Frequency frequency;
  /**
   * The roll convention used when building the schedule.
   * <p>
   * If the schedule was not built from a regular periodic frequency, then the convention should be 'None'.
   */
  @PropertyDefinition(validate = "notNull")
  private final RollConvention rollConvention;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the adjusted and unadjusted dates.
   * 
   * @param type  the period type
   * @param startDate  the start date, used for financial calculations such as interest accrual
   * @param endDate  the end date, used for financial calculations such as interest accrual
   * @param unadjustedStartDate  the unadjusted start date
   * @param unadjustedEndDate  the adjusted end date
   * @param frequency  the frequency used to create the schedule
   * @param rollConvention  the roll convention used to create the schedule
   * @return the period
   */
  public static SchedulePeriod of(
      SchedulePeriodType type,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      Frequency frequency,
      RollConvention rollConvention) {
    return new SchedulePeriod(
        type, startDate, endDate, unadjustedStartDate, unadjustedEndDate, frequency, rollConvention);
  }

  /**
   * Obtains an instance from the dates.
   * <p>
   * This factory is used when there is no business day adjustment of schedule dates.
   * 
   * @param type  the period type
   * @param startDate  the start date, used for financial calculations such as interest accrual
   * @param endDate  the end date, used for financial calculations such as interest accrual
   * @param frequency  the frequency used to create the schedule
   * @param rollConvention  the roll convention used to create the schedule
   * @return the period
   */
  public static SchedulePeriod of(
      SchedulePeriodType type,
      LocalDate startDate,
      LocalDate endDate,
      Frequency frequency,
      RollConvention rollConvention) {
    return new SchedulePeriod(
        type, startDate, endDate, startDate, endDate, frequency, rollConvention);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(unadjustedStartDate, unadjustedEndDate, "unadjustedStartDate", "unadjustedEndDate");
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the specified date is the end of the overall schedule.
   * <p>
   * This is used to check for the maturity/termination date.
   * 
   * @param date  the date to check
   * @return true if the date is the last date in the overall schedule
   */
  @Override
  public boolean isScheduleEndDate(LocalDate date) {
    return type == SchedulePeriodType.FINAL && date.equals(endDate);
  }

  /**
   * Checks if the end of month convention is in use.
   * <p>
   * If true then when building a schedule, dates will be at the end-of-month if the
   * first date in the series is at the end-of-month.
   * 
   * @return true if the end of month convention is in use
   */
  @Override
  public boolean isEndOfMonthConvention() {
    return rollConvention == RollConventions.EOM;
  }

  /**
   * Checks if this period is an initial or final stub.
   * <p>
   * Only an initial or final period can be a stub.
   * The result is true if the length of the period differs from that calculated by
   * the frequency and roll convention.
   * 
   * @return true if the period is an initial or final stub
   */
  public boolean isStub() {
    if (type == SchedulePeriodType.INITIAL) {
      return !rollConvention.previous(unadjustedEndDate, frequency).equals(unadjustedStartDate);
    } else if (type == SchedulePeriodType.FINAL) {
      return !rollConvention.next(unadjustedStartDate, frequency).equals(unadjustedEndDate);
    } else {
      return false;
    }
  }

  /**
   * Calculates the year fraction using the specified day count.
   * <p>
   * Additional information from this period is made available to the day count algorithm.
   * 
   * @param dayCount  the day count convention
   * @return the year fraction, calculated via the day count
   */
  public double yearFraction(DayCount dayCount) {
    ArgChecker.notNull(dayCount, "dayCount");
    return dayCount.getDayCountFraction(startDate, endDate, this);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this period to another by unadjusted start date, then unadjusted end date.
   * 
   * @param other  the other period
   * @return the comparison value
   */
  @Override
  public int compareTo(SchedulePeriod other) {
    return ComparisonChain.start()
        .compare(unadjustedStartDate, other.unadjustedStartDate)
        .compare(unadjustedEndDate, other.unadjustedEndDate)
        .result();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SchedulePeriod}.
   * @return the meta-bean, not null
   */
  public static SchedulePeriod.Meta meta() {
    return SchedulePeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SchedulePeriod.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SchedulePeriod.Builder builder() {
    return new SchedulePeriod.Builder();
  }

  private SchedulePeriod(
      SchedulePeriodType type,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      Frequency frequency,
      RollConvention rollConvention) {
    JodaBeanUtils.notNull(type, "type");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
    JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
    JodaBeanUtils.notNull(frequency, "frequency");
    JodaBeanUtils.notNull(rollConvention, "rollConvention");
    this.type = type;
    this.startDate = startDate;
    this.endDate = endDate;
    this.unadjustedStartDate = unadjustedStartDate;
    this.unadjustedEndDate = unadjustedEndDate;
    this.frequency = frequency;
    this.rollConvention = rollConvention;
    validate();
  }

  @Override
  public SchedulePeriod.Meta metaBean() {
    return SchedulePeriod.Meta.INSTANCE;
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
   * Gets the type of this period.
   * <p>
   * This defines whether this period is an initial, final, term or normal period.
   * @return the value of the property, not null
   */
  @Override
  public SchedulePeriodType getType() {
    return type;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of this period, used for financial calculations such as interest accrual.
   * <p>
   * The first date in the schedule period, typically treated as inclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of this period, used for financial calculations such as interest accrual.
   * <p>
   * The last date in the schedule period, typically treated as exclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted start date.
   * <p>
   * The start date before any business day adjustment.
   * If the schedule adjusts for business days, then this is typically the regular periodic date.
   * If the schedule does not adjust for business days, then this is the same as the start date.
   * @return the value of the property, not null
   */
  public LocalDate getUnadjustedStartDate() {
    return unadjustedStartDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted end date.
   * <p>
   * The end date before any business day adjustment.
   * If the schedule adjusts for business days, then this is typically the regular periodic date.
   * If the schedule does not adjust for business days, then this is the same as the end date.
   * @return the value of the property, not null
   */
  public LocalDate getUnadjustedEndDate() {
    return unadjustedEndDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the periodic frequency used when building the schedule.
   * <p>
   * If the schedule was not built from a regular periodic frequency, then the frequency should
   * be the period between the unadjusted start and end date.
   * @return the value of the property, not null
   */
  @Override
  public Frequency getFrequency() {
    return frequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the roll convention used when building the schedule.
   * <p>
   * If the schedule was not built from a regular periodic frequency, then the convention should be 'None'.
   * @return the value of the property, not null
   */
  public RollConvention getRollConvention() {
    return rollConvention;
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
      SchedulePeriod other = (SchedulePeriod) obj;
      return JodaBeanUtils.equal(getType(), other.getType()) &&
          JodaBeanUtils.equal(getStartDate(), other.getStartDate()) &&
          JodaBeanUtils.equal(getEndDate(), other.getEndDate()) &&
          JodaBeanUtils.equal(getUnadjustedStartDate(), other.getUnadjustedStartDate()) &&
          JodaBeanUtils.equal(getUnadjustedEndDate(), other.getUnadjustedEndDate()) &&
          JodaBeanUtils.equal(getFrequency(), other.getFrequency()) &&
          JodaBeanUtils.equal(getRollConvention(), other.getRollConvention());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getType());
    hash += hash * 31 + JodaBeanUtils.hashCode(getStartDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getEndDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getUnadjustedStartDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getUnadjustedEndDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFrequency());
    hash += hash * 31 + JodaBeanUtils.hashCode(getRollConvention());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("SchedulePeriod{");
    buf.append("type").append('=').append(getType()).append(',').append(' ');
    buf.append("startDate").append('=').append(getStartDate()).append(',').append(' ');
    buf.append("endDate").append('=').append(getEndDate()).append(',').append(' ');
    buf.append("unadjustedStartDate").append('=').append(getUnadjustedStartDate()).append(',').append(' ');
    buf.append("unadjustedEndDate").append('=').append(getUnadjustedEndDate()).append(',').append(' ');
    buf.append("frequency").append('=').append(getFrequency()).append(',').append(' ');
    buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(getRollConvention()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SchedulePeriod}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code type} property.
     */
    private final MetaProperty<SchedulePeriodType> type = DirectMetaProperty.ofImmutable(
        this, "type", SchedulePeriod.class, SchedulePeriodType.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", SchedulePeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", SchedulePeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedStartDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedStartDate", SchedulePeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedEndDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedEndDate", SchedulePeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code frequency} property.
     */
    private final MetaProperty<Frequency> frequency = DirectMetaProperty.ofImmutable(
        this, "frequency", SchedulePeriod.class, Frequency.class);
    /**
     * The meta-property for the {@code rollConvention} property.
     */
    private final MetaProperty<RollConvention> rollConvention = DirectMetaProperty.ofImmutable(
        this, "rollConvention", SchedulePeriod.class, RollConvention.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "type",
        "startDate",
        "endDate",
        "unadjustedStartDate",
        "unadjustedEndDate",
        "frequency",
        "rollConvention");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return type;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -70023844:  // frequency
          return frequency;
        case -10223666:  // rollConvention
          return rollConvention;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SchedulePeriod.Builder builder() {
      return new SchedulePeriod.Builder();
    }

    @Override
    public Class<? extends SchedulePeriod> beanType() {
      return SchedulePeriod.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code type} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SchedulePeriodType> type() {
      return type;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> unadjustedStartDate() {
      return unadjustedStartDate;
    }

    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> unadjustedEndDate() {
      return unadjustedEndDate;
    }

    /**
     * The meta-property for the {@code frequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> frequency() {
      return frequency;
    }

    /**
     * The meta-property for the {@code rollConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RollConvention> rollConvention() {
      return rollConvention;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return ((SchedulePeriod) bean).getType();
        case -2129778896:  // startDate
          return ((SchedulePeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((SchedulePeriod) bean).getEndDate();
        case 1457691881:  // unadjustedStartDate
          return ((SchedulePeriod) bean).getUnadjustedStartDate();
        case 31758114:  // unadjustedEndDate
          return ((SchedulePeriod) bean).getUnadjustedEndDate();
        case -70023844:  // frequency
          return ((SchedulePeriod) bean).getFrequency();
        case -10223666:  // rollConvention
          return ((SchedulePeriod) bean).getRollConvention();
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
   * The bean-builder for {@code SchedulePeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<SchedulePeriod> {

    private SchedulePeriodType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate unadjustedStartDate;
    private LocalDate unadjustedEndDate;
    private Frequency frequency;
    private RollConvention rollConvention;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(SchedulePeriod beanToCopy) {
      this.type = beanToCopy.getType();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.unadjustedStartDate = beanToCopy.getUnadjustedStartDate();
      this.unadjustedEndDate = beanToCopy.getUnadjustedEndDate();
      this.frequency = beanToCopy.getFrequency();
      this.rollConvention = beanToCopy.getRollConvention();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return type;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -70023844:  // frequency
          return frequency;
        case -10223666:  // rollConvention
          return rollConvention;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          this.type = (SchedulePeriodType) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case 1457691881:  // unadjustedStartDate
          this.unadjustedStartDate = (LocalDate) newValue;
          break;
        case 31758114:  // unadjustedEndDate
          this.unadjustedEndDate = (LocalDate) newValue;
          break;
        case -70023844:  // frequency
          this.frequency = (Frequency) newValue;
          break;
        case -10223666:  // rollConvention
          this.rollConvention = (RollConvention) newValue;
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
    public SchedulePeriod build() {
      return new SchedulePeriod(
          type,
          startDate,
          endDate,
          unadjustedStartDate,
          unadjustedEndDate,
          frequency,
          rollConvention);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code type} property in the builder.
     * @param type  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder type(SchedulePeriodType type) {
      JodaBeanUtils.notNull(type, "type");
      this.type = type;
      return this;
    }

    /**
     * Sets the {@code startDate} property in the builder.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the {@code endDate} property in the builder.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the {@code unadjustedStartDate} property in the builder.
     * @param unadjustedStartDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedStartDate(LocalDate unadjustedStartDate) {
      JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
      this.unadjustedStartDate = unadjustedStartDate;
      return this;
    }

    /**
     * Sets the {@code unadjustedEndDate} property in the builder.
     * @param unadjustedEndDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedEndDate(LocalDate unadjustedEndDate) {
      JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
      this.unadjustedEndDate = unadjustedEndDate;
      return this;
    }

    /**
     * Sets the {@code frequency} property in the builder.
     * @param frequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder frequency(Frequency frequency) {
      JodaBeanUtils.notNull(frequency, "frequency");
      this.frequency = frequency;
      return this;
    }

    /**
     * Sets the {@code rollConvention} property in the builder.
     * @param rollConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rollConvention(RollConvention rollConvention) {
      JodaBeanUtils.notNull(rollConvention, "rollConvention");
      this.rollConvention = rollConvention;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("SchedulePeriod.Builder{");
      buf.append("type").append('=').append(JodaBeanUtils.toString(type)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ');
      buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate)).append(',').append(' ');
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
