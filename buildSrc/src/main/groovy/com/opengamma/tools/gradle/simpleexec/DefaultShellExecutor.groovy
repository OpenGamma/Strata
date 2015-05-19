/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.simpleexec

import org.gradle.api.GradleException

class DefaultShellExecutor implements ShellExecutor
{
	ShellResult execute(Object command, Map<String, String> env = null, File wd = null)
	{
		int exit = -1
		Process p

		StringBuilder sbOut = new StringBuilder()
		StringBuilder sbErr = new StringBuilder()

		try
		{
			p = startProcess(command, wd, env)
//			p.consumeProcessOutput(sbOut, sbErr)
			p.waitForProcessOutput(sbOut, sbErr)
			exit = p.waitFor()
		}
		catch(final Exception ex)
		{
			throw new GradleException("Could not run system command", ex)
		}

		return new ShellResult(exit, sbOut.toString().trim(), sbErr.toString().trim())
	}

	private Process startProcess(Object command, File wd, Map<String, String> env)
	{
		if(command instanceof List)
		{
			ProcessBuilder pb = new ProcessBuilder().command(command).directory(wd)
			Map<String, String> environment = pb.environment()
			environment.putAll env
			return pb.start()
		}
		else
		{
			String[] flatEnv = env.collect { k, v -> "${k}=${v}" } as String[]
			return command.toString().execute(flatEnv, wd)
		}
	}
}
