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
      int month = base.getMonthValue();
      int offset = (month % 3 == 0 ? 0 : 3 - month % 3) + (sequenceNumber - 1) * 3;
      return base.plusMonths(offset).with(THIRD_WEDNESDAY);
    }

    @Override
    public LocalDate dateMatching(YearMonth yearMonth) {
      return nextOrSame(yearMonth.atDay(1));
    }
  },

  // IMM in Mar/Jun/Sep/Dec plus first 4 non quarterly serial months
  QUARTERLY_IMM_4_SERIAL("Quarterly-IMM-4-Serial") {
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
      int month = base.getMonthValue();
      int offset = (month % 3 == 0 ? 0 : 3 - month % 3) + (effectiveQuarterlySequenceNumber - 1) * 3;
      return base.plusMonths(offset).with(THIRD_WEDNESDAY);
    }

    @Override
    public LocalDate dateMatching(YearMonth yearMonth) {
      return nextOrSame(yearMonth.atDay(1));
    }
  },

  // IMM in Mar/Jun/Sep/Dec plus first 2 non quarterly serial months
  QUARTERLY_IMM_2_SERIAL("Quarterly-IMM-2-Serial") {
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
      int month = base.getMonthValue();
      int offset = (month % 3 == 0 ? 0 : 3 - month % 3) + (effectiveQuarterlySequenceNumber - 1) * 3;
      return base.plusMonths(offset).with(THIRD_WEDNESDAY);
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
      int month = base.getMonthValue();
      int offset = (month % 3 == 0 ? 0 : 3 - month % 3) + (sequenceNumber - 1) * 3;
      return base.plusMonths(offset).withDayOfMonth(10);
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

  // Third Wednesday
  private static final TemporalAdjuster THIRD_WEDNESDAY = TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY);

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
