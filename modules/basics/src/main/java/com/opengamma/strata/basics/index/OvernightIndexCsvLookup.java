/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.collect.io.CsvFile;
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
        for (int i = 0; i < csv.rowCount(); i++) {
          OvernightIndex parsed = parseOvernightIndex(csv, i);
          map.put(parsed.getName(), parsed);
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as Overnight Index CSV file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static OvernightIndex parseOvernightIndex(CsvFile csv, int row) {
    String name = csv.field(row, NAME_FIELD);
    Currency currency = Currency.parse(csv.field(row, CURRENCY_FIELD));
    DayCount dayCount = DayCount.of(csv.field(row, DAY_COUNT_FIELD));
    HolidayCalendar fixingCal = HolidayCalendar.of(csv.field(row, FIXING_CALENDAR_FIELD));
    int publicationDays = Integer.parseInt(csv.field(row, PUBLICATION_DAYS_FIELD));
    int effectiveDays = Integer.parseInt(csv.field(row, EFFECTIVE_DAYS_FIELD));
    // build result
    return ImmutableOvernightIndex.builder()
        .name(name)
        .currency(currency)
        .dayCount(dayCount)
        .fixingCalendar(fixingCal)
        .publicationDateOffset(publicationDays)
        .effectiveDateOffset(effectiveDays)
        .build();
  }

}
