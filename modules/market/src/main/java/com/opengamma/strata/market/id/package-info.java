/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Package containing IDs that identify items of market data.
 * <p>
 * A market data ID uniquely identifies a piece of market data across the whole system. For example,
 * an ID might identify the USD discounting curve in a named curve group.
 * <p>
 * Calculations use market data keys to request market data. For example, a calculation might create
 * a market data key for the USD discounting curve and use it to request the curve.
 * <p>
 * A key identifies a piece of market data which can appear multiple time in the system. For example,
 * the system can contain an unlimited number of curve groups, and each of those groups can contain
 * a USD discounting curve.
 * <p>
 * When a calculation requests market data using a key, there is a mapping step to convert the key
 * to an ID, and the ID is used to look up the data. The mappings between keys and IDs are part
 * of the configuration of the calculations and are defined in a set of market data rules.
 */
package com.opengamma.strata.market.id;

