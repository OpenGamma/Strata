/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import org.joda.convert.RenameHandler;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.product.common.CcpId;
import com.opengamma.strata.product.common.CcpIds;
import com.opengamma.strata.product.etd.EtdContractCode;

/**
 * Test {@link AttributeType}.
 */
public class AttributeTypeTest {

  @Test
  public void test_constant_description() {
    AttributeType<String> test = AttributeType.DESCRIPTION;
    assertThat(AttributeType.of("description")).isSameAs(test);
    assertThat(test.toString()).isEqualTo("description");
    assertThat(test.getName()).isEqualTo("description");
    assertThat(test.normalized()).isSameAs(test);
    assertThat(test.toStoredForm("xxx")).isEqualTo("xxx");
    assertThat(test.fromStoredForm("xxx")).isEqualTo("xxx");
    assertThat(test.toStoredForm(null)).isNull();
    assertThat(test.fromStoredForm(null)).isNull();
    assertThat(test.captureWildcard()).isSameAs(test);
  }

  @Test
  public void test_constant_name() {
    AttributeType<String> test = AttributeType.NAME;
    assertThat(AttributeType.of("name")).isSameAs(test);
    assertThat(test.getName()).isEqualTo("name");
  }

  @Test
  public void test_constant_ccp() {
    AttributeType<CcpId> test = AttributeType.CCP;
    assertThat(AttributeType.of("ccp")).isSameAs(test);
    assertThat(test.toString()).isEqualTo("ccp");
    assertThat(test.getName()).isEqualTo("ccp");
    assertThat(test.normalized()).isSameAs(test);
    assertThat(test.toStoredForm(CcpId.of("OGX"))).isEqualTo("OGX");
    assertThat(test.fromStoredForm("OGX")).isEqualTo(CcpId.of("OGX"));
    assertThat(test.fromStoredForm(CcpId.of("OGX"))).isEqualTo(CcpId.of("OGX"));
  }

  @Test
  public void test_of_notRegistered_convert() {
    AttributeType<LegalEntityId> test = AttributeType.of("testingLegalEntityId");
    assertThat(AttributeType.of("testingLegalEntityId")).isEqualTo(test);
    assertThat(test.getName()).isEqualTo("testingLegalEntityId");
    assertThat(test.toString()).isEqualTo("testingLegalEntityId");
    assertThat(test.normalized()).isSameAs(test);
    assertThat(test.toStoredForm(LegalEntityId.of("OG", "A"))).isEqualTo(LegalEntityId.of("OG", "A"));
    assertThat(test.fromStoredForm(LegalEntityId.of("OG", "A"))).isEqualTo(LegalEntityId.of("OG", "A"));
    // now register it, simulating later load of constants in another class
    AttributeType<LegalEntityId> regType =
        AttributeType.registerInstance("testingLegalEntityId2", LegalEntityId.class, "testingLegalEntityId");
    assertThat(AttributeType.of("testingLegalEntityId2")).isSameAs(regType);
    assertThat(AttributeType.of("testingLegalEntityId")).isSameAs(regType);
    assertThat(test.normalized()).isSameAs(regType);
    assertThat(test.toStoredForm(LegalEntityId.of("OG", "A"))).isEqualTo("OG~A");
    assertThat(test.fromStoredForm("OG~A")).isEqualTo(LegalEntityId.of("OG", "A"));
    assertThat(test.fromStoredForm(LegalEntityId.of("OG", "A"))).isEqualTo(LegalEntityId.of("OG", "A"));
    assertThat(regType.normalized()).isSameAs(regType);
    assertThat(regType.toStoredForm(LegalEntityId.of("OG", "A"))).isEqualTo("OG~A");
    assertThat(regType.fromStoredForm("OG~A")).isEqualTo(LegalEntityId.of("OG", "A"));
    assertThat(regType.fromStoredForm(LegalEntityId.of("OG", "A"))).isEqualTo(LegalEntityId.of("OG", "A"));
  }

  @Test
  public void test_of_notRegistered_bean() {
    TradeInfo info = TradeInfo.of(date(2019, 6, 30));

    AttributeType<TradeInfo> test = AttributeType.of("testingTradeInfo");
    assertThat(test.getName()).isEqualTo("testingTradeInfo");
    assertThat(test.toString()).isEqualTo("testingTradeInfo");
    assertThat(test.normalized()).isSameAs(test);
    assertThat(test.toStoredForm(info)).isEqualTo(info);
    assertThat(test.fromStoredForm(info)).isEqualTo(info);
    // now register it, simulating later load of constants in another class
    AttributeType<TradeInfo> regType = AttributeType.registerInstance("testingTradeInfo", TradeInfo.class);
    assertThat(test.normalized()).isSameAs(regType);
    assertThat(test.toStoredForm(info)).isEqualTo(info);
    assertThat(test.fromStoredForm(info)).isEqualTo(info);
    assertThat(regType.normalized()).isSameAs(regType);
    assertThat(regType.toStoredForm(info)).isEqualTo(info);
    assertThat(regType.fromStoredForm(info)).isEqualTo(info);
  }

  @Test
  public void test_of_changingType() {
    AttributeType<CcpId> test = AttributeType.CCP;
    assertThat(test.fromStoredForm(CcpIds.CME)).isEqualTo(CcpIds.CME);
    assertThat(test.fromStoredForm("CME")).isEqualTo(CcpIds.CME);
    assertThat(test.fromStoredForm(EtdContractCode.of("CME"))).isEqualTo(CcpIds.CME);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    AttributeType<String> a = AttributeType.of("test");
    AttributeType<String> a2 = AttributeType.of("test");
    AttributeType<String> b = AttributeType.of("test2");
    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(a2)
        .isLessThan(b);
  }

  @Test
  public void test_jodaConvert() throws Exception {
    assertThat(RenameHandler.INSTANCE.lookupType("com.opengamma.strata.product.PositionAttributeType"))
        .isEqualTo(AttributeType.class);
    assertThat(RenameHandler.INSTANCE.lookupType("com.opengamma.strata.product.SecurityAttributeType"))
        .isEqualTo(AttributeType.class);
    assertThat(RenameHandler.INSTANCE.lookupType("com.opengamma.strata.product.TradeAttributeType"))
        .isEqualTo(AttributeType.class);
  }

}
