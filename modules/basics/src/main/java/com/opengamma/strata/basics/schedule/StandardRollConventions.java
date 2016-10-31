/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Standard roll convention implementations.
 * <p>
 * See {@link RollConventions} for the description of each.
 */
enum StandardRollConventions implements RollConvention {

  // no adjustment
  NONE("None") {
    @Override
    public LocalDate adjust(LocalDate date) {
      return ArgChecker.notNull(date, "date");
    }
  },

  // last day of month
  EOM("EOM") {
    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.withDayOfMonth(date.lengthOfMonth());
    }

    @Override
    public int getDayOfMonth() {
      // EOM is equivalent to 31 in FpML in most cases
      // because roll conventions 30 and 29 also have to adjust to end of February
      return 31;
    }
  },

  // 3rd Wednesday
  IMM("IMM") {
    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY));
    }
  },

  // day before 2nd Friday
  IMMAUD("IMMAUD") {
    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.FRIDAY)).minusDays(1);
    }
  },

  // Wednesday on or after 9th
  IMMNZD("IMMNZD") {
    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.withDayOfMonth(9).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
    }
  },

  // 2nd Friday
  SFE("SFE") {
    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.FRIDAY));
    }
  };

  // name
  private final String name;

  // create
  private StandardRollConventions(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

}
