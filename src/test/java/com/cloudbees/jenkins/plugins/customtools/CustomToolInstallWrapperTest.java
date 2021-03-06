/*
 * Copyright 2012, CloudBees Inc.
 * Copyright 2013, Synopsys Inc., Oleg Nenashev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudbees.jenkins.plugins.customtools;

import com.synopsys.arc.jenkins.plugins.customtools.util.CommandCallerInstaller;
import com.synopsys.arc.jenkins.plugins.customtools.util.StubWrapper;
import com.synopsys.arc.jenkinsci.plugins.customtools.multiconfig.MulticonfigWrapperOptions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Contains tests for {@link CustomToolInstallWrapper}.
 * @author rcampbell
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 */
public class CustomToolInstallWrapperTest extends HudsonTestCase {
    
    private static final String NON_EXISTENT_TOOL = "non-existent";
        
    /**
     * Inserts {@link CustomToolInstallWrapper} after {@link StubWrapper}.
     * @throws Exception 
     */
    public void testNestedWrapper() throws Exception {
        List<BuildWrapper> wrappers = new ArrayList<BuildWrapper>(2);
        wrappers.add(new StubWrapper());
        wrappers.add(setupCustomToolsWrapper());
        nestedWrapperTestImpl(wrappers, true);
    }
       
    /**
     * Inserts {@link StubWrapper} after {@link CustomToolInstallWrapper}.
     * @throws Exception 
     */
    public void testNestedWrapperReverse() throws Exception {
        List<BuildWrapper> wrappers = new ArrayList<BuildWrapper>(2);
        wrappers.add(setupCustomToolsWrapper());
        wrappers.add(new StubWrapper());       
        nestedWrapperTestImpl(wrappers, true);
    }
    
    
    
    /**
     * Tests custom tools with wrapper, which calls wrapper without
     * specifying of envs.
     * @throws Exception 
     */
    //@Bug(19506))
    public void testNestedLauncherCalls() throws Exception {
        List<BuildWrapper> wrappers = new ArrayList<BuildWrapper>(2);
        wrappers.add(new CommandCallerInstaller());  
        wrappers.add(setupCustomToolsWrapper());     
        nestedWrapperTestImpl(wrappers, false);
    }
    
    @Bug(20560)
    public void testEmptyToolsList() throws Exception {
        List<BuildWrapper> wrappers = new ArrayList<BuildWrapper>(0); 
        wrappers.add(new CommandCallerInstaller());
        wrappers.add(new CustomToolInstallWrapper(null, MulticonfigWrapperOptions.DEFAULT, false));
        nestedWrapperTestImpl(wrappers, false);
    }
    
    @Bug(0)
    public void testDeletedTool() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        
        CustomToolInstallWrapper.SelectedTool[] tools = 
                new CustomToolInstallWrapper.SelectedTool[] { 
                    new CustomToolInstallWrapper.SelectedTool(NON_EXISTENT_TOOL)
                };
        
        project.getBuildWrappersList().add(
                new CustomToolInstallWrapper(tools, MulticonfigWrapperOptions.DEFAULT, false));
        
        Future<FreeStyleBuild> build = project.scheduleBuild2(0);
        assertBuildStatus(Result.FAILURE, build.get());
        assertLogContains( 
                Messages.CustomTool_GetToolByName_ErrorMessage(NON_EXISTENT_TOOL), build.get());
    }
    
    /**
     * Implements tests for nested wrappers.
     * The test checks that environment variables have been set correctly.
     * It also expects existence of {@link StubWrapper} in the wrappers list.
     */
    private void nestedWrapperTestImpl(List<BuildWrapper> wrappers, boolean checkEnvironment) throws Exception {      
        hudson.setNumExecutors(0);
        createSlave();
        
        // Create test project
        FreeStyleProject project = createFreeStyleProject();        
        project.getBuildWrappersList().addAll(wrappers);   
        
        if (checkEnvironment) {
            project.getBuildersList().add(checkVariableBuilder(StubWrapper.ENV_TESTVAR_NAME, StubWrapper.ENV_TESTVAR_VALUE));
            project.getBuildersList().add(checkVariableBuilder(StubWrapper.SCRIPT_TESTVAR_NAME, StubWrapper.SCRIPT_TESTVAR_VALUE));
        }
        
        Future<FreeStyleBuild> build = project.scheduleBuild2(0);
        assertBuildStatusSuccess(build);
    }
    
    private CustomToolInstallWrapper setupCustomToolsWrapper() 
            throws IOException {
        CustomTool.DescriptorImpl tools = hudson.getDescriptorByType(CustomTool.DescriptorImpl.class);
        tools.setInstallations(CustomToolInstallerTest.createTool("MyTrue"));
        CustomToolInstallWrapper.SelectedTool selectedTool = new CustomToolInstallWrapper.SelectedTool("MyTrue");
        
        return new CustomToolInstallWrapper(
                new CustomToolInstallWrapper.SelectedTool[] { selectedTool }, 
                MulticonfigWrapperOptions.DEFAULT, false);
    }   
    
    private Builder checkVariableBuilder(String varName, String varValue) {
        Builder b = new Shell("env \nif [ \"$"+ varName+"\" != \""+ 
                varValue + "\" ] ; then \n  echo Test failed \n  exit -1 \n" +
                "else \n  echo OK:"+varName+"="+varValue+" \nfi");
        return b;      
    }
}
