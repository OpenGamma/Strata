package com.opengamma.strata.product.corporateaction.cashdividend;

/**
 * Test {@link CashDividendPosition}.
 */
public class CashDividendPositionTest {

 /* private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final PositionInfo POSITION_INFO1 = PositionInfo.builder()
      .id(StandardId.of("A", "B"))
      .build();
  private static final PositionInfo POSITION_INFO2 = PositionInfo.builder()
      .id(StandardId.of("A", "C"))
      .build();
  private static final double QUANTITY1 = 10;

  private static final CashDividend PRODUCT1 = CashDividendTest.CASH_DIVIDEND;


  @Test
  public void test_builder_of() {
    CashDividendPosition test = CashDividendPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .quantity(QUANTITY1)
        .build();
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getId()).isEqualTo(POSITION_INFO1.getId());
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO1);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY1);
    assertThat(test.getProduct()).isEqualTo(PRODUCT1);
    assertThat(test.getSecurityId()).isEqualTo(PRODUCT1.getSecurityId());
    CashDividendPosition test1 = CashDividendPosition.of(POSITION_INFO1, PRODUCT1, QUANTITY1);
    assertThat(test).isEqualTo(test1);
  }

 /* @Test
  public void test_summarize() {
    CashDividendPosition base = CashDividendPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .quantity(QUANTITY1)
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO1.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.OTHER)
        .currencies(GBP)
        .description("APPLE x 10")
        .build();
    assertThat(base.summarize()).isEqualTo(expected);
  } */

  }
