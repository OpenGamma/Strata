/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.sesame.engine.FunctionService;

/**
 * Parses a list of strings and extracts a set of function
 * services from it. The list is provided via user configuration
 * so is checked to ensure the values are valid.
 */
public class FunctionServiceParser {

  private static final Logger s_logger = LoggerFactory.getLogger(FunctionServiceParser.class);

  /**
   * The services that were requested by the user, may be null.
   */
  private final List<String> _requestedFunctionServices;

  /**
   * Create a parser for the supplied services.
   *
   * @param requestedFunctionServices  the services that were requested
   * by the user, may be null
   */
  public FunctionServiceParser(List<String> requestedFunctionServices) {
    _requestedFunctionServices = requestedFunctionServices;
  }

  /**
   * Parse the set of function services from the set provided by the
   * user. If none were provided (i.e. the list was null), or no
   * values can be successfully parsed, then
   * {@link FunctionService#DEFAULT_SERVICES} will be used.
   *
   * If an element cannot be parsed then it will be ignored with a
   * warning logged.
   *
   * @return the set of FunctionServices to be used, not null but may be empty
   */
  public EnumSet<FunctionService> determineFunctionServices() {

    EnumSet<FunctionService> defaultServices = FunctionService.DEFAULT_SERVICES;
    if (_requestedFunctionServices == null) {
      s_logger.info("No function services property defined - using default set: {}", defaultServices);
      return defaultServices;
    } else if (_requestedFunctionServices.isEmpty()) {
      s_logger.info("Empty function services defined - no services will be used");
      return EnumSet.noneOf(FunctionService.class);
    } else {

      EnumSet<FunctionService> parsedServices = parseFunctionServices();
      if (parsedServices.isEmpty()) {
        s_logger.info("No usable function services defined - using default set: {}", defaultServices);
        return defaultServices;
      } else {
        s_logger.info("Function services defined - using: {}", parsedServices);
        return parsedServices;
      }
    }
  }

  private EnumSet<FunctionService> parseFunctionServices() {

    Set<FunctionService> parsed = new HashSet<>();
    for (String service : _requestedFunctionServices) {
      try {
        parsed.add(FunctionService.valueOf(service));
      } catch (IllegalArgumentException e) {
        s_logger.warn("Ignoring unknown function service: {} - accepted values are: {}",
                      service, FunctionService.values());
      }
    }
    return parsed.isEmpty() ?
        EnumSet.noneOf(FunctionService.class) :
        EnumSet.copyOf(parsed);
  }
}
