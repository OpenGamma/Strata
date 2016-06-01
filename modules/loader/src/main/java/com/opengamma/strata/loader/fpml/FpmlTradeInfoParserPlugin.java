/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import java.time.LocalDate;

import com.google.common.collect.ListMultimap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradeInfoBuilder;

/**
 * Pluggable FpML trade information parser.
 * <p>
 * Implementations of this interface parse FpML to produce {@link TradeInfo}.
 * The {@link FpmlDocument} instance provides many useful helper methods.
 * <p>
 * See {@link FpmlDocumentParser} for the main entry point for FpML parsing.
 */
@FunctionalInterface
public interface FpmlTradeInfoParserPlugin {

  /**
   * Returns the standard parser plugin that parses the trade date and the first
   * identifier of "our" party.
   * 
   * @return the standard trade info parser
   */
  public static FpmlTradeInfoParserPlugin standard() {
    return FpmlDocument.TRADE_INFO_STANDARD;
  }

  //-------------------------------------------------------------------------
  /**
   * Parses trade information from the FpML document.
   * <p>
   * This parses any trade info that is desired from the specified FpML document.
   * Details of the whole document and parser helper methods are provided.
   * Typically such parsing will require accessing the {@code <tradeHeader>} element
   * from the root FpML element in the document.
   * <p>
   * Since most implementations will need the trade date and a trade identifier,
   * these are pre-parsed before the method is invoked. The parties associated with
   * the party href id can be obtained from the document.
   * <p>
   * A new instance of the builder must be returned each time the method is invoked.
   * The builder is returned to allow the counterparty to be added by the
   * {@link FpmlParserPlugin} implementation based on the trade direction.
   * 
   * @param tradeDate  the trade date from the document
   * @param allTradeIds  the collection of trade identifiers in the document, keyed by party href id
   * @param document  the document-wide information and parser helper
   * @return the trade info object
   * @throws RuntimeException if unable to parse
   */
  public abstract TradeInfoBuilder parseTrade(
      FpmlDocument document,
      LocalDate tradeDate,
      ListMultimap<String, StandardId> allTradeIds);

}
