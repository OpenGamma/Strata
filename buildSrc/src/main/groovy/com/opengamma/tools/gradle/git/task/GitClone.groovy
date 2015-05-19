/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.git.task

import com.opengamma.tools.gradle.simpleexec.SimpleExec
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GitClone extends BaseGitTask
{
	@Input
	String gitRepoURL

	@Input
	@Optional
	String gitBranch = "master"

	@Input
	@Optional
	boolean wipeExisting = true

	@OutputDirectory
	File outputDirectory


	@TaskAction
	void doClone()
	{
		if(outputDirectory.isDirectory() && wipeExisting)
			outputDirectory.deleteDir()

		Task doClone = project.tasks.create("execGitClone", SimpleExec)
		doClone.configure {
			command "git clone ${-> gitRepoURL} ${-> outputDirectory.canonicalPath}"
		}

		Task doCheckoutBranch = project.tasks.create("execGitCheckout", SimpleExec)
		doCheckoutBranch.configure {
			command "git checkout ${-> gitBranch}"
			workingDirectory = outputDirectory
		}

		doClone.execute()
		doCheckoutBranch.execute()
	}
}
