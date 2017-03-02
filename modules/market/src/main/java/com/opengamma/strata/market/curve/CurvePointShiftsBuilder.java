/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ShiftType;

/**
 * Mutable builder for building instances of {@link CurvePointShifts}.
 * <p>
 * This is created via {@link CurvePointShifts#builder(ShiftType)}.
 */
public final class CurvePointShiftsBuilder {

  /**
   * The type of shift to apply to the rates.
   */
  private final ShiftType shiftType;
  /**
   * The shift amounts, keyed by the identifier of the node to which they should be applied.
   * <p>
   * This is a linked map in order to preserve the insertion order. This means the node identifiers
   * will appear in the same order as the nodes, assuming the shifts are added in node order (which seems
   * likely).
   */
  private final Map<Pair<Integer, Object>, Double> shifts = new LinkedHashMap<>();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor used by {@link CurvePointShifts#builder}.
   *
   * @param shiftType  the type of shift to apply to the rates
   */
  CurvePointShiftsBuilder(ShiftType shiftType) {
    this.shiftType = ArgChecker.notNull(shiftType, "shiftType");
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a shift for a curve node to the builder.
   *
   * @param scenarioIndex  the index of the scenario containing the shift
   * @param nodeIdentifier  the identifier of the node to which the shift should be applied
   * @param shiftAmount  the size of the shift
   * @return this builder
   */
  public CurvePointShiftsBuilder addShift(int scenarioIndex, Object nodeIdentifier, double shiftAmount) {
    ArgChecker.notNull(nodeIdentifier, "nodeIdentifier");
    ArgChecker.notNegative(scenarioIndex, "scenarioIndex");
    shifts.put(Pair.of(scenarioIndex, nodeIdentifier), shiftAmount);
    return this;
  }

  /**
   * Adds multiple shifts to the builder.
   *
   * @param scenarioIndex  the index of the scenario containing the shifts
   * @param shiftMap  the shift amounts, keyed by the identifier of the node to which they should be applied
   * @return this builder
   */
  public CurvePointShiftsBuilder addShifts(int scenarioIndex, Map<?, Double> shiftMap) {
    ArgChecker.notNull(shiftMap, "shiftMap");
    ArgChecker.notNegative(scenarioIndex, "scenarioIndex");
    MapStream.of(shiftMap).forEach((id, shift) -> shifts.put(Pair.of(scenarioIndex, id), shift));
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance of {@link CurvePointShifts} built from the data in this builder.
   *
   * @return an instance of {@link CurvePointShifts} built from the data in this builder
   */
  public CurvePointShifts build() {
    // This finds the scenario count by finding the maximum index and adding 1.
    // If OptionalInt had map() it could be written more sensibly as: ...max().map(i -> i + 1).orElse(0)
    // but it doesn't, hence using -1 and adding 1 to it for the case of zero scenarios
    int scenarioCount = shifts.keySet().stream()
        .mapToInt(Pair::getFirst)
        .max()
        .orElse(-1) + 1;
    List<Object> nodeIdentifiers = shifts.keySet().stream()
        .map(Pair::getSecond)
        .distinct() // Use distinct to preserve order. Collecting to a set wouldn't preserve it
        .collect(toImmutableList());
    DoubleMatrix shiftMatrix =
        DoubleMatrix.of(scenarioCount, nodeIdentifiers.size(), (r, c) -> shiftValue(r, nodeIdentifiers.get(c)));
    return new CurvePointShifts(shiftType, shiftMatrix, nodeIdentifiers);
  }

  private double shiftValue(int scenarioIndex, Object nodeIdentifier) {
    Double shift = shifts.get(Pair.of(scenarioIndex, nodeIdentifier));
    return shift != null ? shift : 0;
  }
}
