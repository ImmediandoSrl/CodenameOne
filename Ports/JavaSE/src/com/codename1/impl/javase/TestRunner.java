/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.testing.TestReporting;
import com.codename1.ui.Display;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;


/**
 * The test runner allows running Codename One unit tests on a specific Codename One application within the 
 * simulator. 
 *
 * @author Shai Almog
 */
public class TestRunner {
    private static final int VERSION = 1;
    private String mainClass;
    private String[] testCases;
    private String[] skins; 
    private boolean forceLandscape;
    private boolean forcePortrait;
    private boolean quietMode;
    private boolean cleanMode = true;
    private boolean stopOnFail;
    
    private TestRunner() {
    }
    
    private void printUsage() {
        System.out.println("Usage: TestRunner mainClass [-testCases testCase1,testCase2...] [-skins skin1,skin2...] [-quiet] [-cleanMode] [-junitXML]"
                + "[-stopOnFail]\n\n"
                + "mainClass - the main application class which is being tested, this is the full name for the lifecycle class.\n"
                + "testCases - optional set of test cases to run using the full package/class name syntax and comma delimited. If "
                + "ommitted all test cases will be executed.\n"
                + "skins - the skins on which the test cases should be executed. If ommitted the default simulator skin is used.\n"
                + "quietMode - when set the skin running the tests will not appear and the tests will be executed in the background\n"
                + "cleanMode - clean mode executes every test in complete isolation from the previous test restarting the Classloader"
                + "completely. Clean mode can't be used on the device so its only useful for debugging\n"
                + "stopOnFail - indicates that execution should stop the moment a failure occured\n"
                + "junitXML - output is written in junit XML format");
    }
    
    private void init(String[] argv) {
        try {
            if(argv[0].startsWith("-") || argv[0].startsWith("/")) {
                printUsage();
                return;
            }
            try {
                mainClass = argv[0];
                int pos = 1;
                while(pos < argv.length) {
                    String s = argv[pos];
                    if (s.equalsIgnoreCase("-stopOnFail")) {
                        pos++;
                        stopOnFail = true;
                        continue;
                    }
                    if(s.equalsIgnoreCase("-testCases")) {
                        pos++;
                        testCases = argv[pos].split(",");
                        pos++;
                        continue;
                    }
                                        
                    if(s.equalsIgnoreCase("-skins")) {
                        pos++;
                        skins = argv[pos].split(",");
                        pos++;
                        continue;
                    }

                    if(s.equalsIgnoreCase("-landscape")) {
                        forceLandscape = true;
                        pos++;
                        continue;
                    }
                    
                    if(s.equalsIgnoreCase("-portrait")) {
                        forcePortrait = true;
                        pos++;
                        continue;
                    }

                    if(s.equalsIgnoreCase("-quietMode")) {
                        quietMode = true;
                        pos++;
                        continue;
                    }

                    if(s.equalsIgnoreCase("-junitXML")) {
                        TestReporting.setInstance(new JUnitXMLReporting());
                        pos++;
                        continue;
                    }
                    
                    if(s.equalsIgnoreCase("-cleanMode")) {
                        cleanMode = true;
                        pos++;
                        continue;
                    }

                    System.out.println("Unrecognized argument: " + s);
                    printUsage();
                    System.exit(1);
                    return;
                }
            } catch(Exception err) {
                err.printStackTrace();
                printUsage();
                return;
            }
            
            String[] tests;
            if(testCases == null || testCases.length == 0) {
                InputStream is = getClass().getResourceAsStream("/tests.dat");
                if(is == null) {
                    System.err.println("Test data not found in the file, make sure the ant task was executed in full");
                    System.exit(2);
                    return;
                }
                DataInputStream di = new DataInputStream(is);
                int version = di.readInt();
                if(version > VERSION) {
                    System.err.println("Tests were built with a new version of Codename One and can't be executed with this runner");
                    System.exit(4);
                    return;
                }

                tests = new String[di.readInt()];
                for(int iter = 0 ; iter < tests.length ; iter++) {
                    tests[iter] = di.readUTF();
                }
                di.close();
            } else {
                tests = testCases;
            }

            
            if(forceLandscape) {
                System.out.println("Forcing landscape");
                Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                pref.putBoolean("Portrait", false);
                pref.sync();
            } else {
                if(forcePortrait) {
                    System.out.println("Forcing portrait");
                    Preferences pref = Preferences.userNodeForPackage(JavaSEPort.class);
                    pref.putBoolean("Portrait", true);
                    pref.sync();
                }
            }
            
            System.out.println("Preparing to execute " + tests.length + " tests");
            // Required to provide implementations of Util for static functions in Log using the current classloader
            Display.init(new java.awt.Container());

            String classPathStr = System.getProperty("java.class.path");
            if (System.getProperty("cn1.class.path") != null) {
                classPathStr += File.pathSeparator + System.getProperty("cn1.class.path");
            }

            StringTokenizer t = new StringTokenizer(classPathStr, File.pathSeparator);
            File[] files = new File[t.countTokens()];
            for (int iter = 0; iter < files.length; iter++) {
                files[iter] = new File(t.nextToken());
            }
            int passedTests = 0;
            int failedTests = 0;

            if(cleanMode) {
                for(String currentTestClass : tests) {
                    ClassLoader ldr = createClassLoader(files);
                    Class<?> c = Class.forName("com.codename1.impl.javase.TestExecuter", true, ldr);
                    Method m = c.getDeclaredMethod("runTest", String.class, String.class, Boolean.TYPE);
                    Boolean passed = (Boolean)m.invoke(null, mainClass, currentTestClass, quietMode);
                    if(passed) {
                        passedTests++;
                    } else {
                        failedTests++;
                        if(stopOnFail) {
                            System.exit(100);
                            return;
                        }
                    }
                }
            } else {
                ClassLoader ldr = createClassLoader(files);
                Class<?> c = Class.forName("com.codename1.impl.javase.TestExecuter", true, ldr);
                for(String currentTestClass : tests) {
                    Method m = c.getDeclaredMethod("runTest", String.class, String.class, Boolean.TYPE);
                    Boolean passed = (Boolean)m.invoke(null, mainClass, currentTestClass, quietMode);
                    if(passed) {
                        System.out.println(currentTestClass + " passed!");
                        passedTests++;
                    } else {
                        System.out.println(currentTestClass + " failed!");
                        failedTests++;
                        if(stopOnFail) {
                            System.exit(100);
                            return;
                        }
                    }
                }
            }
            TestReporting.getInstance().testExecutionFinished(mainClass);
            int exitCode = 0;
            if(failedTests > 0) {
                System.out.println("Test execution finished, some failed tests occurred. Passed: " + passedTests + " tests. Failed: " + failedTests + " tests.");
                exitCode = 100;
            } else {
                System.out.println("All tests passed. Total " + passedTests + " tests passed");
            }
            System.exit(exitCode);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(3);
        }
    }

    private ClassLoader createClassLoader(File[] files) {
        ClassPathLoader ldr = new ClassPathLoader(files);
        ldr.addExclude("com.codename1.testing.TestReporting");
        return ldr;
    }
    
 
    /**
     * The main method accepts several arguments of which only the main class is a requirement
     * @param argv
     */
    public static void main(String[] argv) throws Exception {
        if (CN1Bootstrap.run(TestRunner.class, argv)) {
            return;
        }
        new TestRunner().init(argv);
    }
}
