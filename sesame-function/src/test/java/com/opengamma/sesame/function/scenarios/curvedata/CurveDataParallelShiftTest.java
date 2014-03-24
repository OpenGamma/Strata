/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.DataFieldType;
import com.opengamma.financial.analytics.ircurve.strips.RateFutureNode;
import com.opengamma.financial.analytics.ircurve.strips.SwapNode;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.Tenor;

@Test(groups = TestGroup.UNIT)
public class CurveDataParallelShiftTest {

  private static final ExternalId ID1 = ExternalId.of("scheme", "1");
  private static final ExternalId ID2 = ExternalId.of("scheme", "2");
  private static final ExternalId ID3 = ExternalId.of("scheme", "3");
  private static final ExternalId ID4 = ExternalId.of("scheme", "4");
  private static final CurveNode NODE1 = node();
  private static final CurveNode NODE2 = node();
  private static final CurveNode NODE3 = futureNode();
  private static final CurveNode NODE4 = node();
  private static final ImmutableMap<ExternalIdBundle, Double> VALUE_MAP = ImmutableMap.of(ID1.toBundle(), 0.1,
                                                                                          ID2.toBundle(), 0.2,
                                                                                          ID3.toBundle(), 0.7,
                                                                                          ID4.toBundle(), 0.4);
  private static final List<CurveNodeWithIdentifier> NODES = Lists.newArrayList(nodeWithId(ID1, NODE1),
                                                                                nodeWithId(ID2, NODE2),
                                                                                nodeWithId(ID3, NODE3),
                                                                                nodeWithId(ID4, NODE4));
  private static final String CURVE_NAME = "curveName";
  private static final CurveSpecification CURVE_SPEC = new CurveSpecification(LocalDate.now(), CURVE_NAME, NODES);
  private static final double DELTA = 1e-8;

  private static CurveNode node() {
    return new SwapNode(Tenor.DAY,
                        Tenor.EIGHT_MONTHS,
                        ExternalId.of("convention", "payLeg"),
                        ExternalId.of("convention", "receiveLeg"),
                        "nodeMapper");
  }

  private static CurveNode futureNode() {
    return new RateFutureNode(1,
                              Tenor.DAY,
                              Tenor.EIGHT_MONTHS,
                              Tenor.EIGHT_YEARS,
                              ExternalId.of("convention", "foo"),
                              "nodeMapper");
  }

  private static CurveNodeWithIdentifier nodeWithId(ExternalId id, CurveNode node) {
    return new CurveNodeWithIdentifier(node, id, "fieldName", DataFieldType.OUTRIGHT);
  }

  @Test
  public void absolute() {
    CurveDataParallelShift shift = CurveDataParallelShift.absolute(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    Map<ExternalIdBundle, Double> shiftedValues = shift.apply(CURVE_SPEC, VALUE_MAP);
    assertEquals(0.2, shiftedValues.get(ID1.toBundle()), DELTA);
    assertEquals(0.3, shiftedValues.get(ID2.toBundle()), DELTA);
    assertEquals(0.6, shiftedValues.get(ID3.toBundle()), DELTA);
    assertEquals(0.5, shiftedValues.get(ID4.toBundle()), DELTA);
  }

  @Test
  public void parallel() {
    CurveDataParallelShift shift = CurveDataParallelShift.relative(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    Map<ExternalIdBundle, Double> shiftedValues = shift.apply(CURVE_SPEC, VALUE_MAP);
    assertEquals(0.11, shiftedValues.get(ID1.toBundle()), DELTA);
    assertEquals(0.22, shiftedValues.get(ID2.toBundle()), DELTA);
    assertEquals(0.67, shiftedValues.get(ID3.toBundle()), DELTA);
    assertEquals(0.44, shiftedValues.get(ID4.toBundle()), DELTA);
  }

  @Test
  public void noMatch() {
    CurveSpecification curveSpec = new CurveSpecification(LocalDate.now(), "a different name", NODES);
    CurveDataParallelShift shift = CurveDataParallelShift.absolute(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    Map<ExternalIdBundle, Double> shiftedValues = shift.apply(curveSpec, VALUE_MAP);
    assertEquals(0.1, shiftedValues.get(ID1.toBundle()), DELTA);
    assertEquals(0.2, shiftedValues.get(ID2.toBundle()), DELTA);
    assertEquals(0.7, shiftedValues.get(ID3.toBundle()), DELTA);
    assertEquals(0.4, shiftedValues.get(ID4.toBundle()), DELTA);
  }
}
