package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;

public class FxNdfTradeCsvPlugin implements TradeCsvParserPlugin, TradeTypeCsvWriter<FxNdfTrade> {

  public static final FxNdfTradeCsvPlugin INSTANCE = new FxNdfTradeCsvPlugin();

  private static final String NON_DELIVERABLE_CURRENCY_FIELD = "ND Currency";
  private static final String SETTLEMENT_CURRENCY_FIELD = "Settlement Currency";
  private static final String SETTLEMENT_CURRENCY_DIRECTION_FIELD = "Settlement Currency Direction";
  private static final String SETTLEMENT_CURRENCY_NOTIONAL_FIELD = "Settlement Currency Notional";
  private static final String STRIKE_FIELD = "Strike";

  private static final ImmutableList<String> HEADERS = ImmutableList.<String>builder()
      .add(PAYMENT_DATE_FIELD)
      .add(SETTLEMENT_CURRENCY_FIELD)
      .add(SETTLEMENT_CURRENCY_DIRECTION_FIELD)
      .add(SETTLEMENT_CURRENCY_NOTIONAL_FIELD)
      .add(NON_DELIVERABLE_CURRENCY_FIELD)
      .add(STRIKE_FIELD)
      .build();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("FXNDF", "FX NDF");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(FxNdfTrade.class)) {
      return Optional.of(resolver.parseFxNdfTrade(baseRow, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "FxNdf";
  }

  //-------------------------------------------------------------------------
  static FxNdfTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FxNdfTrade trade = parseRow(row, info);
    return resolver.completeTrade(row, trade);
  }

  private static FxNdfTrade parseRow(CsvRow row, TradeInfo info) {
    CurrencyAmount settlementCurrencyNotional = CsvLoaderUtils.parseCurrencyAmountWithDirection(
        row, SETTLEMENT_CURRENCY_FIELD, SETTLEMENT_CURRENCY_NOTIONAL_FIELD, SETTLEMENT_CURRENCY_DIRECTION_FIELD);
    LocalDate paymentDate = row.getField(PAYMENT_DATE_FIELD, LoaderUtils::parseDate);
    Currency settlementCurrency = row.getField(SETTLEMENT_CURRENCY_FIELD, LoaderUtils::parseCurrency);
    Currency nonDeliverableCurrency = row.getField(NON_DELIVERABLE_CURRENCY_FIELD, LoaderUtils::parseCurrency);
    CurrencyPair currencyPair = CurrencyPair.of(settlementCurrency, nonDeliverableCurrency).toConventional();
    FxRate agreedFxRate = FxRate.of(currencyPair, row.getField(STRIKE_FIELD, LoaderUtils::parseDouble));
    FxIndex.extendedEnum();
    FxIndex index = parseFxIndex(currencyPair).orElseThrow(() -> new IllegalArgumentException(Messages.format(
        "No FX Index found for currency pair {}. Known FX Index required to construct NDF trade",
        currencyPair.toString())));
    FxNdf fxNdf = FxNdf.builder()
        .settlementCurrencyNotional(settlementCurrencyNotional)
        .agreedFxRate(agreedFxRate)
        .index(index)
        .paymentDate(paymentDate)
        .build();
    return FxNdfTrade.of(info, fxNdf);
  }

  private static Optional<FxIndex> parseFxIndex(CurrencyPair currencyPair) {
    ImmutableMap<CurrencyPair, FxIndex> ccyFxIndexMap = FxIndex.extendedEnum().lookupAll().values().stream()
        .collect(toImmutableMap(
            fxIndex -> fxIndex.getCurrencyPair().toConventional(),
            fxIndex -> fxIndex,
            (fxIndex1, fxIndex2) -> fxIndex1)); //arbitrarily chose when we have multiple indices for the same pair
    return Optional.of(ccyFxIndexMap.get(currencyPair));
  }

  //-------------------------------------------------------------------------
  @Override
  public List<String> headers(List<FxNdfTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvRowOutputWithHeaders csv, FxNdfTrade trade) {
    csv.writeCell(TRADE_TYPE_FIELD, "FxNdf");
    FxNdf fxNdf = trade.getProduct();
    csv.writeCell(PAYMENT_DATE_FIELD, fxNdf.getPaymentDate());
    csv.writeCell(SETTLEMENT_CURRENCY_FIELD, fxNdf.getSettlementCurrency());
    csv.writeCell(SETTLEMENT_CURRENCY_DIRECTION_FIELD,
        fxNdf.getSettlementCurrencyNotional().isNegative() ? PayReceive.PAY : PayReceive.RECEIVE);
    csv.writeCell(SETTLEMENT_CURRENCY_NOTIONAL_FIELD, Math.abs(fxNdf.getSettlementCurrencyNotional().getAmount()));
    csv.writeCell(NON_DELIVERABLE_CURRENCY_FIELD, fxNdf.getNonDeliverableCurrency());
    csv.writeCell(STRIKE_FIELD, fxNdf.getAgreedFxRate());
    csv.writeNewLine();
  }
}
