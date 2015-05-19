package com.opengamma.tools.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.jvm.application.tasks.CreateStartScripts

class ApplicationsConvention
{
    final MultiScriptPlugin plugin
    private static final Random random = new Random()

    public ApplicationsConvention(MultiScriptPlugin plugin)
    {
        this.plugin = plugin
    }

    public void applications(Closure c)
    {
        c.setDelegate(new ApplicationsBuilder())
        c.call()
    }

    private class ApplicationsBuilder extends BuilderSupport
    {
        String applicationName

//        @Delegate
//        JavaExec propertyHolder

        @Override
        protected Object createNode(Object name, Object value)
        {
            defineApplication(
                    name: name,
                    mainClassName: value.toString()
            )
        }

        @Override
        protected Object createNode(Object name, Map attributes)
        {
            println "[!!] createNode(${name}, ${attributes}) [name, attributes]"
        }

        @Override
        protected Object createNode(Object name, Map attributes, Object value)
        {
            println "[!!] createNode(${name}, ${attributes}, ${value}) [name, attributes, value]"
        }

        ///////////////////

        @Override
        protected void setParent(Object parent, Object child)
        {
            println "[!!] setParent(${parent}, ${child})"
        }

        @Override
        protected Object createNode(Object name)
        {
            println "[!!] createNode(${name}) [name]"
        }

        private void defineApplication(Map params)
        {

            Project project = plugin.project
            TaskContainer tc = project.tasks
            CreateStartScripts task = tc.create("RandomizeThis${random.nextInt(999)}", CreateStartScripts)
            task.configure {
                params.each { k, v ->
                    if(hasProperty(k))
                        this[k] = v
                }
            }
        }

        /////////////////

//        @Override
//        Object invokeMethod(String methodName) {
//            println "[!!] invokeMethod(${methodName})"
//            return super.invokeMethod(methodName)
//        }
//
//        @Override
//        Object invokeMethod(String methodName, Object args)
//        {
//            print "[!!] invokeMethod(${methodName}, ${args}) ==> "
//            println "(${args.collect { it.getClass().name + ", " }})"
//
//            if(args.size() == 2) {
//                println "[!!!] ${args[1] instanceof Closure}"
//            }
//            return super.invokeMethod(methodName, args)
//        }
    }


}
