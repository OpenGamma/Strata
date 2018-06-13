/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static org.joda.beans.test.BeanAssert.assertBeanEquals;
import static org.testng.Assert.assertEquals;

import java.time.YearMonth;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.ResourceLocator;
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
@Test
public class PositionCsvLoaderTest {

  private static final EtdContractCode FGBL = EtdContractCode.of("FGBL");
  private static final EtdContractCode OGBL = EtdContractCode.of("OGBL");

  private static final SecurityPosition SECURITY1 = SecurityPosition.builder()
      .info(PositionInfo.builder()
          .id(StandardId.of("OG", "123431"))
          .build())
      .securityId(SecurityId.of("OG-Security", "AAPL"))
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
      .securityId(SecurityId.of("OG-Security", "AAPL"))
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
                  SecurityId.of("OG-Security", "AAPL"),
                  SecurityPriceInfo.of(5, CurrencyAmount.of(USD, 0.01), 10))))
      .longQuantity(12d)
      .shortQuantity(14.5d)
      .build();

  private static final ResourceLocator FILE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/positions.csv");

  //-------------------------------------------------------------------------
  public void test_isKnownFormat() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    assertEquals(test.isKnownFormat(FILE.getCharSource()), true);
  }

  //-------------------------------------------------------------------------
  public void test_load_security() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<Position>> trades = test.load(FILE);

    List<SecurityPosition> filtered = trades.getValue().stream()
        .filter(SecurityPosition.class::isInstance)
        .map(SecurityPosition.class::cast)
        .collect(toImmutableList());
    assertEquals(filtered.size(), 2);

    assertBeanEquals(SECURITY1, filtered.get(0));
    assertBeanEquals(SECURITY2, filtered.get(1));
  }

  public void test_load_genericSecurity() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<Position>> trades = test.load(FILE);

    List<GenericSecurityPosition> filtered = trades.getValue().stream()
        .filter(GenericSecurityPosition.class::isInstance)
        .map(GenericSecurityPosition.class::cast)
        .collect(toImmutableList());
    assertEquals(filtered.size(), 1);

    assertBeanEquals(SECURITY3FULL, filtered.get(0));
  }

  public void test_parseFiltering() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    assertEquals(test.parse(ImmutableList.of(FILE.getCharSource())).getValue().size(), 3);  // 7 errors
    assertEquals(test.parse(ImmutableList.of(FILE.getCharSource()), SecurityPosition.class).getValue().size(), 10);
    assertEquals(test.parse(ImmutableList.of(FILE.getCharSource()), ResolvableSecurityPosition.class).getValue().size(), 3);
    assertEquals(test.parse(ImmutableList.of(FILE.getCharSource()), GenericSecurityPosition.class).getValue().size(), 1);
  }

  //-------------------------------------------------------------------------
  public void test_parse_future() {
    EtdContractSpecId specId = EtdContractSpecId.of("OG-ETD", "F-ECAG-FGBL");
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
    assertEquals(filtered.size(), 4);

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
  public void test_parse_option() {
    EtdContractSpecId specId = EtdContractSpecId.of("OG-ETD", "O-ECAG-OGBL");
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
    assertEquals(filtered.size(), 3);

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
  public void test_parseLightweight() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<SecurityPosition>> trades = test.parseLightweight(ImmutableList.of(FILE.getCharSource()));
    List<SecurityPosition> filtered = trades.getValue();
    assertEquals(filtered.size(), 10);

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
  public void test_load_invalidNoHeader() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<Position>> trades = test.parse(ImmutableList.of(CharSource.wrap("")));

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage().contains("CSV file could not be parsed"), true);
  }

  public void test_load_invalidNoType() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<Position>> trades = test.parse(ImmutableList.of(CharSource.wrap("Id")));

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage().contains("CSV file does not contain 'Strata Position Type' header"), true);
  }

  public void test_load_invalidUnknownType() {
    PositionCsvLoader test = PositionCsvLoader.standard();
    ValueWithFailures<List<Position>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Position Type\nFoo")));

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage(), "CSV file position type 'Foo' is not known at line 2");
  }

  public void test_load_invalidNoQuantity() {
    EtdContractSpecId specId = EtdContractSpecId.of("OG-ETD", "F-ECAG-FGBL");
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

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage(),
        "CSV file position could not be parsed at line 2: " +
            "Security must contain a quantity column, either 'Quantity' or 'Long Quantity' and 'Short Quantity'");
  }

}
