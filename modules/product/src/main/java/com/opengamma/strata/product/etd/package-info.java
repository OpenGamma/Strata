/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Entity objects describing Exchange Traded Derivatives (ETDs).
 * <p>
 * This package models exchange-traded futures and options.
 * Each ETD - {@link com.opengamma.strata.product.etd.EtdFutureSecurity EtdFutureSecurity} and
 * {@link com.opengamma.strata.product.etd.EtdOptionSecurity EtdOptionSecurity} - runs for a
 * specific period of time and has a known expiry date.
 * The individual future/option is one of a series, based on a
 * {@link com.opengamma.strata.product.etd.EtdContractSpec EtdContractSpec}.
 * The specification effectively acts as a factory for futures/options.
 * Different contract specifications can be grouped together for risk purposes using
 * {@link com.opengamma.strata.product.etd.EtdContractGroupId EtdContractGroupId}.
 * <p>
 * Strata provides a standard approach to ETD identifiers in
 * {@link com.opengamma.strata.product.etd.EtdIdUtils EtdIdUtils}.
 */
package com.opengamma.strata.product.etd;
