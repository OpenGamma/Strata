/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.CorporateActionConvention;
import com.opengamma.strata.product.SecurityId;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import java.time.LocalDate;

/**
 * A market convention for swap trades.
 * <p>
 * This defines the market convention for a a swap.
 * Each different type of swap has its own convention - this interface provides an abstraction.
 */
public interface SingleCashPaymentConvention
    extends CorporateActionConvention, Named {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static SingleCashPaymentConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  public static ExtendedEnum<SingleCashPaymentConvention> extendedEnum() {
    return SingleCashPaymentConventions.ENUM_LOOKUP;
  }

  public default AnnouncementCorporateAction toAnnouncementCorporateAction( //Kanske Announcement
      String corpRefProvider,
      String corpRefOfficial,
      String eventType,
      SecurityId neededSecurityId,
      double quantityNeeded,
      LocalDate paymentDate,
      CurrencyAmount payment){

    CorporateActionInfo info = CorporateActionInfo.builder()
        .corpRefProvider(StandardId.of("OG-Test", corpRefProvider)) //DPDPDP
        .corpRefOfficial(StandardId.of("OG-Test", corpRefOfficial))
        .eventType(CorporateActionEventType.of(eventType))
        .build();

    return toAnnouncementCorporateAction(info, neededSecurityId,  quantityNeeded, paymentDate, payment);
  }

  public AnnouncementCorporateAction toAnnouncementCorporateAction( //Kanske Announcement
      CorporateActionInfo info,
      SecurityId neededSecurityId,
      double quantityNeeded,
      LocalDate paymentDate,
      CurrencyAmount payment);

  //-------------------------------------------------------------------------

  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   *
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();
}
