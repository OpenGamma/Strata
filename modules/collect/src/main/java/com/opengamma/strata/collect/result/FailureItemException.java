/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * An exception thrown when an exception can be represented by a {@code FailureItem}.
 */
public class FailureItemException extends RuntimeException implements FailureItemProvider {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** The failure item. */
  private final FailureItem item;

  /**
   * Returns an exception wrapping the failure item.
   *
   * @param item  the failure item
   */
  public FailureItemException(FailureItem item) {
    super(item.getMessage());
    this.item = ArgChecker.notNull(item, "item");
  }

  /**
   * Returns an exception from a reason and message.
   * <p>
   * The message is produced using a template that contains zero to many "{argName}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#formatWithAttributes(String, Object...)} for more details.
   * 
   * @param reason  the reason
   * @param messageTemplate  a message explaining the failure, not empty, uses "{argName}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   */
  public FailureItemException(FailureReason reason, String messageTemplate, Object... messageArgs) {
    this(FailureItem.of(reason, messageTemplate, messageArgs));
  }

  /**
   * Returns an exception from a reason, cause and message.
   * <p>
   * The message is produced using a template that contains zero to many "{argName}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#formatWithAttributes(String, Object...)} for more details.
   * 
   * @param reason  the reason
   * @param cause  the cause
   * @param messageTemplate  a message explaining the failure, not empty, uses "{argName}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   */
  public FailureItemException(FailureReason reason, Throwable cause, String messageTemplate, Object... messageArgs) {
    this(FailureItem.of(reason, cause, messageTemplate, messageArgs));
    initCause(cause);
  }

  /**
   * Gets the failure item.
   *
   * @return the failure item
   */
  @Override
  public FailureItem getFailureItem() {
    return item;
  }

}
