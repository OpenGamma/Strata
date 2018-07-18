/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Provider that loads common global holiday calendars from binary form on the classpath.
 */
final class GlobalHolidayCalendarLookup implements NamedLookup<HolidayCalendar> {

  /**
   * The singleton instance of the lookup.
   */
  public static final GlobalHolidayCalendarLookup INSTANCE = new GlobalHolidayCalendarLookup();

  // lookup of conventions
  static final ImmutableMap<String, HolidayCalendar> MAP;
  static {
    ImmutableMap.Builder<String, HolidayCalendar> builder = ImmutableMap.builder();
    ResourceLocator locator =
        ResourceLocator.ofClasspath("com/opengamma/strata/basics/date/GlobalHolidayCalendars.bin");
    try (InputStream fis = locator.getByteSource().openStream()) {
      try (DataInputStream in = new DataInputStream(fis)) {
        if (in.readByte() != 'H' || in.readByte() != 'C' || in.readByte() != 'a' || in.readByte() != 'l') {
          System.err.println("ERROR: Corrupt holiday calendar data file");
        } else {
          short calSize = in.readShort();
          for (int i = 0; i < calSize; i++) {
            HolidayCalendar cal = ImmutableHolidayCalendar.readExternal(in);
            builder.put(cal.getId().getName(), cal);
          }
        }
      }
    } catch (IOException ex) {
      System.err.println("ERROR: Unable to parse holiday calendar data file: " + ex.getMessage());
      ex.printStackTrace();
    }
    MAP = builder.build();
  }

  /**
   * Restricted constructor.
   */
  private GlobalHolidayCalendarLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableMap<String, HolidayCalendar> lookupAll() {
    return MAP;
  }

}
