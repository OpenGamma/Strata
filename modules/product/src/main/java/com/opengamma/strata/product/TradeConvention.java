/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

/**
 * A market convention for trades.
 * <p>
 * A convention contains key information that is commonly used in the market.
 * For example, a USD LIBOR forward rate agreement (FRA) will have a day count convention
 * of 'Act/360', spot date offset of T+2 and ISDA discounting.
 * <p>
 * A convention is typically combined with additional information to form a {@link TradeTemplate},
 * however this is not required. It is often possible to get a market price for a trade
 * based on the template, however it is not possible to obtain a market price for a convention.
 * <p>
 * Each implementation should provide a method with the name {@code toTrade} with
 * whatever arguments are necessary to complete the trade.
 * If there is an associated template, implementations should consider providing a
 * method with the name {@code toTemplate} to provide the conversion.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface TradeConvention {

}
