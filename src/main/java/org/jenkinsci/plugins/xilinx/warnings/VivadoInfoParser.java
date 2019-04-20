package org.jenkinsci.plugins.xilinx.warnings;

public class VivadoInfoParser extends VivadoParser {
    public VivadoInfoParser() {
        super(INFO_PATTERN);
    }
    protected boolean isLineInteresting(String line) {
        updateModuleHierarchy(line);
        return line.startsWith("INFO");
    }
}
