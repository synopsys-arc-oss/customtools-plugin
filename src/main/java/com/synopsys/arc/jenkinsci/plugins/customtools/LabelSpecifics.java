/*
 * Copyright 2013 Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.synopsys.arc.jenkinsci.plugins.customtools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import java.io.Serializable;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Contains label-specific options.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.3
 */
public class LabelSpecifics extends AbstractDescribableImpl<LabelSpecifics> implements Serializable {
    
    private final @CheckForNull String label;
    private final @CheckForNull String additionalVars;
    private final @CheckForNull String exportedPaths;
    
    @DataBoundConstructor
    public LabelSpecifics(@CheckForNull String label, @CheckForNull String additionalVars, @CheckForNull String exportedPaths) {
        this.label = Util.fixEmptyAndTrim(label);
        this.additionalVars = additionalVars;
        this.exportedPaths = exportedPaths;
    }
    
    public @CheckForNull String getAdditionalVars() {
        return additionalVars;
    }
    
     public boolean hasAdditionalVars() {
        return additionalVars != null;
    }

    public @CheckForNull String getLabel() {
        return label;
    }

    public @CheckForNull String getExportedPaths() {
        return exportedPaths;
    }
    
    /**
     * Check if specifics is applicable to node 
     * @param node Node to be checked
     * @return True if specifics is applicable to node
     */
    public boolean appliesTo(@Nonnull Node node) {
        String correctedLabel = Util.fixEmptyAndTrim(label);
        if (correctedLabel == null) {
            return true;
        }
        
        Label l = Jenkins.getInstance().getLabel(label);
        return l == null || l.contains(node);
    }
    
    public @Nonnull LabelSpecifics substitute(EnvVars vars) {
        return new LabelSpecifics(label, vars.expand(additionalVars), vars.expand(exportedPaths));
    }
    
    public @Nonnull LabelSpecifics substitute(Node node) {
        return new LabelSpecifics(label, 
                EnvStringParseHelper.resolveExportedPath(additionalVars, node), 
                EnvStringParseHelper.resolveExportedPath(exportedPaths, node));
    }
    
    public static @Nonnull LabelSpecifics[] substitute (LabelSpecifics[] specifics, EnvVars vars) {
        LabelSpecifics[] out = new LabelSpecifics[specifics.length];
        for (int i=0; i<specifics.length; i++) {
            out[i] = specifics[i].substitute(vars);
        }
        return out;
    }
    
    public static @Nonnull LabelSpecifics[] substitute (LabelSpecifics[] specifics, Node node) {
        LabelSpecifics[] out = new LabelSpecifics[specifics.length];
        for (int i=0; i<specifics.length; i++) {
            out[i] = specifics[i].substitute(node);
        }
        return out;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<LabelSpecifics> {
        @Override
        public String getDisplayName() {
            return Messages.LabelSpecifics_DisplayName();
        }
    }  
}
