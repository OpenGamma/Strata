/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx.error

import groovy.transform.TupleConstructor

@TupleConstructor
class MissingApplicationError implements EnvironmentError
{
	String applicationName

	@Override
	public String getMessage()
	{
		"No ${-> applicationName} on path"
	}

	@Override
	public String getHelp()
	{
		return "Install missing applications from your OS's package manager"
	}
}
