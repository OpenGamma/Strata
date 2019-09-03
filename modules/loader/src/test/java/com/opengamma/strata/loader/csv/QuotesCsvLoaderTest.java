/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.observable.QuoteId;

/**
 * Test {@link QuotesCsvLoader}.
 */
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
  @Test
  public void test_noFiles() {
    Map<QuoteId, Double> map = QuotesCsvLoader.load(DATE1);
    assertThat(map).hasSize(0);
  }

  @Test
  public void test_load_oneDate_file1_date1() {
    Map<QuoteId, Double> map = QuotesCsvLoader.load(DATE1, QUOTES_1);
    assertThat(map).hasSize(2);
    assertFile1Date1(map);
  }

  @Test
  public void test_load_oneDate_file1_date1date2() {
    Map<LocalDate, ImmutableMap<QuoteId, Double>> map = QuotesCsvLoader.load(ImmutableSet.of(DATE1, DATE2), QUOTES_1);
    assertThat(map).hasSize(2);
    assertFile1Date1Date2(map);
  }

  @Test
  public void test_load_oneDate_file1_date2() {
    Map<QuoteId, Double> map = QuotesCsvLoader.load(DATE2, ImmutableList.of(QUOTES_1));
    assertThat(map).hasSize(2);
    assertFile1Date2(map);
  }

  @Test
  public void test_load_oneDate_file1file2_date1() {
    Map<QuoteId, Double> map = QuotesCsvLoader.load(DATE1, ImmutableList.of(QUOTES_1, QUOTES_2));
    assertThat(map).hasSize(3);
    assertFile1Date1(map);
    assertFile2Date1(map);
  }

  @Test
  public void test_load_oneDate_invalidDate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> QuotesCsvLoader.load(date(2015, 10, 2), QUOTES_INVALID_DATE))
        .withMessageStartingWith("Error processing resource as CSV file: ");
  }

  @Test
  public void test_invalidDuplicate() {
    assertThatIllegalArgumentException().isThrownBy(() -> QuotesCsvLoader.load(DATE1, QUOTES_INVALID_DUPLICATE));
  }

  @Test
  public void test_load_dateSet_file1_date1() {
    Map<LocalDate, ImmutableMap<QuoteId, Double>> map = QuotesCsvLoader.load(ImmutableSet.of(DATE1, DATE2), QUOTES_1);
    assertThat(map).hasSize(2);
    assertFile1Date1(map.get(DATE1));
    assertFile1Date2(map.get(DATE2));
  }

  @Test
  public void test_load_alLDates_file1_date1() {
    Map<LocalDate, ImmutableMap<QuoteId, Double>> map = QuotesCsvLoader.loadAllDates(QUOTES_1);
    assertThat(map).hasSize(2);
    assertFile1Date1(map.get(DATE1));
    assertFile1Date2(map.get(DATE2));
  }

  //-------------------------------------------------------------------------
  private void assertFile1Date1(Map<QuoteId, Double> map) {
    assertThat(map.containsKey(FGBL_MAR14)).isTrue();
    assertThat(map.containsKey(ED_MAR14)).isTrue();
    assertThat(map.get(FGBL_MAR14)).isCloseTo(150.43, offset(1e-6));
    assertThat(map.get(ED_MAR14)).isCloseTo(99.62, offset(1e-6));
  }

  private void assertFile1Date1Date2(Map<LocalDate, ImmutableMap<QuoteId, Double>> map) {
    assertThat(map.containsKey(DATE1)).isTrue();
    assertThat(map.containsKey(DATE2)).isTrue();
    assertThat(map.get(DATE1).containsKey(FGBL_MAR14)).isTrue();
    assertThat(map.get(DATE2).containsKey(FGBL_MAR14)).isTrue();
    assertThat(map.get(DATE1).containsKey(ED_MAR14)).isTrue();
    assertThat(map.get(DATE2).containsKey(ED_MAR14)).isTrue();
    assertThat(map.get(DATE1).get(FGBL_MAR14)).isCloseTo(150.43, offset(1e-6));
    assertThat(map.get(DATE1).get(ED_MAR14)).isCloseTo(99.62, offset(1e-6));
    assertThat(map.get(DATE2).get(FGBL_MAR14)).isCloseTo(150.5, offset(1e-6));
    assertThat(map.get(DATE2).get(ED_MAR14)).isCloseTo(99.63, offset(1e-6));
  }

  private void assertFile1Date2(Map<QuoteId, Double> map) {
    assertThat(map.containsKey(FGBL_MAR14)).isTrue();
    assertThat(map.containsKey(ED_MAR14)).isTrue();
    assertThat(map.get(FGBL_MAR14)).isCloseTo(150.50, offset(1e-6));
    assertThat(map.get(ED_MAR14)).isCloseTo(99.63, offset(1e-6));
  }

  private void assertFile2Date1(Map<QuoteId, Double> map) {
    assertThat(map.containsKey(FGBL_JUN14)).isTrue();
    assertThat(map.get(FGBL_JUN14)).isCloseTo(150.99, offset(1e-6));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(QuotesCsvLoader.class);
  }

}
