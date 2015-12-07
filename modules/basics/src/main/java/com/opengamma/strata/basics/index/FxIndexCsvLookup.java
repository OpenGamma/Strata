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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Loads standard FX Index implementations from CSV.
 * <p>
 * See {@link FxIndices} for the description of each.
 */
final class FxIndexCsvLookup
    implements NamedLookup<FxIndex> {

  // http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(FxIndexCsvLookup.class.getName());
  /**
   * The singleton instance of the lookup.
   */
  public static final FxIndexCsvLookup INSTANCE = new FxIndexCsvLookup();

  // CSV column headers
  private static final String NAME_FIELD = "Name";
  private static final String BASE_CURRENCY_FIELD = "Base Currency";
  private static final String COUNTER_CURRENCY_FIELD = "Counter Currency";
  private static final String FIXING_CALENDAR_FIELD = "Fixing Calendar";
  private static final String MATURITY_DAYS_FIELD = "Maturity Days";
  private static final String MATURITY_CALENDAR_FIELD = "Maturity Calendar";

  /**
   * The cache by name.
   */
  private static final ImmutableMap<String, FxIndex> BY_NAME = loadFromCsv();

  /**
   * Restricted constructor.
   */
  private FxIndexCsvLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, FxIndex> lookupAll() {
    return BY_NAME;
  }

  private static ImmutableMap<String, FxIndex> loadFromCsv() {
    List<ResourceLocator> resources = ResourceConfig.orderedResources("FxIndexData.csv");
    Map<String, FxIndex> map = new HashMap<>();
    for (ResourceLocator resource : resources) {
      try {
        CsvFile csv = CsvFile.of(resource.getCharSource(), true);
        for (int i = 0; i < csv.rowCount(); i++) {
          FxIndex parsed = parseFxIndex(csv, i);
          map.put(parsed.getName(), parsed);
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as FX Index CSV file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static FxIndex parseFxIndex(CsvFile csv, int row) {
    String name = csv.field(row, NAME_FIELD);
    Currency baseCurrency = Currency.parse(csv.field(row, BASE_CURRENCY_FIELD));
    Currency counterCurrency = Currency.parse(csv.field(row, COUNTER_CURRENCY_FIELD));
    HolidayCalendar fixingCal = HolidayCalendar.of(csv.field(row, FIXING_CALENDAR_FIELD));
    int maturityDays = Integer.parseInt(csv.field(row, MATURITY_DAYS_FIELD));
    HolidayCalendar maturityCal = HolidayCalendar.of(csv.field(row, MATURITY_CALENDAR_FIELD));
    // build result
    return ImmutableFxIndex.builder()
        .name(name)
        .currencyPair(CurrencyPair.of(baseCurrency, counterCurrency))
        .fixingCalendar(fixingCal)
        .maturityDateOffset(DaysAdjustment.ofBusinessDays(maturityDays, maturityCal))
        .build();
  }

}
