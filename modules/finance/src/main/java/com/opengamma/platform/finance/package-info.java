/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */

/**
 * Entity objects describing the domain of finance.
 * <p>
 * The trade model has three basic concepts, trades, securities and products.
 * <p>
 * A {@link com.opengamma.platform.finance.Trade Trade} is the basic element of finance,
 * a transaction between two organizations, known as counterparties.
 * Most trades represented in the system will be contracts that have been agreed on a date in the past.
 * The trade model also allows trades with a date in the future, or without any date.
 * This supports "what if" trades and aggregated trades, also known as <i>positions</i>.
 * <p>
 * A {@link com.opengamma.platform.finance.Security Security} is a standard contract that is traded,
 * such as an equity share or futures contract. Securities are typically created once and shared
 * using an identifier, represented by a {@link com.opengamma.collect.id.StandardId StandardId}.
 * They are often referred to as <i>reference data</i>.
 * The standard implementation of {@code Security} is {@link com.opengamma.platform.finance.UnitSecurity UnitSecurity}.
 * <p>
 * A {@link com.opengamma.platform.finance.Product Product} is the financial details of the trade or security.
 * A product typically contains enough information to be priced, such as the dates, holidays, indices,
 * currencies and amounts. There is an implementation of {@code Product} for each distinct type
 * of financial instrument.
 * <p>
 * Trades are typically classified as Over-The-Counter (OTC) and listed.
 * OTC trades are represented by {@link com.opengamma.platform.finance.OtcTrade OtcTrade}.
 * An OTC trade directly embeds the product it refers to.
 * <p>
 * Listed trades are represented by {@link com.opengamma.platform.finance.QuantityTrade QuantityTrade}.
 * A listed trade contains a {@link com.opengamma.platform.finance.SecurityLink SecurityLink}, which
 * loosely connects the trade to the security. The security contains details of the actual product.
 * <p>
 * For example, consider an OTC instrument such as an interest rate swap.
 * The object model will consist of an {@code OtcTrade} that contains a
 * {@link com.opengamma.platform.finance.rate.swap.Swap Swap}, creating an object of type
 * {@code OtcTrade<Swap>}.
 * <p>
 * An another example, consider a trade in a listed equity.
 * The object model will consist of a {@code QuantityTrade} that contains a
 * {@code SecurityLink} where the identifier refers to the equity security.
 * The security will typically exist in a database, but may be embedded within the link.
 * The product of the security will be an {@link com.opengamma.platform.finance.equity.Equity Equity}.
 * As such the security will be of type {@code Security<Equity>} and the trade will be of type
 * {@code QuantityTrade<Equity>}.
 * <p>
 * The key to understanding the model is appreciating the separation of products from trades and securities.
 * The advantage of this approach is that it allows a product to be both OTC and listed, such as future options.
 * It also allows a product to be an underlying of another product, such as a swap within a swaption.
 * Note that on the listed side, it is often possible to price either against the market or against a model.
 * Details for pricing against the market are primarily held in the security.
 * Details for pricing against the model are primarily held in the product.
 */
@javax.annotation.ParametersAreNonnullByDefault
package com.opengamma.platform.finance;

