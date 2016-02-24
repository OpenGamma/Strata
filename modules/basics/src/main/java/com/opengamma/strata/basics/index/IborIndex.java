/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * An inter-bank lending rate index, such as Libor or Euribor.
 * <p>
 * An index represented by this class relates to inter-bank lending for periods
 * from one day to one year. They are typically calculated and published as the
 * trimmed arithmetic mean of estimated rates contributed by banks.
 * <p>
 * The index is defined by three dates.
 * The fixing date is the date on which the index is to be observed.
 * The effective date is the date on which the implied deposit starts.
 * The maturity date is the date on which the implied deposit ends.
 * <p>
 * The most common implementations are provided in {@link IborIndices}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface IborIndex
    extends RateIndex, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static IborIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the index to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<IborIndex> extendedEnum() {
    return IborIndices.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the day count convention of the index.
   * 
   * @return the day count convention
   */
  public abstract DayCount getDayCount();

  /**
   * Gets the fixing calendar of the index.
   * <p>
   * The rate will be fixed on each business day in this calendar.
   * 
   * @return the currency pair of the index
   */
  public abstract HolidayCalendar getFixingCalendar();

  /**
   * Gets the tenor of the index.
   * 
   * @return the tenor
   */
  public abstract Tenor getTenor();

  //-------------------------------------------------------------------------
  /**
   * Converts the fixing date-time from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The fixing date-time is the specific date and time of the observation.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * 
   * @param fixingDate  the fixing date
   * @return  the fixing date-time
   */
  public abstract ZonedDateTime calculateFixingDateTime(LocalDate fixingDate);

  /**
   * Calculates the effective date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the effective date
   */
  public abstract LocalDate calculateEffectiveFromFixing(LocalDate fixingDate);

  /**
   * Calculates the maturity date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the maturity date
   */
  public abstract LocalDate calculateMaturityFromFixing(LocalDate fixingDate);

  /**
   * Calculates the fixing date from the effective date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the fixing date
   */
  public abstract LocalDate calculateFixingFromEffective(LocalDate effectiveDate);

  /**
   * Calculates the maturity date from the effective date.
   * <p>
   * The effective date is the date on which the implied deposit starts.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the maturity date
   */
  public abstract LocalDate calculateMaturityFromEffective(LocalDate effectiveDate);

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustment applied to the effective date to obtain the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * In most cases, the fixing date is 0 or 2 days before the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   * 
   * @return the fixing date offset
   */
  public abstract DaysAdjustment getFixingDateOffset();

  /**
   * Gets the adjustment applied to the fixing date to obtain the effective date.
   * <p>
   * The effective date is the start date of the indexed deposit.
   * In most cases, the effective date is 0 or 2 days after the fixing date.
   * This data structure allows the complex rules of some indices to be represented.
   * 
   * @return the effective date offset
   */
  public abstract DaysAdjustment getEffectiveDateOffset();

  /**
   * Gets the adjustment applied to the effective date to obtain the maturity date.
   * <p>
   * The maturity date is the end date of the indexed deposit and is relative to the effective date.
   * This data structure allows the complex rules of some indices to be represented.
   * 
   * @return the tenor date offset
   */
  public abstract TenorAdjustment getMaturityDateOffset();

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
