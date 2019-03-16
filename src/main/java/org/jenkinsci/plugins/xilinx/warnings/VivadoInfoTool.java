package org.jenkinsci.plugins.xilinx.warnings;

import hudson.Extension;
import io.jenkins.plugins.analysis.core.model.ReportScanningTool;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Provides a parser and customized messages for your tool.
 */
public class VivadoInfoTool extends ReportScanningTool {
    static final String ID = "xilinx-vivado-info";

    /** Creates a new instance of {@link VivadoInfoTool}. */
    @DataBoundConstructor
    public VivadoInfoTool() {
        super();
        // empty constructor required for stapler
    }

    @Override
    public VivadoParser createParser() {
        return new VivadoInfoParser();
    }

    /** Descriptor for this static analysis tool. */
    @Extension
    public static class Descriptor extends ReportScanningToolDescriptor {
        /** Creates the descriptor instance. */
        public Descriptor() {
            super(ID);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Vivado Info";
        }
    }
}
