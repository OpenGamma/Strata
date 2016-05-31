/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.impl.fpml;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.loader.fpml.FpmlDocument;
import com.opengamma.strata.loader.fpml.FpmlParseException;
import com.opengamma.strata.loader.fpml.FpmlParserPlugin;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swaption.CashSettlement;
import com.opengamma.strata.product.swaption.CashSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * FpML parser for Swaptions.
 * <p>
 * This parser handles the subset of FpML necessary to populate the swaption trade model.
 */
final class SwaptionFpmlParserPlugin implements FpmlParserPlugin {

  /**
   * The singleton instance of the parser.
   */
  public static final SwaptionFpmlParserPlugin INSTANCE = new SwaptionFpmlParserPlugin();

  /**
   * Restricted constructor.
   */
  private SwaptionFpmlParserPlugin() {
  }

//-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(FpmlDocument document, XmlElement tradeEl) {
    // supported elements:
    //  'swaption'
    //  'swaption/buyerPartyReference'
    //  'swaption/sellerPartyReference'
    //  'swaption/premium/payerPartyReference'
    //  'swaption/premium/receiverPartyReference'
    //  'swaption/premium/paymentAmount'
    //  'swaption/premium/paymentDate'
    //  'swaption/europeanExercise'
    //  'swaption/europeanExercise/expirationDate'
    //  'swaption/europeanExercise/expirationDate/adjustableDate'
    //  'swaption/europeanExercise/expirationDate/adjustableDate/unadjustedDate'
    //  'swaption/europeanExercise/expirationDate/adjustableDate/dateAdjustments'
    //  'swaption/europeanExercise/expirationTime
    //  'swaption/swap'
    // ignored elements:
    //  'Product.model?'
    //  'swaption/calculationAgent'
    //  'swaption/assetClass'
    //  'swaption/primaryAssestClass'
    //  'swaption/productId'
    //  'swaption/productType'
    //  'swaption/secondaryAssetClass'
    //  'swaption/sellerAccountReference'
    //  'swaption/sellerPartyReference'
    //  'swaption/swaptionAdjustedDates'
    //  'swaption/swaptionStraddle'
    TradeInfoBuilder tradeInfoBuilder = document.parseTradeInfo(tradeEl);

    XmlElement swaptionEl = tradeEl.getChild("swaption");
    XmlElement europeanExerciseEl = swaptionEl.getChild("europeanExercise");
    XmlElement expirationTimeEl = europeanExerciseEl.getChild("expirationTime");

    // Parse the premium, expiry date, expiry time and expiry zone, longShort and swaption settlement.
    Payment premium = parsePremium(swaptionEl, document, tradeInfoBuilder);
    AdjustableDate expiryDate = parseExpiryDate(europeanExerciseEl, document);
    LocalTime expiryTime = parseExpiryTime(expirationTimeEl, document);
    ZoneId expiryZone = parseExpiryZone(expirationTimeEl, document);
    LongShort longShort = parseLongShort(swaptionEl, document, tradeInfoBuilder);
    SwaptionSettlement swaptionSettlement = parseSettlement(swaptionEl, document);

    //Re use the Swap FpML parser to parse the underlying swap on this swaption.
    SwapFpmlParserPlugin INSTANCE = SwapFpmlParserPlugin.INSTANCE;
    Swap swap = INSTANCE.parseSwap(document, swaptionEl, tradeInfoBuilder);

    Swaption swaption = Swaption.builder()
        .expiryDate(expiryDate)
        .expiryZone(expiryZone)
        .expiryTime(expiryTime)
        .longShort(longShort)
        .swaptionSettlement(swaptionSettlement)
        .underlying(swap)
        .build();

    return SwaptionTrade.builder()
        .info(tradeInfoBuilder.build())
        .product(swaption)
        .premium(premium)
        .build();
  }

  private AdjustableDate parseExpiryDate(XmlElement europeanExerciseEl, FpmlDocument document) {
    XmlElement expirationDate = europeanExerciseEl.getChild("expirationDate");
    return expirationDate.findChild("adjustableDate")
        .map(el -> document.parseAdjustableDate(el)).get();
  }

  private LocalTime parseExpiryTime(XmlElement expirationTimeEl, FpmlDocument document) {
    return document.parseTime(expirationTimeEl.getChild("hourMinuteTime"));
  }

  private ZoneId parseExpiryZone(XmlElement expirationTimeEl, FpmlDocument document) {
    String businessCenter = expirationTimeEl.getChild("businessCenter").getContent();
    Optional<ZoneId> optionalZoneId = document.getZoneId(businessCenter);
    if (!optionalZoneId.isPresent()) {
      throw new FpmlParseException("Unknown businessCenter" + " attribute value: " + businessCenter);
    }
    return optionalZoneId.get();
  }

  private Payment parsePremium(XmlElement swaptionEl, FpmlDocument document, TradeInfoBuilder tradeInfoBuilder) {
    XmlElement premiumEl = swaptionEl.getChild("premium");
    PayReceive payReceive = document.parsePayerReceiver(premiumEl, tradeInfoBuilder);
    XmlElement paymentAmountEl = premiumEl.getChild("paymentAmount");
    CurrencyAmount ccyAmount = document.parseCurrencyAmount(paymentAmountEl);
    ccyAmount = payReceive.isPay() ? ccyAmount.negated() : ccyAmount;
    AdjustableDate paymentDate = premiumEl.findChild("paymentDate")
        .map(el -> document.parseAdjustableDate(el)).get();
    return Payment.of(ccyAmount, paymentDate.getUnadjusted());
  }

  private LongShort parseLongShort(XmlElement swaptionEl, FpmlDocument document, TradeInfoBuilder tradeInfoBuilder) {
    BuySell buySell = document.parseBuyerSeller(swaptionEl, tradeInfoBuilder);
    return buySell.isBuy() ? LongShort.LONG : LongShort.SHORT;
  }

  private SwaptionSettlement parseSettlement(XmlElement swaptionEl, FpmlDocument document) {
    Optional<String> optionalCashSettlement = swaptionEl.findAttribute("cashSettlement");
    if (optionalCashSettlement.isPresent()) {
      CashSettlement.Builder builder = CashSettlement.builder();
      XmlElement cashSettlement = swaptionEl.getChild("cashSettlement");
      LocalDate settlementDate = document.parseAdjustedRelativeDateOffset(cashSettlement).getUnadjusted();
      if (cashSettlement.findAttribute("cashPriceAlternateMethod").isPresent()) {
        builder = builder.cashSettlementMethod(CashSettlementMethod.CASH_PRICE);
      } else if (cashSettlement.findAttribute("parYieldCurveUnadjustedMethod").isPresent() ||
          cashSettlement.findAttribute("parYieldCurveAadjustedMethod").isPresent()) {
        builder = builder.cashSettlementMethod(CashSettlementMethod.PAR_YIELD);
      } else if (cashSettlement.findAttribute("zeroCouponYieldAdjustedMethod").isPresent()) {
        builder = builder.cashSettlementMethod(CashSettlementMethod.ZERO_COUPON_YIELD);
      }
      else {
        throw new FpmlParseException("Invalid Cash Settlement Method");
      }
      return builder
          .settlementDate(settlementDate)
          .build();
    } else { //If cash settlement is not specified, then physical settlement is applicable.
      return PhysicalSettlement.DEFAULT;
    }
  }

//-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "swaption";
  }

}
