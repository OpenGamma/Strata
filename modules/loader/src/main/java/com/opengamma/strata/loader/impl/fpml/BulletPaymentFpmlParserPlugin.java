/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.impl.fpml;

import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.loader.fpml.FpmlDocument;
import com.opengamma.strata.loader.fpml.FpmlParserPlugin;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;

/**
 * FpML parser for Bullet Payments.
 * <p>
 * This parser handles the subset of FpML necessary to populate the trade model.
 */
final class BulletPaymentFpmlParserPlugin
    implements FpmlParserPlugin {
  // this class is loaded by ExtendedEnum reflection

  /**
   * The singleton instance of the parser.
   */
  public static final BulletPaymentFpmlParserPlugin INSTANCE = new BulletPaymentFpmlParserPlugin();

  /**
   * Restricted constructor.
   */
  private BulletPaymentFpmlParserPlugin() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(FpmlDocument document, XmlElement tradeEl) {
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
    TradeInfoBuilder tradeInfoBuilder = document.parseTradeInfo(tradeEl);
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
        .info(tradeInfoBuilder.build())
        .product(bulletBuilder.build())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "bulletPayment";
  }

}
