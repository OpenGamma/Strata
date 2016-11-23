/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */

/**
 * Representations of currency and money.
 * <p>
 * The representation of {@link com.opengamma.strata.basics.currency.Currency Currency} is
 * separate from that in the JDK to provide more control. A pair of currencies is
 * represented by {@link com.opengamma.strata.basics.currency.CurrencyPair CurrencyPair},
 * which provides a mechanism of determining whether the pair is in standard FX market order.
 * <p>
 * {@link com.opengamma.strata.basics.currency.CurrencyAmount CurrencyAmount} provides
 * the primary monetary representation, while
 * {@link com.opengamma.strata.basics.currency.MultiCurrencyAmount MultiCurrencyAmount}
 * provide a representation where the amount is in multiple currencies.
 * <p>
 * Basic support for FX conversions is also provided. A single FX rate can be represented
 * using {@link com.opengamma.strata.basics.currency.FxRate FxRate}, while a matrix of
 * FX rates is represented using {@link com.opengamma.strata.basics.currency.FxMatrix FxMatrix}.
 * The {@link com.opengamma.strata.basics.currency.FxConvertible FxConvertible} and
 * {@link com.opengamma.strata.basics.currency.FxRateProvider FxRateProvider} interfaces
 * provide the glue to make currency conversion easy.
 */
package com.opengamma.strata.basics.currency;
