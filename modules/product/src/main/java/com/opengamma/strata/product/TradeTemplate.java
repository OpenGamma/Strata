/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

/**
 * A template used to create a trade.
 * <p>
 * A template contains almost all the information necessary to create a trade.
 * The missing elements are likely to include the trade date and market price.
 * As such, it is often possible to get a market price for a trade based on the template.
 * <p>
 * A template is typically built on an underlying {@link TradeConvention}, however this is not required.
 * <p>
 * Each implementation should provide a method with the name {@code toTrade} with
 * whatever arguments are necessary to complete the trade.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface TradeTemplate {

}
