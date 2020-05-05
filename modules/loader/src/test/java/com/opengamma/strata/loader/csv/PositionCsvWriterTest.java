/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.common.ExchangeIds;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.etd.EtdContractCode;
import com.opengamma.strata.product.etd.EtdContractSpec;
import com.opengamma.strata.product.etd.EtdContractSpecId;
import com.opengamma.strata.product.etd.EtdFuturePosition;
import com.opengamma.strata.product.etd.EtdFutureSecurity;
import com.opengamma.strata.product.etd.EtdOptionPosition;
import com.opengamma.strata.product.etd.EtdOptionSecurity;
import com.opengamma.strata.product.etd.EtdType;
import com.opengamma.strata.product.etd.EtdVariant;

/**
 * Test {@link PositionCsvWriter}.
 */
class PositionCsvWriterTest {

  private static final ResourceLocator FILE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/positions-2.csv");

  private static EtdContractSpecId futureSpecId;
  private static EtdContractSpecId optionSpecId;
  private static EtdContractSpec futureContract;
  private static EtdContractSpec optionContract;

  @BeforeAll
  static void beforeAll() {
    futureSpecId = EtdContractSpecId.of("OG-ETD", "F-ECAG-FGBL");

    optionSpecId = EtdContractSpecId.of("OG-ETD", "O-ECAG-OGBL");

    futureContract = EtdContractSpec.builder()
        .id(futureSpecId)
        .type(EtdType.FUTURE)
        .exchangeId(ExchangeIds.ECAG)
        .contractCode(EtdContractCode.of("FGBL"))
        .description("Dummy")
        .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
        .build();

    optionContract = EtdContractSpec.builder()
        .id(optionSpecId)
        .type(EtdType.OPTION)
        .exchangeId(ExchangeIds.ECAG)
        .contractCode(EtdContractCode.of("OGBL"))
        .description("Dummy")
        .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
        .build();
  }

  @Test
  void test_write_etdFuturePosition() {
    EtdFuturePosition position = EtdFuturePosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123424"))
            .build())
        .security(EtdFutureSecurity.of(
            futureContract,
            YearMonth.of(2017, 6),
            EtdVariant.ofDaily(3)))
        .longQuantity(30d)
        .shortQuantity(0d)
        .build();

    StringBuffer buf = new StringBuffer();
    PositionCsvWriter.standard().write(ImmutableList.of(position), buf);
    String content = buf.toString();

    String expected = "Strata Position Type,Id Scheme,Id,Contract Code,Exchange,Expiry,Long Quantity,Short Quantity,Expiry Day\n" +
        "FUT,OG,123424,FGBL,ECAG,2017-06,30,0,3\n";
    assertThat(content).isEqualTo(expected);
  }

  @Test
  void test_write_etdOptionPosition() {
    EtdOptionPosition position = EtdOptionPosition.builder()
        .info(PositionInfo.builder()
            .id(StandardId.of("OG", "123431"))
            .build())
        .security(EtdOptionSecurity.of(
            optionContract,
            YearMonth.of(2017, 6),
            EtdVariant.ofMonthly(), 0,
            PutCall.PUT, 3d,
            YearMonth.of(2017, 9)))
        .longQuantity(15d)
        .shortQuantity(2d)
        .build();

    StringBuffer buf = new StringBuffer();
    PositionCsvWriter.standard().write(ImmutableList.of(position), buf);
    String content = buf.toString();

    String expected = "Strata Position Type,Id Scheme,Id,Contract Code,Exchange,Exercise Price,Expiry,Long Quantity,Put Call,Short Quantity,Underlying Expiry\n"
        + "OPT,OG,123431,OGBL,ECAG,3,2017-06,15,Put,2,2017-09\n";
    assertThat(content).isEqualTo(expected);
  }

  @Test
  void test_write_roundTrip() {
    ReferenceData referenceData = ImmutableReferenceData.of(ImmutableMap.of(
        futureSpecId, futureContract,
        optionSpecId, optionContract));
    PositionCsvLoader loader = PositionCsvLoader.of(referenceData);

    ValueWithFailures<List<Position>> parsed1 = loader.parse(ImmutableList.of(FILE.getCharSource()));
    assertThat(parsed1.getFailures()).isEmpty();
    List<Position> positionsList1 = parsed1.getValue();
    assertThat(positionsList1).hasSize(2);

    StringBuffer buf = new StringBuffer();
    PositionCsvWriter.standard().write(positionsList1, buf);
    String content = buf.toString();

    ValueWithFailures<List<Position>> parsed2 = loader.parse(ImmutableList.of(CharSource.wrap(content)));
    assertThat(parsed2.getFailures()).isEmpty();
    List<Position> positionList2 = parsed2.getValue();
    assertThat(positionList2).hasSize(2);

    assertThat(positionList2).isEqualTo(positionsList1);
  }

}
