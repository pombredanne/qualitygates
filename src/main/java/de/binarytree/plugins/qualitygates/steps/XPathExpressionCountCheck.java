package de.binarytree.plugins.qualitygates.steps;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.binarytree.plugins.qualitygates.result.GateStepReport;

public class XPathExpressionCountCheck extends XMLCheck {

    private int successThreshold;

    private int warningThreshold;

    private String name;

    @DataBoundConstructor
    public XPathExpressionCountCheck(String name, String targetFile,
            String expression, int successThreshold, int warningThreshold) {
        this(targetFile, expression, successThreshold, warningThreshold);
        this.name = name;
    }

    public XPathExpressionCountCheck(String targetFile, String expression,
            int successThreshold, int warningThreshold) {
        super(expression, targetFile);
        this.successThreshold = successThreshold;
        this.warningThreshold = warningThreshold;
    }

    public int getSuccessThreshold() {
        return this.successThreshold;
    }

    public int getWarningThreshold() {
        return this.warningThreshold;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void doStep(AbstractBuild build, Launcher launcher,
            BuildListener listener, GateStepReport checkReport) {
        try {
            matchExpression(build, checkReport);
        } catch (Exception e) {
            failStepWithExceptionAsReason(checkReport, e);
        }
    }

    private void matchExpression(AbstractBuild build, GateStepReport checkReport)
            throws IOException, ParserConfigurationException, SAXException,
            XPathExpressionException {
        InputStream stream = this.obtainInputStream(build);
        NodeList nodes = getMatchingNodes(stream);
        this.setCheckResult(checkReport, nodes);
    }

    private void setCheckResult(GateStepReport checkReport, NodeList nodes) {
        if (nodes != null) {
            int length = nodes.getLength();

            Result result = Result.FAILURE;
            String reason;
            if (this.countIsSuccess(length)) {
                result = Result.SUCCESS;
                reason = length + " <= success threshold("
                        + this.successThreshold + ")";
            } else if (this.countIsWarning(length)) {
                result = Result.UNSTABLE;
                reason = "success threshold(" + this.successThreshold + ") < "
                        + length + " <= warning threshold("
                        + this.warningThreshold + ")";
            } else {
                reason = "warning threshold(" + this.warningThreshold + ") < "
                        + length;
            }

            checkReport.setResult(result, reason);
        } else {
            checkReport
                    .setResult(Result.SUCCESS, "No occurrence");
        }
    }

    public boolean countIsSuccess(int count) {
        return count <= this.successThreshold;
    }

    public boolean countIsWarning(int count) {
        return count <= this.warningThreshold;
    }

    @Override
    public String getDescription() {
        return this.name + ": Count of " + this.getExpression() + " in "
                + this.getTargetFile();
    }

    @Extension
    public static class DescriptorImpl extends XMLCheckDescriptor {

        @Override
        public String getDisplayName() {
            return "Count the occurence of an XPath expression in an XML file";
        }

    }
}