/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.simpleexec

interface ShellExecutor
{
	ShellResult execute(Object command)
	ShellResult execute(Object command, Map<String, String> env, File wd)
}
