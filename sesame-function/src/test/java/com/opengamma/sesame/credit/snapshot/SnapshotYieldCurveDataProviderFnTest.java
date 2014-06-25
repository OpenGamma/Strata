package com.opengamma.sesame.credit.snapshot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.DataNotFoundException;
import com.opengamma.core.link.SnapshotLink;
import com.opengamma.financial.analytics.isda.credit.YieldCurveData;
import com.opengamma.financial.analytics.isda.credit.YieldCurveDataSnapshot;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;

/**
 * Tests {@link SnapshotYieldCurveDataProviderFn}.
 */
public class SnapshotYieldCurveDataProviderFnTest {
  
  private SnapshotYieldCurveDataProviderFn _fnWithBadLink;
  private SnapshotYieldCurveDataProviderFn _fnWithUSDCurve;

  @SuppressWarnings("unchecked")
  @BeforeClass
  public void beforeClass() {
    
    SnapshotLink<YieldCurveDataSnapshot> badLink = mock(SnapshotLink.class);
    when(badLink.resolve()).thenThrow(new DataNotFoundException("test"));
    _fnWithBadLink = new SnapshotYieldCurveDataProviderFn(badLink);
    
    YieldCurveDataSnapshot snapshot = YieldCurveDataSnapshot.builder()
                      .name("")
                      .yieldCurves(ImmutableMap.of(Currency.USD, mock(YieldCurveData.class)))
                      .build();
    SnapshotLink<YieldCurveDataSnapshot> goodLink = SnapshotLink.resolved(snapshot);
    
    _fnWithUSDCurve = new SnapshotYieldCurveDataProviderFn(goodLink);
  }
  
  @Test
  public void testFnWithBadLink() {
    
    Result<YieldCurveData> result = _fnWithBadLink.loadYieldCurveData(Currency.USD);
    
    assertFalse("Link threw exception so function should fail.", result.isSuccess());
    
  }

  @Test
  public void testFnMissingData() {
    
    Result<YieldCurveData> result = _fnWithUSDCurve.loadYieldCurveData(Currency.GBP);
    
    assertFalse("GBP is missing so result should be failure.", result.isSuccess());
    
  }

  @Test
  public void testFn() {
    
    Result<YieldCurveData> result = _fnWithUSDCurve.loadYieldCurveData(Currency.USD);
    
    assertTrue("USD present in snapshot so should succeed.", result.isSuccess());
    
  }

}
