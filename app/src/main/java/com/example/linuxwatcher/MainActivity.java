/*
 * Copyright (c) 2016 Austin Ochoa
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.example.linuxwatcher;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {
    /*
        We will use the makefile for the Linux kernel source code hosted on
        kernel.org to find the current version info for the Linux kernel
     */
    private static final String KERNEL_MAKEFILE_URL = "https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/plain/Makefile/";
    private TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.versionPrintout);
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadMakefile().execute(KERNEL_MAKEFILE_URL);
        } else {
            textView.setText("Cannot connect to the internet.");
        }
    }

    /**
     * Parses Linux kernel makefile in order to find version info
     * @param str Portion of makefile containing version info
     * @param query String in makefile to use in order to parse the text
     * @return A substring of the makefile composed of a portion of the version info
     */
    private String parseText(String str, String query) {
        int occurrence = str.indexOf(query);
        String cut = str.substring(occurrence+query.length()+3);
        int newl = cut.indexOf("\n");
        return cut.substring(0,newl);
    }

    private class DownloadMakefile extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url_arr) {
            try {
                return downloadUrl(url_arr[0]);
            } catch (IOException e) {
                return "Cannot retrieve current version of Linux. An error may have occurred.";
            }
        }

        /**
         * Process to obtain makefile
         * @param url URL of Linux kernel makefile
         * @return String of text from makefile
         * @throws IOException
         */
        private String downloadUrl(String url) throws IOException {
            InputStream is = null;
            try {
                URL makefile = new URL(url);
                HttpURLConnection makefileConnection = (HttpURLConnection) makefile.openConnection();
                makefileConnection.setReadTimeout(10000);
                makefileConnection.setConnectTimeout(15000);
                makefileConnection.setRequestMethod("GET");
                makefileConnection.setDoInput(true);
                makefileConnection.connect();
                is = makefileConnection.getInputStream();
                return getText(is);
            } finally {
                if (is != null) is.close();
            }
        }

        /**
         * Continued process to obtain makefile
         * @param stream Object for the input stream which we will get text from
         * @return String with the text we want from the makefile
         * @throws IOException
         */
        public String getText(InputStream stream) throws IOException {
            Reader reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[100]; // Should be enough to get first few lines of Makefile
            reader.read(buffer);
            return new String(buffer);
        }

        @Override
        protected void onPostExecute(String result) {
            String version = parseText(result, "VERSION");
            String patchlevel = parseText(result, "PATCHLEVEL");
            String sublevel = parseText(result, "SUBLEVEL");
            String name = parseText(result, "NAME");
            textView.setTextSize(20);
            textView.setText(version + "." + patchlevel + "." + sublevel + " \"" + name + "\"");
        }
    }
}
