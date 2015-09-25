/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.impl.loader;

import java.time.LocalDate;

import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.fx.FxSingle;
import com.opengamma.strata.finance.fx.FxSwap;
import com.opengamma.strata.finance.fx.FxSwapTrade;
import com.opengamma.strata.finance.loader.FpmlDocument;
import com.opengamma.strata.finance.loader.FpmlParseException;
import com.opengamma.strata.finance.loader.FpmlTradeParser;

/**
 * FpML parser for FX swap.
 * <p>
 * This parser handles the subset of FpML necessary to populate the trade model.
 */
final class FxSwapFpmlTradeParser
    implements FpmlTradeParser {
  // this class is loaded by ExtendedEnum reflection

  /**
   * The singleton instance of the parser.
   */
  public static final FxSwapFpmlTradeParser INSTANCE = new FxSwapFpmlTradeParser();

  /**
   * Restricted constructor.
   */
  private FxSwapFpmlTradeParser() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(XmlElement tradeEl, FpmlDocument document) {
    // supported elements:
    // 'nearLeg'
    // 'farLeg'
    TradeInfo.Builder tradeInfoBuilder = document.parseTradeInfo(tradeEl);
    XmlElement fxEl = tradeEl.getChild("fxSwap");
    FxSingle nearLeg = parseLeg(fxEl.getChild("nearLeg"), document, tradeInfoBuilder);
    FxSingle farLeg = parseLeg(fxEl.getChild("farLeg"), document, tradeInfoBuilder);
    return FxSwapTrade.builder()
        .tradeInfo(tradeInfoBuilder.build())
        .product(FxSwap.of(nearLeg, farLeg))
        .build();
  }

  private FxSingle parseLeg(XmlElement legEl, FpmlDocument document, TradeInfo.Builder tradeInfoBuilder) {
    // supported elements:
    // 'exchangedCurrency1/paymentAmount'
    // 'exchangedCurrency2/paymentAmount'
    // 'valueDate'
    // ignored elements:
    // 'dealtCurrency?'
    // 'exchangeRate'
    // rejected elements:
    // 'nonDeliverableSettlement?'
    // 'currency1ValueDate'
    // 'currency2ValueDate'
    document.validateNotPresent(legEl, "currency1ValueDate");
    document.validateNotPresent(legEl, "currency2ValueDate");
    document.validateNotPresent(legEl, "nonDeliverableSettlement");
    XmlElement curr1El = legEl.getChild("exchangedCurrency1");
    XmlElement curr2El = legEl.getChild("exchangedCurrency2");
    // pay/receive and counterparty
    PayReceive curr1PayReceive = document.parsePayerReceiver(curr1El, tradeInfoBuilder);
    PayReceive curr2PayReceive = document.parsePayerReceiver(curr2El, tradeInfoBuilder);
    if (curr1PayReceive == curr2PayReceive) {
      throw new FpmlParseException("FX single leg currencies must not have same Pay/Receive direction");
    }
    // amount
    CurrencyAmount curr1Amount = document.parseCurrencyAmount(curr1El.getChild("paymentAmount"));
    CurrencyAmount curr2Amount = document.parseCurrencyAmount(curr2El.getChild("paymentAmount"));
    if (curr1PayReceive == PayReceive.PAY) {
      curr1Amount = curr1Amount.negative();
      curr2Amount = curr2Amount.positive();
    } else {
      curr1Amount = curr1Amount.positive();
      curr2Amount = curr2Amount.negative();
    }
    // payment date
    LocalDate valueDate = document.parseDate(legEl.getChild("valueDate"));
    // result
    return FxSingle.of(curr1Amount, curr2Amount, valueDate);
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "fxSwap";
  }

}
