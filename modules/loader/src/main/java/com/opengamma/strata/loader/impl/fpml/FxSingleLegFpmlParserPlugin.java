/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.impl.fpml;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.ImmutableFxIndex;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.loader.fpml.FpmlDocument;
import com.opengamma.strata.loader.fpml.FpmlParseException;
import com.opengamma.strata.loader.fpml.FpmlParserPlugin;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;

/**
 * FpML parser for single leg FX.
 * <p>
 * This parser handles the subset of FpML necessary to populate the trade model.
 */
final class FxSingleLegFpmlParserPlugin
    implements FpmlParserPlugin {
  // this class is loaded by ExtendedEnum reflection

  /**
   * The singleton instance of the parser.
   */
  public static final FxSingleLegFpmlParserPlugin INSTANCE = new FxSingleLegFpmlParserPlugin();

  /**
   * Restricted constructor.
   */
  private FxSingleLegFpmlParserPlugin() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(FpmlDocument document, XmlElement tradeEl) {
    // supported elements:
    // 'exchangedCurrency1/paymentAmount'
    // 'exchangedCurrency2/paymentAmount'
    // 'valueDate'
    // 'nonDeliverableSettlement?'
    // ignored elements:
    // 'dealtCurrency?'
    // 'exchangeRate'
    // rejected elements:
    // 'currency1ValueDate'
    // 'currency2ValueDate'
    XmlElement fxEl = tradeEl.getChild("fxSingleLeg");
    document.validateNotPresent(fxEl, "currency1ValueDate");
    document.validateNotPresent(fxEl, "currency2ValueDate");
    // amounts
    TradeInfoBuilder tradeInfoBuilder = document.parseTradeInfo(tradeEl);
    XmlElement curr1El = fxEl.getChild("exchangedCurrency1");
    XmlElement curr2El = fxEl.getChild("exchangedCurrency2");
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
    LocalDate valueDate = document.parseDate(fxEl.getChild("valueDate"));
    // FxSingle or NDF
    Optional<XmlElement> ndfEl = fxEl.findChild("nonDeliverableSettlement");
    if (!ndfEl.isPresent()) {
      return FxSingleTrade.builder()
          .info(tradeInfoBuilder.build())
          .product(FxSingle.of(curr1Amount, curr2Amount, valueDate))
          .build();
    }
    return parseNdf(document, fxEl, ndfEl.get(), curr1Amount, curr2Amount, valueDate, tradeInfoBuilder);
  }

  private Trade parseNdf(
      FpmlDocument document,
      XmlElement fxEl,
      XmlElement ndfEl,
      CurrencyAmount curr1Amount,
      CurrencyAmount curr2Amount,
      LocalDate valueDate,
      TradeInfoBuilder tradeInfoBuilder) {

    // rate
    XmlElement rateEl = fxEl.getChild("exchangeRate");
    double rate = document.parseDecimal(rateEl.getChild("rate"));
    XmlElement pairEl = rateEl.getChild("quotedCurrencyPair");
    Currency curr1 = document.parseCurrency(pairEl.getChild("currency1"));
    Currency curr2 = document.parseCurrency(pairEl.getChild("currency2"));
    String basis = pairEl.getChild("quoteBasis").getContent();
    FxRate fxRate;
    if ("Currency2PerCurrency1".equals(basis)) {
      fxRate = FxRate.of(curr1, curr2, rate);
    } else if ("Currency1PerCurrency2".equals(basis)) {
      fxRate = FxRate.of(curr2, curr1, rate);
    } else {
      throw new FpmlParseException("Unknown quote basis: " + basis);
    }
    // settlement currency
    Currency settleCurr = document.parseCurrency(ndfEl.getChild("settlementCurrency"));
    CurrencyAmount settleCurrAmount = curr1Amount.getCurrency().equals(settleCurr) ? curr1Amount : curr2Amount;
    // index
    XmlElement fixingEl = ndfEl.getChild("fixing");  // only support one of these in pricing model
    LocalDate fixingDate = document.parseDate(fixingEl.getChild("fixingDate"));
    DaysAdjustment offset = DaysAdjustment.ofCalendarDays(Math.toIntExact(valueDate.until(fixingDate, DAYS)));
    XmlElement sourceEl = fixingEl.getChild("fxSpotRateSource");  // required for our model
    XmlElement primarySourceEl = sourceEl.getChild("primaryRateSource");
    String primarySource = primarySourceEl.getChild("rateSource").getContent();
    String primaryPage = primarySourceEl.findChild("rateSourcePage").map(e -> e.getContent()).orElse("");
    LocalTime time = document.parseTime(sourceEl.getChild("fixingTime").getChild("hourMinuteTime"));  // required for our model
    HolidayCalendarId calendar = document.parseBusinessCenter(sourceEl.getChild("fixingTime").getChild("businessCenter"));
    FxIndex index = ImmutableFxIndex.builder()
        .name(primarySource + "/" + primaryPage + "/" + time)
        .currencyPair(CurrencyPair.of(curr1, curr2))
        .fixingCalendar(calendar)
        .maturityDateOffset(offset)
        .build();
    return FxNdfTrade.builder()
        .info(tradeInfoBuilder.build())
        .product(FxNdf.builder()
            .settlementCurrencyNotional(settleCurrAmount)
            .agreedFxRate(fxRate)
            .index(index)
            .paymentDate(valueDate)
            .build())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "fxSingleLeg";
  }

}
