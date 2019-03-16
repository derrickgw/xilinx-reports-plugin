package org.jenkinsci.plugins.xilinx.warnings;

public class VivadoWarningParser extends VivadoParser {
    public VivadoWarningParser() {
        super(WARN_PATTERN);
    }

    @Override
    protected boolean isLineInteresting(String line)
    {
        updateModuleHierarchy(line);
        return (line.startsWith("CRITICAL") ||
                line.startsWith("WARNING") ||
                line.startsWith("ERROR"));
    }
}
