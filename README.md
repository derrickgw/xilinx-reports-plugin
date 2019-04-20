# xilinx-reports Jenkins plugin

Jenkins Plugin for various reporting that would be useful for an FPGA project.

Vivado Infos are reported separately from Warnings/Errors to reduce noise in the warnings plot.
Info parsing is provided to see extreme trends that indicate a problem (often a sudden decrease
in the number of messages).


Example for plotting BRAM, Slice and DSP utilization in a pipeline:
```
node {
    stage('Publish'){
        xilinxUtilization graphConfiguration: [
            [graphCaption: 'BRAMs', graphDataList: 'Block_RAM_Tile'], 
            [graphCaption: 'Slices', graphDataList: 'Slice_LUTs,Slice_Registers,LUT_Flip_Flop_Pairs'],
            [graphCaption: 'DSPs', graphDataList: 'DSPs']], 
            report: 'utilization.rpt'
    }
}
```

Same Example in JobDSL:
```
publishers {
  xilinxUtilization {
    reportName '**/utilization.rpt'
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
```

Example for reporting vivado warnings:
```
node
{
    stage('publish')
    {
        recordIssues tools: [[$class: 'VivadoWarningsTool', pattern: 'vivado.log']],
            filters: [
                excludeFile('.*/prj/vivado/.*\.srcs/sources_1/ip/.*'),
                excludeType('Synth 8-6014')
            ]
    }
}
```