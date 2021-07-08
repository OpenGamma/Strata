/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.impl.fpml;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.StandardSchemes;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.loader.fpml.FpmlDocument;
import com.opengamma.strata.loader.fpml.FpmlParseException;
import com.opengamma.strata.loader.fpml.FpmlParserPlugin;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;

/**
 * FpML parser for CDS.
 * <p>
 * This parser handles the subset of FpML necessary to populate the trade model.
 */
final class CdsFpmlParserPlugin
    implements FpmlParserPlugin {
  // this class is loaded by ExtendedEnum reflection

  /**
   * The singleton instance of the parser.
   */
  public static final CdsFpmlParserPlugin INSTANCE = new CdsFpmlParserPlugin();

  /**
   * Restricted constructor.
   */
  private CdsFpmlParserPlugin() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(FpmlDocument document, XmlElement tradeEl) {
    // supported elements:
    //  'generalTerms/effectiveDate'
    //  'generalTerms/scheduledTerminationDate'
    //  'generalTerms/buyerSellerModel'
    //  'generalTerms/dateAdjustments'
    //  'generalTerms/referenceInformation'
    //  'generalTerms/indexReferenceInformation'
    //  'feeLeg/initialPayment'
    //  'feeLeg/periodicPayment'
    //  'protectionTerms/calculationAmount'
    // ignored elements:
    //  'generalTerms/additionalTerm'
    //  'generalTerms/substitution'
    //  'generalTerms/modifiedEquityDelivery'
    //  'feeLeg/periodicPayment/adjustedPaymentDates'
    //  'feeLeg/marketFixedRate'
    //  'feeLeg/paymentDelay'
    //  'feeLeg/initialPoints'
    //  'feeLeg/marketPrice'
    //  'feeLeg/quotationStyle'
    //  'protectionTerms/*'
    //  'cashSettlementTerms'
    //  'physicalSettlementTerms'
    // rejected elements:
    //  'generalTerms/basketReferenceInformation'
    //  'feeLeg/singlePayment'
    TradeInfoBuilder tradeInfoBuilder = document.parseTradeInfo(tradeEl);
    return parseCds(document, tradeEl, tradeInfoBuilder);
  }

  // parses the CDS
  Trade parseCds(FpmlDocument document, XmlElement tradeEl, TradeInfoBuilder tradeInfoBuilder) {
    XmlElement cdsEl = tradeEl.getChild("creditDefaultSwap");
    XmlElement generalTermsEl = cdsEl.getChild("generalTerms");
    XmlElement feeLegEl = cdsEl.getChild("feeLeg");
    document.validateNotPresent(generalTermsEl, "basketReferenceInformation");
    document.validateNotPresent(feeLegEl, "singlePayment");
    BuySell buySell = document.parseBuyerSeller(generalTermsEl, tradeInfoBuilder);

    // effective and termination date are optional in FpML but mandatory for Strata
    AdjustableDate effectiveDate = document.parseAdjustableDate(generalTermsEl.getChild("effectiveDate"));
    AdjustableDate terminationDate = document.parseAdjustableDate(generalTermsEl.getChild("scheduledTerminationDate"));
    BusinessDayAdjustment bda = generalTermsEl.findChild("dateAdjustments")
        .map(el -> document.parseBusinessDayAdjustments(el))
        .orElse(BusinessDayAdjustment.NONE);
    PeriodicSchedule.Builder scheduleBuilder = PeriodicSchedule.builder()
        .startDate(effectiveDate.getUnadjusted())
        .startDateBusinessDayAdjustment(effectiveDate.getAdjustment())
        .endDate(terminationDate.getUnadjusted())
        .endDateBusinessDayAdjustment(terminationDate.getAdjustment())
        .businessDayAdjustment(bda);

    // an upfront fee
    Optional<XmlElement> initialPaymentOptEl = feeLegEl.findChild("initialPayment");
    AdjustablePayment upfrontFee = null;
    if (initialPaymentOptEl.isPresent()) {
      XmlElement initialPaymentEl = initialPaymentOptEl.get();
      PayReceive payRec = document.parsePayerReceiver(initialPaymentEl, tradeInfoBuilder);
      CurrencyAmount amount = document.parseCurrencyAmount(initialPaymentEl.getChild("paymentAmount"));
      LocalDate date = initialPaymentEl.findChild("adjustablePaymentDate")
          .map(el -> document.parseDate(el))
          .orElse(effectiveDate.getUnadjusted());
      AdjustableDate adjDate = AdjustableDate.of(date, bda);
      upfrontFee = payRec.isPay() ? AdjustablePayment.ofPay(amount, adjDate) : AdjustablePayment.ofReceive(amount, adjDate);
    }

    // we require a periodicPayment and fixedAmountCalculation
    XmlElement periodicPaymentEl = feeLegEl.getChild("periodicPayment");
    scheduleBuilder.frequency(periodicPaymentEl.findChild("paymentFrequency")
        .map(el -> document.parseFrequency(el))
        .orElse(Frequency.P3M));
    periodicPaymentEl.findChild("firstPaymentDate")
        .ifPresent(el -> scheduleBuilder.firstRegularStartDate(document.parseDate(el)));
    periodicPaymentEl.findChild("firstPeriodStartDate")
        .ifPresent(el -> scheduleBuilder.overrideStartDate(AdjustableDate.of(document.parseDate(el))));
    periodicPaymentEl.findChild("lastRegularPaymentDate")
        .ifPresent(el -> scheduleBuilder.lastRegularEndDate(document.parseDate(el)));
    scheduleBuilder.rollConvention(
        periodicPaymentEl.findChild("rollConvention")
            .map(el -> document.convertRollConvention(el.getContent()))
            .orElse(null));
    XmlElement fixedAmountCalcEl = periodicPaymentEl.getChild("fixedAmountCalculation");
    double fixedRate = document.parseDecimal(fixedAmountCalcEl.getChild("fixedRate"));
    DayCount dayCount = fixedAmountCalcEl.findChild("dayCountFraction")
        .map(el -> document.parseDayCountFraction(el))
        .orElse(DayCounts.ACT_360);

    // handle a single protectionTerms element
    XmlElement protectionTermEl = cdsEl.getChild("protectionTerms");
    CurrencyAmount notional = document.parseCurrencyAmount(protectionTermEl.getChild("calculationAmount"));

    // single name CDS
    Optional<XmlElement> singleOptEl = generalTermsEl.findChild("referenceInformation");
    if (singleOptEl.isPresent()) {
      // we require a single entityId
      XmlElement referenceEntityEl = singleOptEl.get().getChild("referenceEntity");
      XmlElement entityIdEl = referenceEntityEl.getChild("entityId");
      String scheme =
          entityIdEl.findAttribute("entityIdScheme").orElse("http://www.fpml.org/coding-scheme/external/entity-id-RED-1-0");
      String value = entityIdEl.getContent();
      StandardId entityId = StandardId.of(scheme, value);
      Cds cds = Cds.builder()
          .buySell(buySell)
          .legalEntityId(entityId)
          .currency(notional.getCurrency())
          .notional(notional.getAmount())
          .paymentSchedule(scheduleBuilder.build())
          .fixedRate(fixedRate)
          .dayCount(dayCount)
          .build();
      return CdsTrade.builder()
          .info(tradeInfoBuilder.build())
          .product(cds)
          .upfrontFee(upfrontFee)
          .build();
    }

    // CDS index
    Optional<XmlElement> indexOptEl = generalTermsEl.findChild("indexReferenceInformation");
    if (indexOptEl.isPresent()) {
      XmlElement indexInformation = indexOptEl.get();
      StandardId standardId = tryParseIndexId(indexInformation)
          .orElseGet(() -> StandardId.of("CDX-Name", indexInformation.getChild("indexName").getContent()));
      CdsIndex cdsIndex = CdsIndex.builder()
          .buySell(buySell)
          .cdsIndexId(standardId)
          .currency(notional.getCurrency())
          .notional(notional.getAmount())
          .paymentSchedule(scheduleBuilder.build())
          .fixedRate(fixedRate)
          .dayCount(dayCount)
          .build();
      return CdsIndexTrade.builder()
          .info(tradeInfoBuilder.build())
          .product(cdsIndex)
          .upfrontFee(upfrontFee)
          .build();
    }

    // unknown type
    throw new FpmlParseException("FpML CDS must be single name or index");
  }

  private Optional<StandardId> tryParseIndexId(XmlElement indexInformation) {
    Optional<XmlElement> idOpt = indexInformation.findChild("indexId");
    if (idOpt.isPresent()) {
      XmlElement idElement = idOpt.get();
      String id = idElement.getContent();
      if (id.length() == 9) {
        return Optional.of(StandardId.of(StandardSchemes.RED9_SCHEME, id));
      }
      return idElement.findAttribute("indexIdScheme").map(scheme -> StandardId.of(scheme, id));
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "creditDefaultSwap";
  }

}
