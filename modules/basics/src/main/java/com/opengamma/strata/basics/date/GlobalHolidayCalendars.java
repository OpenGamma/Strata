/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.temporal.TemporalAdjusters.dayOfWeekInMonth;
import static java.time.temporal.TemporalAdjusters.firstInMonth;
import static java.time.temporal.TemporalAdjusters.lastInMonth;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previous;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;

/**
 * Implementation of some common global holiday calendars.
 * <p>
 * The data provided here has been identified through direct research and is not
 * derived from a vendor of holiday calendar data.
 * This data may or may not be sufficient for your production needs.
 */
final class GlobalHolidayCalendars {
  // WARNING!!
  // If you change this file, you must run the main method to update the binary file
  // which is used at runtime (for performance reasons)

  /** Where to store the file. */
  private static final File DATA_FILE =
      new File("src/main/resources/com/opengamma/strata/basics/date/GlobalHolidayCalendars.bin");

  //-------------------------------------------------------------------------
  /**
   * Used to generate a binary holiday data file.
   * 
   * @param args ignored
   * @throws IOException if an IO error occurs
   */
  public static void main(String[] args) throws IOException {
    Files.createParentDirs(DATA_FILE);
    ImmutableHolidayCalendar[] calendars = {
        generateLondon(),
        generateParis(),
        generateFrankfurt(),
        generateZurich(),
        generateEuropeanTarget(),
        generateUsGovtSecurities(),
        generateUsNewYork(),
        generateNewYorkFed(),
        generateNewYorkStockExchange(),
        generateTokyo(),
        generateSydney(),
        generateBrazil(),
        generateMontreal(),
        generateToronto(),
        generatePrague(),
        generateCopenhagen(),
        generateBudapest(),
        generateMexicoCity(),
        generateOslo(),
        generateAuckland(),
        generateWellington(),
        generateNewZealand(),
        generateWarsaw(),
        generateStockholm(),
        generateJohannesburg(),
    };
    try (FileOutputStream fos = new FileOutputStream(DATA_FILE)) {
      try (DataOutputStream out = new DataOutputStream(fos)) {
        out.writeByte('H');
        out.writeByte('C');
        out.writeByte('a');
        out.writeByte('l');
        out.writeShort(calendars.length);
        for (ImmutableHolidayCalendar cal : calendars) {
          cal.writeExternal(out);
        }
      }
    }
  }

  /**
   * Restricted constructor.
   */
  private GlobalHolidayCalendars() {
  }

  //-------------------------------------------------------------------------
  // generate GBLO
  // common law (including before 1871) good friday and christmas day (unadjusted for weekends)
  // from 1871 easter monday, whit monday, first Mon in Aug and boxing day
  // from 1965 to 1970, first in Aug moved to Mon after last Sat in Aug
  // from 1971, whitsun moved to last Mon in May, last Mon in Aug
  // from 1974, added new year
  // from 1978, added first Mon in May
  // see Hansard for specific details
  // 1965, Whitsun, Last Mon Aug - http://hansard.millbanksystems.com/commons/1964/mar/04/staggered-holidays
  // 1966, Whitsun May - http://hansard.millbanksystems.com/commons/1964/mar/04/staggered-holidays
  // 1966, 29th Aug - http://hansard.millbanksystems.com/written_answers/1965/nov/25/august-bank-holiday
  // 1967, 29th May, 28th Aug - http://hansard.millbanksystems.com/written_answers/1965/jun/03/bank-holidays-1967-and-1968
  // 1968, 3rd Jun, 2nd Sep - http://hansard.millbanksystems.com/written_answers/1965/jun/03/bank-holidays-1967-and-1968
  // 1969, 26th May, 1st Sep - http://hansard.millbanksystems.com/written_answers/1967/mar/21/bank-holidays-1969-dates
  // 1970, 25th May, 31st Aug - http://hansard.millbanksystems.com/written_answers/1967/jul/28/bank-holidays
  // 2022, 2nd and 3rd Jun - https://www.gov.uk/government/news/extra-bank-holiday-to-mark-the-queens-platinum-jubilee-in-2022
  // 2022, 19th Sep - https://www.gov.uk/government/news/bank-holiday-announced-for-her-majesty-queen-elizabeth-iis-state-funeral-on-monday-19-september
  static ImmutableHolidayCalendar generateLondon() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      if (year >= 1974) {
        holidays.add(bumpToMon(first(year, 1)));
      }
      // easter
      holidays.add(easter(year).minusDays(2));
      holidays.add(easter(year).plusDays(1));
      // early May
      if (year == 1995 || year == 2020) {
        // ve day
        holidays.add(date(year, 5, 8));
      } else if (year >= 1978) {
        holidays.add(first(year, 5).with(firstInMonth(MONDAY)));
      }
      // spring
      if (year == 2002) {
        // golden jubilee
        holidays.add(date(2002, 6, 3));
        holidays.add(date(2002, 6, 4));
      } else if (year == 2012) {
        // diamond jubilee
        holidays.add(date(2012, 6, 4));
        holidays.add(date(2012, 6, 5));
      } else if (year == 2022) {
        // platinum jubilee
        holidays.add(date(2022, 6, 2));
        holidays.add(date(2022, 6, 3));
      } else if (year == 1967 || year == 1970) {
        holidays.add(first(year, 5).with(lastInMonth(MONDAY)));
      } else if (year < 1971) {
        // whitsun
        holidays.add(easter(year).plusDays(50));
      } else {
        holidays.add(first(year, 5).with(lastInMonth(MONDAY)));
      }
      // summer
      if (year < 1965) {
        holidays.add(first(year, 8).with(firstInMonth(MONDAY)));
      } else if (year < 1971) {
        holidays.add(first(year, 8).with(lastInMonth(SATURDAY)).plusDays(2));
      } else {
        holidays.add(first(year, 8).with(lastInMonth(MONDAY)));
      }
      // queen's funeral
      if (year == 2022) {
        holidays.add(date(2022, 9, 19));
      }
      // christmas
      holidays.add(christmasBumpedSatSun(year));
      holidays.add(boxingDayBumpedSatSun(year));
    }
    holidays.add(date(1999, 12, 31));  // millennium
    holidays.add(date(2011, 4, 29));  // royal wedding
    holidays.add(date(2023, 5, 8));  // king's coronation
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.GBLO, holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate FRPA
  // data sources
  // http://www.legifrance.gouv.fr/affichCodeArticle.do?idArticle=LEGIARTI000006902611&cidTexte=LEGITEXT000006072050
  // http://jollyday.sourceforge.net/data/fr.html
  // Euronext holidays only New Year, Good Friday, Easter Monday, Labour Day, Christmas Day, Boxing Day
  // New Years Eve is holiday for cash markets and derivatives in 2015
  // https://www.euronext.com/en/holidays-and-hours
  // https://www.euronext.com/en/trading/nyse-euronext-trading-calendar/archives
  // some sources have Monday is holiday when Tuesday is, and Friday is holiday when Thursday is (not applying this)
  static ImmutableHolidayCalendar generateParis() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      holidays.add(date(year, 1, 1));  // new year
      holidays.add(easter(year).minusDays(2));  // good friday
      holidays.add(easter(year).plusDays(1));  // easter monday
      holidays.add(date(year, 5, 1));  // labour day
      holidays.add(date(year, 5, 8));  // victory in europe
      holidays.add(easter(year).plusDays(39));  // ascension day
      if (year <= 2004 || year >= 2008) {
        holidays.add(easter(year).plusDays(50));  // whit monday
      }
      holidays.add(date(year, 7, 14));  // bastille
      holidays.add(date(year, 8, 15));  // assumption of mary
      holidays.add(date(year, 11, 1));  // all saints
      holidays.add(date(year, 11, 11));  // armistice day
      holidays.add(date(year, 12, 25));  // christmas day
      holidays.add(date(year, 12, 26));  // saint stephen
    }
    holidays.add(date(1999, 12, 31));  // millennium
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.FRPA, holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate DEFR
  // data sources
  // https://www.feiertagskalender.ch/index.php?geo=3122&klasse=3&jahr=2017&hl=en
  // http://jollyday.sourceforge.net/data/de.html
  // http://en.boerse-frankfurt.de/basics-marketplaces-tradingcalendar2019
  static ImmutableHolidayCalendar generateFrankfurt() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      holidays.add(date(year, 1, 1));  // new year
      holidays.add(easter(year).minusDays(2));  // good friday
      holidays.add(easter(year).plusDays(1));  // easter monday
      holidays.add(date(year, 5, 1));  // labour day
      holidays.add(easter(year).plusDays(39));  // ascension day
      holidays.add(easter(year).plusDays(50));  // whit monday
      holidays.add(easter(year).plusDays(60));  // corpus christi
      if (year >= 2000) {
        holidays.add(date(year, 10, 3));  // german unity
      }
      if (year <= 1994) {
        // Wed before the Sunday that is 2 weeks before first advent, which is 4th Sunday before Christmas
        holidays.add(date(year, 12, 25).with(previous(SUNDAY)).minusWeeks(6).minusDays(4));  // repentance
      }
      holidays.add(date(year, 12, 24));  // christmas eve
      holidays.add(date(year, 12, 25));  // christmas day
      holidays.add(date(year, 12, 26));  // saint stephen
      holidays.add(date(year, 12, 31));  // new year
    }
    holidays.add(date(2017, 10, 31));  // reformation day
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.DEFR, holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate CHZU
  // data sources
  // http://jollyday.sourceforge.net/data/ch.html
  // https://github.com/lballabio/quantlib/blob/master/QuantLib/ql/time/calendars/switzerland.cpp
  // http://www.six-swiss-exchange.com/funds/trading/trading_and_settlement_calendar_en.html
  // http://www.six-swiss-exchange.com/swx_messages/online/swx7299e.pdf
  static ImmutableHolidayCalendar generateZurich() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      holidays.add(date(year, 1, 1));  // new year
      holidays.add(date(year, 1, 2));  // saint berchtoldstag
      holidays.add(easter(year).minusDays(2));  // good friday
      holidays.add(easter(year).plusDays(1));  // easter monday
      holidays.add(date(year, 5, 1));  // labour day
      holidays.add(easter(year).plusDays(39));  // ascension day
      holidays.add(easter(year).plusDays(50));  // whit monday
      holidays.add(date(year, 8, 1));  // national day
      holidays.add(date(year, 12, 25));  // christmas day
      holidays.add(date(year, 12, 26));  // saint stephen
    }
    holidays.add(date(1999, 12, 31));  // millennium
    holidays.add(date(2000, 1, 3));  // millennium
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.CHZU, holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate EUTA
  // 1997 - 1998 (testing phase), Jan 1, christmas day
  // https://www.ecb.europa.eu/pub/pdf/other/tagien.pdf
  // in 1999, Jan 1, christmas day, Dec 26, Dec 31
  // http://www.ecb.europa.eu/press/pr/date/1999/html/pr990715_1.en.html
  // http://www.ecb.europa.eu/press/pr/date/1999/html/pr990331.en.html
  // in 2000, Jan 1, good friday, easter monday, May 1, christmas day, Dec 26
  // http://www.ecb.europa.eu/press/pr/date/1999/html/pr990715_1.en.html
  // in 2001, Jan 1, good friday, easter monday, May 1, christmas day, Dec 26, Dec 31
  // http://www.ecb.europa.eu/press/pr/date/2000/html/pr000525_2.en.html
  // from 2002, Jan 1, good friday, easter monday, May 1, christmas day, Dec 26
  // http://www.ecb.europa.eu/press/pr/date/2000/html/pr001214_4.en.html
  static ImmutableHolidayCalendar generateEuropeanTarget() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1997; year <= 2099; year++) {
      if (year >= 2000) {
        holidays.add(date(year, 1, 1));
        holidays.add(easter(year).minusDays(2));
        holidays.add(easter(year).plusDays(1));
        holidays.add(date(year, 5, 1));
        holidays.add(date(year, 12, 25));
        holidays.add(date(year, 12, 26));
      } else {  // 1997 to 1999
        holidays.add(date(year, 1, 1));
        holidays.add(date(year, 12, 25));
      }
      if (year == 1999 || year == 2001) {
        holidays.add(date(year, 12, 31));
      }
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.EUTA, holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // common US holidays
  private static void usCommon(
      List<LocalDate> holidays, int year, boolean bumpBack, boolean columbusVeteran, int mlkStartYear) {
    // new year, adjusted if Sunday
    holidays.add(bumpSunToMon(date(year, 1, 1)));
    // martin luther king
    if (year >= mlkStartYear) {
      holidays.add(date(year, 1, 1).with(dayOfWeekInMonth(3, MONDAY)));
    }
    // washington
    if (year < 1971) {
      holidays.add(bumpSunToMon(date(year, 2, 22)));
    } else {
      holidays.add(date(year, 2, 1).with(dayOfWeekInMonth(3, MONDAY)));
    }
    // memorial
    if (year < 1971) {
      holidays.add(bumpSunToMon(date(year, 5, 30)));
    } else {
      holidays.add(date(year, 5, 1).with(lastInMonth(MONDAY)));
    }
    // juneteenth (seems like it wasn't widely applied in 2021)
    if (year >= 2022) {
      holidays.add(bumpToFriOrMon(date(year, 6, 19)));
    }
    // labor day
    holidays.add(date(year, 9, 1).with(firstInMonth(MONDAY)));
    // columbus day
    if (columbusVeteran) {
      if (year < 1971) {
        holidays.add(bumpSunToMon(date(year, 10, 12)));
      } else {
        holidays.add(date(year, 10, 1).with(dayOfWeekInMonth(2, MONDAY)));
      }
    }
    // veterans day
    if (columbusVeteran) {
      if (year >= 1971 && year < 1978) {
        holidays.add(date(year, 10, 1).with(dayOfWeekInMonth(4, MONDAY)));
      } else {
        holidays.add(bumpSunToMon(date(year, 11, 11)));
      }
    }
    // thanksgiving
    holidays.add(date(year, 11, 1).with(dayOfWeekInMonth(4, THURSDAY)));
    // independence day & christmas day
    if (bumpBack) {
      holidays.add(bumpToFriOrMon(date(year, 7, 4)));
      holidays.add(bumpToFriOrMon(date(year, 12, 25)));
    } else {
      holidays.add(bumpSunToMon(date(year, 7, 4)));
      holidays.add(bumpSunToMon(date(year, 12, 25)));
    }
  }

  // generate USGS
  // http://www.sifma.org/services/holiday-schedule/
  static ImmutableHolidayCalendar generateUsGovtSecurities() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      usCommon(holidays, year, true, true, 1986);
      // good friday, in 1999/2007 only a partial holiday
      holidays.add(easter(year).minusDays(2));
      // hurricane sandy
      if (year == 2012) {
        holidays.add(date(year, 10, 30));
      }
    }
    holidays.add(date(2018, 12, 5));  // Death of George H.W. Bush
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.USGS, holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate USNY
  // http://www.cs.ny.gov/attendance_leave/2012_legal_holidays.cfm
  // http://www.cs.ny.gov/attendance_leave/2013_legal_holidays.cfm
  // etc
  // ignore election day and lincoln day
  static ImmutableHolidayCalendar generateUsNewYork() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      usCommon(holidays, year, false, true, 1986);
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.USNY, holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate NYFD
  // http://www.ny.frb.org/aboutthefed/holiday_schedule.html
  static ImmutableHolidayCalendar generateNewYorkFed() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      usCommon(holidays, year, false, true, 1986);
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.NYFD, holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate NYSE
  // https://www.nyse.com/markets/hours-calendars
  // http://www1.nyse.com/pdfs/closings.pdf
  static ImmutableHolidayCalendar generateNewYorkStockExchange() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      usCommon(holidays, year, true, false, 1998);
      // good friday
      holidays.add(easter(year).minusDays(2));
    }
    // Lincoln day 1896-1953
    // Columbus day 1909-1953
    // Veterans day 1934-1953
    for (int i = 1950; i <= 1953; i++) {
      holidays.add(date(i, 2, 12));
      holidays.add(date(i, 10, 12));
      holidays.add(date(i, 11, 11));
    }
    // election day, Tue after first Monday of November
    for (int i = 1950; i <= 1968; i++) {
      holidays.add(date(i, 11, 1).with(TemporalAdjusters.nextOrSame(MONDAY)).plusDays(1));
    }
    holidays.add(date(1972, 11, 7));
    holidays.add(date(1976, 11, 2));
    holidays.add(date(1980, 11, 4));
    // special days
    holidays.add(date(1955, 12, 24));  // Christmas Eve
    holidays.add(date(1956, 12, 24));  // Christmas Eve
    holidays.add(date(1958, 12, 26));  // Day after Christmas
    holidays.add(date(1961, 5, 29));  // Decoration day
    holidays.add(date(1963, 11, 25));  // Death of John F Kennedy
    holidays.add(date(1965, 12, 24));  // Christmas Eve
    holidays.add(date(1968, 2, 12));  // Lincoln birthday
    holidays.add(date(1968, 4, 9));  // Death of Martin Luther King
    holidays.add(date(1968, 6, 12));  // Paperwork crisis
    holidays.add(date(1968, 6, 19));  // Paperwork crisis
    holidays.add(date(1968, 6, 26));  // Paperwork crisis
    holidays.add(date(1968, 7, 3));  // Paperwork crisis
    holidays.add(date(1968, 7, 5));  // Day after independence
    holidays.add(date(1968, 7, 10));  // Paperwork crisis
    holidays.add(date(1968, 7, 17));  // Paperwork crisis
    holidays.add(date(1968, 7, 24));  // Paperwork crisis
    holidays.add(date(1968, 7, 31));  // Paperwork crisis
    holidays.add(date(1968, 8, 7));  // Paperwork crisis
    holidays.add(date(1968, 8, 13));  // Paperwork crisis
    holidays.add(date(1968, 8, 21));  // Paperwork crisis
    holidays.add(date(1968, 8, 28));  // Paperwork crisis
    holidays.add(date(1968, 9, 4));  // Paperwork crisis
    holidays.add(date(1968, 9, 11));  // Paperwork crisis
    holidays.add(date(1968, 9, 18));  // Paperwork crisis
    holidays.add(date(1968, 9, 25));  // Paperwork crisis
    holidays.add(date(1968, 10, 2));  // Paperwork crisis
    holidays.add(date(1968, 10, 9));  // Paperwork crisis
    holidays.add(date(1968, 10, 16));  // Paperwork crisis
    holidays.add(date(1968, 10, 23));  // Paperwork crisis
    holidays.add(date(1968, 10, 30));  // Paperwork crisis
    holidays.add(date(1968, 11, 6));  // Paperwork crisis
    holidays.add(date(1968, 11, 13));  // Paperwork crisis
    holidays.add(date(1968, 11, 20));  // Paperwork crisis
    holidays.add(date(1968, 11, 27));  // Paperwork crisis
    holidays.add(date(1968, 12, 4));  // Paperwork crisis
    holidays.add(date(1968, 12, 11));  // Paperwork crisis
    holidays.add(date(1968, 12, 18));  // Paperwork crisis
    holidays.add(date(1968, 12, 25));  // Paperwork crisis
    holidays.add(date(1968, 12, 31));  // Paperwork crisis
    holidays.add(date(1969, 2, 10));  // Snow
    holidays.add(date(1969, 3, 31));  // Death of Dwight Eisenhower
    holidays.add(date(1969, 7, 21));  // Lunar exploration
    holidays.add(date(1972, 12, 28));  // Death of Harry Truman
    holidays.add(date(1973, 1, 25));  // Death of Lyndon Johnson
    holidays.add(date(1977, 7, 14));  // Blackout
    holidays.add(date(1985, 9, 27));  // Hurricane Gloria
    holidays.add(date(1994, 4, 27));  // Death of Richard Nixon
    holidays.add(date(2001, 9, 11));  // 9/11 attack
    holidays.add(date(2001, 9, 12));  // 9/11 attack
    holidays.add(date(2001, 9, 13));  // 9/11 attack
    holidays.add(date(2001, 9, 14));  // 9/11 attack
    holidays.add(date(2004, 6, 11));  // Death of Ronald Reagan
    holidays.add(date(2007, 1, 2));  // Death of Gerald Ford
    holidays.add(date(2012, 10, 30));  // Hurricane Sandy
    holidays.add(date(2018, 12, 5));  // Death of George H.W. Bush
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.NYSE, holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate JPTO
  // data sources
  // https://www.boj.or.jp/en/about/outline/holi.htm/
  // http://web.archive.org/web/20110513190217/http://www.boj.or.jp/en/about/outline/holi.htm/
  // http://web.archive.org/web/20130502031733/http://www.boj.or.jp/en/about/outline/holi.htm
  // http://www8.cao.go.jp/chosei/shukujitsu/gaiyou.html (law)
  // http://www.nao.ac.jp/faq/a0301.html (equinox)
  // http://eco.mtk.nao.ac.jp/koyomi/faq/holiday.html.en
  // https://www.jpx.co.jp/english/announce/market-holidays.html
  // https://www.loc.gov/law/foreign-news/article/japan-three-holidays-to-be-moved-to-ease-2020-olympic-ceremony-traffic/
  // https://www.nippon.com/en/japan-data/h00738/
  static ImmutableHolidayCalendar generateTokyo() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(date(year, 1, 1));
      holidays.add(date(year, 1, 2));
      holidays.add(date(year, 1, 3));
      // coming of age
      if (year >= 2000) {
        holidays.add(date(year, 1, 1).with(dayOfWeekInMonth(2, MONDAY)));
      } else {
        holidays.add(bumpSunToMon(date(year, 1, 15)));
      }
      // national foundation
      if (year >= 1967) {
        holidays.add(bumpSunToMon(date(year, 2, 11)));
      }
      // vernal equinox (from 1948), 20th or 21st (predictions/facts 2000 to 2030)
      if (year == 2000 || year == 2001 || year == 2004 || year == 2005 || year == 2008 || year == 2009 ||
          year == 2012 || year == 2013 || year == 2016 || year == 2017 ||
          year == 2020 || year == 2021 || year == 2024 || year == 2025 || year == 2026 || year == 2028 ||
          year == 2029 || year == 2030) {
        holidays.add(bumpSunToMon(date(year, 3, 20)));
      } else {
        holidays.add(bumpSunToMon(date(year, 3, 21)));
      }
      // showa (from 2007 onwards), greenery (from 1989 to 2006), emperor (before 1989)
      // http://news.bbc.co.uk/1/hi/world/asia-pacific/4543461.stm
      holidays.add(bumpSunToMon(date(year, 4, 29)));
      // constitution (from 1948)
      // greenery (from 2007 onwards), holiday between two other holidays before that (from 1985)
      // children (from 1948)
      if (year >= 1985) {
        holidays.add(bumpSunToMon(date(year, 5, 3)));
        holidays.add(bumpSunToMon(date(year, 5, 4)));
        holidays.add(bumpSunToMon(date(year, 5, 5)));
        if (year >= 2007 && (date(year, 5, 3).getDayOfWeek() == SUNDAY || date(year, 5, 4).getDayOfWeek() == SUNDAY)) {
          holidays.add(date(year, 5, 6));
        }
      } else {
        holidays.add(bumpSunToMon(date(year, 5, 3)));
        holidays.add(bumpSunToMon(date(year, 5, 5)));
      }
      // marine
      if (year == 2021) {
        // moved because of the Olympics
        holidays.add(date(year, 7, 22));
      } else if (year == 2020) {
        // moved because of the Olympics (day prior to opening ceremony)
        holidays.add(date(year, 7, 23));
      } else if (year >= 2003) {
        holidays.add(date(year, 7, 1).with(dayOfWeekInMonth(3, MONDAY)));
      } else if (year >= 1996) {
        holidays.add(bumpSunToMon(date(year, 7, 20)));
      }
      // mountain
      if (year == 2021) {
        // moved because of the Olympics
        holidays.add(date(year, 8, 9));
      } else if (year == 2020) {
        // moved because of the Olympics (day after closing ceremony)
        holidays.add(date(year, 8, 10));
      } else if (year >= 2016) {
        holidays.add(bumpSunToMon(date(year, 8, 11)));
      }
      // aged
      if (year >= 2003) {
        holidays.add(date(year, 9, 1).with(dayOfWeekInMonth(3, MONDAY)));
      } else if (year >= 1966) {
        holidays.add(bumpSunToMon(date(year, 9, 15)));
      }
      // autumn equinox (from 1948), 22nd or 23rd (predictions/facts 2000 to 2030)
      if (year == 2012 || year == 2016 || year == 2020 || year == 2024 || year == 2028) {
        holidays.add(bumpSunToMon(date(year, 9, 22)));
      } else {
        holidays.add(bumpSunToMon(date(year, 9, 23)));
      }
      citizensDay(holidays, date(year, 9, 20), date(year, 9, 22));
      citizensDay(holidays, date(year, 9, 21), date(year, 9, 23));
      // health-sports
      if (year == 2021) {
        // moved because of the Olympics
        holidays.add(date(year, 7, 23));
      } else if (year == 2020) {
        // moved because of the Olympics (day of opening ceremony)
        holidays.add(date(year, 7, 24));
      } else if (year >= 2000) {
        holidays.add(date(year, 10, 1).with(dayOfWeekInMonth(2, MONDAY)));
      } else if (year >= 1966) {
        holidays.add(bumpSunToMon(date(year, 10, 10)));
      }
      // culture (from 1948)
      holidays.add(bumpSunToMon(date(year, 11, 3)));
      // labor (from 1948)
      holidays.add(bumpSunToMon(date(year, 11, 23)));
      // emperor (current emporer birthday)
      if (year >= 1990 && year < 2019) {
        holidays.add(bumpSunToMon(date(year, 12, 23)));
      } else if (year >= 2020) {
        holidays.add(bumpSunToMon(date(year, 2, 23)));
      }
      // new years eve - bank of Japan, but not national holiday
      holidays.add(bumpSunToMon(date(year, 12, 31)));
    }
    holidays.add(date(1959, 4, 10));  // marriage akihito
    holidays.add(date(1989, 2, 24));  // funeral showa
    holidays.add(date(1990, 11, 12));  // enthrone akihito
    holidays.add(date(1993, 6, 9));  // marriage naruhito
    holidays.add(date(2019, 4, 30));  // abdication
    holidays.add(date(2019, 5, 1));  // accession
    holidays.add(date(2019, 5, 2));  // accession
    holidays.add(date(2019, 10, 22));  // enthronement
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.JPTO, holidays, SATURDAY, SUNDAY);
  }

  // extra day between two other holidays, appears to exclude weekends
  private static void citizensDay(List<LocalDate> holidays, LocalDate date1, LocalDate date2) {
    if (holidays.contains(date1) && holidays.contains(date2)) {
      if (date1.getDayOfWeek() == MONDAY || date1.getDayOfWeek() == TUESDAY || date1.getDayOfWeek() == WEDNESDAY) {
        holidays.add(date1.plusDays(1));
      }
    }
  }

  //-------------------------------------------------------------------------
  // generate CAMO
  // data sources
  // https://www.cnesst.gouv.qc.ca/en/working-conditions/leave/statutory-holidays/list-paid-statutory-holidays
  // https://www.canada.ca/en/revenue-agency/services/tax/public-holidays.html
  static ImmutableHolidayCalendar generateMontreal() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(bumpToMon(date(year, 1, 1)));
      // good friday
      holidays.add(easter(year).minusDays(2));
      // patriots
      holidays.add(date(year, 5, 25).with(TemporalAdjusters.previous(MONDAY)));
      // fete nationale quebec
      holidays.add(bumpToMon(date(year, 6, 24)));
      // canada
      holidays.add(bumpToMon(date(year, 7, 1)));
      // labour
      holidays.add(first(year, 9).with(dayOfWeekInMonth(1, MONDAY)));
      // thanksgiving
      holidays.add(first(year, 10).with(dayOfWeekInMonth(2, MONDAY)));
      // christmas
      holidays.add(bumpToMon(date(year, 12, 25)));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("CAMO"), holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate CATO
  // data sources
  // http://www.labour.gov.on.ca/english/es/pubs/guide/publicholidays.php
  // http://www.cra-arc.gc.ca/tx/hldys/menu-eng.html
  // http://www.tmxmoney.com/en/investor_tools/market_hours.html
  // http://www.statutoryholidayscanada.com/
  // http://www.osc.gov.on.ca/en/SecuritiesLaw_csa_20151209_13-315_sra-closed-dates.htm
  static ImmutableHolidayCalendar generateToronto() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year (public)
      holidays.add(bumpToMon(date(year, 1, 1)));
      // family (public)
      if (year >= 2008) {
        holidays.add(first(year, 2).with(dayOfWeekInMonth(3, MONDAY)));
      }
      // good friday (public)
      holidays.add(easter(year).minusDays(2));
      // victoria (public)
      holidays.add(date(year, 5, 25).with(TemporalAdjusters.previous(MONDAY)));
      // canada (public)
      holidays.add(bumpToMon(date(year, 7, 1)));
      // civic
      holidays.add(first(year, 8).with(dayOfWeekInMonth(1, MONDAY)));
      // labour (public)
      holidays.add(first(year, 9).with(dayOfWeekInMonth(1, MONDAY)));
      // thanksgiving (public)
      holidays.add(first(year, 10).with(dayOfWeekInMonth(2, MONDAY)));
      // remembrance
      holidays.add(bumpToMon(date(year, 11, 11)));
      // christmas (public)
      holidays.add(christmasBumpedSatSun(year));
      // boxing (public)
      holidays.add(boxingDayBumpedSatSun(year));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("CATO"), holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate DKCO
  // data sources
  // http://www.finansraadet.dk/Bankkunde/Pages/bankhelligdage.aspx
  // web archive history of those pages
  static ImmutableHolidayCalendar generateCopenhagen() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(date(year, 1, 1));
      // maundy thursday
      holidays.add(easter(year).minusDays(3));
      // good friday
      holidays.add(easter(year).minusDays(2));
      // easter monday
      holidays.add(easter(year).plusDays(1));
      // prayer day (Friday)
      holidays.add(easter(year).plusDays(26));
      // ascension (Thursday)
      holidays.add(easter(year).plusDays(39));
      // ascension + 1 (Friday)
      holidays.add(easter(year).plusDays(40));
      // whit monday
      holidays.add(easter(year).plusDays(50));
      // constitution
      holidays.add(date(year, 6, 5));
      // christmas eve
      holidays.add(date(year, 12, 24));
      // christmas
      holidays.add(date(year, 12, 25));
      // boxing
      holidays.add(date(year, 12, 26));
      // new years eve
      holidays.add(date(year, 12, 31));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("DKCO"), holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate NOOS
  // data sources
  // http://www.oslobors.no/ob_eng/Oslo-Boers/About-Oslo-Boers/Opening-hours
  // http://www.oslobors.no/Oslo-Boers/Om-Oslo-Boers/AApningstider
  // web archive history of those pages
  static ImmutableHolidayCalendar generateOslo() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(date(year, 1, 1));
      // maundy thursday
      holidays.add(easter(year).minusDays(3));
      // good friday
      holidays.add(easter(year).minusDays(2));
      // easter monday
      holidays.add(easter(year).plusDays(1));
      // labour
      holidays.add(date(year, 5, 1));
      // constitution
      holidays.add(date(year, 5, 17));
      // ascension
      holidays.add(easter(year).plusDays(39));
      // whit monday
      holidays.add(easter(year).plusDays(50));
      // christmas eve
      holidays.add(date(year, 12, 24));
      // christmas
      holidays.add(date(year, 12, 25));
      // boxing
      holidays.add(date(year, 12, 26));
      // new years eve
      holidays.add(date(year, 12, 31));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("NOOS"), holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate NZAU
  // https://www.nzfma.org/Site/practices_standards/market_conventions.aspx
  static ImmutableHolidayCalendar generateAuckland() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      newZealand(holidays, year);
      // auckland anniversary day
      holidays.add(date(year, 1, 29).minusDays(3).with(nextOrSame(MONDAY)));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("NZAU"), holidays, SATURDAY, SUNDAY);
  }

  // generate NZWE
  // https://www.nzfma.org/Site/practices_standards/market_conventions.aspx
  static ImmutableHolidayCalendar generateWellington() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      newZealand(holidays, year);
      // wellington anniversary day
      holidays.add(date(year, 1, 22).minusDays(3).with(nextOrSame(MONDAY)));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("NZWE"), holidays, SATURDAY, SUNDAY);
  }

  // generate NZBD
  // https://www.nzfma.org/Site/practices_standards/market_conventions.aspx
  static ImmutableHolidayCalendar generateNewZealand() {
    // artificial non-ISDA definition named after BRBD for Brazil
    // this is needed as NZD-BBR index is published on both Wellington and Auckland anniversary days
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      newZealand(holidays, year);
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("NZBD"), holidays, SATURDAY, SUNDAY);
  }

  private static void newZealand(List<LocalDate> holidays, int year) {
    // new year and day after
    LocalDate newYear = bumpToMon(date(year, 1, 1));
    holidays.add(newYear);
    holidays.add(bumpToMon(newYear.plusDays(1)));
    // waitangi day
    // https://www.employment.govt.nz/leave-and-holidays/public-holidays/public-holidays-and-anniversary-dates/
    if (year >= 2014) {
      holidays.add(bumpToMon(date(year, 2, 6)));
    } else {
      holidays.add(date(year, 2, 6));
    }
    // good friday
    holidays.add(easter(year).minusDays(2));
    // easter monday
    holidays.add(easter(year).plusDays(1));
    // anzac day
    // https://www.employment.govt.nz/leave-and-holidays/public-holidays/public-holidays-and-anniversary-dates/
    if (year >= 2014) {
      holidays.add(bumpToMon(date(year, 4, 25)));
    } else {
      holidays.add(date(year, 4, 25));
    }
    // queen's birthday
    holidays.add(first(year, 6).with(firstInMonth(MONDAY)));
    // queen's funeral
    if (year == 2022) {
      holidays.add(date(year, 9, 26));
    }
    // labour day
    holidays.add(first(year, 10).with(dayOfWeekInMonth(4, MONDAY)));
    // christmas
    holidays.add(christmasBumpedSatSun(year));
    holidays.add(boxingDayBumpedSatSun(year));
  }

  //-------------------------------------------------------------------------
  // generate PLWA
  // data sources#
  // http://isap.sejm.gov.pl/DetailsServlet?id=WDU19510040028 and linked pages
  // https://www.gpw.pl/dni_bez_sesji_en
  // http://jollyday.sourceforge.net/data/pl.html
  // https://www.gpw.pl/session-details
  // https://www.gpw.pl/news?cmn_id=107609&title=No+exchange+trading+session+on+12+November+2018
  // https://www.gpw.pl/news?cmn_id=107794&title=December+24%2C+2018+-+Closing+day
  static ImmutableHolidayCalendar generateWarsaw() {
    // holiday law dates from 1951, but don't know situation before then, so ignore 1951 date
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(date(year, 1, 1));
      // epiphany
      if (year < 1961 || year >= 2011) {
        holidays.add(date(year, 1, 6));
      }
      // easter monday
      holidays.add(easter(year).plusDays(1));
      // state
      holidays.add(date(year, 5, 1));
      // constitution
      if (year >= 1990) {
        holidays.add(date(year, 5, 3));
      }
      // rebirth/national
      if (year < 1990) {
        holidays.add(date(year, 7, 22));
      }
      // corpus christi
      holidays.add(easter(year).plusDays(60));
      // assumption
      if (year < 1961 || year >= 1989) {
        holidays.add(date(year, 8, 15));
      }
      // all saints
      holidays.add(date(year, 11, 1));
      // independence
      if (year >= 1990) {
        holidays.add(date(year, 11, 11));
      }
      // christmas (exchange)
      holidays.add(date(year, 12, 24));
      // christmas
      holidays.add(date(year, 12, 25));
      // boxing
      holidays.add(date(year, 12, 26));
      // new years eve (exchange, rule based on sample data)
      LocalDate nyeve = date(year, 12, 31);
      if (nyeve.getDayOfWeek() == MONDAY || nyeve.getDayOfWeek() == THURSDAY || nyeve.getDayOfWeek() == FRIDAY) {
        holidays.add(nyeve);
      }
    }
    // 100th independence day anniversary
    holidays.add(date(2018, 11, 12));

    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("PLWA"), holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate SEST
  // data sources - history of dates that STIBOR fixing occurred
  // http://www.riksbank.se/en/Interest-and-exchange-rates/search-interest-rates-exchange-rates/?g5-SEDP1MSTIBOR=on&from=2016-01-01&to=2016-10-05&f=Day&cAverage=Average&s=Comma#search
  static ImmutableHolidayCalendar generateStockholm() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(date(year, 1, 1));
      // epiphany
      holidays.add(date(year, 1, 6));
      // good friday
      holidays.add(easter(year).minusDays(2));
      // easter monday
      holidays.add(easter(year).plusDays(1));
      // labour
      holidays.add(date(year, 5, 1));
      // ascension
      holidays.add(easter(year).plusDays(39));
      // midsummer friday
      holidays.add(date(year, 6, 19).with(nextOrSame(FRIDAY)));
      // national
      if (year > 2005) {
        holidays.add(date(year, 6, 6));
      }
      // christmas
      holidays.add(date(year, 12, 24));
      // christmas
      holidays.add(date(year, 12, 25));
      // boxing
      holidays.add(date(year, 12, 26));
      // new years eve (fixings, rule based on sample data)
      holidays.add(date(year, 12, 31));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("SEST"), holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // http://www.rba.gov.au/schedules-events/bank-holidays/bank-holidays-2016.html
  // http://www.rba.gov.au/schedules-events/bank-holidays/bank-holidays-2017.html
  // web archive history of those pages
  static ImmutableHolidayCalendar generateSydney() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(bumpToMon(date(year, 1, 1)));
      // australia day
      holidays.add(bumpToMon(date(year, 1, 26)));
      // good friday
      holidays.add(easter(year).minusDays(2));
      // easter monday
      holidays.add(easter(year).plusDays(1));
      // anzac day
      holidays.add(date(year, 4, 25));
      // queen's birthday
      holidays.add(first(year, 6).with(dayOfWeekInMonth(2, MONDAY)));
      // bank holiday
      holidays.add(first(year, 8).with(dayOfWeekInMonth(1, MONDAY)));
      // queen's funeral
      if (year == 2022) {
        holidays.add(date(year, 9, 22));
      }
      // labour day
      holidays.add(first(year, 10).with(dayOfWeekInMonth(1, MONDAY)));
      // christmas
      holidays.add(christmasBumpedSatSun(year));
      // boxing
      holidays.add(boxingDayBumpedSatSun(year));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("AUSY"), holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // http://www.gov.za/about-sa/public-holidays
  // http://www.gov.za/sites/www.gov.za/files/Act36of1994.pdf
  // http://www.gov.za/sites/www.gov.za/files/Act48of1995.pdf
  // 27th Dec when Tue http://www.gov.za/sites/www.gov.za/files/34881_proc72.pdf
  static ImmutableHolidayCalendar generateJohannesburg() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // from 1995 (act of 7 Dec 1994)
      // older act from 1952 not implemented here
      // new year
      holidays.add(bumpSunToMon(date(year, 1, 1)));
      // human rights day
      holidays.add(bumpSunToMon(date(year, 3, 21)));
      // good friday
      holidays.add(easter(year).minusDays(2));
      // family day (easter monday)
      holidays.add(easter(year).plusDays(1));
      // freedom day
      holidays.add(bumpSunToMon(date(year, 4, 27)));
      // workers day
      holidays.add(bumpSunToMon(date(year, 5, 1)));
      // youth day
      holidays.add(bumpSunToMon(date(year, 6, 16)));
      // womens day
      holidays.add(bumpSunToMon(date(year, 8, 9)));
      // heritage day
      holidays.add(bumpSunToMon(date(year, 9, 24)));
      // reconcilliation
      holidays.add(bumpSunToMon(date(year, 12, 16)));
      // christmas
      holidays.add(christmasBumpedSun(year));
      // goodwill
      holidays.add(boxingDayBumpedSun(year));
    }
    // mostly election days
    // http://www.gov.za/sites/www.gov.za/files/40125_proc%2045.pdf
    holidays.add(date(2016, 8, 3));
    // http://www.gov.za/sites/www.gov.za/files/37376_proc13.pdf
    holidays.add(date(2014, 5, 7));
    // http://www.gov.za/sites/www.gov.za/files/34127_proc27.pdf
    holidays.add(date(2011, 5, 18));
    // http://www.gov.za/sites/www.gov.za/files/32039_17.pdf
    holidays.add(date(2009, 4, 22));
    // http://www.gov.za/sites/www.gov.za/files/30900_7.pdf (moved human rights day)
    holidays.add(date(2008, 5, 2));
    // http://www.gov.za/sites/www.gov.za/files/28442_0.pdf
    holidays.add(date(2006, 3, 1));
    // http://www.gov.za/sites/www.gov.za/files/26075.pdf
    holidays.add(date(2004, 4, 14));
    // http://www.gov.za/sites/www.gov.za/files/20032_0.pdf
    holidays.add(date(1999, 12, 31));
    holidays.add(date(2000, 1, 1));
    holidays.add(date(2000, 1, 2));
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("ZAJO"), holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // http://www.magyarkozlony.hu/dokumentumok/b0d596a3e6ce15a2350a9e138c058a78dd8622d0/megtekintes (article 148)
  // http://www.mfa.gov.hu/NR/rdonlyres/18C1949E-D740-45E0-923A-BDFC81EC44C8/0/ListofHolidays2016.pdf
  // http://jollyday.sourceforge.net/data/hu.html
  // https://englishhungary.wordpress.com/2012/01/15/bridge-days/
  // http://www.ucmsgroup.hu/newsletter/public-holiday-and-related-work-schedule-changes-in-2015/
  // http://www.ucmsgroup.hu/newsletter/public-holiday-and-related-work-schedule-changes-in-2014/
  // https://www.bse.hu/Products-and-Services/Trading-information/tranding-calendar-2019
  // https://www.bse.hu/Products-and-Services/Trading-information/trading-calendar-2020
  // https://www.bse.hu/Products-and-Services/Trading-information/trading-calendar-2021
  // https://www.bse.hu/Products-and-Services/Trading-information/trading-calendar-2022
  static ImmutableHolidayCalendar generateBudapest() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    Set<LocalDate> workDays = new HashSet<>(500);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      addDateWithHungarianBridging(date(year, 1, 1), -1, 1, holidays, workDays);
      // national day
      // in 2022 the working saturday was 2 weeks after, in 2021 it was 1 week after
      // logic is determined yearly by government decree
      int nationalDayTuesRelativeWeeks = year == 2022 ? 1 : -2;
      addDateWithHungarianBridging(date(year, 3, 15), nationalDayTuesRelativeWeeks, 1, holidays, workDays);
      if (year >= 2017) {
        // good friday
        holidays.add(easter(year).minusDays(2));
      }
      // easter monday
      holidays.add(easter(year).plusDays(1));
      // labour day
      addDateWithHungarianBridging(date(year, 5, 1), 0, 1, holidays, workDays);
      // pentecost monday
      holidays.add(easter(year).plusDays(50));
      // state foundation day
      // in 2015 the working saturday was 2 weeks before, in 2020 it was 1 week after
      // logic is determined yearly by government decree
      int foundationDayThuRelativeWeeks = year == 2020 ? 1 : -2;
      addDateWithHungarianBridging(date(year, 8, 20), 0 , foundationDayThuRelativeWeeks, holidays, workDays);
      // national day
      addDateWithHungarianBridging(date(year, 10, 23), 0, -1, holidays, workDays);
      // all saints day
      addDateWithHungarianBridging(date(year, 11, 1), -3, 1, holidays, workDays);
      // christmas
      holidays.add(date(year, 12, 24));
      holidays.add(date(year, 12, 25));
      holidays.add(date(year, 12, 26));
      if (date(year, 12, 25).getDayOfWeek() == TUESDAY) {
        holidays.add(date(year, 12, 24));
        workDays.add(date(year, 12, 15));
      } else if (date(year, 12, 25).getDayOfWeek() == WEDNESDAY) {
        holidays.add(date(year, 12, 24));
        holidays.add(date(year, 12, 27));
        workDays.add(date(year, 12, 7));
        workDays.add(date(year, 12, 21));
      } else if (date(year, 12, 25).getDayOfWeek() == THURSDAY) {
        holidays.add(date(year, 12, 24));
      } else if (date(year, 12, 25).getDayOfWeek() == FRIDAY) {
        holidays.add(date(year, 12, 24));
        workDays.add(date(year, 12, 12));
      }
    }
    // some Saturdays are work days
    addHungarianSaturdays(holidays, workDays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("HUBU"), holidays, SUNDAY, SUNDAY);
  }

  // an attempt to divine the official rules from the data available
  private static void addDateWithHungarianBridging(
      LocalDate date,
      int relativeWeeksTue,
      int relativeWeeksThu,
      List<LocalDate> holidays,
      Set<LocalDate> workDays) {

    DayOfWeek dow = date.getDayOfWeek();
    switch (dow) {
      case MONDAY:
      case WEDNESDAY:
      case FRIDAY:
        holidays.add(date);
        return;
      case TUESDAY:
        holidays.add(date.minusDays(1));
        holidays.add(date);
        workDays.add(date.plusDays(4).plusWeeks(relativeWeeksTue));  // a Saturday is now a workday
        return;
      case THURSDAY:
        holidays.add(date.plusDays(1));
        holidays.add(date);
        workDays.add(date.plusDays(2).plusWeeks(relativeWeeksThu));  // a Saturday is now a workday
        return;
      case SATURDAY:
      case SUNDAY:
      default:
        return;
    }
  }

  private static void addHungarianSaturdays(List<LocalDate> holidays, Set<LocalDate> workDays) {
    // remove all saturdays and sundays
    removeSatSun(holidays);
    // add all saturdays
    LocalDate endDate = LocalDate.of(2099, 12, 31);
    LocalDate date = LocalDate.of(1950, 1, 7);
    while (date.isBefore(endDate)) {
      if (!workDays.contains(date)) {
        holidays.add(date);
      }
      date = date.plusDays(7);
    }
  }

  //-------------------------------------------------------------------------
  // generate MXMC
  // dates of published fixings - https://twitter.com/Banxico
  // http://www.banxico.org.mx/SieInternet/consultarDirectorioInternetAction.do?accion=consultarCuadro&idCuadro=CF111&locale=en
  // http://www.gob.mx/cms/uploads/attachment/file/161094/calendario_vacaciones2016.pdf
  // https://comunicacionsocial.diputados.gob.mx/index.php/boletines/la-camara-de-diputados-declaro-el-1-de-octubre-de-cada-seis-a-os-como-dia-de-descanso-obligatorio
  static ImmutableHolidayCalendar generateMexicoCity() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(date(year, 1, 1));
      // constitution
      holidays.add(first(year, 2).with(firstInMonth(MONDAY)));
      // president
      holidays.add(first(year, 3).with(firstInMonth(MONDAY)).plusWeeks(2));
      // maundy thursday
      holidays.add(easter(year).minusDays(3));
      // good friday
      holidays.add(easter(year).minusDays(2));
      // labour
      holidays.add(date(year, 5, 1));
      // independence
      holidays.add(date(year, 9, 16));
      // inaguration day - occurring once in every 6 years (2024, 2030, etc).
      if (year >= 2024 && (year + 4) % 6 == 0) {
        holidays.add(date(year, 10, 1));
      }
      // dead
      holidays.add(date(year, 11, 2));
      // revolution
      holidays.add(first(year, 11).with(firstInMonth(MONDAY)).plusWeeks(2));
      // guadalupe
      holidays.add(date(year, 12, 12));
      // christmas
      holidays.add(date(year, 12, 25));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarIds.MXMC, holidays, SATURDAY, SUNDAY);
  }

  // generate BRBD
  // a holiday in this calendar is only declared if there is a holiday in Sao Paulo, Rio de Janeiro and Brasilia
  // http://www.planalto.gov.br/ccivil_03/leis/l0662.htm
  // http://www.planalto.gov.br/ccivil_03/Leis/L6802.htm
  // http://www.planalto.gov.br/ccivil_03/leis/2002/L10607.htm
  static ImmutableHolidayCalendar generateBrazil() {
    // base law is from 1949, reworded in 2002
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(date(year, 1, 1));
      // carnival
      holidays.add(easter(year).minusDays(48));
      holidays.add(easter(year).minusDays(47));
      // tiradentes
      holidays.add(date(year, 4, 21));
      // good friday
      holidays.add(easter(year).minusDays(2));
      // labour
      holidays.add(date(year, 5, 1));
      // corpus christi
      holidays.add(easter(year).plusDays(60));
      // independence
      holidays.add(date(year, 9, 7));
      // aparedica
      if (year >= 1980) {
        holidays.add(date(year, 10, 12));
      }
      // dead
      holidays.add(date(year, 11, 2));
      // republic
      holidays.add(date(year, 11, 15));
      // christmas
      holidays.add(date(year, 12, 25));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("BRBD"), holidays, SATURDAY, SUNDAY);
  }

  // generate CZPR
  // https://www.cnb.cz/en/public/media_service/schedules/media_svatky.html
  static ImmutableHolidayCalendar generatePrague() {
    // dates are fixed - no moving Sunday to Monday or similar
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      // new year
      holidays.add(date(year, 1, 1));
      // good friday
      if (year > 2015) {
        holidays.add(easter(year).minusDays(2));
      }
      // easter monday
      holidays.add(easter(year).plusDays(1));
      // may day
      holidays.add(date(year, 5, 1));
      // liberation from fascism
      holidays.add(date(year, 5, 8));
      // cyril and methodius
      holidays.add(date(year, 7, 5));
      // jan hus
      holidays.add(date(year, 7, 6));
      // statehood
      holidays.add(date(year, 9, 28));
      // republic
      holidays.add(date(year, 10, 28));
      // freedom and democracy
      holidays.add(date(year, 11, 17));
      // christmas eve
      holidays.add(date(year, 12, 24));
      // christmas
      holidays.add(date(year, 12, 25));
      // boxing
      holidays.add(date(year, 12, 26));
    }
    removeSatSun(holidays);
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of("CZPR"), holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // date
  private static LocalDate date(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

  // bump to following Monday
  private static LocalDate bumpToMon(LocalDate date) {
    if (date.getDayOfWeek() == SATURDAY) {
      return date.plusDays(2);
    } else if (date.getDayOfWeek() == SUNDAY) {
      return date.plusDays(1);
    }
    return date;
  }

  // bump Sunday to following Monday
  private static LocalDate bumpSunToMon(LocalDate date) {
    if (date.getDayOfWeek() == SUNDAY) {
      return date.plusDays(1);
    }
    return date;
  }

  // bump to Saturday to Friday and Sunday to Monday
  private static LocalDate bumpToFriOrMon(LocalDate date) {
    if (date.getDayOfWeek() == SATURDAY) {
      return date.minusDays(1);
    } else if (date.getDayOfWeek() == SUNDAY) {
      return date.plusDays(1);
    }
    return date;
  }

  // christmas
  @VisibleForTesting
  static LocalDate christmasBumpedSatSun(int year) {
    LocalDate base = LocalDate.of(year, 12, 25);
    if (base.getDayOfWeek() == SATURDAY || base.getDayOfWeek() == SUNDAY) {
      return LocalDate.of(year, 12, 27);
    }
    return base;
  }

  // christmas (if Christmas is Sunday, moved to Monday)
  private static LocalDate christmasBumpedSun(int year) {
    LocalDate base = LocalDate.of(year, 12, 25);
    if (base.getDayOfWeek() == SUNDAY) {
      return LocalDate.of(year, 12, 26);
    }
    return base;
  }

  // boxing day
  @VisibleForTesting
  static LocalDate boxingDayBumpedSatSun(int year) {
    LocalDate base = LocalDate.of(year, 12, 26);
    if (base.getDayOfWeek() == SATURDAY || base.getDayOfWeek() == SUNDAY) {
      return LocalDate.of(year, 12, 28);
    }
    return base;
  }

  // boxing day (if Christmas is Sunday, boxing day moved from Monday to Tuesday)
  private static LocalDate boxingDayBumpedSun(int year) {
    LocalDate base = LocalDate.of(year, 12, 26);
    if (base.getDayOfWeek() == MONDAY) {
      return LocalDate.of(year, 12, 27);
    }
    return base;
  }

  // first of a month
  private static LocalDate first(int year, int month) {
    return LocalDate.of(year, month, 1);
  }

  // remove any holidays covered by Sat/Sun
  private static void removeSatSun(List<LocalDate> holidays) {
    holidays.removeIf(date -> date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY);
  }

  // calculate easter day by Delambre
  static LocalDate easter(int year) {
    int a = year % 19;
    int b = year / 100;
    int c = year % 100;
    int d = b / 4;
    int e = b % 4;
    int f = (b + 8) / 25;
    int g = (b - f + 1) / 3;
    int h = (19 * a + b - d - g + 15) % 30;
    int i = c / 4;
    int k = c % 4;
    int l = (32 + 2 * e + 2 * i - h - k) % 7;
    int m = (a + 11 * h + 22 * l) / 451;
    int month = (h + l - 7 * m + 114) / 31;
    int day = ((h + l - 7 * m + 114) % 31) + 1;
    return LocalDate.of(year, month, day);
  }

}
