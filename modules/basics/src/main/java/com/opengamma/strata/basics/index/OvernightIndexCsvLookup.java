/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Loads standard Overnight Index implementations from CSV.
 * <p>
 * See {@link OvernightIndices} for the description of each.
 */
final class OvernightIndexCsvLookup
    implements NamedLookup<OvernightIndex> {

  // http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(OvernightIndexCsvLookup.class.getName());
  /**
   * The singleton instance of the lookup.
   */
  public static final OvernightIndexCsvLookup INSTANCE = new OvernightIndexCsvLookup();

  // CSV column headers
  private static final String NAME_FIELD = "Name";
  private static final String CURRENCY_FIELD = "Currency";
  private static final String ACTIVE_FIELD = "Active";
  private static final String DAY_COUNT_FIELD = "Day Count";
  private static final String FIXING_CALENDAR_FIELD = "Fixing Calendar";
  private static final String PUBLICATION_DAYS_FIELD = "Publication Offset Days";
  private static final String EFFECTIVE_DAYS_FIELD = "Effective Offset Days";

  /**
   * The cache by name.
   */
  private static final ImmutableMap<String, OvernightIndex> BY_NAME = loadFromCsv();

  /**
   * Restricted constructor.
   */
  private OvernightIndexCsvLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, OvernightIndex> lookupAll() {
    return BY_NAME;
  }

  private static ImmutableMap<String, OvernightIndex> loadFromCsv() {
    List<ResourceLocator> resources = ResourceConfig.orderedResources("OvernightIndexData.csv");
    Map<String, OvernightIndex> map = new HashMap<>();
    for (ResourceLocator resource : resources) {
      try {
        CsvFile csv = CsvFile.of(resource.getCharSource(), true);
        for (CsvRow row : csv.rows()) {
          OvernightIndex parsed = parseOvernightIndex(row);
          map.put(parsed.getName(), parsed);
          map.putIfAbsent(parsed.getName().toUpperCase(Locale.ENGLISH), parsed);
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as Overnight Index CSV file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static OvernightIndex parseOvernightIndex(CsvRow row) {
    String name = row.getField(NAME_FIELD);
    Currency currency = Currency.parse(row.getField(CURRENCY_FIELD));
    boolean active = Boolean.parseBoolean(row.getField(ACTIVE_FIELD));
    DayCount dayCount = DayCount.of(row.getField(DAY_COUNT_FIELD));
    HolidayCalendarId fixingCal = HolidayCalendarId.of(row.getField(FIXING_CALENDAR_FIELD));
    int publicationDays = Integer.parseInt(row.getField(PUBLICATION_DAYS_FIELD));
    int effectiveDays = Integer.parseInt(row.getField(EFFECTIVE_DAYS_FIELD));
    // build result
    return ImmutableOvernightIndex.builder()
        .name(name)
        .currency(currency)
        .active(active)
        .dayCount(dayCount)
        .fixingCalendar(fixingCal)
        .publicationDateOffset(publicationDays)
        .effectiveDateOffset(effectiveDays)
        .build();
  }

}
