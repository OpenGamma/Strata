/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;

/**
 * Test {@code GlobalHolidayCalendars}.
 */
public class GlobalHolidayCalendarsTest {

  public static Object[][] data_easter() {
    return new Object[][] {
        {15, 4, 1900},
        {15, 4, 1900},
        {7, 4, 1901},
        {30, 3, 1902},
        {12, 4, 1903},
        {3, 4, 1904},
        {23, 4, 1905},
        {15, 4, 1906},
        {31, 3, 1907},
        {19, 4, 1908},
        {11, 4, 1909},
        {27, 3, 1910},
        {16, 4, 1911},
        {7, 4, 1912},
        {23, 3, 1913},
        {12, 4, 1914},
        {4, 4, 1915},
        {23, 4, 1916},
        {8, 4, 1917},
        {31, 3, 1918},
        {20, 4, 1919},
        {4, 4, 1920},
        {27, 3, 1921},
        {16, 4, 1922},
        {1, 4, 1923},
        {20, 4, 1924},
        {12, 4, 1925},
        {4, 4, 1926},
        {17, 4, 1927},
        {8, 4, 1928},
        {31, 3, 1929},
        {20, 4, 1930},
        {5, 4, 1931},
        {27, 3, 1932},
        {16, 4, 1933},
        {1, 4, 1934},
        {21, 4, 1935},
        {12, 4, 1936},
        {28, 3, 1937},
        {17, 4, 1938},
        {9, 4, 1939},
        {24, 3, 1940},
        {13, 4, 1941},
        {5, 4, 1942},
        {25, 4, 1943},
        {9, 4, 1944},
        {1, 4, 1945},
        {21, 4, 1946},
        {6, 4, 1947},
        {28, 3, 1948},
        {17, 4, 1949},
        {9, 4, 1950},
        {25, 3, 1951},
        {13, 4, 1952},
        {5, 4, 1953},
        {18, 4, 1954},
        {10, 4, 1955},
        {1, 4, 1956},
        {21, 4, 1957},
        {6, 4, 1958},
        {29, 3, 1959},
        {17, 4, 1960},
        {2, 4, 1961},
        {22, 4, 1962},
        {14, 4, 1963},
        {29, 3, 1964},
        {18, 4, 1965},
        {10, 4, 1966},
        {26, 3, 1967},
        {14, 4, 1968},
        {6, 4, 1969},
        {29, 3, 1970},
        {11, 4, 1971},
        {2, 4, 1972},
        {22, 4, 1973},
        {14, 4, 1974},
        {30, 3, 1975},
        {18, 4, 1976},
        {10, 4, 1977},
        {26, 3, 1978},
        {15, 4, 1979},
        {6, 4, 1980},
        {19, 4, 1981},
        {11, 4, 1982},
        {3, 4, 1983},
        {22, 4, 1984},
        {7, 4, 1985},
        {30, 3, 1986},
        {19, 4, 1987},
        {3, 4, 1988},
        {26, 3, 1989},
        {15, 4, 1990},
        {31, 3, 1991},
        {19, 4, 1992},
        {11, 4, 1993},
        {3, 4, 1994},
        {16, 4, 1995},
        {7, 4, 1996},
        {30, 3, 1997},
        {12, 4, 1998},
        {4, 4, 1999},
        {23, 4, 2000},
        {15, 4, 2001},
        {31, 3, 2002},
        {20, 4, 2003},
        {11, 4, 2004},
        {27, 3, 2005},
        {16, 4, 2006},
        {8, 4, 2007},
        {23, 3, 2008},
        {12, 4, 2009},
        {4, 4, 2010},
        {24, 4, 2011},
        {8, 4, 2012},
        {31, 3, 2013},
        {20, 4, 2014},
        {5, 4, 2015},
        {27, 3, 2016},
        {16, 4, 2017},
        {1, 4, 2018},
        {21, 4, 2019},
        {12, 4, 2020},
        {4, 4, 2021},
        {17, 4, 2022},
        {9, 4, 2023},
        {31, 3, 2024},
        {20, 4, 2025},
        {5, 4, 2026},
        {28, 3, 2027},
        {16, 4, 2028},
        {1, 4, 2029},
        {21, 4, 2030},
        {13, 4, 2031},
        {28, 3, 2032},
        {17, 4, 2033},
        {9, 4, 2034},
        {25, 3, 2035},
        {13, 4, 2036},
        {5, 4, 2037},
        {25, 4, 2038},
        {10, 4, 2039},
        {1, 4, 2040},
        {21, 4, 2041},
        {6, 4, 2042},
        {29, 3, 2043},
        {17, 4, 2044},
        {9, 4, 2045},
        {25, 3, 2046},
        {14, 4, 2047},
        {5, 4, 2048},
        {18, 4, 2049},
        {10, 4, 2050},
        {2, 4, 2051},
        {21, 4, 2052},
        {6, 4, 2053},
        {29, 3, 2054},
        {18, 4, 2055},
        {2, 4, 2056},
        {22, 4, 2057},
        {14, 4, 2058},
        {30, 3, 2059},
        {18, 4, 2060},
        {10, 4, 2061},
        {26, 3, 2062},
        {15, 4, 2063},
        {6, 4, 2064},
        {29, 3, 2065},
        {11, 4, 2066},
        {3, 4, 2067},
        {22, 4, 2068},
        {14, 4, 2069},
        {30, 3, 2070},
        {19, 4, 2071},
        {10, 4, 2072},
        {26, 3, 2073},
        {15, 4, 2074},
        {7, 4, 2075},
        {19, 4, 2076},
        {11, 4, 2077},
        {3, 4, 2078},
        {23, 4, 2079},
        {7, 4, 2080},
        {30, 3, 2081},
        {19, 4, 2082},
        {4, 4, 2083},
        {26, 3, 2084},
        {15, 4, 2085},
        {31, 3, 2086},
        {20, 4, 2087},
        {11, 4, 2088},
        {3, 4, 2089},
        {16, 4, 2090},
        {8, 4, 2091},
        {30, 3, 2092},
        {12, 4, 2093},
        {4, 4, 2094},
        {24, 4, 2095},
        {15, 4, 2096},
        {31, 3, 2097},
        {20, 4, 2098},
        {12, 4, 2099},
    };
  }

  @ParameterizedTest
  @MethodSource("data_easter")
  public void test_easter(int day, int month, int year) {
    assertThat(GlobalHolidayCalendars.easter(year)).isEqualTo(LocalDate.of(year, month, day));
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar GBLO = GlobalHolidayCalendars.generateLondon();

  public static Object[][] data_gblo() {
    return new Object[][] {
        // Whitsun, Last Mon Aug - http://hansard.millbanksystems.com/commons/1964/mar/04/staggered-holidays
        {1965, mds(1965, md(4, 16), md(4, 19), md(6, 7), md(8, 30), md(12, 27), md(12, 28))},
        // Whitsun May - http://hansard.millbanksystems.com/commons/1964/mar/04/staggered-holidays
        // 29th Aug - http://hansard.millbanksystems.com/written_answers/1965/nov/25/august-bank-holiday
        {1966, mds(1966, md(4, 8), md(4, 11), md(5, 30), md(8, 29), md(12, 26), md(12, 27))},
        // 29th May, 28th Aug - http://hansard.millbanksystems.com/written_answers/1965/jun/03/bank-holidays-1967-and-1968
        {1967, mds(1967, md(3, 24), md(3, 27), md(5, 29), md(8, 28), md(12, 25), md(12, 26))},
        // 3rd Jun, 2nd Sep - http://hansard.millbanksystems.com/written_answers/1965/jun/03/bank-holidays-1967-and-1968
        {1968, mds(1968, md(4, 12), md(4, 15), md(6, 3), md(9, 2), md(12, 25), md(12, 26))},
        // 26th May, 1st Sep - http://hansard.millbanksystems.com/written_answers/1967/mar/21/bank-holidays-1969-dates
        {1969, mds(1969, md(4, 4), md(4, 7), md(5, 26), md(9, 1), md(12, 25), md(12, 26))},
        // 25th May, 31st Aug - http://hansard.millbanksystems.com/written_answers/1967/jul/28/bank-holidays
        {1970, mds(1970, md(3, 27), md(3, 30), md(5, 25), md(8, 31), md(12, 25), md(12, 28))},
        // applying rules
        {1971, mds(1971, md(4, 9), md(4, 12), md(5, 31), md(8, 30), md(12, 27), md(12, 28))},
        {2009, mds(2009, md(1, 1), md(4, 10), md(4, 13), md(5, 4), md(5, 25), md(8, 31), md(12, 25), md(12, 28))},
        {2010, mds(2010, md(1, 1), md(4, 2), md(4, 5), md(5, 3), md(5, 31), md(8, 30), md(12, 27), md(12, 28))},
        // https://www.gov.uk/bank-holidays
        {2012, mds(2012, md(1, 2), md(4, 6), md(4, 9), md(5, 7), md(6, 4), md(6, 5), md(8, 27), md(12, 25), md(12, 26))},
        {2013, mds(2013, md(1, 1), md(3, 29), md(4, 1), md(5, 6), md(5, 27), md(8, 26), md(12, 25), md(12, 26))},
        {2014, mds(2014, md(1, 1), md(4, 18), md(4, 21), md(5, 5), md(5, 26), md(8, 25), md(12, 25), md(12, 26))},
        {2015, mds(2015, md(1, 1), md(4, 3), md(4, 6), md(5, 4), md(5, 25), md(8, 31), md(12, 25), md(12, 28))},
        {2016, mds(2016, md(1, 1), md(3, 25), md(3, 28), md(5, 2), md(5, 30), md(8, 29), md(12, 26), md(12, 27))},
        {2020, mds(2020, md(1, 1), md(4, 10), md(4, 13), md(5, 8), md(5, 25), md(8, 31), md(12, 25), md(12, 28))},
        {2022, mds(2022, md(1, 3), md(4, 15), md(4, 18), md(5, 2), md(6, 2), md(6, 3), md(8, 29), md(9, 19), md(12, 26), md(12, 27))},
        {2023, mds(2023, md(1, 2), md(4, 7), md(4, 10), md(5, 1), md(5, 8), md(5, 29), md(8, 28), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_gblo")
  public void test_gblo(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(GBLO.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar FRPA = GlobalHolidayCalendars.generateParis();

  public static Object[][] data_frpa() {
    return new Object[][] {
        // dates not shifted if fall on a weekend
        {2003, mds(2003, md(1, 1), md(4, 18), md(4, 21), md(5, 1), md(5, 8), md(5, 29),
            md(6, 9), md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
        {2004, mds(2004, md(1, 1), md(4, 9), md(4, 12), md(5, 1), md(5, 8), md(5, 20), md(5, 31),
            md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
        {2005, mds(2005, md(1, 1), md(3, 25), md(3, 28), md(5, 1), md(5, 5), md(5, 8),
            md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
        {2006, mds(2006, md(1, 1), md(4, 14), md(4, 17), md(5, 1), md(5, 8), md(5, 25),
            md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
        {2007, mds(2007, md(1, 1), md(4, 6), md(4, 9), md(5, 1), md(5, 8), md(5, 17),
            md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
        {2008, mds(2008, md(1, 1), md(3, 21), md(3, 24), md(5, 1), md(5, 8), md(5, 12), md(5, 24),
            md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},

        {2012, mds(2012, md(1, 1), md(4, 6), md(4, 9), md(5, 1), md(5, 8), md(5, 17),
            md(5, 28), md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
        {2013, mds(2013, md(1, 1), md(3, 29), md(4, 1), md(5, 1), md(5, 8), md(5, 9), md(5, 20),
            md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
        {2014, mds(2014, md(1, 1), md(4, 18), md(4, 21), md(5, 1), md(5, 8), md(5, 29),
            md(6, 9), md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
        {2015, mds(2015, md(1, 1), md(4, 3), md(4, 6), md(5, 1), md(5, 8), md(5, 14), md(5, 25),
            md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
        {2016, mds(2016, md(1, 1), md(3, 25), md(3, 28), md(5, 1), md(5, 5), md(5, 8), md(5, 16),
            md(7, 14), md(8, 15), md(11, 1), md(11, 11), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_frpa")
  public void test_frpa(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(FRPA.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar DEFR = GlobalHolidayCalendars.generateFrankfurt();

  public static Object[][] data_defr() {
    return new Object[][] {
        // dates not shifted if fall on a weekend
        {2014, mds(2014, md(1, 1), md(4, 18), md(4, 21), md(5, 1), md(5, 29), md(6, 9), md(6, 19),
            md(10, 3), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
        {2015, mds(2015, md(1, 1), md(4, 3), md(4, 6), md(5, 1), md(5, 14), md(5, 25), md(6, 4),
            md(10, 3), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
        {2016, mds(2016, md(1, 1), md(3, 25), md(3, 28), md(5, 1), md(5, 5), md(5, 16), md(5, 26),
            md(10, 3), md(12, 25), md(12, 26), md(12, 31))},
        {2017, mds(2017, md(1, 1), md(4, 14), md(4, 17), md(5, 1), md(5, 25), md(6, 5), md(6, 15),
            md(10, 3), md(10, 31), md(12, 25), md(12, 26), md(12, 31))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_defr")
  public void test_defr(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(DEFR.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar CHZU = GlobalHolidayCalendars.generateZurich();

  public static Object[][] data_chzu() {
    return new Object[][] {
        // dates not shifted if fall on a weekend
        {2012, mds(2012, md(1, 1), md(1, 2), md(4, 6), md(4, 9), md(5, 1), md(5, 17), md(5, 28),
            md(8, 1), md(12, 25), md(12, 26))},
        {2013, mds(2013, md(1, 1), md(1, 2), md(3, 29), md(4, 1), md(5, 1), md(5, 9), md(5, 20),
            md(8, 1), md(12, 25), md(12, 26))},
        {2014, mds(2014, md(1, 1), md(1, 2), md(4, 18), md(4, 21), md(5, 1), md(5, 29), md(6, 9),
            md(8, 1), md(12, 25), md(12, 26))},
        {2015, mds(2015, md(1, 1), md(1, 2), md(4, 3), md(4, 6), md(5, 1), md(5, 14), md(5, 25),
            md(8, 1), md(12, 25), md(12, 26))},
        {2016, mds(2016, md(1, 1), md(1, 2), md(3, 25), md(3, 28), md(5, 1), md(5, 5), md(5, 16),
            md(8, 1), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_chzu")
  public void test_chzu(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(CHZU.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar EUTA = GlobalHolidayCalendars.generateEuropeanTarget();

  public static Object[][] data_euta() {
    return new Object[][] {
        // 1997 - 1998 (testing phase), Jan 1, christmas day
        {1997, mds(1997, md(1, 1), md(12, 25))},
        {1998, mds(1998, md(1, 1), md(12, 25))},
        // in 1999, Jan 1, christmas day, Dec 26, Dec 31
        {1999, mds(1999, md(1, 1), md(12, 25), md(12, 31))},
        // in 2000, Jan 1, good friday, easter monday, May 1, christmas day, Dec 26
        {2000, mds(2000, md(1, 1), md(4, 21), md(4, 24), md(5, 1), md(12, 25), md(12, 26))},
        // in 2001, Jan 1, good friday, easter monday, May 1, christmas day, Dec 26, Dec 31
        {2001, mds(2001, md(1, 1), md(4, 13), md(4, 16), md(5, 1), md(12, 25), md(12, 26), md(12, 31))},
        // from 2002, Jan 1, good friday, easter monday, May 1, christmas day, Dec 26
        {2002, mds(2002, md(1, 1), md(3, 29), md(4, 1), md(5, 1), md(12, 25), md(12, 26))},
        {2003, mds(2003, md(1, 1), md(4, 18), md(4, 21), md(5, 1), md(12, 25), md(12, 26))},
        // http://www.ecb.europa.eu/home/html/holidays.en.html
        {2014, mds(2014, md(1, 1), md(4, 18), md(4, 21), md(5, 1), md(12, 25), md(12, 26))},
        {2015, mds(2015, md(1, 1), md(4, 3), md(4, 6), md(5, 1), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_euta")
  public void test_euta(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(EUTA.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar USGS = GlobalHolidayCalendars.generateUsGovtSecurities();

  public static Object[][] data_usgs() {
    return new Object[][] {
        // http://www.sifma.org/uploadedfiles/research/statistics/statisticsfiles/misc-us-historical-holiday-market-recommendations-sifma.pdf?n=53384
        {1996, mds(1996, md(1, 1), md(1, 15), md(2, 19), md(4, 5), md(5, 27), md(7, 4),
            md(9, 2), md(10, 14), md(11, 11), md(11, 28), md(12, 25))},
        {1997, mds(1997, md(1, 1), md(1, 20), md(2, 17), md(3, 28), md(5, 26), md(7, 4),
            md(9, 1), md(10, 13), md(11, 11), md(11, 27), md(12, 25))},
        {1998, mds(1998, md(1, 1), md(1, 19), md(2, 16), md(4, 10), md(5, 25), md(7, 3),
            md(9, 7), md(10, 12), md(11, 11), md(11, 26), md(12, 25))},
        {1999, mds(1999, md(1, 1), md(1, 18), md(2, 15), md(4, 2), md(5, 31), md(7, 5),
            md(9, 6), md(10, 11), md(11, 11), md(11, 25), md(12, 24))},
        {2000, mds(2000, md(1, 17), md(2, 21), md(4, 21), md(5, 29), md(7, 4),
            md(9, 4), md(10, 9), md(11, 23), md(12, 25))},
        {2001, mds(2001, md(1, 1), md(1, 15), md(2, 19), md(4, 13), md(5, 28), md(7, 4),
            md(9, 3), md(10, 8), md(11, 12), md(11, 22), md(12, 25))},
        {2002, mds(2002, md(1, 1), md(1, 21), md(2, 18), md(3, 29), md(5, 27), md(7, 4),
            md(9, 2), md(10, 14), md(11, 11), md(11, 28), md(12, 25))},
        {2003, mds(2003, md(1, 1), md(1, 20), md(2, 17), md(4, 18), md(5, 26), md(7, 4),
            md(9, 1), md(10, 13), md(11, 11), md(11, 27), md(12, 25))},
        {2004, mds(2004, md(1, 1), md(1, 19), md(2, 16), md(4, 9), md(5, 31), md(7, 5),
            md(9, 6), md(10, 11), md(11, 11), md(11, 25), md(12, 24))},
        {2005, mds(2005, md(1, 17), md(2, 21), md(3, 25), md(5, 30), md(7, 4),
            md(9, 5), md(10, 10), md(11, 11), md(11, 24), md(12, 26))},
        {2006, mds(2006, md(1, 2), md(1, 16), md(2, 20), md(4, 14), md(5, 29), md(7, 4),
            md(9, 4), md(10, 9), md(11, 23), md(12, 25))},
        {2007, mds(2007, md(1, 1), md(1, 15), md(2, 19), md(4, 6), md(5, 28), md(7, 4),
            md(9, 3), md(10, 8), md(11, 12), md(11, 22), md(12, 25))},
        {2008, mds(2008, md(1, 1), md(1, 21), md(2, 18), md(3, 21), md(5, 26), md(7, 4),
            md(9, 1), md(10, 13), md(11, 11), md(11, 27), md(12, 25))},
        {2009, mds(2009, md(1, 1), md(1, 19), md(2, 16), md(4, 10), md(5, 25), md(7, 3),
            md(9, 7), md(10, 12), md(11, 11), md(11, 26), md(12, 25))},
        {2010, mds(2010, md(1, 1), md(1, 18), md(2, 15), md(4, 2), md(5, 31), md(7, 5),
            md(9, 6), md(10, 11), md(11, 11), md(11, 25), md(12, 24))},
        {2011, mds(2011, md(1, 17), md(2, 21), md(4, 22), md(5, 30), md(7, 4),
            md(9, 5), md(10, 10), md(11, 11), md(11, 24), md(12, 26))},
        {2012, mds(2012, md(1, 2), md(1, 16), md(2, 20), md(4, 6), md(5, 28), md(7, 4),
            md(9, 3), md(10, 8), md(10, 30), md(11, 12), md(11, 22), md(12, 25))},
        {2013, mds(2013, md(1, 1), md(1, 21), md(2, 18), md(3, 29), md(5, 27), md(7, 4),
            md(9, 2), md(10, 14), md(11, 11), md(11, 28), md(12, 25))},
        {2014, mds(2014, md(1, 1), md(1, 20), md(2, 17), md(4, 18), md(5, 26), md(7, 4),
            md(9, 1), md(10, 13), md(11, 11), md(11, 27), md(12, 25))},
        {2015, mds(2015, md(1, 1), md(1, 19), md(2, 16), md(4, 3), md(5, 25), md(7, 3),
            md(9, 7), md(10, 12), md(11, 11), md(11, 26), md(12, 25))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_usgs")
  public void test_usgs(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(USGS.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar USNY = GlobalHolidayCalendars.generateUsNewYork();

  public static Object[][] data_usny() {
    return new Object[][] {
        // http://www.cs.ny.gov/attendance_leave/2012_legal_holidays.cfm
        // change year for other pages
        {2008, mds(2008, md(1, 1), md(1, 21), md(2, 18), md(5, 26), md(7, 4),
            md(9, 1), md(10, 13), md(11, 11), md(11, 27), md(12, 25))},
        {2009, mds(2009, md(1, 1), md(1, 19), md(2, 16), md(5, 25), md(7, 4),
            md(9, 7), md(10, 12), md(11, 11), md(11, 26), md(12, 25))},
        {2010, mds(2010, md(1, 1), md(1, 18), md(2, 15), md(5, 31), md(7, 5),
            md(9, 6), md(10, 11), md(11, 11), md(11, 25), md(12, 25))},
        {2011, mds(2011, md(1, 1), md(1, 17), md(2, 21), md(5, 30), md(7, 4),
            md(9, 5), md(10, 10), md(11, 11), md(11, 24), md(12, 26))},
        {2012, mds(2012, md(1, 2), md(1, 16), md(2, 20), md(5, 28), md(7, 4),
            md(9, 3), md(10, 8), md(11, 12), md(11, 22), md(12, 25))},
        {2013, mds(2013, md(1, 1), md(1, 21), md(2, 18), md(5, 27), md(7, 4),
            md(9, 2), md(10, 14), md(11, 11), md(11, 28), md(12, 25))},
        {2014, mds(2014, md(1, 1), md(1, 20), md(2, 17), md(5, 26), md(7, 4),
            md(9, 1), md(10, 13), md(11, 11), md(11, 27), md(12, 25))},
        {2015, mds(2015, md(1, 1), md(1, 19), md(2, 16), md(5, 25), md(7, 4),
            md(9, 7), md(10, 12), md(11, 11), md(11, 26), md(12, 25))},
        {2021, mds(2021, md(1, 1), md(1, 18), md(2, 15), md(5, 31), md(7, 5),
            md(9, 6), md(10, 11), md(11, 11), md(11, 25), md(12, 25))},
        {2022, mds(2022, md(1, 1), md(1, 17), md(2, 21), md(5, 30), md(6, 20), md(7, 4),
            md(9, 5), md(10, 10), md(11, 11), md(11, 24), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_usny")
  public void test_usny(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(USNY.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar NYFD = GlobalHolidayCalendars.generateNewYorkFed();

  public static Object[][] data_nyfd() {
    return new Object[][] {
        // http://www.ny.frb.org/aboutthefed/holiday_schedule.html
        // http://web.archive.org/web/20080403230805/http://www.ny.frb.org/aboutthefed/holiday_schedule.html
        // http://web.archive.org/web/20100827003740/http://www.ny.frb.org/aboutthefed/holiday_schedule.html
        // http://web.archive.org/web/20031007222458/http://www.ny.frb.org/aboutthefed/holiday_schedule.html
        // http://www.federalreserve.gov/aboutthefed/k8.htm
        {2003, mds(2003, md(1, 1), md(1, 20), md(2, 17), md(5, 26), md(7, 4),
            md(9, 1), md(10, 13), md(11, 11), md(11, 27), md(12, 25))},
        {2004, mds(2004, md(1, 1), md(1, 19), md(2, 16), md(5, 31), md(7, 5),
            md(9, 6), md(10, 11), md(11, 11), md(11, 25))},
        {2005, mds(2005, md(1, 17), md(2, 21), md(5, 30), md(7, 4),
            md(9, 5), md(10, 10), md(11, 11), md(11, 24), md(12, 26))},
        {2006, mds(2006, md(1, 2), md(1, 16), md(2, 20), md(5, 29), md(7, 4),
            md(9, 4), md(10, 9), md(11, 23), md(12, 25))},
        {2007, mds(2007, md(1, 1), md(1, 15), md(2, 19), md(5, 28), md(7, 4),
            md(9, 3), md(10, 8), md(11, 12), md(11, 22), md(12, 25))},
        {2008, mds(2008, md(1, 1), md(1, 21), md(2, 18), md(5, 26), md(7, 4),
            md(9, 1), md(10, 13), md(11, 11), md(11, 27), md(12, 25))},
        {2009, mds(2009, md(1, 1), md(1, 19), md(2, 16), md(5, 25),
            md(9, 7), md(10, 12), md(11, 11), md(11, 26), md(12, 25))},
        {2010, mds(2010, md(1, 1), md(1, 18), md(2, 15), md(5, 31), md(7, 5),
            md(9, 6), md(10, 11), md(11, 11), md(11, 25))},
        {2011, mds(2011, md(1, 17), md(2, 21), md(5, 30), md(7, 4),
            md(9, 5), md(10, 10), md(11, 11), md(11, 24), md(12, 26))},
        {2012, mds(2012, md(1, 2), md(1, 16), md(2, 20), md(5, 28), md(7, 4),
            md(9, 3), md(10, 8), md(11, 12), md(11, 22), md(12, 25))},
        {2013, mds(2013, md(1, 1), md(1, 21), md(2, 18), md(5, 27), md(7, 4),
            md(9, 2), md(10, 14), md(11, 11), md(11, 28), md(12, 25))},
        {2014, mds(2014, md(1, 1), md(1, 20), md(2, 17), md(5, 26), md(7, 4),
            md(9, 1), md(10, 13), md(11, 11), md(11, 27), md(12, 25))},
        {2015, mds(2015, md(1, 1), md(1, 19), md(2, 16), md(5, 25),
            md(9, 7), md(10, 12), md(11, 11), md(11, 26), md(12, 25))},
        {2016, mds(2016, md(1, 1), md(1, 18), md(2, 15), md(5, 30), md(7, 4),
            md(9, 5), md(10, 10), md(11, 11), md(11, 24), md(12, 26))},
        {2017, mds(2017, md(1, 2), md(1, 16), md(2, 20), md(5, 29), md(7, 4),
            md(9, 4), md(10, 9), md(11, 23), md(12, 25))},
        {2018, mds(2018, md(1, 1), md(1, 15), md(2, 19), md(5, 28), md(7, 4),
            md(9, 3), md(10, 8), md(11, 12), md(11, 22), md(12, 25))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_nyfd")
  public void test_nyfd(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(NYFD.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar NYSE = GlobalHolidayCalendars.generateNewYorkStockExchange();

  public static Object[][] data_nyse() {
    return new Object[][] {
        // https://www.nyse.com/markets/hours-calendars
        // http://web.archive.org/web/20110320011340/http://www.nyse.com/about/newsevents/1176373643795.html?sa_campaign=/internal_ads/homepage/08262008holidays
        // http://web.archive.org/web/20080901164729/http://www.nyse.com/about/newsevents/1176373643795.html?sa_campaign=/internal_ads/homepage/08262008holidays
        {2008, mds(2008, md(1, 1), md(1, 21), md(2, 18), md(3, 21), md(5, 26), md(7, 4),
            md(9, 1), md(11, 27), md(12, 25))},
        {2009, mds(2009, md(1, 1), md(1, 19), md(2, 16), md(4, 10), md(5, 25), md(7, 3),
            md(9, 7), md(11, 26), md(12, 25))},
        {2010, mds(2010, md(1, 1), md(1, 18), md(2, 15), md(4, 2), md(5, 31), md(7, 5),
            md(9, 6), md(11, 25), md(12, 24))},
        {2011, mds(2011, md(1, 1), md(1, 17), md(2, 21), md(4, 22), md(5, 30), md(7, 4),
            md(9, 5), md(11, 24), md(12, 26))},
        {2012, mds(2012, md(1, 2), md(1, 16), md(2, 20), md(4, 6), md(5, 28), md(7, 4),
            md(9, 3), md(10, 30), md(11, 22), md(12, 25))},
        {2013, mds(2013, md(1, 1), md(1, 21), md(2, 18), md(3, 29), md(5, 27), md(7, 4),
            md(9, 2), md(11, 28), md(12, 25))},
        {2014, mds(2014, md(1, 1), md(1, 20), md(2, 17), md(4, 18), md(5, 26), md(7, 4),
            md(9, 1), md(11, 27), md(12, 25))},
        {2015, mds(2015, md(1, 1), md(1, 19), md(2, 16), md(4, 3), md(5, 25), md(7, 3),
            md(9, 7), md(11, 26), md(12, 25))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_nyse")
  public void test_nyse(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(NYSE.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar JPTO = GlobalHolidayCalendars.generateTokyo();

  public static Object[][] data_jpto() {
    return new Object[][] {
        // https://www.boj.or.jp/en/about/outline/holi.htm/
        // http://web.archive.org/web/20110513190217/http://www.boj.or.jp/en/about/outline/holi.htm/
        // https://www.japanspecialist.co.uk/travel-tips/national-holidays-in-japan/
        {1999, mds(1999, md(1, 1), md(1, 2), md(1, 3), md(1, 15), md(2, 11), md(3, 22), md(4, 29), md(5, 3), md(5, 4), md(5, 5),
            md(7, 20), md(9, 15), md(9, 23), md(10, 11), md(11, 3), md(11, 23), md(12, 23), md(12, 31))},
        {2000, mds(2000, md(1, 1), md(1, 2), md(1, 3), md(1, 10), md(2, 11), md(3, 20), md(4, 29), md(5, 3), md(5, 4), md(5, 5),
            md(7, 20), md(9, 15), md(9, 23), md(10, 9), md(11, 3), md(11, 23), md(12, 23), md(12, 31))},
        {2001, mds(2001, md(1, 1), md(1, 2), md(1, 3), md(1, 8), md(2, 12), md(3, 20), md(4, 30), md(5, 3), md(5, 4), md(5, 5),
            md(7, 20), md(9, 15), md(9, 24), md(10, 8), md(11, 3), md(11, 23), md(12, 24), md(12, 31))},
        {2002, mds(2002, md(1, 1), md(1, 2), md(1, 3), md(1, 14), md(2, 11), md(3, 21), md(4, 29), md(5, 3), md(5, 4), md(5, 6),
            md(7, 20), md(9, 16), md(9, 23), md(10, 14), md(11, 4), md(11, 23), md(12, 23), md(12, 31))},
        {2003, mds(2003, md(1, 1), md(1, 2), md(1, 3), md(1, 13), md(2, 11), md(3, 21), md(4, 29), md(5, 3), md(5, 4), md(5, 5),
            md(7, 21), md(9, 15), md(9, 23), md(10, 13), md(11, 3), md(11, 24), md(12, 23), md(12, 31))},
        {2004, mds(2004, md(1, 1), md(1, 2), md(1, 3), md(1, 12), md(2, 11), md(3, 20), md(4, 29), md(5, 3), md(5, 4), md(5, 5),
            md(7, 19), md(9, 20), md(9, 23), md(10, 11), md(11, 3), md(11, 23), md(12, 23), md(12, 31))},
        {2005, mds(2005, md(1, 1), md(1, 2), md(1, 3), md(1, 10), md(2, 11), md(3, 21), md(4, 29), md(5, 3), md(5, 4), md(5, 5),
            md(7, 18), md(9, 19), md(9, 23), md(10, 10), md(11, 3), md(11, 23), md(12, 23), md(12, 31))},
        {2006, mds(2006, md(1, 1), md(1, 2), md(1, 3), md(1, 9), md(2, 11), md(3, 21), md(4, 29), md(5, 3), md(5, 4), md(5, 5),
            md(7, 17), md(9, 18), md(9, 23), md(10, 9), md(11, 3), md(11, 23), md(12, 23), md(12, 31))},
        {2011, mds(2011, md(1, 1), md(1, 2), md(1, 3), md(1, 10), md(2, 11), md(3, 21), md(4, 29), md(5, 3), md(5, 4), md(5, 5),
            md(7, 18), md(9, 19), md(9, 23), md(10, 10), md(11, 3), md(11, 23), md(12, 23), md(12, 31))},
        {2012, mds(2012, md(1, 1), md(1, 2), md(1, 3), md(1, 9), md(2, 11), md(3, 20), md(4, 30), md(5, 3), md(5, 4), md(5, 5),
            md(7, 16), md(9, 17), md(9, 22), md(10, 8), md(11, 3), md(11, 23), md(12, 24), md(12, 31))},
        {2013, mds(2013, md(1, 1), md(1, 2), md(1, 3), md(1, 14), md(2, 11), md(3, 20), md(4, 29),
            md(5, 3), md(5, 4), md(5, 5), md(5, 6),
            md(7, 15), md(9, 16), md(9, 23), md(10, 14), md(11, 4), md(11, 23), md(12, 23), md(12, 31))},
        {2014, mds(2014, md(1, 1), md(1, 2), md(1, 3), md(1, 13), md(2, 11), md(3, 21), md(4, 29),
            md(5, 3), md(5, 4), md(5, 5), md(5, 6),
            md(7, 21), md(9, 15), md(9, 23), md(10, 13), md(11, 3), md(11, 24), md(12, 23), md(12, 31))},
        {2015, mds(2015, md(1, 1), md(1, 2), md(1, 3), md(1, 12), md(2, 11), md(3, 21), md(4, 29),
            md(5, 3), md(5, 4), md(5, 5), md(5, 6),
            md(7, 20), md(9, 21), md(9, 22), md(9, 23), md(10, 12), md(11, 3), md(11, 23), md(12, 23), md(12, 31))},
        {2018, mds(2018, md(1, 1), md(1, 2), md(1, 3), md(1, 8), md(2, 12), md(3, 21), md(4, 30),
            md(5, 3), md(5, 4), md(5, 5), md(7, 16), md(8, 11), md(9, 17), md(9, 24),
            md(10, 8), md(11, 3), md(11, 23), md(12, 23), md(12, 24), md(12, 31))},
        {2019, mds(2019, md(1, 1), md(1, 2), md(1, 3), md(1, 14), md(2, 11), md(3, 21), md(4, 29), md(4, 30),
            md(5, 1), md(5, 2), md(5, 3), md(5, 4), md(5, 5), md(5, 6), md(7, 15), md(8, 12), md(9, 16), md(9, 23),
            md(10, 14), md(10, 22), md(11, 4), md(11, 23), md(12, 31))},
        {2020, mds(2020, md(1, 1), md(1, 2), md(1, 3), md(1, 13), md(2, 11), md(2, 24), md(3, 20), md(4, 29),
            md(5, 3), md(5, 4), md(5, 5), md(5, 6), md(7, 23), md(7, 24), md(8, 10), md(9, 21), md(9, 22),
            md(11, 3), md(11, 23), md(12, 31))},
        {2021, mds(2021, md(1, 1), md(1, 11), md(2, 11), md(2, 23), md(3, 20), md(4, 29),
            md(5, 3), md(5, 4), md(5, 5), md(7, 22), md(7, 23), md(8, 9), md(9, 20),
            md(9, 23), md(11, 3), md(11, 23), md(12, 31))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_jpto")
  public void test_jpto(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(JPTO.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar AUSY = GlobalHolidayCalendars.generateSydney();

  public static Object[][] data_ausy() {
    return new Object[][] {
        {2012, mds(2012, md(1, 1), md(1, 2), md(1, 26), md(4, 6), md(4, 7), md(4, 8), md(4, 9),
            md(4, 25), md(6, 11), md(8, 6), md(10, 1), md(12, 25), md(12, 26))},
        {2013, mds(2013, md(1, 1), md(1, 26), md(1, 28), md(3, 29), md(3, 30), md(3, 31), md(4, 1),
            md(4, 25), md(6, 10), md(8, 5), md(10, 7), md(12, 25), md(12, 26))},
        {2014, mds(2014, md(1, 1), md(1, 26), md(1, 27), md(4, 18), md(4, 19), md(4, 20), md(4, 21),
            md(4, 25), md(6, 9), md(8, 4), md(10, 6), md(12, 25), md(12, 26))},
        {2015, mds(2015, md(1, 1), md(1, 26), md(4, 3), md(4, 4), md(4, 5), md(4, 6), md(4, 25),
            md(6, 8), md(8, 3), md(10, 5), md(12, 25), md(12, 26), md(12, 27), md(12, 28))},
        {2016, mds(2016, md(1, 1), md(1, 26), md(3, 25), md(3, 26), md(3, 27), md(3, 28),
            md(4, 25), md(6, 13), md(8, 1), md(10, 3), md(12, 25), md(12, 26), md(12, 27))},
        {2017, mds(2017, md(1, 1), md(1, 2), md(1, 26), md(4, 14), md(4, 15), md(4, 16), md(4, 17),
            md(4, 25), md(6, 12), md(8, 7), md(10, 2), md(12, 25), md(12, 26))},
        {2022, mds(2022, md(1, 3), md(1, 26), md(4, 15), md(4, 18),
            md(4, 25), md(6, 13), md(8, 1), md(9, 22), md(10, 3), md(12, 26), md(12, 27))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_ausy")
  public void test_ausy(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(AUSY.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar BRBD = GlobalHolidayCalendars.generateBrazil();

  public static Object[][] data_brbd() {
    // http://www.planalto.gov.br/ccivil_03/leis/2002/L10607.htm
    // fixing data
    return new Object[][] {
        {2013, mds(2013, md(1, 1), md(2, 11), md(2, 12), md(3, 29), md(4, 21), md(5, 1),
            md(5, 30), md(9, 7), md(10, 12), md(11, 2), md(11, 15), md(12, 25))},
        {2014, mds(2014, md(1, 1), md(3, 3), md(3, 4), md(4, 18), md(4, 21), md(5, 1),
            md(6, 19), md(9, 7), md(10, 12), md(11, 2), md(11, 15), md(12, 25))},
        {2015, mds(2015, md(1, 1), md(2, 16), md(2, 17), md(4, 3), md(4, 21), md(5, 1),
            md(6, 4), md(9, 7), md(10, 12), md(11, 2), md(11, 15), md(12, 25))},
        {2016, mds(2016, md(1, 1), md(2, 8), md(2, 9), md(3, 25), md(4, 21), md(5, 1),
            md(5, 26), md(9, 7), md(10, 12), md(11, 2), md(11, 15), md(12, 25))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_brbd")
  public void test_brbd(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(BRBD.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar CAMO = GlobalHolidayCalendars.generateMontreal();

  public static Object[][] data_camo() {
    // https://www.bankofcanada.ca/about/contact-information/bank-of-canada-holiday-schedule/
    // also indicate day after new year and boxing day, but no other sources for this
    return new Object[][] {
        {2017, mds(2017, md(1, 2), md(4, 14),
            md(5, 22), md(6, 26), md(7, 3), md(9, 4), md(10, 9), md(12, 25))},
        {2018, mds(2018, md(1, 1), md(3, 30),
            md(5, 21), md(6, 25), md(7, 2), md(9, 3), md(10, 8), md(12, 25))},
        {2022, mds(2022, md(1, 3), md(4, 15),
            md(5, 23), md(6, 24), md(7, 1), md(9, 5), md(10, 10), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_camo")
  public void test_camo(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(CAMO.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar CATO = GlobalHolidayCalendars.generateToronto();

  public static Object[][] data_cato() {
    return new Object[][] {
        {2009, mds(2009, md(1, 1), md(2, 16), md(4, 10),
            md(5, 18), md(7, 1), md(8, 3), md(9, 7), md(10, 12), md(11, 11), md(12, 25), md(12, 28))},
        {2010, mds(2010, md(1, 1), md(2, 15), md(4, 2),
            md(5, 24), md(7, 1), md(8, 2), md(9, 6), md(10, 11), md(11, 11), md(12, 27), md(12, 28))},
        {2011, mds(2011, md(1, 3), md(2, 21), md(4, 22),
            md(5, 23), md(7, 1), md(8, 1), md(9, 5), md(10, 10), md(11, 11), md(12, 26), md(12, 27))},
        {2012, mds(2012, md(1, 2), md(2, 20), md(4, 6),
            md(5, 21), md(7, 2), md(8, 6), md(9, 3), md(10, 8), md(11, 12), md(12, 25), md(12, 26))},
        {2013, mds(2013, md(1, 1), md(2, 18), md(3, 29),
            md(5, 20), md(7, 1), md(8, 5), md(9, 2), md(10, 14), md(11, 11), md(12, 25), md(12, 26))},
        {2014, mds(2014, md(1, 1), md(2, 17), md(4, 18),
            md(5, 19), md(7, 1), md(8, 4), md(9, 1), md(10, 13), md(11, 11), md(12, 25), md(12, 26))},
        {2015, mds(2015, md(1, 1), md(2, 16), md(4, 3),
            md(5, 18), md(7, 1), md(8, 3), md(9, 7), md(10, 12), md(11, 11), md(12, 25), md(12, 28))},
        {2016, mds(2016, md(1, 1), md(2, 15), md(3, 25),
            md(5, 23), md(7, 1), md(8, 1), md(9, 5), md(10, 10), md(11, 11), md(12, 26), md(12, 27))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_cato")
  public void test_cato(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(CATO.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar CZPR = GlobalHolidayCalendars.generatePrague();

  public static Object[][] data_czpr() {
    // official data from Czech National Bank
    // https://www.cnb.cz/en/public/media_service/schedules/media_svatky.html
    return new Object[][] {
        {2008, mds(2008, md(1, 1), md(3, 24), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
        {2009, mds(2009, md(1, 1), md(4, 13), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
        {2010, mds(2010, md(1, 1), md(4, 5), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
        {2011, mds(2011, md(1, 1), md(4, 25), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
        {2012, mds(2012, md(1, 1), md(4, 9), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
        {2013, mds(2013, md(1, 1), md(4, 1), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
        {2014, mds(2014, md(1, 1), md(4, 21), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
        {2015, mds(2015, md(1, 1), md(4, 6), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
        {2016, mds(2016, md(1, 1), md(3, 25), md(3, 28), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
        {2017, mds(2017, md(1, 1), md(4, 14), md(4, 17), md(5, 1), md(5, 8),
            md(7, 5), md(7, 6), md(9, 28), md(10, 28), md(11, 17), md(12, 24), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_czpr")
  public void test_czpr(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(CZPR.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar DKCO = GlobalHolidayCalendars.generateCopenhagen();

  public static Object[][] data_dkco() {
    // official data from Danish Bankers association via web archive
    return new Object[][] {
        {2013, mds(2013, md(1, 1), md(3, 28), md(3, 29), md(4, 1),
            md(4, 26), md(5, 9), md(5, 10), md(5, 20), md(6, 5), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
        {2014, mds(2014, md(1, 1), md(4, 17), md(4, 18), md(4, 21),
            md(5, 16), md(5, 29), md(5, 30), md(6, 5), md(6, 9), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
        {2015, mds(2015, md(1, 1), md(4, 2), md(4, 3), md(4, 6),
            md(5, 1), md(5, 14), md(5, 15), md(5, 25), md(6, 5), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
        {2016, mds(2016, md(1, 1), md(3, 24), md(3, 25), md(3, 28),
            md(4, 22), md(5, 5), md(5, 6), md(5, 16), md(6, 5), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_dkco")
  public void test_dkco(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(DKCO.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar HUBU = GlobalHolidayCalendars.generateBudapest();

  public static Object[][] data_hubu() {
    // http://www.mnb.hu/letoltes/bubor2.xls
    // http://holidays.kayaposoft.com/public_holidays.php?year=2013&country=hun&region=#
    return new Object[][] {
        {2012, mds(2012, md(3, 15), md(3, 16), md(4, 9), md(4, 30), md(5, 1), md(5, 28),
            md(8, 20), md(10, 22), md(10, 23), md(11, 1), md(11, 2), md(12, 24), md(12, 25), md(12, 26), md(12, 31)),
            ImmutableList.of(date(2012, 3, 24), date(2012, 5, 5), date(2012, 10, 27),
                date(2012, 11, 10), date(2012, 12, 15), date(2012, 12, 29))},
        {2013, mds(2013, md(1, 1), md(3, 15), md(4, 1), md(5, 1), md(5, 20),
            md(8, 19), md(8, 20), md(10, 23), md(11, 1), md(12, 24), md(12, 25), md(12, 26), md(12, 27)),
            ImmutableList.of(date(2013, 8, 24), date(2013, 12, 7), date(2013, 12, 21))},
        {2014, mds(2014, md(1, 1), md(3, 15), md(4, 21), md(5, 1), md(5, 2),
            md(6, 9), md(8, 20), md(10, 23), md(10, 24), md(12, 24), md(12, 25), md(12, 26)),
            ImmutableList.of(date(2014, 5, 10), date(2014, 10, 18))},
        {2015, mds(2015, md(1, 1), md(1, 2), md(3, 15), md(4, 6), md(5, 1), md(5, 25),
            md(8, 20), md(8, 21), md(10, 23), md(12, 24), md(12, 25), md(12, 26)),
            ImmutableList.of(date(2015, 1, 10), date(2015, 8, 8), date(2015, 12, 12))},
        {2016, mds(2016, md(1, 1), md(3, 14), md(3, 15), md(3, 28), md(5, 1), md(5, 16),
            md(10, 31), md(11, 1), md(12, 24), md(12, 25), md(12, 26)),
            ImmutableList.of(date(2016, 3, 5), date(2016, 10, 15))},
        {2020, mds(2020, md(1, 1), md(3, 15), md(4, 10), md(4, 13), md(5, 1), md(6, 1),
            md(8, 20), md(8, 21), md(10, 23), md(12, 24), md(12, 25), md(12, 26)),
            ImmutableList.of(date(2020, 8, 29), date(2020, 12, 12))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_hubu")
  public void test_hubu(int year, List<LocalDate> holidays, List<LocalDate> workDays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = (holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY) &&
          !workDays.contains(date);
      assertThat(HUBU.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar MXMC = GlobalHolidayCalendars.generateMexicoCity();

  public static Object[][] data_mxmc() {
    // http://www.banxico.org.mx/SieInternet/consultarDirectorioInternetAction.do?accion=consultarCuadro&idCuadro=CF111&locale=en
    return new Object[][] {
        {2012, mds(2012, md(1, 1), md(2, 6), md(3, 19), md(4, 5), md(4, 6),
            md(5, 1), md(9, 16), md(11, 2), md(11, 19), md(12, 12), md(12, 25))},
        {2013, mds(2013, md(1, 1), md(2, 4), md(3, 18), md(3, 28), md(3, 29),
            md(5, 1), md(9, 16), md(11, 2), md(11, 18), md(12, 12), md(12, 25))},
        {2014, mds(2014, md(1, 1), md(2, 3), md(3, 17), md(4, 17), md(4, 18),
            md(5, 1), md(9, 16), md(11, 2), md(11, 17), md(12, 12), md(12, 25))},
        {2015, mds(2015, md(1, 1), md(2, 2), md(3, 16), md(4, 2), md(4, 3),
            md(5, 1), md(9, 16), md(11, 2), md(11, 16), md(12, 12), md(12, 25))},
        {2016, mds(2016, md(1, 1), md(2, 1), md(3, 21), md(3, 24), md(3, 25),
            md(5, 1), md(9, 16), md(11, 2), md(11, 21), md(12, 12), md(12, 25))},
        {2024, mds(2024, md(1, 1), md(2, 5), md(3, 18), md(3, 28), md(3, 29),
            md(5, 1), md(9, 16), md(10, 1), md(11, 2), md(11, 18), md(12, 12), md(12, 25))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_mxmc")
  public void test_mxmc(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(MXMC.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar NOOS = GlobalHolidayCalendars.generateOslo();

  public static Object[][] data_noos() {
    // official data from Oslo Bors via web archive
    return new Object[][] {
        {2009, mds(2009, md(1, 1), md(4, 9), md(4, 10), md(4, 13),
            md(5, 1), md(5, 21), md(6, 1), md(12, 24), md(12, 25), md(12, 31))},
        {2011, mds(2011, md(4, 21), md(4, 22), md(4, 25),
            md(5, 17), md(6, 2), md(6, 13), md(12, 26))},
        {2012, mds(2012, md(4, 5), md(4, 6), md(4, 9),
            md(5, 1), md(5, 17), md(5, 28), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
        {2013, mds(2013, md(1, 1), md(3, 28), md(3, 29), md(4, 1),
            md(5, 1), md(5, 9), md(5, 17), md(5, 20), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
        {2014, mds(2014, md(1, 1), md(4, 17), md(4, 18), md(4, 21),
            md(5, 1), md(5, 17), md(5, 29), md(6, 9), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
        {2015, mds(2015, md(1, 1), md(4, 2), md(4, 3), md(4, 6),
            md(5, 1), md(5, 14), md(5, 25), md(12, 24), md(12, 25), md(12, 31))},
        {2016, mds(2016, md(1, 1), md(3, 24), md(3, 25), md(3, 28),
            md(5, 5), md(5, 16), md(5, 17), md(12, 26))},
        {2017, mds(2017, md(4, 13), md(4, 14), md(4, 17),
            md(5, 1), md(5, 17), md(5, 25), md(6, 5), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_noos")
  public void test_noos(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(NOOS.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar NZAU = GlobalHolidayCalendars.generateAuckland();

  public static Object[][] data_nzau() {
    // https://www.govt.nz/browse/work/public-holidays-and-work/public-holidays-and-anniversary-dates/
    // https://www.employment.govt.nz/leave-and-holidays/public-holidays/public-holidays-and-anniversary-dates/dates-for-previous-years/
    return new Object[][] {
        {2015, mds(2015, md(1, 1), md(1, 2), md(1, 26), md(2, 6), md(4, 3), md(4, 6),
            md(4, 27), md(6, 1), md(10, 26), md(12, 25), md(12, 28))},
        {2016, mds(2016, md(1, 1), md(1, 4), md(2, 1), md(2, 8), md(3, 25), md(3, 28),
            md(4, 25), md(6, 6), md(10, 24), md(12, 26), md(12, 27))},
        {2017, mds(2017, md(1, 2), md(1, 3), md(1, 30), md(2, 6), md(4, 14), md(4, 17),
            md(4, 25), md(6, 5), md(10, 23), md(12, 25), md(12, 26))},
        {2018, mds(2018, md(1, 1), md(1, 2), md(1, 29), md(2, 6), md(3, 30), md(4, 2),
            md(4, 25), md(6, 4), md(10, 22), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_nzau")
  public void test_nzau(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(NZAU.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar NZWE = GlobalHolidayCalendars.generateWellington();

  public static Object[][] data_nzwe() {
    // https://www.govt.nz/browse/work/public-holidays-and-work/public-holidays-and-anniversary-dates/
    // https://www.employment.govt.nz/leave-and-holidays/public-holidays/public-holidays-and-anniversary-dates/dates-for-previous-years/
    return new Object[][] {
        {2015, mds(2015, md(1, 1), md(1, 2), md(1, 19), md(2, 6), md(4, 3), md(4, 6),
            md(4, 27), md(6, 1), md(10, 26), md(12, 25), md(12, 28))},
        {2016, mds(2016, md(1, 1), md(1, 4), md(1, 25), md(2, 8), md(3, 25), md(3, 28),
            md(4, 25), md(6, 6), md(10, 24), md(12, 26), md(12, 27))},
        {2017, mds(2017, md(1, 2), md(1, 3), md(1, 23), md(2, 6), md(4, 14), md(4, 17),
            md(4, 25), md(6, 5), md(10, 23), md(12, 25), md(12, 26))},
        {2018, mds(2018, md(1, 1), md(1, 2), md(1, 22), md(2, 6), md(3, 30), md(4, 2),
            md(4, 25), md(6, 4), md(10, 22), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_nzwe")
  public void test_nzwe(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(NZWE.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar NZBD = GlobalHolidayCalendars.generateNewZealand();

  public static Object[][] data_nzbd() {
    // https://www.govt.nz/browse/work/public-holidays-and-work/public-holidays-and-anniversary-dates/
    // https://www.employment.govt.nz/leave-and-holidays/public-holidays/public-holidays-and-anniversary-dates/dates-for-previous-years/
    return new Object[][] {
        {2015, mds(2015, md(1, 1), md(1, 2), md(2, 6), md(4, 3), md(4, 6),
            md(4, 27), md(6, 1), md(10, 26), md(12, 25), md(12, 28))},
        {2016, mds(2016, md(1, 1), md(1, 4), md(2, 8), md(3, 25), md(3, 28),
            md(4, 25), md(6, 6), md(10, 24), md(12, 26), md(12, 27))},
        {2017, mds(2017, md(1, 2), md(1, 3), md(2, 6), md(4, 14), md(4, 17),
            md(4, 25), md(6, 5), md(10, 23), md(12, 25), md(12, 26))},
        {2018, mds(2018, md(1, 1), md(1, 2), md(2, 6), md(3, 30), md(4, 2),
            md(4, 25), md(6, 4), md(10, 22), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_nzbd")
  public void test_nzbd(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(NZBD.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar PLWA = GlobalHolidayCalendars.generateWarsaw();

  public static Object[][] data_plwa() {
    // based on government law data and stock exchange holidays
    return new Object[][] {
        {2013, mds(2013, md(1, 1), md(4, 1),
            md(5, 1), md(5, 3), md(5, 30), md(8, 15), md(11, 1), md(11, 11), md(12, 24), md(12, 25), md(12, 26))},
        {2014, mds(2014, md(1, 1), md(1, 6), md(4, 21),
            md(5, 1), md(6, 19), md(8, 15), md(11, 11), md(12, 24), md(12, 25), md(12, 26))},
        {2015, mds(2015, md(1, 1), md(1, 6), md(4, 6),
            md(5, 1), md(6, 4), md(11, 11), md(12, 24), md(12, 25), md(12, 31))},
        {2016, mds(2016, md(1, 1), md(1, 6), md(3, 28),
            md(5, 3), md(5, 26), md(8, 15), md(11, 1), md(11, 11), md(12, 26))},
        {2017, mds(2017, md(1, 6), md(4, 17),
            md(5, 1), md(5, 3), md(6, 15), md(8, 15), md(11, 1), md(12, 25), md(12, 26))},
        {2018, mds(2018, md(1, 1), md(1, 6), md(4, 1), md(4, 2), md(5, 1), md(5, 3),
            md(5, 20), md(5, 31), md(8, 15), md(11, 1), md(11, 11), md(11, 12), md(12, 24), md(12, 25), md(12, 26), md(12, 31))}
    };
  }

  @ParameterizedTest
  @MethodSource("data_plwa")
  public void test_plwa(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(PLWA.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar SEST = GlobalHolidayCalendars.generateStockholm();

  public static Object[][] data_sest() {
    // official data from published fixing dates
    return new Object[][] {
        {2014, mds(2014, md(1, 1), md(1, 6), md(4, 18), md(4, 21),
            md(5, 1), md(5, 29), md(6, 6), md(6, 20), md(12, 24), md(12, 25), md(12, 26), md(12, 31))},
        {2015, mds(2015, md(1, 1), md(1, 6), md(4, 3), md(4, 6),
            md(5, 1), md(5, 14), md(6, 19), md(12, 24), md(12, 25), md(12, 31))},
        {2016, mds(2016, md(1, 1), md(1, 6), md(3, 25), md(3, 28),
            md(5, 5), md(6, 6), md(6, 24), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_sest")
  public void test_sest(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(SEST.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static final HolidayCalendar ZAJO = GlobalHolidayCalendars.generateJohannesburg();

  public static Object[][] data_zajo() {
    // http://www.gov.za/about-sa/public-holidays
    // https://web.archive.org/web/20151230214958/http://www.gov.za/about-sa/public-holidays
    return new Object[][] {
        {2015, mds(2015, md(1, 1), md(3, 21), md(4, 3), md(4, 6), md(4, 27), md(5, 1),
            md(6, 16), md(8, 10), md(9, 24), md(12, 16), md(12, 25), md(12, 26))},
        {2016, mds(2016, md(1, 1), md(3, 21), md(3, 25), md(3, 28), md(4, 27), md(5, 2),
            md(6, 16), md(8, 3), md(8, 9), md(9, 24), md(12, 16), md(12, 26), md(12, 27))},
        {2017, mds(2017, md(1, 1), md(1, 2), md(3, 21), md(4, 14), md(4, 17), md(4, 27), md(5, 1),
            md(6, 16), md(8, 9), md(9, 25), md(12, 16), md(12, 16), md(12, 25), md(12, 26))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_zajo")
  public void test_zajo(int year, List<LocalDate> holidays) {
    LocalDate date = LocalDate.of(year, 1, 1);
    int len = date.lengthOfYear();
    for (int i = 0; i < len; i++) {
      boolean isHoliday = holidays.contains(date) || date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
      assertThat(ZAJO.isHoliday(date)).as(date.toString()).isEqualTo(isHoliday);
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    HolidayCalendar combined =
        ImmutableHolidayCalendar.combined((ImmutableHolidayCalendar) JPTO, (ImmutableHolidayCalendar) USNY);
    LocalDate date = LocalDate.of(1950, 1, 1);
    while (date.getYear() < 2040) {
      assertThat(combined.isHoliday(date)).as("Date: " + date).isEqualTo(JPTO.isHoliday(date) || USNY.isHoliday(date));
      date = date.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  private static List<LocalDate> mds(int year, MonthDay... monthDays) {
    List<LocalDate> holidays = new ArrayList<>();
    for (MonthDay md : monthDays) {
      holidays.add(md.atYear(year));
    }
    return holidays;
  }

  private static MonthDay md(int month, int day) {
    return MonthDay.of(month, day);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_christmas() {
    // christmas on Friday
    assertThat(GlobalHolidayCalendars.christmasBumpedSatSun(2020)).isEqualTo(LocalDate.of(2020, 12, 25));
    assertThat(GlobalHolidayCalendars.boxingDayBumpedSatSun(2020)).isEqualTo(LocalDate.of(2020, 12, 28));
    // christmas on Saturday
    assertThat(GlobalHolidayCalendars.christmasBumpedSatSun(2021)).isEqualTo(LocalDate.of(2021, 12, 27));
    assertThat(GlobalHolidayCalendars.boxingDayBumpedSatSun(2021)).isEqualTo(LocalDate.of(2021, 12, 28));
    // christmas on Sunday
    assertThat(GlobalHolidayCalendars.christmasBumpedSatSun(2022)).isEqualTo(LocalDate.of(2022, 12, 27));
    assertThat(GlobalHolidayCalendars.boxingDayBumpedSatSun(2022)).isEqualTo(LocalDate.of(2022, 12, 26));
    // christmas on Monday
    assertThat(GlobalHolidayCalendars.christmasBumpedSatSun(2023)).isEqualTo(LocalDate.of(2023, 12, 25));
    assertThat(GlobalHolidayCalendars.boxingDayBumpedSatSun(2023)).isEqualTo(LocalDate.of(2023, 12, 26));
  }

  //-------------------------------------------------------------------------
  public static void coverage() {
    coverPrivateConstructor(GlobalHolidayCalendars.class);
  }

}
