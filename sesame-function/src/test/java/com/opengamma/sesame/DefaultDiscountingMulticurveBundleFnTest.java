/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;

@Test(groups = TestGroup.UNIT)
public class DefaultDiscountingMulticurveBundleFnTest {

  private static final String IMPLIED_DEPO = "implied depo";

  private DefaultDiscountingMulticurveBundleFn _fn;
  
  private Environment _env = mock(Environment.class);
  private CurveConstructionConfiguration _ccc = mock(CurveConstructionConfiguration.class);
  private CurveConstructionConfiguration _cccExo = mock(CurveConstructionConfiguration.class);
  
  private FXMatrixFn _fxmProvider = mock(FXMatrixFn.class);
  
  
  @BeforeMethod
  public void setup() {
    _fn = new DefaultDiscountingMulticurveBundleFn(null, null, null, _fxmProvider, null, null, null, null, null, null, StringSet.of(IMPLIED_DEPO));
  }
  
  
  
  @SuppressWarnings("unchecked")
  @Test
  public void generateBundle() {
    
    when(_fxmProvider.getFXMatrix(_env, _ccc)).thenReturn(Result.success(mock(FXMatrix.class)));
    when(_ccc.resolveCurveConfigurations()).thenReturn(Collections.singletonList(_cccExo));
    when(_fxmProvider.getFXMatrix(_env, _cccExo)).thenReturn(Result.<FXMatrix> failure(new Exception()));
    CurveGroupConfiguration cgc = mock(CurveGroupConfiguration.class);
    when(_ccc.getCurveGroups()).thenReturn(Lists.newArrayList(cgc));
    CurveDefinition cd = new CurveDefinition(IMPLIED_DEPO, Collections.<CurveNode> emptySet());
    @SuppressWarnings("rawtypes")
    Map map = ImmutableMap.of(cd, Lists.newArrayList());
    when(cgc.resolveTypesForCurves()).thenReturn(map);
    Result<Pair<MulticurveProviderDiscount,CurveBuildingBlockBundle>> result = _fn.generateBundle(_env, _ccc);
    
    assertTrue("Expected failure", !result.isSuccess());
    
  }
}
