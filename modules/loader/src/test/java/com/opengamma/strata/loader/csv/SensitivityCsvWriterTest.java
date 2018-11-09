/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.sensitivity.CurveSensitivitiesType.ZERO_RATE_DELTA;
import static com.opengamma.strata.market.sensitivity.CurveSensitivitiesType.ZERO_RATE_GAMMA;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.market.sensitivity.CurveSensitivities;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.PortfolioItemInfo;

/**
 * Test {@link SensitivityCsvLoader}.
 */
@Test
public final class SensitivityCsvWriterTest {

  private static final AttributeType<String> CCP_ATTR = AttributeType.of("CCP");
  private static final SensitivityCsvInfoSupplier SUPPLIER_CCP = new SensitivityCsvInfoSupplier() {
    @Override
    public List<String> headers(CurveSensitivities curveSens) {
      return ImmutableList.of("CCP");
    }

    @Override
    public List<String> values(
        List<String> additionalHeaders,
        CurveSensitivities curveSens,
        CurrencyParameterSensitivity paramSens) {

      return ImmutableList.of(curveSens.getInfo().findAttribute(CCP_ATTR).orElse(""));
    }
  };
  private static final SensitivityCsvLoader LOADER = SensitivityCsvLoader.standard();
  private static final SensitivityCsvWriter WRITER = SensitivityCsvWriter.standard();
  private static final SensitivityCsvWriter WRITER_CCP = SensitivityCsvWriter.of(SUPPLIER_CCP);

  //-------------------------------------------------------------------------
  public void test_write_standard() {
    CurveName curve1 = CurveName.of("GBDSC");
    CurveName curve2 = CurveName.of("GBFWD");
    // listed in reverse order to check ordering
    CurveSensitivities sens = CurveSensitivities.builder(PortfolioItemInfo.empty().withAttribute(CCP_ATTR, "LCH"))
        .add(ZERO_RATE_GAMMA, curve2, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_3M), 1)
        .add(ZERO_RATE_GAMMA, curve2, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_6M), 2)
        .add(ZERO_RATE_DELTA, curve2, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_3M), 3)
        .add(ZERO_RATE_DELTA, curve2, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_6M), 5)
        .add(ZERO_RATE_DELTA, curve1, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_3M), 2)
        .add(ZERO_RATE_DELTA, curve1, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_6M), 4)
        .build();

    StringBuffer buf = new StringBuffer();
    WRITER_CCP.write(sens, buf);
    String content = buf.toString();

    String expected = "" +
        "Reference,Sensitivity Type,Sensitivity Tenor,Currency,Value,CCP\n" +
        "GBDSC,ZeroRateDelta,3M,GBP,2.0,LCH\n" +
        "GBDSC,ZeroRateDelta,6M,GBP,4.0,LCH\n" +
        "GBFWD,ZeroRateDelta,3M,GBP,3.0,LCH\n" +
        "GBFWD,ZeroRateDelta,6M,GBP,5.0,LCH\n" +
        "GBFWD,ZeroRateGamma,3M,GBP,1.0,LCH\n" +
        "GBFWD,ZeroRateGamma,6M,GBP,2.0,LCH\n";
    assertEquals(content, expected);
  }

  public void test_write_standard_withDate() {
    CurveName curve1 = CurveName.of("GBDSC");
    CurveName curve2 = CurveName.of("GBFWD");
    // listed in reverse order to check ordering
    CurveSensitivities sens = CurveSensitivities.builder(PortfolioItemInfo.empty().withAttribute(CCP_ATTR, "LCH"))
        .add(ZERO_RATE_GAMMA, curve2, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_3M), 1)
        .add(ZERO_RATE_GAMMA, curve2, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_6M), 2)
        .add(ZERO_RATE_DELTA, curve2, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_3M), 3)
        .add(ZERO_RATE_DELTA, curve2, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_6M), 5)
        .add(ZERO_RATE_DELTA, curve1, Currency.GBP, TenorDateParameterMetadata.of(date(2018, 6, 30), Tenor.TENOR_3M), 2)
        .add(ZERO_RATE_DELTA, curve1, Currency.GBP, TenorParameterMetadata.of(Tenor.TENOR_6M), 4)
        .build();

    StringBuffer buf = new StringBuffer();
    WRITER_CCP.write(sens, buf);
    String content = buf.toString();

    String expected = "" +
        "Reference,Sensitivity Type,Sensitivity Tenor,Sensitivity Date,Currency,Value,CCP\n" +
        "GBDSC,ZeroRateDelta,3M,2018-06-30,GBP,2.0,LCH\n" +
        "GBDSC,ZeroRateDelta,6M,,GBP,4.0,LCH\n" +
        "GBFWD,ZeroRateDelta,3M,,GBP,3.0,LCH\n" +
        "GBFWD,ZeroRateDelta,6M,,GBP,5.0,LCH\n" +
        "GBFWD,ZeroRateGamma,3M,,GBP,1.0,LCH\n" +
        "GBFWD,ZeroRateGamma,6M,,GBP,2.0,LCH\n";
    assertEquals(content, expected);
  }

  public void test_write_standard_roundTrip() {
    CharSource source =
        ResourceLocator.ofClasspath("com/opengamma/strata/loader/csv/sensitivity-standard.csv").getCharSource();
    ValueWithFailures<ListMultimap<String, CurveSensitivities>> parsed1 = LOADER.parse(ImmutableList.of(source));
    assertEquals(parsed1.getFailures().size(), 0, parsed1.getFailures().toString());
    assertEquals(parsed1.getValue().size(), 1);
    List<CurveSensitivities> csensList1 = parsed1.getValue().get("");
    assertEquals(csensList1.size(), 1);
    CurveSensitivities csens1 = csensList1.get(0);

    StringBuffer buf = new StringBuffer();
    WRITER.write(csens1, buf);
    String content = buf.toString();

    ValueWithFailures<ListMultimap<String, CurveSensitivities>> parsed2 =
        LOADER.parse(ImmutableList.of(CharSource.wrap(content)));
    assertEquals(parsed2.getFailures().size(), 0, parsed2.getFailures().toString());
    assertEquals(parsed2.getValue().size(), 1);
    List<CurveSensitivities> csensList2 = parsed2.getValue().get("");
    assertEquals(csensList2.size(), 1);
    CurveSensitivities csens2 = csensList2.get(0);

    assertEquals(csens2, csens1);
  }

}
