/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package io.cordova.hellocordova;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Base64;

import org.apache.cordova.*;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends CordovaActivity
{
    private CordovaWebViewEngine mEngine;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // Set by <content src="index.html" /> in config.xml
         loadUrl(launchUrl);

        mEngine = appView.getEngine();
    }

    @Override
    public Object onMessage(String id, Object data) {
        if (id.equals("onXWalkReady")) {
            XWalkView webView = (XWalkView) appView.getView();
            XWalkSettings settings = webView.getSettings();
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
        } else if (id.equals("onPageFinished")) {
            injectCordova();
        }

        return null;
    }

    private final ArrayList<String> preInjectionFileNames = new ArrayList<String>();

    private void injectCordova() {
        List<String> jsPaths = new ArrayList<String>();
        for (String path : preInjectionFileNames) {
            jsPaths.add(path);
        }

        jsPaths.add("www/cordova.js");

        // We load the plugin code manually rather than allow cordova to load them (via
        // cordova_plugins.js).  The reason for this is the WebView will attempt to load the
        // file in the origin of the page (e.g. https://truckmover.com/plugins/plugin/plugin.js).
        // By loading them first cordova will skip its loading process altogether.
        jsPaths.addAll(jsPathsToInject(getResources().getAssets(), "www/plugins"));

        // Initialize the cordova plugin registry.
        jsPaths.add("www/cordova_plugins.js");

        jsPaths.add("www/js/common.js");

        // The way that I figured out to inject for android is to inject it as a script
        // tag with the full JS encoded as a data URI
        // (https://developer.mozilla.org/en-US/docs/Web/HTTP/data_URIs).  The script tag
        // is appended to the DOM and executed via a javascript URL (e.g. javascript:doJsStuff()).
        StringBuilder jsToInject = new StringBuilder();
        for (String path : jsPaths) {
            jsToInject.append(readFile(getResources().getAssets(), path));
        }
        String jsUrl = "javascript:var script = document.createElement('script');";
        jsUrl += "script.src=\"data:text/javascript;charset=utf-8;base64,";

        jsUrl += Base64.encodeToString(jsToInject.toString().getBytes(), Base64.NO_WRAP);
        jsUrl += "\";";

        jsUrl += "document.getElementsByTagName('head')[0].appendChild(script);";

        mEngine.loadUrl(jsUrl, false);
    }

    private String readFile(AssetManager assets, String filePath) {
        StringBuilder out = new StringBuilder();
        BufferedReader in = null;
        try {
            InputStream stream = assets.open(filePath);
            in = new BufferedReader(new InputStreamReader(stream));
            String str = "";

            while ((str = in.readLine()) != null) {
                out.append(str);
                out.append("\n");
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return out.toString();
    }

    private List<String> jsPathsToInject(AssetManager assets, String path) {
        List<String> jsPaths = new ArrayList<String>();

        try {
            for (String filePath : assets.list(path)) {
                String fullPath = path + File.separator + filePath;

                if (fullPath.endsWith(".js")) {
                    jsPaths.add(fullPath);
                } else {
                    List<String> childPaths = jsPathsToInject(assets, fullPath);
                    if (!childPaths.isEmpty()) {
                        jsPaths.addAll(childPaths);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jsPaths;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
