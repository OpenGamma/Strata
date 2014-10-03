/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.collect.ArgChecker;

/**
 * Definition of a periodic schedule.
 * <p>
 * A periodic schedule is determined using a "periodic frequency".
 * This splits the schedule into "regular" periods of a fixed length, such as every 3 months.
 * Any remaining days are allocated to irregular "stubs" at the start and/or end.
 * <p>
 * For example, a 24 month (2 year) swap might be divided into 3 month periods.
 * The 24 month period is the overall schedule and the 3 month period is the periodic frequency.
 * <p>
 * Note that a 23 month swap cannot be split into even 3 month periods.
 * Instead, there will be a 2 month "initial" stub at the start, a 2 month "final" stub at the end
 * or both an initial and final stub with a combined length of 2 months.
 * 
 * <h4>Example</h4>
 * <p>
 * This example creates a schedule for a 13 month swap cannot be split into 3 month periods
 * with a long initial stub rolling at end-of-month:
 * <pre>
 *  // example swap using builder
 *  BusinessDayAdjustment businessDayAdj =
 *    BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, GlobalHolidayCalendars.EUTA);
 *  PeriodicScheduleDefn defn = PeriodicScheduleDefn.builder()
 *      .startDate(AdjustableDate.of(LocalDate.of(2014, 2, 12), businessDayAdj))
 *      .endDate(AdjustableDate.of(LocalDate.of(2015, 3, 31), businessDayAdj))
 *      .businessDayAdjustment(businessDayAdj)
 *      .frequency(Frequency.P3M)
 *      .stubConvention(StubConvention.LONG_INITIAL)
 *      .rollConvention(RollConventions.EOM)
 *      .build();
 *  PeriodicSchedule schedule = defn.createSchedule();
 *  
 *  // result
 *  period 1: 2014-02-12 to 2014-06-30
 *  period 2: 2014-06-30 to 2014-09-30
 *  period 3: 2014-09-30 to 2014-12-31
 *  period 4: 2014-12-31 to 2015-03-31
 * </pre>
 * 
 * <h4>Details about stubs and date rolling</h4>
 * <p>
 * The stubs are specified using a combination of the {@link StubConvention}, {@link RollConvention} and dates.
 * <p>
 * The explicit stub dates are checked first. An explicit stub occurs if 'firstRegularStartDate' or
 * 'lastRegularEndDate' is non-null and they differ from 'startDate' and 'endDate'.
 * <p>
 * If explicit stub dates are specified then they are used to lock the initial or final stub.
 * If the stub convention is non-null, it is matched and validated against the locked stub.
 * For example, if an initial stub is specified by dates and the stub convention is 'ShortInitial'
 * or 'LongInitial' then the convention is considered to be matched, thus the periodic frequency is
 * applied using the implicit stub convention 'None'.
 * If the stub convention does not match the dates, then an exception will be thrown during schedule creation.
 * If the stub convention is null, then the periodic frequency is applied using the implicit stub convention 'None'.
 * <p>
 * If explicit stub dates are not specified then the stub convention is used.
 * The convention selects whether to use the start date or the end date as the beginning of the schedule calculation.
 * The beginning of the calculation must match the roll convention, unless the convention is 'EOM',
 * in which case 'EOM' is only applied if the calculation starts at the end of the month.
 * <p>
 * In all cases, the roll convention is used to fine-tune the dates.
 * If null or 'None', the convention is effectively implied from the first date of the calculation.
 * All calculated dates will match the roll convention.
 * If this is not possible due to the dates specified then an exception will be thrown during schedule creation.
 * <p>
 * The schedule operates primarily on "unadjusted" dates.
 * An unadjusted date can be any day, including non-business days.
 * When the unadjusted schedule has been determined, the appropriate business day adjustment
 * is applied to create a parallel schedule of "adjusted" dates.
 */
@BeanDefinition
public final class PeriodicScheduleDefn
    implements ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The start date, which is the start of the first schedule period.
   * <p>
   * This is the start date of the schedule.
   * It is is unadjusted and as such might be a weekend or holiday.
   * Any applicable business day adjustment will be applied when creating the schedule.
   * This is also known as the unadjusted effective date.
   * <p>
   * In most cases, the start date of a financial instrument is just after the trade date,
   * such as two business days later. However, the start date of a schedule is permitted
   * to be any date, which includes dates before or after the trade date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date, which is the end of the last schedule period.
   * <p>
   * This is the end date of the schedule.
   * It is is unadjusted and as such might be a weekend or holiday.
   * Any applicable business day adjustment will be applied when creating the schedule.
   * This is also known as the unadjusted maturity date or unadjusted termination date.
   * This date must be after the start date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The regular periodic frequency to use.
   * <p>
   * Most dates are calculated using a regular periodic frequency, such as every 3 months.
   * The actual day-of-month or day-of-week is selected using the roll and stub conventions.
   */
  @PropertyDefinition(validate = "notNull")
  private final Frequency frequency;
  /**
   * The business day adjustment to apply.
   * <p>
   * Each date in the calculated schedule is determined without taking into account weekends and holidays.
   * The adjustment specified here is used to convert those dates to valid business days.
   * <p>
   * The start date and end date may have their own business day adjustment rules.
   * If those are null, then this adjustment is used instead.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The optional business day adjustment to apply to the start date.
   * <p>
   * The start date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the start date to a valid business day.
   * <p>
   * If this property is null, the standard {@code businessDayAdjustment} property is used instead.
   */
  @PropertyDefinition
  private final BusinessDayAdjustment startDateBusinessDayAdjustment;
  /**
   * The optional business day adjustment to apply to the end date.
   * <p>
   * The end date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the end date to a valid business day.
   * <p>
   * If this property is null, the standard {@code businessDayAdjustment} property is used instead.
   */
  @PropertyDefinition
  private final BusinessDayAdjustment endDateBusinessDayAdjustment;
  /**
   * The optional convention defining how to handle stubs.
   * <p>
   * The stub convention is used during schedule construction to determine whether the irregular
   * remaining period occurs at the start or end of the schedule.
   * It also determines whether the irregular period is shorter or longer than the regular period.
   * <p>
   * The convention 'None' may be used to explicitly indicate there are no stubs.
   * This will be validated during schedule construction.
   * <p>
   * The convention 'Both' may be used to explicitly indicate there is both an initial and final stub.
   * The stubs themselves must be specified using explicit dates.
   * This will be validated during schedule construction.
   * <p>
   * A null stub convention indicates that the convention should be implied from the actual
   * explicit dates that have been specified.
   * <p>
   * If both a stub convention and explicit dates are specified, then the combination will be
   * validated during schedule construction. For example, the combination of an explicit dated
   * initial stub and a stub convention of 'ShortInitial' or 'LongInitial' is valid, but other
   * stub conventions, such as 'ShortFinal' or 'None' would be invalid.
   */
  @PropertyDefinition
  private final StubConvention stubConvention;
  /**
   * The optional convention defining how to roll dates.
   * <p>
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   */
  @PropertyDefinition
  private final RollConvention rollConvention;
  /**
   * The optional start date of the first regular schedule period, which is the end date of the initial stub.
   * <p>
   * This is used to identify the boundary date between the initial stub and the first regular schedule period.
   * <p>
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be on or after 'startDate'.
   * <p>
   * During schedule construction, if this is non-null it will be used to determine the schedule.
   * If null, then the overall schedule start date will be used instead, resulting in no initial stub.
   */
  @PropertyDefinition
  private final LocalDate firstRegularStartDate;
  /**
   * The optional end date of the last regular schedule period, which is the start date of the final stub.
   * <p>
   * This is used to identify the boundary date between the last regular schedule period and the final stub.
   * <p>
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be after 'startDate' and after 'firstRegularStartDate'.
   * This date must be on or before 'endDate'.
   * <p>
   * During schedule construction, if this is non-null it will be used to determine the schedule.
   * If null, then the overall schedule end date will be used instead, resulting in no final stub.
   */
  @PropertyDefinition
  private final LocalDate lastRegularEndDate;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a stub convention and end-of-month flag.
   * <p>
   * The business day adjustment is used for all dates.
   * The stub convention is used to determine whether there are any stubs.
   * If the end-of-month flag is true, then in any case of ambiguity the
   * end-of-month will be chosen.
   * 
   * @param unadjustedStartDate  start date, which is the start of the first schedule period
   * @param unadjustedEndDate  the end date, which is the end of the last schedule period
   * @param frequency  the regular periodic frequency
   * @param businessDayAdjustment  the business day adjustment to apply
   * @param stubConvention  the non-null convention defining how to handle stubs
   * @param preferEndOfMonth  whether to prefer the end-of-month when rolling
   * @return the definition
   */
  public static PeriodicScheduleDefn of(
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      Frequency frequency,
      BusinessDayAdjustment businessDayAdjustment,
      StubConvention stubConvention,
      boolean preferEndOfMonth) {
    ArgChecker.notNull(unadjustedStartDate, "unadjustedStartDate");
    ArgChecker.notNull(unadjustedEndDate, "unadjustedEndDate");
    ArgChecker.notNull(frequency, "frequency");
    ArgChecker.notNull(businessDayAdjustment, "businessDayAdjustment");
    ArgChecker.notNull(stubConvention, "stubConvention");
    return PeriodicScheduleDefn.builder()
        .startDate(unadjustedStartDate)
        .endDate(unadjustedEndDate)
        .frequency(frequency)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConvention)
        .rollConvention(preferEndOfMonth ? RollConventions.EOM : null)
        .build();
  }

  /**
   * Obtains an instance based on roll and stub conventions.
   * <p>
   * The business day adjustment is used for all dates.
   * The stub convention is used to determine whether there are any stubs.
   * The roll convention is used to fine tune each rolled date.
   * 
   * @param unadjustedStartDate  start date, which is the start of the first schedule period
   * @param unadjustedEndDate  the end date, which is the end of the last schedule period
   * @param frequency  the regular periodic frequency
   * @param businessDayAdjustment  the business day adjustment to apply
   * @param stubConvention  the non-null convention defining how to handle stubs
   * @param rollConvention  the non-null convention defining how to roll dates
   * @return the definition
   */
  public static PeriodicScheduleDefn of(
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      Frequency frequency,
      BusinessDayAdjustment businessDayAdjustment,
      StubConvention stubConvention,
      RollConvention rollConvention) {
    ArgChecker.notNull(unadjustedStartDate, "unadjustedStartDate");
    ArgChecker.notNull(unadjustedEndDate, "unadjustedEndDate");
    ArgChecker.notNull(frequency, "frequency");
    ArgChecker.notNull(businessDayAdjustment, "businessDayAdjustment");
    ArgChecker.notNull(stubConvention, "stubConvention");
    ArgChecker.notNull(rollConvention, "rollConvention");
    return PeriodicScheduleDefn.builder()
        .startDate(unadjustedStartDate)
        .endDate(unadjustedEndDate)
        .frequency(frequency)
        .businessDayAdjustment(businessDayAdjustment)
        .stubConvention(stubConvention)
        .rollConvention(rollConvention)
        .build();
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(
        startDate, endDate, "startDate", "endDate");
    if (firstRegularStartDate != null) {
      ArgChecker.inOrderOrEqual(
          startDate, firstRegularStartDate, "unadjusted", "firstRegularStartDate");
      if (lastRegularEndDate != null) {
        ArgChecker.inOrderNotEqual(
            firstRegularStartDate, lastRegularEndDate, "firstRegularStartDate", "lastRegularEndDate");
      }
    }
    if (lastRegularEndDate != null) {
      ArgChecker.inOrderOrEqual(
          lastRegularEndDate, endDate, "lastRegularEndDate", "endDate");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the periodic schedule from the definition.
   * <p>
   * The schedule consists of an optional initial stub, a number of regular periods
   * and an optional final stub.
   * <p>
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is null it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is non-null, then it will be validated against the stub dates.
   * If the stub convention and stub dates are null, then no stubs are allowed.
   * 
   * @return the schedule
   * @throws PeriodicScheduleException if the definition is invalid
   */
  public PeriodicSchedule createSchedule() {
    List<LocalDate> unadj = createUnadjustedDates();
    List<LocalDate> adj = applyBusinessDayAdjustment(unadj);
    RollConvention rollConv = getEffectiveRollConvention();
    List<SchedulePeriod> periods = new ArrayList<>();
    for (int i = 0; i < unadj.size() - 1; i++) {
      periods.add(
          SchedulePeriod.builder()
            .type(SchedulePeriodType.of(i, unadj.size()))
            .startDate(adj.get(i))
            .endDate(adj.get(i + 1))
            .unadjustedStartDate(unadj.get(i))
            .unadjustedEndDate(unadj.get(i + 1))
            .frequency(frequency)
            .rollConvention(rollConv)
            .build());
    }
    return PeriodicSchedule.of(periods);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the list of unadjusted dates in the schedule.
   * <p>
   * The unadjusted date list will contain at least two elements, the start date and end date.
   * Between those dates will be the calculated periodic schedule.
   * <p>
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is null it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is non-null, then it will be validated against the stub dates.
   * If the stub convention and stub dates are null, then no stubs are allowed.
   * If the frequency is 'Term' explicit stub dates are disallowed, and the roll and stub convention are ignored.
   * 
   * @return the schedule of unadjusted dates
   * @throws PeriodicScheduleException if the definition is invalid
   */
  public ImmutableList<LocalDate> createUnadjustedDates() {
    LocalDate regStart = getEffectiveFirstRegularStartDate();
    LocalDate regEnd = getEffectiveLastRegularEndDate();
    boolean explicitInitialStub = !startDate.equals(regStart);
    boolean explicitFinalStub = !endDate.equals(regEnd);
    // handle TERM frequency
    if (frequency == Frequency.TERM) {
      if (explicitInitialStub || explicitFinalStub) {
        throw new PeriodicScheduleException(this, "Explict stubs must not be specified when using 'Term' frequency");
      }
      return ImmutableList.of(startDate, endDate);
    }
    // calculate base schedule excluding explicit stubs
    RollConvention rollConv = getEffectiveRollConvention();
    StubConvention implicitStubConv = generateImplicitStubConvention(explicitInitialStub, explicitFinalStub);
    List<LocalDate> unadj = (implicitStubConv.isCalculateBackwards() ?
      generateBackwards(regStart, regEnd, rollConv, implicitStubConv) :
      generateForwards(regStart, regEnd, rollConv, implicitStubConv));
    // add explicit stubs
    if (explicitInitialStub) {
      unadj.add(0, startDate);
    }
    if (explicitFinalStub) {
      unadj.add(endDate);
    }
    // sanity check
    ImmutableList<LocalDate> deduplicated = ImmutableSet.copyOf(unadj).asList();
    if (deduplicated.size() < unadj.size()) {
      throw new PeriodicScheduleException(this, "Schedule calculation resulted in duplicate unadjusted dates: {}", unadj);
    }
    return deduplicated;
  }

  // using knowledge of the explicit stubs, generate the correct convention for implicit stubs
  private StubConvention generateImplicitStubConvention(boolean explicitInitialStub, boolean explicitFinalStub) {
    // null is not same as NONE
    // NONE validates that there are no explicit stubs
    // null ensures that remainder after explicit stubs are removed has no stubs
    if (stubConvention != null) {
      return stubConvention.toImplicit(this, explicitInitialStub, explicitFinalStub);
    }
    return StubConvention.NONE;
  }

  // generate the schedule of dates backwards from the end
  private List<LocalDate> generateBackwards(LocalDate start, LocalDate end, RollConvention rollConv, StubConvention stubConv) {
    // validate
    if (rollConv.matches(end) == false) {
      throw new PeriodicScheduleException(
          this, "Date '{}' does not match roll convention '{}' when starting to roll backwards", end, rollConv);
    }
    // generate
    List<LocalDate> dates = new ArrayList<>();
    dates.add(start);
    dates.add(end);
    LocalDate temp = rollConv.previous(end, frequency);
    while (temp.isAfter(start)) {
      dates.add(1, temp);
      temp = rollConv.previous(temp, frequency);
    }
    // convert short stub to long stub, but only if we actually have a stub
    boolean stub = temp.equals(start) == false;
    if (stub && stubConv.isLong() && dates.size() > 2) {
      dates.remove(1);
    }
    return dates;
  }

  // generate the schedule of dates forwards from the start
  private List<LocalDate> generateForwards(LocalDate start, LocalDate end, RollConvention rollConv, StubConvention stubConv) {
    // validate
    if (rollConv.matches(start) == false) {
      throw new PeriodicScheduleException(
          this, "Date '{}' does not match roll convention '{}' when starting to roll forwards", start, rollConv);
    }
    // generate
    List<LocalDate> dates = new ArrayList<>();
    dates.add(start);
    LocalDate temp = rollConv.next(start, frequency);
    while (temp.isBefore(end)) {
      dates.add(temp);
      temp = rollConv.next(temp, frequency);
    }
    dates.add(end);
    // convert short stub to long stub, but only if we actually have a stub
    boolean stub = temp.equals(end) == false;
    if (stub && dates.size() > 2) {
      if (stubConv == StubConvention.NONE) {
        throw new PeriodicScheduleException(
            this, "Period '{}' to '{}' resulted in a disallowed stub with frequency '{}'", start, end, frequency);
      }
      if (stubConv.isLong()) {
        dates.remove(dates.size() - 2);
      }
    }
    return dates;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the list of adjusted dates in the schedule.
   * <p>
   * The adjusted date list will contain at least two elements, the start date and end date.
   * Between those dates will be the calculated periodic schedule.
   * Each date will be a valid business day as per the appropriate business day adjustment.
   * <p>
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is null it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is non-null, then it will be validated against the stub dates.
   * If the stub convention and stub dates are null, then no stubs are allowed.
   * 
   * @return the schedule of dates adjusted to valid business days
   * @throws PeriodicScheduleException if the definition is invalid
   */
  public ImmutableList<LocalDate> createAdjustedDates() {
    ImmutableList<LocalDate> unadj = createUnadjustedDates();
    return ImmutableList.copyOf(applyBusinessDayAdjustment(unadj));
  }

  // applies the appropriate business day adjustment to each date
  private List<LocalDate> applyBusinessDayAdjustment(List<LocalDate> unadj) {
    List<LocalDate> adj = unadj.stream()
        .map(businessDayAdjustment::adjust)
        .collect(Collectors.toList());
    if (startDateBusinessDayAdjustment != null) {
      adj.set(0, startDateBusinessDayAdjustment.adjust(startDate));
    }
    if (endDateBusinessDayAdjustment != null) {
      adj.set(adj.size() - 1, endDateBusinessDayAdjustment.adjust(endDate));
    }
    ImmutableSet<LocalDate> deduplicated = ImmutableSet.copyOf(adj);
    if (deduplicated.size() < adj.size()) {
      throw new PeriodicScheduleException(this, "Schedule calculation resulted in duplicate adjusted dates: {}", adj);
    }
    return adj;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the effective roll convention defining how to roll dates.
   * <p>
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   * <p>
   * The effective roll convention is a non-null value.
   * If the roll convention property is null, this is determined from the
   * stub convention, dates and frequency, defaulting to 'None' if necessary.
   * 
   * @return the non-null roll convention
   */
  public RollConvention getEffectiveRollConvention() {
    // determine roll convention from stub convention, using EOM as a flag
    if (stubConvention != null) {
      // special handling for EOM as it is advisory rather than mandatory
      if (rollConvention == RollConventions.EOM) {
        RollConvention derived = stubConvention.toRollConvention(
            getEffectiveFirstRegularStartDate(), getEffectiveLastRegularEndDate(), frequency, true);
        return (derived == RollConventions.NONE ? RollConventions.EOM : derived);
      }
      // avoid RollConventions.NONE if possible
      if (rollConvention == null || rollConvention == RollConventions.NONE) {
        return stubConvention.toRollConvention(
            getEffectiveFirstRegularStartDate(), getEffectiveLastRegularEndDate(), frequency, false);
      }
    }
    // avoid RollConventions.NONE if possible
    if (rollConvention == null || rollConvention == RollConventions.NONE) {
      return StubConvention.NONE.toRollConvention(
          getEffectiveFirstRegularStartDate(), getEffectiveLastRegularEndDate(), frequency, false);
    }
    // use RollConventions.NONE if nothing else applies
    return Objects.firstNonNull(rollConvention, RollConventions.NONE);
  }

  /**
   * Gets the effective first regular start date.
   * <p>
   * This will be either 'firstRegularStartDate' or 'startDate.unadjusted'.
   * 
   * @return the non-null start date of the first regular period
   */
  public LocalDate getEffectiveFirstRegularStartDate() {
    return Objects.firstNonNull(firstRegularStartDate, startDate);
  }

  /**
   * Gets the effective last regular end date.
   * <p>
   * This will be either 'lastRegularEndDate' or 'endDate.unadjusted'.
   * 
   * @return the non-null end date of the last regular period
   */
  public LocalDate getEffectiveLastRegularEndDate() {
    return Objects.firstNonNull(lastRegularEndDate, endDate);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PeriodicScheduleDefn}.
   * @return the meta-bean, not null
   */
  public static PeriodicScheduleDefn.Meta meta() {
    return PeriodicScheduleDefn.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PeriodicScheduleDefn.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static PeriodicScheduleDefn.Builder builder() {
    return new PeriodicScheduleDefn.Builder();
  }

  private PeriodicScheduleDefn(
      LocalDate startDate,
      LocalDate endDate,
      Frequency frequency,
      BusinessDayAdjustment businessDayAdjustment,
      BusinessDayAdjustment startDateBusinessDayAdjustment,
      BusinessDayAdjustment endDateBusinessDayAdjustment,
      StubConvention stubConvention,
      RollConvention rollConvention,
      LocalDate firstRegularStartDate,
      LocalDate lastRegularEndDate) {
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(frequency, "frequency");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    this.startDate = startDate;
    this.endDate = endDate;
    this.frequency = frequency;
    this.businessDayAdjustment = businessDayAdjustment;
    this.startDateBusinessDayAdjustment = startDateBusinessDayAdjustment;
    this.endDateBusinessDayAdjustment = endDateBusinessDayAdjustment;
    this.stubConvention = stubConvention;
    this.rollConvention = rollConvention;
    this.firstRegularStartDate = firstRegularStartDate;
    this.lastRegularEndDate = lastRegularEndDate;
    validate();
  }

  @Override
  public PeriodicScheduleDefn.Meta metaBean() {
    return PeriodicScheduleDefn.Meta.INSTANCE;
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
   * Gets the start date, which is the start of the first schedule period.
   * <p>
   * This is the start date of the schedule.
   * It is is unadjusted and as such might be a weekend or holiday.
   * Any applicable business day adjustment will be applied when creating the schedule.
   * This is also known as the unadjusted effective date.
   * <p>
   * In most cases, the start date of a financial instrument is just after the trade date,
   * such as two business days later. However, the start date of a schedule is permitted
   * to be any date, which includes dates before or after the trade date.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date, which is the end of the last schedule period.
   * <p>
   * This is the end date of the schedule.
   * It is is unadjusted and as such might be a weekend or holiday.
   * Any applicable business day adjustment will be applied when creating the schedule.
   * This is also known as the unadjusted maturity date or unadjusted termination date.
   * This date must be after the start date.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the regular periodic frequency to use.
   * <p>
   * Most dates are calculated using a regular periodic frequency, such as every 3 months.
   * The actual day-of-month or day-of-week is selected using the roll and stub conventions.
   * @return the value of the property, not null
   */
  public Frequency getFrequency() {
    return frequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply.
   * <p>
   * Each date in the calculated schedule is determined without taking into account weekends and holidays.
   * The adjustment specified here is used to convert those dates to valid business days.
   * <p>
   * The start date and end date may have their own business day adjustment rules.
   * If those are null, then this adjustment is used instead.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional business day adjustment to apply to the start date.
   * <p>
   * The start date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the start date to a valid business day.
   * <p>
   * If this property is null, the standard {@code businessDayAdjustment} property is used instead.
   * @return the value of the property
   */
  public BusinessDayAdjustment getStartDateBusinessDayAdjustment() {
    return startDateBusinessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional business day adjustment to apply to the end date.
   * <p>
   * The end date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the end date to a valid business day.
   * <p>
   * If this property is null, the standard {@code businessDayAdjustment} property is used instead.
   * @return the value of the property
   */
  public BusinessDayAdjustment getEndDateBusinessDayAdjustment() {
    return endDateBusinessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional convention defining how to handle stubs.
   * <p>
   * The stub convention is used during schedule construction to determine whether the irregular
   * remaining period occurs at the start or end of the schedule.
   * It also determines whether the irregular period is shorter or longer than the regular period.
   * <p>
   * The convention 'None' may be used to explicitly indicate there are no stubs.
   * This will be validated during schedule construction.
   * <p>
   * The convention 'Both' may be used to explicitly indicate there is both an initial and final stub.
   * The stubs themselves must be specified using explicit dates.
   * This will be validated during schedule construction.
   * <p>
   * A null stub convention indicates that the convention should be implied from the actual
   * explicit dates that have been specified.
   * <p>
   * If both a stub convention and explicit dates are specified, then the combination will be
   * validated during schedule construction. For example, the combination of an explicit dated
   * initial stub and a stub convention of 'ShortInitial' or 'LongInitial' is valid, but other
   * stub conventions, such as 'ShortFinal' or 'None' would be invalid.
   * @return the value of the property
   */
  public StubConvention getStubConvention() {
    return stubConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional convention defining how to roll dates.
   * <p>
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   * @return the value of the property
   */
  public RollConvention getRollConvention() {
    return rollConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional start date of the first regular schedule period, which is the end date of the initial stub.
   * <p>
   * This is used to identify the boundary date between the initial stub and the first regular schedule period.
   * <p>
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be on or after 'startDate'.
   * <p>
   * During schedule construction, if this is non-null it will be used to determine the schedule.
   * If null, then the overall schedule start date will be used instead, resulting in no initial stub.
   * @return the value of the property
   */
  public LocalDate getFirstRegularStartDate() {
    return firstRegularStartDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional end date of the last regular schedule period, which is the start date of the final stub.
   * <p>
   * This is used to identify the boundary date between the last regular schedule period and the final stub.
   * <p>
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be after 'startDate' and after 'firstRegularStartDate'.
   * This date must be on or before 'endDate'.
   * <p>
   * During schedule construction, if this is non-null it will be used to determine the schedule.
   * If null, then the overall schedule end date will be used instead, resulting in no final stub.
   * @return the value of the property
   */
  public LocalDate getLastRegularEndDate() {
    return lastRegularEndDate;
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
      PeriodicScheduleDefn other = (PeriodicScheduleDefn) obj;
      return JodaBeanUtils.equal(getStartDate(), other.getStartDate()) &&
          JodaBeanUtils.equal(getEndDate(), other.getEndDate()) &&
          JodaBeanUtils.equal(getFrequency(), other.getFrequency()) &&
          JodaBeanUtils.equal(getBusinessDayAdjustment(), other.getBusinessDayAdjustment()) &&
          JodaBeanUtils.equal(getStartDateBusinessDayAdjustment(), other.getStartDateBusinessDayAdjustment()) &&
          JodaBeanUtils.equal(getEndDateBusinessDayAdjustment(), other.getEndDateBusinessDayAdjustment()) &&
          JodaBeanUtils.equal(getStubConvention(), other.getStubConvention()) &&
          JodaBeanUtils.equal(getRollConvention(), other.getRollConvention()) &&
          JodaBeanUtils.equal(getFirstRegularStartDate(), other.getFirstRegularStartDate()) &&
          JodaBeanUtils.equal(getLastRegularEndDate(), other.getLastRegularEndDate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getStartDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getEndDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFrequency());
    hash += hash * 31 + JodaBeanUtils.hashCode(getBusinessDayAdjustment());
    hash += hash * 31 + JodaBeanUtils.hashCode(getStartDateBusinessDayAdjustment());
    hash += hash * 31 + JodaBeanUtils.hashCode(getEndDateBusinessDayAdjustment());
    hash += hash * 31 + JodaBeanUtils.hashCode(getStubConvention());
    hash += hash * 31 + JodaBeanUtils.hashCode(getRollConvention());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFirstRegularStartDate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getLastRegularEndDate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("PeriodicScheduleDefn{");
    buf.append("startDate").append('=').append(getStartDate()).append(',').append(' ');
    buf.append("endDate").append('=').append(getEndDate()).append(',').append(' ');
    buf.append("frequency").append('=').append(getFrequency()).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(getBusinessDayAdjustment()).append(',').append(' ');
    buf.append("startDateBusinessDayAdjustment").append('=').append(getStartDateBusinessDayAdjustment()).append(',').append(' ');
    buf.append("endDateBusinessDayAdjustment").append('=').append(getEndDateBusinessDayAdjustment()).append(',').append(' ');
    buf.append("stubConvention").append('=').append(getStubConvention()).append(',').append(' ');
    buf.append("rollConvention").append('=').append(getRollConvention()).append(',').append(' ');
    buf.append("firstRegularStartDate").append('=').append(getFirstRegularStartDate()).append(',').append(' ');
    buf.append("lastRegularEndDate").append('=').append(JodaBeanUtils.toString(getLastRegularEndDate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PeriodicScheduleDefn}.
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
        this, "startDate", PeriodicScheduleDefn.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", PeriodicScheduleDefn.class, LocalDate.class);
    /**
     * The meta-property for the {@code frequency} property.
     */
    private final MetaProperty<Frequency> frequency = DirectMetaProperty.ofImmutable(
        this, "frequency", PeriodicScheduleDefn.class, Frequency.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", PeriodicScheduleDefn.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code startDateBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> startDateBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "startDateBusinessDayAdjustment", PeriodicScheduleDefn.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code endDateBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> endDateBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "endDateBusinessDayAdjustment", PeriodicScheduleDefn.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code stubConvention} property.
     */
    private final MetaProperty<StubConvention> stubConvention = DirectMetaProperty.ofImmutable(
        this, "stubConvention", PeriodicScheduleDefn.class, StubConvention.class);
    /**
     * The meta-property for the {@code rollConvention} property.
     */
    private final MetaProperty<RollConvention> rollConvention = DirectMetaProperty.ofImmutable(
        this, "rollConvention", PeriodicScheduleDefn.class, RollConvention.class);
    /**
     * The meta-property for the {@code firstRegularStartDate} property.
     */
    private final MetaProperty<LocalDate> firstRegularStartDate = DirectMetaProperty.ofImmutable(
        this, "firstRegularStartDate", PeriodicScheduleDefn.class, LocalDate.class);
    /**
     * The meta-property for the {@code lastRegularEndDate} property.
     */
    private final MetaProperty<LocalDate> lastRegularEndDate = DirectMetaProperty.ofImmutable(
        this, "lastRegularEndDate", PeriodicScheduleDefn.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "frequency",
        "businessDayAdjustment",
        "startDateBusinessDayAdjustment",
        "endDateBusinessDayAdjustment",
        "stubConvention",
        "rollConvention",
        "firstRegularStartDate",
        "lastRegularEndDate");

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
        case -70023844:  // frequency
          return frequency;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 429197561:  // startDateBusinessDayAdjustment
          return startDateBusinessDayAdjustment;
        case -734327136:  // endDateBusinessDayAdjustment
          return endDateBusinessDayAdjustment;
        case -31408449:  // stubConvention
          return stubConvention;
        case -10223666:  // rollConvention
          return rollConvention;
        case 2011803076:  // firstRegularStartDate
          return firstRegularStartDate;
        case -1540679645:  // lastRegularEndDate
          return lastRegularEndDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public PeriodicScheduleDefn.Builder builder() {
      return new PeriodicScheduleDefn.Builder();
    }

    @Override
    public Class<? extends PeriodicScheduleDefn> beanType() {
      return PeriodicScheduleDefn.class;
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
     * The meta-property for the {@code frequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> frequency() {
      return frequency;
    }

    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code startDateBusinessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> startDateBusinessDayAdjustment() {
      return startDateBusinessDayAdjustment;
    }

    /**
     * The meta-property for the {@code endDateBusinessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> endDateBusinessDayAdjustment() {
      return endDateBusinessDayAdjustment;
    }

    /**
     * The meta-property for the {@code stubConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StubConvention> stubConvention() {
      return stubConvention;
    }

    /**
     * The meta-property for the {@code rollConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RollConvention> rollConvention() {
      return rollConvention;
    }

    /**
     * The meta-property for the {@code firstRegularStartDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> firstRegularStartDate() {
      return firstRegularStartDate;
    }

    /**
     * The meta-property for the {@code lastRegularEndDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> lastRegularEndDate() {
      return lastRegularEndDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((PeriodicScheduleDefn) bean).getStartDate();
        case -1607727319:  // endDate
          return ((PeriodicScheduleDefn) bean).getEndDate();
        case -70023844:  // frequency
          return ((PeriodicScheduleDefn) bean).getFrequency();
        case -1065319863:  // businessDayAdjustment
          return ((PeriodicScheduleDefn) bean).getBusinessDayAdjustment();
        case 429197561:  // startDateBusinessDayAdjustment
          return ((PeriodicScheduleDefn) bean).getStartDateBusinessDayAdjustment();
        case -734327136:  // endDateBusinessDayAdjustment
          return ((PeriodicScheduleDefn) bean).getEndDateBusinessDayAdjustment();
        case -31408449:  // stubConvention
          return ((PeriodicScheduleDefn) bean).getStubConvention();
        case -10223666:  // rollConvention
          return ((PeriodicScheduleDefn) bean).getRollConvention();
        case 2011803076:  // firstRegularStartDate
          return ((PeriodicScheduleDefn) bean).getFirstRegularStartDate();
        case -1540679645:  // lastRegularEndDate
          return ((PeriodicScheduleDefn) bean).getLastRegularEndDate();
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
   * The bean-builder for {@code PeriodicScheduleDefn}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<PeriodicScheduleDefn> {

    private LocalDate startDate;
    private LocalDate endDate;
    private Frequency frequency;
    private BusinessDayAdjustment businessDayAdjustment;
    private BusinessDayAdjustment startDateBusinessDayAdjustment;
    private BusinessDayAdjustment endDateBusinessDayAdjustment;
    private StubConvention stubConvention;
    private RollConvention rollConvention;
    private LocalDate firstRegularStartDate;
    private LocalDate lastRegularEndDate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(PeriodicScheduleDefn beanToCopy) {
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.frequency = beanToCopy.getFrequency();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
      this.startDateBusinessDayAdjustment = beanToCopy.getStartDateBusinessDayAdjustment();
      this.endDateBusinessDayAdjustment = beanToCopy.getEndDateBusinessDayAdjustment();
      this.stubConvention = beanToCopy.getStubConvention();
      this.rollConvention = beanToCopy.getRollConvention();
      this.firstRegularStartDate = beanToCopy.getFirstRegularStartDate();
      this.lastRegularEndDate = beanToCopy.getLastRegularEndDate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -70023844:  // frequency
          return frequency;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 429197561:  // startDateBusinessDayAdjustment
          return startDateBusinessDayAdjustment;
        case -734327136:  // endDateBusinessDayAdjustment
          return endDateBusinessDayAdjustment;
        case -31408449:  // stubConvention
          return stubConvention;
        case -10223666:  // rollConvention
          return rollConvention;
        case 2011803076:  // firstRegularStartDate
          return firstRegularStartDate;
        case -1540679645:  // lastRegularEndDate
          return lastRegularEndDate;
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
        case -70023844:  // frequency
          this.frequency = (Frequency) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 429197561:  // startDateBusinessDayAdjustment
          this.startDateBusinessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case -734327136:  // endDateBusinessDayAdjustment
          this.endDateBusinessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case -31408449:  // stubConvention
          this.stubConvention = (StubConvention) newValue;
          break;
        case -10223666:  // rollConvention
          this.rollConvention = (RollConvention) newValue;
          break;
        case 2011803076:  // firstRegularStartDate
          this.firstRegularStartDate = (LocalDate) newValue;
          break;
        case -1540679645:  // lastRegularEndDate
          this.lastRegularEndDate = (LocalDate) newValue;
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
    public PeriodicScheduleDefn build() {
      return new PeriodicScheduleDefn(
          startDate,
          endDate,
          frequency,
          businessDayAdjustment,
          startDateBusinessDayAdjustment,
          endDateBusinessDayAdjustment,
          stubConvention,
          rollConvention,
          firstRegularStartDate,
          lastRegularEndDate);
    }

    //-----------------------------------------------------------------------
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
     * Sets the {@code businessDayAdjustment} property in the builder.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the {@code startDateBusinessDayAdjustment} property in the builder.
     * @param startDateBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder startDateBusinessDayAdjustment(BusinessDayAdjustment startDateBusinessDayAdjustment) {
      this.startDateBusinessDayAdjustment = startDateBusinessDayAdjustment;
      return this;
    }

    /**
     * Sets the {@code endDateBusinessDayAdjustment} property in the builder.
     * @param endDateBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder endDateBusinessDayAdjustment(BusinessDayAdjustment endDateBusinessDayAdjustment) {
      this.endDateBusinessDayAdjustment = endDateBusinessDayAdjustment;
      return this;
    }

    /**
     * Sets the {@code stubConvention} property in the builder.
     * @param stubConvention  the new value
     * @return this, for chaining, not null
     */
    public Builder stubConvention(StubConvention stubConvention) {
      this.stubConvention = stubConvention;
      return this;
    }

    /**
     * Sets the {@code rollConvention} property in the builder.
     * @param rollConvention  the new value
     * @return this, for chaining, not null
     */
    public Builder rollConvention(RollConvention rollConvention) {
      this.rollConvention = rollConvention;
      return this;
    }

    /**
     * Sets the {@code firstRegularStartDate} property in the builder.
     * @param firstRegularStartDate  the new value
     * @return this, for chaining, not null
     */
    public Builder firstRegularStartDate(LocalDate firstRegularStartDate) {
      this.firstRegularStartDate = firstRegularStartDate;
      return this;
    }

    /**
     * Sets the {@code lastRegularEndDate} property in the builder.
     * @param lastRegularEndDate  the new value
     * @return this, for chaining, not null
     */
    public Builder lastRegularEndDate(LocalDate lastRegularEndDate) {
      this.lastRegularEndDate = lastRegularEndDate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("PeriodicScheduleDefn.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("startDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(startDateBusinessDayAdjustment)).append(',').append(' ');
      buf.append("endDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(endDateBusinessDayAdjustment)).append(',').append(' ');
      buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ');
      buf.append("firstRegularStartDate").append('=').append(JodaBeanUtils.toString(firstRegularStartDate)).append(',').append(' ');
      buf.append("lastRegularEndDate").append('=').append(JodaBeanUtils.toString(lastRegularEndDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
