/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.collect.ArgChecker;

/**
 * The type of a schedule period.
 * <p>
 * A period may be an initial stub, regular period or a final stub.
 */
public enum SchedulePeriodType {

  /**
   * The first period in the schedule.
   * It may be a stub period.
   */
  INITIAL,
  /**
   * A normal period, neither initial nor final.
   */
  NORMAL,
  /**
   * The last period in the schedule.
   * It may be a stub period.
   */
  FINAL,
  /**
   * The term period, which lasts for the whole of the schedule.
   */
  TERM;

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from an index and size.
   * 
   * @param index  the loop index, from zero to size - 1
   * @param size  the loop size
   * @return the type
   */
  static SchedulePeriodType of(int index, int size) {
    if (size <= 2) {
      return TERM;
    }
    if (index == 0) {
      return INITIAL;
    }
    if (index >= size - 2) {
      return FINAL;
    }
    return NORMAL;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static SchedulePeriodType of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
