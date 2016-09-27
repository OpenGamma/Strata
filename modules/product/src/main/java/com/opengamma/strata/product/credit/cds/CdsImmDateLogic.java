package com.opengamma.strata.product.credit.cds;

import java.time.LocalDate;

public class CdsImmDateLogic {

  private static final int IMM_DAY = 20;
  private static final int[] IMM_MONTHS = new int[] {3, 6, 9, 12};
  private static final int[] INDEX_ROLL_MONTHS = new int[] {3, 9};

  /**
   * Index roll dates are 20th March and September.
   * 
   * @param date  the date
   * @return true is date is an IMM date
   */
  public static boolean isIndexRollDate(LocalDate date) {
    if (date.getDayOfMonth() != IMM_DAY) {
      return false;
    }
    int month = date.getMonthValue();
    return month == INDEX_ROLL_MONTHS[0] || month == INDEX_ROLL_MONTHS[1];
  }

  /**
   * IMM dates are 20th March, June, September and December.
   * This returns the previous IMM date from the given date - if the date
   * is an IMM date the previous IMM date (i.e. 3 months before) is returned.
   * 
   * @param date  a given date
   * @return the next IMM date
   */
  public static LocalDate getPrevIMMDate(LocalDate date) {

    int day = date.getDayOfMonth();
    int month = date.getMonthValue();
    int year = date.getYear();
    if (month % 3 == 0) { //in an IMM month
      if (day > IMM_DAY) {
        return LocalDate.of(year, month, IMM_DAY);
      } else {
        if (month != 3) {
          return LocalDate.of(year, month - 3, IMM_DAY);
        } else {
          return LocalDate.of(year - 1, IMM_MONTHS[3], IMM_DAY);
        }
      }
    } else {
      int i = month / 3;
      if (i == 0) {
        return LocalDate.of(year - 1, IMM_MONTHS[3], IMM_DAY);
      } else {
        return LocalDate.of(year, IMM_MONTHS[i - 1], IMM_DAY);
      }
    }
  }

  /**
   * Index roll dates  are 20th March and September. This returns the next roll date from the
   * given date - if the date is a roll date the next roll date (i.e. 6 months on) is returned.
   * 
   * @param date  a given date
   * @return the next Index roll date
   */
  public static LocalDate getNextIndexRollDate(LocalDate date) {

    int day = date.getDayOfMonth();
    int month = date.getMonthValue();
    int year = date.getYear();
    if (isIndexRollDate(date)) { //on an index roll 
      if (month == INDEX_ROLL_MONTHS[0]) {
        return LocalDate.of(year, INDEX_ROLL_MONTHS[1], IMM_DAY);
      } else {
        return LocalDate.of(year + 1, INDEX_ROLL_MONTHS[0], IMM_DAY);
      }
    } else {
      if (month < INDEX_ROLL_MONTHS[0]) {
        return LocalDate.of(year, INDEX_ROLL_MONTHS[0], IMM_DAY);
      } else if (month == INDEX_ROLL_MONTHS[0]) {
        if (day < IMM_DAY) {
          return LocalDate.of(year, month, IMM_DAY);
        } else {
          return LocalDate.of(year, INDEX_ROLL_MONTHS[1], IMM_DAY);
        }
      } else if (month < INDEX_ROLL_MONTHS[1]) {
        return LocalDate.of(year, INDEX_ROLL_MONTHS[1], IMM_DAY);
      } else if (month == INDEX_ROLL_MONTHS[1]) {
        if (day < IMM_DAY) {
          return LocalDate.of(year, month, IMM_DAY);
        } else {
          return LocalDate.of(year + 1, INDEX_ROLL_MONTHS[0], IMM_DAY);
        }
      } else {
        return LocalDate.of(year + 1, INDEX_ROLL_MONTHS[0], IMM_DAY);
      }
    }
  }

}
