/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

//-------------------------------------------------------------------------

import static com.opengamma.strata.loader.csv.CsvLoaderColumns.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PRICE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.QUANTITY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.TRADE_TYPE_FIELD;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.product.SecurityQuantityTrade;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.bond.BillTrade;
import com.opengamma.strata.product.bond.BondFutureOptionTrade;
import com.opengamma.strata.product.bond.CapitalIndexedBondTrade;
import com.opengamma.strata.product.bond.FixedCouponBondTrade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.dsf.DsfTrade;
import com.opengamma.strata.product.etd.EtdFutureTrade;
import com.opengamma.strata.product.etd.EtdOptionTrade;
import com.opengamma.strata.product.index.IborFutureOptionTrade;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.OvernightFutureTrade;

/**
 * aaa
 */
public class SecurityTradeCsvPlugin implements TradeCsvWriterPlugin<SecurityQuantityTrade> {

  /**
   * The singleton instance of the plugin.
   */
  public static final SecurityTradeCsvPlugin INSTANCE = new SecurityTradeCsvPlugin();

  /** The headers. */
  private static final ImmutableSet<String> HEADERS = ImmutableSet.<String>builder()
      .add(SECURITY_ID_SCHEME_FIELD)
      .add(SECURITY_ID_FIELD)
      .add(BUY_SELL_FIELD)
      .add(QUANTITY_FIELD)
      .add(PRICE_FIELD)
      .build();

  @Override

  public Set<String> headers(List<SecurityQuantityTrade> trades) {
    return HEADERS;
  }

  @Override
  public void writeCsv(CsvOutput.CsvRowOutputWithHeaders csv, SecurityQuantityTrade trade) {
    csv.writeCell(TRADE_TYPE_FIELD, "Security");
    csv.writeCell(SECURITY_ID_SCHEME_FIELD, trade.getSecurityId().getStandardId().getScheme());
    csv.writeCell(SECURITY_ID_FIELD, trade.getSecurityId().getStandardId().getValue());
    csv.writeCell(BUY_SELL_FIELD, trade.getQuantity() < 0 ? BuySell.SELL : BuySell.BUY);
    csv.writeCell(QUANTITY_FIELD, Math.abs(trade.getQuantity()));
    csv.writeCell(PRICE_FIELD, trade.getPrice());
    csv.writeNewLine();
  }

  @Override
  public String getName() {
    return SecurityQuantityTrade.class.getSimpleName();
  }

  @Override
  public Set<String> supportedTradeTypes() {
    return ImmutableSet.of(
        SecurityTrade.class.getSimpleName(),
        EtdFutureTrade.class.getSimpleName(),
        EtdOptionTrade.class.getSimpleName(),
        BillTrade.class.getSimpleName(),
        BondFutureOptionTrade.class.getSimpleName(),
        CapitalIndexedBondTrade.class.getSimpleName(),
        DsfTrade.class.getSimpleName(),
        FixedCouponBondTrade.class.getSimpleName(),
        IborFutureOptionTrade.class.getSimpleName(),
        IborFutureTrade.class.getSimpleName(),
        OvernightFutureTrade.class.getSimpleName());
  }
}

