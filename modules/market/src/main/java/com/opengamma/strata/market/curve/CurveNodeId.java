/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

/**
 * Marker interface for types that can identify curve nodes.
 * <p>
 * Curve node IDs are used by the scenario framework when applying perturbations to individual nodes in a curve.
 * When a curve group is constructed, IDs can be provided for the curve nodes. The same IDs must be used
 * in the scenario definition to identify the nodes which should be perturbed.
 * <p>
 * The most common IDs contain tenors or a futures expiry (a year and a month) but users are free to define
 * their own node IDs. The system makes no assumptions about the contents of a node ID. The only requirement is
 * that implementations have a sensible {@code hashCode()} and {@code equals()} implementation.
 */
public interface CurveNodeId {

}
