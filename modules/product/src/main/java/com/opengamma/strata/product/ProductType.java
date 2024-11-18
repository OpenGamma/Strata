/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.TypedString;
import com.opengamma.strata.collect.named.Described;
import com.opengamma.strata.product.bond.Bill;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.BondFutureOption;
import com.opengamma.strata.product.bond.CapitalIndexedBond;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.capfloor.IborCapFloor;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapFloor;
import com.opengamma.strata.product.cms.Cms;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.dsf.Dsf;
import com.opengamma.strata.product.etd.EtdFutureSecurity;
import com.opengamma.strata.product.etd.EtdOptionSecurity;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fxopt.FxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.index.IborFuture;
import com.opengamma.strata.product.index.IborFutureOption;
import com.opengamma.strata.product.index.OvernightFuture;
import com.opengamma.strata.product.index.OvernightFutureOption;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * The type of a portfolio item.
 * <p>
 * This provides a classification of the trade or position.
 */
public final class ProductType
    extends TypedString<ProductType>
    implements Described {

  /**
   * A {@link BulletPayment}.
   */
  public static final ProductType BULLET_PAYMENT = ProductType.of("BulletPayment", "Payment");
  /**
   * A {@link Bill}.
   */
  public static final ProductType BILL = ProductType.of("Bill");
  /**
   * A {@link FixedCouponBond} or {@link CapitalIndexedBond}.
   */
  public static final ProductType BOND = ProductType.of("Bond");
  /**
   * A {@link BondFuture}.
   */
  public static final ProductType BOND_FUTURE = ProductType.of("BondFuture", "Bond Future");
  /**
   * A {@link BondFutureOption}.
   */
  public static final ProductType BOND_FUTURE_OPTION = ProductType.of("BondFutureOption", "Bond Future Option");
  /**
   * A {@link Cds}.
   */
  public static final ProductType CDS = ProductType.of("Cds", "CDS");
  /**
   * A {@link CdsIndex}.
   */
  public static final ProductType CDS_INDEX = ProductType.of("Cds Index", "CDS Index");
  /**
   * A {@link Cms}.
   */
  public static final ProductType CMS = ProductType.of("Cms", "CMS");
  /**
   * A {@link Dsf}.
   */
  public static final ProductType DSF = ProductType.of("Dsf", "DSF");
  /**
   * A {@link Fra}.
   */
  public static final ProductType FRA = ProductType.of("Fra", "FRA");
  /**
   * A {@link FxNdf}.
   */
  public static final ProductType FX_NDF = ProductType.of("FxNdf", "FX NDF");
  /**
   * A {@link FxSingleBarrierOption}.
   */
  public static final ProductType FX_SINGLE_BARRIER_OPTION = ProductType.of("FxSingleBarrierOption", "FX Single Barrier Option");
  /**
   * A {@link FxSingle}.
   */
  public static final ProductType FX_SINGLE = ProductType.of("FxSingle", "FX");
  /**
   * A {@link FxSwap}.
   */
  public static final ProductType FX_SWAP = ProductType.of("FxSwap", "FX Swap");
  /**
   * A {@link FxVanillaOption}.
   */
  public static final ProductType FX_VANILLA_OPTION = ProductType.of("FxVanillaOption", "FX Vanilla Option");
  /**
   * A {@link IborCapFloor}.
   */
  public static final ProductType IBOR_CAP_FLOOR = ProductType.of("IborCapFloor", "Cap/Floor");
  /**
   * A {@link OvernightInArrearsCapFloor}.
   */
  public static final ProductType OVERNIGHT_IN_ARREARS_CAP_FLOOR = ProductType.of(
      "OvernightInArrearsCapFloor",
      "Overnight In Arrears Cap/Floor");
  /**
   * A {@link IborFuture}.
   */
  public static final ProductType IBOR_FUTURE = ProductType.of("IborFuture", "STIR Future");
  /**
   * A {@link IborFutureOption}.
   */
  public static final ProductType IBOR_FUTURE_OPTION = ProductType.of("IborFutureOption", "STIR Future Option");
  /**
   * A {@link OvernightFuture}.
   */
  public static final ProductType OVERNIGHT_FUTURE = ProductType.of("OvernightFuture", "Overnight Future");
  /**
   * A {@link OvernightFutureOption}.
   */
  public static final ProductType OVERNIGHT_FUTURE_OPTION = ProductType.of("OvernightFutureOption", "Overnight Future Option");
  /**
   * A representation based on sensitivities.
   */
  public static final ProductType SENSITIVITIES = ProductType.of("Sensitivities");
  /**
   * A {@link Swap}.
   */
  public static final ProductType SWAP = ProductType.of("Swap");
  /**
   * A {@link Swaption}.
   */
  public static final ProductType SWAPTION = ProductType.of("Swaption");
  /**
   * A {@link TermDeposit}.
   */
  public static final ProductType TERM_DEPOSIT = ProductType.of("TermDeposit", "Deposit");
  /**
   * An {@link EtdFutureSecurity}.
   */
  public static final ProductType ETD_FUTURE = ProductType.of("EtdFuture", "ETD Future");
  /**
   * A {@link EtdOptionSecurity}.
   */
  public static final ProductType ETD_OPTION = ProductType.of("EtdOption", "ETD Option");
  /**
   * A {@link Security}, used where the kind of security is not known.
   */
  public static final ProductType SECURITY = ProductType.of("Security");
  /**
   * A product only used for calibration.
   */
  public static final ProductType CALIBRATION = ProductType.of("Calibration");
  /**
   * Another kind of product, details not known.
   */
  public static final ProductType OTHER = ProductType.of("Other");

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The description.
   */
  private final String description;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * The name may contain any character, but must not be empty.
   *
   * @param name  the name
   * @return a type instance with the specified name
   */
  @FromString
  public static ProductType of(String name) {
    return new ProductType(name, name);
  }

  /**
   * Obtains an instance from the specified name.
   * <p>
   * The name may contain any character, but must not be empty.
   *
   * @param name  the name
   * @param description  the description
   * @return a type instance with the specified name
   */
  public static ProductType of(String name, String description) {
    return new ProductType(name, description);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   * @param description  the description
   */
  private ProductType(String name, String description) {
    super(name);
    this.description = ArgChecker.notBlank(description, "description");
  }

  /**
   * Gets the human-readable description of the type.
   * 
   * @return the description
   */
  @Override
  public String getDescription() {
    return description;
  }

}
