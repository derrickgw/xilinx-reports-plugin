package org.jenkinsci.plugins.xilinx.warnings;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.LookaheadParser;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.util.LookaheadStream;

import java.util.LinkedList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VivadoParser extends LookaheadParser {

    protected static final String WARN_PATTERN = "(.*WARNING|ERROR): \\[(.*?)\\] (.*)(?: \\[([^:]*):(.*)\\])?";
    protected static final String INFO_PATTERN = "(INFO): \\[(.*?)\\] (.*)(?: \\[([^:]*):(.*)\\])?";
    private static final String FILE_PATTERN = "(.*)(?:\\[(\\D[^:\\]]*)(?::(\\d*)])?)";
    private static final Pattern FILENAME = Pattern.compile(FILE_PATTERN);

    public VivadoParser(String pattern) {
        super(pattern);
    }

    private LinkedList<String> module_path = new LinkedList<>();

    protected void updateModuleHierarchy(final String line)
    {
        final String push = "INFO: [Synth 8-638] synthesizing module '";
        final String pop = "INFO: [Synth 8-256] done synthesizing module '";
        if (line.startsWith(push))
        {
            int nameStart = push.length();
            int nameEnd = line.indexOf("'", nameStart);
            module_path.addLast(line.substring(nameStart, nameEnd));
            System.out.println(getModuleList());
        }
        else if (line.startsWith(pop))
        {
            String module_name = module_path.removeLast();
            int nameStart = pop.length();
            int nameEnd = line.indexOf("'", nameStart);
            String expected_name = line.substring(nameStart, nameEnd);
            //TODO: how to log an error from here.
        }
    }

    private String getModuleList()
    {
        if (module_path.isEmpty())
            return "unknown";

        StringBuilder buf = new StringBuilder("/");
        for(String m : module_path)
        {
            buf.append(m);
            buf.append("/");
        }
        return buf.toString();
    }

    @Override
    protected Optional<Issue> createIssue(final Matcher matcher, final LookaheadStream lookahead,
                                          final IssueBuilder builder) {
        String xilPriority = matcher.group(1);
        builder.setModuleName(getModuleList());
        builder.setCategory(matcher.group(2).split(" ")[0]);
        builder.setType(matcher.group(2));
        builder.setSeverity(mapPriority(xilPriority));

        StringBuilder description = new StringBuilder();
        while (lookahead.hasNext("^($|\t|Resolution:)"))
        {
            description.append("<br>");
            description.append(lookahead.next());
        }

        Matcher m = FILENAME.matcher(matcher.group(3));
        if (m.matches()) {
            builder.setMessage(m.group(1));
            builder.setFileName(m.group(2));
            builder.setLineStart(m.group(3));
            description.insert(0, matcher.group(3));
        } else {
            builder.setMessage(matcher.group(3));
            builder.setFileName(null);
            builder.setLineStart(null);
        }
        builder.setDescription(description.toString());

        builder.setOrigin("Vivado");

        return builder.buildOptional();
    }

    private Severity mapPriority(final String xilPriority) {
        Severity severity = Severity.WARNING_LOW;
        switch (xilPriority) {
            case "ERROR":
                severity = Severity.ERROR;
                break;
            case "CRITICAL WARNING":
                severity = Severity.WARNING_HIGH;
                break;
            case "WARNING":
                severity = Severity.WARNING_NORMAL;
                break;
        }
        return severity;
    }
}
