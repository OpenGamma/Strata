/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.strata.examples.finance;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.examples.marketdata.credit.markit.MarkitRedCode;
import com.opengamma.strata.examples.report.TradePortfolio;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.type.CdsConventions;
import org.joda.beans.ser.JodaBeanSer;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.finance.credit.RestructuringClause.NO_RESTRUCTURING_2014;
import static com.opengamma.strata.finance.credit.SeniorityLevel.SENIOR_UNSECURED_FOREIGN;

/**
 * An example application showing how to load the example portfolio from the classpath
 * as well as build an equivalent portfolio using the Strata API.
 */
public class CdsTradeExample {

  /**
   * Runs the example, serializing and deserializing the portfolio and printing to the screen.
   *
   * @param args  ignored
   */
  public static void main(String[] args) {

    String xmlString = serializePretty(portfolio);
    System.out.println("Successfully serialized " + portfolio.toString());
    System.out.println("Serialized XML is:\n" + xmlString);

    TradePortfolio deserializedPortfolioFromString = deserialize(xmlString);
    System.out.println("Successfully deserialized from string " + deserializedPortfolioFromString.toString());

    TradePortfolio deserializedPortfolioFromFile = deserialize(loadExamplePortfolio());
    System.out.println("Successfully deserialized from file " + deserializedPortfolioFromFile.toString());

  }

  public static String serializeCompact(TradePortfolio portfolio) {
    return JodaBeanSer.COMPACT.xmlWriter().write(portfolio);
  }

  public static String serializePretty(TradePortfolio portfolio) {
    return JodaBeanSer.PRETTY.xmlWriter().write(portfolio);
  }

  public static TradePortfolio deserialize(String xmlString) {
    return JodaBeanSer.COMPACT.xmlReader().read(xmlString, TradePortfolio.class);
  }

  public static String loadExamplePortfolio() {
    String resourceName = "example-portfolios/cds-portfolio.xml";
    try {
      ResourceLocator resourceLocator = ResourceLocator.streamOfClasspathResources(resourceName).findFirst().get();
      return CharStreams.toString(resourceLocator.getCharSource().openStream());
    } catch (IOException e) {
      throw new RuntimeException("Error loading from file " + resourceName, e);
    }
  }

  public static CdsTrade comp01 =
      CdsConventions.NORTH_AMERICAN_USD
          .toSingleNameTrade(
              LocalDate.of(2014, 9, 22),
              LocalDate.of(2019, 12, 20),
              BUY,
              100_000_000D,
              0.0100,
              MarkitRedCode.id("COMP01"),
              SENIOR_UNSECURED_FOREIGN,
              NO_RESTRUCTURING_2014,
              3_694_117.73D,
              LocalDate.of(2014, 10, 21));

  public static CdsTrade comp02 =
      CdsConventions.NORTH_AMERICAN_USD
          .toSingleNameTrade(
              LocalDate.of(2014, 9, 22),
              LocalDate.of(2019, 12, 20),
              BUY,
              100_000_000D,
              0.0500,
              MarkitRedCode.id("COMP02"),
              SENIOR_UNSECURED_FOREIGN,
              NO_RESTRUCTURING_2014,
              -1_370_582.00D,
              LocalDate.of(2014, 10, 21));

  public static CdsTrade index0001 =
      CdsConventions.NORTH_AMERICAN_USD
          .toIndexTrade(
              LocalDate.of(2014, 3, 20),
              LocalDate.of(2019, 6, 20),
              BUY,
              100_000_000D,
              0.0500,
              MarkitRedCode.id("INDEX0001"),
              22,
              4,
              2_000_000D,
              LocalDate.of(2014, 10, 21));

  public static List<Trade> trades = ImmutableList.of(
      comp01,
      comp02,
      index0001);

  public static TradePortfolio portfolio = TradePortfolio.of(trades);

}
