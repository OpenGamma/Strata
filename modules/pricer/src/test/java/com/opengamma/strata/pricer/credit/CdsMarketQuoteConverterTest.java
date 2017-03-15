/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsQuote;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.type.CdsQuoteConvention;

/**
 * Test {@link CdsMarketQuoteConverter}.
 */
@Test
public class CdsMarketQuoteConverterTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CdsMarketQuoteConverter CONV = CdsMarketQuoteConverter.DEFAULT;
  private static final CdsMarketQuoteConverter CONV_MARKIT_FIX = new CdsMarketQuoteConverter(AccrualOnDefaultFormula.MARKIT_FIX);
  private static final IsdaCompliantCreditCurveCalibrator CALIB = FastCreditCurveCalibrator.standard();

  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "AAB");
  private static final HolidayCalendarId DEFAULT_CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final LocalDate TODAY = LocalDate.of(2008, 9, 19);
  private static final LocalDate START_DATE = LocalDate.of(2007, 3, 20);
  private static final LocalDate END_DATE = LocalDate.of(2015, 12, 20);
  private static final LocalDate[] MATURITIES = new LocalDate[] {
      LocalDate.of(2008, 12, 20), LocalDate.of(2009, 6, 20), LocalDate.of(2010, 6, 20), LocalDate.of(2011, 6, 20),
      LocalDate.of(2012, 6, 20), LocalDate.of(2014, 6, 20), LocalDate.of(2017, 6, 20)};
  // yield curve
  private static final DoubleArray DSC_TIME = DoubleArray.ofUnsafe(new double[] {
      0.09315068493150684, 0.18082191780821918, 0.2602739726027397, 0.5068493150684932, 0.7589041095890411, 1.010958904109589,
      2.010958904109589, 3.010958904109589, 4.016438356164384, 5.013698630136987, 6.013698630136987, 7.013698630136987,
      8.016438356164384, 9.021917808219179, 10.01917808219178, 11.016438356164384, 12.01917808219178, 15.024657534246575,
      20.030136986301372, 25.027397260273972, 30.030136986301372,});
  private static final DoubleArray DSC_RATE = DoubleArray.ofUnsafe(new double[] {
      0.004510969198370304, 0.00930277781406035, 0.012152971715618414, 0.017638643770220588, 0.019260098011444397,
      0.02072958904811958, 0.01658424716087226, 0.02035074046575936, 0.023313764334801694, 0.025640888682876155,
      0.027453756419591822, 0.028832553111413566, 0.029976760913966324, 0.030912599984222154, 0.03173930709211652,
      0.03249979503727117, 0.033314372450170285, 0.034875344837724434, 0.03532470846114178, 0.03501411934224827,
      0.03490957722439039,});
  private static final CurveName DSC_NAME = CurveName.of("gbp_dsc");
  private static final IsdaCreditDiscountFactors DSC_CURVE =
      IsdaCreditDiscountFactors.of(GBP, TODAY, DSC_NAME, DSC_TIME, DSC_RATE, ACT_365F);
  // recovery rate
  private static final double RECOVERY_RATE = 0.4;
  private static final ConstantRecoveryRates REC_RATES = ConstantRecoveryRates.of(LEGAL_ENTITY, TODAY, RECOVERY_RATE);
  // rates provider without credit curve
  private static final CreditRatesProvider RATES_PROVIDER = ImmutableCreditRatesProvider.builder()
      .discountCurves(ImmutableMap.of(GBP, DSC_CURVE))
      .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, REC_RATES))
      .valuationDate(TODAY)
      .build();

  private static final double ONE_BP = 1.0e-4;
  private static final double TOL = 1.e-15;

  public void standardQuoteTest() {
    double pointsUpFront = 0.007;
    double expectedParSpread = 0.011112592882846; // taken from Excel-ISDA 1.8.2
    double premium = 100d * ONE_BP;
    Cds product = Cds.of(BUY, LEGAL_ENTITY, GBP, 1.0e6, START_DATE, END_DATE, Frequency.P3M, DEFAULT_CALENDAR, premium);
    TradeInfo info =
        TradeInfo.builder().tradeDate(TODAY).settlementDate(product.getSettlementDateOffset().adjust(TODAY, REF_DATA)).build();
    ResolvedCdsTrade trade = CdsTrade.builder().product(product).info(info).build().resolve(REF_DATA);
    CdsQuote pufQuote = CdsQuote.of(CdsQuoteConvention.POINTS_UPFRONT, pointsUpFront);
    CdsQuote quotedSpread = CONV.quotedSpreadFromPointsUpfront(trade, pufQuote, RATES_PROVIDER, REF_DATA);
    assertEquals(quotedSpread.getQuotedValue(), expectedParSpread, 1e-14);
    assertTrue(quotedSpread.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD));
    CdsQuote derivedPuf = CONV.pointsUpFrontFromQuotedSpread(trade, quotedSpread, RATES_PROVIDER, REF_DATA);
    assertEquals(derivedPuf.getQuotedValue(), pointsUpFront, 1e-15);
    assertTrue(derivedPuf.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT));
  }

  public void standardQuoteTest2() {
    double quotedSpread = 143.4 * ONE_BP;
    double expectedPuf = -0.2195134271137960; // taken from Excel-ISDA 1.8.2
    double premium = 500d * ONE_BP;
    Cds product = Cds.of(SELL, LEGAL_ENTITY, GBP, 1.0e8, START_DATE, END_DATE, Frequency.P6M, DEFAULT_CALENDAR, premium);
    TradeInfo info =
        TradeInfo.builder().tradeDate(TODAY).settlementDate(product.getSettlementDateOffset().adjust(TODAY, REF_DATA)).build();
    ResolvedCdsTrade trade = CdsTrade.builder().product(product).info(info).build().resolve(REF_DATA);
    CdsQuote quotedSpreadQuoted = CdsQuote.of(CdsQuoteConvention.QUOTED_SPREAD, quotedSpread);
    CdsQuote derivedPuf = CONV.pointsUpFrontFromQuotedSpread(trade, quotedSpreadQuoted, RATES_PROVIDER, REF_DATA);
    assertEquals(derivedPuf.getQuotedValue(), expectedPuf, 5e-13);
    assertTrue(derivedPuf.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT));
    CdsQuote derivedQuotedSpread = CONV.quotedSpreadFromPointsUpfront(trade, derivedPuf, RATES_PROVIDER, REF_DATA);
    assertEquals(derivedQuotedSpread.getQuotedValue(), quotedSpread, 1e-15);
    assertTrue(derivedQuotedSpread.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD));
  }

  public void parSpreadQuoteTest() {
    int nPillars = MATURITIES.length;
    List<Cds> products = new ArrayList<>(nPillars);
    List<CdsQuote> quotes = new ArrayList<>(nPillars);
    double[] parSpreads = new double[] {0.00769041167742121, 0.010780108645654813, 0.014587245777777417, 0.017417253343028126,
        0.01933997409465104, 0.022289540511698912, 0.025190509434219924};
    for (int i = 0; i < nPillars; ++i) {
      products.add(
          Cds.of(BUY, LEGAL_ENTITY, GBP, 1.0e6, START_DATE, MATURITIES[i], Frequency.P3M, DEFAULT_CALENDAR, parSpreads[i]));
      quotes.add(CdsQuote.of(CdsQuoteConvention.PAR_SPREAD, parSpreads[i]));
    }
    TradeInfo info = TradeInfo.builder()
        .tradeDate(TODAY)
        .settlementDate(products.get(0).getSettlementDateOffset().adjust(TODAY, REF_DATA))
        .build();
    List<ResolvedCdsTrade> trades = products.stream()
        .map(p -> CdsTrade.builder().product(p).info(info).build().resolve(REF_DATA))
        .collect(Collectors.toList());
    List<CdsQuote> pufsComp =
        CONV.quotesFromParSpread(trades, quotes, RATES_PROVIDER, CdsQuoteConvention.POINTS_UPFRONT, REF_DATA);
    List<CdsQuote> pufsMfComp =
        CONV_MARKIT_FIX.quotesFromParSpread(trades, quotes, RATES_PROVIDER, CdsQuoteConvention.POINTS_UPFRONT, REF_DATA);
    List<CdsQuote> qssComp =
        CONV.quotesFromParSpread(trades, quotes, RATES_PROVIDER, CdsQuoteConvention.QUOTED_SPREAD, REF_DATA);
    List<CdsQuote> qssMfComp =
        CONV_MARKIT_FIX.quotesFromParSpread(trades, quotes, RATES_PROVIDER, CdsQuoteConvention.QUOTED_SPREAD, REF_DATA);
    for (int i = 0; i < nPillars; ++i) {
      assertEquals(pufsComp.get(i).getQuotedValue(), 0d, TOL);
      assertTrue(pufsComp.get(i).getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT));
      assertEquals(pufsMfComp.get(i).getQuotedValue(), 0d, TOL);
      assertTrue(pufsMfComp.get(i).getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT));
    }
    for (int i = 0; i < nPillars; ++i) {
      CdsQuote qsRe = CONV.quotedSpreadFromPointsUpfront(trades.get(i), pufsComp.get(i), RATES_PROVIDER, REF_DATA);
      CdsQuote qsMfRe = CONV_MARKIT_FIX.quotedSpreadFromPointsUpfront(trades.get(i), pufsMfComp.get(i), RATES_PROVIDER, REF_DATA);
      assertEquals(qsRe.getQuotedValue(), qssComp.get(i).getQuotedValue(), TOL);
      assertEquals(qsMfRe.getQuotedValue(), qssMfComp.get(i).getQuotedValue(), TOL);
    }
  }

  public void pricePufTest() {
    double premium = 150d * ONE_BP;
    Cds product = Cds.of(BUY, LEGAL_ENTITY, GBP, 1.0e6, START_DATE, END_DATE, Frequency.P3M, DEFAULT_CALENDAR, premium);
    TradeInfo info =
        TradeInfo.builder().tradeDate(TODAY).settlementDate(product.getSettlementDateOffset().adjust(TODAY, REF_DATA)).build();
    ResolvedCdsTrade trade = CdsTrade.builder().product(product).info(info).build().resolve(REF_DATA);
    NodalCurve cc = CALIB.calibrate(ImmutableList.of(trade), DoubleArray.of(0.0123), DoubleArray.of(0.0),
        CurveName.of("test"), TODAY, DSC_CURVE, REC_RATES, REF_DATA);
    CreditRatesProvider rates = RATES_PROVIDER.toImmutableCreditRatesProvider().toBuilder()
        .creditCurves(ImmutableMap.of(
            Pair.of(LEGAL_ENTITY, GBP),
            LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, IsdaCreditDiscountFactors.of(GBP, TODAY, cc))))
        .build();
    double pointsUpFront = CONV.pointsUpfront(trade, rates, REF_DATA);
    double cleanPrice = CONV.cleanPrice(trade, rates, REF_DATA);
    double cleanPriceRe = CONV.cleanPriceFromPointsUpfront(pointsUpFront);
    assertEquals(cleanPrice, cleanPriceRe, TOL);
  }

}
