/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.impl.loader;

import java.util.List;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.loader.FpmlDocument;
import com.opengamma.strata.finance.loader.FpmlParseException;
import com.opengamma.strata.finance.loader.FpmlTradeParser;
import com.opengamma.strata.finance.rate.fra.Fra;
import com.opengamma.strata.finance.rate.fra.FraDiscountingMethod;
import com.opengamma.strata.finance.rate.fra.FraTrade;

/**
 * FpML parser for FRAs.
 * <p>
 * This parser handles the subset of FpML necessary to populate the trade model.
 */
final class FraFpmlTradeParser
    implements FpmlTradeParser {
  // this class is loaded by ExtendedEnum reflection

  /**
   * The singleton instance of the parser.
   */
  public static final FraFpmlTradeParser INSTANCE = new FraFpmlTradeParser();

  /**
   * Restricted constructor.
   */
  private FraFpmlTradeParser() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(XmlElement tradeEl, FpmlDocument document) {
    // supported elements:
    //  'buyerPartyReference'
    //  'sellerPartyReference'
    //  'adjustedTerminationDate'
    //  'paymentDate'
    //  'fixingDateOffset'
    //  'dayCountFraction'
    //  'notional'
    //  'fixedRate'
    //  'floatingRateIndex'
    //  'indexTenor+'
    //  'fraDiscounting'
    // ignored elements:
    //  'Product.model?'
    //  'buyerAccountReference?'
    //  'sellerAccountReference?'
    //  'calculationPeriodNumberOfDays'
    //  'additionalPayment*'
    TradeInfo.Builder tradeInfoBuilder = document.parseTradeInfo(tradeEl);
    XmlElement fraEl = tradeEl.getChild("fra");
    Fra.Builder fraBuilder = Fra.builder();
    // buy/sell and counterparty
    fraBuilder.buySell(document.parseBuyerSeller(fraEl, tradeInfoBuilder));
    // start date
    fraBuilder.startDate(document.parseDate(fraEl.getChild("adjustedEffectiveDate")));
    // end date
    fraBuilder.endDate(document.parseDate(fraEl.getChild("adjustedTerminationDate")));
    // payment date
    fraBuilder.paymentDate(document.parseAdjustableDate(fraEl.getChild("paymentDate")));
    // fixing offset
    fraBuilder.fixingDateOffset(document.parseRelativeDateOffsetDays(fraEl.getChild("fixingDateOffset")));
    // dateRelativeTo required to refer to adjustedEffectiveDate, so ignored here
    // day count
    fraBuilder.dayCount(document.parseDayCountFraction(fraEl.getChild("dayCountFraction")));
    // notional
    CurrencyAmount notional = document.parseCurrencyAmount(fraEl.getChild("notional"));
    fraBuilder.currency(notional.getCurrency());
    fraBuilder.notional(notional.getAmount());
    // fixed rate
    fraBuilder.fixedRate(document.parseDecimal(fraEl.getChild("fixedRate")));
    // index
    List<Index> indexes = document.parseIndexes(fraEl);
    switch (indexes.size()) {
      case 1:
        fraBuilder.index((IborIndex) indexes.get(0));
        break;
      case 2:
        fraBuilder.index((IborIndex) indexes.get(0));
        fraBuilder.indexInterpolated((IborIndex) indexes.get(1));
        break;
      default:
        throw new FpmlParseException("Expected one or two indexes, but found " + indexes.size());
    }
    // discounting
    fraBuilder.discounting(FraDiscountingMethod.of(fraEl.getChild("fraDiscounting").getContent()));

    return FraTrade.builder()
        .tradeInfo(tradeInfoBuilder.build())
        .product(fraBuilder.build())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "fra";
  }

}
