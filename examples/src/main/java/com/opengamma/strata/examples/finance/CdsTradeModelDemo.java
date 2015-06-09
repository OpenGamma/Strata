/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.examples.report.TradePortfolio;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.common.RedCode;
import com.opengamma.strata.finance.credit.general.reference.SeniorityLevel;
import com.opengamma.strata.finance.credit.protection.RestructuringClause;
import com.opengamma.strata.finance.credit.type.StandardCdsConventions;
import com.opengamma.strata.finance.credit.type.StandardCdsTemplate;
import org.joda.beans.ser.JodaBeanSer;

import java.time.LocalDate;
import java.time.Period;
import java.util.stream.Collectors;

/**
 * Demonstrate use of the API for credit default swaps.
 * <p>
 * This class exists for demonstration purposes to aid with understanding credit default swaps.
 * It is not intended to be used in a production environment.
 */
public class CdsTradeModelDemo {

  /**
   * Launch demo, no arguments needed.
   *
   * @param args no arguments needed
   */
  public static void main(String[] args) {
    CdsTradeModelDemo demo = new CdsTradeModelDemo();

    TradePortfolio trades = TradePortfolio.of(
        ImmutableList.of(
            demo.simpleSingleNameUsd(),
            demo.simpleSingleNameEur(),
            demo.simpleIndex()
        )
    );

    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(trades));

  }

  //-----------------------------------------------------------------------
  private CdsTrade simpleSingleNameUsd() {
    return StandardCdsTemplate
        .of(StandardCdsConventions.northAmericanUsd())
        .toSingleNameTrade(
            StandardId.of("tradeid", "62726762"),
            LocalDate.of(2014, 10, 16),
            Period.ofYears(5),
            BuySell.BUY,
            100_000_000D,
            0.0100,
            RedCode.of("AH98A7"),
            "Ford Motor Company",
            SeniorityLevel.SeniorUnSec,
            RestructuringClause.XR,
            3_694_117.73D,
            LocalDate.of(2014, 10, 21)
        );
  }

  public CdsTrade simpleSingleNameEur() {
    return StandardCdsTemplate
        .of(StandardCdsConventions.europeanUsd())
        .toSingleNameTrade(
            StandardId.of("tradeid", "62726763"),
            LocalDate.of(2014, 1, 1),
            Period.ofYears(5),
            BuySell.BUY,
            1_000_000D,
            0.0050,
            RedCode.of("D28123"),
            "Addidas",
            SeniorityLevel.SeniorUnSec,
            RestructuringClause.XR
        );
  }

  public CdsTrade simpleIndex() {
    return StandardCdsTemplate
        .of(StandardCdsConventions.northAmericanUsd())
        .toIndexTrade(
            StandardId.of("tradeid", "62726764"),
            LocalDate.of(2014, 1, 1),
            Period.ofYears(5),
            BuySell.BUY,
            10_000_000D,
            0.0100,
            RedCode.of("D28123245"),
            "Dow Jones CDX.NA.IG",
            3,
            1,
            RestructuringClause.XR,
            50632D,
            LocalDate.of(2014, 1, 4)
        );
  }

}
