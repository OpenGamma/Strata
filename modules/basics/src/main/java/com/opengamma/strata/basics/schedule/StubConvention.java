/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * A convention defining how to calculate stub periods.
 * <p>
 * A {@linkplain PeriodicSchedule periodic schedule} is determined using a periodic frequency.
 * This splits the schedule into "regular" periods of a fixed length, such as every 3 months.
 * Any remaining days are allocated to irregular "stubs" at the start and/or end.
 * <p>
 * The stub convention is provided as a simple declarative mechanism to define stubs.
 * The convention handles the case of no stubs, or a single stub at the start or end.
 * If there is a stub at both the start and end, then explicit stub dates must be used.
 * <p>
 * For example, dividing a 24 month (2 year) swap into 3 month periods is easy as it splits exactly.
 * However, a 23 month swap cannot be split into even 3 month periods.
 * Instead, there will be a 2 month "initial" stub at the start, a 2 month "final" stub at the end
 * or both an initial and final stub with a combined length of 2 months.
 * <p>
 * The 'ShortInitial', 'LongInitial' or 'SmartInitial' convention causes the regular periods to be determined
 * <i>backwards</i> from the end date of the schedule, with remaining days allocated to the stub.
 * <p>
 * The 'ShortFinal', 'LongFinal' or 'SmartFinal' convention causes the regular periods to be determined
 * <i>forwards</i> from the start date of the schedule, with remaining days allocated to the stub.
 * <p>
 * The 'None' convention may be used to explicitly indicate there are no stubs.
 * <p>
 * The 'Both' convention may be used to explicitly indicate there is both an initial and final stub.
 * In this case, dates must be used to identify the stubs.
 */
public enum StubConvention implements NamedEnum {

  /**
   * Explicitly states that there are no stubs.
   * <p>
   * This is used to indicate that the term of the schedule evenly divides by the
   * periodic frequency leaving no stubs.
   * For example, a 6 month trade can be exactly divided by a 3 month frequency.
   * <p>
   * If the term of the schedule is less than the frequency, then only one period exists.
   * In this case, the period is not treated as a stub.
   * <p>
   * When creating a schedule, there must be no explicit stubs.
   */
  NONE {
    @Override
    StubConvention toImplicit(PeriodicSchedule definition, boolean explicitInitialStub, boolean explicitFinalStub) {
      if (explicitInitialStub || explicitFinalStub) {
        throw new ScheduleException(
            definition, "Dates specify an explicit stub, but stub convention is 'None'");
      }
      return NONE;
    }
  },
  /**
   * A short initial stub.
   * <p>
   * The schedule periods will be determined backwards from the regular period end date.
   * Any remaining period, shorter than the standard frequency, will be allocated at the start.
   * <p>
   * For example, an 8 month trade with a 3 month periodic frequency would result in
   * a 2 month initial short stub followed by two periods of 3 months.
   * <p>
   * If there is no remaining period when calculating, then there is no stub.
   * For example, a 6 month trade can be exactly divided by a 3 month frequency.
   * <p>
   * When creating a schedule, there must be no explicit final stub.
   * If there is an explicit initial stub, then this convention is considered to be matched
   * and the remaining period is calculated using the stub convention 'None'.
   */
  SHORT_INITIAL {
    @Override
    StubConvention toImplicit(PeriodicSchedule definition, boolean explicitInitialStub, boolean explicitFinalStub) {
      if (explicitFinalStub) {
        throw new ScheduleException(
            definition, "Dates specify an explicit final stub, but stub convention is 'ShortInitial'");
      }
      return (explicitInitialStub ? NONE : SHORT_INITIAL);
    }
  },
  /**
   * A long initial stub.
   * <p>
   * The schedule periods will be determined backwards from the regular period end date.
   * Any remaining period, shorter than the standard frequency, will be allocated at the start
   * and combined with the next period, making a total period longer than the standard frequency.
   * <p>
   * For example, an 8 month trade with a 3 month periodic frequency would result in
   * a 5 month initial long stub followed by one period of 3 months.
   * <p>
   * If there is no remaining period when calculating, then there is no stub.
   * For example, a 6 month trade can be exactly divided by a 3 month frequency.
   * <p>
   * When creating a schedule, there must be no explicit final stub.
   * If there is an explicit initial stub, then this convention is considered to be matched
   * and the remaining period is calculated using the stub convention 'None'.
   */
  LONG_INITIAL {
    @Override
    StubConvention toImplicit(PeriodicSchedule definition, boolean explicitInitialStub, boolean explicitFinalStub) {
      if (explicitFinalStub) {
        throw new ScheduleException(
            definition, "Dates specify an explicit final stub, but stub convention is 'LongInitial'");
      }
      return (explicitInitialStub ? NONE : LONG_INITIAL);
    }

    @Override
    boolean isStubLong(LocalDate date1, LocalDate date2) {
      return true;
    }
  },
  /**
   * A smart initial stub.
   * <p>
   * The schedule periods will be determined backwards from the regular period end date.
   * Any remaining period, shorter than the standard frequency, will be allocated at the start.
   * If this results in a stub of less than 7 days, the stub will be combined with the next period.
   * If this results in a stub of 7 days or more, the stub will be retained.
   * This is the equivalent of {@link #LONG_INITIAL} up to 7 days and {@link #SHORT_INITIAL} beyond that.
   * The 7 days are calculated based on unadjusted dates.
   * This convention appears to match that used by Bloomberg.
   * <p>
   * If there is no remaining period when calculating, then there is no stub.
   * For example, a 6 month trade can be exactly divided by a 3 month frequency.
   * <p>
   * If there is an explicit initial stub, then this convention is considered to be matched
   * and the remaining period is calculated using the stub convention 'None'.
   */
  SMART_INITIAL {
    @Override
    StubConvention toImplicit(PeriodicSchedule definition, boolean explicitInitialStub, boolean explicitFinalStub) {
      if (explicitFinalStub) {
        return (explicitInitialStub ? BOTH : SMART_INITIAL);
      }
      return (explicitInitialStub ? NONE : SMART_INITIAL);
    }

    @Override
    boolean isStubLong(LocalDate date1, LocalDate date2) {
      return date1.plusDays(7).isAfter(date2);
    }
  },
  /**
   * A short final stub.
   * <p>
   * The schedule periods will be determined forwards from the regular period start date.
   * Any remaining period, shorter than the standard frequency, will be allocated at the end.
   * <p>
   * For example, an 8 month trade with a 3 month periodic frequency would result in
   * two periods of 3 months followed by a 2 month final short stub.
   * <p>
   * If there is no remaining period when calculating, then there is no stub.
   * For example, a 6 month trade can be exactly divided by a 3 month frequency.
   * <p>
   * When creating a schedule, there must be no explicit initial stub.
   * If there is an explicit final stub, then this convention is considered to be matched
   * and the remaining period is calculated using the stub convention 'None'.
   */
  SHORT_FINAL {
    @Override
    StubConvention toImplicit(PeriodicSchedule definition, boolean explicitInitialStub, boolean explicitFinalStub) {
      if (explicitInitialStub) {
        throw new ScheduleException(
            definition, "Dates specify an explicit initial stub, but stub convention is 'ShortFinal'");
      }
      return (explicitFinalStub ? NONE : SHORT_FINAL);
    }
  },
  /**
   * A long final stub.
   * <p>
   * The schedule periods will be determined forwards from the regular period start date.
   * Any remaining period, shorter than the standard frequency, will be allocated at the end
   * and combined with the previous period, making a total period longer than the standard frequency.
   * <p>
   * For example, an 8 month trade with a 3 month periodic frequency would result in
   * one period of 3 months followed by a 5 month final long stub.
   * <p>
   * If there is no remaining period when calculating, then there is no stub.
   * For example, a 6 month trade can be exactly divided by a 3 month frequency.
   * <p>
   * When creating a schedule, there must be no explicit initial stub.
   * If there is an explicit final stub, then this convention is considered to be matched
   * and the remaining period is calculated using the stub convention 'None'.
   */
  LONG_FINAL {
    @Override
    StubConvention toImplicit(PeriodicSchedule definition, boolean explicitInitialStub, boolean explicitFinalStub) {
      if (explicitInitialStub) {
        throw new ScheduleException(
            definition, "Dates specify an explicit initial stub, but stub convention is 'LongFinal'");
      }
      return (explicitFinalStub ? NONE : LONG_FINAL);
    }

    @Override
    boolean isStubLong(LocalDate date1, LocalDate date2) {
      return true;
    }
  },
  /**
   * A smart final stub.
   * <p>
   * The schedule periods will be determined forwards from the regular period start date.
   * Any remaining period, shorter than the standard frequency, will be allocated at the end.
   * If this results in a stub of less than 7 days, the stub will be combined with the next period.
   * If this results in a stub of 7 days or more, the stub will be retained.
   * This is the equivalent of {@link #LONG_FINAL} up to 7 days and {@link #SHORT_FINAL} beyond that.
   * The 7 days are calculated based on unadjusted dates.
   * This convention appears to match that used by Bloomberg.
   * <p>
   * If there is no remaining period when calculating, then there is no stub.
   * For example, a 6 month trade can be exactly divided by a 3 month frequency.
   * <p>
   * If there is an explicit final stub, then this convention is considered to be matched
   * and the remaining period is calculated using the stub convention 'None'.
   */
  SMART_FINAL {
    @Override
    StubConvention toImplicit(PeriodicSchedule definition, boolean explicitInitialStub, boolean explicitFinalStub) {
      if (explicitInitialStub) {
        return (explicitFinalStub ? BOTH : SMART_FINAL);
      }
      return (explicitFinalStub ? NONE : SMART_FINAL);
    }

    @Override
    boolean isStubLong(LocalDate date1, LocalDate date2) {
      return date1.plusDays(7).isAfter(date2);
    }
  },
  /**
   * Both ends of the schedule have a stub.
   * <p>
   * The schedule periods will be determined from two dates - the regular period start date
   * and the regular period end date.
   * Days before the first regular period start date form the initial stub.
   * Days after the last regular period end date form the final stub.
   * <p>
   * When creating a schedule, there must be both an explicit initial and final stub.
   */
  BOTH {
    @Override
    StubConvention toImplicit(PeriodicSchedule definition, boolean explicitInitialStub, boolean explicitFinalStub) {
      if ((explicitInitialStub && explicitFinalStub) == false) {
        throw new ScheduleException(
            definition, "Stub convention is 'Both' but explicit dates not specified");
      }
      return NONE;
    }
  };

  // helper for name conversions
  private static final EnumNames<StubConvention> NAMES = EnumNames.of(StubConvention.class);

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static StubConvention of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this stub convention to the appropriate roll convention.
   * <p>
   * This converts a stub convention to a {@link RollConvention} based on the
   * start date, end date, frequency and preference for end-of-month.
   * The net result is to imply the roll convention from the schedule data.
   * <p>
   * The rules are as follows:
   * <p>
   * If the input frequency is month-based, then the implied convention is based on
   * the day-of-month of the initial date, where the initial date is the start date
   * if rolling forwards or the end date otherwise.
   * If that date is on the 31st day, or if the 'preferEndOfMonth' flag is true and
   * the relevant date is at the end of the month, then the implied convention is 'EOM'.
   * For example, if the initial date of the sequence is 2014-06-20 and the periodic
   * frequency is 'P3M' (month-based), then the implied convention is 'Day20'.
   * <p>
   * If the input frequency is week-based, then the implied convention is based on
   * the day-of-week of the initial date, where the initial date is the start date
   * if rolling forwards or the end date otherwise.
   * For example, if the initial date of the sequence is 2014-06-20 and the periodic
   * frequency is 'P2W' (week-based), then the implied convention is 'DayFri',
   * because 2014-06-20 is a Friday.
   * <p>
   * In all other cases, the implied convention is 'None'.
   * 
   * @param start  the start date of the schedule
   * @param end  the end date of the schedule
   * @param frequency  the periodic frequency of the schedule
   * @param preferEndOfMonth  whether to prefer the end-of-month when rolling
   * @return the derived roll convention
   */
  public RollConvention toRollConvention(
      LocalDate start,
      LocalDate end,
      Frequency frequency,
      boolean preferEndOfMonth) {

    ArgChecker.notNull(start, "start");
    ArgChecker.notNull(end, "end");
    ArgChecker.notNull(frequency, "frequency");
    // if the day-of-month differs, need to handle case where one or both
    // dates are at the end of the month, and in different months
    if (this == NONE && frequency.isMonthBased()) {
      if (start.getDayOfMonth() != end.getDayOfMonth() &&
          start.getLong(PROLEPTIC_MONTH) != end.getLong(PROLEPTIC_MONTH) &&
          (start.getDayOfMonth() == start.lengthOfMonth() || end.getDayOfMonth() == end.lengthOfMonth())) {

        return preferEndOfMonth ?
            RollConventions.EOM :
            RollConvention.ofDayOfMonth(Math.max(start.getDayOfMonth(), end.getDayOfMonth()));
      }
    }
    if (isCalculateBackwards()) {
      return impliedRollConvention(end, start, frequency, preferEndOfMonth);
    } else {
      return impliedRollConvention(start, end, frequency, preferEndOfMonth);
    }
  }

  // helper for converting to roll convention
  private static RollConvention impliedRollConvention(
      LocalDate date,
      LocalDate otherDate,
      Frequency frequency,
      boolean preferEndOfMonth) {

    if (frequency.isMonthBased()) {
      if (preferEndOfMonth && date.getDayOfMonth() == date.lengthOfMonth()) {
        return RollConventions.EOM;
      }
      return RollConvention.ofDayOfMonth(date.getDayOfMonth());
    } else if (frequency.isWeekBased()) {
      return RollConvention.ofDayOfWeek(date.getDayOfWeek());
    } else {
      // neither monthly nor weekly means no known roll convention
      return RollConventions.NONE;
    }
  }

  /**
   * Converts this stub convention to one that creates implicit stubs, validating that
   * any explicit stubs are correct.
   * <p>
   * Stubs can be specified in two ways, using dates or using this convention.
   * This method is passed flags indicating whether explicit stubs have been specified using dates.
   * It validated that such stubs are compatible, and returns a convention suitable for
   * creating stubs implicitly during rolling.
   * <p>
   * For example, an invalid stub convention would be to specify two stubs using explicit dates but
   * declaring the convention as 'ShortFinal'.
   * <p>
   * The result is the implicit stub convention to apply between the two calculation dates.
   * For example, if an initial stub is defined by dates then it cannot also be created automatically,
   * thus the implicit stub convention is 'None'.
   * 
   * @param definition  the schedule definition, for error messages
   * @param explicitInitialStub  an initial stub has been explicitly defined by dates
   * @param explicitFinalStub  a final stub has been explicitly defined by dates
   * @return the effective stub convention
   * @throws ScheduleException if the input data is invalid
   */
  abstract StubConvention toImplicit(
      PeriodicSchedule definition, boolean explicitInitialStub, boolean explicitFinalStub);

  /**
   * Decides if the period between two dates implies a long stub.
   * <p>
   * This is used by the smart conventions.
   * 
   * @param date1  the first date
   * @param date2  the second date
   * @return true if a long stub should be created by deleting one of the two input dates
   */
  boolean isStubLong(LocalDate date1, LocalDate date2) {
    return false;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the schedule is calculated forwards from the start date to the end date.
   * <p>
   * If true, then there will typically be a stub at the end of the schedule.
   * <p>
   * The 'None', 'ShortFinal', 'LongFinal' and 'SmartFinal' conventions return true.
   * Other conventions return false.
   * 
   * @return true if calculation occurs forwards from the start date to the end date
   */
  public boolean isCalculateForwards() {
    return this == SHORT_FINAL || this == LONG_FINAL || this == SMART_FINAL || this == NONE;
  }

  /**
   * Checks if the schedule is calculated backwards from the end date to the start date.
   * <p>
   * If true, then there will typically be a stub at the start of the schedule.
   * <p>
   * The 'ShortInitial', 'LongInitial' and 'SmartInitial' conventions return true.
   * Other conventions return false.
   * 
   * @return true if calculation occurs backwards from the end date to the start date
   */
  public boolean isCalculateBackwards() {
    return this == SHORT_INITIAL || this == LONG_INITIAL || this == SMART_INITIAL;
  }

  /**
   * Checks if this convention tries to produce a final stub.
   * <p>
   * If true, then there will typically be a stub at the end of the schedule.
   * <p>
   * The 'ShortFinal', 'LongFinal' and 'SmartFinal' conventions return true.
   * Other conventions return false.
   * 
   * @return true if calculation occurs forwards from the start date to the end date
   */
  public boolean isFinal() {
    return this == SHORT_FINAL || this == LONG_FINAL || this == SMART_FINAL;
  }

  /**
   * Checks if this convention tries to produce a long stub.
   * <p>
   * The 'LongInitial' and 'LongFinal' conventions return true.
   * Other conventions return false.
   * 
   * @return true if there may be a long stub
   */
  public boolean isLong() {
    return this == LONG_INITIAL || this == LONG_FINAL;
  }

  /**
   * Checks if this convention tries to produce a short stub.
   * <p>
   * The 'ShortInitial' and 'ShortFinal' conventions return true.
   * Other conventions return false.
   * 
   * @return true if there may be a short stub
   */
  public boolean isShort() {
    return this == SHORT_INITIAL || this == SHORT_FINAL;
  }

  /**
   * Checks if this convention uses smart rules to create a stub.
   * <p>
   * The 'SmartInitial' and 'SmartFinal' conventions return true.
   * Other conventions return false.
   * 
   * @return true if there may be a long stub
   */
  public boolean isSmart() {
    return this == SMART_INITIAL || this == SMART_FINAL;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return NAMES.format(this);
  }

}
