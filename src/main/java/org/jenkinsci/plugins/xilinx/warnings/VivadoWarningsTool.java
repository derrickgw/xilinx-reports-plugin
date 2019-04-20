package org.jenkinsci.plugins.xilinx.warnings;

import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;
import io.jenkins.plugins.analysis.core.model.ReportScanningTool;
import hudson.Extension;

/**
 * Provides a parser and customized messages for your tool.
 */
public class VivadoWarningsTool extends ReportScanningTool {
    static final String ID = "xilinx-vivado-warnings";

    /** Creates a new instance of {@link VivadoWarningsTool}. */
    @DataBoundConstructor
    public VivadoWarningsTool() {
        super();
        // empty constructor required for stapler
    }

    @Override
    public VivadoParser createParser() {
        return new VivadoWarningParser();
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
            return "Vivado";
        }
    }
}
