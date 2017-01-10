/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Standard roll convention implementations.
 * <p>
 * See {@link RollConventions} for the description of each.
 */
final class DayRollConventions implements NamedLookup<RollConvention> {

  // lookup of conventions
  static final ImmutableMap<String, RollConvention> MAP;
  static {
    ImmutableMap.Builder<String, RollConvention> mapBuilder = ImmutableMap.builder();
    for (RollConvention roll : Dom.CONVENTIONS) {
      mapBuilder.put(roll.getName(), roll);
      mapBuilder.put(roll.getName().toUpperCase(Locale.ENGLISH), roll);
    }
    for (RollConvention roll : Dow.CONVENTIONS) {
      mapBuilder.put(roll.getName(), roll);
      mapBuilder.put(roll.getName().toUpperCase(Locale.ENGLISH), roll);
    }
    MAP = mapBuilder.build();
  }

  /**
   * Restricted constructor.
   */
  private DayRollConventions() {
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableMap<String, RollConvention> lookupAll() {
    return MAP;
  }

  //-------------------------------------------------------------------------
  /**
   * Implementation of the day-of-month roll convention.
   */
  static final class Dom implements RollConvention, Serializable {
    // singleton, so no equals/hashCode

    // Serialization version
    private static final long serialVersionUID = 1L;
    // cache of conventions
    private static final RollConvention[] CONVENTIONS = new RollConvention[30];
    static {
      for (int i = 0; i < 30; i++) {
        CONVENTIONS[i] = new Dom(i + 1);
      }
    }

    // day-of-month
    private final int day;
    // unique name
    private final String name;

    // obtains instance
    static RollConvention of(int day) {
      if (day == 31) {
        return RollConventions.EOM;
      } else if (day < 1 || day > 30) {
        throw new IllegalArgumentException("Invalid day-of-month: " + day);
      }
      return CONVENTIONS[day - 1];
    }

    // create
    private Dom(int day) {
      this.day = day;
      this.name = "Day" + day;
    }

    private Object readResolve() {
      return Dom.of(day);
    }

    @Override
    public int getDayOfMonth() {
      return day;
    }

    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      if (day >= 29 && date.getMonthValue() == 2) {
        return date.withDayOfMonth(date.lengthOfMonth());
      }
      return date.withDayOfMonth(day);
    }

    @Override
    public boolean matches(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.getDayOfMonth() == day ||
          (date.getMonthValue() == 2 && day >= date.lengthOfMonth() && date.getDayOfMonth() == date.lengthOfMonth());
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

  //-------------------------------------------------------------------------
  /**
   * Implementation of the day-of-week roll convention.
   */
  static final class Dow implements RollConvention, Serializable {
    // singleton, so no equals/hashCode

    // Serialization version
    private static final long serialVersionUID = 1L;
    // convention names
    private static final String NAMES = "DayMonDayTueDayWedDayThuDayFriDaySatDaySun";
    // cache of conventions
    private static final RollConvention[] CONVENTIONS = new RollConvention[7];
    static {
      for (int i = 0; i < 7; i++) {
        DayOfWeek dow = DayOfWeek.of(i + 1);
        String name = NAMES.substring(i * 6, (i + 1) * 6);
        CONVENTIONS[i] = new Dow(dow, name);
      }
    }

    // day-of-week
    private final DayOfWeek day;
    // unique name
    private final String name;

    // obtains instance
    static RollConvention of(DayOfWeek dayOfWeek) {
      ArgChecker.notNull(dayOfWeek, "dayOfWeek");
      return CONVENTIONS[dayOfWeek.getValue() - 1];
    }

    private Object readResolve() {
      return Dow.of(day);
    }

    // create
    private Dow(DayOfWeek dayOfWeek, String name) {
      this.day = dayOfWeek;
      this.name = name;
    }

    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.with(TemporalAdjusters.nextOrSame(day));
    }

    @Override
    public boolean matches(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.getDayOfWeek() == day;
    }

    @Override
    public LocalDate next(LocalDate date, Frequency periodicFrequency) {
      ArgChecker.notNull(date, "date");
      ArgChecker.notNull(periodicFrequency, "periodicFrequency");
      LocalDate calculated = date.plus(periodicFrequency);
      return calculated.with(TemporalAdjusters.nextOrSame(day));
    }

    @Override
    public LocalDate previous(LocalDate date, Frequency periodicFrequency) {
      ArgChecker.notNull(date, "date");
      ArgChecker.notNull(periodicFrequency, "periodicFrequency");
      LocalDate calculated = date.minus(periodicFrequency);
      return calculated.with(TemporalAdjusters.previousOrSame(day));
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

}
