/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.collect.range;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjuster;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.validate.ArgChecker;

/**
 * A range of local dates.
 * <p>
 * Provides a mechanism to represent a range of dates.
 * Instances can be constructed from either a half-open or a closed range of dates.
 * Internally, both are unified to a single representation.
 * <p>
 * The constants {@link LocalDate#MIN} and {@link LocalDate#MAX} can be used
 * to indicate an unbounded far-past or far-future. Note that there is no difference
 * between a half-open and a closed range when the end is {@link LocalDate#MAX}.
 * 
 * This holds a range of dates.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class LocalDateRange
    implements Serializable {

  /**
   * A range over the whole time-line.
   */
  public static final LocalDateRange ALL = new LocalDateRange(LocalDate.MIN, LocalDate.MAX);

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The start date, inclusive.
   */
  private final LocalDate start;
  /**
   * The end date, inclusive.
   */
  private final LocalDate endInclusive;

  //-------------------------------------------------------------------------
  /**
   * Obtains a half-open range of dates, including the start and excluding the end.
   * <p>
   * The range includes the start date and excludes the end date, unless the end
   * is {@link LocalDate#MAX}. The start date must be before the end date.
   * 
   * @param startInclusive  the inclusive start date, MIN_DATE treated as unbounded
   * @param endExclusive  the exclusive end date, MAX_DATE treated as unbounded
   * @return the half-open range
   */
  public static LocalDateRange halfOpen(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.notNull(startInclusive, "startDate");
    ArgChecker.notNull(endExclusive, "endExclusive");
    LocalDate endInclusive = (endExclusive.isBefore(LocalDate.MAX) ? endExclusive.minusDays(1) : endExclusive);
    return new LocalDateRange(startInclusive, endInclusive);
  }

  /**
   * Obtains a closed range of dates, including the start and end.
   * <p>
   * The range includes the start date and the end date.
   * The start date must be equal to or before the end date.
   * 
   * @param startInclusive  the inclusive start date, MIN_DATE treated as unbounded
   * @param endInclusive  the inclusive end date, MAX_DATE treated as unbounded
   * @return the closed range
   */
  public static LocalDateRange closed(LocalDate startInclusive, LocalDate endInclusive) {
    ArgChecker.notNull(startInclusive, "startDate");
    ArgChecker.notNull(endInclusive, "endExclusive");
    return new LocalDateRange(startInclusive, endInclusive);
  }

  /**
   * Obtains a range consisting of a single date.
   * <p>
   * This is equivalent to calling {@link #closed(LocalDate, LocalDate)} with
   * the single date passed as the start and end.
   * 
   * @param singleDate  the single date in the range, must not be the MIN or MAX date
   * @return the single date range
   */
  public static LocalDateRange single(LocalDate singleDate) {
    ArgChecker.notNull(singleDate, "singleDate");
    return new LocalDateRange(singleDate, singleDate);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses an instance of {@code LocalDateRange} from a standard text
   * representation, such as {@code [2009-12-03,2014-06-30]}.
   * <p>
   * The string must be one of these formats:<br />
   *  {@code [2009-12-03,2014-06-30]}<br />
   *  {@code [2009-12-03,+INFINITY]}<br />
   *  {@code [-INFINITY,2014-06-30]}<br />
   *  {@code [-INFINITY,+INFINITY]}<br />
   * <p>
   * The date must represent a valid date parsed by {@link LocalDate#parse(CharSequence)}.
   *
   * @param text  the text to parse such as "[2009-12-03,2014-06-30]"
   * @return the parsed range
   * @throws IllegalArgumentException if the text cannot be parsed
   * @throws DateTimeParseException if the text cannot be parsed because the date was invalid
   */
  @FromString
  public static LocalDateRange parse(String rangeStr) {
    ArgChecker.notNull(rangeStr, "rangeStr");
    if (rangeStr.length() < 21) {
      throw new IllegalArgumentException("Invalid range format, too short: " + rangeStr);
    }
    if (rangeStr.startsWith("[") == false) {
      throw new IllegalArgumentException("Invalid range format, must start with [: " + rangeStr);
    }
    if (rangeStr.endsWith("]") == false) {
      throw new IllegalArgumentException("Invalid range format, must end with ]: " + rangeStr);
    }
    String content = rangeStr.substring(1, rangeStr.length() - 1);
    int comma = content.indexOf(',');
    if (comma < 0) {
      throw new IllegalArgumentException("Invalid range format, missing comma: " + rangeStr);
    }
    String startStr = content.substring(0, comma);
    LocalDate start = (startStr.equals("-INFINITY") ? LocalDate.MIN : LocalDate.parse(startStr));
    String endStr = content.substring(comma + 1);
    LocalDate endInclusive = (endStr.equals("+INFINITY") ? LocalDate.MAX : LocalDate.parse(endStr));
    return new LocalDateRange(start, endInclusive);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param startDate  the start date
   * @param endDateInclusive  the end date
   */
  private LocalDateRange(LocalDate startInclusive, LocalDate endInclusive) {
    if (endInclusive.isBefore(startInclusive)) {
      throw new IllegalArgumentException("Start date must be on or after end date");
    }
    this.start = startInclusive;
    this.endInclusive = endInclusive;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the start date, inclusive.
   * <p>
   * This will return {@link LocalDate#MIN} if the range includes all dates
   * up to the
   * 
   * @return the start date
   */
  public LocalDate getStart() {
    return start;
  }

  /**
   * Gets the end date, inclusive.
   * 
   * @return the end date
   */
  public LocalDate getEndInclusive() {
    return endInclusive;
  }

  /**
   * Gets the end date, exclusive.
   * <p>
   * If the end date (inclusive) is {@code MAX_DATE}, then {@code MAX_DATE} is returned.
   * 
   * @return the end date
   */
  public LocalDate getEndExclusive() {
    if (isUnboundedEnd()) {
      return endInclusive;
    }
    return endInclusive.plusDays(1);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the start date is unbounded.
   * 
   * @return true if start is unbounded
   */
  public boolean isUnboundedStart() {
    return start.equals(LocalDate.MIN);
  }

  /**
   * Checks if the end date is unbounded.
   * 
   * @return true if end is unbounded
   */
  public boolean isUnboundedEnd() {
    return endInclusive.equals(LocalDate.MAX);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this range with the start date adjusted.
   * <p>
   * This returns a new instance with the start date altered.
   * Since {@code LocalDate} implements {@code TemporalAdjuster} any
   * local date can simply be passed in.
   * 
   * @param adjuster  the adjuster to use
   * @return a copy of this range with the start date adjusted
   */
  public LocalDateRange withStart(TemporalAdjuster adjuster) {
    ArgChecker.notNull(adjuster, "adjuster");
    return LocalDateRange.closed(start.with(adjuster), endInclusive);
  }

  /**
   * Returns a copy of this range with the end date adjusted.
   * <p>
   * This returns a new instance with the end date altered.
   * Since {@code LocalDate} implements {@code TemporalAdjuster} any
   * local date can simply be passed in.
   * 
   * @param adjuster  the adjuster to use
   * @return a copy of this range with the end date adjusted
   */
  public LocalDateRange withEndInclusive(TemporalAdjuster adjuster) {
    ArgChecker.notNull(adjuster, "adjuster");
    return LocalDateRange.closed(start, endInclusive.with(adjuster));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this range contains the specified date.
   * <p>
   * If this range has an unbounded start then {@code contains(LocalDate#MIN)} returns true.
   * If this range has an unbounded end then {@code contains(LocalDate#MAX)} returns true.
   * 
   * @param date  the date to check for
   * @return true if this range contains the date
   */
  public boolean contains(LocalDate date) {
    ArgChecker.notNull(date, "date");
    // start <= date && date <= end
    return start.compareTo(date) <= 0 && date.compareTo(endInclusive) <= 0;
  }

  /**
   * Checks if this range contains all date in the specified range.
   * <p>
   * This checks that the start date of this range is before or equal the specified
   * start date and the end date of this range is before or equal the specified end date.
   * 
   * @param other  the other range to check for
   * @return true if this range contains all dates in the other range
   */
  public boolean encloses(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    // start <= other.start && other.end <= end
    return start.compareTo(other.start) <= 0 && other.endInclusive.compareTo(endInclusive) <= 0;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this range is entirely before the specified range.
   * 
   * @param other  the other range to check for
   * @return true if every date in this range is before every date in the other range
   */
  public boolean isBefore(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    // end < other.start
    return endInclusive.compareTo(other.start) < 0;
  }

  /**
   * Checks if this range is entirely after the specified range.
   * 
   * @param other  the other range to check for
   * @return true if every date in this range is after every date in the other range
   */
  public boolean isAfter(LocalDateRange other) {
    ArgChecker.notNull(other, "other");
    // start > other.end
    return start.compareTo(other.endInclusive) > 0;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this range equals another.
   * 
   * @param obj  the other object
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof LocalDateRange) {
      LocalDateRange other = (LocalDateRange) obj;
      return start.equals(other.start) && endInclusive.equals(other.endInclusive);
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return start.hashCode() ^ endInclusive.hashCode();
  }

  /**
   * Returns this range as a standard parsable string, such as {@code [2009-12-03,2014-06-30]}.
   * <p>
   * The string will be one of these formats:<br />
   *  {@code [2009-12-03,2014-06-30]}<br />
   *  {@code [2009-12-03,+INFINITY]} - if the end is unbounded<br />
   *  {@code [-INFINITY,2014-06-30]} - if the start is unbounded<br />
   *  {@code [-INFINITY,+INFINITY]} - if the start and end are unbounded<br />
   *
   * @return the standard string
   */
  @Override
  @ToString
  public String toString() {
    StringBuilder buf = new StringBuilder(23);
    if (isUnboundedStart()) {
      buf.append("[-INFINITY,");
    } else {
      buf.append('[').append(start).append(',');
    }
    if (isUnboundedEnd()) {
      buf.append("+INFINITY]");
    } else {
      buf.append(endInclusive).append(']');
    }
    return buf.toString();
  }

}
