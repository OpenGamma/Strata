/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.simpleexec

import groovy.transform.Immutable

@Immutable
public class ShellResult
{
    int exitCode
    String stdOut
    String stdErr
}
