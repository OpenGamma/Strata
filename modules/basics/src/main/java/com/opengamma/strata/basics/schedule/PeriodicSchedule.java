// CSOFF: ALL (class over 2000 lines)
/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.collect.Guavate.inOptional;
import static com.opengamma.strata.collect.Guavate.tryCatchToOptional;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.AbstractList;
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
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.ArgChecker;

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
 *  PeriodicSchedule definition = PeriodicSchedule.builder()
 *      .startDate(LocalDate.of(2014, 2, 12))
 *      .endDate(LocalDate.of(2015, 3, 31))
 *      .businessDayAdjustment(businessDayAdj)
 *      .frequency(Frequency.P3M)
 *      .stubConvention(StubConvention.LONG_INITIAL)
 *      .rollConvention(RollConventions.EOM)
 *      .build();
 *  Schedule schedule = definition.createSchedule();
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
 * 'lastRegularEndDate' is present and they differ from 'startDate' and 'endDate'.
 * <p>
 * If explicit stub dates are specified then they are used to lock the initial or final stub.
 * If the stub convention is present, it is matched and validated against the locked stub.
 * For example, if an initial stub is specified by dates and the stub convention is 'ShortInitial',
 * 'LongInitial' or 'SmartInitial' then the convention is considered to be matched, thus the periodic
 * frequency is applied using the implicit stub convention 'None'.
 * If the stub convention does not match the dates, then an exception will be thrown during schedule creation.
 * If the stub convention is not present, then the periodic frequency is applied
 * using the implicit stub convention 'None'.
 * <p>
 * If explicit stub dates are not specified then the stub convention is used.
 * The convention selects whether to use the start date or the end date as the beginning of the schedule calculation.
 * The beginning of the calculation must match the roll convention, unless the convention is 'EOM',
 * in which case 'EOM' is only applied if the calculation starts at the end of the month.
 * <p>
 * In all cases, the roll convention is used to fine-tune the dates.
 * If not present or 'None', the convention is effectively implied from the first date of the calculation.
 * All calculated dates will match the roll convention.
 * If this is not possible due to the dates specified then an exception will be thrown during schedule creation.
 * <p>
 * It is permitted to have 'firstRegularStartDate' equal to 'endDate', or 'lastRegularEndDate' equal to 'startDate'.
 * In both cases, the effect is to define a schedule that is entirely "stub" and has no regular periods.
 * The resulting schedule will retain the frequency specified here, even though it is not used.
 * <p>
 * The schedule operates primarily on "unadjusted" dates.
 * An unadjusted date can be any day, including non-business days.
 * When the unadjusted schedule has been determined, the appropriate business day adjustment
 * is applied to create a parallel schedule of "adjusted" dates.
 */
@BeanDefinition
public final class PeriodicSchedule implements ImmutableBean, Serializable {

  /**
   * The start date, which is the start of the first schedule period.
   * <p>
   * This is the start date of the schedule, it is unadjusted and as such might be a weekend or holiday.
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
   * This is the end date of the schedule, it is unadjusted and as such might be a weekend or holiday.
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
   * If those are not present, then this adjustment is used instead.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The optional business day adjustment to apply to the start date.
   * <p>
   * The start date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the start date to a valid business day.
   * <p>
   * If this property is not present, the standard {@code businessDayAdjustment} property is used instead.
   */
  @PropertyDefinition(get = "optional")
  private final BusinessDayAdjustment startDateBusinessDayAdjustment;
  /**
   * The optional business day adjustment to apply to the end date.
   * <p>
   * The end date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the end date to a valid business day.
   * <p>
   * If this property is not present, the standard {@code businessDayAdjustment} property is used instead.
   */
  @PropertyDefinition(get = "optional")
  private final BusinessDayAdjustment endDateBusinessDayAdjustment;
  /**
   * The optional convention defining how to handle stubs.
   * <p>
   * The stub convention is used during schedule construction to determine whether the irregular
   * remaining period occurs at the start or end of the schedule.
   * It also determines whether the irregular period is shorter or longer than the regular period.
   * This property interacts with the "explicit dates" of {@link PeriodicSchedule#getFirstRegularStartDate()}
   * and {@link PeriodicSchedule#getLastRegularEndDate()}.
   * <p>
   * The convention 'None' may be used to explicitly indicate there are no stubs.
   * There must be no explicit dates.
   * This will be validated during schedule construction.
   * <p>
   * The convention 'Both' may be used to explicitly indicate there is both an initial and final stub.
   * The stubs themselves must be specified using explicit dates.
   * This will be validated during schedule construction.
   * <p>
   * The conventions 'ShortInitial', 'LongInitial', 'SmartInitial', 'ShortFinal', 'LongFinal'
   * and 'SmartFinal' are used to indicate the type of stub to be generated.
   * The exact behavior varies depending on whether there are explicit dates or not:
   * <p>
   * If explicit dates are specified, then the combination of stub convention an explicit date
   * will be validated during schedule construction. For example, the combination of an explicit dated
   * initial stub and a stub convention of 'ShortInitial', 'LongInitial' or 'SmartInitial' is valid,
   * but other stub conventions, such as 'ShortFinal' or 'None' would be invalid.
   * <p>
   * If explicit dates are not specified, then it is not required that a stub is generated.
   * The convention determines whether to generate dates from the start date forward, or the
   * end date backwards. Date generation may or may not result in a stub, but if it does then
   * the stub will be of the correct type.
   * <p>
   * When the stub convention is not present, the generation of stubs is based on the presence or absence
   * of the explicit dates. When there are no explicit stubs and there is a roll convention that matches
   * the start or end date, then the stub convention will be defaulted to 'SmartInitial' or 'SmartFinal'.
   */
  @PropertyDefinition(get = "optional")
  private final StubConvention stubConvention;
  /**
   * The optional convention defining how to roll dates.
   * <p>
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   * <p>
   * During schedule generation, if this is present it will be used to determine the schedule.
   * If not present, then the roll convention will be implied.
   */
  @PropertyDefinition(get = "optional")
  private final RollConvention rollConvention;
  /**
   * The optional start date of the first regular schedule period, which is the end date of the initial stub.
   * <p>
   * This is used to identify the boundary date between the initial stub and the first regular schedule period.
   * <p>
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be on or after 'startDate' and on or before 'endDate'.
   * <p>
   * During schedule generation, if this is present it will be used to determine the schedule.
   * If not present, then the overall schedule start date will be used instead, resulting in no initial stub.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate firstRegularStartDate;
  /**
   * The optional end date of the last regular schedule period, which is the start date of the final stub.
   * <p>
   * This is used to identify the boundary date between the last regular schedule period and the final stub.
   * <p>
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be one or after 'startDate', on or after 'firstRegularStartDate' and on or before 'endDate'.
   * <p>
   * During schedule generation, if this is present it will be used to determine the schedule.
   * If not present, then the overall schedule end date will be used instead, resulting in no final stub.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate lastRegularEndDate;
  /**
   * The optional start date of the first schedule period, overriding normal schedule generation.
   * <p>
   * This property is rarely used, and is generally needed when accrual starts before the effective date.
   * If specified, it overrides the start date of the first period once schedule generation has been completed.
   * Note that all schedule generation rules apply to 'startDate', with this applied as a final step.
   * This field primarily exists to support the FpML 'firstPeriodStartDate' concept.
   * <p>
   * If a roll convention is explicitly specified and the regular start date does not match it,
   * then the override will be used when generating regular periods.
   * <p>
   * If set, it should be different to the start date, although this is not validated.
   * Validation does check that it is on or before 'firstRegularStartDate' and 'lastRegularEndDate',
   * and before 'endDate'.
   * <p>
   * During schedule generation, if this is present it will be used to override the start date
   * of the first generated schedule period.
   * If not present, then the start of the first period will be the normal start date.
   */
  @PropertyDefinition(get = "optional")
  private final AdjustableDate overrideStartDate;

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
  public static PeriodicSchedule of(
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
    return PeriodicSchedule.builder()
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
  public static PeriodicSchedule of(
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
    return PeriodicSchedule.builder()
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
    // startDate < endDate
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    if (overrideStartDate != null) {
      ArgChecker.inOrderNotEqual(overrideStartDate.getUnadjusted(), endDate, "overrideStartDate", "endDate");
    }
    if (firstRegularStartDate != null) {
      // firstRegularStartDate <= endDate
      ArgChecker.inOrderOrEqual(firstRegularStartDate, endDate, "firstRegularStartDate", "endDate");
      // firstRegularStartDate <= lastRegularEndDate
      if (lastRegularEndDate != null) {
        ArgChecker.inOrderOrEqual(
            firstRegularStartDate, lastRegularEndDate, "firstRegularStartDate", "lastRegularEndDate");
      }
      // startDate (or overrideStartDate) <= firstRegularStartDate
      if (overrideStartDate != null) {
        ArgChecker.inOrderOrEqual(
            overrideStartDate.getUnadjusted(), firstRegularStartDate, "overrideStartDate", "firstRegularStartDate");
      } else {
        ArgChecker.inOrderOrEqual(startDate, firstRegularStartDate, "unadjusted", "firstRegularStartDate");
      }
    }
    if (lastRegularEndDate != null) {
      // startDate (or overrideStartDate) < lastRegularEndDate
      if (overrideStartDate != null) {
        ArgChecker.inOrderOrEqual(
            overrideStartDate.getUnadjusted(), lastRegularEndDate, "overrideStartDate", "lastRegularEndDate");
      } else {
        ArgChecker.inOrderOrEqual(startDate, lastRegularEndDate, "unadjusted", "lastRegularEndDate");
      }
      // lastRegularEndDate <= endDate
      ArgChecker.inOrderOrEqual(lastRegularEndDate, endDate, "lastRegularEndDate", "endDate");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the schedule from the definition, see {@link #createSchedule(ReferenceData, boolean)}.
   *
   * @return the schedule
   * @param refData  the reference data, used to find the holiday calendars
   * @throws ScheduleException if the definition is invalid
   */
  public Schedule createSchedule(ReferenceData refData) {
    return createSchedule(refData, false);
  }

  /**
   * Creates the schedule from the definition.
   * <p>
   * The schedule consists of an optional initial stub, a number of regular periods and an optional final stub.
   * <p>
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is not present it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is present, then it will be validated against the stub dates.
   * If the stub convention and stub dates are not present, then no stubs are allowed.
   * <p>
   * There is special handling for pre-adjusted start dates to avoid creating incorrect stubs.
   * If all the following conditions hold true, then the unadjusted start date is treated
   * as being the day-of-month implied by the roll convention (the adjusted date is unaffected).
   * <ul>
   * <li>the {@code startDateBusinessDayAdjustment} property equals {@link BusinessDayAdjustment#NONE}
   *   or the roll convention is 'EOM'
   * <li>the roll convention is numeric or 'EOM'
   * <li>applying {@code businessDayAdjustment} to the day-of-month implied by the roll convention
   *  yields the specified start date
   * </ul>
   * <p>
   * There is additional special handling for pre-adjusted first/last regular dates and the end date.
   * If the following conditions hold true, then the unadjusted date is treated as being the
   * day-of-month implied by the roll convention (the adjusted date is unaffected).
   * <ul>
   * <li>the roll convention is numeric or 'EOM'
   * <li>applying {@code businessDayAdjustment} to the day-of-month implied by the roll convention
   *  yields the first/last regular date that was specified
   * </ul>
   *
   * @return the schedule
   * @param refData  the reference data, used to find the holiday calendars
   * @param combinePeriodsIfNecessary  determines whether periods should be combined if necessary
   * @throws ScheduleException if the definition is invalid
   */
  public Schedule createSchedule(ReferenceData refData, boolean combinePeriodsIfNecessary) {
    LocalDate unadjStart = calculatedUnadjustedStartDate(refData);
    LocalDate unadjEnd = calculatedUnadjustedEndDate(refData);
    LocalDate regularStart = calculatedFirstRegularStartDate(unadjStart, refData);
    LocalDate regularEnd = calculatedLastRegularEndDate(unadjEnd, refData);
    RollConvention rollConv = calculatedRollConvention(regularStart, regularEnd);
    List<LocalDate> unadj = generateUnadjustedDates(unadjStart, regularStart, regularEnd, unadjEnd, rollConv);
    List<LocalDate> adj = applyBusinessDayAdjustment(unadj, refData);
    List<SchedulePeriod> periods = new ArrayList<>();
    try {
      // Remove any duplicate adjusted dates if requested
      if (combinePeriodsIfNecessary) {
        adj = new ArrayList<>(adj);
        unadj = new ArrayList<>(unadj);
        for (int i = 0; i < adj.size() - 1; i++) {
          while (i < adj.size() - 1 && adj.get(i).equals(adj.get(i + 1))) {
            adj.remove(i);
            unadj.remove(i);
          }
        }
      }
      // for performance, handle silly errors using exceptions
      for (int i = 0; i < unadj.size() - 1; i++) {
        periods.add(SchedulePeriod.of(adj.get(i), adj.get(i + 1), unadj.get(i), unadj.get(i + 1)));
      }
    } catch (IllegalArgumentException ex) {
      // check dates to throw a better exception for duplicate dates in schedule
      createUnadjustedDates();
      createAdjustedDates(refData);
      // unknown exception
      ScheduleException se = new ScheduleException(this, "Schedule calculation resulted in invalid period");
      se.initCause(ex);
      throw se;
    }
    return Schedule.builder()
        .periods(periods)
        .frequency(frequency)
        .rollConvention(rollConv)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the list of unadjusted dates in the schedule.
   * <p>
   * The unadjusted date list will contain at least two elements, the start date and end date.
   * Between those dates will be the calculated periodic schedule.
   * <p>
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is not present it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is present, then it will be validated against the stub dates.
   * If the stub convention and stub dates are not present, then no stubs are allowed.
   * If the frequency is 'Term' explicit stub dates are disallowed, and the roll and stub convention are ignored.
   * <p>
   * The special handling for last business day of month seen in
   * {@link #createUnadjustedDates(ReferenceData)} is not applied.
   * 
   * @return the schedule of unadjusted dates
   * @throws ScheduleException if the definition is invalid
   */
  public ImmutableList<LocalDate> createUnadjustedDates() {
    LocalDate regularStart = calculatedFirstRegularStartDate();
    LocalDate regularEnd = calculatedLastRegularEndDate();
    RollConvention rollConv = calculatedRollConvention(regularStart, regularEnd);
    List<LocalDate> unadj = generateUnadjustedDates(startDate, regularStart, regularEnd, endDate, rollConv);
    // ensure schedule is valid with no duplicated dates
    ImmutableList<LocalDate> deduplicated = ImmutableSet.copyOf(unadj).asList();
    if (deduplicated.size() < unadj.size()) {
      throw new ScheduleException(this, "Schedule calculation resulted in duplicate unadjusted dates {}", unadj);
    }
    return deduplicated;
  }

  /**
   * Creates the list of unadjusted dates in the schedule.
   * <p>
   * The unadjusted date list will contain at least two elements, the start date and end date.
   * Between those dates will be the calculated periodic schedule.
   * <p>
   * The roll convention, stub convention and additional dates are all used to determine the schedule.
   * If the roll convention is not present it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is present, then it will be validated against the stub dates.
   * If the stub convention and stub dates are not present, then no stubs are allowed.
   * If the frequency is 'Term' explicit stub dates are disallowed, and the roll and stub convention are ignored.
   * <p>
   * There is special handling for pre-adjusted start dates to avoid creating incorrect stubs.
   * If all the following conditions hold true, then the unadjusted start date is treated
   * as being the day-of-month implied by the roll convention (the adjusted date is unaffected).
   * <ul>
   * <li>the {@code startDateBusinessDayAdjustment} property equals {@link BusinessDayAdjustment#NONE}
   *   or the roll convention is 'EOM'
   * <li>the roll convention is numeric or 'EOM'
   * <li>applying {@code businessDayAdjustment} to the day-of-month implied by the roll convention
   *  yields the specified start date
   * </ul>
   * <p>
   * There is additional special handling for pre-adjusted first/last regular dates and the end date.
   * If the following conditions hold true, then the unadjusted date is treated as being the
   * day-of-month implied by the roll convention (the adjusted date is unaffected).
   * <ul>
   * <li>the roll convention is numeric or 'EOM'
   * <li>applying {@code businessDayAdjustment} to the day-of-month implied by the roll convention
   *  yields the first/last regular date that was specified
   * </ul>
   * 
   * @param refData  the reference data, used to find the holiday calendars
   * @return the schedule of unadjusted dates
   * @throws ScheduleException if the definition is invalid
   */
  public ImmutableList<LocalDate> createUnadjustedDates(ReferenceData refData) {
    List<LocalDate> unadj = unadjustedDates(refData);
    // ensure schedule is valid with no duplicated dates
    ImmutableList<LocalDate> deduplicated = ImmutableSet.copyOf(unadj).asList();
    if (deduplicated.size() < unadj.size()) {
      throw new ScheduleException(this, "Schedule calculation resulted in duplicate unadjusted dates {}", unadj);
    }
    return deduplicated;
  }

  // using the provided reference data create the unadjusted dates
  private List<LocalDate> unadjustedDates(ReferenceData refData) {
    LocalDate unadjStart = calculatedUnadjustedStartDate(refData);
    LocalDate unadjEnd = calculatedUnadjustedEndDate(refData);
    LocalDate regularStart = calculatedFirstRegularStartDate(unadjStart, refData);
    LocalDate regularEnd = calculatedLastRegularEndDate(unadjEnd, refData);
    RollConvention rollConv = calculatedRollConvention(regularStart, regularEnd);
    return generateUnadjustedDates(unadjStart, regularStart, regularEnd, unadjEnd, rollConv);
  }

  // creates the unadjusted dates, returning the mutable list
  private List<LocalDate> generateUnadjustedDates(
      LocalDate start,
      LocalDate regStart,
      LocalDate regEnd,
      LocalDate end,
      RollConvention rollConv) {

    LocalDate overrideStart = overrideStartDate != null ? overrideStartDate.getUnadjusted() : start;
    boolean explicitInitStub = !start.equals(regStart);
    boolean explicitFinalStub = !end.equals(regEnd);
    // handle case where whole period is stub
    if (regStart.equals(end) || regEnd.equals(start)) {
      return ImmutableList.of(overrideStart, end);
    }
    // handle TERM frequency
    if (frequency == Frequency.TERM) {
      if (explicitInitStub || explicitFinalStub) {
        throw new ScheduleException(this, "Explicit stubs must not be specified when using 'Term' frequency");
      }
      return ImmutableList.of(overrideStart, end);
    }
    // calculate base schedule excluding explicit stubs
    StubConvention stubConv = generateImplicitStubConvention(explicitInitStub, explicitFinalStub, regStart, regEnd);
    // special fallback if there is an override start date with a specified roll convention
    if (overrideStartDate != null &&
        rollConvention != null &&
        firstRegularStartDate == null &&
        !rollConv.matches(regStart) &&
        rollConv.matches(overrideStart)) {
      return generateUnadjustedDates(
          overrideStart, regEnd, rollConv, stubConv, explicitInitStub, overrideStart, explicitFinalStub, end);
    } else {
      return generateUnadjustedDates(
          regStart, regEnd, rollConv, stubConv, explicitInitStub, overrideStart, explicitFinalStub, end);
    }
  }

  // using knowledge of the explicit stubs, generate the correct convention for implicit stubs
  private StubConvention generateImplicitStubConvention(
      boolean explicitInitialStub,
      boolean explicitFinalStub,
      LocalDate regStart,
      LocalDate regEnd) {

    // null is not same as NONE; NONE validates that there are no explicit stubs whereas
    // null ensures that remainder after explicit stubs are removed has no stubs
    if (stubConvention != null) {
      return stubConvention.toImplicit(this, explicitInitialStub, explicitFinalStub);
    }
    // if stub convention is missing, but roll convention is present and matches start/end date, then set roll
    if (rollConvention != null && !explicitInitialStub && !explicitFinalStub) {
      if (rollConvention.getDayOfMonth() == regEnd.getDayOfMonth()) {
        return StubConvention.SMART_INITIAL;
      }
      if (rollConvention.getDayOfMonth() == regStart.getDayOfMonth()) {
        return StubConvention.SMART_FINAL;
      }
    }
    return StubConvention.NONE;
  }

  // generate dates, forwards or backwards
  private List<LocalDate> generateUnadjustedDates(
      LocalDate regStart,
      LocalDate regEnd,
      RollConvention rollCnv,
      StubConvention stubCnv,
      boolean explicitInitStub,
      LocalDate overrideStart,
      boolean explicitFinalStub,
      LocalDate end) {

    if (stubCnv.isCalculateBackwards()) {
      return generateBackwards(
          this, regStart, regEnd, frequency, rollCnv, stubCnv, overrideStart, explicitFinalStub, end);
    } else {
      return generateForwards(
          this, regStart, regEnd, frequency, rollCnv, stubCnv, explicitInitStub, overrideStart, explicitFinalStub, end);
    }
  }

  // generate the schedule of dates backwards from the end, only called when stub convention is initial
  private static List<LocalDate> generateBackwards(
      PeriodicSchedule schedule,
      LocalDate start,
      LocalDate end,
      Frequency frequency,
      RollConvention rollConv,
      StubConvention stubConv,
      LocalDate explicitStartDate,
      boolean explicitFinalStub,
      LocalDate explicitEndDate) {

    // validate
    if (rollConv.matches(end) == false) {
      throw new ScheduleException(
          schedule, "Date '{}' does not match roll convention '{}' when starting to roll backwards", end, rollConv);
    }
    // generate
    BackwardsList dates = new BackwardsList(estimateNumberPeriods(start, end, frequency));
    if (explicitFinalStub) {
      dates.addFirst(explicitEndDate);
    }
    dates.addFirst(end);
    LocalDate temp = rollConv.previous(end, frequency);
    while (temp.isAfter(start)) {
      dates.addFirst(temp);
      temp = rollConv.previous(temp, frequency);
    }
    // convert to long stub, but only if we actually have a stub
    boolean stub = temp.equals(start) == false;
    if (stub && dates.size() > 1 && stubConv.isStubLong(start, dates.get(0))) {
      dates.removeFirst();
    }
    dates.addFirst(explicitStartDate);
    return dates;
  }

  // dedicated list implementation for backwards looping for performance only implements those methods that are needed
  private static class BackwardsList extends AbstractList<LocalDate> {
    private int first;
    private LocalDate[] array;

    BackwardsList(int capacity) {
      this.array = new LocalDate[capacity];
      this.first = array.length;
    }

    @Override
    public LocalDate get(int index) {
      return array[first + index];
    }

    @Override
    public int size() {
      return array.length - first;
    }

    void addFirst(LocalDate date) {
      array[--first] = date;
    }

    void removeFirst() {
      first++;
    }
  }

  // generate the schedule of dates forwards from the start, called when stub convention is not initial
  // start/end dates are regular start/end
  private static List<LocalDate> generateForwards(
      PeriodicSchedule schedule,
      LocalDate start,
      LocalDate end,
      Frequency frequency,
      RollConvention rollConv,
      StubConvention stubConv,
      boolean explicitInitialStub,
      LocalDate explicitStartDate,
      boolean explicitFinalStub,
      LocalDate explicitEndDate) {

    // validate
    if (rollConv.matches(start) == false) {
      throw new ScheduleException(
          schedule, "Date '{}' does not match roll convention '{}' when starting to roll forwards", start, rollConv);
    }
    // generate
    List<LocalDate> dates = new ArrayList<>(estimateNumberPeriods(start, end, frequency));
    if (explicitInitialStub) {
      dates.add(explicitStartDate);
      dates.add(start);
    } else {
      dates.add(explicitStartDate);
    }
    if (!start.equals(end)) {
      LocalDate temp = rollConv.next(start, frequency);
      while (temp.isBefore(end)) {
        dates.add(temp);
        temp = rollConv.next(temp, frequency);
      }
      // convert short stub to long stub, but only if we actually have a stub
      boolean stub = temp.equals(end) == false;
      if (stub && dates.size() > 1) {
        StubConvention applicableStubConv = stubConv;
        if (stubConv == StubConvention.NONE) {
          // handle edge case where the end date does not follow the EOM rule
          if (rollConv == RollConventions.EOM &&
              frequency.isMonthBased() &&
              !explicitFinalStub &&
              start.getDayOfMonth() == start.lengthOfMonth() &&
              end.getDayOfMonth() == start.getDayOfMonth()) {
            // accept the date and move on using smart rules
            applicableStubConv = StubConvention.SMART_FINAL;
          } else {
            throw new ScheduleException(
                schedule, "Period '{}' to '{}' resulted in a disallowed stub with frequency '{}'", start, end,
                frequency);
          }
        }
        // convert a short stub to a long one if necessary
        if (applicableStubConv.isStubLong(dates.get(dates.size() - 1), end)) {
          dates.remove(dates.size() - 1);
        }
      }
      dates.add(end);
    }
    if (explicitFinalStub) {
      dates.add(explicitEndDate);
    }
    return dates;
  }

  // roughly estimate the number of periods (overestimating)
  private static int estimateNumberPeriods(LocalDate start, LocalDate end, Frequency frequency) {
    int termInYearsEstimate = end.getYear() - start.getYear() + 2;
    return (int) (Math.max(frequency.eventsPerYearEstimate(), 1) * termInYearsEstimate);
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
   * If the roll convention is not present it will be defaulted from the stub convention, with 'None' as the default.
   * If there are explicit stub dates then they will be used.
   * If the stub convention is present, then it will be validated against the stub dates.
   * If the stub convention and stub dates are not present, then no stubs are allowed.
   * <p>
   * There is special handling for pre-adjusted start dates to avoid creating incorrect stubs.
   * If all the following conditions hold true, then the unadjusted start date is treated
   * as being the day-of-month implied by the roll convention (the adjusted date is unaffected).
   * <ul>
   * <li>the {@code startDateBusinessDayAdjustment} property equals {@link BusinessDayAdjustment#NONE}
   *   or the roll convention is 'EOM'
   * <li>the roll convention is numeric or 'EOM'
   * <li>applying {@code businessDayAdjustment} to the day-of-month implied by the roll convention
   *  yields the specified start date
   * </ul>
   * <p>
   * There is additional special handling for pre-adjusted first/last regular dates and the end date.
   * If the following conditions hold true, then the unadjusted date is treated as being the
   * day-of-month implied by the roll convention (the adjusted date is unaffected).
   * <ul>
   * <li>the roll convention is numeric or 'EOM'
   * <li>applying {@code businessDayAdjustment} to the day-of-month implied by the roll convention
   *  yields the first/last regular date that was specified
   * </ul>
   * 
   * @return the schedule of dates adjusted to valid business days
   * @param refData  the reference data, used to find the holiday calendar
   * @throws ScheduleException if the definition is invalid
   */
  public ImmutableList<LocalDate> createAdjustedDates(ReferenceData refData) {
    List<LocalDate> unadj = unadjustedDates(refData);
    List<LocalDate> adj = applyBusinessDayAdjustment(unadj, refData);
    // ensure schedule is valid with no duplicated dates
    ImmutableList<LocalDate> deduplicated = ImmutableSet.copyOf(adj).asList();
    if (deduplicated.size() < adj.size()) {
      throw new ScheduleException(
          this,
          "Schedule calculation resulted in duplicate adjusted dates {} from unadjusted dates {} using adjustment '{}'",
          adj,
          unadj,
          businessDayAdjustment);
    }
    return deduplicated;
  }

  // applies the appropriate business day adjustment to each date
  private List<LocalDate> applyBusinessDayAdjustment(List<LocalDate> unadj, ReferenceData refData) {
    List<LocalDate> adj = new ArrayList<>(unadj.size());
    adj.add(calculatedStartDate().adjusted(refData));
    for (int i = 1; i < unadj.size() - 1; i++) {
      adj.add(businessDayAdjustment.adjust(unadj.get(i), refData));
    }
    adj.add(calculatedEndDate().adjusted(refData));
    return adj;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the applicable roll convention defining how to roll dates.
   * <p>
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   * <p>
   * The applicable roll convention is a non-null value.
   * If the roll convention property is not present, this is determined from the
   * stub convention, dates and frequency, defaulting to 'None' if necessary.
   * 
   * @return the non-null roll convention
   */
  public RollConvention calculatedRollConvention() {
    return calculatedRollConvention(calculatedFirstRegularStartDate(), calculatedLastRegularEndDate());
  }

  // calculates the applicable roll convention
  // the calculated start date parameter allows for influence by calculatedUnadjustedStartDate()
  private RollConvention calculatedRollConvention(
      LocalDate calculatedFirstRegStartDate,
      LocalDate calculatedLastRegEndDate) {

    // determine roll convention from stub convention
    StubConvention stubConv = MoreObjects.firstNonNull(stubConvention, StubConvention.NONE);
    // special handling for EOM as it is advisory rather than mandatory
    if (rollConvention == RollConventions.EOM) {
      RollConvention derived =
          stubConv.toRollConvention(calculatedFirstRegStartDate, calculatedLastRegEndDate, frequency, true);
      return (derived == RollConventions.NONE ? RollConventions.EOM : derived);
    }
    // avoid RollConventions.NONE if possible
    if (rollConvention == null || rollConvention == RollConventions.NONE) {
      return stubConv.toRollConvention(calculatedFirstRegStartDate, calculatedLastRegEndDate, frequency, false);
    }
    // use RollConventions.NONE if nothing else applies
    return MoreObjects.firstNonNull(rollConvention, RollConventions.NONE);
  }

  //-------------------------------------------------------------------------
  // calculates the applicable start date
  // applies de facto rule where EOM means last business day for startDate
  // and similar rule for numeric roll conventions
  // http://www.fpml.org/forums/topic/can-a-roll-convention-imply-a-stub/#post-7659
  // For 'StandardRollConventions', such as IMM, adjusted date is identified by finding the closest valid roll date
  // and applying the trade level business day adjustment
  private LocalDate calculatedUnadjustedStartDate(ReferenceData refData) {
    // change date if reference data is available and explicit start adjustment must be NONE or roll convention
    // is EOM and either numeric roll convention and day-of-month actually differs or StandardDayConvention is used
    // and the day is not a valid roll date
    if (refData != null &&
        rollConvention != null &&
        (BusinessDayAdjustment.NONE.equals(startDateBusinessDayAdjustment) || rollConvention == RollConventions.EOM)) {
      return calculatedUnadjustedDateFromAdjusted(startDate, rollConvention, businessDayAdjustment, refData);
    }
    return startDate;
  }

  // calculates the applicable end date
  private LocalDate calculatedUnadjustedEndDate(ReferenceData refData) {
    if (refData != null && rollConvention != null) {
      return calculatedUnadjustedDateFromAdjusted(
          endDate, rollConvention, calculatedEndDateBusinessDayAdjustment(), refData);
    }
    return endDate;
  }

  // calculates an unadjusted date
  // for EOM and day of month roll conventions the unadjusted date is based on the roll day-of-month
  // for other conventions, the nearest unadjusted roll date is calculated, adjusted and compared to the base date
  // this is known not to work for day of week conventions if the passed date has been adjusted forwards
  private static LocalDate calculatedUnadjustedDateFromAdjusted(
      LocalDate baseDate,
      RollConvention rollConvention,
      BusinessDayAdjustment businessDayAdjustment,
      ReferenceData refData) {

    int rollDom = rollConvention.getDayOfMonth();
    if (rollDom > 0 && baseDate.getDayOfMonth() != rollDom) {
      int lengthOfMonth = baseDate.lengthOfMonth();
      int actualDom = Math.min(rollDom, lengthOfMonth);
      // startDate is already the expected day, then nothing to do
      if (baseDate.getDayOfMonth() != actualDom) {
        LocalDate rollImpliedDate = baseDate.withDayOfMonth(actualDom);
        LocalDate adjDate = businessDayAdjustment.adjust(rollImpliedDate, refData);
        if (adjDate.equals(baseDate)) {
          return rollImpliedDate;
        }
      }
    } else if (rollDom == 0) {
      //0 roll day implies that the roll date is calculated relative to the month or week
      //Find the valid (unadjusted) roll date for the given month or week
      LocalDate rollImpliedDate = rollConvention.adjust(baseDate);
      if (!rollImpliedDate.equals(baseDate)) {
        //If roll date is relative to the month the assumption is that the adjusted date is not in a different month to
        //the original unadjusted date. This is safe as the roll day produced by monthly roll conventions are typically
        //not close to the end of the month and hence any reasonable adjustment will not move into the next month.
        //adjust() method for "day of week" roll conventions will roll forward from the passed date; hence this logic
        //will not work for "day of week" conventions if the passed baseDate has been adjusted to be after the original
        //unadjusted date (i.e. has been rolled forward).
        //Calculate the expected adjusted roll date, based on the valid unadjusted roll date
        LocalDate adjDate = businessDayAdjustment.adjust(rollImpliedDate, refData);
        //If the adjusted roll date equals the original base date then that the base date is in fact an adjusted date
        //and hence return the unadjusted date for building the schedule.
        if (adjDate.equals(baseDate)) {
          return rollImpliedDate;
        }
      }

    }
    return baseDate;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the applicable first regular start date.
   * <p>
   * This will be either 'firstRegularStartDate' or 'startDate'.
   * 
   * @return the non-null start date of the first regular period
   */
  public LocalDate calculatedFirstRegularStartDate() {
    return MoreObjects.firstNonNull(firstRegularStartDate, startDate);
  }

  // calculates the first regular start date, adjust when numeric roll convention present
  private LocalDate calculatedFirstRegularStartDate(LocalDate unadjStart, ReferenceData refData) {
    if (firstRegularStartDate == null) {
      return unadjStart;
    }
    if (refData != null && rollConvention != null) {
      return calculatedUnadjustedDateFromAdjusted(
          firstRegularStartDate, rollConvention, businessDayAdjustment, refData);
    }
    return firstRegularStartDate;
  }

  /**
   * Calculates the applicable last regular end date.
   * <p>
   * This will be either 'lastRegularEndDate' or 'endDate'.
   * 
   * @return the non-null end date of the last regular period
   */
  public LocalDate calculatedLastRegularEndDate() {
    return MoreObjects.firstNonNull(lastRegularEndDate, endDate);
  }

  // calculates the last regular end date, adjust when numeric roll convention present
  private LocalDate calculatedLastRegularEndDate(LocalDate unadjEnd, ReferenceData refData) {
    if (lastRegularEndDate == null) {
      return unadjEnd;
    }
    if (refData != null && rollConvention != null) {
      return calculatedUnadjustedDateFromAdjusted(lastRegularEndDate, rollConvention, businessDayAdjustment, refData);
    }
    return lastRegularEndDate;
  }

  /**
   * Calculates the applicable business day adjustment to apply to the start date.
   * <p>
   * This will be either 'startDateBusinessDayAdjustment' or 'businessDayAdjustment'.
   * 
   * @return the non-null business day adjustment to apply to the start date
   */
  private BusinessDayAdjustment calculatedStartDateBusinessDayAdjustment() {
    return MoreObjects.firstNonNull(startDateBusinessDayAdjustment, businessDayAdjustment);
  }

  /**
   * Calculates the applicable business day adjustment to apply to the end date.
   * <p>
   * This will be either 'endDateBusinessDayAdjustment' or 'businessDayAdjustment'.
   * 
   * @return the non-null business day adjustment to apply to the end date
   */
  private BusinessDayAdjustment calculatedEndDateBusinessDayAdjustment() {
    return MoreObjects.firstNonNull(endDateBusinessDayAdjustment, businessDayAdjustment);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the applicable start date.
   * <p>
   * The result combines the start date and the appropriate business day adjustment.
   * If the override start date is present, it will be returned.
   * 
   * @return the calculated start date
   */
  public AdjustableDate calculatedStartDate() {
    if (overrideStartDate != null) {
      return overrideStartDate;
    }
    return AdjustableDate.of(startDate, calculatedStartDateBusinessDayAdjustment());
  }

  /**
   * Calculates the applicable end date.
   * <p>
   * The result combines the end date and the appropriate business day adjustment.
   * 
   * @return the calculated end date
   */
  public AdjustableDate calculatedEndDate() {
    return AdjustableDate.of(endDate, calculatedEndDateBusinessDayAdjustment());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance based on this schedule with the start date replaced.
   * <p>
   * This returns a new instance with the schedule altered to have the specified start date.
   * The specified date is considered to be adjusted, thus {@code startDateBusinessDayAdjustment} is set to 'None'.
   * The {@code firstRegularStartDate} and {@code overrideStartDate} fields are also removed.
   * <p>
   * The stub convention is typically altered to be 'SmartInitial'.
   * The algorithm retains the 'ShortInitial' and 'LongInitial' conventions as is.
   * The algorithm examines "Final" conventions to try and set the {@code lastRegularEndDate}
   * via schedule generation allowing the stub convention to become 'SmartInitial'.
   * 
   * @param adjustedStartDate the proposed start date, which is considered to be adjusted
   * @return a schedule with the proposed start date
   * @throws IllegalArgumentException if the schedule start date cannot be replaced with the proposed start date
   */
  public PeriodicSchedule replaceStartDate(LocalDate adjustedStartDate) {
    if (adjustedStartDate.isAfter(endDate)) {
      throw new IllegalArgumentException("Cannot alter leg to have start date after end date");
    }
    PeriodicSchedule.Builder builder = toBuilder()
        .startDate(adjustedStartDate)
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .firstRegularStartDate(null)
        .overrideStartDate(null);
    if (stubConvention == null || stubConvention == StubConvention.BOTH || stubConvention == StubConvention.NONE) {
      // set the stub convention to SmartInitial to better handle the new start date
      builder.stubConvention(StubConvention.SMART_INITIAL);
    } else if (stubConvention.isFinal()) {
      if (lastRegularEndDate != null) {
        // last regular is set, so the final stub convention can be safely changed
        builder.stubConvention(StubConvention.SMART_INITIAL);
      } else {
        // calculate the last regular date so that SmartInitial can be used
        // if schedule generation fails, make no changes
        for (List<LocalDate> dates : inOptional(tryCatchToOptional(() -> createUnadjustedDates()))) {
          if (dates.size() > 2) {
            LocalDate lastRegular = dates.get(dates.size() - 2);
            builder.lastRegularEndDate(lastRegular);
            builder.stubConvention(StubConvention.SMART_INITIAL);
          }
        }
      }
    }
    return builder.build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code PeriodicSchedule}.
   * @return the meta-bean, not null
   */
  public static PeriodicSchedule.Meta meta() {
    return PeriodicSchedule.Meta.INSTANCE;
  }

  static {
    MetaBean.register(PeriodicSchedule.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static PeriodicSchedule.Builder builder() {
    return new PeriodicSchedule.Builder();
  }

  private PeriodicSchedule(
      LocalDate startDate,
      LocalDate endDate,
      Frequency frequency,
      BusinessDayAdjustment businessDayAdjustment,
      BusinessDayAdjustment startDateBusinessDayAdjustment,
      BusinessDayAdjustment endDateBusinessDayAdjustment,
      StubConvention stubConvention,
      RollConvention rollConvention,
      LocalDate firstRegularStartDate,
      LocalDate lastRegularEndDate,
      AdjustableDate overrideStartDate) {
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
    this.overrideStartDate = overrideStartDate;
    validate();
  }

  @Override
  public PeriodicSchedule.Meta metaBean() {
    return PeriodicSchedule.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date, which is the start of the first schedule period.
   * <p>
   * This is the start date of the schedule, it is unadjusted and as such might be a weekend or holiday.
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
   * This is the end date of the schedule, it is unadjusted and as such might be a weekend or holiday.
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
   * If those are not present, then this adjustment is used instead.
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
   * If this property is not present, the standard {@code businessDayAdjustment} property is used instead.
   * @return the optional value of the property, not null
   */
  public Optional<BusinessDayAdjustment> getStartDateBusinessDayAdjustment() {
    return Optional.ofNullable(startDateBusinessDayAdjustment);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional business day adjustment to apply to the end date.
   * <p>
   * The end date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the end date to a valid business day.
   * <p>
   * If this property is not present, the standard {@code businessDayAdjustment} property is used instead.
   * @return the optional value of the property, not null
   */
  public Optional<BusinessDayAdjustment> getEndDateBusinessDayAdjustment() {
    return Optional.ofNullable(endDateBusinessDayAdjustment);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional convention defining how to handle stubs.
   * <p>
   * The stub convention is used during schedule construction to determine whether the irregular
   * remaining period occurs at the start or end of the schedule.
   * It also determines whether the irregular period is shorter or longer than the regular period.
   * This property interacts with the "explicit dates" of {@link PeriodicSchedule#getFirstRegularStartDate()}
   * and {@link PeriodicSchedule#getLastRegularEndDate()}.
   * <p>
   * The convention 'None' may be used to explicitly indicate there are no stubs.
   * There must be no explicit dates.
   * This will be validated during schedule construction.
   * <p>
   * The convention 'Both' may be used to explicitly indicate there is both an initial and final stub.
   * The stubs themselves must be specified using explicit dates.
   * This will be validated during schedule construction.
   * <p>
   * The conventions 'ShortInitial', 'LongInitial', 'SmartInitial', 'ShortFinal', 'LongFinal'
   * and 'SmartFinal' are used to indicate the type of stub to be generated.
   * The exact behavior varies depending on whether there are explicit dates or not:
   * <p>
   * If explicit dates are specified, then the combination of stub convention an explicit date
   * will be validated during schedule construction. For example, the combination of an explicit dated
   * initial stub and a stub convention of 'ShortInitial', 'LongInitial' or 'SmartInitial' is valid,
   * but other stub conventions, such as 'ShortFinal' or 'None' would be invalid.
   * <p>
   * If explicit dates are not specified, then it is not required that a stub is generated.
   * The convention determines whether to generate dates from the start date forward, or the
   * end date backwards. Date generation may or may not result in a stub, but if it does then
   * the stub will be of the correct type.
   * <p>
   * When the stub convention is not present, the generation of stubs is based on the presence or absence
   * of the explicit dates. When there are no explicit stubs and there is a roll convention that matches
   * the start or end date, then the stub convention will be defaulted to 'SmartInitial' or 'SmartFinal'.
   * @return the optional value of the property, not null
   */
  public Optional<StubConvention> getStubConvention() {
    return Optional.ofNullable(stubConvention);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional convention defining how to roll dates.
   * <p>
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   * <p>
   * During schedule generation, if this is present it will be used to determine the schedule.
   * If not present, then the roll convention will be implied.
   * @return the optional value of the property, not null
   */
  public Optional<RollConvention> getRollConvention() {
    return Optional.ofNullable(rollConvention);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional start date of the first regular schedule period, which is the end date of the initial stub.
   * <p>
   * This is used to identify the boundary date between the initial stub and the first regular schedule period.
   * <p>
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be on or after 'startDate' and on or before 'endDate'.
   * <p>
   * During schedule generation, if this is present it will be used to determine the schedule.
   * If not present, then the overall schedule start date will be used instead, resulting in no initial stub.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getFirstRegularStartDate() {
    return Optional.ofNullable(firstRegularStartDate);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional end date of the last regular schedule period, which is the start date of the final stub.
   * <p>
   * This is used to identify the boundary date between the last regular schedule period and the final stub.
   * <p>
   * This is an unadjusted date, and as such it might not be a valid business day.
   * This date must be one or after 'startDate', on or after 'firstRegularStartDate' and on or before 'endDate'.
   * <p>
   * During schedule generation, if this is present it will be used to determine the schedule.
   * If not present, then the overall schedule end date will be used instead, resulting in no final stub.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getLastRegularEndDate() {
    return Optional.ofNullable(lastRegularEndDate);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional start date of the first schedule period, overriding normal schedule generation.
   * <p>
   * This property is rarely used, and is generally needed when accrual starts before the effective date.
   * If specified, it overrides the start date of the first period once schedule generation has been completed.
   * Note that all schedule generation rules apply to 'startDate', with this applied as a final step.
   * This field primarily exists to support the FpML 'firstPeriodStartDate' concept.
   * <p>
   * If a roll convention is explicitly specified and the regular start date does not match it,
   * then the override will be used when generating regular periods.
   * <p>
   * If set, it should be different to the start date, although this is not validated.
   * Validation does check that it is on or before 'firstRegularStartDate' and 'lastRegularEndDate',
   * and before 'endDate'.
   * <p>
   * During schedule generation, if this is present it will be used to override the start date
   * of the first generated schedule period.
   * If not present, then the start of the first period will be the normal start date.
   * @return the optional value of the property, not null
   */
  public Optional<AdjustableDate> getOverrideStartDate() {
    return Optional.ofNullable(overrideStartDate);
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
      PeriodicSchedule other = (PeriodicSchedule) obj;
      return JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(frequency, other.frequency) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(startDateBusinessDayAdjustment, other.startDateBusinessDayAdjustment) &&
          JodaBeanUtils.equal(endDateBusinessDayAdjustment, other.endDateBusinessDayAdjustment) &&
          JodaBeanUtils.equal(stubConvention, other.stubConvention) &&
          JodaBeanUtils.equal(rollConvention, other.rollConvention) &&
          JodaBeanUtils.equal(firstRegularStartDate, other.firstRegularStartDate) &&
          JodaBeanUtils.equal(lastRegularEndDate, other.lastRegularEndDate) &&
          JodaBeanUtils.equal(overrideStartDate, other.overrideStartDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(frequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDateBusinessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDateBusinessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(stubConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(rollConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(firstRegularStartDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(lastRegularEndDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(overrideStartDate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(384);
    buf.append("PeriodicSchedule{");
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
    buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
    buf.append("startDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(startDateBusinessDayAdjustment)).append(',').append(' ');
    buf.append("endDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(endDateBusinessDayAdjustment)).append(',').append(' ');
    buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
    buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ');
    buf.append("firstRegularStartDate").append('=').append(JodaBeanUtils.toString(firstRegularStartDate)).append(',').append(' ');
    buf.append("lastRegularEndDate").append('=').append(JodaBeanUtils.toString(lastRegularEndDate)).append(',').append(' ');
    buf.append("overrideStartDate").append('=').append(JodaBeanUtils.toString(overrideStartDate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PeriodicSchedule}.
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
        this, "startDate", PeriodicSchedule.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", PeriodicSchedule.class, LocalDate.class);
    /**
     * The meta-property for the {@code frequency} property.
     */
    private final MetaProperty<Frequency> frequency = DirectMetaProperty.ofImmutable(
        this, "frequency", PeriodicSchedule.class, Frequency.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", PeriodicSchedule.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code startDateBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> startDateBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "startDateBusinessDayAdjustment", PeriodicSchedule.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code endDateBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> endDateBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "endDateBusinessDayAdjustment", PeriodicSchedule.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code stubConvention} property.
     */
    private final MetaProperty<StubConvention> stubConvention = DirectMetaProperty.ofImmutable(
        this, "stubConvention", PeriodicSchedule.class, StubConvention.class);
    /**
     * The meta-property for the {@code rollConvention} property.
     */
    private final MetaProperty<RollConvention> rollConvention = DirectMetaProperty.ofImmutable(
        this, "rollConvention", PeriodicSchedule.class, RollConvention.class);
    /**
     * The meta-property for the {@code firstRegularStartDate} property.
     */
    private final MetaProperty<LocalDate> firstRegularStartDate = DirectMetaProperty.ofImmutable(
        this, "firstRegularStartDate", PeriodicSchedule.class, LocalDate.class);
    /**
     * The meta-property for the {@code lastRegularEndDate} property.
     */
    private final MetaProperty<LocalDate> lastRegularEndDate = DirectMetaProperty.ofImmutable(
        this, "lastRegularEndDate", PeriodicSchedule.class, LocalDate.class);
    /**
     * The meta-property for the {@code overrideStartDate} property.
     */
    private final MetaProperty<AdjustableDate> overrideStartDate = DirectMetaProperty.ofImmutable(
        this, "overrideStartDate", PeriodicSchedule.class, AdjustableDate.class);
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
        "lastRegularEndDate",
        "overrideStartDate");

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
        case -599936828:  // overrideStartDate
          return overrideStartDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public PeriodicSchedule.Builder builder() {
      return new PeriodicSchedule.Builder();
    }

    @Override
    public Class<? extends PeriodicSchedule> beanType() {
      return PeriodicSchedule.class;
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

    /**
     * The meta-property for the {@code overrideStartDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDate> overrideStartDate() {
      return overrideStartDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((PeriodicSchedule) bean).getStartDate();
        case -1607727319:  // endDate
          return ((PeriodicSchedule) bean).getEndDate();
        case -70023844:  // frequency
          return ((PeriodicSchedule) bean).getFrequency();
        case -1065319863:  // businessDayAdjustment
          return ((PeriodicSchedule) bean).getBusinessDayAdjustment();
        case 429197561:  // startDateBusinessDayAdjustment
          return ((PeriodicSchedule) bean).startDateBusinessDayAdjustment;
        case -734327136:  // endDateBusinessDayAdjustment
          return ((PeriodicSchedule) bean).endDateBusinessDayAdjustment;
        case -31408449:  // stubConvention
          return ((PeriodicSchedule) bean).stubConvention;
        case -10223666:  // rollConvention
          return ((PeriodicSchedule) bean).rollConvention;
        case 2011803076:  // firstRegularStartDate
          return ((PeriodicSchedule) bean).firstRegularStartDate;
        case -1540679645:  // lastRegularEndDate
          return ((PeriodicSchedule) bean).lastRegularEndDate;
        case -599936828:  // overrideStartDate
          return ((PeriodicSchedule) bean).overrideStartDate;
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
   * The bean-builder for {@code PeriodicSchedule}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<PeriodicSchedule> {

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
    private AdjustableDate overrideStartDate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(PeriodicSchedule beanToCopy) {
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.frequency = beanToCopy.getFrequency();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
      this.startDateBusinessDayAdjustment = beanToCopy.startDateBusinessDayAdjustment;
      this.endDateBusinessDayAdjustment = beanToCopy.endDateBusinessDayAdjustment;
      this.stubConvention = beanToCopy.stubConvention;
      this.rollConvention = beanToCopy.rollConvention;
      this.firstRegularStartDate = beanToCopy.firstRegularStartDate;
      this.lastRegularEndDate = beanToCopy.lastRegularEndDate;
      this.overrideStartDate = beanToCopy.overrideStartDate;
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
        case -599936828:  // overrideStartDate
          return overrideStartDate;
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
        case -599936828:  // overrideStartDate
          this.overrideStartDate = (AdjustableDate) newValue;
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
    public PeriodicSchedule build() {
      return new PeriodicSchedule(
          startDate,
          endDate,
          frequency,
          businessDayAdjustment,
          startDateBusinessDayAdjustment,
          endDateBusinessDayAdjustment,
          stubConvention,
          rollConvention,
          firstRegularStartDate,
          lastRegularEndDate,
          overrideStartDate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the start date, which is the start of the first schedule period.
     * <p>
     * This is the start date of the schedule, it is unadjusted and as such might be a weekend or holiday.
     * Any applicable business day adjustment will be applied when creating the schedule.
     * This is also known as the unadjusted effective date.
     * <p>
     * In most cases, the start date of a financial instrument is just after the trade date,
     * such as two business days later. However, the start date of a schedule is permitted
     * to be any date, which includes dates before or after the trade date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date, which is the end of the last schedule period.
     * <p>
     * This is the end date of the schedule, it is unadjusted and as such might be a weekend or holiday.
     * Any applicable business day adjustment will be applied when creating the schedule.
     * This is also known as the unadjusted maturity date or unadjusted termination date.
     * This date must be after the start date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the regular periodic frequency to use.
     * <p>
     * Most dates are calculated using a regular periodic frequency, such as every 3 months.
     * The actual day-of-month or day-of-week is selected using the roll and stub conventions.
     * @param frequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder frequency(Frequency frequency) {
      JodaBeanUtils.notNull(frequency, "frequency");
      this.frequency = frequency;
      return this;
    }

    /**
     * Sets the business day adjustment to apply.
     * <p>
     * Each date in the calculated schedule is determined without taking into account weekends and holidays.
     * The adjustment specified here is used to convert those dates to valid business days.
     * <p>
     * The start date and end date may have their own business day adjustment rules.
     * If those are not present, then this adjustment is used instead.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the optional business day adjustment to apply to the start date.
     * <p>
     * The start date property is an unadjusted date and as such might be a weekend or holiday.
     * The adjustment specified here is used to convert the start date to a valid business day.
     * <p>
     * If this property is not present, the standard {@code businessDayAdjustment} property is used instead.
     * @param startDateBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder startDateBusinessDayAdjustment(BusinessDayAdjustment startDateBusinessDayAdjustment) {
      this.startDateBusinessDayAdjustment = startDateBusinessDayAdjustment;
      return this;
    }

    /**
     * Sets the optional business day adjustment to apply to the end date.
     * <p>
     * The end date property is an unadjusted date and as such might be a weekend or holiday.
     * The adjustment specified here is used to convert the end date to a valid business day.
     * <p>
     * If this property is not present, the standard {@code businessDayAdjustment} property is used instead.
     * @param endDateBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder endDateBusinessDayAdjustment(BusinessDayAdjustment endDateBusinessDayAdjustment) {
      this.endDateBusinessDayAdjustment = endDateBusinessDayAdjustment;
      return this;
    }

    /**
     * Sets the optional convention defining how to handle stubs.
     * <p>
     * The stub convention is used during schedule construction to determine whether the irregular
     * remaining period occurs at the start or end of the schedule.
     * It also determines whether the irregular period is shorter or longer than the regular period.
     * This property interacts with the "explicit dates" of {@link PeriodicSchedule#getFirstRegularStartDate()}
     * and {@link PeriodicSchedule#getLastRegularEndDate()}.
     * <p>
     * The convention 'None' may be used to explicitly indicate there are no stubs.
     * There must be no explicit dates.
     * This will be validated during schedule construction.
     * <p>
     * The convention 'Both' may be used to explicitly indicate there is both an initial and final stub.
     * The stubs themselves must be specified using explicit dates.
     * This will be validated during schedule construction.
     * <p>
     * The conventions 'ShortInitial', 'LongInitial', 'SmartInitial', 'ShortFinal', 'LongFinal'
     * and 'SmartFinal' are used to indicate the type of stub to be generated.
     * The exact behavior varies depending on whether there are explicit dates or not:
     * <p>
     * If explicit dates are specified, then the combination of stub convention an explicit date
     * will be validated during schedule construction. For example, the combination of an explicit dated
     * initial stub and a stub convention of 'ShortInitial', 'LongInitial' or 'SmartInitial' is valid,
     * but other stub conventions, such as 'ShortFinal' or 'None' would be invalid.
     * <p>
     * If explicit dates are not specified, then it is not required that a stub is generated.
     * The convention determines whether to generate dates from the start date forward, or the
     * end date backwards. Date generation may or may not result in a stub, but if it does then
     * the stub will be of the correct type.
     * <p>
     * When the stub convention is not present, the generation of stubs is based on the presence or absence
     * of the explicit dates. When there are no explicit stubs and there is a roll convention that matches
     * the start or end date, then the stub convention will be defaulted to 'SmartInitial' or 'SmartFinal'.
     * @param stubConvention  the new value
     * @return this, for chaining, not null
     */
    public Builder stubConvention(StubConvention stubConvention) {
      this.stubConvention = stubConvention;
      return this;
    }

    /**
     * Sets the optional convention defining how to roll dates.
     * <p>
     * The schedule periods are determined at the high level by repeatedly adding
     * the frequency to the start date, or subtracting it from the end date.
     * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
     * <p>
     * During schedule generation, if this is present it will be used to determine the schedule.
     * If not present, then the roll convention will be implied.
     * @param rollConvention  the new value
     * @return this, for chaining, not null
     */
    public Builder rollConvention(RollConvention rollConvention) {
      this.rollConvention = rollConvention;
      return this;
    }

    /**
     * Sets the optional start date of the first regular schedule period, which is the end date of the initial stub.
     * <p>
     * This is used to identify the boundary date between the initial stub and the first regular schedule period.
     * <p>
     * This is an unadjusted date, and as such it might not be a valid business day.
     * This date must be on or after 'startDate' and on or before 'endDate'.
     * <p>
     * During schedule generation, if this is present it will be used to determine the schedule.
     * If not present, then the overall schedule start date will be used instead, resulting in no initial stub.
     * @param firstRegularStartDate  the new value
     * @return this, for chaining, not null
     */
    public Builder firstRegularStartDate(LocalDate firstRegularStartDate) {
      this.firstRegularStartDate = firstRegularStartDate;
      return this;
    }

    /**
     * Sets the optional end date of the last regular schedule period, which is the start date of the final stub.
     * <p>
     * This is used to identify the boundary date between the last regular schedule period and the final stub.
     * <p>
     * This is an unadjusted date, and as such it might not be a valid business day.
     * This date must be one or after 'startDate', on or after 'firstRegularStartDate' and on or before 'endDate'.
     * <p>
     * During schedule generation, if this is present it will be used to determine the schedule.
     * If not present, then the overall schedule end date will be used instead, resulting in no final stub.
     * @param lastRegularEndDate  the new value
     * @return this, for chaining, not null
     */
    public Builder lastRegularEndDate(LocalDate lastRegularEndDate) {
      this.lastRegularEndDate = lastRegularEndDate;
      return this;
    }

    /**
     * Sets the optional start date of the first schedule period, overriding normal schedule generation.
     * <p>
     * This property is rarely used, and is generally needed when accrual starts before the effective date.
     * If specified, it overrides the start date of the first period once schedule generation has been completed.
     * Note that all schedule generation rules apply to 'startDate', with this applied as a final step.
     * This field primarily exists to support the FpML 'firstPeriodStartDate' concept.
     * <p>
     * If a roll convention is explicitly specified and the regular start date does not match it,
     * then the override will be used when generating regular periods.
     * <p>
     * If set, it should be different to the start date, although this is not validated.
     * Validation does check that it is on or before 'firstRegularStartDate' and 'lastRegularEndDate',
     * and before 'endDate'.
     * <p>
     * During schedule generation, if this is present it will be used to override the start date
     * of the first generated schedule period.
     * If not present, then the start of the first period will be the normal start date.
     * @param overrideStartDate  the new value
     * @return this, for chaining, not null
     */
    public Builder overrideStartDate(AdjustableDate overrideStartDate) {
      this.overrideStartDate = overrideStartDate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(384);
      buf.append("PeriodicSchedule.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("startDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(startDateBusinessDayAdjustment)).append(',').append(' ');
      buf.append("endDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(endDateBusinessDayAdjustment)).append(',').append(' ');
      buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ');
      buf.append("firstRegularStartDate").append('=').append(JodaBeanUtils.toString(firstRegularStartDate)).append(',').append(' ');
      buf.append("lastRegularEndDate").append('=').append(JodaBeanUtils.toString(lastRegularEndDate)).append(',').append(' ');
      buf.append("overrideStartDate").append('=').append(JodaBeanUtils.toString(overrideStartDate));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
