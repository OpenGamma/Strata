/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.RateFutureNode;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;

/**
 * Shifts to apply to the points in a curve, specified by the tenors of the points to which they apply.
 */
public abstract class CurveDataPointShifts {

  /** Curves are only shifted if they match this matcher. */
  private final CurveSpecificationMatcher _matcher;

  /** Shift amounts keyed by tenor of the point to which they apply. */
  private final Map<Tenor, Double> _shifts;

  protected CurveDataPointShifts(CurveSpecificationMatcher matcher, List<PointShift> shifts) {
    ArgumentChecker.notNull(shifts, "shifts");
    _matcher = ArgumentChecker.notNull(matcher, "matcher");
    _shifts = new HashMap<>(shifts.size());

    for (PointShift shift : shifts) {
      _shifts.put(shift._tenor, shift._shiftAmount);
    }
  }

  /**
   * Returns a new map of values containing the data in the input map with shifts applied.
   * Shifts are only applied if the curve specification matches the matcher.
   *
   * @param curveSpec specification of the curve
   * @param values market data of the curve's points, keyed by identifier
   * @return the shifted values
   */
  public Result<Map<ExternalIdBundle, Double>> apply(CurveSpecification curveSpec, Map<ExternalIdBundle, Double> values) {
    if (!_matcher.matches(curveSpec)) {
      return Result.success(values);
    }
    // populate the results with the value. if there is no shift defined for a point the original value will be used
    Map<ExternalIdBundle, Double> results = new HashMap<>(values);
    ExternalIdMap<Double> valueMap = new ExternalIdMap<>(values);
    Map<Tenor, CurveNodeWithIdentifier> tenorNodeMap = createNodeMap(curveSpec);
    List<Result<?>> failures = new ArrayList<>();

    for (Map.Entry<Tenor, Double> entry : _shifts.entrySet()) {
      Tenor tenor = entry.getKey();
      Double shiftAmount = entry.getValue();
      double shiftedValue;
      CurveNodeWithIdentifier node = tenorNodeMap.get(tenor);

      if (node == null) {
        failures.add(Result.failure(FailureStatus.MISSING_DATA, "No curve point found with tenor {}", tenor));
      } else {
        ExternalId nodeId = node.getIdentifier();
        Double value = valueMap.get(nodeId);

        // futures are quoted the other way round, i.e. (1 - value)
        if (node.getCurveNode() instanceof RateFutureNode) {
          shiftedValue = 1 - shift(1 - value, shiftAmount);
        } else {
          shiftedValue = shift(value, shiftAmount);
        }
        results.put(valueMap.getBundle(nodeId), shiftedValue);
      }
    }
    if (failures.isEmpty()) {
      return Result.success(results);
    } else {
      return Result.failure(failures);
    }
  }

  /**
   * Applies a shift to a value.
   *
   * @param normalizedValue the value to shift
   * @param shiftAmount the amount to shift by
   * @return the shifted amount
   */
  protected abstract double shift(double normalizedValue, double shiftAmount);

  private static Map<Tenor, CurveNodeWithIdentifier> createNodeMap(CurveSpecification curveSpec) {
    Set<CurveNodeWithIdentifier> nodes = curveSpec.getNodes();
    Map<Tenor, CurveNodeWithIdentifier> nodeMap = new HashMap<>(nodes.size());

    for (CurveNodeWithIdentifier node : nodes) {
      nodeMap.put(node.getCurveNode().getResolvedMaturity(), node);
    }
    return nodeMap;
  }

  /**
   * Creates a shift that adds an absolute amount to each market data point in the curve.
   *
   * @param matcher for deciding whether a curve should be shifted
   * @param shifts the shifts to apply to the curve points
   * @return an instance to perform the shift
   */
  public static CurveDataPointShifts absolute(CurveSpecificationMatcher matcher, PointShift... shifts) {
    return new Absolute(matcher, Arrays.asList(shifts));
  }

  /**
   * Creates a shift that adds an absolute amount to each market data point in the curve.
   *
   * @param matcher for deciding whether a curve should be shifted
   * @param shifts the shifts to apply to the curve points
   * @return an instance to perform the shift
   */
  public static CurveDataPointShifts absolute(CurveSpecificationMatcher matcher, List<PointShift> shifts) {
    return new Absolute(matcher, shifts);
  }

  /**
   * Creates a shift that adds a relative amount to each market data point in the curve.
   * A shift of 0.1 (+10%) scales the point value by 1.1, a shift of -0.2 (-20%) scales the point value by 0.8.
   *
   * @param matcher for deciding whether a curve should be shifted
   * @param shifts the shifts to apply to the curve points
   * @return an instance to perform the shift
   */
  public static CurveDataPointShifts relative(CurveSpecificationMatcher matcher, PointShift... shifts) {
    return new Relative(matcher, Arrays.asList(shifts));
  }

  /**
   * Creates a shift that adds a relative amount to each market data point in the curve.
   * A shift of 0.1 (+10%) scales the point value by 1.1, a shift of -0.2 (-20%) scales the point value by 0.8.
   *
   * @param matcher for deciding whether a curve should be shifted
   * @param shifts the shifts to apply to the curve points
   * @return an instance to perform the shift
   */
  public static CurveDataPointShifts relative(CurveSpecificationMatcher matcher, List<PointShift> shifts) {
    return new Relative(matcher, shifts);
  }

  /**
   * A shift amount and the tenor of the point to which it should be applied.
   */
  public static final class PointShift {

    private final Tenor _tenor;
    private final double _shiftAmount;

    private PointShift(Tenor tenor, double shiftAmount) {
      _tenor = ArgumentChecker.notNull(tenor, "tenor");
      _shiftAmount = shiftAmount;
    }

    /**
     * Creates a shift.
     *
     * @param tenor the tenor of the point to which the shift should be applied
     * @param shiftAmount the amount to shift by
     * @return the shift
     */
    public static PointShift of(Tenor tenor, double shiftAmount) {
      return new PointShift(tenor, shiftAmount);
    }
  }

  /**
   * Adds an absolute amount to the point's value.
   */
  private static final class Absolute extends CurveDataPointShifts {

    public Absolute(CurveSpecificationMatcher matcher, List<PointShift> shifts) {
      super(matcher, shifts);
    }

    @Override
    protected double shift(double normalizedValue, double shiftAmount) {
      return normalizedValue + shiftAmount;
    }
  }

  /**
   * Scales the point's value.
   */
  private static final class Relative extends CurveDataPointShifts {

    public Relative(CurveSpecificationMatcher matcher, List<PointShift> shifts) {
      super(matcher, shifts);
    }

    @Override
    protected double shift(double normalizedValue, double shiftAmount) {
      return normalizedValue * (1 + shiftAmount);
    }
  }
}
