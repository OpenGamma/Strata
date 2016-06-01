/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.credit.RestructuringClause.NO_RESTRUCTURING_2014;
import static com.opengamma.strata.product.credit.SeniorityLevel.SENIOR_UNSECURED_FOREIGN;

import java.time.LocalDate;
import java.util.List;

import org.joda.beans.ser.JodaBeanSer;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.Unchecked;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.examples.marketdata.credit.markit.MarkitRedCode;
import com.opengamma.strata.examples.report.TradePortfolio;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;
import com.opengamma.strata.product.credit.type.CdsConventions;

/**
 * An example application showing how to load the example portfolio from the classpath
 * as well as build an equivalent portfolio using the API.
 */
public class CdsTradeExample {

  /**
   * Resource name.
   */
  private static final String CDS_PORTFOLIO_XML = "example-portfolios/cds-portfolio.xml";

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
    return Unchecked.wrap(() -> ResourceLocator.ofClasspath(CDS_PORTFOLIO_XML).getCharSource().read());
  }

  public static CdsTrade comp01 =
      CdsConventions.USD_NORTH_AMERICAN
          .toTrade(
              LocalDate.of(2014, 9, 22),
              LocalDate.of(2019, 12, 20),
              BUY,
              100_000_000d,
              0.0100,
              SingleNameReferenceInformation.of(
                  MarkitRedCode.id("COMP01"),
                  SENIOR_UNSECURED_FOREIGN,
                  Currency.USD,
                  NO_RESTRUCTURING_2014),
              3_694_117.73d,
              LocalDate.of(2014, 10, 21));

  public static CdsTrade comp02 =
      CdsConventions.USD_NORTH_AMERICAN
          .toTrade(
              LocalDate.of(2014, 9, 22),
              LocalDate.of(2019, 12, 20),
              BUY,
              100_000_000d,
              0.0500,
              SingleNameReferenceInformation.of(
                  MarkitRedCode.id("COMP02"),
                  SENIOR_UNSECURED_FOREIGN,
                  Currency.USD,
                  NO_RESTRUCTURING_2014),
              -1_370_582.00d,
              LocalDate.of(2014, 10, 21));

  public static CdsTrade index0001 =
      CdsConventions.USD_NORTH_AMERICAN
          .toTrade(
              LocalDate.of(2014, 3, 20),
              LocalDate.of(2019, 6, 20),
              BUY,
              100_000_000d,
              0.0500,
              IndexReferenceInformation.of(MarkitRedCode.id("INDEX0001"), 22, 4),
              2_000_000d,
              LocalDate.of(2014, 10, 21));

  public static List<Trade> trades = ImmutableList.of(
      comp01,
      comp02,
      index0001);

  public static TradePortfolio portfolio = TradePortfolio.of(trades);

}
