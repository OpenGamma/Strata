/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.examples.finance.credit.api.TradeSource;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.credit.common.RestructuringClause;
import com.opengamma.strata.finance.credit.common.SeniorityLevel;
import com.opengamma.strata.finance.credit.markit.RedCode;
import com.opengamma.strata.finance.credit.type.StandardCdsConventions;
import com.opengamma.strata.finance.credit.type.StandardCdsTemplate;

import java.time.LocalDate;
import java.time.Period;

public class ExampleTradeSource implements TradeSource {

  private ExampleTradeSource() {
  }

  @Override
  public ImmutableList<Trade> trades() {
    return ImmutableList.of(
        StandardCdsTemplate
            .of(StandardCdsConventions.northAmericanUsd())
            .toSingleNameTrade(
                StandardId.of("tradeid", "62726762"),
                LocalDate.of(2014, 9, 22),
                LocalDate.of(2019, 12, 20),
                BuySell.BUY,
                100_000_000D,
                0.0100,
                RedCode.id("AH98A7"),
                SeniorityLevel.SeniorUnSec,
                RestructuringClause.XR,
                3_694_117.73D,
                LocalDate.of(2014, 10, 21)
            )
    );
  }

  public static TradeSource of() {
    return new ExampleTradeSource();
  }
}
