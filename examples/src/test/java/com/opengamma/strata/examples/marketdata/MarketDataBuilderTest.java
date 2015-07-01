/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.opengamma.strata.market.id.IsdaIndexRecoveryRateId;
import com.opengamma.strata.market.id.IsdaSingleNameRecoveryRateId;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.engine.config.MarketDataRule;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.examples.marketdata.credit.markit.MarkitRedCode;
import com.opengamma.strata.finance.credit.IndexReferenceInformation;
import com.opengamma.strata.finance.credit.RestructuringClause;
import com.opengamma.strata.finance.credit.SeniorityLevel;
import com.opengamma.strata.finance.credit.SingleNameReferenceInformation;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.IndexRateId;
import com.opengamma.strata.market.id.IsdaIndexCreditCurveParRatesId;
import com.opengamma.strata.market.id.IsdaSingleNameCreditCurveParRatesId;
import com.opengamma.strata.market.id.IsdaYieldCurveParRatesId;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.id.RateIndexCurveId;

/**
 * Test {@link MarketDataBuilder}, {@link DirectoryMarketDataBuilder} and {@link JarMarketDataBuilder}.
 */
@Test
public class MarketDataBuilderTest {

  private static final String EXAMPLE_MARKET_DATA_CLASSPATH_ROOT = "example-marketdata";
  private static final String EXAMPLE_MARKET_DATA_DIRECTORY_ROOT = "src/main/resources/example-marketdata";

  private static final String TEST_SPACES_DIRECTORY_ROOT = "src/test/resources/test-marketdata with spaces";
  private static final String TEST_SPACES_CLASSPATH_ROOT = "test-marketdata with spaces";

  private static final CurveGroupName DEFAULT_CURVE_GROUP = CurveGroupName.of("Default");

  private static final LocalDate MARKET_DATA_DATE = LocalDate.of(2014, 1, 22);

  private static final Set<ObservableId> TIME_SERIES = ImmutableSet.of(
      IndexRateId.of(IborIndices.USD_LIBOR_3M),
      IndexRateId.of(IborIndices.USD_LIBOR_6M),
      IndexRateId.of(OvernightIndices.USD_FED_FUND),
      IndexRateId.of(IborIndices.GBP_LIBOR_3M));

  private static final Set<MarketDataId<?>> VALUES = ImmutableSet.of(
      DiscountCurveId.of(Currency.USD, DEFAULT_CURVE_GROUP),
      RateIndexCurveId.of(IborIndices.USD_LIBOR_3M, DEFAULT_CURVE_GROUP),
      RateIndexCurveId.of(IborIndices.USD_LIBOR_6M, DEFAULT_CURVE_GROUP),
      RateIndexCurveId.of(OvernightIndices.USD_FED_FUND, DEFAULT_CURVE_GROUP),
      DiscountCurveId.of(Currency.GBP, DEFAULT_CURVE_GROUP),
      RateIndexCurveId.of(IborIndices.GBP_LIBOR_3M, DEFAULT_CURVE_GROUP),
      FxRateId.of(Currency.USD, Currency.GBP),
      QuoteId.of(StandardId.of("OG-Future", "Eurex-FGBL-Mar14")),
      QuoteId.of(StandardId.of("OG-FutOpt", "Eurex-OGBL-Mar14-C150")),
      QuoteId.of(StandardId.of("OG-Future", "CME-ED-Mar14")),
      IsdaYieldCurveParRatesId.of(Currency.USD),
      IsdaSingleNameCreditCurveParRatesId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP10"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.USD,
              RestructuringClause.NO_RESTRUCTURING_2014)),
      IsdaSingleNameCreditCurveParRatesId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP02"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.USD,
              RestructuringClause.NO_RESTRUCTURING_2014)),
      IsdaSingleNameCreditCurveParRatesId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP01"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.USD,
              RestructuringClause.NO_RESTRUCTURING_2014)),
      IsdaSingleNameCreditCurveParRatesId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP11"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.EUR,
              RestructuringClause.MOD_MOD_RESTRUCTURING_2014)),
      IsdaSingleNameCreditCurveParRatesId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP11"),
              SeniorityLevel.SUBORDINATE_LOWER_TIER_2,
              Currency.EUR,
              RestructuringClause.MOD_MOD_RESTRUCTURING_2014)),
      IsdaIndexCreditCurveParRatesId.of(
          IndexReferenceInformation.of(
              MarkitRedCode.id("INDEX0001"),
              22,
              4)),
      IsdaSingleNameRecoveryRateId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP10"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.USD,
              RestructuringClause.NO_RESTRUCTURING_2014)),
      IsdaSingleNameRecoveryRateId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP02"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.USD,
              RestructuringClause.NO_RESTRUCTURING_2014)),
      IsdaSingleNameRecoveryRateId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP01"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.USD,
              RestructuringClause.NO_RESTRUCTURING_2014)),
      IsdaSingleNameRecoveryRateId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP11"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.EUR,
              RestructuringClause.MOD_MOD_RESTRUCTURING_2014)),
      IsdaSingleNameRecoveryRateId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP11"),
              SeniorityLevel.SUBORDINATE_LOWER_TIER_2,
              Currency.EUR,
              RestructuringClause.MOD_MOD_RESTRUCTURING_2014)),
      IsdaIndexRecoveryRateId.of(
          IndexReferenceInformation.of(
              MarkitRedCode.id("INDEX0001"),
              22,
              4)));


  public void test_directory() {
    Path rootPath = new File(EXAMPLE_MARKET_DATA_DIRECTORY_ROOT).toPath();
    DirectoryMarketDataBuilder builder = new DirectoryMarketDataBuilder(rootPath);
    assertBuilder(builder);
  }

  public void test_classpath_jar() throws Exception {

    // Create a JAR file containing the example market data
    File tempFile = File.createTempFile(MarketDataBuilderTest.class.getSimpleName(), ".jar");
    try (FileOutputStream tempFileOut = new FileOutputStream(tempFile)) {
      try (ZipOutputStream zipFileOut = new ZipOutputStream(tempFileOut)) {
        File diskRoot = new File(EXAMPLE_MARKET_DATA_DIRECTORY_ROOT);
        appendToZip(diskRoot, "zip-data", diskRoot, zipFileOut);
      }
    }

    // Obtain a classloader which can see this JAR
    ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] {tempFile.toURI().toURL()})) {
      // Test automatically finding the resource inside the JAR
      Thread.currentThread().setContextClassLoader(classLoader);
      assertBuilder(MarketDataBuilder.ofResource("zip-data", classLoader));
    } finally {
      Thread.currentThread().setContextClassLoader(originalContextClassLoader);
      try {
        Files.deleteIfExists(tempFile.toPath());
      } catch (IOException ex) {
        // ignore
      }
    }
  }

  public void test_of_path() {
    Path rootPath = new File(EXAMPLE_MARKET_DATA_DIRECTORY_ROOT).toPath();
    MarketDataBuilder builder = MarketDataBuilder.ofPath(rootPath);
    assertBuilder(builder);
  }

  public void test_of_path_with_spaces() {
    Path rootPath = new File(TEST_SPACES_DIRECTORY_ROOT).toPath();
    MarketDataBuilder builder = MarketDataBuilder.ofPath(rootPath);

    BaseMarketData snapshot = builder.buildSnapshot(LocalDate.of(2015, 1, 1));
    assertEquals(snapshot.getTimeSeries().size(), 1);
  }

  public void test_of_resource_directory() {
    MarketDataBuilder builder = MarketDataBuilder.ofResource(EXAMPLE_MARKET_DATA_CLASSPATH_ROOT);
    assertBuilder(builder);
  }

  public void test_of_resource_directory_with_spaces() {
    MarketDataBuilder builder = MarketDataBuilder.ofResource(TEST_SPACES_CLASSPATH_ROOT);

    BaseMarketData snapshot = builder.buildSnapshot(MARKET_DATA_DATE);
    assertEquals(snapshot.getTimeSeries().size(), 1);
  }

  //-------------------------------------------------------------------------
  private void assertBuilder(MarketDataBuilder builder) {
    BaseMarketData snapshot = builder.buildSnapshot(MARKET_DATA_DATE);

    assertEquals(MARKET_DATA_DATE, snapshot.getValuationDate());

    for (ObservableId id : TIME_SERIES) {
      assertTrue(snapshot.containsTimeSeries(id));
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

    MarketDataRules expectedRules = MarketDataRules.of(
        MarketDataRule.anyTarget(
            MarketDataMappingsBuilder.create()
                .curveGroup(CurveGroupName.of("Default"))
                .build()));
    assertEquals(builder.rules(), expectedRules);
  }

  private void appendToZip(File sourceRootDir, String destRootPath, File currentFile, ZipOutputStream zipOutput)
      throws IOException {
    if (currentFile.isDirectory()) {
      String entryName = getEntryName(sourceRootDir, destRootPath, currentFile) + File.separator;
      zipOutput.putNextEntry(new ZipEntry(entryName));
      zipOutput.closeEntry();
      for (File content : currentFile.listFiles()) {
        appendToZip(sourceRootDir, destRootPath, content, zipOutput);
      }
    } else {
      String entryName = getEntryName(sourceRootDir, destRootPath, currentFile);
      zipOutput.putNextEntry(new ZipEntry(entryName));
      try (FileInputStream fileIn = new FileInputStream(currentFile)) {
        byte[] b = new byte[1024];
        int len;
        while ((len = fileIn.read(b)) != -1) {
          zipOutput.write(b, 0, len);
        }
      }
      zipOutput.closeEntry();
    }
  }

  private String getEntryName(File sourceRootDir, String destRootPath, File currentFile) {
    return destRootPath + currentFile.getAbsolutePath().substring(sourceRootDir.getAbsolutePath().length());
  }

}
