/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.StandardSchemes.OG_ETD_SCHEME;
import static com.opengamma.strata.basics.StandardSchemes.OG_SECURITY_SCHEME;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.Guavate.casting;
import static com.opengamma.strata.collect.Guavate.filtering;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.beans.test.BeanAssert.assertBeanEquals;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.io.StringCharSource;
import com.opengamma.strata.collect.result.FailureAttributeKeys;
import com.opengamma.strata.collect.result.FailureItem;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityPosition;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.ResolvableSecurityPosition;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.common.ExchangeIds;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.etd.EtdContractCode;
import com.opengamma.strata.product.etd.EtdContractSpec;
import com.opengamma.strata.product.etd.EtdContractSpecId;
import com.opengamma.strata.product.etd.EtdFuturePosition;
import com.opengamma.strata.product.etd.EtdFutureSecurity;
import com.opengamma.strata.product.etd.EtdIdUtils;
import com.opengamma.strata.product.etd.EtdOptionPosition;
import com.opengamma.strata.product.etd.EtdOptionSecurity;
import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.etd.EtdSettlementType;
import com.opengamma.strata.product.etd.EtdType;
import com.opengamma.strata.product.etd.EtdVariant;

/**
 * Test {@link PositionCsvLoader}.
 */
public class PositionCsvLoaderTest {

  private static final EtdContractCode FGBL = EtdContractCode.of("FGBL");
  private static final EtdContractCode OGBL = EtdContractCode.of("OGBL");

  private static final SecurityPosition SECURITY1 = SecurityPosition.builder()
      .info(PositionInfo.builder()
          .id(StandardId.of("OG", "123431"))
          .build())
      .securityId(SecurityId.of(OG_SECURITY_SCHEME, "AAPL"))
      .longQuantity(12d)
      .shortQuantity(14.5d)
      .build();
  private static final SecurityPosition SECURITY2 = SecurityPosition.builder()
      .info(PositionInfo.builder()
          .id(StandardId.of("OG", "123432"))
          .build())
      .securityId(SecurityId.of("BBG", "MSFT"))
      .longQuantity(20d)
      .shortQuantity(0d)
      .build();
  private static final SecurityPosition SECURITY3 = SecurityPosition.builder()
      .info(PositionInfo.builder()
          .id(StandardId.of("OG", "123433"))
          .build())
      .securityId(SecurityId.of(OG_SECURITY_SCHEME, "AAPL"))
      .longQuantity(12d)
      .shortQuantity(14.5d)
      .build();
  private static final GenericSecurityPosition SECURITY3FULL = GenericSecurityPosition.builder()
      .info(PositionInfo.builder()
          .id(StandardId.of("OG", "123433"))
          .build())
      .security(
          GenericSecurity.of(
              SecurityInfo.of(
                  SecurityId.of(OG_SECURITY_SCHEME, "AAPL"),
                  SecurityPriceInfo.of(5, CurrencyAmount.of(USD, 0.01), 10))))
      .longQuantity(12d)
      .shortQuantity(14.5d)
      .build();

  private static final ResourceLocator FILE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/positions.csv");

  //-------------------------------------------------------------------------
  @Test
  public void test_isKnownFormat() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    assertThat(test.isKnownFormat(FILE.getCharSource())).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_mixed() {
    PositionCsvLoader standard = PositionCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/mixed-trades-positions.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<Position>> loadedData = standard.parse(charSources);
    assertThat(loadedData.getFailures().size()).as(loadedData.getFailures().toString()).isEqualTo(1);
    assertThat(loadedData.getFailures()).first().hasToString(
        "PARSING: CSV position file 'mixed-trades-positions.csv' contained row with mixed " +
            "trade/position/sensitivity type 'FX/EtdFuture/CRIF' at line 6");

    List<Position> loadedPositions = loadedData.getValue();
    assertThat(loadedPositions).hasSize(2).allMatch(position -> position instanceof SecurityPosition);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_security() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<Position>> trades = test.load(FILE);

    List<SecurityPosition> filtered = trades.getValue().stream()
        .flatMap(filtering(SecurityPosition.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(2);

    assertBeanEquals(SECURITY1, filtered.get(0));
    assertBeanEquals(SECURITY2, filtered.get(1));
  }

  @Test
  public void test_load_genericSecurity() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<Position>> trades = test.load(FILE);

    List<GenericSecurityPosition> filtered = trades.getValue().stream()
        .flatMap(filtering(GenericSecurityPosition.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(1);

    assertBeanEquals(SECURITY3FULL, filtered.get(0));
  }

  @Test
  public void test_parseFiltering() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    assertThat(test.parse(ImmutableList.of(FILE.getCharSource())).getValue()).hasSize(3);  // 7 errors
    assertThat(test.parse(ImmutableList.of(FILE.getCharSource()), SecurityPosition.class).getValue()).hasSize(10);
    assertThat(test.parse(ImmutableList.of(FILE.getCharSource()), ResolvableSecurityPosition.class).getValue()).hasSize(3);
    assertThat(test.parse(ImmutableList.of(FILE.getCharSource()), GenericSecurityPosition.class).getValue()).hasSize(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_future() {
    EtdContractSpecId specId = EtdContractSpecId.of(OG_ETD_SCHEME, "F-ECAG-FGBL");
    EtdContractSpec contract = EtdContractSpec.builder()
        .id(specId)
        .type(EtdType.FUTURE)
        .exchangeId(ExchangeIds.ECAG)
        .contractCode(FGBL)
        .description("Dummy")
        .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
        .build();
    ReferenceData refData = ImmutableReferenceData.of(specId, contract);
    PositionCsvLoader test = PositionCsvLoader.of(refData);
    ValueWithFailures<List<EtdFuturePosition>> trades =
        test.parse(ImmutableList.of(FILE.getCharSource()), EtdFuturePosition.class);
    List<EtdFuturePosition> filtered = trades.getValue();
    assertThat(filtered).hasSize(4);

    EtdFuturePosition expected1 = EtdFuturePosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123421"))
            .build())
        .security(EtdFutureSecurity.of(contract, YearMonth.of(2017, 6), EtdVariant.ofMonthly()))
        .longQuantity(15d)
        .shortQuantity(2d)
        .build();
    assertBeanEquals(expected1, filtered.get(0));

    EtdFuturePosition expected2 = EtdFuturePosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123422"))
            .build())
        .security(EtdFutureSecurity.of(contract, YearMonth.of(2017, 6), EtdVariant.ofFlexFuture(13, EtdSettlementType.CASH)))
        .longQuantity(0d)
        .shortQuantity(13d)
        .build();
    assertBeanEquals(expected2, filtered.get(1));

    EtdFuturePosition expected3 = EtdFuturePosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123423"))
            .build())
        .security(EtdFutureSecurity.of(contract, YearMonth.of(2017, 6), EtdVariant.ofWeekly(2)))
        .longQuantity(0d)
        .shortQuantity(20d)
        .build();
    assertBeanEquals(expected3, filtered.get(2));

    EtdFuturePosition expected4 = EtdFuturePosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123424"))
            .build())
        .security(EtdFutureSecurity.of(contract, YearMonth.of(2017, 6), EtdVariant.ofDaily(3)))
        .longQuantity(30d)
        .shortQuantity(0d)
        .build();
    assertBeanEquals(expected4, filtered.get(3));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_option() {
    EtdContractSpecId specId = EtdContractSpecId.of(OG_ETD_SCHEME, "O-ECAG-OGBL");
    EtdContractSpec contract = EtdContractSpec.builder()
        .id(specId)
        .type(EtdType.OPTION)
        .exchangeId(ExchangeIds.ECAG)
        .contractCode(OGBL)
        .description("Dummy")
        .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
        .build();
    ReferenceData refData = ImmutableReferenceData.of(specId, contract);
    PositionCsvLoader test = PositionCsvLoader.of(refData);
    ValueWithFailures<List<EtdOptionPosition>> trades =
        test.parse(ImmutableList.of(FILE.getCharSource()), EtdOptionPosition.class);

    List<EtdOptionPosition> filtered = trades.getValue();
    assertThat(filtered).hasSize(3);

    EtdOptionPosition expected1 = EtdOptionPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123431"))
            .build())
        .security(EtdOptionSecurity.of(
            contract, YearMonth.of(2017, 6), EtdVariant.ofMonthly(), 0, PutCall.PUT, 3d, YearMonth.of(2017, 9)))
        .longQuantity(15d)
        .shortQuantity(2d)
        .build();
    assertBeanEquals(expected1, filtered.get(0));

    EtdOptionPosition expected2 = EtdOptionPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123432"))
            .build())
        .security(EtdOptionSecurity.of(
            contract,
            YearMonth.of(2017, 6),
            EtdVariant.ofFlexOption(13, EtdSettlementType.CASH, EtdOptionType.AMERICAN),
            0,
            PutCall.CALL,
            4d))
        .longQuantity(0d)
        .shortQuantity(13d)
        .build();
    assertBeanEquals(expected2, filtered.get(1));

    EtdOptionPosition expected3 = EtdOptionPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123433"))
            .build())
        .security(EtdOptionSecurity.of(contract, YearMonth.of(2017, 6), EtdVariant.ofWeekly(2), 0, PutCall.PUT, 5.1d))
        .longQuantity(0d)
        .shortQuantity(20d)
        .build();
    assertBeanEquals(expected3, filtered.get(2));
  }

  //-------------------------------------------------------------------------
  @Test
  @SuppressWarnings("deprecation")
  public void test_parseLightweight() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<SecurityPosition>> trades = test.parseLightweight(ImmutableList.of(FILE.getCharSource()));
    List<SecurityPosition> filtered = trades.getValue();
    assertLightweight(filtered);
  }

  @Test
  public void test_parse_lightweightResolver() {
    PositionCsvLoader test = PositionCsvLoader.of(LightweightPositionCsvInfoResolver.standard());
    ValueWithFailures<List<Position>> trades = test.parse(ImmutableList.of(FILE.getCharSource()));
    List<SecurityPosition> filtered = trades.getValue().stream()
        .map(casting(SecurityPosition.class))
        .collect(toImmutableList());
    assertLightweight(filtered);
  }

  private void assertLightweight(List<SecurityPosition> filtered) {
    assertThat(filtered).hasSize(10);
    assertBeanEquals(SECURITY1, filtered.get(0));
    assertBeanEquals(SECURITY2, filtered.get(1));
    assertBeanEquals(SECURITY3, filtered.get(2));

    SecurityPosition expected3 = SecurityPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123421"))
            .build())
        .securityId(EtdIdUtils.futureId(ExchangeIds.ECAG, FGBL, YearMonth.of(2017, 6), EtdVariant.ofMonthly()))
        .longQuantity(15d)
        .shortQuantity(2d)
        .build();
    assertBeanEquals(expected3, filtered.get(3));

    SecurityPosition expected4 = SecurityPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123422"))
            .build())
        .securityId(EtdIdUtils.futureId(
            ExchangeIds.ECAG, FGBL, YearMonth.of(2017, 6), EtdVariant.ofFlexFuture(13, EtdSettlementType.CASH)))
        .longQuantity(0d)
        .shortQuantity(13d)
        .build();
    assertBeanEquals(expected4, filtered.get(4));

    SecurityPosition expected5 = SecurityPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123423"))
            .build())
        .securityId(EtdIdUtils.futureId(ExchangeIds.ECAG, FGBL, YearMonth.of(2017, 6), EtdVariant.ofWeekly(2)))
        .longQuantity(0d)
        .shortQuantity(20d)
        .build();
    assertBeanEquals(expected5, filtered.get(5));

    SecurityPosition expected6 = SecurityPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123424"))
            .build())
        .securityId(EtdIdUtils.futureId(ExchangeIds.ECAG, FGBL, YearMonth.of(2017, 6), EtdVariant.ofDaily(3)))
        .longQuantity(30d)
        .shortQuantity(0d)
        .build();
    assertBeanEquals(expected6, filtered.get(6));

    SecurityPosition expected7 = SecurityPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123431"))
            .build())
        .securityId(EtdIdUtils.optionId(
            ExchangeIds.ECAG, OGBL, YearMonth.of(2017, 6), EtdVariant.ofMonthly(), 0, PutCall.PUT, 3d, YearMonth.of(2017, 9)))
        .longQuantity(15d)
        .shortQuantity(2d)
        .build();
    assertBeanEquals(expected7, filtered.get(7));

    SecurityPosition expected8 = SecurityPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123432"))
            .build())
        .securityId(EtdIdUtils.optionId(
            ExchangeIds.ECAG,
            OGBL,
            YearMonth.of(2017, 6),
            EtdVariant.ofFlexOption(13, EtdSettlementType.CASH, EtdOptionType.AMERICAN),
            0,
            PutCall.CALL,
            4d))
        .longQuantity(0d)
        .shortQuantity(13d)
        .build();
    assertBeanEquals(expected8, filtered.get(8));

    SecurityPosition expected9 = SecurityPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123433"))
            .build())
        .securityId(EtdIdUtils.optionId(
            ExchangeIds.ECAG, OGBL, YearMonth.of(2017, 6), EtdVariant.ofWeekly(2), 0, PutCall.PUT, 5.1d))
        .longQuantity(0d)
        .shortQuantity(20d)
        .build();
    assertBeanEquals(expected9, filtered.get(9));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_invalidNoHeader() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<Position>> trades = test.parse(ImmutableList.of(CharSource.wrap("")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage()).contains("CSV position file 'Unknown.txt' could not be parsed");
  }

  @Test
  public void test_load_invalidNoType() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    CharSource source = StringCharSource.of("Id").withFileName("Test.csv");
    ValueWithFailures<List<Position>> trades = test.parse(ImmutableList.of(source));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage()).contains("CSV position file 'Test.csv' does not contain 'Strata Position Type' header");
  }

  @Test
  public void test_load_invalidUnknownType() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<Position>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Position Type\nFoo")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage()).isEqualTo("CSV position file 'Unknown.txt' contained unknown position type 'Foo' at line 2");
  }

  @Test
  public void test_load_invalidNoQuantity() {
    EtdContractSpecId specId = EtdContractSpecId.of(OG_ETD_SCHEME, "F-ECAG-FGBL");
    EtdContractSpec contract = EtdContractSpec.builder()
        .id(specId)
        .type(EtdType.FUTURE)
        .exchangeId(ExchangeIds.ECAG)
        .contractCode(FGBL)
        .description("Dummy")
        .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
        .build();
    ReferenceData refData = ImmutableReferenceData.of(specId, contract);
    PositionCsvLoader test = PositionCsvLoader.of(refData);
    ValueWithFailures<List<Position>> trades =
        test.parse(
            ImmutableList.of(CharSource.wrap("Strata Position Type,Exchange,Contract Code,Expiry\nFUT,ECAG,FGBL,2017-06")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage()).isEqualTo("CSV position file 'Unknown.txt' type 'FUT' could not be parsed at line 2: " +
        "Security must contain a quantity column, either 'Quantity' or 'Long Quantity' and 'Short Quantity'");
    assertThat(failure.getAttributes()).containsKey(FailureAttributeKeys.SHORT_MESSAGE);
    assertThat((failure.getAttributes().get(FailureAttributeKeys.SHORT_MESSAGE))).isEqualTo("Security must contain a quantity column, either 'Quantity' or 'Long Quantity' and 'Short Quantity'");
  }

}
