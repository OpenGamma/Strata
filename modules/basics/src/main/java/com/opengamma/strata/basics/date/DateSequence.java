/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.LocalDateUtils.plusDays;

import java.time.LocalDate;
import java.time.YearMonth;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * A series of dates identified by name.
 * <p>
 * This interface encapsulates a sequence of dates as used in standard financial instruments.
 * The most common are the quarterly IMM dates, which are on the third Wednesday of March,
 * June, September and December.
 * <p>
 * The most common implementations are provided in {@link DateSequences}.
 * <p>
 * Note that the dates produced by the sequence may not be business days.
 * The application of a holiday calendar is typically the responsibility of the caller.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface DateSequence
    extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the date sequence
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static DateSequence of(String uniqueName) {
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the sequence to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<DateSequence> extendedEnum() {
    return DateSequences.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the next date in the sequence, always returning a date later than the input date.
   * <p>
   * Given an input date, this method returns the next date after it from the sequence.
   * 
   * @param date  the input date
   * @return the next sequence date after the input date
   * @throws IllegalArgumentException if there are no more sequence dates
   */
  public default LocalDate next(LocalDate date) {
    LocalDate next = plusDays(date, 1);
    return nextOrSame(next);
  }

  /**
   * Finds the next date in the sequence, returning the input date if it is a date in the sequence.
   * <p>
   * Given an input date, this method returns a date from the sequence.
   * If the input date is in the sequence, it is returned.
   * Otherwise, the next date in the sequence after the input date is returned.
   * 
   * @param date  the input date
   * @return the input date if it is a sequence date, otherwise the next sequence date
   * @throws IllegalArgumentException if there are no more sequence dates
   */
  public abstract LocalDate nextOrSame(LocalDate date);

  //-------------------------------------------------------------------------
  /**
   * Finds the nth date in the sequence after the input date,
   * always returning a date later than the input date.
   * <p>
   * Given an input date, this method returns a date from the sequence.
   * If the sequence number is 1, then the first date in the sequence after the input date is returned.
   * <p>
   * If the sequence number is 2 or larger, then the date referred to by sequence number 1
   * is calculated, and the nth matching sequence date after that date returned.
   * 
   * @param date  the input date
   * @param sequenceNumber  the 1-based index of the date to find
   * @return the nth sequence date after the input date
   * @throws IllegalArgumentException if the sequence number is zero or negative or if there are no more sequence dates
   */
  public default LocalDate nth(LocalDate date, int sequenceNumber) {
    ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
    if (sequenceNumber == 1) {
      return next(date);
    } else {
      return nth(next(date), sequenceNumber - 1);
    }
  }

  /**
   * Finds the nth date in the sequence on or after the input date,
   * returning the input date if it is a date in the sequence.
   * <p>
   * Given an input date, this method returns a date from the sequence.
   * If the sequence number is 1, then either the input date or the first date
   * in the sequence after the input date is returned.
   * <p>
   * If the sequence number is 2 or larger, then the date referred to by sequence number 1
   * is calculated, and the nth matching sequence date after that date returned.
   * 
   * @param date  the input date
   * @param sequenceNumber  the 1-based index of the date to find
   * @return the nth sequence date on or after the input date
   * @throws IllegalArgumentException if the sequence number is zero or negative or if there are no more sequence dates
   */
  public default LocalDate nthOrSame(LocalDate date, int sequenceNumber) {
    ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
    if (sequenceNumber == 1) {
      return nextOrSame(date);
    } else {
      return nth(nextOrSame(date), sequenceNumber - 1);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the date in the sequence that corresponds to the specified year-month.
   * <p>
   * Given an input month, this method returns the date from the sequence that is associated with the year-month.
   * In most cases, the returned date will be in the same month as the input month,
   * but this is not guaranteed.
   * 
   * @param yearMonth  the input year-month
   * @return the next sequence date after the input date
   * @throws IllegalArgumentException if there are no more sequence dates
   */
  public abstract LocalDate dateMatching(YearMonth yearMonth);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this sequence.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
