/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;

/**
 * Test {@link ExampleMarketDataBuilder}, {@link DirectoryMarketDataBuilder} and {@link JarMarketDataBuilder}.
 */
@Test
public class ExampleMarketDataBuilderTest {

  private static final String EXAMPLE_MARKET_DATA_CLASSPATH_ROOT = "example-marketdata";
  private static final String EXAMPLE_MARKET_DATA_DIRECTORY_ROOT = "src/main/resources/example-marketdata";

  private static final String TEST_SPACES_DIRECTORY_ROOT = "src/test/resources/test-marketdata with spaces";
  private static final String TEST_SPACES_CLASSPATH_ROOT = "test-marketdata with spaces";

  private static final CurveGroupName DEFAULT_CURVE_GROUP = CurveGroupName.of("Default");

  private static final LocalDate MARKET_DATA_DATE = LocalDate.of(2014, 1, 22);

  private static final Set<ObservableId> TIME_SERIES = ImmutableSet.of(
      IndexQuoteId.of(IborIndices.USD_LIBOR_3M),
      IndexQuoteId.of(IborIndices.USD_LIBOR_6M),
      IndexQuoteId.of(OvernightIndices.USD_FED_FUND),
      IndexQuoteId.of(IborIndices.GBP_LIBOR_3M));

  private static final Set<MarketDataId<?>> VALUES = ImmutableSet.of(
      CurveId.of(DEFAULT_CURVE_GROUP, CurveName.of("USD-Disc")),
      CurveId.of(DEFAULT_CURVE_GROUP, CurveName.of("GBP-Disc")),
      CurveId.of(DEFAULT_CURVE_GROUP, CurveName.of("USD-3ML")),
      CurveId.of(DEFAULT_CURVE_GROUP, CurveName.of("USD-6ML")),
      CurveId.of(DEFAULT_CURVE_GROUP, CurveName.of("GBP-3ML")),
      FxRateId.of(Currency.USD, Currency.GBP),
      QuoteId.of(StandardId.of("OG-Future", "Eurex-FGBL-Mar14")),
      QuoteId.of(StandardId.of("OG-Future", "Eurex-FGBL-Mar14"), FieldName.SETTLEMENT_PRICE),
      QuoteId.of(StandardId.of("OG-FutOpt", "Eurex-OGBL-Mar14-C150")),
      QuoteId.of(StandardId.of("OG-FutOpt", "Eurex-OGBL-Mar14-C150"), FieldName.SETTLEMENT_PRICE),
      QuoteId.of(StandardId.of("OG-Future", "CME-ED-Mar14")),
      QuoteId.of(StandardId.of("OG-Future", "CME-ED-Mar14"), FieldName.SETTLEMENT_PRICE),
      QuoteId.of(StandardId.of("OG-Future", "Ibor-USD-LIBOR-3M-Mar15")),
      QuoteId.of(StandardId.of("OG-Future", "Ibor-USD-LIBOR-3M-Mar15"), FieldName.SETTLEMENT_PRICE),
      QuoteId.of(StandardId.of("OG-Future", "Ibor-USD-LIBOR-3M-Jun15")),
      QuoteId.of(StandardId.of("OG-Future", "Ibor-USD-LIBOR-3M-Jun15"), FieldName.SETTLEMENT_PRICE),
      QuoteId.of(StandardId.of("OG-Future", "CME-F1U-Mar15")),
      QuoteId.of(StandardId.of("OG-Future", "CME-F1U-Mar15"), FieldName.SETTLEMENT_PRICE),
      QuoteId.of(StandardId.of("OG-Future", "CME-F1U-Jun15")),
      QuoteId.of(StandardId.of("OG-Future", "CME-F1U-Jun15"), FieldName.SETTLEMENT_PRICE));

  public void test_directory() {
    Path rootPath = new File(EXAMPLE_MARKET_DATA_DIRECTORY_ROOT).toPath();
    DirectoryMarketDataBuilder builder = new DirectoryMarketDataBuilder(rootPath);
    assertBuilder(builder);
  }

  public void test_ofPath() {
    Path rootPath = new File(EXAMPLE_MARKET_DATA_DIRECTORY_ROOT).toPath();
    ExampleMarketDataBuilder builder = ExampleMarketDataBuilder.ofPath(rootPath);
    assertBuilder(builder);
  }

  public void test_ofPath_with_spaces() {
    Path rootPath = new File(TEST_SPACES_DIRECTORY_ROOT).toPath();
    ExampleMarketDataBuilder builder = ExampleMarketDataBuilder.ofPath(rootPath);

    ImmutableMarketData snapshot = builder.buildSnapshot(LocalDate.of(2015, 1, 1));
    assertEquals(snapshot.getTimeSeries().size(), 1);
  }

  public void test_ofResource_directory() {
    ExampleMarketDataBuilder builder = ExampleMarketDataBuilder.ofResource(EXAMPLE_MARKET_DATA_CLASSPATH_ROOT);
    assertBuilder(builder);
  }

  public void test_ofResource_directory_extraSlashes() {
    ExampleMarketDataBuilder builder = ExampleMarketDataBuilder.ofResource("/" + EXAMPLE_MARKET_DATA_CLASSPATH_ROOT + "/");
    assertBuilder(builder);
  }

  public void test_ofResource_directory_notFound() {
    assertThrowsIllegalArg(() -> ExampleMarketDataBuilder.ofResource("bad-dir"));
  }

  public void test_ofResource_directory_with_spaces() {
    ExampleMarketDataBuilder builder = ExampleMarketDataBuilder.ofResource(TEST_SPACES_CLASSPATH_ROOT);

    ImmutableMarketData snapshot = builder.buildSnapshot(MARKET_DATA_DATE);
    assertEquals(snapshot.getTimeSeries().size(), 1);
  }

  //-------------------------------------------------------------------------
  private void assertBuilder(ExampleMarketDataBuilder builder) {
    ImmutableMarketData snapshot = builder.buildSnapshot(MARKET_DATA_DATE);

    assertEquals(MARKET_DATA_DATE, snapshot.getValuationDate());

    for (ObservableId id : TIME_SERIES) {
      assertFalse(snapshot.getTimeSeries(id).isEmpty(), "Time-series not found: " + id);
    }
    assertEquals(snapshot.getTimeSeries().size(), TIME_SERIES.size(),
        Messages.format("Snapshot contained unexpected time-series: {}",
            Sets.difference(snapshot.getTimeSeries().keySet(), TIME_SERIES)));

    for (MarketDataId<?> id : VALUES) {
      assertTrue(snapshot.containsValue(id), "Id not found: " + id);
    }

    assertEquals(snapshot.getValues().size(), VALUES.size(),
        Messages.format("Snapshot contained unexpected market data: {}",
            Sets.difference(snapshot.getValues().keySet(), VALUES)));
  }

}
