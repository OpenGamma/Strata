/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.Splitter;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.ExchangeId;

/**
 * An identifier for a group of ETD contracts.
 * <p>
 * This is used to group similar ETD contracts together for the purpose of allocating risk.
 * <p>
 * The identifier is formed of two parts, the exchange and the code defined by the exchange.
 */
public final class EtdContractGroupId implements Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** The string format separator. */
  private static final String SEPARATOR = "::";

  /**
   * The exchange identifier.
   */
  private final ExchangeId exchangeId;
  /**
   * The contract group code, as defined by the exchange.
   */
  private final EtdContractGroupCode code;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the exchange identifier and group code.
   *
   * @param exchangeId  the exchange identifier
   * @param contractGroupCode  the code defined by the exchange
   * @return the identifier
   * @throws IllegalArgumentException if either value is invalid
   */
  public static EtdContractGroupId of(String exchangeId, String contractGroupCode) {
    return of(ExchangeId.of(exchangeId), EtdContractGroupCode.of(contractGroupCode));
  }

  /**
   * Creates an instance from the exchange identifier and group code.
   *
   * @param exchangeId  the exchange identifier
   * @param contractGroupCode  the code defined by the exchange
   * @return the identifier
   */
  public static EtdContractGroupId of(ExchangeId exchangeId, EtdContractGroupCode contractGroupCode) {
    return new EtdContractGroupId(exchangeId, contractGroupCode);
  }

  /**
   * Parses an {@code StandardId} from a formatted scheme and value.
   * <p>
   * This parses the identifier from the form produced by {@code toString()}
   * which is '{@code $exchange::$code}'.
   *
   * @param str  the identifier to parse
   * @return the identifier
   * @throws IllegalArgumentException if the identifier cannot be parsed
   */
  @FromString
  public static EtdContractGroupId parse(String str) {
    List<String> split = Splitter.on(SEPARATOR).splitToList(str);
    if (split.size() != 2) {
      throw new IllegalArgumentException("Invalid contract group format: " + str);
    }
    return EtdContractGroupId.of(split.get(0), split.get(1));
  }

  // creates an identifier
  private EtdContractGroupId(ExchangeId exchangeId, EtdContractGroupCode contractGroupCode) {
    this.exchangeId = ArgChecker.notNull(exchangeId, "exchangeId");
    this.code = ArgChecker.notNull(contractGroupCode, "contractGroupCode");
  }

  // resolve after deserialization
  private Object readResolve() {
    return of(exchangeId, code);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the exchange identifier.
   *
   * @return the exchange identifier
   */
  public ExchangeId getExchangeId() {
    return exchangeId;
  }

  /**
   * Gets the contract group code, as defined by the exchange.
   *
   * @return the group code
   */
  public EtdContractGroupCode getCode() {
    return code;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this identifier equals another identifier.
   * <p>
   * The comparison checks the name.
   *
   * @param obj  the other identifier, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof EtdContractGroupId) {
      EtdContractGroupId other = (EtdContractGroupId) obj;
      return exchangeId.equals(other.exchangeId) && code.equals(other.code);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the identifier.
   *
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return Objects.hash(exchangeId, code);
  }

  /**
   * Returns the identifier in a standard string format.
   * <p>
   * The returned string is in the form '{@code $exchange::$code}'.
   * This is suitable for use with {@link #parse(String)}.
   *
   * @return a parsable representation of the identifier
   */
  @ToString
  @Override
  public String toString() {
    return exchangeId + SEPARATOR + code;
  }

}
