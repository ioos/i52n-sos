package org.n52.sos.ioos.validation;

public class JettyHelper {
    public static String getJettyUrl(){
        return "http://localhost:" + getPort() + "/sos";
    }

    /**
     * Get test port (use "jetty.port" or "port" system properties or default)
     * 
     * @return test port
     */
    protected static int getPort() {
        if (System.getProperty("jetty.port") != null) {
            return Integer.valueOf(System.getProperty("jetty.port").trim());
        } else if (System.getProperty("port") != null) {
            return Integer.valueOf(System.getProperty("port").trim());
        }
        //default port
        return 9090;
    }
}
