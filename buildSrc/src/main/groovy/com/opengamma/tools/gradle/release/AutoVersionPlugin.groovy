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

class AutoVersionPlugin implements Plugin<Project>, ReleaseExtensionCreator, TaskNamer
{
    public final static String UPDATE_VERSION_TASK_BASE_NAME = "updateVersion"
    public final static String DESCRIBE_TAG_TASK_BASE_NAME = "describeGitTag"
    public final static String DESCRIBE_COMMIT_TASK_BASE_NAME = "describeGitCommit"
	public final static String VERSIONING_EXTENSION_NAME = "versioning"

    Project project

    @Override
    void apply(Project target)
    {
        this.project = target
	    ClassEnhancer.enhanceVersion()
        createReleaseExtension()
	    createVersioningExtension()

	    Task describeTagTask = addDescribeGitTagTask()
	    Task describeCommitTask = addDescribeGitCommitTask()
        Task updateVersionTask = addUpdateVersionTask()

	    updateVersionTask.dependsOn([describeCommitTask, describeTagTask])

	    project.tasks.all {
		    if([
				    updateVersionTask,
				    describeTagTask,
				    describeCommitTask,
				    project.rootProject.tasks.findByName(ReleasePlugin.CHECK_RELEASE_ENVIRONMENT_TASK_NAME)
		    ].contains(it)) return
		    it.dependsOn updateVersionTask
	    }
    }

    private Task addUpdateVersionTask()
    {
        Task t = project.tasks.create(taskNameFor(UPDATE_VERSION_TASK_BASE_NAME), UpdateVersion)
        project.rootProject.afterEvaluate {
			t.mustRunAfter project.rootProject.tasks[ReleasePlugin.CHECK_RELEASE_ENVIRONMENT_TASK_NAME]
		}
		return t
    }

	private Task addDescribeGitTagTask()
	{
		Task t = project.tasks.create(taskNameFor(DESCRIBE_TAG_TASK_BASE_NAME), SimpleExecWithFailover)
		t.command = "git describe --abbrev=0"
		t.failoverCommand = "git describe --abbrev=0 --tags"
		t.throwOnFailure = false
		t.workingDirectory = project.projectDir
		return t
	}

	private Task addDescribeGitCommitTask()
	{
		Task t = project.tasks.create(taskNameFor(DESCRIBE_COMMIT_TASK_BASE_NAME), SimpleExecWithFailover)
		t.command = "git describe"
		t.failoverCommand = "git describe --tags"
		t.throwOnFailure = false
		t.workingDirectory = project.projectDir
		return t
	}

	private void createVersioningExtension()
	{
		project.extensions.create(VERSIONING_EXTENSION_NAME, AutoVersionExtension)
	}
}
