/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.impl.loader;

import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.loader.FpmlDocument;
import com.opengamma.strata.finance.loader.FpmlTradeParser;
import com.opengamma.strata.finance.payment.BulletPayment;
import com.opengamma.strata.finance.payment.BulletPaymentTrade;

/**
 * FpML parser for Bullet Payments.
 * <p>
 * This parser handles the subset of FpML necessary to populate the trade model.
 */
final class BulletPaymentFpmlTradeParser
    implements FpmlTradeParser {
  // this class is loaded by ExtendedEnum reflection

  /**
   * The singleton instance of the parser.
   */
  public static final BulletPaymentFpmlTradeParser INSTANCE = new BulletPaymentFpmlTradeParser();

  /**
   * Restricted constructor.
   */
  private BulletPaymentFpmlTradeParser() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(XmlElement tradeEl, FpmlDocument document) {
    // supported elements:
    // 'payment/payerPartyReference'
    // 'payment/receiverPartyReference'
    // 'payment/paymentAmount'
    // 'payment/paymentDate?'
    // ignored elements:
    // 'payment/payerAccountReference?'
    // 'payment/receiverAccountReference?'
    // 'payment/paymentType?'
    // 'payment/settlementInformation?'
    // 'payment/discountFactor?'
    // 'payment/presentValueAmount?'
    TradeInfo.Builder tradeInfoBuilder = document.parseTradeInfo(tradeEl);
    XmlElement bulletEl = tradeEl.getChild("bulletPayment");
    XmlElement paymentEl = bulletEl.getChild("payment");
    BulletPayment.Builder bulletBuilder = BulletPayment.builder();
    // pay/receive and counterparty
    bulletBuilder.payReceive(document.parsePayerReceiver(paymentEl, tradeInfoBuilder));
    // payment date
    bulletBuilder.date(document.parseAdjustableDate(paymentEl.getChild("paymentDate")));
    // amount
    bulletBuilder.value(document.parseCurrencyAmount(paymentEl.getChild("paymentAmount")));

    return BulletPaymentTrade.builder()
        .tradeInfo(tradeInfoBuilder.build())
        .product(bulletBuilder.build())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "bulletPayment";
  }

}
