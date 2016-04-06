/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Fixed-Inflation swap conventions.
 */
public final class FixedInflationSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<FixedInflationSwapConvention> ENUM_LOOKUP = ExtendedEnum.of(FixedInflationSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * GBP vanilla fixed vs UK HCIP swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_6M_GB_HCIP =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.GBP_FIXED_6M_GB_HCIP.getName());
  
  /**
   * GBP vanilla fixed vs UK RPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_6M_GB_RPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.GBP_FIXED_6M_GB_RPI.getName());
  
  /**
   * GBP vanilla fixed vs UK RPIX swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_6M_GB_RPIX =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.GBP_FIXED_6M_GB_RPIX.getName());

  /**
   * CHF vanilla fixed vs Switzerland CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention CHF_FIXED_6M_CH_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.CHF_FIXED_6M_CH_CPI.getName());
  
  /**
   * Euro vanilla fixed vs Europe CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_6M_EU_AI_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.EUR_FIXED_6M_EU_AI_CPI.getName());
  
  /**
   * Euro vanilla fixed vs Europe (Excluding Tobacco) CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_6M_EU_EXT_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.EUR_FIXED_6M_EU_EXT_CPI.getName());
  
  /**
   * JPY vanilla fixed vs Japan (Excluding Fresh Food) CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention JPY_FIXED_6M_JP_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.JPY_FIXED_6M_JP_CPI.getName());
  
  /**
   * USD(NY) vanilla fixed vs US Urban consumers CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention USD_FIXED_6M_US_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.USD_FIXED_6M_US_CPI.getName());
  
  /**
   * Euro vanilla fixed vs France CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_6M_FR_CPI =
      FixedInflationSwapConvention.of(StandardFixedInflationSwapConventions.EUR_FIXED_6M_FR_CPI.getName());
  
  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FixedInflationSwapConventions() {
  }

}
