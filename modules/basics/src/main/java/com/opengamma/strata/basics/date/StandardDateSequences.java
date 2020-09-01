/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Standard date sequence implementations.
 * <p>
 * See {@link DateSequences} for the description of each.
 */
enum StandardDateSequences implements DateSequence {

  // IMM in Mar/Jun/Sep/Dec
  QUARTERLY_IMM("Quarterly-IMM") {
    @Override
    public LocalDate next(LocalDate date) {
      return nth(date, 1);
    }

    @Override
    public LocalDate nextOrSame(LocalDate date) {
      return nthOrSame(date, 1);
    }

    @Override
    public LocalDate nth(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate base = date.with(THIRD_WEDNESDAY);
      if (!base.isAfter(date)) {
        base = base.plusMonths(1);
      }
      return shift(base, sequenceNumber);
    }

    @Override
    public LocalDate nthOrSame(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate base = date.with(THIRD_WEDNESDAY);
      if (base.isBefore(date)) {
        base = base.plusMonths(1);
      }
      return shift(base, sequenceNumber);
    }

    private LocalDate shift(LocalDate base, int sequenceNumber) {
      int monthsToAdd = monthsToAdd(base.getMonthValue(), sequenceNumber);
      return base.plusMonths(monthsToAdd).with(THIRD_WEDNESDAY);
    }

    @Override
    public LocalDate dateMatching(YearMonth yearMonth) {
      return nextOrSame(yearMonth.atDay(1));
    }
  },

  // IMM in Mar/Jun/Sep/Dec plus first 4 non quarterly serial months making 6 serial months in total
  QUARTERLY_IMM_6_SERIAL("Quarterly-IMM-6-Serial") {
    @Override
    public DateSequence baseSequence() {
      return QUARTERLY_IMM;
    }

    @Override
    public LocalDate next(LocalDate date) {
      return nth(date, 1);
    }

    @Override
    public LocalDate nextOrSame(LocalDate date) {
      return nthOrSame(date, 1);
    }

    @Override
    public LocalDate nth(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate baseImm = date.with(THIRD_WEDNESDAY);
      if (!baseImm.isAfter(date)) {
        baseImm = baseImm.plusMonths(1);
      }
      return shift(baseImm, sequenceNumber);
    }

    @Override
    public LocalDate nthOrSame(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate baseImm = date.with(THIRD_WEDNESDAY);
      if (baseImm.isBefore(date)) {
        baseImm = baseImm.plusMonths(1);
      }
      return shift(baseImm, sequenceNumber);
    }

    private LocalDate shift(LocalDate base, int sequenceNumber) {
      // first 4 serial can be expanded to first 6 serial by subsuming first two quarterly
      if (sequenceNumber <= 6) {
        return base.plusMonths(sequenceNumber - 1).with(THIRD_WEDNESDAY);
      }
      int effectiveQuarterlySequenceNumber = sequenceNumber - 4;
      int monthsToAdd = monthsToAdd(base.getMonthValue(), effectiveQuarterlySequenceNumber);
      return base.plusMonths(monthsToAdd).with(THIRD_WEDNESDAY);
    }

    @Override
    public LocalDate dateMatching(YearMonth yearMonth) {
      return nextOrSame(yearMonth.atDay(1));
    }
  },

  // IMM in Mar/Jun/Sep/Dec plus first 2 non quarterly serial months making 3 serial months in total
  QUARTERLY_IMM_3_SERIAL("Quarterly-IMM-3-Serial") {
    @Override
    public DateSequence baseSequence() {
      return QUARTERLY_IMM;
    }

    @Override
    public LocalDate next(LocalDate date) {
      return nth(date, 1);
    }

    @Override
    public LocalDate nextOrSame(LocalDate date) {
      return nthOrSame(date, 1);
    }

    @Override
    public LocalDate nth(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate baseImm = date.with(THIRD_WEDNESDAY);
      if (!baseImm.isAfter(date)) {
        baseImm = baseImm.plusMonths(1);
      }
      return shift(baseImm, sequenceNumber);
    }

    @Override
    public LocalDate nthOrSame(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate baseImm = date.with(THIRD_WEDNESDAY);
      if (baseImm.isBefore(date)) {
        baseImm = baseImm.plusMonths(1);
      }
      return shift(baseImm, sequenceNumber);
    }

    private LocalDate shift(LocalDate base, int sequenceNumber) {
      // first 2 serial can be expanded to first 3 serial by subsuming first quarterly
      if (sequenceNumber <= 3) {
        return base.plusMonths(sequenceNumber - 1).with(THIRD_WEDNESDAY);
      }
      int effectiveQuarterlySequenceNumber = sequenceNumber - 2;
      int monthsToAdd = monthsToAdd(base.getMonthValue(), effectiveQuarterlySequenceNumber);
      return base.plusMonths(monthsToAdd).with(THIRD_WEDNESDAY);
    }

    @Override
    public LocalDate dateMatching(YearMonth yearMonth) {
      return nextOrSame(yearMonth.atDay(1));
    }
  },

  // Third Wednesday
  MONTHLY_IMM("Monthly-IMM") {
    @Override
    public LocalDate next(LocalDate date) {
      return nth(date, 1);
    }

    @Override
    public LocalDate nextOrSame(LocalDate date) {
      return nthOrSame(date, 1);
    }

    @Override
    public LocalDate nth(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate base = date.with(THIRD_WEDNESDAY);
      if (!base.isAfter(date)) {
        return base.plusMonths(sequenceNumber).with(THIRD_WEDNESDAY);
      }
      return base.plusMonths(sequenceNumber - 1).with(THIRD_WEDNESDAY);
    }

    @Override
    public LocalDate nthOrSame(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate base = date.with(THIRD_WEDNESDAY);
      if (base.isBefore(date)) {
        return base.plusMonths(sequenceNumber).with(THIRD_WEDNESDAY);
      }
      return base.plusMonths(sequenceNumber - 1).with(THIRD_WEDNESDAY);
    }

    @Override
    public LocalDate dateMatching(YearMonth yearMonth) {
      return yearMonth.atDay(1).with(THIRD_WEDNESDAY);
    }
  },

  // 10th in Mar/Jun/Sep/Dec
  QUARTERLY_10TH("Quarterly-10th") {
    @Override
    public LocalDate next(LocalDate date) {
      return nth(date, 1);
    }

    @Override
    public LocalDate nextOrSame(LocalDate date) {
      return nthOrSame(date, 1);
    }

    @Override
    public LocalDate nth(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate base = date.withDayOfMonth(10);
      if (!base.isAfter(date)) {
        base = base.plusMonths(1);
      }
      return shift(base, sequenceNumber);
    }

    @Override
    public LocalDate nthOrSame(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate base = date.withDayOfMonth(10);
      if (base.isBefore(date)) {
        base = base.plusMonths(1);
      }
      return shift(base, sequenceNumber);
    }

    private LocalDate shift(LocalDate base, int sequenceNumber) {
      int monthsToAdd = monthsToAdd(base.getMonthValue(), sequenceNumber);
      return base.plusMonths(monthsToAdd).withDayOfMonth(10);
    }

    @Override
    public LocalDate dateMatching(YearMonth yearMonth) {
      return nextOrSame(yearMonth.atDay(1));
    }
  },

  // 1st of the month
  MONTHLY_1ST("Monthly-1st") {
    @Override
    public LocalDate next(LocalDate date) {
      return nth(date, 1);
    }

    @Override
    public LocalDate nextOrSame(LocalDate date) {
      return nthOrSame(date, 1);
    }

    @Override
    public LocalDate nth(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate base = date.withDayOfMonth(1);
      if (!base.isAfter(date)) {
        base = base.plusMonths(1);
      }
      return base.plusMonths(sequenceNumber - 1);
    }

    @Override
    public LocalDate nthOrSame(LocalDate date, int sequenceNumber) {
      ArgChecker.notNegativeOrZero(sequenceNumber, "sequenceNumber");
      LocalDate base = date.withDayOfMonth(1);
      if (base.isBefore(date)) {
        base = base.plusMonths(1);
      }
      return base.plusMonths(sequenceNumber - 1);
    }

    @Override
    public LocalDate dateMatching(YearMonth yearMonth) {
      return nextOrSame(yearMonth.atDay(1));
    }
  };

  //-------------------------------------------------------------------------
  // Third Wednesday
  private static final TemporalAdjuster THIRD_WEDNESDAY = TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY);

  // calculates the number of months to add to the base date for a quarterly sequence (Mar/Jun/Sep/Dec)
  private static int monthsToAdd(int baseMonth, int sequenceNumber) {
    int monthInQuarter = (baseMonth + 2) % 3;  // Jan/Apr/Jul/Oct is 0, Feb/May/Aug/Nov is 1, Mar/Jun/Sep/Dec is 2
    int monthsUntilNextQuarter = 2 - monthInQuarter;
    return monthsUntilNextQuarter + (sequenceNumber - 1) * 3;
  }

  //-------------------------------------------------------------------------
  // name
  private final String name;

  // create
  private StandardDateSequences(String name) {
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
