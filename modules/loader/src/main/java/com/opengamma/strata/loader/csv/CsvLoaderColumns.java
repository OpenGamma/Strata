/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

/**
 * Column names for CSV files.
 * <p>
 * This class provides constants for the column names in the Strata CSV format.
 */
public final class CsvLoaderColumns {

  /** CSV header (Basic). */
  public static final String TRADE_TYPE_FIELD = "Strata Trade Type";
  /** CSV header (Basic). */
  public static final String POSITION_TYPE_FIELD = "Strata Position Type";
  /** CSV header (Basic). */
  public static final String ID_SCHEME_FIELD = "Id Scheme";
  /** CSV header (Basic). */
  public static final String ID_FIELD = "Id";
  /** CSV header (Basic). */
  public static final String DESCRIPTION_FIELD = "Description";
  /** CSV header (Basic). */
  public static final String NAME_FIELD = "Name";
  /** CSV header (Basic). */
  public static final String CCP_FIELD = "CCP";
  /** CSV header (Basic). */
  public static final String CPTY_SCHEME_FIELD = "Counterparty Scheme";
  /** CSV header (Basic). */
  public static final String CPTY_FIELD = "Counterparty";
  /** CSV header (Basic). */
  public static final String TRADE_DATE_FIELD = "Trade Date";
  /** CSV header (Basic). */
  public static final String TRADE_TIME_FIELD = "Trade Time";
  /** CSV header (Basic). */
  public static final String TRADE_ZONE_FIELD = "Trade Zone";
  /** CSV header (Basic). */
  public static final String SETTLEMENT_DATE_FIELD = "Settlement Date";

  /** CSV header. */
  public static final String CONVENTION_FIELD = "Convention";
  /** CSV header. */
  public static final String BUY_SELL_FIELD = "Buy Sell";
  /** CSV header. */
  public static final String DIRECTION_FIELD = "Direction";
  /** CSV header. */
  public static final String CURRENCY_FIELD = "Currency";
  /** CSV header. */
  public static final String NOTIONAL_FIELD = "Notional";
  /** CSV header. */
  public static final String INDEX_FIELD = "Index";
  /** CSV header. */
  public static final String INTERPOLATED_INDEX_FIELD = "Interpolated Index";
  /** CSV header. */
  public static final String FIXED_RATE_FIELD = "Fixed Rate";
  /** CSV header. */
  public static final String PERIOD_TO_START_FIELD = "Period To Start";
  /** CSV header. */
  public static final String TENOR_FIELD = "Tenor";
  /** CSV header. */
  public static final String START_DATE_FIELD = "Start Date";
  /** CSV header. */
  public static final String END_DATE_FIELD = "End Date";
  /** CSV header. */
  public static final String DATE_ADJ_CNV_FIELD = "Date Convention";
  /** CSV header. */
  public static final String DATE_ADJ_CAL_FIELD = "Date Calendar";
  /** CSV header. */
  public static final String DAY_COUNT_FIELD = "Day Count";
  /** CSV header. */
  public static final String FX_RATE_FIELD = "FX Rate";
  /** CSV header. */
  public static final String PAYMENT_DATE_FIELD = "Payment Date";
  /** CSV header. */
  public static final String PAYMENT_DATE_CNV_FIELD = "Payment Date Convention";
  /** CSV header. */
  public static final String PAYMENT_DATE_CAL_FIELD = "Payment Date Calendar";
  /** CSV header. */
  public static final String LONG_SHORT_FIELD = "Long Short";
  /** CSV header. */
  public static final String EXPIRY_DATE_FIELD = "Expiry Date";
  /** CSV header. */
  public static final String EXPIRY_DATE_CNV_FIELD = "Expiry Date Convention";
  /** CSV header. */
  public static final String EXPIRY_DATE_CAL_FIELD = "Expiry Date Calendar";
  /** CSV header. */
  public static final String EXPIRY_TIME_FIELD = "Expiry Time";
  /** CSV header. */
  public static final String EXPIRY_ZONE_FIELD = "Expiry Zone";
  /** CSV header. */
  public static final String PREMIUM_CURRENCY_FIELD = "Premium Currency";
  /** CSV header. */
  public static final String PREMIUM_AMOUNT_FIELD = "Premium Amount";
  /** CSV header. */
  public static final String PREMIUM_DIRECTION_FIELD = "Premium Direction";
  /** CSV header. */
  public static final String PREMIUM_DATE_FIELD = "Premium Date";
  /** CSV header. */
  public static final String PREMIUM_DATE_CNV_FIELD = "Premium Date Convention";
  /** CSV header. */
  public static final String PREMIUM_DATE_CAL_FIELD = "Premium Date Calendar";
  /** CSV header. */
  public static final String FRA_DISCOUNTING_FIELD = "FRA Discounting Method";
  /** CSV header. */
  public static final String PUT_CALL_FIELD = "Put Call";

  /** CSV header. */
  public static final String FREQUENCY_FIELD = "Frequency";
  /** CSV header. */
  public static final String START_DATE_CNV_FIELD = "Start Date Convention";
  /** CSV header. */
  public static final String START_DATE_CAL_FIELD = "Start Date Calendar";
  /** CSV header. */
  public static final String END_DATE_CNV_FIELD = "End Date Convention";
  /** CSV header. */
  public static final String END_DATE_CAL_FIELD = "End Date Calendar";
  /** CSV header. */
  public static final String ROLL_CONVENTION_FIELD = "Roll Convention";
  /** CSV header. */
  public static final String STUB_CONVENTION_FIELD = "Stub Convention";
  /** CSV header. */
  public static final String FIRST_REGULAR_START_DATE_FIELD = "First Regular Start Date";
  /** CSV header. */
  public static final String LAST_REGULAR_END_DATE_FIELD = "Last Regular End Date";
  /** CSV header. */
  public static final String OVERRIDE_START_DATE_FIELD = "Override Start Date";
  /** CSV header. */
  public static final String OVERRIDE_START_DATE_CNV_FIELD = "Override Start Date Convention";
  /** CSV header. */
  public static final String OVERRIDE_START_DATE_CAL_FIELD = "Override Start Date Calendar";

  /** CSV header (CDS). */
  public static final String CDS_INDEX_ID_SCHEME_FIELD = "CDS Index Id Scheme";
  /** CSV header (CDS). */
  public static final String CDS_INDEX_ID_FIELD = "CDS Index Id";
  /** CSV header (CDS). */
  public static final String LEGAL_ENTITY_ID_SCHEME_FIELD = "Legal Entity Id Scheme";
  /** CSV header (CDS). */
  public static final String LEGAL_ENTITY_ID_FIELD = "Legal Entity Id";
  /** CSV header (CDS). */
  public static final String PAYMENT_ON_DEFAULT_FIELD = "Payment On Default";
  /** CSV header (CDS). */
  public static final String PROTECTION_START_FIELD = "Protection Start";
  /** CSV header (CDS). */
  public static final String STEP_IN_DATE_OFFSET_DAYS_FIELD = "Step In Date Offset Days";
  /** CSV header (CDS). */
  public static final String STEP_IN_DATE_OFFSET_CAL_FIELD = "Step In Date Offset Calendar";
  /** CSV header (CDS). */
  public static final String STEP_IN_DATE_OFFSET_ADJ_CNV_FIELD = "Step In Date Offset Adjustment Convention";
  /** CSV header (CDS). */
  public static final String STEP_IN_DATE_OFFSET_ADJ_CAL_FIELD = "Step In Date Offset Adjustment Calendar";
  /** CSV header (CDS). */
  public static final String SETTLEMENT_DATE_OFFSET_DAYS_FIELD = "Settlement Date Offset Days";
  /** CSV header (CDS). */
  public static final String SETTLEMENT_DATE_OFFSET_CAL_FIELD = "Settlement Date Offset Calendar";
  /** CSV header (CDS). */
  public static final String SETTLEMENT_DATE_OFFSET_ADJ_CNV_FIELD = "Settlement Date Offset Adjustment Convention";
  /** CSV header (CDS). */
  public static final String SETTLEMENT_DATE_OFFSET_ADJ_CAL_FIELD = "Settlement Date Offset Adjustment Calendar";

  /** CSV header (Swap). */
  public static final String PAYMENT_FREQUENCY_FIELD = "Payment Frequency";
  /** CSV header (Swap). */
  public static final String PAYMENT_RELATIVE_TO_FIELD = "Payment Relative To";
  /** CSV header (Swap). */
  public static final String PAYMENT_OFFSET_DAYS_FIELD = "Payment Offset Days";
  /** CSV header (Swap). */
  public static final String PAYMENT_OFFSET_CAL_FIELD = "Payment Offset Calendar";
  /** CSV header (Swap). */
  public static final String PAYMENT_OFFSET_ADJ_CNV_FIELD = "Payment Offset Adjustment Convention";
  /** CSV header (Swap). */
  public static final String PAYMENT_OFFSET_ADJ_CAL_FIELD = "Payment Offset Adjustment Calendar";
  /** CSV header (Swap). */
  public static final String COMPOUNDING_METHOD_FIELD = "Compounding Method";
  /** CSV header (Swap). */
  public static final String PAYMENT_FIRST_REGULAR_START_DATE_FIELD = "Payment First Regular Start Date";
  /** CSV header (Swap). */
  public static final String PAYMENT_LAST_REGULAR_END_DATE_FIELD = "Payment Last Regular End Date";

  /** CSV header (Swap). */
  public static final String NOTIONAL_CURRENCY_FIELD = "Notional Currency";
  /** CSV header (Swap). */
  public static final String NOTIONAL_INITIAL_EXCHANGE_FIELD = "Notional Initial Exchange";
  /** CSV header (Swap). */
  public static final String NOTIONAL_INTERMEDIATE_EXCHANGE_FIELD = "Notional Intermediate Exchange";
  /** CSV header (Swap). */
  public static final String NOTIONAL_FINAL_EXCHANGE_FIELD = "Notional Final Exchange";
  /** CSV header (Swap). */
  public static final String FX_RESET_INDEX_FIELD = "FX Reset Index";
  /** CSV header (Swap). */
  public static final String FX_RESET_RELATIVE_TO_FIELD = "FX Reset Relative To";
  /** CSV header (Swap). */
  public static final String FX_RESET_OFFSET_DAYS_FIELD = "FX Reset Offset Days";
  /** CSV header (Swap). */
  public static final String FX_RESET_OFFSET_CAL_FIELD = "FX Reset Offset Calendar";
  /** CSV header (Swap). */
  public static final String FX_RESET_OFFSET_ADJ_CNV_FIELD = "FX Reset Offset Adjustment Convention";
  /** CSV header (Swap). */
  public static final String FX_RESET_OFFSET_ADJ_CAL_FIELD = "FX Reset Offset Adjustment Calendar";
  /** CSV header (Swap). */
  public static final String FX_RESET_INITIAL_NOTIONAL_FIELD = "FX Reset Initial Notional";

  /** CSV header (Swap). */
  public static final String INITIAL_STUB_RATE_FIELD = "Initial Stub Rate";
  /** CSV header (Swap). */
  public static final String INITIAL_STUB_AMOUNT_FIELD = "Initial Stub Amount";
  /** CSV header (Swap). */
  public static final String INITIAL_STUB_AMOUNT_CURRENCY_FIELD = "Initial Stub Amount Currency";
  /** CSV header (Swap). */
  public static final String INITIAL_STUB_INDEX_FIELD = "Initial Stub Index";
  /** CSV header (Swap). */
  public static final String INITIAL_STUB_INTERPOLATED_INDEX_FIELD = "Initial Stub Interpolated Index";
  /** CSV header (Swap). */
  public static final String FINAL_STUB_RATE_FIELD = "Final Stub Rate";
  /** CSV header (Swap). */
  public static final String FINAL_STUB_AMOUNT_FIELD = "Final Stub Amount";
  /** CSV header (Swap). */
  public static final String FINAL_STUB_AMOUNT_CURRENCY_FIELD = "Final Stub Amount Currency";
  /** CSV header (Swap). */
  public static final String FINAL_STUB_INDEX_FIELD = "Final Stub Index";
  /** CSV header (Swap). */
  public static final String FINAL_STUB_INTERPOLATED_INDEX_FIELD = "Final Stub Interpolated Index";
  /** CSV header (Swap). */
  public static final String RESET_FREQUENCY_FIELD = "Reset Frequency";
  /** CSV header (Swap). */
  public static final String RESET_DATE_CNV_FIELD = "Reset Date Convention";
  /** CSV header (Swap). */
  public static final String RESET_DATE_CAL_FIELD = "Reset Date Calendar";
  /** CSV header (Swap). */
  public static final String RESET_METHOD_FIELD = "Reset Method";
  /** CSV header (Swap). */
  public static final String FIXING_RELATIVE_TO_FIELD = "Fixing Relative To";
  /** CSV header (Swap). */
  public static final String FIXING_OFFSET_DAYS_FIELD = "Fixing Offset Days";
  /** CSV header (Swap). */
  public static final String FIXING_OFFSET_CAL_FIELD = "Fixing Offset Calendar";
  /** CSV header (Swap). */
  public static final String FIXING_OFFSET_ADJ_CNV_FIELD = "Fixing Offset Adjustment Convention";
  /** CSV header (Swap). */
  public static final String FIXING_OFFSET_ADJ_CAL_FIELD = "Fixing Offset Adjustment Calendar";
  /** CSV header (Swap). */
  public static final String FUTURE_VALUE_NOTIONAL_FIELD = "Future Value Notional";
  /** CSV header (Swap). */
  public static final String NEGATIVE_RATE_METHOD_FIELD = "Negative Rate Method";
  /** CSV header (Swap). */
  public static final String FIRST_RATE_FIELD = "First Rate";
  /** CSV header (Swap). */
  public static final String FIRST_REGULAR_RATE_FIELD = "First Regular Rate";
  /** CSV header (Swap). */
  public static final String ACCRUAL_METHOD_FIELD = "Accrual Method";
  /** CSV header (Swap). */
  public static final String RATE_CUT_OFF_DAYS_FIELD = "Rate Cut Off Days";
  /** CSV header (Swap). */
  public static final String INFLATION_LAG_FIELD = "Inflation Lag";
  /** CSV header (Swap). */
  public static final String INFLATION_METHOD_FIELD = "Inflation Method";
  /** CSV header (Swap). */
  public static final String INFLATION_FIRST_INDEX_VALUE_FIELD = "Inflation First Index Value";
  /** CSV header (Swap). */
  public static final String GEARING_FIELD = "Gearing";
  /** CSV header (Swap). */
  public static final String SPREAD_FIELD = "Spread";
  /** CSV header (Swap). */
  public static final String KNOWN_AMOUNT_FIELD = "Known Amount";

  /** CSV header (Swaption). */
  public static final String PAYOFF_SETTLEMENT_TYPE_FIELD = "Payoff Settlement Type";
  /** CSV header (Swaption). */
  public static final String PAYOFF_SETTLEMENT_DATE_FIELD = "Payoff Settlement Date";

  /** CSV header (FX). */
  public static final String FAR_FX_RATE_DATE_FIELD = "Far FX Rate";
  /** CSV header (FX). */
  public static final String FAR_PAYMENT_DATE_FIELD = "Far Payment Date";

  /** CSV header (Position/Security). */
  public static final String SECURITY_ID_SCHEME_FIELD = "Security Id Scheme";
  /** CSV header (Position/Security). */
  public static final String SECURITY_ID_FIELD = "Security Id";
  /** CSV header (Position/Security). */
  public static final String EXCHANGE_FIELD = "Exchange";
  /** CSV header (Position/Security). */
  public static final String CONTRACT_CODE_FIELD = "Contract Code";
  /** CSV header (Position/Security). */
  public static final String LONG_QUANTITY_FIELD = "Long Quantity";
  /** CSV header (Position/Security). */
  public static final String SHORT_QUANTITY_FIELD = "Short Quantity";
  /** CSV header (Position/Security). */
  public static final String QUANTITY_FIELD = "Quantity";
  /** CSV header (Position/Security). */
  public static final String PRICE_FIELD = "Price";
  /** CSV header (Position/Security). */
  public static final String EXPIRY_FIELD = "Expiry";
  /** CSV header (Position/Security). */
  public static final String EXPIRY_WEEK_FIELD = "Expiry Week";
  /** CSV header (Position/Security). */
  public static final String EXPIRY_DAY_FIELD = "Expiry Day";
  /** CSV header (Position/Security). */
  public static final String SETTLEMENT_TYPE_FIELD = "Settlement Type";
  /** CSV header (Position/Security). */
  public static final String EXERCISE_STYLE_FIELD = "Exercise Style";
  /** CSV header (Position/Security). */
  public static final String VERSION_FIELD = "Version";
  /** CSV header (Position/Security). */
  public static final String EXERCISE_PRICE_FIELD = "Exercise Price";
  /** CSV header (Position/Security). */
  public static final String UNDERLYING_EXPIRY_FIELD = "Underlying Expiry";
  /** CSV header (Position/Security). */
  public static final String TICK_SIZE_FIELD = "Tick Size";
  /** CSV header (Position/Security). */
  public static final String TICK_VALUE_FIELD = "Tick Value";
  /** CSV header (Position/Security). */
  public static final String CONTRACT_SIZE_FIELD = "Contract Size";

  // restricted constructor
  private CsvLoaderColumns() {
  }

}
