/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sittinglittleduck.DirBuster;

import java.net.URL;

/**
 *
 * @author james
 */
public class Result
{
    public static final int DIR = 0;
    public static final int FILE = 1;

    private int type = -1;
    private URL itemFound;
    private int responseCode = 0;
    private String responseHeader = "";
    private String responseBody = "";
    private BaseCase baseCaseObj = null;

    public Result(int type, URL itemFound, int responseCode, String responseHeader, String responseBody, BaseCase baseCaseObj)
    {
        this.type = type;
        this.itemFound = itemFound;
        this.responseCode = responseCode;
        this.responseHeader = responseHeader;
        this.responseBody = responseBody;
        this.baseCaseObj = baseCaseObj;
    }

    public BaseCase getBaseCaseObj()
    {
        return baseCaseObj;
    }

    public URL getItemFound()
    {
        return itemFound;
    }

    public String getResponseBody()
    {
        return responseBody;
    }

    public String getResponseHeader()
    {
        return responseHeader;
    }

    public int getresponseCode()
    {
        return responseCode;
    }

    public int getType()
    {
        return type;
    }
}
