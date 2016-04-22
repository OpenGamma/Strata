/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.impl.fpml;

import java.util.List;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.loader.fpml.FpmlDocument;
import com.opengamma.strata.loader.fpml.FpmlParseException;
import com.opengamma.strata.loader.fpml.FpmlParserPlugin;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraDiscountingMethod;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * FpML parser for FRAs.
 * <p>
 * This parser handles the subset of FpML necessary to populate the trade model.
 */
final class FraFpmlParserPlugin
    implements FpmlParserPlugin {
  // this class is loaded by ExtendedEnum reflection

  /**
   * The singleton instance of the parser.
   */
  public static final FraFpmlParserPlugin INSTANCE = new FraFpmlParserPlugin();

  /**
   * Restricted constructor.
   */
  private FraFpmlParserPlugin() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(FpmlDocument document, XmlElement tradeEl) {
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
    TradeInfoBuilder tradeInfoBuilder = document.parseTradeInfo(tradeEl);
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
        .info(tradeInfoBuilder.build())
        .product(fraBuilder.build())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "fra";
  }

}
