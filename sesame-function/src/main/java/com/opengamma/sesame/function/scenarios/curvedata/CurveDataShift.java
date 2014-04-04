/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.RateFutureNode;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;

/**
 * Performs a parallel shift on a set of curve data if the curve matches the {@link CurveSpecificationMatcher matcher}.
 */
public abstract class CurveDataShift {

  /** Curves are only shifted if they match this matcher. */
  private final CurveSpecificationMatcher _matcher;

  /** The amount to add to each point on the curve. */
  private final double _shiftAmount;

  /**
   * Creates a shift that adds an absolute amount to each market data point in the curve.
   *
   * @param shiftAmount the amount to add to each point
   * @param matcher for deciding whether a curve should be shifted
   */
  protected CurveDataShift(double shiftAmount, CurveSpecificationMatcher matcher) {
    _shiftAmount = shiftAmount;
    _matcher = ArgumentChecker.notNull(matcher, "matcher");
  }

  /**
   * Returns a new map of values containing the data in the input map with shifts applied.
   * Shifts are only applied if the curve specification matches the matcher.
   *
   * @param curveSpec specification of the curve
   * @param values market data of the curve's points, keyed by identifier
   * @return the shifted values
   */
  public Map<ExternalIdBundle, Double> apply(CurveSpecification curveSpec, Map<ExternalIdBundle, Double> values) {
    if (!_matcher.matches(curveSpec)) {
      return values;
    }
    Map<ExternalIdBundle, Double> results = new HashMap<>(values.size());
    Map<ExternalId, CurveNode> nodeMap = createNodeMap(curveSpec);

    for (Map.Entry<ExternalIdBundle, Double> entry : values.entrySet()) {
      ExternalIdBundle idBundle = entry.getKey();
      Double value = entry.getValue();
      CurveNode node = getNode(idBundle, nodeMap);
      double shiftedValue;

      // futures are quoted the other way round, i.e. (1 - value)
      if (node instanceof RateFutureNode) {
        shiftedValue = 1 - shift(1 - value);
      } else {
        shiftedValue = shift(value);
      }
      results.put(idBundle, shiftedValue);
    }
    return results;
  }

  private static Map<ExternalId, CurveNode> createNodeMap(CurveSpecification curveSpec) {
    Set<CurveNodeWithIdentifier> nodes = curveSpec.getNodes();
    Map<ExternalId, CurveNode> nodeMap = new HashMap<>(nodes.size());

    for (CurveNodeWithIdentifier node : nodes) {
      nodeMap.put(node.getIdentifier(), node.getCurveNode());
    }
    return nodeMap;
  }

  private static CurveNode getNode(ExternalIdBundle idBundle, Map<ExternalId, CurveNode> nodeMap) {
    for (ExternalId id : idBundle) {
      CurveNode node = nodeMap.get(id);

      if (node != null) {
        return node;
      }
    }
    // this should never happen
    throw new IllegalArgumentException("No curve node found for ID bundle " + idBundle + " in map " + nodeMap);
  }

  /**
   * @return the shift amount to apply to the curve data.
   */
  protected double getShiftAmount() {
    return _shiftAmount;
  }

  /**
   * Shifts a single value. Only invoked if the curve matches the matcher. If the curve data is quoted the other
   * way up (e.g. for futures) it is inverted before calling this method.
   *
   * @param normalizedValue the value
   * @return the shifted value
   */
  protected abstract double shift(double normalizedValue);
}
