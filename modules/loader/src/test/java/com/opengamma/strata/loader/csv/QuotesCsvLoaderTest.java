/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.observable.QuoteId;

/**
 * Test {@link QuotesCsvLoader}.
 */
@Test
public class QuotesCsvLoaderTest {

  private static final QuoteId FGBL_MAR14 = QuoteId.of(StandardId.of("OG-Future", "Eurex-FGBL-Mar14"));
  private static final QuoteId ED_MAR14 = QuoteId.of(StandardId.of("OG-Future", "CME-ED-Mar14"));
  private static final QuoteId FGBL_JUN14 = QuoteId.of(StandardId.of("OG-Future", "Eurex-FGBL-Jun14"));

  private static final LocalDate DATE1 = date(2014, 1, 22);
  private static final LocalDate DATE2 = date(2014, 1, 23);

  private static final ResourceLocator QUOTES_1 =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/quotes-1.csv");
  private static final ResourceLocator QUOTES_2 =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/quotes-2.csv");
  private static final ResourceLocator QUOTES_INVALID_DATE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/quotes-invalid-date.csv");
  private static final ResourceLocator QUOTES_INVALID_DUPLICATE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/quotes-invalid-duplicate.csv");

  //-------------------------------------------------------------------------
  public void test_noFiles() {
    Map<QuoteId, Double> map = QuotesCsvLoader.load(DATE1);
    assertEquals(map.size(), 0);
  }

  public void test_load_oneDate_file1_date1() {
    Map<QuoteId, Double> map = QuotesCsvLoader.load(DATE1, QUOTES_1);
    assertEquals(map.size(), 2);
    assertFile1Date1(map);
  }

  public void test_load_oneDate_file1_date1date2() {
    Map<LocalDate, ImmutableMap<QuoteId, Double>> map = QuotesCsvLoader.load(ImmutableSet.of(DATE1, DATE2), QUOTES_1);
    assertEquals(map.size(), 2);
    assertFile1Date1Date2(map);
  }

  public void test_load_oneDate_file1_date2() {
    Map<QuoteId, Double> map = QuotesCsvLoader.load(DATE2, ImmutableList.of(QUOTES_1));
    assertEquals(map.size(), 2);
    assertFile1Date2(map);
  }

  public void test_load_oneDate_file1file2_date1() {
    Map<QuoteId, Double> map = QuotesCsvLoader.load(DATE1, ImmutableList.of(QUOTES_1, QUOTES_2));
    assertEquals(map.size(), 3);
    assertFile1Date1(map);
    assertFile2Date1(map);
  }

  public void test_load_oneDate_invalidDate() {
    assertThrows(
        () -> QuotesCsvLoader.load(date(2015, 10, 2), QUOTES_INVALID_DATE),
        IllegalArgumentException.class,
        "Error processing resource as CSV file: .*");
  }

  public void test_invalidDuplicate() {
    assertThrowsIllegalArg(() -> QuotesCsvLoader.load(DATE1, QUOTES_INVALID_DUPLICATE));
  }

  public void test_load_dateSet_file1_date1() {
    Map<LocalDate, ImmutableMap<QuoteId, Double>> map = QuotesCsvLoader.load(ImmutableSet.of(DATE1, DATE2), QUOTES_1);
    assertEquals(map.size(), 2);
    assertFile1Date1(map.get(DATE1));
    assertFile1Date2(map.get(DATE2));
  }

  public void test_load_alLDates_file1_date1() {
    Map<LocalDate, ImmutableMap<QuoteId, Double>> map = QuotesCsvLoader.loadAllDates(QUOTES_1);
    assertEquals(map.size(), 2);
    assertFile1Date1(map.get(DATE1));
    assertFile1Date2(map.get(DATE2));
  }

  //-------------------------------------------------------------------------
  private void assertFile1Date1(Map<QuoteId, Double> map) {
    assertTrue(map.containsKey(FGBL_MAR14));
    assertTrue(map.containsKey(ED_MAR14));
    assertEquals(map.get(FGBL_MAR14), 150.43, 1e-6);
    assertEquals(map.get(ED_MAR14), 99.62, 1e-6);
  }

  private void assertFile1Date1Date2(Map<LocalDate, ImmutableMap<QuoteId, Double>> map) {
    assertTrue(map.containsKey(DATE1));
    assertTrue(map.containsKey(DATE2));
    assertTrue(map.get(DATE1).containsKey(FGBL_MAR14));
    assertTrue(map.get(DATE2).containsKey(FGBL_MAR14));
    assertTrue(map.get(DATE1).containsKey(ED_MAR14));
    assertTrue(map.get(DATE2).containsKey(ED_MAR14));
    assertEquals(map.get(DATE1).get(FGBL_MAR14), 150.43, 1e-6);
    assertEquals(map.get(DATE1).get(ED_MAR14), 99.62, 1e-6);
    assertEquals(map.get(DATE2).get(FGBL_MAR14), 150.5, 1e-6);
    assertEquals(map.get(DATE2).get(ED_MAR14), 99.63, 1e-6);
  }

  private void assertFile1Date2(Map<QuoteId, Double> map) {
    assertTrue(map.containsKey(FGBL_MAR14));
    assertTrue(map.containsKey(ED_MAR14));
    assertEquals(map.get(FGBL_MAR14), 150.50, 1e-6);
    assertEquals(map.get(ED_MAR14), 99.63, 1e-6);
  }

  private void assertFile2Date1(Map<QuoteId, Double> map) {
    assertTrue(map.containsKey(FGBL_JUN14));
    assertEquals(map.get(FGBL_JUN14), 150.99, 1e-6);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(QuotesCsvLoader.class);
  }

}
