/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Package containing keys that identify items or market data.
 * <p>
 * Market data keys identify market data used by calculations. For example, a calculation might create
 * a market data key for the USD discounting curve and use it to request the curve.
 * <p>
 * A key identifies a piece of market data which can appear multiple time in the system. For example,
 * the system can contain an unlimited number of curve groups, and each of those groups can contain
 * a USD discounting curve.
 * <p>
 * The individual instances of the USD discounting curve are uniquely identified by a market data ID.
 * When a calculation requests market data using a key, there is a mapping step to convert the key
 * to an ID, and the ID is used to look up the data. The mappings between keys and IDs are part
 * of the configuration of the calculations and are defined in a set of market data rules.
 */
package com.opengamma.strata.market.key;

