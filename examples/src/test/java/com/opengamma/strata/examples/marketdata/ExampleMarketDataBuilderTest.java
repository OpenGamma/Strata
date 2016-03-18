/**
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.examples.marketdata.credit.markit.MarkitRedCode;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.IborIndexCurveId;
import com.opengamma.strata.market.id.IndexRateId;
import com.opengamma.strata.market.id.IsdaIndexCreditCurveInputsId;
import com.opengamma.strata.market.id.IsdaIndexRecoveryRateId;
import com.opengamma.strata.market.id.IsdaSingleNameCreditCurveInputsId;
import com.opengamma.strata.market.id.IsdaSingleNameRecoveryRateId;
import com.opengamma.strata.market.id.IsdaYieldCurveInputsId;
import com.opengamma.strata.market.id.OvernightIndexCurveId;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.RestructuringClause;
import com.opengamma.strata.product.credit.SeniorityLevel;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;

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
      IndexRateId.of(IborIndices.USD_LIBOR_3M),
      IndexRateId.of(IborIndices.USD_LIBOR_6M),
      IndexRateId.of(OvernightIndices.USD_FED_FUND),
      IndexRateId.of(IborIndices.GBP_LIBOR_3M));

  private static final Set<MarketDataId<?>> VALUES = ImmutableSet.of(
      CurveGroupId.of(DEFAULT_CURVE_GROUP),
      DiscountCurveId.of(Currency.USD, DEFAULT_CURVE_GROUP),
      DiscountCurveId.of(Currency.GBP, DEFAULT_CURVE_GROUP),
      IborIndexCurveId.of(IborIndices.USD_LIBOR_3M, DEFAULT_CURVE_GROUP),
      IborIndexCurveId.of(IborIndices.USD_LIBOR_6M, DEFAULT_CURVE_GROUP),
      IborIndexCurveId.of(IborIndices.GBP_LIBOR_3M, DEFAULT_CURVE_GROUP),
      OvernightIndexCurveId.of(OvernightIndices.USD_FED_FUND, DEFAULT_CURVE_GROUP),
      FxRateId.of(Currency.USD, Currency.GBP),
      QuoteId.of(StandardId.of("OG-Future", "Eurex-FGBL-Mar14")),
      QuoteId.of(StandardId.of("OG-FutOpt", "Eurex-OGBL-Mar14-C150")),
      QuoteId.of(StandardId.of("OG-Future", "CME-ED-Mar14")),
      QuoteId.of(StandardId.of("OG-Future", "Ibor-USD-LIBOR-3M-Mar15")),
      QuoteId.of(StandardId.of("OG-Future", "Ibor-USD-LIBOR-3M-Jun15")),
      QuoteId.of(StandardId.of("OG-Future", "CME-F1U-Mar15")),
      QuoteId.of(StandardId.of("OG-Future", "CME-F1U-Jun15")),
      IsdaYieldCurveInputsId.of(Currency.USD),
      IsdaSingleNameCreditCurveInputsId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP10"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.USD,
              RestructuringClause.NO_RESTRUCTURING_2014)),
      IsdaSingleNameCreditCurveInputsId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP02"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.USD,
              RestructuringClause.NO_RESTRUCTURING_2014)),
      IsdaSingleNameCreditCurveInputsId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP01"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.USD,
              RestructuringClause.NO_RESTRUCTURING_2014)),
      IsdaSingleNameCreditCurveInputsId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP11"),
              SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
              Currency.EUR,
              RestructuringClause.MOD_MOD_RESTRUCTURING_2014)),
      IsdaSingleNameCreditCurveInputsId.of(
          SingleNameReferenceInformation.of(
              MarkitRedCode.id("COMP11"),
              SeniorityLevel.SUBORDINATE_LOWER_TIER_2,
              Currency.EUR,
              RestructuringClause.MOD_MOD_RESTRUCTURING_2014)),
      IsdaIndexCreditCurveInputsId.of(
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
    File tempFile = File.createTempFile(ExampleMarketDataBuilderTest.class.getSimpleName(), ".jar");
    try (FileOutputStream tempFileOut = new FileOutputStream(tempFile)) {
      try (JarOutputStream zipFileOut = new JarOutputStream(tempFileOut)) {
        File diskRoot = new File(EXAMPLE_MARKET_DATA_DIRECTORY_ROOT);
        appendToJar(diskRoot, "zip-data", diskRoot, zipFileOut);
      }
    }

    // Obtain a classloader which can see this JAR
    ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] {tempFile.toURI().toURL()})) {
      // Test automatically finding the resource inside the JAR
      Thread.currentThread().setContextClassLoader(classLoader);
      assertBuilder(ExampleMarketDataBuilder.ofResource("zip-data", classLoader));
    } finally {
      Thread.currentThread().setContextClassLoader(originalContextClassLoader);
      try {
        Files.deleteIfExists(tempFile.toPath());
      } catch (IOException ex) {
        // ignore
      }
    }
  }

  public void test_ofPath() {
    Path rootPath = new File(EXAMPLE_MARKET_DATA_DIRECTORY_ROOT).toPath();
    ExampleMarketDataBuilder builder = ExampleMarketDataBuilder.ofPath(rootPath);
    assertBuilder(builder);
  }

  public void test_ofPath_with_spaces() {
    Path rootPath = new File(TEST_SPACES_DIRECTORY_ROOT).toPath();
    ExampleMarketDataBuilder builder = ExampleMarketDataBuilder.ofPath(rootPath);

    MarketEnvironment snapshot = builder.buildSnapshot(LocalDate.of(2015, 1, 1));
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

    MarketEnvironment snapshot = builder.buildSnapshot(MARKET_DATA_DATE);
    assertEquals(snapshot.getTimeSeries().size(), 1);
  }

  //-------------------------------------------------------------------------
  private void assertBuilder(ExampleMarketDataBuilder builder) {
    MarketEnvironment snapshot = builder.buildSnapshot(MARKET_DATA_DATE);

    assertEquals(MARKET_DATA_DATE, snapshot.getValuationDate().getSingleValue());

    for (ObservableId id : TIME_SERIES) {
      assertFalse(snapshot.getTimeSeries(id).isEmpty(), "Time-series not found: " + id);
    }
    assertEquals(snapshot.getTimeSeries().size(), TIME_SERIES.size(),
        Messages.format("Snapshot contained unexpected time-series: {}",
            Sets.difference(snapshot.getTimeSeries().keySet(), TIME_SERIES)));

    for (MarketDataId<?> id : VALUES) {
      assertTrue(snapshot.containsValue(id), "Id not found: " + id);
    }
    MarketDataBox<CurveGroup> curveGroupBox = snapshot.getValue(CurveGroupId.of(DEFAULT_CURVE_GROUP));
    assertTrue(curveGroupBox.isSingleValue());
    CurveGroup curveGroup = curveGroupBox.getSingleValue();
    assertTrue(curveGroup.findDiscountCurve(Currency.USD).isPresent());
    assertTrue(curveGroup.findDiscountCurve(Currency.GBP).isPresent());
    assertTrue(curveGroup.findForwardCurve(IborIndices.USD_LIBOR_3M).isPresent());
    assertTrue(curveGroup.findForwardCurve(IborIndices.GBP_LIBOR_3M).isPresent());
    assertTrue(curveGroup.findForwardCurve(IborIndices.USD_LIBOR_6M).isPresent());
    assertTrue(curveGroup.findForwardCurve(OvernightIndices.USD_FED_FUND).isPresent());

    assertEquals(snapshot.getValues().size(), VALUES.size(),
        Messages.format("Snapshot contained unexpected market data: {}",
            Sets.difference(snapshot.getValues().keySet(), VALUES)));

    MarketDataRules expectedRules = MarketDataRules.anyTarget(
        MarketDataMappingsBuilder.create()
            .curveGroup(CurveGroupName.of("Default"))
            .build());
    assertEquals(builder.rules(), expectedRules);
  }

  //-------------------------------------------------------------------------
  // build jar file
  // jar files use forward-slash on all operating systems
  // directories always have a trailing forward-slash
  // there must be no slash at the root
  private void appendToJar(File sourceRootDir, String destRootPath, File currentFile, JarOutputStream jarOutput)
      throws IOException {
    if (currentFile.isDirectory()) {
      String entryName = getEntryName(sourceRootDir, destRootPath, currentFile) + '/';
      jarOutput.putNextEntry(new JarEntry(entryName));
      jarOutput.closeEntry();
      for (File content : currentFile.listFiles()) {
        appendToJar(sourceRootDir, destRootPath, content, jarOutput);
      }
    } else {
      String entryName = getEntryName(sourceRootDir, destRootPath, currentFile);
      jarOutput.putNextEntry(new JarEntry(entryName));
      try (FileInputStream fileIn = new FileInputStream(currentFile)) {
        byte[] b = new byte[1024];
        int len;
        while ((len = fileIn.read(b)) != -1) {
          jarOutput.write(b, 0, len);
        }
      }
      jarOutput.closeEntry();
    }
  }

  private String getEntryName(File sourceRootDir, String destRootPath, File currentFile) {
    String relativePath = currentFile.getAbsolutePath().substring(sourceRootDir.getAbsolutePath().length());
    return destRootPath + relativePath.replace('\\', '/');
  }

}
