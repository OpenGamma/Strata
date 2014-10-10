/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */

/**
 * Identifiers for entities.
 * <p>
 * This includes various different ways to identify entities:
 * <ul>
 * <li>{@code StandardId}: An two-part identifier used for identifying objects in the system</li>
 * <li>{@code ObjectId}: An internal identifier for an entity</li>
 * <li>{@code UniqueId}: Builds on {@code ObjectId} to add a version number</li>
 * <li>{@code ExternalId}: An external identifier defined by another organization, such as "CUSIP~912828LS7"</li>
 * <li>{@code ExternalIdBundle}: A set of external identifiers for the same entity</li>
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
package com.opengamma.platform.source.id;
