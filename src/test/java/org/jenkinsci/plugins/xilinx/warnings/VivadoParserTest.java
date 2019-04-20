package org.jenkinsci.plugins.xilinx.warnings;

import edu.hm.hafner.analysis.AbstractParserTest;
import edu.hm.hafner.analysis.IssueParser;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.analysis.assertj.SoftAssertions;

/**
 * Tests the class {@link VivadoParserTest}.
 *
 * @author Derrick Gibelyou
 */
class VivadoParserTest extends AbstractParserTest {
    VivadoParserTest() {
        super("vivado.log");
    }

    @Override
    protected IssueParser createParser() {
        return new VivadoWarningParser();
    }

    @Override
    protected void assertThatIssuesArePresent(final Report report, final SoftAssertions softly) {
        softly.assertThat(report).hasSize(5);

        softly.assertThat(report.get(0))
                .hasLineStart(1275)
                .hasFileName("/fpga_tools/Xilinx/Vivado/2017.2/data/ip/xpm/xpm_memory/hdl/xpm_memory.sv")
                .hasType("Synth 8-6014")
                .hasCategory("Synth")
                .hasMessage("Unused sequential element gen_wr_a.gen_word_wide.addralsb_reg was removed.")
                .hasModuleName("/xpm_fifo_async/xpm_fifo_base/xpm_memory_base/")
                .hasSeverity(Severity.WARNING_NORMAL);

        softly.assertThat(report.get(1))
                .hasFileName("/fpga_tools/Xilinx/Vivado/2017.2/data/ip/xpm/xpm_cdc/hdl/xpm_cdc.sv")
                .hasLineStart(302)
                .hasType("Synth 8-6014")
                .hasCategory("Synth")
                .hasMessage("Unused sequential element dest_out_bin_ff_reg was removed.")
                .hasModuleName("/xpm_fifo_async/xpm_fifo_base/xpm_cdc_gray/")
                .hasSeverity(Severity.WARNING_NORMAL);

        softly.assertThat(report.get(2))
                .hasLineStart(16)
                .hasFileName("/data/tmp/jenkins/workspace/generate_bitfile/firmware/src/buses/axis_interface/axis.sv")
                .hasMessage("Net dout\\.user in module/entity axis_assign does not have driver.")
                .hasType("Synth 8-3848")
                .hasCategory("Synth")
                .hasModuleName("/axis_assign/")
                .hasSeverity(Severity.WARNING_HIGH);

        softly.assertThat(report.get(3))
                .hasLineStart(0)
                .hasFileName("-")
                .hasMessage("A LUT 'top/external_io/sclk_r_i_1__0' is driving clock pin of 19 registers. This could lead to large hold time violations. First few involved registers are:")
                .hasCategory("Place")
                .hasType("Place 30-568")
                .hasModuleName("-")
                .hasSeverity(Severity.WARNING_HIGH);

        softly.assertThat(report.get(4))
                .hasLineStart(0)
                .hasFileName("-")
                .hasMessage("Unroutable Placement! A BUFR / MMCM component pair is not placed in a routable site pair. The pair can use the dedicated path between them if they are placed in the same clock region. If this sub optimal condition is acceptable for this design, you may use the CLOCK_DEDICATED_ROUTE constraint in the .xdc file to demote this message to a WARNING. However, the use of this override is highly discouraged. These examples can be used directly in the .xdc file to override this clock rule.")
                .hasDescription("<br>\t< set_property CLOCK_DEDICATED_ROUTE FALSE [get_nets top/sys_clk_buf] ><br><br>\ttop/sys_clk_bufr (BUFR.O) is provisionally placed by clockplacer on BUFR_X0Y12<br>\ttop/mmcm_base_i2 (MMCME2_ADV.CLKIN1) is provisionally placed by clockplacer on MMCME2_ADV_X0Y3<br>\ttop/mmcm_adv_inst (MMCME2_ADV.CLKIN1) is provisionally placed by clockplacer on MMCME2_ADV_X0Y4<br>")
                .hasType("Place 30-132")
                .hasCategory("Place")
                .hasModuleName("-")
                .hasSeverity(Severity.ERROR);


    }
}
