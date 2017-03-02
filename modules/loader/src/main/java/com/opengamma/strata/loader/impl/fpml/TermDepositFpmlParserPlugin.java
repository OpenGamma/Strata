/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.impl.fpml;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.loader.fpml.FpmlDocument;
import com.opengamma.strata.loader.fpml.FpmlParserPlugin;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;

/**
 * FpML parser for Term Deposits.
 * <p>
 * This parser handles the subset of FpML necessary to populate the trade model.
 */
final class TermDepositFpmlParserPlugin
    implements FpmlParserPlugin {
  // this class is loaded by ExtendedEnum reflection

  /**
   * The singleton instance of the parser.
   */
  public static final TermDepositFpmlParserPlugin INSTANCE = new TermDepositFpmlParserPlugin();

  /**
   * Restricted constructor.
   */
  private TermDepositFpmlParserPlugin() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(FpmlDocument document, XmlElement tradeEl) {
    // supported elements:
    // 'payerPartyReference'
    // 'receiverPartyReference'
    // 'startDate'
    // 'maturityDate'
    // 'principal'
    // 'fixedRate'
    // 'dayCountFraction'
    // ignored elements:
    // 'payerAccountReference?'
    // 'receiverAccountReference?'
    // 'interest?'
    // rejected elements:
    // 'features?'
    // 'payment*'
    TradeInfoBuilder tradeInfoBuilder = document.parseTradeInfo(tradeEl);
    XmlElement termEl = tradeEl.getChild("termDeposit");
    document.validateNotPresent(termEl, "features");
    document.validateNotPresent(termEl, "payment");
    TermDeposit.Builder termBuilder = TermDeposit.builder();
    // pay/receive and counterparty
    PayReceive payReceive = document.parsePayerReceiver(termEl, tradeInfoBuilder);
    termBuilder.buySell(BuySell.ofBuy(payReceive.isPay()));
    // start date
    termBuilder.startDate(document.parseDate(termEl.getChild("startDate")));
    // maturity date
    termBuilder.endDate(document.parseDate(termEl.getChild("maturityDate")));
    // principal
    CurrencyAmount principal = document.parseCurrencyAmount(termEl.getChild("principal"));
    termBuilder.currency(principal.getCurrency());
    termBuilder.notional(principal.getAmount());
    // fixed rate
    termBuilder.rate(document.parseDecimal(termEl.getChild("fixedRate")));
    // day count
    termBuilder.dayCount(document.parseDayCountFraction(termEl.getChild("dayCountFraction")));

    return TermDepositTrade.builder()
        .info(tradeInfoBuilder.build())
        .product(termBuilder.build())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "termDeposit";
  }

}
