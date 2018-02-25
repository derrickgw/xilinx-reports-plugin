package org.jenkinsci.plugins.xilinx.utilization;

import hudson.Extension;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;
import net.praqma.jenkins.memorymap.graph.MemoryMapGraphConfiguration;

import java.util.ArrayList;
import java.util.List;

/*
```
job {
    publishers {
        xilinxUtilization {
            reportName (String reportName)
            graph {
                graphCaption  (String graphCaption)
                graphDataList (String graphData)
            }
        }
    }
}
```

```
job ('utilization_GEN'){
    publishers {
        xilinxUtilization {
            parserTitle 'utilization.rpt'
            graph {
                graphCaption  'DSPs'
                graphDataList 'DSPs'
            }
            graph {
                graphCaption  'BRAM'
                graphDataList 'Block_RAM_Tile'
            }
            graph {
                graphCaption  'Slices'
                graphDataList 'Slice_LUTs,Slice_Registers,LUT_Flip_Flop_Pairs'
            }
        }
    }
}
```
*/

@Extension(optional = true)
public class VivadoUtilizationDslExtension extends ContextExtensionPoint {
    
    @RequiresPlugin(id = "xilinx-reports", minimumVersion = "0.1")
    @DslExtensionMethod(context = PublisherContext.class)
    public Object memoryMap(Runnable closure){
        VivadoUtilizationJobDslContext context = new VivadoUtilizationJobDslContext();
        executeInContext(closure, context);

        return new VivadoUtilizationBuildStep(context.reportName, context.graphConfigurations);
    }


    public class VivadoUtilizationJobDslContext implements Context {

        String reportName = "utilization.rpt";

        public void reportName(String value) {
            reportName = value;
        }
        
        List<MemoryMapGraphConfiguration> graphConfigurations = new ArrayList<>();

        public void graph(Runnable closure){
            VivadoUtilizationGraphDslContext context = new VivadoUtilizationGraphDslContext();
            executeInContext(closure, context);

            graphConfigurations.add(new MemoryMapGraphConfiguration(context.graphDataList, context.graphCaption));
        }


    }

    public class VivadoUtilizationGraphDslContext implements Context {
        String graphDataList;
        String graphCaption;

        public VivadoUtilizationGraphDslContext() {
        }

        public void graphDataList(String value) {
            this.graphDataList = value;
        }

        public void graphCaption(String value) {
            this.graphCaption = value;
        }
    }
}