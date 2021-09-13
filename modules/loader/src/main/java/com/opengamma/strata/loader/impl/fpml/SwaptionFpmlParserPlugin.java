/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.impl.fpml;

import static com.opengamma.strata.collect.Guavate.firstNonEmpty;
import static com.opengamma.strata.collect.Guavate.inOptional;
import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.AdjustableDates;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
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
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionExercise;
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
    //  'swaption/europeanExercise/expirationDate/adjustableDate/unadjustedDate'
    //  'swaption/europeanExercise/expirationDate/adjustableDate/dateAdjustments'
    //  'swaption/europeanExercise/relevantUnderlyingDate/relativeDates'
    //  'swaption/europeanExercise/expirationTime'
    //  'swaption/americanExercise/commencementDate/adjustableDate/unadjustedDate'
    //  'swaption/americanExercise/expirationDate/adjustableDate/unadjustedDate'
    //  'swaption/europeanExercise/expirationDate/adjustableDate/dateAdjustments'
    //  'swaption/americanExercise/relevantUnderlyingDate/relativeDates'
    //  'swaption/americanExercise/expirationTime'
    //  'swaption/americanExercise/latestExerciseTime'
    //  'swaption/bermudaExercise/bermudaExerciseDates/adjustableDates'
    //  'swaption/bermudaExercise/expirationTime'
    //  'swaption/bermudaExercise/latestExerciseTime'
    //  'swaption/swap'
    // ignored elements:
    //  'Product.model?'
    //  'swaption/xxxExercise/earliestExerciseTime'
    //  'swaption/xxxExercise/multipleExercise'
    //  'swaption/xxxExercise/partialExercise'
    //  'swaption/xxxExercise/exerciseFee'
    //  'swaption/xxxExercise/exerciseFeeSchedule'
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
    // rejected elements:
    //  'swaption/xxxExercise/relevantUnderlyingDate/adjustableDates'
    //  'swaption/europeanExercise/expirationDate/relativeDate'
    //  'swaption/americanExercise/commencementDate/relativeDate'
    //  'swaption/bermudaExercise/bermudaExerciseDates/relativeDates'
    TradeInfoBuilder tradeInfoBuilder = document.parseTradeInfo(tradeEl);

    XmlElement swaptionEl = tradeEl.getChild("swaption");
    XmlElement exerciseEl = firstNonEmpty(
        () -> swaptionEl.findChild("europeanExercise"),
        () -> swaptionEl.findChild("americanExercise"),
        () -> swaptionEl.findChild("bermudaExercise"))
            .orElseGet(() -> swaptionEl.getChild("europeanExercise")); // trigger exception if not found

    // advanced exercise info
    DaysAdjustment swapStartOffset = parseSwapStartOffset(document, exerciseEl);
    SwaptionExercise exercise = null;
    AdjustableDate expiryDate;
    // exercise dates
    if (exerciseEl.getName().equals("bermudaExercise")) {
      AdjustableDates adjDates = parseBermudaDates(exerciseEl, document);
      LocalDate expiry = adjDates.getUnadjusted().get(adjDates.getUnadjusted().size() - 1);
      expiryDate = AdjustableDate.of(expiry, adjDates.getAdjustment());
      exercise = SwaptionExercise.ofBermudan(adjDates, swapStartOffset);

    } else if (exerciseEl.getName().equals("americanExercise")) {
      expiryDate = parseExpirationDate(exerciseEl, document);
      LocalDate commencementDate = parseCommencementDate(exerciseEl, document);
      exercise = SwaptionExercise.ofAmerican(
          commencementDate, expiryDate.getUnadjusted(), expiryDate.getAdjustment(), swapStartOffset);

    } else {
      expiryDate = parseExpirationDate(exerciseEl, document);
      exercise = SwaptionExercise.ofEuropean(expiryDate, swapStartOffset);
    }

    // expiry time
    XmlElement expirationTimeEl = firstNonEmpty(
        () -> exerciseEl.findChild("expirationTime"),
        () -> exerciseEl.findChild("latestExerciseTime"))
            .orElseGet(() -> exerciseEl.getChild("expirationTime")); // trigger exception if not found
    LocalTime expiryTime = parseExpiryTime(expirationTimeEl, document);
    ZoneId expiryZone = parseExpiryZone(expirationTimeEl, document);

    // parse the premium, longShort and swaption settlement
    AdjustablePayment premium = parsePremium(swaptionEl, document, tradeInfoBuilder);
    LongShort longShort = parseLongShort(swaptionEl, document, tradeInfoBuilder);
    SwaptionSettlement swaptionSettlement = parseSettlement(swaptionEl, document);

    // re use the Swap FpML parser to parse the underlying swap on this swaption
    SwapFpmlParserPlugin swapParser = SwapFpmlParserPlugin.INSTANCE;
    Swap swap = swapParser.parseSwap(document, swaptionEl, tradeInfoBuilder);

    Swaption swaption = Swaption.builder()
        .exerciseInfo(exercise)
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

  private DaysAdjustment parseSwapStartOffset(FpmlDocument document, XmlElement exerciseEl) {
    for (XmlElement relevantUnderlyingDateEl : inOptional(exerciseEl.findChild("relevantUnderlyingDate"))) {
      document.validateNotPresent(relevantUnderlyingDateEl, "adjustableDates");
      XmlElement relativeDatesEl = relevantUnderlyingDateEl.getChild("relativeDates");
      return document.parseRelativeDateOffsetDays(relativeDatesEl);
    }
    return null;
  }

  private AdjustableDates parseBermudaDates(XmlElement exerciseEl, FpmlDocument document) {
    XmlElement bermudanDatesEl = exerciseEl.getChild("bermudaExerciseDates");
    document.validateNotPresent(bermudanDatesEl, "relativeDates");
    XmlElement adjustableDatesEl = bermudanDatesEl.getChild("adjustableDates");
    List<LocalDate> dates = adjustableDatesEl.getChildren("unadjustedDate").stream()
        .map(el -> document.parseDate(el))
        .collect(toImmutableList());
    BusinessDayAdjustment bda = document.parseBusinessDayAdjustments(adjustableDatesEl.getChild("dateAdjustments"));
    return AdjustableDates.of(bda, dates);
  }

  private AdjustableDate parseExpirationDate(XmlElement exerciseEl, FpmlDocument document) {
    XmlElement expirationDate = exerciseEl.getChild("expirationDate");
    document.validateNotPresent(expirationDate, "relativeDate");
    return expirationDate.findChild("adjustableDate")
        .map(el -> document.parseAdjustableDate(el)).get();
  }

  // Strata model cannot handle commencement date with a different convention to expiry date, so only parse unadjusted
  private LocalDate parseCommencementDate(XmlElement exerciseEl, FpmlDocument document) {
    XmlElement expirationDate = exerciseEl.getChild("commencementDate");
    document.validateNotPresent(expirationDate, "relativeDate");
    XmlElement unadjustedEl = expirationDate.getChild("adjustableDate").getChild("unadjustedDate");
    return document.parseDate(unadjustedEl);
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

  private AdjustablePayment parsePremium(XmlElement swaptionEl, FpmlDocument document, TradeInfoBuilder tradeInfoBuilder) {
    XmlElement premiumEl = swaptionEl.getChild("premium");
    PayReceive payReceive = document.parsePayerReceiver(premiumEl, tradeInfoBuilder);
    XmlElement paymentAmountEl = premiumEl.getChild("paymentAmount");
    CurrencyAmount ccyAmount = document.parseCurrencyAmount(paymentAmountEl);
    ccyAmount = payReceive.isPay() ? ccyAmount.negated() : ccyAmount;
    AdjustableDate paymentDate = premiumEl.findChild("paymentDate")
        .map(el -> document.parseAdjustableDate(el)).get();
    return AdjustablePayment.of(ccyAmount, paymentDate);
  }

  private LongShort parseLongShort(XmlElement swaptionEl, FpmlDocument document, TradeInfoBuilder tradeInfoBuilder) {
    BuySell buySell = document.parseBuyerSeller(swaptionEl, tradeInfoBuilder);
    return buySell.isBuy() ? LongShort.LONG : LongShort.SHORT;
  }

  private SwaptionSettlement parseSettlement(XmlElement swaptionEl, FpmlDocument document) {
    Optional<String> optionalCashSettlement = swaptionEl.findAttribute("cashSettlement");
    if (optionalCashSettlement.isPresent()) {
      XmlElement cashSettlementEl = swaptionEl.getChild("cashSettlement");
      CashSwaptionSettlementMethod method = parseCashSettlementMethod(cashSettlementEl);
      LocalDate settlementDate = document.parseAdjustedRelativeDateOffset(cashSettlementEl).getUnadjusted();
      return CashSwaptionSettlement.of(settlementDate, method);
    } else {
      // treat physical as the default to match FpML examples
      return PhysicalSwaptionSettlement.DEFAULT;
    }
  }

  private CashSwaptionSettlementMethod parseCashSettlementMethod(XmlElement cashSettlementEl) {
    if (cashSettlementEl.findChild("cashPriceMethod").isPresent() ||
        cashSettlementEl.findChild("cashPriceAlternateMethod").isPresent()) {
      return CashSwaptionSettlementMethod.CASH_PRICE;

    } else if (cashSettlementEl.findChild("parYieldCurveUnadjustedMethod").isPresent() ||
        cashSettlementEl.findChild("parYieldCurveAdjustedMethod").isPresent()) {
      return CashSwaptionSettlementMethod.PAR_YIELD;

    } else if (cashSettlementEl.findChild("zeroCouponYieldAdjustedMethod").isPresent()) {
      return CashSwaptionSettlementMethod.ZERO_COUPON_YIELD;

    } else if (cashSettlementEl.findChild("collateralizedCashPriceMethod").isPresent()) {
      return CashSwaptionSettlementMethod.COLLATERALIZED_CASH_PRICE;

    } else {
      throw new FpmlParseException("Invalid swaption cash settlement method: " + cashSettlementEl);
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "swaption";
  }

}
