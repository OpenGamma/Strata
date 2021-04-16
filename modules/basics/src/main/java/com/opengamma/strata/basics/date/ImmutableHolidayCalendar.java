/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;
import org.joda.beans.ser.SerDeserializer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * An immutable holiday calendar implementation.
 * <p>
 * A standard immutable implementation of {@link HolidayCalendar} that stores all
 * dates that are holidays, plus a list of weekend days.
 * <p>
 * Internally, the class uses a range to determine the range of known holiday dates.
 * Beyond the range of known holiday dates, weekend days are used to determine business days.
 * Dates may be queried from year zero to year 10,000.
 * <p>
 * Applications should refer to holidays using {@link HolidayCalendarId}.
 * The identifier must be {@linkplain HolidayCalendarId#resolve(ReferenceData) resolved}
 * to a {@link HolidayCalendar} before the holiday data methods can be accessed.
 * See {@link HolidayCalendarIds} for a standard set of identifiers available in {@link ReferenceData#standard()}.
 */
@BeanDefinition(builderScope = "private")
public final class ImmutableHolidayCalendar
    implements HolidayCalendar, ImmutableBean, Serializable {
  // optimized implementation of HolidayCalendar
  // uses an int array where each int represents a month
  // each bit within the int represents a date, where 0 is a holiday and 1 is a business day
  // (most logic involves finding business days, finding 1 is easier than finding 0
  // when using Integer.numberOfTrailingZeros and Integer.numberOfLeadingZeros)
  // benchmarking showed nextOrSame() and previousOrSame() do not need to be overridden
  // out-of-range and weekend-only (used in testing) are handled using exceptions to fast-path the common case

  /**
   * The deserializer, for compatibility.
   */
  @SuppressWarnings("unused")
  public static final SerDeserializer DESERIALIZER = new ImmutableHolidayCalendarDeserializer();

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 2L;

  /**
   * The identifier, such as 'GBLO'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final HolidayCalendarId id;
  /**
   * The set of weekend days.
   * <p>
   * Each date that has a day-of-week matching one of these days is not a business day.
   */
  @PropertyDefinition(get = "")
  private final int weekends;
  /**
   * The start year.
   * Used as the base year for the lookup table.
   */
  @PropertyDefinition(get = "")
  private final int startYear;
  /**
   * The lookup table, where each item represents a month from January of startYear onwards.
   * Bits 0 to 31 are used for each day-of-month, where 0 is a holiday and 1 is a business day.
   * Trailing bits are set to 0 so they act as holidays, avoiding month length logic.
   */
  @PropertyDefinition(validate = "notNull", get = "")
  private final int[] lookup;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a set of holiday dates and weekend days.
   * <p>
   * The holiday dates will be extracted into a set with duplicates ignored.
   * The minimum supported date for query is the start of the year of the earliest holiday.
   * The maximum supported date for query is the end of the year of the latest holiday.
   * <p>
   * The weekend days may both be the same.
   * 
   * @param id  the identifier
   * @param holidays  the set of holiday dates
   * @param firstWeekendDay  the first weekend day
   * @param secondWeekendDay  the second weekend day, may be same as first
   * @return the holiday calendar
   */
  public static ImmutableHolidayCalendar of(
      HolidayCalendarId id,
      Iterable<LocalDate> holidays,
      DayOfWeek firstWeekendDay,
      DayOfWeek secondWeekendDay) {

    ImmutableSet<DayOfWeek> weekendDays = Sets.immutableEnumSet(firstWeekendDay, secondWeekendDay);
    return of(id, ImmutableSortedSet.copyOf(holidays), weekendDays, ImmutableSet.of());
  }

  /**
   * Obtains an instance from a set of holiday dates and weekend days.
   * <p>
   * The holiday dates will be extracted into a set with duplicates ignored.
   * The minimum supported date for query is the start of the year of the earliest holiday.
   * The maximum supported date for query is the end of the year of the latest holiday.
   * <p>
   * The weekend days may be empty, in which case the holiday dates should contain any weekends.
   * 
   * @param id  the identifier
   * @param holidays  the set of holiday dates
   * @param weekendDays  the days that define the weekend, if empty then weekends are treated as business days
   * @return the holiday calendar
   */
  public static ImmutableHolidayCalendar of(
      HolidayCalendarId id,
      Iterable<LocalDate> holidays,
      Iterable<DayOfWeek> weekendDays) {

    return of(id, ImmutableSortedSet.copyOf(holidays), Sets.immutableEnumSet(weekendDays), ImmutableSet.of());
  }

  /**
   * Obtains an instance from a set of holiday dates and weekend days.
   * <p>
   * The holiday dates will be extracted into a set with duplicates ignored.
   * The minimum supported date for query is the start of the year of the earliest holiday.
   * The maximum supported date for query is the end of the year of the latest holiday.
   * <p>
   * The weekend days may be empty, in which case the holiday dates should contain any weekends.
   * The working days are processed last, changing holidays and weekends back to business days,
   * but only within the range of supported years.
   * 
   * @param id  the identifier
   * @param holidays  the set of holiday dates
   * @param weekendDays  the days that define the weekend, if empty then weekends are treated as business days
   * @param workingDays  the working days that override holidays and weekends
   * @return the holiday calendar
   */
  public static ImmutableHolidayCalendar of(
      HolidayCalendarId id,
      Iterable<LocalDate> holidays,
      Iterable<DayOfWeek> weekendDays,
      Iterable<LocalDate> workingDays) {

    return of(
        id,
        ImmutableSortedSet.copyOf(holidays),
        Sets.immutableEnumSet(weekendDays),
        ArgChecker.notNull(workingDays, "workingDays"));
  }

  /**
   * Obtains a combined holiday calendar instance.
   * <p>
   * This combines the two input calendars.
   * It is intended for up-front occasional use rather than continuous use, as it can be relatively slow.
   * 
   * @param cal1  the first calendar
   * @param cal2  the second calendar
   * @return the combined calendar
   */
  public static ImmutableHolidayCalendar combined(ImmutableHolidayCalendar cal1, ImmutableHolidayCalendar cal2) {
    // do not override combinedWith(), as this is too slow
    if (cal1 == cal2) {
      return ArgChecker.notNull(cal1, "cal1");
    }
    HolidayCalendarId newId = cal1.id.combinedWith(cal2.id);

    // use slow version if lookup arrays do not overlap
    int endYear1 = cal1.startYear + cal1.lookup.length / 12;
    int endYear2 = cal2.startYear + cal2.lookup.length / 12;
    if (endYear1 < cal2.startYear || endYear2 < cal1.startYear) {
      Pair<ImmutableSortedSet<LocalDate>, ImmutableSortedSet<LocalDate>> holsWork1 = cal1.getHolidaysAndWorkingDays();
      Pair<ImmutableSortedSet<LocalDate>, ImmutableSortedSet<LocalDate>> holsWork2 = cal2.getHolidaysAndWorkingDays();
      ImmutableSortedSet<LocalDate> newHolidays =
          ImmutableSortedSet.copyOf(Iterables.concat(holsWork1.getFirst(), holsWork2.getFirst()));
      ImmutableSet<DayOfWeek> newWeekends =
          ImmutableSet.copyOf(Iterables.concat(cal1.getWeekendDays(), cal2.getWeekendDays()));
      ImmutableSet<LocalDate> newWorkingDays =
          ImmutableSet.copyOf(Iterables.concat(holsWork1.getSecond(), holsWork2.getSecond()));
      return of(newId, newHolidays, newWeekends, newWorkingDays);
    }

    // merge calendars using bitwise operations
    // figure out which has the lower start year and use that as the base
    boolean cal1Lower = cal1.startYear <= cal2.startYear;
    int[] lookup1 = cal1Lower ? cal1.lookup : cal2.lookup;
    int[] lookup2 = cal1Lower ? cal2.lookup : cal1.lookup;
    int newStartYear = cal1Lower ? cal1.startYear : cal2.startYear;
    int otherStartYear = cal1Lower ? cal2.startYear : cal1.startYear;
    // copy base array and map data from the other on top
    int newSize = Math.max(lookup1.length, lookup2.length + (otherStartYear - newStartYear) * 12);
    int offset = (otherStartYear - newStartYear) * 12;
    int[] newLookup = Arrays.copyOf(lookup1, newSize);
    for (int i = 0; i < lookup2.length; i++) {
      newLookup[i + offset] &= lookup2[i]; // use & because 1 = business day (not holiday)
    }
    int newWeekends = cal1.weekends | cal2.weekends; // use | because 1 = weekend day
    return new ImmutableHolidayCalendar(newId, newWeekends, newStartYear, newLookup, false);
  }

  // creates an instance calculating the supported range
  static ImmutableHolidayCalendar of(
      HolidayCalendarId id,
      SortedSet<LocalDate> holidays,
      Set<DayOfWeek> weekendDays,
      Iterable<LocalDate> workingDays) {

    ArgChecker.notNull(id, "id");
    int weekends = weekendDays.stream().mapToInt(dow -> 1 << (dow.getValue() - 1)).sum();
    // initial case where no holiday dates are specified
    int startYear = 0;
    int[] lookup = new int[0];
    if (!holidays.isEmpty()) {
      // normal case where holidays are specified
      startYear = holidays.first().getYear();
      int endYearExclusive = holidays.last().getYear() + 1;
      lookup = buildLookupArray(holidays, weekendDays, startYear, endYearExclusive, workingDays);
    }
    return new ImmutableHolidayCalendar(id, weekends, startYear, lookup);
  }

  // create and populate the int[] lookup
  // use 1 for business days and 0 for holidays
  private static int[] buildLookupArray(
      Iterable<LocalDate> holidays,
      Iterable<DayOfWeek> weekendDays,
      int startYear,
      int endYearExclusive,
      Iterable<LocalDate> workingDays) {

    // array that has one entry for each month
    int[] array = new int[(endYearExclusive - startYear) * 12];
    // loop through all months to handle end-of-month and weekends
    LocalDate firstOfMonth = LocalDate.of(startYear, 1, 1);
    for (int i = 0; i < array.length; i++) {
      int monthLen = firstOfMonth.lengthOfMonth();
      // set each valid day-of-month to be a business day
      // the bits for days beyond the end-of-month will be unset and thus treated as non-business days
      // the minus one part converts a single set bit into each lower bit being set
      array[i] = (1 << monthLen) - 1;
      // unset the bits associated with a weekend
      // can unset across whole month using repeating pattern of 7 bits
      // just need to find the offset between the weekend and the day-of-week of the 1st of the month
      for (DayOfWeek weekendDow : weekendDays) {
        int daysDiff = weekendDow.getValue() - firstOfMonth.getDayOfWeek().getValue();
        int offset = (daysDiff < 0 ? daysDiff + 7 : daysDiff);
        array[i] &= ~(0b10000001000000100000010000001 << offset); // CSIGNORE
      }
      firstOfMonth = firstOfMonth.plusMonths(1);
    }
    // unset the bit associated with each holiday date
    for (LocalDate date : holidays) {
      int index = (date.getYear() - startYear) * 12 + date.getMonthValue() - 1;
      array[index] &= ~(1 << (date.getDayOfMonth() - 1));
    }
    // set the bit associated with each overriding working day
    for (LocalDate date : workingDays) {
      if (date.getYear() < startYear || date.getYear() >= endYearExclusive) {
        continue;
      }
      int index = (date.getYear() - startYear) * 12 + date.getMonthValue() - 1;
      array[index] |= (1 << (date.getDayOfMonth() - 1));
    }
    return array;
  }

  //-------------------------------------------------------------------------
  // writes the binary format
  void writeExternal(DataOutput out) throws IOException {
    out.writeUTF(id.getName());
    out.writeShort(weekends);  // using short rather than byte helps align data with 4 char identifiers
    out.writeShort(startYear);
    out.writeShort(lookup.length);
    for (int i = 0; i < lookup.length; i++) {
      out.writeInt(lookup[i]);
    }
  }

  // reads the binary format
  static ImmutableHolidayCalendar readExternal(DataInput in) throws IOException {
    String id = in.readUTF();
    int weekendDays = in.readShort();
    int startYear = in.readShort();
    int lookupSize = in.readShort();
    // this logic was found to be the fastest way to deserialize the int array
    byte[] bytes = new byte[lookupSize * 4];
    int[] lookup = new int[lookupSize];
    in.readFully(bytes);
    int offset = 0;
    for (int i = 0; i < lookupSize; i++) {
      lookup[i] =
          ((bytes[offset++] & 0xFF) << 24) | ((bytes[offset++] & 0xFF) << 16) |
              ((bytes[offset++] & 0xFF) << 8) | (bytes[offset++] & 0xFF);
    }
    return new ImmutableHolidayCalendar(HolidayCalendarId.of(id), weekendDays, startYear, lookup, false);
  }

  //-------------------------------------------------------------------------
  // creates an instance, not cloning the lookup
  ImmutableHolidayCalendar(HolidayCalendarId id, int weekendDays, int startYear, int[] lookup, boolean flag) {
    this.id = ArgChecker.notNull(id, "id");
    this.weekends = weekendDays;
    this.startYear = startYear;
    this.lookup = ArgChecker.notNull(lookup, "lookup");
  }

  //-------------------------------------------------------------------------
  // returns the holidays as a set
  @VisibleForTesting
  ImmutableSortedSet<LocalDate> getHolidays() {
    return getHolidaysAndWorkingDays().getFirst();
  }

  // returns the weekend days as a set
  @VisibleForTesting
  ImmutableSet<DayOfWeek> getWeekendDays() {
    return Stream.of(DayOfWeek.values())
        .filter(dow -> (weekends & (1 << dow.ordinal())) != 0)
        .collect(toImmutableSet());
  }

  // returns the working day overrides as a set
  @VisibleForTesting
  ImmutableSortedSet<LocalDate> getWorkingDays() {
    return getHolidaysAndWorkingDays().getSecond();
  }

  // returns the working day overrides as a set
  private Pair<ImmutableSortedSet<LocalDate>, ImmutableSortedSet<LocalDate>> getHolidaysAndWorkingDays() {
    if (startYear == 0) {
      return Pair.of(ImmutableSortedSet.of(), ImmutableSortedSet.of());
    }
    ImmutableSortedSet.Builder<LocalDate> holidays = ImmutableSortedSet.naturalOrder();
    ImmutableSortedSet.Builder<LocalDate> workingDays = ImmutableSortedSet.naturalOrder();
    LocalDate firstOfMonth = LocalDate.of(startYear, 1, 1);
    for (int i = 0; i < lookup.length; i++) {
      int monthData = lookup[i];
      int monthLen = firstOfMonth.lengthOfMonth();
      int dow0 = firstOfMonth.getDayOfWeek().ordinal();
      int bit = 1;
      for (int j = 0; j < monthLen; j++) {
        // if it is a holiday and not a weekend, then add the date
        if ((monthData & bit) == 0 && (weekends & (1 << dow0)) == 0) {
          holidays.add(firstOfMonth.withDayOfMonth(j + 1));
        }
        // if it is a working day and a weekend, then add the date
        if ((monthData & bit) != 0 && (weekends & (1 << dow0)) != 0) {
          workingDays.add(firstOfMonth.withDayOfMonth(j + 1));
        }
        dow0 = (dow0 + 1) % 7;
        bit <<= 1;
      }
      firstOfMonth = firstOfMonth.plusMonths(1);
    }
    return Pair.of(holidays.build(), workingDays.build());
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isHoliday(LocalDate date) {
    try {
      // find data for month
      int index = (date.getYear() - startYear) * 12 + date.getMonthValue() - 1;
      // check if bit is 1 at zero-based day-of-month
      return (lookup[index] & (1 << (date.getDayOfMonth() - 1))) == 0;

    } catch (ArrayIndexOutOfBoundsException ex) {
      return isHolidayOutOfRange(date);
    }
  }

  // pulled out to aid hotspot inlining
  private boolean isHolidayOutOfRange(LocalDate date) {
    if (date.getYear() >= 0 && date.getYear() < 10000) {
      return (weekends & (1 << date.getDayOfWeek().ordinal())) != 0;
    }
    throw new IllegalArgumentException("Date is outside the accepted range (year 0000 to 10,000): " + date);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate shift(LocalDate date, int amount) {
    try {
      if (amount > 0) {
        // day-of-month: minus one for zero-based day-of-month, plus one to start from next day
        return shiftNext(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), amount);
      } else if (amount < 0) {
        // day-of-month: minus one to start from previous day
        return shiftPrev(date.getYear(), date.getMonthValue(), date.getDayOfMonth() - 1, amount);
      }
      return date;

    } catch (ArrayIndexOutOfBoundsException ex) {
      return shiftOutOfRange(date, amount);
    }
  }

  // pulled out to aid hotspot inlining
  private LocalDate shiftOutOfRange(LocalDate date, int amount) {
    if (date.getYear() >= 0 && date.getYear() < 10000) {
      return HolidayCalendar.super.shift(date, amount);
    }
    throw new IllegalArgumentException("Date is outside the accepted range (year 0000 to 10,000): " + date);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate next(LocalDate date) {
    try {
      // day-of-month: minus one for zero-based day-of-month, plus one to start from next day
      return shiftNext(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), 1);

    } catch (ArrayIndexOutOfBoundsException ex) {
      return HolidayCalendar.super.next(date);
    }
  }

  // shift to a later working day, following nextOrSame semantics
  // input day-of-month is zero-based
  private LocalDate shiftNext(int baseYear, int baseMonth, int baseDom0, int amount) {
    // find data for month
    int index = (baseYear - startYear) * 12 + baseMonth - 1;
    int monthData = lookup[index];
    // loop around amount, the number of days to shift by
    // use domOffset to keep track of day-of-month
    int domOffset = baseDom0;
    for (int amt = amount; amt > 0; amt--) {
      // shift to move the target day-of-month into bit-0, removing earlier days
      int shifted = monthData >>> domOffset;
      // recurse to next month if no more business days in the month
      if (shifted == 0) {
        return baseMonth == 12 ? shiftNext(baseYear + 1, 1, 0, amt) : shiftNext(baseYear, baseMonth + 1, 0, amt);
      }
      // find least significant bit, which is next business day
      // use JDK numberOfTrailingZeros() method which is mapped to a fast intrinsic
      domOffset += (Integer.numberOfTrailingZeros(shifted) + 1);
    }
    return LocalDate.of(baseYear, baseMonth, domOffset);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate previous(LocalDate date) {
    try {
      // day-of-month: minus one to start from previous day
      return shiftPrev(date.getYear(), date.getMonthValue(), date.getDayOfMonth() - 1, -1);

    } catch (ArrayIndexOutOfBoundsException ex) {
      return previousOutOfRange(date);
    }
  }

  // shift to an earlier working day, following previousOrSame semantics
  // input day-of-month is one-based and may be zero or negative
  private LocalDate shiftPrev(int baseYear, int baseMonth, int baseDom, int amount) {
    // find data for month
    int index = (baseYear - startYear) * 12 + baseMonth - 1;
    int monthData = lookup[index];
    // loop around amount, the number of days to shift by
    // use domOffset to keep track of day-of-month
    int domOffset = baseDom;
    for (int amt = amount; amt < 0; amt++) {
      // shift to move the target day-of-month into bit-31, removing later days
      int shifted = (monthData << (32 - domOffset));
      // recurse to previous month if no more business days in the month
      if (shifted == 0 || domOffset <= 0) {
        return baseMonth == 1 ? shiftPrev(baseYear - 1, 12, 31, amt) : shiftPrev(baseYear, baseMonth - 1, 31, amt);
      }
      // find most significant bit, which is previous business day
      // use JDK numberOfLeadingZeros() method which is mapped to a fast intrinsic
      domOffset -= (Integer.numberOfLeadingZeros(shifted) + 1);
    }
    return LocalDate.of(baseYear, baseMonth, domOffset + 1);
  }

  // pulled out to aid hotspot inlining
  private LocalDate previousOutOfRange(LocalDate date) {
    if (date.getYear() >= 0 && date.getYear() < 10000) {
      return HolidayCalendar.super.previous(date);
    }
    throw new IllegalArgumentException("Date is outside the accepted range (year 0000 to 10,000): " + date);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate nextSameOrLastInMonth(LocalDate date) {
    try {
      // day-of-month: no alteration as method is one-based and same is valid
      return shiftNextSameLast(date);

    } catch (ArrayIndexOutOfBoundsException ex) {
      return HolidayCalendar.super.nextSameOrLastInMonth(date);
    }
  }

  // shift to a later working day, following nextOrSame semantics
  // falling back to the last business day-of-month to avoid crossing a month boundary
  // input day-of-month is one-based
  private LocalDate shiftNextSameLast(LocalDate baseDate) {
    int baseYear = baseDate.getYear();
    int baseMonth = baseDate.getMonthValue();
    int baseDom = baseDate.getDayOfMonth();
    // find data for month
    int index = (baseYear - startYear) * 12 + baseMonth - 1;
    int monthData = lookup[index];
    // shift to move the target day-of-month into bit-0, removing earlier days
    int shifted = monthData >>> (baseDom - 1);
    // return last business day-of-month if no more business days in the month
    int dom;
    if (shifted == 0) {
      // need to find the most significant bit, which is the last business day
      // use JDK numberOfLeadingZeros() method which is mapped to a fast intrinsic
      int leading = Integer.numberOfLeadingZeros(monthData);
      dom = 32 - leading;
    } else {
      // find least significant bit, which is the next/same business day
      // use JDK numberOfTrailingZeros() method which is mapped to a fast intrinsic
      dom = baseDom + Integer.numberOfTrailingZeros(shifted);
    }
    // only one call to LocalDate to aid inlining
    return baseDate.withDayOfMonth(dom);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isLastBusinessDayOfMonth(LocalDate date) {
    try {
      // find data for month
      int index = (date.getYear() - startYear) * 12 + date.getMonthValue() - 1;
      // shift right, leaving the input date as bit-0 and filling with 0 on the left
      // if the result is 1, which is all zeroes and a final 1 (...0001) then it is last business day of month
      return (lookup[index] >>> (date.getDayOfMonth() - 1)) == 1;

    } catch (ArrayIndexOutOfBoundsException ex) {
      return isLastBusinessDayOfMonthOutOfRange(date);
    }
  }

  // pulled out to aid hotspot inlining
  private boolean isLastBusinessDayOfMonthOutOfRange(LocalDate date) {
    if (date.getYear() >= 0 && date.getYear() < 10000) {
      return HolidayCalendar.super.isLastBusinessDayOfMonth(date);
    }
    throw new IllegalArgumentException("Date is outside the accepted range (year 0000 to 10,000): " + date);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate lastBusinessDayOfMonth(LocalDate date) {
    try {
      // find data for month
      int index = (date.getYear() - startYear) * 12 + date.getMonthValue() - 1;
      // need to find the most significant bit, which is the last business day
      // use JDK numberOfLeadingZeros() method which is mapped to a fast intrinsic
      int leading = Integer.numberOfLeadingZeros(lookup[index]);
      return date.withDayOfMonth(32 - leading);

    } catch (ArrayIndexOutOfBoundsException ex) {
      return lastBusinessDayOfMonthOutOfRange(date);
    }
  }

  // pulled out to aid hotspot inlining
  private LocalDate lastBusinessDayOfMonthOutOfRange(LocalDate date) {
    if (date.getYear() >= 0 && date.getYear() < 10000) {
      return HolidayCalendar.super.lastBusinessDayOfMonth(date);
    }
    throw new IllegalArgumentException("Date is outside the accepted range (year 0000 to 10,000): " + date);
  }

  //-------------------------------------------------------------------------
  @Override
  public int daysBetween(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.inOrderOrEqual(startInclusive, endExclusive, "startInclusive", "endExclusive");
    try {
      // find data for start and end month
      int startIndex = (startInclusive.getYear() - startYear) * 12 + startInclusive.getMonthValue() - 1;
      int endIndex = (endExclusive.getYear() - startYear) * 12 + endExclusive.getMonthValue() - 1;
      
      // count of first month = ones after day of month inclusive
      // e.g 4th day of month - want holidays from index 3 inclusive
      int start = Integer.bitCount(lookup[startIndex] >>> (startInclusive.getDayOfMonth() - 1));
      // count of last month = ones before day of month exclusive == total for month - ones after end inclusive
      int missingEnd = Integer.bitCount(lookup[endIndex] >>> (endExclusive.getDayOfMonth() - 1));
      if (startIndex == endIndex) {
        // same month - return holidays up to end exclusive 
        return start - missingEnd;
      }
      
      int end = Integer.bitCount(lookup[endIndex]) - missingEnd;
      // otherwise add start and end month counts, and sum months between
      int count = start + end;
      for (int i = startIndex + 1; i < endIndex; i++) {
        count += Integer.bitCount(lookup[i]);
      }
      return count;

    } catch (ArrayIndexOutOfBoundsException ex) {
      return daysBetweenOutOfRange(startInclusive, endExclusive);
    }
  }

  // pulled out to aid hotspot inlining
  private int daysBetweenOutOfRange(LocalDate startInclusive, LocalDate endExclusive) {
    if (startInclusive.getYear() >= 0 && startInclusive.getYear() < 10000 &&
        endExclusive.getYear() >= 0 && endExclusive.getYear() < 10000) {
      return HolidayCalendar.super.daysBetween(startInclusive, endExclusive);
    }
    throw new IllegalArgumentException(Messages.format(
        "Dates are outside the accepted range (year 0000 to 10,000): {}, {}",
        startInclusive, endExclusive));
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof ImmutableHolidayCalendar) {
      return id.equals(((ImmutableHolidayCalendar) obj).id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the name of the calendar.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    return "HolidayCalendar[" + getName() + ']';
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ImmutableHolidayCalendar}.
   * @return the meta-bean, not null
   */
  public static ImmutableHolidayCalendar.Meta meta() {
    return ImmutableHolidayCalendar.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ImmutableHolidayCalendar.Meta.INSTANCE);
  }

  private ImmutableHolidayCalendar(
      HolidayCalendarId id,
      int weekends,
      int startYear,
      int[] lookup) {
    JodaBeanUtils.notNull(id, "id");
    JodaBeanUtils.notNull(lookup, "lookup");
    this.id = id;
    this.weekends = weekends;
    this.startYear = startYear;
    this.lookup = lookup.clone();
  }

  @Override
  public ImmutableHolidayCalendar.Meta metaBean() {
    return ImmutableHolidayCalendar.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier, such as 'GBLO'.
   * @return the value of the property, not null
   */
  @Override
  public HolidayCalendarId getId() {
    return id;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableHolidayCalendar}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code id} property.
     */
    private final MetaProperty<HolidayCalendarId> id = DirectMetaProperty.ofImmutable(
        this, "id", ImmutableHolidayCalendar.class, HolidayCalendarId.class);
    /**
     * The meta-property for the {@code weekends} property.
     */
    private final MetaProperty<Integer> weekends = DirectMetaProperty.ofImmutable(
        this, "weekends", ImmutableHolidayCalendar.class, Integer.TYPE);
    /**
     * The meta-property for the {@code startYear} property.
     */
    private final MetaProperty<Integer> startYear = DirectMetaProperty.ofImmutable(
        this, "startYear", ImmutableHolidayCalendar.class, Integer.TYPE);
    /**
     * The meta-property for the {@code lookup} property.
     */
    private final MetaProperty<int[]> lookup = DirectMetaProperty.ofImmutable(
        this, "lookup", ImmutableHolidayCalendar.class, int[].class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "weekends",
        "startYear",
        "lookup");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case -621930260:  // weekends
          return weekends;
        case -2129150017:  // startYear
          return startYear;
        case -1097094790:  // lookup
          return lookup;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ImmutableHolidayCalendar> builder() {
      return new ImmutableHolidayCalendar.Builder();
    }

    @Override
    public Class<? extends ImmutableHolidayCalendar> beanType() {
      return ImmutableHolidayCalendar.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code id} property.
     * @return the meta-property, not null
     */
    public MetaProperty<HolidayCalendarId> id() {
      return id;
    }

    /**
     * The meta-property for the {@code weekends} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> weekends() {
      return weekends;
    }

    /**
     * The meta-property for the {@code startYear} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> startYear() {
      return startYear;
    }

    /**
     * The meta-property for the {@code lookup} property.
     * @return the meta-property, not null
     */
    public MetaProperty<int[]> lookup() {
      return lookup;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((ImmutableHolidayCalendar) bean).getId();
        case -621930260:  // weekends
          return ((ImmutableHolidayCalendar) bean).weekends;
        case -2129150017:  // startYear
          return ((ImmutableHolidayCalendar) bean).startYear;
        case -1097094790:  // lookup
          return ((ImmutableHolidayCalendar) bean).lookup;
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
   * The bean-builder for {@code ImmutableHolidayCalendar}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<ImmutableHolidayCalendar> {

    private HolidayCalendarId id;
    private int weekends;
    private int startYear;
    private int[] lookup;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case -621930260:  // weekends
          return weekends;
        case -2129150017:  // startYear
          return startYear;
        case -1097094790:  // lookup
          return lookup;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          this.id = (HolidayCalendarId) newValue;
          break;
        case -621930260:  // weekends
          this.weekends = (Integer) newValue;
          break;
        case -2129150017:  // startYear
          this.startYear = (Integer) newValue;
          break;
        case -1097094790:  // lookup
          this.lookup = (int[]) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public ImmutableHolidayCalendar build() {
      return new ImmutableHolidayCalendar(
          id,
          weekends,
          startYear,
          lookup);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ImmutableHolidayCalendar.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("weekends").append('=').append(JodaBeanUtils.toString(weekends)).append(',').append(' ');
      buf.append("startYear").append('=').append(JodaBeanUtils.toString(startYear)).append(',').append(' ');
      buf.append("lookup").append('=').append(JodaBeanUtils.toString(lookup));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
