/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

/**
 * Common attribute keys for {@linkplain FailureItem#getAttributes()}.
 * <p>
 * The attribute keys are used to help interpret the failure message.
 */
public final class FailureAttributeKeys {

  /**
   * The attribute for specifying file id.
   */
  public static final String FILE_ID = "fileId";
  /**
   * The attribute for specifying the file name in which the error occurred.
   */
  public static final String FILE_NAME = "fileName";
  /**
   * The attribute for specifying the file format in which the error occurred.
   */
  public static final String FORMAT = "format";
  /**
   * The attribute for specifying the line number in which the error occurred.
   */
  public static final String LINE_NUMBER = "lineNumber";
  /**
   * The attribute for the value that caused the failure.
   */
  public static final String VALUE = "value";
  /**
   * The attribute for the options that were valid.
   */
  public static final String OPTIONS = "options";
  /**
   * The attribute for specifying the message from a runtime exception.
   */
  public static final String EXCEPTION_MESSAGE = "exceptionMessage";
  /**
   * The attribute for decoding the message to extract the template locations.
   */
  public static final String TEMPLATE_LOCATION = "templateLocation";

  /**
   * The attribute for specifying the LEI associated with the error.
   */
  public static final String LEGAL_ENTITY_ID = "legalEntityId";
  /**
   * The attribute for specifying the account associated with the error.
   */
  public static final String ACCOUNT = "account";
  /**
   * The attribute for specifying the broker associated with the error.
   */
  public static final String BROKER = "broker";
  /**
   * The attribute for specifying the CCP associated with the error.
   */
  public static final String CCP = "ccp";
  /**
   * The attribute for specifying the product type associated with the error.
   */
  public static final String PRODUCT_TYPE = "productType";

  // restricted constructor
  private FailureAttributeKeys() {
  }
}
