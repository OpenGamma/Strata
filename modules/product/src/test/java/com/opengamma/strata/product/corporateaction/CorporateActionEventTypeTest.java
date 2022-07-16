package com.opengamma.strata.product.corporateaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class CorporateActionEventTypeTest {

  public static Object[][] data_name() {
    return new Object[][] {
        {StandardCorporateActionEventTypes.CASH_DIVIDEND, "Cash Dividend"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(CorporateActionEventType convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(CorporateActionEventType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(CorporateActionEventType convention, String name) {
    assertThat(CorporateActionEventType.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> CorporateActionEventType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> CorporateActionEventType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(CorporateActionEventTypes.class);
  }
}



