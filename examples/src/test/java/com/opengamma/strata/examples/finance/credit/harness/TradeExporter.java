/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit.harness;

import static com.opengamma.strata.examples.finance.credit.harness.TestHarness.TradeFactory.withCompany01;
import static com.opengamma.strata.examples.finance.credit.harness.TestHarness.TradeFactory.withCompany02;
import static com.opengamma.strata.examples.finance.credit.harness.TestHarness.TradeFactory.withIndex0001;

import java.util.List;

import org.joda.beans.ser.JodaBeanSer;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.examples.report.TradePortfolio;
import com.opengamma.strata.finance.Trade;

public class TradeExporter {

  public void test_export() {
    List<Trade> trades = ImmutableList.of(
        withCompany01().getTrade(),
        withCompany02().getTrade(),
        withIndex0001().getTrade());

    TradePortfolio portfolio = TradePortfolio.of(trades);

    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(portfolio));
  }

}
