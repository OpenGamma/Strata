/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DayCount.ScheduleInfo;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * A complete schedule of periods (date ranges), with both unadjusted and adjusted dates.
 * <p>
 * The schedule consists of one or more adjacent periods (date ranges).
 * This is typically used as the basis for financial calculations, such as accrual of interest.
 * <p>
 * It is recommended to create a {@link Schedule} using a {@link PeriodicSchedule}.
 */
@BeanDefinition
public final class Schedule
    implements ScheduleInfo, ImmutableBean, Serializable {

  /**
   * The schedule periods.
   * <p>
   * There will be at least one period.
   * The periods are ordered from earliest to latest.
   * It is intended that each period is adjacent to the next one, however each
   * period is independent and non-adjacent periods are allowed.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<SchedulePeriod> periods;
  /**
   * The periodic frequency used when building the schedule.
   * <p>
   * If the schedule was not built from a regular periodic frequency,
   * then the frequency should be a suitable estimate.
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
   * Obtains a 'Term' instance based on a single period.
   * <p>
   * A 'Term' schedule has one period with a frequency of 'Term'.
   * 
   * @param period  the single period
   * @return the merged 'Term' schedule
   */
  public static Schedule ofTerm(SchedulePeriod period) {
    ArgChecker.notNull(period, "period");
    return Schedule.builder()
        .periods(ImmutableList.of(period))
        .frequency(Frequency.TERM)
        .rollConvention(RollConventions.NONE)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of periods in the schedule.
   * <p>
   * This returns the number of periods, which will be at least one.
   * 
   * @return the number of periods
   */
  public int size() {
    return periods.size();
  }

  /**
   * Checks if this schedule represents a single 'Term' period.
   * <p>
   * A 'Term' schedule has one period and a frequency of 'Term'.
   * 
   * @return true if this is a 'Term' schedule
   */
  public boolean isTerm() {
    return size() == 1 && frequency.equals(Frequency.TERM);
  }

  /**
   * Checks if this schedule has a single period.
   * 
   * @return true if this is a single period
   */
  public boolean isSinglePeriod() {
    return size() == 1;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a schedule period by index.
   * <p>
   * This returns a period using a zero-based index.
   * 
   * @param index  the zero-based period index
   * @return the schedule period
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public SchedulePeriod getPeriod(int index) {
    return periods.get(index);
  }

  /**
   * Gets the first schedule period.
   * 
   * @return the first schedule period
   */
  public SchedulePeriod getFirstPeriod() {
    return periods.get(0);
  }

  /**
   * Gets the last schedule period.
   * 
   * @return the last schedule period
   */
  public SchedulePeriod getLastPeriod() {
    return periods.get(periods.size() - 1);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the start date of the schedule.
   * <p>
   * The first date in the schedule, typically treated as inclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * 
   * @return the schedule start date
   */
  @Override
  public LocalDate getStartDate() {
    return getFirstPeriod().getStartDate();
  }

  /**
   * Gets the end date of the schedule.
   * <p>
   * The last date in the schedule, typically treated as exclusive.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * 
   * @return the schedule end date
   */
  @Override
  public LocalDate getEndDate() {
    return getLastPeriod().getEndDate();
  }

  /**
   * Gets the unadjusted start date.
   * <p>
   * The start date before any business day adjustment.
   * 
   * @return the unadjusted schedule start date
   */
  public LocalDate getUnadjustedStartDate() {
    return getFirstPeriod().getUnadjustedStartDate();
  }

  /**
   * Gets the unadjusted end date.
   * <p>
   * The end date before any business day adjustment.
   * 
   * @return the unadjusted schedule end date
   */
  public LocalDate getUnadjustedEndDate() {
    return getLastPeriod().getUnadjustedEndDate();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the initial stub if it exists.
   * <p>
   * There is an initial stub if the first period is a stub and the frequency is not 'Term'.
   * <p>
   * A period will be allocated to one and only one of {@link #getInitialStub()},
   * {@link #getRegularPeriods()} and {@link #getFinalStub()}.
   * 
   * @return the initial stub, empty if no initial stub
   */
  public Optional<SchedulePeriod> getInitialStub() {
    return (isInitialStub() ? Optional.of(getFirstPeriod()) : Optional.empty());
  }

  // checks if there is an initial stub
  private boolean isInitialStub() {
    return !isTerm() && !getFirstPeriod().isRegular(frequency, rollConvention);
  }

  /**
   * Gets the final stub if it exists.
   * <p>
   * There is a final stub if there is more than one period and the last
   * period is a stub.
   * <p>
   * A period will be allocated to one and only one of {@link #getInitialStub()},
   * {@link #getRegularPeriods()} and {@link #getFinalStub()}.
   * 
   * @return the final stub, empty if no final stub
   */
  public Optional<SchedulePeriod> getFinalStub() {
    return (isFinalStub() ? Optional.of(getLastPeriod()) : Optional.empty());
  }

  // checks if there is a final stub
  private boolean isFinalStub() {
    return !isSinglePeriod() && !getLastPeriod().isRegular(frequency, rollConvention);
  }

  /**
   * Gets the stubs if they exist.
   * <p>
   * This method returns the initial and final stub.
   * A flag is used to handle the case where there are no regular periods and it is unclear whether
   * the stub is initial or final.
   * <p>
   * A period will be allocated to one and only one of {@link #getStubs} and {@link #getRegularPeriods()}.
   * 
   * @param preferFinal true to prefer final if there is only one period
   * @return the stubs, empty if no stub
   */
  public Pair<Optional<SchedulePeriod>, Optional<SchedulePeriod>> getStubs(boolean preferFinal) {
    Optional<SchedulePeriod> initialStub = getInitialStub();
    if (preferFinal && size() == 1 && initialStub.isPresent()) {
      return Pair.of(Optional.empty(), initialStub);
    }
    return Pair.of(initialStub, getFinalStub());
  }

  /**
   * Gets the regular schedule periods.
   * <p>
   * The regular periods exclude any initial or final stub.
   * In most cases, the periods returned will be regular, corresponding to the periodic
   * frequency and roll convention, however there are cases when this is not true.
   * This includes the case where {@link #isTerm()} returns true.
   * See {@link SchedulePeriod#isRegular(Frequency, RollConvention)}.
   * <p>
   * A period will be allocated to one and only one of {@link #getInitialStub()},
   * {@link #getRegularPeriods()} and {@link #getFinalStub()}.
   * 
   * @return the non-stub schedule periods
   */
  public ImmutableList<SchedulePeriod> getRegularPeriods() {
    if (isTerm()) {
      return periods;
    }
    int startStub = isInitialStub() ? 1 : 0;
    int endStub = isFinalStub() ? 1 : 0;
    return (startStub == 0 && endStub == 0 ? periods : periods.subList(startStub, periods.size() - endStub));
  }

  /**
   * Gets the complete list of unadjusted dates.
   * <p>
   * This returns a list including all the unadjusted period boundary dates.
   * This is the same as a list containing the unadjusted start date of the schedule
   * followed by the unadjusted end date of each period.
   * 
   * @return the list of unadjusted dates, in order
   */
  public ImmutableList<LocalDate> getUnadjustedDates() {
    ImmutableList.Builder<LocalDate> dates = ImmutableList.builder();
    dates.add(getUnadjustedStartDate());
    for (SchedulePeriod period : periods) {
      dates.add(period.getUnadjustedEndDate());
    }
    return dates.build();
  }

  //-------------------------------------------------------------------------
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
   * Finds the period end date given a date in the period.
   * <p>
   * The first matching period is returned.
   * The adjusted start and end dates of each period are used in the comparison.
   * The start date is included, the end date is excluded.
   * 
   * @param date  the date to find
   * @return the end date of the period that includes the specified date
   */
  @Override
  public LocalDate getPeriodEndDate(LocalDate date) {
    return periods.stream()
        .filter(p -> p.contains(date))
        .map(p -> p.getEndDate())
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Date is not contained in any period"));
  }

  //-------------------------------------------------------------------------
  /**
   * Merges this schedule to form a new schedule with a single 'Term' period.
   * <p>
   * The result will have one period of type 'Term', with dates matching this schedule.
   * 
   * @return the merged 'Term' schedule
   */
  public Schedule mergeToTerm() {
    if (isTerm()) {
      return this;
    }
    SchedulePeriod first = getFirstPeriod();
    SchedulePeriod last = getLastPeriod();
    return Schedule.ofTerm(SchedulePeriod.of(
        first.getStartDate(),
        last.getEndDate(),
        first.getUnadjustedStartDate(),
        last.getUnadjustedEndDate()));
  }

  /**
   * Merges this schedule to form a new schedule by combining the schedule periods.
   * <p>
   * This produces a schedule where some periods are merged together.
   * For example, this could be used to convert a 3 monthly schedule into a 6 monthly schedule.
   * <p>
   * The merging is controlled by the group size, which defines the number of periods
   * to merge together in the result. For example, to convert a 3 monthly schedule into
   * a 6 monthly schedule the group size would be 2 (6 divided by 3).
   * <p>
   * A group size of zero or less will throw an exception.
   * A group size of 1 will return this schedule providing that the specified start and end date match.
   * A larger group size will return a schedule where each group of regular periods are merged.
   * <p>
   * The specified dates must be one of the dates of this schedule (unadjusted or adjusted).
   * All periods of this schedule before the first regular start date, if any, will form a single period in the result.
   * All periods of this schedule after the last regular start date, if any, will form a single period in the result.
   * If this schedule has an initial or final stub, it may be merged with a regular period as part of the process.
   * <p>
   * For example, a schedule with an initial stub and 5 regular periods can be grouped by 2 if the
   * specified {@code firstRegularStartDate} equals the end of the first regular period.
   * 
   * @param groupSize  the group size
   * @param firstRegularStartDate  the unadjusted start date of the first regular payment period
   * @param lastRegularEndDate  the unadjusted end date of the last regular payment period
   * @return the merged schedule
   * @throws IllegalArgumentException if the group size is zero or less
   * @throws ScheduleException if the merged schedule cannot be created because the dates don't
   *   match this schedule or the regular periods don't match the grouping size
   */
  public Schedule merge(int groupSize, LocalDate firstRegularStartDate, LocalDate lastRegularEndDate) {
    ArgChecker.notNegativeOrZero(groupSize, "groupSize");
    ArgChecker.inOrderOrEqual(firstRegularStartDate, lastRegularEndDate, "firstRegularStartDate", "lastRegularEndDate");
    if (isSinglePeriod() || groupSize == 1) {
      return this;
    }
    // determine stubs and regular
    int startRegularIndex = -1;
    int endRegularIndex = -1;
    for (int i = 0; i < size(); i++) {
      SchedulePeriod period = periods.get(i);
      if (period.getUnadjustedStartDate().equals(firstRegularStartDate) || period.getStartDate().equals(firstRegularStartDate)) {
        startRegularIndex = i;
      }
      if (period.getUnadjustedEndDate().equals(lastRegularEndDate) || period.getEndDate().equals(lastRegularEndDate)) {
        endRegularIndex = i + 1;
      }
    }
    if (startRegularIndex < 0) {
      throw new ScheduleException(
          "Unable to merge schedule, firstRegularStartDate {} does not match any date in the underlying schedule {}",
          firstRegularStartDate,
          getUnadjustedDates());
    }
    if (endRegularIndex < 0) {
      throw new ScheduleException(
          "Unable to merge schedule, lastRegularEndDate {} does not match any date in the underlying schedule {}",
          lastRegularEndDate,
          getUnadjustedDates());
    }
    int numberRegular = endRegularIndex - startRegularIndex;
    if ((numberRegular % groupSize) != 0) {
      Period newFrequency = frequency.getPeriod().multipliedBy(groupSize);
      throw new ScheduleException(
          "Unable to merge schedule, firstRegularStartDate {} and lastRegularEndDate {} cannot be used to " +
              "create regular periods of frequency '{}'",
          firstRegularStartDate, lastRegularEndDate, newFrequency);
    }
    List<SchedulePeriod> newSchedule = new ArrayList<>();
    if (startRegularIndex > 0) {
      newSchedule.add(createSchedulePeriod(periods.subList(0, startRegularIndex)));
    }
    for (int i = startRegularIndex; i < endRegularIndex; i += groupSize) {
      newSchedule.add(createSchedulePeriod(periods.subList(i, i + groupSize)));
    }
    if (endRegularIndex < periods.size()) {
      newSchedule.add(createSchedulePeriod(periods.subList(endRegularIndex, periods.size())));
    }
    // build schedule
    return Schedule.builder()
        .periods(newSchedule)
        .frequency(Frequency.of(frequency.getPeriod().multipliedBy(groupSize)))
        .rollConvention(rollConvention)
        .build();
  }

  /**
   * Merges this schedule to form a new schedule by combining the regular schedule periods.
   * <p>
   * This produces a schedule where some periods are merged together.
   * For example, this could be used to convert a 3 monthly schedule into a 6 monthly schedule.
   * <p>
   * The merging is controlled by the group size, which defines the number of periods
   * to merge together in the result. For example, to convert a 3 monthly schedule into
   * a 6 monthly schedule the group size would be 2 (6 divided by 3).
   * <p>
   * A group size of zero or less will throw an exception.
   * A group size of 1 will return this schedule.
   * A larger group size will return a schedule where each group of regular periods are merged.
   * The roll flag is used to determine the direction in which grouping occurs.
   * <p>
   * Any existing stub periods are considered to be special, and are not merged.
   * Even if the grouping results in an excess period, such as 10 periods with a group size
   * of 3, the excess period will not be merged with a stub.
   * <p>
   * If this period is a 'Term' period, this schedule is returned.
   * 
   * @param groupSize  the group size
   * @param rollForwards  whether to roll forwards (true) or backwards (false)
   * @return the merged schedule
   * @throws IllegalArgumentException if the group size is zero or less
   */
  public Schedule mergeRegular(int groupSize, boolean rollForwards) {
    ArgChecker.notNegativeOrZero(groupSize, "groupSize");
    if (isSinglePeriod() || groupSize == 1) {
      return this;
    }
    List<SchedulePeriod> newSchedule = new ArrayList<>();
    // retain initial stub
    Optional<SchedulePeriod> initialStub = getInitialStub();
    if (initialStub.isPresent()) {
      newSchedule.add(initialStub.get());
    }
    // merge regular, handling stubs via min/max
    ImmutableList<SchedulePeriod> regularPeriods = getRegularPeriods();
    int regularSize = regularPeriods.size();
    int remainder = regularSize % groupSize;
    int startIndex = (rollForwards || remainder == 0 ? 0 : -(groupSize - remainder));
    for (int i = startIndex; i < regularSize; i += groupSize) {
      int from = Math.max(i, 0);
      int to = Math.min(i + groupSize, regularSize);
      newSchedule.add(createSchedulePeriod(regularPeriods.subList(from, to)));
    }
    // retain final stub
    Optional<SchedulePeriod> finalStub = getFinalStub();
    if (finalStub.isPresent()) {
      newSchedule.add(finalStub.get());
    }
    // build schedule
    return Schedule.builder()
        .periods(newSchedule)
        .frequency(Frequency.of(frequency.getPeriod().multipliedBy(groupSize)))
        .rollConvention(rollConvention)
        .build();
  }

  // creates a schedule period
  private SchedulePeriod createSchedulePeriod(List<SchedulePeriod> accruals) {
    SchedulePeriod first = accruals.get(0);
    if (accruals.size() == 1) {
      return first;
    }
    SchedulePeriod last = accruals.get(accruals.size() - 1);
    return SchedulePeriod.of(
        first.getStartDate(),
        last.getEndDate(),
        first.getUnadjustedStartDate(),
        last.getUnadjustedEndDate());
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this schedule to a schedule where all the start and end dates are
   * adjusted using the specified adjuster.
   * <p>
   * The result will have the same number of periods, but each start date and
   * end date is replaced by the adjusted date as returned by the adjuster.
   * The unadjusted start date and unadjusted end date of each period will not be changed.
   * 
   * @param adjuster  the adjuster to use
   * @return the adjusted schedule
   */
  public Schedule toAdjusted(DateAdjuster adjuster) {
    // implementation needs to return 'this' if unchanged to optimize downstream code
    boolean adjusted = false;
    ImmutableList.Builder<SchedulePeriod> builder = ImmutableList.builder();
    for (SchedulePeriod period : periods) {
      SchedulePeriod adjPeriod = period.toAdjusted(adjuster);
      builder.add(adjPeriod);
      adjusted |= (adjPeriod != period);
    }
    return adjusted ? new Schedule(builder.build(), frequency, rollConvention) : this;
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this schedule to a schedule where every adjusted date is reset
   * to the unadjusted equivalent.
   * <p>
   * The result will have the same number of periods, but each start date and
   * end date is replaced by the matching unadjusted start or end date.
   * 
   * @return the equivalent unadjusted schedule
   */
  public Schedule toUnadjusted() {
    return toBuilder()
        .periods(periods.stream()
            .map(p -> p.toUnadjusted())
            .collect(toImmutableList()))
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code Schedule}.
   * @return the meta-bean, not null
   */
  public static Schedule.Meta meta() {
    return Schedule.Meta.INSTANCE;
  }

  static {
    MetaBean.register(Schedule.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Schedule.Builder builder() {
    return new Schedule.Builder();
  }

  private Schedule(
      List<SchedulePeriod> periods,
      Frequency frequency,
      RollConvention rollConvention) {
    JodaBeanUtils.notEmpty(periods, "periods");
    JodaBeanUtils.notNull(frequency, "frequency");
    JodaBeanUtils.notNull(rollConvention, "rollConvention");
    this.periods = ImmutableList.copyOf(periods);
    this.frequency = frequency;
    this.rollConvention = rollConvention;
  }

  @Override
  public Schedule.Meta metaBean() {
    return Schedule.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the schedule periods.
   * <p>
   * There will be at least one period.
   * The periods are ordered from earliest to latest.
   * It is intended that each period is adjacent to the next one, however each
   * period is independent and non-adjacent periods are allowed.
   * @return the value of the property, not empty
   */
  public ImmutableList<SchedulePeriod> getPeriods() {
    return periods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the periodic frequency used when building the schedule.
   * <p>
   * If the schedule was not built from a regular periodic frequency,
   * then the frequency should be a suitable estimate.
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
      Schedule other = (Schedule) obj;
      return JodaBeanUtils.equal(periods, other.periods) &&
          JodaBeanUtils.equal(frequency, other.frequency) &&
          JodaBeanUtils.equal(rollConvention, other.rollConvention);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(periods);
    hash = hash * 31 + JodaBeanUtils.hashCode(frequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(rollConvention);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("Schedule{");
    buf.append("periods").append('=').append(JodaBeanUtils.toString(periods)).append(',').append(' ');
    buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
    buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Schedule}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code periods} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<SchedulePeriod>> periods = DirectMetaProperty.ofImmutable(
        this, "periods", Schedule.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code frequency} property.
     */
    private final MetaProperty<Frequency> frequency = DirectMetaProperty.ofImmutable(
        this, "frequency", Schedule.class, Frequency.class);
    /**
     * The meta-property for the {@code rollConvention} property.
     */
    private final MetaProperty<RollConvention> rollConvention = DirectMetaProperty.ofImmutable(
        this, "rollConvention", Schedule.class, RollConvention.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "periods",
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
        case -678739246:  // periods
          return periods;
        case -70023844:  // frequency
          return frequency;
        case -10223666:  // rollConvention
          return rollConvention;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Schedule.Builder builder() {
      return new Schedule.Builder();
    }

    @Override
    public Class<? extends Schedule> beanType() {
      return Schedule.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code periods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<SchedulePeriod>> periods() {
      return periods;
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
        case -678739246:  // periods
          return ((Schedule) bean).getPeriods();
        case -70023844:  // frequency
          return ((Schedule) bean).getFrequency();
        case -10223666:  // rollConvention
          return ((Schedule) bean).getRollConvention();
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
   * The bean-builder for {@code Schedule}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Schedule> {

    private List<SchedulePeriod> periods = ImmutableList.of();
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
    private Builder(Schedule beanToCopy) {
      this.periods = beanToCopy.getPeriods();
      this.frequency = beanToCopy.getFrequency();
      this.rollConvention = beanToCopy.getRollConvention();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -678739246:  // periods
          return periods;
        case -70023844:  // frequency
          return frequency;
        case -10223666:  // rollConvention
          return rollConvention;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -678739246:  // periods
          this.periods = (List<SchedulePeriod>) newValue;
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
    public Schedule build() {
      return new Schedule(
          periods,
          frequency,
          rollConvention);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the schedule periods.
     * <p>
     * There will be at least one period.
     * The periods are ordered from earliest to latest.
     * It is intended that each period is adjacent to the next one, however each
     * period is independent and non-adjacent periods are allowed.
     * @param periods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder periods(List<SchedulePeriod> periods) {
      JodaBeanUtils.notEmpty(periods, "periods");
      this.periods = periods;
      return this;
    }

    /**
     * Sets the {@code periods} property in the builder
     * from an array of objects.
     * @param periods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder periods(SchedulePeriod... periods) {
      return periods(ImmutableList.copyOf(periods));
    }

    /**
     * Sets the periodic frequency used when building the schedule.
     * <p>
     * If the schedule was not built from a regular periodic frequency,
     * then the frequency should be a suitable estimate.
     * @param frequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder frequency(Frequency frequency) {
      JodaBeanUtils.notNull(frequency, "frequency");
      this.frequency = frequency;
      return this;
    }

    /**
     * Sets the roll convention used when building the schedule.
     * <p>
     * If the schedule was not built from a regular periodic frequency, then the convention should be 'None'.
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
      StringBuilder buf = new StringBuilder(128);
      buf.append("Schedule.Builder{");
      buf.append("periods").append('=').append(JodaBeanUtils.toString(periods)).append(',').append(' ');
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
