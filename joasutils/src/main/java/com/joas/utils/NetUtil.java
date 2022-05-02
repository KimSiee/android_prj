/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 24 오후 4:18
 *
 */

package com.joas.utils;

import android.net.LinkAddress;
import android.net.RouteInfo;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import org.apache.commons.net.util.SubnetUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class NetUtil {

    protected static final String ETHERNET_SET_FILE = "/data/misc/ethernet/ipconfig.txt";

    /* IP and proxy configuration keys */
    protected static final String ID_KEY = "id";
    protected static final String IP_ASSIGNMENT_KEY = "ipAssignment";
    protected static final String LINK_ADDRESS_KEY = "linkAddress";
    protected static final String GATEWAY_KEY = "gateway";
    protected static final String DNS_KEY = "dns";
    protected static final String PROXY_SETTINGS_KEY = "proxySettings";
    protected static final String PROXY_HOST_KEY = "proxyHost";
    protected static final String PROXY_PORT_KEY = "proxyPort";
    protected static final String PROXY_PAC_FILE = "proxyPac";
    protected static final String EXCLUSION_LIST_KEY = "exclusionList";
    protected static final String EOS = "eos";
    protected static final int IPCONFIG_FILE_VERSION = 2;

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    private static String getPropInfo(String interfaceName) {
        String re = "";
        try {
            final Process ps = Runtime.getRuntime().exec("/system/bin/getprop "+interfaceName);
            final InputStream is = ps.getInputStream();
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            re = br.readLine();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return re;
    }

    public static boolean isUseDHCP() {
        String prop = getPropInfo("init.svc.dhcpcd_eth0");
        return prop.equals("running");
    }

    public static String getDHCPIpAddr() {
        return getPropInfo("dhcp.eth0.ipaddress");
    }

    public static String getDHCPGateway() {
        return getPropInfo("dhcp.eth0.gateway");
    }

    public static String getDHCPNetmask() {
        return getPropInfo("dhcp.eth0.mask");
    }

    public static String getDHCPDNS() {
        return getPropInfo("dhcp.eth0.dns1");
    }

    public static String getLocalIpAddress() {
        Enumeration<NetworkInterface> nwis;
        try {
            nwis = NetworkInterface.getNetworkInterfaces();
            while (nwis.hasMoreElements()) {

                NetworkInterface ni = nwis.nextElement();
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    int npf = ia.getNetworkPrefixLength();
                    InetAddress address = ia.getAddress();
                    InetAddress broadcast = ia.getBroadcast();

                    Log.i("Local IP", String.format("%s: %s/%d",
                            ni.getDisplayName(), ia.getAddress(), ia.getNetworkPrefixLength()));

                    if (broadcast == null && npf != 8) {
                        //IPV6
                    }
                    else {
                        //IPV4
                        if ( ni.getDisplayName().equals("eth0")) return ia.getAddress().toString().substring(1);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getEthernetMacAddress() {
        String macAddress = "Unknown";
        try {
            List<NetworkInterface> allNetworkInterfaces = Collections.list(NetworkInterface
                    .getNetworkInterfaces());
            for (NetworkInterface nif : allNetworkInterfaces) {
                if (!nif.getName().equalsIgnoreCase("eth0"))
                    continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return macAddress;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                macAddress = res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return macAddress;
    }

    public static boolean pingTest(String ip) {
        Runtime runtime = Runtime.getRuntime();
        try
        {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 "+ip);
            int mExitValue = mIpAddrProcess.waitFor();

            if(mExitValue==0){
                return true;
            }else{
                return false;
            }
        }
        catch (Exception e)
        {
            LogWrapper.e("Ping", " Exception:"+e);
        }
        return false;
    }

    /**
     * Convert a TCP/IP address string into a byte array
     *
     * @param addr String
     * @return byte[]
     */
    public final static byte[] getBytesFromStrIPAddr(String addr) {

        // Convert the TCP/IP address string to an integer value

        int ipInt = parseNumericIPAddress(addr);
        if ( ipInt == 0)
            return null;

        // Convert to bytes

        byte[] ipByts = new byte[4];

        ipByts[3] = (byte) (ipInt & 0xFF);
        ipByts[2] = (byte) ((ipInt >> 8) & 0xFF);
        ipByts[1] = (byte) ((ipInt >> 16) & 0xFF);
        ipByts[0] = (byte) ((ipInt >> 24) & 0xFF);

        // Return the TCP/IP bytes

        return ipByts;
    }
    /**
     * Check if the specified address is a valid numeric TCP/IP address and return as an integer value
     *
     * @param ipaddr String
     * @return int
     */
    public final static int parseNumericIPAddress(String ipaddr) {

        //  Check if the string is valid

        if ( ipaddr == null || ipaddr.length() < 7 || ipaddr.length() > 15)
            return 0;

        //  Check the address string, should be n.n.n.n format

        StringTokenizer token = new StringTokenizer(ipaddr,".");
        if ( token.countTokens() != 4)
            return 0;

        int ipInt = 0;

        while ( token.hasMoreTokens()) {

            //  Get the current token and convert to an integer value

            String ipNum = token.nextToken();

            try {

                //  Validate the current address part

                int ipVal = Integer.valueOf(ipNum).intValue();
                if ( ipVal < 0 || ipVal > 255)
                    return 0;

                //  Add to the integer address

                ipInt = (ipInt << 8) + ipVal;
            }
            catch (NumberFormatException ex) {
                return 0;
            }
        }

        //  Return the integer address

        return ipInt;
    }

    public static ArrayMap<String, String> readIpConfigurations() {
        BufferedInputStream bufferedInputStream;
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(ETHERNET_SET_FILE));
        } catch (FileNotFoundException e) {
            // Return an empty array here because callers expect an empty array when the file is
            // not present.
            Log.e("readIpConfig","Error opening configuration file: " + e);
            return new ArrayMap<>(0);
        }
        return readIpConfigurations(bufferedInputStream);
    }

    /** Returns a map of network identity token and {@link IpConfiguration}. */
    public static ArrayMap<String, String> readIpConfigurations(
            InputStream inputStream) {
        ArrayMap<String, String> networks = new ArrayMap<>();
        DataInputStream in = null;
        try {
            in = new DataInputStream(inputStream);
            int version = in.readInt();
            if (version != 3 && version != 2 && version != 1) {
                Log.e("readIPConfig","Bad version on IP configuration file, ignore read");
                return null;
            }
            while (true) {
                String uniqueToken = null;
                // Default is DHCP with no proxy
                IpConfiguration.IpAssignment ipAssignment = IpConfiguration.IpAssignment.DHCP;
                StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
                String key;
                do {
                    key = in.readUTF();
                    try {
                        if (key.equals(ID_KEY)) {
                            if (version < 3) {
                                int id = in.readInt();
                                uniqueToken = String.valueOf(id);
                            } else {
                                uniqueToken = in.readUTF();
                            }
                        } else if (key.equals(IP_ASSIGNMENT_KEY)) {
                            ipAssignment = IpConfiguration.IpAssignment.valueOf(in.readUTF());
                            networks.put("type", ipAssignment.toString());

                        }  else if (key.equals(LINK_ADDRESS_KEY)) {
                            String strLinkAddr = in.readUTF();
                            int linkInt = in.readInt();
                            Log.d("readIPConfig","Link addr: " + strLinkAddr + ", int:"+linkInt);

                            networks.put("ipaddress", strLinkAddr);
                            int bits = 32 - linkInt;
                            final int mask = 0xFFFFFFFF - ((1 << bits)-1);

                            String strMask = Integer.toString(mask >> 24 & 0xFF, 10) + "." +
                                    Integer.toString(mask >> 16 & 0xFF, 10) + "." +
                                    Integer.toString(mask >>  8 & 0xFF, 10) + "." +
                                    Integer.toString(mask >>  0 & 0xFF, 10);

                            networks.put("netmask", strMask);
                        }
                        else if (key.equals(GATEWAY_KEY)) {
                            LinkAddress dest = null;
                            InetAddress gateway = null;
                            if (version == 1) {
                                String strGateway = in.readUTF();
                                networks.put("gateway", strGateway);
                            } else {
                                if (in.readInt() == 1) {
                                    String strGateway = in.readUTF();
                                    in.readInt();
                                    networks.put("gateway", strGateway);
                                }
                                if (in.readInt() == 1) {
                                    String strGateway = in.readUTF();
                                    networks.put("gateway", strGateway);
                                }
                            }
                        } else if (key.equals(DNS_KEY)) {
                            String strDNS = in.readUTF();
                            networks.put("dns", strDNS);
                        } else if (key.equals(PROXY_SETTINGS_KEY)) {
                            in.readUTF();
                        } else if (key.equals(PROXY_HOST_KEY)) {
                            in.readUTF();
                        } else if (key.equals(PROXY_PORT_KEY)) {
                            in.readUTF();
                        } else if (key.equals(PROXY_PAC_FILE)) {
                            in.readUTF();
                        } else if (key.equals(EXCLUSION_LIST_KEY)) {
                            in.readUTF();
                        } else if (key.equals(EOS)) {
                            break;
                        } else {
                            Log.e("readIPConfig","Ignore unknown key " + key + "while reading");
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e("readIPConfig","Ignore invalid address while reading" + e);
                    }
                } while (true);
            }
        } catch (EOFException ignore) {
        } catch (IOException e) {
            Log.e("readIPConfig","Error parsing configuration: " + e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {}
            }
        }
        return networks;
    }
    

    /*
    public static void configurationStaticIP() {
        FileOutputStream f = null;
        DataOutputStream out = null;
        try {
            f = new FileOutputStream("/data/misc/ethernet/ipconfig.txt");
            out = new DataOutputStream(f);
        }catch (Exception e) {
            LogWrapper.e("IPset", "Err file Open:"+e.toString());
        }

        NetUtil.IpConfiguration config = new NetUtil.IpConfiguration();
        config.ipAssignment = NetUtil.IpConfiguration.IpAssignment.STATIC;
        config.proxySettings = NetUtil.IpConfiguration.ProxySettings.NONE;

        NetUtil.StaticIpConfiguration staticIpConfig;
        staticIpConfig = new NetUtil.StaticIpConfiguration();

        try {
            Constructor<?> linkAddressConstructor = null;
            linkAddressConstructor = LinkAddress.class.getDeclaredConstructor(String.class);

            staticIpConfig.ipAddress = (LinkAddress)linkAddressConstructor.newInstance("192.168.0.235/24");
            byte[] gwAddr = new byte[]{(byte)192, (byte)168, (byte)0, (byte)1};
            staticIpConfig.gateway = InetAddress.getByAddress(gwAddr);
            byte[] dnsAddr = new byte[]{(byte)8,(byte)8,(byte)8,(byte)8};
            staticIpConfig.dnsServers.add(InetAddress.getByAddress(dnsAddr));

            config.staticIpConfiguration = staticIpConfig;
            NetUtil.writeIPConfig(out, config);

            out.close();
            f.close();
            File targetFile = new File("/data/misc/ethernet/ipconfig.txt");
            targetFile.setReadable(true, false);
            targetFile.setWritable(true, false);
            targetFile.setExecutable(true, false);

        }catch (Exception e) {
            LogWrapper.e("IPset", "Err:"+e.toString());
        }
    }
    */

    public static void configurationDHCP(){
        FileOutputStream f = null;
        DataOutputStream out = null;
        try {
            f = new FileOutputStream(ETHERNET_SET_FILE);
            out = new DataOutputStream(f);
        }catch (Exception e) {
            LogWrapper.e("IPset", "Err file Open:"+e.toString());
        }

        NetUtil.IpConfiguration config = new NetUtil.IpConfiguration();
        config.ipAssignment = IpConfiguration.IpAssignment.DHCP;
        config.proxySettings = NetUtil.IpConfiguration.ProxySettings.NONE;

        try {
            NetUtil.writeIPConfig(out, config);
            out.close();
            f.close();
            File targetFile = new File(ETHERNET_SET_FILE);
            targetFile.setReadable(true, false);
            targetFile.setWritable(true, false);
            targetFile.setExecutable(true, false);

        }catch (Exception e) {
            LogWrapper.e("IPset", "Err:"+e.toString());
        }

    }

    public static void configurationStaticIP(String ipaddr, String netmask, String gateway, String dns) {
        FileOutputStream f = null;
        DataOutputStream out = null;
        try {
            f = new FileOutputStream(ETHERNET_SET_FILE);
            out = new DataOutputStream(f);
        }catch (Exception e) {
            LogWrapper.e("IPset", "Err file Open:"+e.toString());
        }

        NetUtil.IpConfiguration config = new NetUtil.IpConfiguration();
        config.ipAssignment = NetUtil.IpConfiguration.IpAssignment.STATIC;
        config.proxySettings = NetUtil.IpConfiguration.ProxySettings.NONE;

        NetUtil.StaticIpConfiguration staticIpConfig;
        staticIpConfig = new NetUtil.StaticIpConfiguration();

        try {
            Constructor<?> linkAddressConstructor = null;
            linkAddressConstructor = LinkAddress.class.getDeclaredConstructor(String.class);

            SubnetUtils utils = new SubnetUtils(ipaddr, netmask);

            staticIpConfig.ipAddress = (LinkAddress)linkAddressConstructor.newInstance(utils.getInfo().getCidrSignature());

            staticIpConfig.gateway = InetAddress.getByName(gateway);
            staticIpConfig.dnsServers.add(InetAddress.getByName(dns));

            config.staticIpConfiguration = staticIpConfig;
            NetUtil.writeIPConfig(out, config);

            out.close();
            f.close();
            File targetFile = new File(ETHERNET_SET_FILE);
            targetFile.setReadable(true, false);
            targetFile.setWritable(true, false);
            targetFile.setExecutable(true, false);

        }catch (Exception e) {
            LogWrapper.e("IPset", "Err:"+e.toString());
        }
    }
    public static boolean writeIPConfig(DataOutputStream out, IpConfiguration config) throws IOException {
        return writeIPConfig(out, "0", config, IPCONFIG_FILE_VERSION);
    }


    public static boolean writeIPConfig(DataOutputStream out, String configKey,
                                      IpConfiguration config, int version) throws IOException {
        boolean written = false;
        out.writeInt(IPCONFIG_FILE_VERSION);
        try {
            switch (config.ipAssignment) {
                case STATIC:
                    out.writeUTF(IP_ASSIGNMENT_KEY);
                    out.writeUTF(config.ipAssignment.toString());
                    StaticIpConfiguration staticIpConfiguration = config.staticIpConfiguration;
                    if (staticIpConfiguration != null) {
                        if (staticIpConfiguration.ipAddress != null) {
                            LinkAddress ipAddress = staticIpConfiguration.ipAddress;
                            out.writeUTF(LINK_ADDRESS_KEY);
                            out.writeUTF(ipAddress.getAddress().getHostAddress());
                            out.writeInt(ipAddress.getPrefixLength());
                        }
                        if (staticIpConfiguration.gateway != null) {
                            out.writeUTF(GATEWAY_KEY);
                            out.writeInt(0);  // Default route.
                            out.writeInt(1);  // Have a gateway.
                            out.writeUTF(staticIpConfiguration.gateway.getHostAddress());
                        }
                        for (InetAddress inetAddr : staticIpConfiguration.dnsServers) {
                            out.writeUTF(DNS_KEY);
                            out.writeUTF(inetAddr.getHostAddress());
                        }
                    }
                    written = true;
                    break;
                case DHCP:
                    out.writeUTF(IP_ASSIGNMENT_KEY);
                    out.writeUTF(config.ipAssignment.toString());
                    written = true;
                    break;
                case UNASSIGNED:
                    /* Ignore */
                    break;
                default:
                    Log.e("NetUtil", "Ignore invalid ip assignment while writing");
                    break;
            }
            switch (config.proxySettings) {
                case STATIC:
                    ProxyInfo proxyProperties = config.httpProxy;
                    String exclusionList = proxyProperties.getExclusionListAsString();
                    out.writeUTF(PROXY_SETTINGS_KEY);
                    out.writeUTF(config.proxySettings.toString());
                    out.writeUTF(PROXY_HOST_KEY);
                    out.writeUTF(proxyProperties.getHost());
                    out.writeUTF(PROXY_PORT_KEY);
                    out.writeInt(proxyProperties.getPort());
                    if (exclusionList != null) {
                        out.writeUTF(EXCLUSION_LIST_KEY);
                        out.writeUTF(exclusionList);
                    }
                    written = true;
                    break;
                case PAC:
                    ProxyInfo proxyPacProperties = config.httpProxy;
                    out.writeUTF(PROXY_SETTINGS_KEY);
                    out.writeUTF(config.proxySettings.toString());
                    out.writeUTF(PROXY_PAC_FILE);
                    out.writeUTF(proxyPacProperties.getPacFileUrl().toString());
                    written = true;
                    break;
                case NONE:
                    out.writeUTF(PROXY_SETTINGS_KEY);
                    out.writeUTF(config.proxySettings.toString());
                    written = true;
                    break;
                case UNASSIGNED:
                    /* Ignore */
                    break;
                default:
                    Log.e("NetUtil","Ignore invalid proxy settings while writing");
                    break;
            }
            if (written) {
                out.writeUTF(ID_KEY);
                if (version < 3) {
                    out.writeInt(Integer.valueOf(configKey));
                } else {
                    out.writeUTF(configKey);
                }
            }
        } catch (NullPointerException e) {
            Log.e("NetUtil", "Failure in writing " + config + e);
        }
        out.writeUTF(EOS);
        return written;
    }

    public static class IpConfiguration {
        private static final String TAG = "IpConfiguration";

        public enum IpAssignment {
            /* Use statically configured IP settings. Configuration can be accessed
             * with staticIpConfiguration */
            STATIC,
            /* Use dynamically configured IP settings */
            DHCP,
            /* no IP details are assigned, this is used to indicate
             * that any existing IP settings should be retained */
            UNASSIGNED
        }

        public IpAssignment ipAssignment;
        public StaticIpConfiguration staticIpConfiguration;

        public enum ProxySettings {
            /* No proxy is to be used. Any existing proxy settings
             * should be cleared. */
            NONE,
            /* Use statically configured proxy. Configuration can be accessed
             * with httpProxy. */
            STATIC,
            /* no proxy details are assigned, this is used to indicate
             * that any existing proxy settings should be retained */
            UNASSIGNED,
            /* Use a Pac based proxy.
             */
            PAC
        }
        public ProxySettings proxySettings;
        public ProxyInfo httpProxy;
    }


    public static class StaticIpConfiguration {
        public LinkAddress ipAddress = null;
        public InetAddress gateway = null;
        public final ArrayList<InetAddress> dnsServers = new ArrayList<>();
        public String domains = "";

        public StaticIpConfiguration() {}
    }

    public static class ProxyInfo {
        private final String mHost = "";
        private final int mPort = 9000;
        private final String mExclusionList = "";
        private final String[] mParsedExclusionList = null;
        private final Uri mPacFileUrl = null;
        /**
         * @hide
         */
        public static final String LOCAL_EXCL_LIST = "";
        /**
         * @hide
         */
        public static final int LOCAL_PORT = -1;
        /**
         * @hide
         */
        public static final String LOCAL_HOST = "localhost";

        /**
         * Returns the URL of the current PAC script or null if there is
         * no PAC script.
         */
        public Uri getPacFileUrl() {
            return mPacFileUrl;
        }
        /**
         * When configured to use a Direct Proxy this returns the host
         * of the proxy.
         */
        public String getHost() {
            return mHost;
        }
        /**
         * When configured to use a Direct Proxy this returns the port
         * of the proxy
         */
        public int getPort() {
            return mPort;
        }
        /**
         * When configured to use a Direct Proxy this returns the list
         * of hosts for which the proxy is ignored.
         */
        public String[] getExclusionList() {
            return mParsedExclusionList;
        }
        /**
         * comma separated
         * @hide
         */
        public String getExclusionListAsString() {
            return mExclusionList;
        }

    }

}


