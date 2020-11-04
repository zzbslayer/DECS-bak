package com.example.edgenode.util;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Utils {
    /**
     * 从请求中获取ip地址
     *
     * @param request 请求
     * @return
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        if (ip.split(",").length > 1) {
            ip = ip.split(",")[0];
        }
        return ip;
    }


    /**
     * 获取本机的的剩余空间
     * @return
     * @throws Exception
     */
    public static double getAvailRation() throws Exception {
        double totalhd = 0;
        double usedhd = 0;
        double avail = 0;
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("df -hl /");//df -hl 查看硬盘空间
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String str = null;
            String[] strArray = null;
            while ((str = in.readLine()) != null) {
                int m = 0;
                strArray = str.split(" ");
                for (String tmp : strArray) {
                    if (tmp.trim().length() == 0)
                        continue;
                    ++m;
                    if (tmp.indexOf("G") != -1) {
                        if (m == 2) {
                            if (!tmp.equals("") && !tmp.equals("0"))
                                totalhd += Double.parseDouble(tmp.substring(0, tmp.length() - 2)) * 1024;
                        }
                        if (m == 3) {
                            if (!tmp.equals("none") && !tmp.equals("0"))
                                usedhd += Double.parseDouble(tmp.substring(0, tmp.length() - 2)) * 1024;
                        }
                        if (m == 4) {
                            if (!tmp.equals("") && !tmp.equals("0"))
                                avail += Double.parseDouble(tmp.substring(0, tmp.length() - 2)) * 1024;
                        }
                    }
                    if (tmp.indexOf("M") != -1) {
                        if (m == 2) {
                            if (!tmp.equals("") && !tmp.equals("0"))
                                totalhd += Double.parseDouble(tmp.substring(0, tmp.length() - 1));
                        }
                        if (m == 3) {
                            if (!tmp.equals("none") && !tmp.equals("0"))
                                usedhd += Double.parseDouble(tmp.substring(0, tmp.length() - 1));
                        }
                        if (m == 4) {
                            if (!tmp.equals("none") && !tmp.equals("0"))
                                avail += Double.parseDouble(tmp.substring(0, tmp.length() - 1));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            in.close();
        }
        return avail / totalhd;
    }
}
