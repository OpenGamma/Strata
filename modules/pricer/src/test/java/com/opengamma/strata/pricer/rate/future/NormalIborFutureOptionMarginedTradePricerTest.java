/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.GridInterpolator2D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.UnitSecurity;
import com.opengamma.strata.finance.rate.future.IborFutureOption;
import com.opengamma.strata.finance.rate.future.IborFutureOptionTrade;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.option.OptionPointSensitivity;

/**
 * Tests {@link NormalIborFutureOptionMarginedTradePricer}
 */
public class NormalIborFutureOptionMarginedTradePricerTest {

  private static final Interpolator1D LINEAR_FLAT =
      CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final double[] TIMES =
      new double[] {0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00 };
  private static final double[] MONEYNESS_PRICES =
      new double[] {-0.02, -0.02, -0.02, -0.01, -0.01, -0.01, 0.00, 0.00, 0.00, 0.01, 0.01, 0.01 };
  private static final double[] NORMAL_VOL =
      new double[] {0.01, 0.011, 0.012, 0.011, 0.012, 0.013, 0.012, 0.013, 0.014, 0.010, 0.012, 0.014 };
  private static final InterpolatedDoublesSurface PARAMETERS_PRICE =
      new InterpolatedDoublesSurface(TIMES, MONEYNESS_PRICES, NORMAL_VOL, INTERPOLATOR_2D);

  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId VALUATION_ZONE = ZoneId.of("Europe/London");
  
  private static final NormalVolatilityIborFutureParameters VOL_SIMPLE_MONEY_PRICE =
      new NormalVolatilityExpSimpleMoneynessIborFutureParameters(
          PARAMETERS_PRICE, true, GBP_LIBOR_2M, ACT_365F, VALUATION_DATE, VALUATION_TIME, VALUATION_ZONE);
  
  private static final IborFutureOption FUTURE_OPTION_PRODUCT = IborFutureDummyData.IBOR_FUTURE_OPTION_2;
  private static final StandardId OPTION_SECURITY_ID = StandardId.of("OG-Ticker", "OptionSec");
  private static final Security<IborFutureOption> IBOR_FUTURE_OPTION_SECURITY =
      UnitSecurity.builder(FUTURE_OPTION_PRODUCT).standardId(OPTION_SECURITY_ID).build();
  private static final StandardId OPTION_TRADE_ID = StandardId.of("OG-Ticker", "123");
  private static final LocalDate TRADE_DATE = date(2015, 2, 16);
  private static final long OPTION_QUANTITY = 12345;
  private static final double TRADE_PRICE = 0.0100;
  private static final IborFutureOptionTrade FUTURE_OPTION_TRADE_TD = IborFutureOptionTrade.builder()
      .standardId(OPTION_TRADE_ID).tradeInfo(TradeInfo.builder().tradeDate(VALUATION_DATE).build())
      .securityLink(SecurityLink.resolved(IBOR_FUTURE_OPTION_SECURITY)).quantity(OPTION_QUANTITY)
      .initialPrice(TRADE_PRICE).build();
  private static final IborFutureOptionTrade FUTURE_OPTION_TRADE = IborFutureOptionTrade.builder()
      .standardId(OPTION_TRADE_ID).tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
      .securityLink(SecurityLink.resolved(IBOR_FUTURE_OPTION_SECURITY)).quantity(OPTION_QUANTITY)
      .initialPrice(TRADE_PRICE).build();
  
  private static final double RATE = 0.015;
  private static final PricingEnvironment ENV_MOCK = mock(PricingEnvironment.class);
  static {
    when(ENV_MOCK.iborIndexRate(FUTURE_OPTION_PRODUCT.getUnderlying().getProduct().getIndex(), 
        FUTURE_OPTION_PRODUCT.getUnderlying().getProduct().getLastTradeDate())).thenReturn(RATE);
  }

  private static final DiscountingIborFutureProductPricer FUTURE_PRICER = DiscountingIborFutureProductPricer.DEFAULT;
  private static final NormalIborFutureOptionMarginedProductPricer OPTION_PRODUCT_PRICER = 
      new NormalIborFutureOptionMarginedProductPricer(FUTURE_PRICER);
  private static final NormalIborFutureOptionMarginedTradePricer OPTION_TRADE_PRICER = 
      new NormalIborFutureOptionMarginedTradePricer(OPTION_PRODUCT_PRICER);
  
  private static final double TOLERANCE_PV = 1.0E-2;
  private static final double TOLERANCE_PV_DELTA = 1.0E-1;

  // ----------     present value     ----------
  @Test
  public void presentvalue_from_no_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    IborFutureOptionTrade trade = IborFutureOptionTrade.builder()
        .standardId(OPTION_TRADE_ID).tradeInfo(TradeInfo.builder().build())
        .securityLink(SecurityLink.resolved(IBOR_FUTURE_OPTION_SECURITY)).quantity(OPTION_QUANTITY)
        .initialPrice(TRADE_PRICE).build();
    assertThrowsIllegalArg(() -> OPTION_TRADE_PRICER.presentValue(trade, VALUATION_DATE, optionPrice, lastClosingPrice));
  }
  
  @Test
  public void presentvalue_from_no_trade_price() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    IborFutureOptionTrade trade = IborFutureOptionTrade.builder()
        .standardId(OPTION_TRADE_ID).tradeInfo(TradeInfo.builder().tradeDate(VALUATION_DATE).build())
        .securityLink(SecurityLink.resolved(IBOR_FUTURE_OPTION_SECURITY)).quantity(OPTION_QUANTITY)
        .build();
    assertThrowsIllegalArg(() -> OPTION_TRADE_PRICER.presentValue(trade, VALUATION_DATE, optionPrice, lastClosingPrice));
  }
  
  @Test
  public void presentvalue_from_option_price_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice  = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE_TD, VALUATION_DATE, optionPrice, lastClosingPrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, optionPrice) - 
        OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, TRADE_PRICE) ) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);    
  }

  @Test
  public void presentvalue_from_option_price_after_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice  = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE, VALUATION_DATE, optionPrice, lastClosingPrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, optionPrice) - 
        OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, lastClosingPrice) ) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);    
  }

  @Test
  public void presentvalue_from_future_price() {
    double futurePrice = 0.9875;
    double lastClosingPrice  = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice, lastClosingPrice);
    double optionPrice = 
        OPTION_PRODUCT_PRICER.price(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, futurePrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, optionPrice) - 
        OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, lastClosingPrice) ) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);    
  }

  @Test
  public void presentvalue_from_env() {
    double lastClosingPrice  = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE, lastClosingPrice);
    double optionPrice = 
        OPTION_PRODUCT_PRICER.price(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, optionPrice) - 
        OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, lastClosingPrice) ) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);    
  }

  // ----------     present value sensitivity     ----------

  @Test
  public void presentvalueSensitivity_from_env() {
    PointSensitivities psProduct = 
        OPTION_PRODUCT_PRICER.priceSensitivity(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    PointSensitivities psExpected = psProduct
        .multipliedBy(OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, 1) * OPTION_QUANTITY);
    PointSensitivities psComputed = OPTION_TRADE_PRICER
    .presentValueSensitivity(FUTURE_OPTION_TRADE, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(psComputed.equalWithTolerance(psExpected, TOLERANCE_PV_DELTA));    
  }

  // ----------     present value normal vol sensitivity     ----------

  @Test
  public void presentvalue_normalVolSensitivity_from_env() {
    OptionPointSensitivity psProduct = 
        OPTION_PRODUCT_PRICER.priceSensitivityNormalVolatility(FUTURE_OPTION_PRODUCT, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    OptionPointSensitivity psExpected = psProduct.withSensitivity(
        psProduct.getValue() * OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, 1) * OPTION_QUANTITY);
    OptionPointSensitivity psComputed = OPTION_TRADE_PRICER
        .presentValueSensitivityNormalVolatility(FUTURE_OPTION_TRADE, ENV_MOCK, VOL_SIMPLE_MONEY_PRICE);
    assertTrue(psComputed.equalWithTolerance(psExpected, TOLERANCE_PV_DELTA));    
  }
  
}
