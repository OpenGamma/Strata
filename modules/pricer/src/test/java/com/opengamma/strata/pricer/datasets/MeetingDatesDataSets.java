/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.datasets;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * List of central bank meeting dates used for testing.
 */
public class MeetingDatesDataSets {
  
  public static final List<LocalDate> FOMC_MEETINGS_2015 = new ArrayList<>();
  static {
    FOMC_MEETINGS_2015.add(LocalDate.of(2015, 1, 28));
    FOMC_MEETINGS_2015.add(LocalDate.of(2015, 3, 18));
    FOMC_MEETINGS_2015.add(LocalDate.of(2015, 4, 29));
    FOMC_MEETINGS_2015.add(LocalDate.of(2015, 6, 17));
    FOMC_MEETINGS_2015.add(LocalDate.of(2015, 7, 29));
    FOMC_MEETINGS_2015.add(LocalDate.of(2015, 9, 17));
    FOMC_MEETINGS_2015.add(LocalDate.of(2015, 10, 28));
    FOMC_MEETINGS_2015.add(LocalDate.of(2015, 12, 16));
  };
  public static final List<LocalDate> FOMC_MEETINGS_2016 = new ArrayList<>();
  static {
    FOMC_MEETINGS_2016.add(LocalDate.of(2016, 1, 27));
    FOMC_MEETINGS_2016.add(LocalDate.of(2016, 3, 16));
    FOMC_MEETINGS_2016.add(LocalDate.of(2016, 4, 27));
    FOMC_MEETINGS_2016.add(LocalDate.of(2016, 6, 15));
    FOMC_MEETINGS_2016.add(LocalDate.of(2016, 7, 27));
    FOMC_MEETINGS_2016.add(LocalDate.of(2016, 9, 21));
    FOMC_MEETINGS_2016.add(LocalDate.of(2016, 11, 2));
    FOMC_MEETINGS_2016.add(LocalDate.of(2016, 12, 14));
  };
  
  public static final List<LocalDate> FOMC_MEETINGS = new ArrayList<>();
  static {
    FOMC_MEETINGS.addAll(FOMC_MEETINGS_2015);
    FOMC_MEETINGS.addAll(FOMC_MEETINGS_2016);
  }

}
