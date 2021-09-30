package com.opengamma.strata.pricer.fxopt.utils;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.FxOptionProduct;
import com.opengamma.strata.product.fx.FxOptionTrade;
import com.opengamma.strata.product.fx.FxProduct;
import com.opengamma.strata.product.fx.FxTrade;
import com.opengamma.strata.product.fx.ResolvedFxSingle;

public class FxUtils {

  //-------------------------------------------------------------------------

  /**
   * Calculates the forward exchange rate for given FX product.
   *
   * @param fxProduct the FX product
   * @param ratesProvider the rates provider
   * @return the forward rate
   */
  public static FxRate forwardFxRate(FxProduct fxProduct, RatesProvider ratesProvider) {
    CurrencyPair strikePair = fxProduct.getCurrencyPair();
    LocalDate paymentDate = fxProduct.getPaymentDate();
    double forwardRate = ratesProvider.fxForwardRates(strikePair).rate(strikePair.getBase(), paymentDate);
    return FxRate.of(strikePair, forwardRate);
  }

  //-------------------------------------------------------------------------

  /**
   * Calculates the forward exchange rate for given FX trade.
   *
   * @param fxTrade the FX trade
   * @param ratesProvider the rates provider
   * @return the forward rate
   */
  public static FxRate forwardFxRate(FxTrade fxTrade, RatesProvider ratesProvider) {
    return forwardFxRate(fxTrade.getProduct(), ratesProvider);
  }

  public static FxRate forwardFxRate(ResolvedFxSingle fxTrade, RatesProvider ratesProvider) {
    return forwardFxRate(fxTrade.getProduct(), ratesProvider);
  }

  //-------------------------------------------------------------------------

  /**
   * Calculates the implied Black volatility of the FX barrier option product.
   *
   * @param option the option product
   * @param ratesProvider the rates provider
   * @param volatilities the Black volatility provider
   * @return the implied volatility of the product
   * @throws IllegalArgumentException if the option has expired
   */
  public static double impliedVolatility(
      FxOptionProduct option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ZonedDateTime expiry = option.getExpiry();
    double timeToExpiry = volatilities.relativeTime(expiry);
    if (timeToExpiry <= 0d) {
      throw new IllegalArgumentException("valuation is after option's expiry.");
    }
    FxRate forward = forwardFxRate(option, ratesProvider);
    CurrencyPair strikePair = option.getCurrencyPair();
    double strike = option.getStrike();
    return volatilities.volatility(strikePair, expiry, strike, forward.fxRate(strikePair));
  }

  //-------------------------------------------------------------------------

  /**
   * Calculates the implied Black volatility of the FX barrier option trade.
   *
   * @param optionTrade the option trade
   * @param ratesProvider the rates provider
   * @param volatilities the Black volatility provider
   * @return the implied volatility of the product
   * @throws IllegalArgumentException if the option has expired
   */
  public static double impliedVolatility(
      FxOptionTrade optionTrade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {
    return impliedVolatility(optionTrade.getProduct(), ratesProvider, volatilities);
  }
}
