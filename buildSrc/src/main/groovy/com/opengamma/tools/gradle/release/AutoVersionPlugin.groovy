/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.release

import com.opengamma.tools.gradle.release.task.UpdateVersion
import com.opengamma.tools.gradle.simpleexec.SimpleExecWithFailover
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class AutoVersionPlugin implements Plugin<Project>
{
    public final static String UPDATE_VERSION_TASK_NAME = "updateVersion"
    public final static String DESCRIBE_TAG_TASK_NAME = "describeGitTag"
    public final static String DESCRIBE_COMMIT_TASK_NAME = "describeGitCommit"

    Project project

    @Override
    void apply(Project target)
    {
        this.project = target
        project.extensions.create("release", ReleaseExtension)

	    Task describeTagTask = addDescribeGitTagTask()
	    Task describeCommitTask = addDescribeGitCommitTask()
        Task updateVersionTask = addUpdateVersionTask()

	    updateVersionTask.dependsOn([describeCommitTask, describeTagTask])

	    project.tasks.all {
		    if([updateVersionTask, describeTagTask, describeCommitTask].contains(it)) return
		    it.dependsOn updateVersionTask
	    }
    }

    private Task addUpdateVersionTask()
    {
        Task t = project.tasks.create(UPDATE_VERSION_TASK_NAME, UpdateVersion)
        // TODO t.mustRunAfter "checkReleaseEnvironment"
    }

	private Task addDescribeGitTagTask()
	{
		Task t = project.tasks.create(DESCRIBE_TAG_TASK_NAME, SimpleExecWithFailover)
		t.command = "git describe --abbrev=0"
		t.failoverCommand = "git describe --abbrev=0 --tags"
		t.throwOnFailure = false
		t.workingDirectory = project.rootDir
		return t
	}

	private Task addDescribeGitCommitTask()
	{
		Task t = project.tasks.create(DESCRIBE_COMMIT_TASK_NAME, SimpleExecWithFailover)
		t.command = "git describe"
		t.failoverCommand = "git describe --tags"
		t.throwOnFailure = false
		t.workingDirectory = project.rootDir
		return t
	}
}
