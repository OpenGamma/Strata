/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * A swap index.
 * <p>
 * Swap rates for CHF, EUR, GBP, JPY and USD are established by ISDA in co-operation with
 * Reuters (now Thomson Reuters) and Intercapital Brokers (now ICAP plc). 
 * Ref: http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 * <p>
 * The most common implementations are provided in {@link SwapIndices}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface SwapIndex
    extends Index, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static SwapIndex of(String uniqueName) {
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
  public static ExtendedEnum<SwapIndex> extendedEnum() {
    return SwapIndices.ENUM_LOOKUP;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the index is active.
   * <p>
   * Over time some indices become inactive and are no longer produced.
   * If this occurs, this method will return false.
   * 
   * @return true if the index is active, false if inactive
   */
  public abstract boolean isActive();

  /**
   * Gets the fixing time of the index.
   * <p>
   * The fixing time is related to the fixing date and time-zone.
   * 
   * @return the fixing time
   */
  public abstract LocalTime getFixingTime();

  /**
   * Gets the time-zone of the fixing time.
   * <p>
   * The fixing time-zone is related to the fixing date and time.
   * 
   * @return the time-zone of the fixing time
   */
  public abstract ZoneId getFixingZone();

  /**
   * Gets the template for creating Fixed-Ibor swap.
   * 
   * @return the template
   */
  public abstract FixedIborSwapTemplate getTemplate();

  //-------------------------------------------------------------------------
  /**
   * Calculates the fixing date-time from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The result combines the date with the time and zone stored in the index.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * 
   * @param fixingDate  the fixing date
   * @return the fixing date-time
   */
  public default ZonedDateTime calculateFixingDateTime(LocalDate fixingDate) {
    return fixingDate.atTime(getFixingTime()).atZone(getFixingZone());
  }

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
