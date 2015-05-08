/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.index.PriceIndices.JP_CPI_EXF;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.PriceIndexCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.PriceIndexCurveSimple;
import com.opengamma.analytics.math.curve.ConstantDoublesCurve;
import com.opengamma.strata.basics.index.PriceIndex;

/**
 * Test.
 */
@Test
public class PriceIndexCurveMapTest {
  private static final PriceIndexCurve USCPIU_CURVE = new PriceIndexCurveSimple(new ConstantDoublesCurve(252.0d));
  private static final PriceIndexCurve GBPRI_CURVE = new PriceIndexCurveSimple(new ConstantDoublesCurve(252.0d));
  private static final PriceIndexCurve JPCPIEXF_CURVE = new PriceIndexCurveSimple(new ConstantDoublesCurve(194.0d));


  public void test_builder() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(
        US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    PriceIndexCurveMap test = PriceIndexCurveMap.builder().priceIndexCurves(map).build();
    assertEquals(test.getPriceIndexCurves(), map);
  }

  public void test_of_index() {
    PriceIndexCurveMap test = PriceIndexCurveMap.of(GB_RPI, GBPRI_CURVE);
    ImmutableMap<PriceIndex, PriceIndexCurve> expected = ImmutableMap.of(GB_RPI, GBPRI_CURVE);
    assertEquals(test.getPriceIndexCurves(), expected);
  }

  public void test_of_map() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE);
    PriceIndexCurveMap test = PriceIndexCurveMap.of(map);
    assertEquals(test.getPriceIndexCurves(), map);
  }

  public void test_combinedWith_index() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE);
    PriceIndexCurveMap test = PriceIndexCurveMap.of(map);
    ImmutableMap<PriceIndex, PriceIndexCurve> expected = ImmutableMap.of(
        US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    test = test.combinedWith(JP_CPI_EXF, JPCPIEXF_CURVE);
    assertEquals(test.getPriceIndexCurves(), expected);
  }

  public void test_combinedWith_index_same() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map = ImmutableMap.of(US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE);
    PriceIndexCurveMap test = PriceIndexCurveMap.of(map);
    assertThrowsIllegalArg(() -> test.combinedWith(US_CPI_U, USCPIU_CURVE));
  }

  public void test_combinedWith_other() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map1 = ImmutableMap.of(US_CPI_U, USCPIU_CURVE);
    ImmutableMap<PriceIndex, PriceIndexCurve> map2 = ImmutableMap.of(GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    PriceIndexCurveMap test = PriceIndexCurveMap.empty();
    PriceIndexCurveMap other1 = PriceIndexCurveMap.of(map1);
    PriceIndexCurveMap other2 = PriceIndexCurveMap.of(map2);
    PriceIndexCurveMap other3 = PriceIndexCurveMap.empty();
    test = test.combinedWith(other1);
    test = test.combinedWith(other2);
    test = test.combinedWith(other3);
    ImmutableMap<PriceIndex, PriceIndexCurve> expected = ImmutableMap.of(
        US_CPI_U, USCPIU_CURVE, GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    assertEquals(test.getPriceIndexCurves(), expected);
  }

  public void test_combinedWith_other_same() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map1 = ImmutableMap.of(GB_RPI, GBPRI_CURVE, US_CPI_U, USCPIU_CURVE);
    ImmutableMap<PriceIndex, PriceIndexCurve> map2 = ImmutableMap.of(GB_RPI, GBPRI_CURVE, JP_CPI_EXF, JPCPIEXF_CURVE);
    PriceIndexCurveMap test = PriceIndexCurveMap.of(map1);
    PriceIndexCurveMap other = PriceIndexCurveMap.of(map2);
    assertThrowsIllegalArg(() -> test.combinedWith(test.combinedWith(other)));
  }

  public void coverage() {
    ImmutableMap<PriceIndex, PriceIndexCurve> map1 = ImmutableMap.of(US_CPI_U, USCPIU_CURVE);
    ImmutableMap<PriceIndex, PriceIndexCurve> map2 = ImmutableMap.of(GB_RPI, GBPRI_CURVE);
    PriceIndexCurveMap test1 = PriceIndexCurveMap.builder().priceIndexCurves(map1).build();
    coverImmutableBean(test1);
    PriceIndexCurveMap test2 = PriceIndexCurveMap.builder().priceIndexCurves(map2).build();
    coverBeanEquals(test1, test2);
  }
}
