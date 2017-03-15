/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;

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
    implements ImmutableBean, Comparable<SchedulePeriod>, Serializable {

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
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The unadjusted start date.
   * <p>
   * The start date before any business day adjustment.
   * If the schedule adjusts for business days, then this is typically the regular periodic date.
   * If the schedule does not adjust for business days, then this is the same as the start date.
   * <p>
   * When building, this will default to the start date if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedStartDate;
  /**
   * The unadjusted end date.
   * <p>
   * The end date before any business day adjustment.
   * If the schedule adjusts for business days, then this is typically the regular periodic date.
   * If the schedule does not adjust for business days, then this is the same as the end date.
   * <p>
   * When building, this will default to the end date if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedEndDate;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the adjusted and unadjusted dates.
   * 
   * @param startDate  the start date, used for financial calculations such as interest accrual
   * @param endDate  the end date, used for financial calculations such as interest accrual
   * @param unadjustedStartDate  the unadjusted start date
   * @param unadjustedEndDate  the adjusted end date
   * @return the period
   */
  public static SchedulePeriod of(
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate) {
    return new SchedulePeriod(startDate, endDate, unadjustedStartDate, unadjustedEndDate);
  }

  /**
   * Obtains an instance from two dates.
   * <p>
   * This factory is used when there is no business day adjustment of schedule dates.
   * 
   * @param startDate  the start date, used for financial calculations such as interest accrual
   * @param endDate  the end date, used for financial calculations such as interest accrual
   * @return the period
   */
  public static SchedulePeriod of(
      LocalDate startDate,
      LocalDate endDate) {
    return new SchedulePeriod(startDate, endDate, startDate, endDate);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(unadjustedStartDate, unadjustedEndDate, "unadjustedStartDate", "unadjustedEndDate");
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.unadjustedStartDate == null) {
      builder.unadjustedStartDate = builder.startDate;
    }
    if (builder.unadjustedEndDate == null) {
      builder.unadjustedEndDate = builder.endDate;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the length of the period.
   * <p>
   * This returns the length of the period, considering the adjusted start and end dates.
   * The calculation does not involve a day count or holiday calendar.
   * The period is calculated using {@link Period#between(LocalDate, LocalDate)} and as
   * such includes the start date and excludes the end date.
   * 
   * @return the length of the period
   */
  public Period length() {
    return Period.between(startDate, endDate);
  }

  /**
   * Calculates the number of days in the period.
   * <p>
   * This returns the actual number of days in the period, considering the adjusted start and end dates.
   * The calculation does not involve a day count or holiday calendar.
   * The length includes one date and excludes the other.
   * 
   * @return the actual number of days in the period
   */
  public int lengthInDays() {
    return Math.toIntExact(endDate.toEpochDay() - startDate.toEpochDay());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the year fraction using the specified day count.
   * <p>
   * Additional information from the schedule is made available to the day count algorithm.
   * 
   * @param dayCount  the day count convention
   * @param schedule  the schedule that contains this period
   * @return the year fraction, calculated via the day count
   */
  public double yearFraction(DayCount dayCount, Schedule schedule) {
    ArgChecker.notNull(dayCount, "dayCount");
    ArgChecker.notNull(schedule, "schedule");
    return dayCount.yearFraction(startDate, endDate, schedule);
  }

  /**
   * Checks if this period is regular according to the specified frequency and roll convention.
   * <p>
   * A schedule period is normally created from a frequency and roll convention.
   * These can therefore be used to determine if the period is regular, which simply
   * means that the period end date can be generated from the start date and vice versa.
   * 
   * @param frequency  the frequency
   * @param rollConvention  the roll convention
   * @return true if the period is regular
   */
  public boolean isRegular(Frequency frequency, RollConvention rollConvention) {
    ArgChecker.notNull(frequency, "frequency");
    ArgChecker.notNull(rollConvention, "rollConvention");
    return rollConvention.next(unadjustedStartDate, frequency).equals(unadjustedEndDate) &&
        rollConvention.previous(unadjustedEndDate, frequency).equals(unadjustedStartDate);
  }

  /**
   * Checks if this period contains the specified date.
   * <p>
   * The adjusted start and end dates are used in the comparison.
   * The start date is included, the end date is excluded.
   * 
   * @param date  the date to check
   * @return true if this period contains the date
   */
  public boolean contains(LocalDate date) {
    ArgChecker.notNull(date, "date");
    return !date.isBefore(startDate) && date.isBefore(endDate);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a sub-schedule within this period.
   * <p>
   * The sub-schedule will have the one or more periods.
   * The schedule is bounded by the unadjusted start and end date of this period.
   * The frequency and roll convention are used to build unadjusted schedule dates.
   * The stub convention is used to handle any remaining time when the new frequency
   * does not evenly divide into the period.
   * 
   * @param frequency  the frequency of the sub-schedule
   * @param rollConvention  the roll convention to use for rolling
   * @param stubConvention  the stub convention to use for any excess
   * @param adjustment  the business day adjustment to apply to the sub-schedule
   * @return the sub-schedule
   * @throws ScheduleException if the schedule cannot be created
   */
  public PeriodicSchedule subSchedule(
      Frequency frequency,
      RollConvention rollConvention,
      StubConvention stubConvention,
      BusinessDayAdjustment adjustment) {

    return PeriodicSchedule.builder()
        .startDate(unadjustedStartDate)
        .endDate(unadjustedEndDate)
        .frequency(frequency)
        .businessDayAdjustment(adjustment)
        .rollConvention(rollConvention)
        .stubConvention(stubConvention)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this period to one where the start and end dates are adjusted using the specified adjuster.
   * <p>
   * The start date of the result will be the start date of this period as altered by the specified adjuster.
   * The end date of the result will be the end date of this period as altered by the specified adjuster.
   * The unadjusted start date and unadjusted end date will be the same as in this period.
   * <p>
   * The adjuster will typically be obtained from {@link BusinessDayAdjustment#resolve(ReferenceData)}.
   * 
   * @param adjuster  the adjuster to use
   * @return the adjusted schedule period
   */
  public SchedulePeriod toAdjusted(DateAdjuster adjuster) {
    // implementation needs to return 'this' if unchanged to optimize downstream code
    LocalDate resultStart = adjuster.adjust(startDate);
    LocalDate resultEnd = adjuster.adjust(endDate);
    if (resultStart.equals(startDate) && resultEnd.equals(endDate)) {
      return this;
    }
    return of(resultStart, resultEnd, unadjustedStartDate, unadjustedEndDate);
  }

  /**
   * Converts this period to one where the start and end dates are set to the unadjusted dates.
   * <p>
   * The start date of the result will be the unadjusted start date of this period.
   * The end date of the result will be the unadjusted end date of this period.
   * The unadjusted start date and unadjusted end date will be the same as in this period.
   * 
   * @return the unadjusted schedule period
   */
  public SchedulePeriod toUnadjusted() {
    if (unadjustedStartDate.equals(startDate) && unadjustedEndDate.equals(endDate)) {
      return this;
    }
    return of(unadjustedStartDate, unadjustedEndDate);
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
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SchedulePeriod.Builder builder() {
    return new SchedulePeriod.Builder();
  }

  private SchedulePeriod(
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate) {
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
    JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
    this.startDate = startDate;
    this.endDate = endDate;
    this.unadjustedStartDate = unadjustedStartDate;
    this.unadjustedEndDate = unadjustedEndDate;
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
   * <p>
   * When building, this will default to the start date if not specified.
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
   * <p>
   * When building, this will default to the end date if not specified.
   * @return the value of the property, not null
   */
  public LocalDate getUnadjustedEndDate() {
    return unadjustedEndDate;
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
      return JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(unadjustedStartDate, other.unadjustedStartDate) &&
          JodaBeanUtils.equal(unadjustedEndDate, other.unadjustedEndDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedStartDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedEndDate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("SchedulePeriod{");
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("unadjustedStartDate").append('=').append(unadjustedStartDate).append(',').append(' ');
    buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate));
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
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "unadjustedStartDate",
        "unadjustedEndDate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((SchedulePeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((SchedulePeriod) bean).getEndDate();
        case 1457691881:  // unadjustedStartDate
          return ((SchedulePeriod) bean).getUnadjustedStartDate();
        case 31758114:  // unadjustedEndDate
          return ((SchedulePeriod) bean).getUnadjustedEndDate();
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

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate unadjustedStartDate;
    private LocalDate unadjustedEndDate;

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
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.unadjustedStartDate = beanToCopy.getUnadjustedStartDate();
      this.unadjustedEndDate = beanToCopy.getUnadjustedEndDate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
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
      preBuild(this);
      return new SchedulePeriod(
          startDate,
          endDate,
          unadjustedStartDate,
          unadjustedEndDate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the start date of this period, used for financial calculations such as interest accrual.
     * <p>
     * The first date in the schedule period, typically treated as inclusive.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date of this period, used for financial calculations such as interest accrual.
     * <p>
     * The last date in the schedule period, typically treated as exclusive.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the unadjusted start date.
     * <p>
     * The start date before any business day adjustment.
     * If the schedule adjusts for business days, then this is typically the regular periodic date.
     * If the schedule does not adjust for business days, then this is the same as the start date.
     * <p>
     * When building, this will default to the start date if not specified.
     * @param unadjustedStartDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedStartDate(LocalDate unadjustedStartDate) {
      JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
      this.unadjustedStartDate = unadjustedStartDate;
      return this;
    }

    /**
     * Sets the unadjusted end date.
     * <p>
     * The end date before any business day adjustment.
     * If the schedule adjusts for business days, then this is typically the regular periodic date.
     * If the schedule does not adjust for business days, then this is the same as the end date.
     * <p>
     * When building, this will default to the end date if not specified.
     * @param unadjustedEndDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedEndDate(LocalDate unadjustedEndDate) {
      JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
      this.unadjustedEndDate = unadjustedEndDate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("SchedulePeriod.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ');
      buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
