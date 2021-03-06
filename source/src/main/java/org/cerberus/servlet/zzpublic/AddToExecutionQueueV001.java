/**
 * Cerberus Copyright (C) 2013 - 2017 cerberustesting
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This file is part of Cerberus.
 *
 * Cerberus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cerberus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cerberus.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cerberus.servlet.zzpublic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.cerberus.crud.entity.CampaignParameter;
import org.cerberus.crud.entity.TestCase;
import org.cerberus.crud.entity.TestCaseExecutionQueue;
import org.cerberus.crud.service.ICampaignParameterService;
import org.cerberus.crud.service.ILogEventService;
import org.cerberus.crud.service.ITestCaseService;
import org.cerberus.engine.threadpool.IExecutionThreadPoolService;
import org.cerberus.enums.MessageEventEnum;
import org.cerberus.exception.CerberusException;
import org.cerberus.exception.FactoryCreationException;
import org.cerberus.util.ParameterParserUtil;
import org.cerberus.util.answer.AnswerItem;
import org.cerberus.util.servlet.ServletUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.cerberus.crud.factory.IFactoryTestCaseExecutionQueue;
import org.cerberus.crud.service.ITestCaseExecutionQueueService;

/**
 * Add a test case to the execution queue (so to be executed later).
 *
 * @author abourdon
 */
@WebServlet(name = "AddToExecutionQueueV001", urlPatterns = {"/AddToExecutionQueueV001"})
public class AddToExecutionQueueV001 extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(AddToExecutionQueueV001.class);

    private static final String PARAMETER_CAMPAIGN = "campaign";
    private static final String PARAMETER_SELECTED_TEST = "testlist";
    private static final String PARAMETER_SELECTED_TEST_KEY_TEST = "test";
    private static final String PARAMETER_SELECTED_TEST_KEY_TESTCASE = "testcase";
    private static final String PARAMETER_COUNTRY = "country";
    private static final String PARAMETER_ENVIRONMENT = "environment";
    private static final String PARAMETER_BROWSER = "browser";
    private static final String PARAMETER_ROBOT = "robot";
    private static final String PARAMETER_ROBOT_IP = "ss_ip";
    private static final String PARAMETER_ROBOT_PORT = "ss_p";
    private static final String PARAMETER_BROWSER_VERSION = "version";
    private static final String PARAMETER_PLATFORM = "platform";
    private static final String PARAMETER_SCREENSIZE = "screensize";
    private static final String PARAMETER_MANUAL_URL = "manualurl";
    private static final String PARAMETER_MANUAL_HOST = "myhost";
    private static final String PARAMETER_MANUAL_CONTEXT_ROOT = "mycontextroot";
    private static final String PARAMETER_MANUAL_LOGIN_RELATIVE_URL = "myloginrelativeurl";
    private static final String PARAMETER_MANUAL_ENV_DATA = "myenvdata";
    private static final String PARAMETER_TAG = "tag";
    private static final String PARAMETER_SCREENSHOT = "screenshot";
    private static final String PARAMETER_VERBOSE = "verbose";
    private static final String PARAMETER_TIMEOUT = "timeout";
    private static final String PARAMETER_PAGE_SOURCE = "pagesource";
    private static final String PARAMETER_SELENIUM_LOG = "seleniumlog";
    private static final String PARAMETER_RETRIES = "retries";
    private static final String PARAMETER_MANUAL_EXECUTION = "manualexecution";

    private static final int DEFAULT_VALUE_SCREENSHOT = 0;
    private static final int DEFAULT_VALUE_MANUAL_URL = 0;
    private static final int DEFAULT_VALUE_VERBOSE = 0;
    private static final long DEFAULT_VALUE_TIMEOUT = 300;
    private static final int DEFAULT_VALUE_PAGE_SOURCE = 1;
    private static final int DEFAULT_VALUE_SELENIUM_LOG = 1;
    private static final int DEFAULT_VALUE_RETRIES = 0;
    private static final String DEFAULT_VALUE_MANUAL_EXECUTION = "N";

    private static final String LINE_SEPARATOR = "\n";

    private ITestCaseExecutionQueueService inQueueService;
    private IFactoryTestCaseExecutionQueue inQueueFactoryService;
    private IExecutionThreadPoolService executionThreadService;
    private ITestCaseService testCaseService;
    private ICampaignParameterService campaignParameterService;

    /**
     * Process request for both GET and POST method.
     *
     * <p>
     * Request processing is divided in two parts:
     * <ol>
     * <li>Getting all test cases which have been sent to this servlet;</li>
     * <li>Try to insert all these test cases to the execution queue.</li>
     * </ol>
     * </p>
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        final String charset = request.getCharacterEncoding();
        Date requestDate = new Date();

        // Loading Services.
        ApplicationContext appContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        inQueueService = appContext.getBean(ITestCaseExecutionQueueService.class);
        inQueueFactoryService = appContext.getBean(IFactoryTestCaseExecutionQueue.class);
        executionThreadService = appContext.getBean(IExecutionThreadPoolService.class);
        testCaseService = appContext.getBean(ITestCaseService.class);
        campaignParameterService = appContext.getBean(ICampaignParameterService.class);

        // Calling Servlet Transversal Util.
        ServletUtil.servletStart(request);

        /**
         * Adding Log entry.
         */
        ILogEventService logEventService = appContext.getBean(ILogEventService.class);
        logEventService.createForPublicCalls("/AddToExecutionQueueV001", "CALL", "AddToExecutionQueueV001 called : " + request.getRequestURL(), request);

        // Parsing all parameters.
        // Execution scope parameters : Campaign, TestCases, Countries, Environment, Browser.
        String campaign = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_CAMPAIGN), null, charset);
        List<Map<String, String>> selectedTests;
        selectedTests = ParameterParserUtil.parseListMapParamAndDecode(request.getParameterValues(PARAMETER_SELECTED_TEST), null, charset);
        List<String> countries;
        countries = ParameterParserUtil.parseListParamAndDecode(request.getParameterValues(PARAMETER_COUNTRY), null, charset);
        List<String> environments;
        environments = ParameterParserUtil.parseListParamAndDecode(request.getParameterValues(PARAMETER_ENVIRONMENT), null, charset);
        List<String> browsers;
        browsers = ParameterParserUtil.parseListParamAndDecode(request.getParameterValues(PARAMETER_BROWSER), null, charset);
        // Execution parameters.
        String tag = ParameterParserUtil.parseStringParam(request.getParameter(PARAMETER_TAG), "");
        String robot = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_ROBOT), null, charset);
        String robotIP = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_ROBOT_IP), null, charset);
        String robotPort = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_ROBOT_PORT), null, charset);
        String browserVersion = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_BROWSER_VERSION), null, charset);
        String platform = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_PLATFORM), null, charset);
        String screenSize = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_SCREENSIZE), null, charset);
        int manualURL = ParameterParserUtil.parseIntegerParamAndDecode(request.getParameter(PARAMETER_MANUAL_URL), DEFAULT_VALUE_MANUAL_URL, charset);
        String manualHost = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_MANUAL_HOST), null, charset);
        String manualContextRoot = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_MANUAL_CONTEXT_ROOT), null, charset);
        String manualLoginRelativeURL = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_MANUAL_LOGIN_RELATIVE_URL), null, charset);
        String manualEnvData = ParameterParserUtil.parseStringParamAndDecodeAndSanitize(request.getParameter(PARAMETER_MANUAL_ENV_DATA), null, charset);
        int screenshot = ParameterParserUtil.parseIntegerParamAndDecode(request.getParameter(PARAMETER_SCREENSHOT), DEFAULT_VALUE_SCREENSHOT, charset);
        int verbose = ParameterParserUtil.parseIntegerParamAndDecode(request.getParameter(PARAMETER_VERBOSE), DEFAULT_VALUE_VERBOSE, charset);
        String timeout = request.getParameter(PARAMETER_TIMEOUT);
        int pageSource = ParameterParserUtil.parseIntegerParamAndDecode(request.getParameter(PARAMETER_PAGE_SOURCE), DEFAULT_VALUE_PAGE_SOURCE, charset);
        int seleniumLog = ParameterParserUtil.parseIntegerParamAndDecode(request.getParameter(PARAMETER_SELENIUM_LOG), DEFAULT_VALUE_SELENIUM_LOG, charset);
        int retries = ParameterParserUtil.parseIntegerParamAndDecode(request.getParameter(PARAMETER_RETRIES), DEFAULT_VALUE_RETRIES, charset);
        String manualExecution = ParameterParserUtil.parseStringParamAndDecode(request.getParameter(PARAMETER_MANUAL_EXECUTION), DEFAULT_VALUE_MANUAL_EXECUTION, charset);

        // Defining help message.
        String helpMessage = "\nThis servlet is used to add to Cerberus execution queue a list of execution. Execution list will be calculated from cartesian product of "
                + "testcase, country, environment and browser list. Those list can be defined from the associated servlet parameter but can also be defined from campaign directy inside Cerberus.\n"
                + "List defined from servlet overwrite the list defined from the campaign. All other execution parameters will be taken to each execution.\n"
                + "Available parameters:\n"
                + "- " + PARAMETER_CAMPAIGN + " : Campaign name from which testcase, countries, environment and browser can be defined from Cerberus. [" + campaign + "]\n"
                + "- " + PARAMETER_SELECTED_TEST + " : List of testCase to trigger. That list overwrite the list coming from the Campaign (if defined). Ex : " + PARAMETER_SELECTED_TEST + "=" + PARAMETER_SELECTED_TEST_KEY_TEST + "=Cerberus%26" + PARAMETER_SELECTED_TEST_KEY_TESTCASE + "=9644A. [" + selectedTests + "]\n"
                + "- " + PARAMETER_COUNTRY + " : List of countries to trigger. That list overwrite the list coming from the Campaign (if defined).. [" + countries + "]\n"
                + "- " + PARAMETER_ENVIRONMENT + " : List of environment to trigger. That list overwrite the list coming from the Campaign (if defined).. [" + environments + "]\n"
                + "- " + PARAMETER_BROWSER + " : List of browser to trigger. That list overwrite the list coming from the Campaign (if defined).. [" + browsers + "]\n"
                + "- " + PARAMETER_ROBOT + " : Robot Name that will be used for every execution triggered. [" + robot + "]\n"
                + "- " + PARAMETER_ROBOT_IP + " : Robot IP that will be used for every execution triggered. [" + robotIP + "]\n"
                + "- " + PARAMETER_ROBOT_PORT + " : Robot Port that will be used for every execution triggered. [" + robotPort + "]\n"
                + "- " + PARAMETER_BROWSER_VERSION + " : Browser Version that will be used for every execution triggered. [" + browserVersion + "]\n"
                + "- " + PARAMETER_PLATFORM + " : Platform that will be used for every execution triggered. [" + platform + "]\n"
                + "- " + PARAMETER_SCREENSIZE + " : Size of the screen that will be used for every execution triggered. [" + screenSize + "]\n"
                + "- " + PARAMETER_MANUAL_URL + " : Activate (1) or not (0) the Manual URL of the application to execute. If activated the 4 parameters after are necessary. [" + manualURL + "]\n"
                + "- " + PARAMETER_MANUAL_HOST + " : Host of the application to test (only used when " + PARAMETER_MANUAL_URL + " is activated). [" + manualHost + "]\n"
                + "- " + PARAMETER_MANUAL_CONTEXT_ROOT + " : Context root of the application to test (only used when " + PARAMETER_MANUAL_URL + " is activated). [" + manualContextRoot + "]\n"
                + "- " + PARAMETER_MANUAL_LOGIN_RELATIVE_URL + " : Relative login URL of the application (only used when " + PARAMETER_MANUAL_URL + " is activated). [" + manualLoginRelativeURL + "]\n"
                + "- " + PARAMETER_MANUAL_ENV_DATA + " : Environment where to get the test data when a " + PARAMETER_MANUAL_URL + " is defined. (only used when manualURL is active). [" + manualEnvData + "]\n"
                + "- " + PARAMETER_TAG + " [mandatory] : Tag that will be used for every execution triggered. [" + tag + "]\n"
                + "- " + PARAMETER_SCREENSHOT + " : Activate or not the screenshots for every execution triggered. [" + screenshot + "]\n"
                + "- " + PARAMETER_VERBOSE + " : Verbose level for every execution triggered. [" + verbose + "]\n"
                + "- " + PARAMETER_TIMEOUT + " : Timeout used for the action that will be used for every execution triggered. [" + timeout + "]\n"
                + "- " + PARAMETER_PAGE_SOURCE + " : Record Page Source during for every execution triggered. [" + pageSource + "]\n"
                + "- " + PARAMETER_SELENIUM_LOG + " : Get the SeleniumLog at the end of the execution for every execution triggered. [" + seleniumLog + "]\n"
                + "- " + PARAMETER_MANUAL_EXECUTION + " : Execute testcase in manual mode for every execution triggered. [" + manualExecution + "]\n"
                + "- " + PARAMETER_RETRIES + " : Number of tries if the result is not OK for every execution triggered. [" + retries + "]\n";

//        try {
        // Checking the parameter validity.
        boolean error = false;
        if (tag == null || tag.isEmpty()) {
            out.println("Error - Parameter " + PARAMETER_TAG + " is mandatory.");
            error = true;
        }
        if (campaign != null && !campaign.isEmpty()) {
            final AnswerItem<Map<String, List<String>>> parsedCampaignParameters = campaignParameterService.parseParametersByCampaign(campaign);
            if (parsedCampaignParameters.isCodeEquals(MessageEventEnum.DATA_OPERATION_OK.getCode())) {
                // Parameters from campaign could be retreived. we can now replace the parameters comming from the calls in case they are still not defined.
                // If parameters are already defined from request, we ignore the campaign values.
                if (countries == null || countries.isEmpty()) {
                    countries = parsedCampaignParameters.getItem().get(CampaignParameter.COUNTRY_PARAMETER);
                }
                if (environments == null || environments.isEmpty()) {
                    environments = parsedCampaignParameters.getItem().get(CampaignParameter.ENVIRONMENT_PARAMETER);
                }
                if (browsers == null || browsers.isEmpty()) {
                    browsers = parsedCampaignParameters.getItem().get(CampaignParameter.BROWSER_PARAMETER);
                }
            }
            if ((countries != null) && (selectedTests == null || selectedTests.isEmpty())) {
                // If no countries are found, there is no need to get the testcase list. None will be returned.
                selectedTests = new ArrayList<>();
                for (final TestCase testCase : testCaseService.findTestCaseByCampaignNameAndCountries(campaign, countries.toArray(new String[countries.size()]))) {
                    selectedTests.add(new HashMap<String, String>() {
                        {
                            put(PARAMETER_SELECTED_TEST_KEY_TEST, testCase.getTest());
                            put(PARAMETER_SELECTED_TEST_KEY_TESTCASE, testCase.getTestCase());
                        }
                    });
                }
            }
        }
        if (countries == null || countries.isEmpty()) {
            out.println("Error - No Country defined. You can either feed it with parameter '" + PARAMETER_COUNTRY + "' or add it into the campaign definition.");
            error = true;
        }
        if (browsers == null || browsers.isEmpty()) {
            out.println("Error - No Browser defined. You can either feed it with parameter '" + PARAMETER_BROWSER + "' or add it into the campaign definition.");
            error = true;
        }
        if (selectedTests == null || selectedTests.isEmpty()) {
            out.println("Error - No TestCases defined. You can either feed it with parameter '" + PARAMETER_SELECTED_TEST + "' or add it into the campaign definition.");
            error = true;
        }
        if (manualURL >= 1) {
            if (manualHost == null || manualEnvData == null) {
                out.println("Error - ManualURL has been activated but no ManualHost or Manual Environment defined.");
                error = true;
            }
        } else if (environments == null || environments.isEmpty()) {
            out.println("Error - No Environment defined (and " + PARAMETER_MANUAL_URL + " not activated). You can either feed it with parameter '" + PARAMETER_ENVIRONMENT + "' or add it into the campaign definition.");
            error = true;
        }

        // Starting the request only if previous parameters exist.
        if (!error) {

            // Part 1: Getting all possible xecution from test cases + countries + environments + browsers which have been sent to this servlet.
            List<TestCaseExecutionQueue> toInserts = new ArrayList<TestCaseExecutionQueue>();
            for (Map<String, String> selectedTest : selectedTests) {
                String test = selectedTest.get(PARAMETER_SELECTED_TEST_KEY_TEST);
                String testCase = selectedTest.get(PARAMETER_SELECTED_TEST_KEY_TESTCASE);
                for (String country : countries) {
                    for (String environment : environments) {
                        for (String browser : browsers) {
                            try {
                                toInserts.add(inQueueFactoryService.create(test, testCase, country, environment, robot, robotIP, robotPort, browser, browserVersion,
                                        platform, screenSize, manualURL, manualHost, manualContextRoot, manualLoginRelativeURL, manualEnvData, tag, screenshot, verbose,
                                        timeout, pageSource, seleniumLog, 0, retries, manualExecution, request.getRemoteUser(), null, null, null));
                            } catch (FactoryCreationException e) {
                                LOG.error("Unable to insert record due to: " + e);
                                LOG.error("test: " + test + "-" + testCase + "-" + country + "-" + environment + "-" + robot);
                            }
                        }
                    }
                }
            }

            // Part 2: Try to insert all these test cases to the execution queue.
            int nbExe = 0;
            List<String> errorMessages = new ArrayList<String>();
            for (TestCaseExecutionQueue toInsert : toInserts) {
                try {
                    inQueueService.convert(inQueueService.create(toInsert));
                    nbExe++;
                } catch (CerberusException e) {
                    String errorMessage = "Unable to insert " + toInsert.toString() + " due to " + e.getMessage();
                    LOG.warn(errorMessage);
                    errorMessages.add(errorMessage);
                    continue;
                }
            }

            // Part 3 : Put these tests in the queue in memory
            try {
                executionThreadService.executeNextInQueue();
            } catch (CerberusException ex) {
                String errorMessage = "Unable to feed the execution queue due to " + ex.getMessage();
                LOG.warn(errorMessage);
                errorMessages.add(errorMessage);
            }

            if (!errorMessages.isEmpty()) {
                StringBuilder errorMessage = new StringBuilder();
                for (String item : errorMessages) {
                    errorMessage.append(item);
                    errorMessage.append(LINE_SEPARATOR);
                }
                out.println(errorMessage.toString());
            }

            out.println(nbExe + " execution(s) succesfully inserted to queue.");

        } else {
            // In case of errors, we display the help message.
            out.println(helpMessage);
        }

//        } catch (Exception e) {
//            LOG.error(e);
//            out.println(helpMessage);
//            out.println(e.toString());
//        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
