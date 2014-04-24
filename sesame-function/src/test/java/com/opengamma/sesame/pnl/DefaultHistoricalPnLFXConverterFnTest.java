package com.opengamma.sesame.pnl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.Sets;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.integration.regression.EqualityChecker;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.timeseries.date.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.LocalDateRange;

/**
 * Tests calculation of conversion from historical rates to today's rates.
 */
public class DefaultHistoricalPnLFXConverterFnTest {

  private static double[] FX_RATES = {1.499, 1.5, 1.501, 1.502, 1.502, 1.503};
  private static double[] PNL = {100, 101, 99, 5, 55};
  private static LocalDate PNL_START = LocalDate.of(2014, 1, 8);
  
  private static double[] EXPECTED_PNL_WITH_START_FX = {99.7338656, 100.7984032, 98.86826347, 4.99667332, 54.96340652};
  private static double[] EXPECTED_PNL_WITH_END_FX = {99.8003992, 100.8656021, 98.93413174, 4.99667332, 55};
  
  private FXMatrixFn _fxMatrixFn;
  private HistoricalMarketDataFn _mdFn;
  
  private final CurrencyPair _ccyPair = CurrencyPair.of(Currency.GBP, Currency.USD);
  private final SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), mock(MarketDataSource.class));
  
  private final FXMatrix fxMatrix = new FXMatrix(_ccyPair.getBase());

  private LocalDateRange _range;
  private LocalDateRange _adjustedRange;
  
  private LocalDateDoubleTimeSeries _inputPnL;
  private LocalDateDoubleTimeSeries _reciprocalFxRates;
  
  @BeforeClass
  public void init() {
    
    LocalDate[] pnlDates = new LocalDate[PNL.length];
    LocalDate[] fxDates = new LocalDate[FX_RATES.length];
    fxDates[0] = PNL_START.minusDays(1);
    for (int i = 0; i < pnlDates.length; i++) {
      pnlDates[i] = PNL_START.plusDays(i);
      fxDates[i + 1] = PNL_START.plusDays(i);
    }
    
    _fxMatrixFn = mock(FXMatrixFn.class);
    _mdFn = mock(HistoricalMarketDataFn.class);
    
    fxMatrix.addCurrency(_ccyPair.getCounter(), _ccyPair.getBase(), 1. / FX_RATES[FX_RATES.length-1]);

    _inputPnL = ImmutableLocalDateDoubleTimeSeries.of(pnlDates, PNL);
    _reciprocalFxRates = ImmutableLocalDateDoubleTimeSeries.of(fxDates, FX_RATES).reciprocal();
    
    _range = LocalDateRange.of(PNL_START, PNL_START.plusDays(_inputPnL.size()-1), true);
    _adjustedRange = LocalDateRange.of(PNL_START.minusWeeks(1), _range.getEndDateInclusive(), true);
    
    when(_fxMatrixFn.getFXMatrix(env, Sets.newHashSet(_ccyPair.getBase(), _ccyPair.getCounter()))).thenReturn(Result.success(fxMatrix));

  }
  
  
  @Test
  public void convertToSpotEnd() {
    
    HistoricalPnLFXConverterFn fn = new DefaultHistoricalPnLFXConverterFn(_fxMatrixFn, _mdFn, PnLPeriodBound.END);
    
    when(_mdFn.getFxRates(env, _ccyPair, _range)).thenReturn(Result.success(_reciprocalFxRates));
    
    Result<LocalDateDoubleTimeSeries> result = fn.convertToSpotRate(env, _ccyPair, _inputPnL);
    
    assertTrue(result.isSuccess());
    
    assertEquals("Expected size of series to remain the same", _inputPnL.size(), result.getValue().size());
    assertTrue("Converted PnL did not match expected results.", EqualityChecker.equals(result.getValue().valuesArrayFast(), EXPECTED_PNL_WITH_END_FX, 0.000001));
    
  }
  
  @Test
  public void convertToSpotStart() {
    
    HistoricalPnLFXConverterFn fn = new DefaultHistoricalPnLFXConverterFn(_fxMatrixFn, _mdFn, PnLPeriodBound.START);
    
    when(_mdFn.getFxRates(env, _ccyPair, _adjustedRange)).thenReturn(Result.success(_reciprocalFxRates));
    
    Result<LocalDateDoubleTimeSeries> result = fn.convertToSpotRate(env, _ccyPair, _inputPnL);
    
    assertEquals("Expected size of series to remain the same", _inputPnL.size(), result.getValue().size());
    assertTrue("Converted PnL did not match expected results.", EqualityChecker.equals(result.getValue().valuesArrayFast(), EXPECTED_PNL_WITH_START_FX, 0.000001));
    
  }

}
