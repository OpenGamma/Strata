/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.PeriodAdditionConvention;
import com.opengamma.strata.basics.date.PeriodAdditionConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Loads standard Ibor Index implementations from CSV.
 * <p>
 * See {@link IborIndices} for the description of each.
 */
final class IborIndexCsvLookup
    implements NamedLookup<IborIndex> {

  // https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf
  // LIBOR - http://www.bbalibor.com/technical-aspects/fixing-value-and-maturity
  // different rules for overnight
  // EURIBOR - http://www.bbalibor.com/technical-aspects/fixing-value-and-maturity
  // EURIBOR - http://www.emmi-benchmarks.eu/assets/files/Euribor_code_conduct.pdf
  // TIBOR - http://www.jbatibor.or.jp/english/public/

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(IborIndexCsvLookup.class.getName());
  /**
   * The singleton instance of the lookup.
   */
  public static final IborIndexCsvLookup INSTANCE = new IborIndexCsvLookup();

  // CSV column headers
  private static final String NAME_FIELD = "Name";
  private static final String CURRENCY_FIELD = "Currency";
  private static final String ACTIVE_FIELD = "Active";
  private static final String DAY_COUNT_FIELD = "Day Count";
  private static final String FIXING_CALENDAR_FIELD = "Fixing Calendar";
  private static final String OFFSET_DAYS_FIELD = "Offset Days";
  private static final String OFFSET_CALENDAR_FIELD = "Offset Calendar";
  private static final String EFFECTIVE_DATE_CALENDAR_FIELD = "Effective Date Calendar";
  private static final String TENOR_FIELD = "Tenor";
  private static final String TENOR_CONVENTION_FIELD = "Tenor Convention";
  private static final String FIXING_TIME_FIELD = "FixingTime";
  private static final String FIXING_ZONE_FIELD = "FixingZone";
  private static final String FIXED_LEG_DAY_COUNT = "Fixed Leg Day Count";

  /**
   * The time formatter.
   */
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH[:mm]", Locale.ENGLISH);
  /**
   * The cache by name.
   */
  private static final ImmutableMap<String, IborIndex> BY_NAME = loadFromCsv();

  /**
   * Restricted constructor.
   */
  private IborIndexCsvLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, IborIndex> lookupAll() {
    return BY_NAME;
  }

  private static ImmutableMap<String, IborIndex> loadFromCsv() {
    List<ResourceLocator> resources = ResourceConfig.orderedResources("IborIndexData.csv");
    Map<String, IborIndex> map = new HashMap<>();
    // files are ordered lowest priority to highest, thus Map::put is used
    for (ResourceLocator resource : resources) {
      try {
        CsvFile csv = CsvFile.of(resource.getCharSource(), true);
        for (CsvRow row : csv.rows()) {
          IborIndex parsed = parseIborIndex(row);
          map.put(parsed.getName(), parsed);
          map.put(parsed.getName().toUpperCase(Locale.ENGLISH), parsed);
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as Ibor Index CSV file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static IborIndex parseIborIndex(CsvRow row) {
    String name = row.getValue(NAME_FIELD);
    Currency currency = Currency.parse(row.getValue(CURRENCY_FIELD));
    boolean active = Boolean.parseBoolean(row.getValue(ACTIVE_FIELD));
    DayCount dayCount = DayCount.of(row.getValue(DAY_COUNT_FIELD));
    HolidayCalendarId fixingCal = HolidayCalendarId.of(row.getValue(FIXING_CALENDAR_FIELD));
    int offsetDays = Integer.parseInt(row.getValue(OFFSET_DAYS_FIELD));
    HolidayCalendarId offsetCal = HolidayCalendarId.of(row.getValue(OFFSET_CALENDAR_FIELD));
    HolidayCalendarId effectiveCal = HolidayCalendarId.of(row.getValue(EFFECTIVE_DATE_CALENDAR_FIELD));
    Tenor tenor = Tenor.parse(row.getValue(TENOR_FIELD));
    LocalTime time = LocalTime.parse(row.getValue(FIXING_TIME_FIELD), TIME_FORMAT);
    ZoneId zoneId = ZoneId.of(row.getValue(FIXING_ZONE_FIELD));
    DayCount fixedLegDayCount = DayCount.of(row.getValue(FIXED_LEG_DAY_COUNT));

    // interpret CSV
    DaysAdjustment fixingOffset = DaysAdjustment.ofBusinessDays(
        -offsetDays, offsetCal, BusinessDayAdjustment.of(PRECEDING, fixingCal)).normalized();
    DaysAdjustment effectiveOffset = DaysAdjustment.ofBusinessDays(
        offsetDays, offsetCal, BusinessDayAdjustment.of(FOLLOWING, effectiveCal)).normalized();

    // convention can be two different things
    PeriodAdditionConvention periodAdditionConvention =
        PeriodAdditionConvention.extendedEnum().find(row.getField(TENOR_CONVENTION_FIELD))
            .orElse(PeriodAdditionConventions.NONE);
    BusinessDayConvention tenorBusinessConvention =
        BusinessDayConvention.extendedEnum().find(row.getField(TENOR_CONVENTION_FIELD))
            .orElse(isEndOfMonth(periodAdditionConvention) ? MODIFIED_FOLLOWING : FOLLOWING);
    BusinessDayAdjustment adj = BusinessDayAdjustment.of(tenorBusinessConvention, effectiveCal);
    TenorAdjustment tenorAdjustment = TenorAdjustment.of(tenor, periodAdditionConvention, adj);

    // build result
    return ImmutableIborIndex.builder()
        .name(name)
        .currency(currency)
        .active(active)
        .dayCount(dayCount)
        .fixingCalendar(fixingCal)
        .fixingDateOffset(fixingOffset)
        .effectiveDateOffset(effectiveOffset)
        .maturityDateOffset(tenorAdjustment)
        .fixingTime(time)
        .fixingZone(zoneId)
        .defaultFixedLegDayCount(fixedLegDayCount)
        .build();
  }

  private static boolean isEndOfMonth(PeriodAdditionConvention tenorConvention) {
    return tenorConvention.equals(PeriodAdditionConventions.LAST_BUSINESS_DAY) ||
        tenorConvention.equals(PeriodAdditionConventions.LAST_DAY);
  }

}
