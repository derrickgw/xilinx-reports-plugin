package org.jenkinsci.plugins.xilinx.utilization;

import hudson.model.Job;
import net.praqma.jenkins.memorymap.MemoryMapProjectAction;

public class VivadoUtilizationProjectAction extends MemoryMapProjectAction {

    public VivadoUtilizationProjectAction(Job<?, ?> project) {
        super(project);
    }

    @Override
    public String getDisplayName() {
        return "Xilinx Utilization";
    }

    @Override
    public String getSearchUrl() {
        return "Xilinx Utilization";
    }

    @Override
    public String getIconFileName() {
        return ICON_NAME;
    }

    @Override
    public String getUrlName() {
        return "xilinx-utilization";
    }

}
