/*
 * GenBaseCase.java
 *
 * Created on 28 June 2007, 23:16
 *
 * Copyright 2007 James Fisher
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */
package com.sittinglittleduck.DirBuster;

import com.sittinglittleduck.DirBuster.gui.JDialogInconsistentFailCodes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Generates a base case for a dir or file rangle that is about to be scanned
 * @author James
 */
public class GenBaseCase
{

    /** Creates a new instance of GenBaseCase */
    public GenBaseCase()
    {
    }

    /**
     * Generates the base case
     * @param url The directy or file we need a base case for
     * @param isDir true if it's dir, else false if it's a file
     * @param fileExtention File extention to be scanned, set to null if it's a dir that is to be tested
     * @return A BaseCase Object
     */
    public static synchronized BaseCase genBaseCase(String url, boolean isDir, String fileExtention) throws MalformedURLException, IOException
    {
        String type;
        if(isDir)
        {
            type = "Dir";
        }
        else
        {
            type = "File";
        }

        /*
         * markers for using regex instead
         */
        boolean useRegexInstead = false;
        String regex = null;

        //get the manager instance
        Manager manager = Manager.getInstance();

        BaseCase tempBaseCase = manager.getBaseCase(url, isDir, fileExtention);

        if(tempBaseCase != null)
        {
            //System.out.println("Using prestored basecase");
            return tempBaseCase;

        }


        //System.out.println("DEBUG GenBaseCase: URL to get baseCase for: " + url + " (" + type + ")");
        if(Config.debug)
        {
            System.out.println("DEBUG GenBaseCase: URL to get baseCase for:" + url);
        }

        BaseCase baseCase = null;
        int failcode = 0;
        String failString = Config.failCaseString;
        String baseResponse = "";
        URL failurl = null;
        if(isDir)
        {
            failurl = new URL(url + failString + "/");
        }
        else
        {
            /*
            if(manager.isBlankExt())
            {
                fileExtention = "";
                failurl = new URL(url + failString + fileExtention);
            }
            else
            {
                if(!fileExtention.startsWith("."))
                {
                    fileExtention = "." + fileExtention;
                }
                failurl = new URL(url + failString + fileExtention);
            }
             */
            failurl = new URL(url + failString + fileExtention);
        }

        //System.out.println("DEBUG GenBaseCase: Getting :" + failurl + " (" + type + ")");
        if(Config.debug)
        {
            System.out.println("DEBUG GenBaseCase: Getting:" + failurl);
        }

        GetMethod httpget = new GetMethod(failurl.toString());
        //set the custom HTTP headers
        Vector HTTPheaders = manager.getHTTPHeaders();
        for(int a = 0; a < HTTPheaders.size(); a++)
        {
            HTTPHeader httpHeader = (HTTPHeader) HTTPheaders.elementAt(a);
            /*
             * Host header has to be set in a different way!
             */
            if(httpHeader.getHeader().startsWith("Host"))
            {
                httpget.getParams().setVirtualHost(httpHeader.getValue());
            }
            else
            {
                httpget.setRequestHeader(httpHeader.getHeader(), httpHeader.getValue());
            }

        }
        httpget.setFollowRedirects(Config.followRedirects);

        //save the http response code for the base case
        failcode = manager.getHttpclient().executeMethod(httpget);
        manager.workDone();

        //we now need to get the content as we need a base case!
        if(failcode == 200)
        {
            if(Config.debug)
            {
                System.out.println("DEBUG GenBaseCase: base case for " + failurl.toString() + "came back as 200!");
            }

            BufferedReader input = new BufferedReader(new InputStreamReader(httpget.getResponseBodyAsStream()));
            String tempLine;
            StringBuffer buf = new StringBuffer();
            while((tempLine = input.readLine()) != null)
            {
                buf.append("\r\n" + tempLine);
            }
            baseResponse = buf.toString();
            input.close();

            //HTMLparse.parseHTML();

            //HTMLparse htmlParse = new HTMLparse(baseResponse, null);
            //Thread parse  = new Thread(htmlParse);
            //parse.start();

            //clean up the base case, based on the basecase URL
            baseResponse = FilterResponse.CleanResponse(baseResponse, failurl, failString);

            httpget.releaseConnection();

            /*
             * get the base case twice more, for consisitency checking
             */
            String baseResponse1 = baseResponse;
            String baseResponse2 = getBaseCaseAgain(failurl, failString);
            String baseResponse3 = getBaseCaseAgain(failurl, failString);


            if(baseResponse1 != null && baseResponse2 != null && baseResponse3 != null)
            {
                /*
                 * check that all the responses are same, if they are do nothing if not enter the if statement
                 */

                if(!baseResponse1.equalsIgnoreCase(baseResponse2) || !baseResponse1.equalsIgnoreCase(baseResponse3) || !baseResponse2.equalsIgnoreCase(baseResponse3))
                {
                    /*
                     * if we dont have any regex saved
                     */
                    if(manager.getFailCaseRegexes().size() == 0)
                    {
                        /*
                         * pause workers
                         */
                        manager.pause();

                        /*
                         * pause work generators
                         */
                        manager.pauseAllWorkGen();
                        
                        /*
                         * show diaglog with the three responses, asking the user for a reg instead
                         */
                        JDialogInconsistentFailCodes dialog = new JDialogInconsistentFailCodes(manager.gui, true, baseResponse1, baseResponse2, baseResponse3, failurl.toString());
                        /*
                         * get the returned regex
                         */


                        manager.setFailCaseDialogVisable(true);
                        regex = dialog.showAndGetRegex();
                        manager.addFailCaseRegex(regex);
                        manager.setFailCaseDialogVisable(false);
                        useRegexInstead = true;

                        /*
                         * unpause everything
                         */
                        manager.unPause();
                        manager.unPauseAllWorkGen();

                    }
                    else
                    {
                        
                        /*
                         * for each saved regex see if it will work, if it does then use that one
                         * if not then give the uses the dialog
                         */

                        boolean found = false;
                        Vector<String> failCaseRegexes = manager.getFailCaseRegexes();
                        for(int a = 0; a < failCaseRegexes.size(); a++)
                        {
                            
                            Pattern regexFindFile = Pattern.compile(failCaseRegexes.elementAt(a));

                            Matcher m1 = regexFindFile.matcher(baseResponse1);
                            Matcher m2 = regexFindFile.matcher(baseResponse2);
                            Matcher m3 = regexFindFile.matcher(baseResponse3);

                            boolean test1 = m1.find();
                            boolean test2 = m2.find();
                            boolean test3 = m3.find();

                            if(test1 && test2 && test3)
                            {
                                regex = failCaseRegexes.elementAt(a);
                                useRegexInstead = true;
                                found = true;
                                break;
                            }

                        }

                        if(!found)
                        {
                            /*
                             * pause workers
                             */
                            manager.pause();

                            /*
                             * pause work generators
                             */
                            manager.pauseAllWorkGen();

                            /*
                             * if we get here then we didn't get a match so show the dialog
                             */

                            JDialogInconsistentFailCodes dialog = new JDialogInconsistentFailCodes(manager.gui, true, baseResponse1, baseResponse2, baseResponse3, failurl.toString());
                            /*
                             * get the returned regex
                             */
                            manager.setFailCaseDialogVisable(true);
                            regex = dialog.showAndGetRegex();
                            manager.addFailCaseRegex(regex);
                            manager.setFailCaseDialogVisable(false);
                            useRegexInstead = true;

                            /*
                             * unpause everything
                             */
                            manager.unPause();
                            manager.unPauseAllWorkGen();
                        }

                    }
                }
                else
                {
                    /*
                     * We have a big problem as now we have different response codes for the same request
                     * //TODO think of a way to deal with is
                     */
                }


                if(Config.debug)
                {
                    System.out.println("DEBUG GenBaseCase: base case was set to :" + baseResponse);
                }
            }
        }
        httpget.releaseConnection();

        baseCase = new BaseCase(new URL(url), failcode, isDir, failurl, baseResponse, fileExtention, useRegexInstead, regex);

        //add the new base case to the manager list
        manager.addBaseCase(baseCase);
        manager.addNumberOfBaseCasesProduced();

        return baseCase;
    }

    /*
     * Used to generate a basecase when we are URL fuzzing
     */
    public static BaseCase genURLFuzzBaseCase(String fuzzStart, String FuzzEnd) throws MalformedURLException, IOException
    {
        Manager manager = Manager.getInstance();
        BaseCase baseCase = null;
        int failcode = 0;
        String failString = Config.failCaseString;
        String baseResponse = "";

        /*
         * markers for using regex instead
         */
        boolean useRegexInstead = false;
        String regex = null;

        URL failurl = new URL(fuzzStart + failString + FuzzEnd);

        GetMethod httpget = new GetMethod(failurl.toString());
        //set the custom HTTP headers
        Vector HTTPheaders = manager.getHTTPHeaders();
        for(int a = 0; a < HTTPheaders.size(); a++)
        {
            HTTPHeader httpHeader = (HTTPHeader) HTTPheaders.elementAt(a);
            /*
             * Host header has to be set in a different way!
             */
            if(httpHeader.getHeader().startsWith("Host"))
            {
                httpget.getParams().setVirtualHost(httpHeader.getValue());
            }
            else
            {
                httpget.setRequestHeader(httpHeader.getHeader(), httpHeader.getValue());
            }

        }
        httpget.setFollowRedirects(Config.followRedirects);

        //save the http response code for the base case
        failcode = manager.getHttpclient().executeMethod(httpget);
        manager.workDone();

        if(failcode == 200)
        {
            if(Config.debug)
            {
                System.out.println("DEBUG GenBaseCase: base case for " + failurl.toString() + "came back as 200!");
            }

            BufferedReader input = new BufferedReader(new InputStreamReader(httpget.getResponseBodyAsStream()));
            String tempLine;
            StringBuffer buf = new StringBuffer();
            while((tempLine = input.readLine()) != null)
            {
                buf.append("\r\n" + tempLine);
            }
            baseResponse = buf.toString();
            input.close();


            //clean up the base case, based on the basecase URL
            baseResponse = FilterResponse.CleanResponse(baseResponse, failurl, failString);

            if(Config.debug)
            {
                System.out.println("DEBUG GenBaseCase: base case was set to :" + baseResponse);
            }
        }

        httpget.releaseConnection();

        /*
         * create the base case object
         */
        baseCase = new BaseCase(null, failcode, false, failurl, baseResponse, null, useRegexInstead, regex);

        return baseCase;
    }

    /*
     * this function is used to get base case again, so we can check that the base case is consitent.
     */
    private static String getBaseCaseAgain(URL failurl, String failString) throws IOException
    {
        int failcode;
        Manager manager = Manager.getInstance();
        String baseResponse = "";

        GetMethod httpget = new GetMethod(failurl.toString());
        //set the custom HTTP headers
        Vector HTTPheaders = manager.getHTTPHeaders();
        for(int a = 0; a < HTTPheaders.size(); a++)
        {
            HTTPHeader httpHeader = (HTTPHeader) HTTPheaders.elementAt(a);
            /*
             * Host header has to be set in a different way!
             */
            if(httpHeader.getHeader().startsWith("Host"))
            {
                httpget.getParams().setVirtualHost(httpHeader.getValue());
            }
            else
            {
                httpget.setRequestHeader(httpHeader.getHeader(), httpHeader.getValue());
            }

        }
        httpget.setFollowRedirects(Config.followRedirects);

        //save the http response code for the base case
        failcode = manager.getHttpclient().executeMethod(httpget);
        manager.workDone();

        //we now need to get the content as we need a base case!
        if(failcode == 200)
        {
            if(Config.debug)
            {
                System.out.println("DEBUG GenBaseCase: base case for " + failurl.toString() + "came back as 200!");
            }

            BufferedReader input = new BufferedReader(new InputStreamReader(httpget.getResponseBodyAsStream()));
            String tempLine;
            StringBuffer buf = new StringBuffer();
            while((tempLine = input.readLine()) != null)
            {
                buf.append("\r\n" + tempLine);
            }
            baseResponse = buf.toString();
            input.close();

            //HTMLparse.parseHTML();

            //HTMLparse htmlParse = new HTMLparse(baseResponse, null);
            //Thread parse  = new Thread(htmlParse);
            //parse.start();

            //clean up the base case, based on the basecase URL
            baseResponse = FilterResponse.CleanResponse(baseResponse, failurl, failString);

            httpget.releaseConnection();

            /*
             * return the cleaned response
             */
            return baseResponse;
        }
        else
        {
            /*
             * we have a big problem here as the server has returned an other response code, for the same request
             * TODO: think of a way to deal with this!
             */
            return null;
        }
    }
}
