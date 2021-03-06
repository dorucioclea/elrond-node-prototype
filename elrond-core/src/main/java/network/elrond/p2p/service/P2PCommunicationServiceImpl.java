package network.elrond.p2p.service;


import network.elrond.core.Util;
import network.elrond.p2p.model.PingResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class P2PCommunicationServiceImpl implements P2PCommunicationService {
    private static final Logger logger = LogManager.getLogger(P2PCommunicationServiceImpl.class);

    private final int pingConnectionTimeOut = 2000;
    private final int portConnectionTimeOut = 1000;

    @Override
	public PingResponse getPingResponse(String address, int port, boolean throwOnPortClosed) throws Exception {
        logger.traceEntry("params: {} {}", address, port);

        Util.check(address != null, "Address is null!");
        Util.check(port > 0, "Port is not valid!");
        Util.check(port < 65535, "Port is not valid!");

        long timeStampStart = System.currentTimeMillis();
        long timeStampEnd = 0;
        long timeStampStartPing = System.currentTimeMillis();

        PingResponse pingResponse = new PingResponse();

        String[] addr = address.split("\\.");
        if (addr.length != 4) {
            throw new IllegalArgumentException("Address is not valid!");
        }

        for (int i = 0; i < 4; i++) {
            int val = Integer.decode(addr[i]);

            if ((val < 0) || (val > 254)) {
                throw new IllegalArgumentException("Address is not valid!");
            }
        }

        //step 1. plain ping
        if (!ping(address)){
            timeStampEnd = System.currentTimeMillis();
            logger.debug("Ping timeout! Took {} ms", timeStampEnd - timeStampStart);

            throw new Exception("Ping timeout!");
        }
        timeStampEnd = System.currentTimeMillis();

        pingResponse.setResponseTimeMs(timeStampEnd - timeStampStartPing);
        pingResponse.setReachablePing(true);

        //step 2. try to open socket on port
        pingResponse.setReachablePort(isPortReachable(address, port, portConnectionTimeOut));

        boolean throwException = !pingResponse.isReachablePort() && throwOnPortClosed;

        if (throwException){
            throw new Exception(String.format("Unreachable port %d", port));
        }

        timeStampEnd = System.currentTimeMillis();
        logger.trace("took {} ms", timeStampEnd - timeStampStart);
        return logger.traceExit(pingResponse);
    }

    @Override
	public boolean isPortReachable(String address, int port, int timeoutPeriod){
        Util.check(address != null, "Address is null!");
        Util.check(port > 0, "Port is not valid!");
        Util.check(port < 65535, "Port is not valid!");
        Util.check(timeoutPeriod > 0, "Timeout is not valid!");

        try{
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), timeoutPeriod);
            OutputStream out = socket.getOutputStream();

            out.close();
            socket.close();
            return(true);
        } catch (Exception ex){
            logger.catching(ex);
        }

        return(false);
    }

    private boolean ping(String host) throws Exception {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        ProcessBuilder processBuilder = new ProcessBuilder("ping", isWindows? "-n" : "-c", "1", host);
        Process proc = processBuilder.start();

        boolean result = proc.waitFor(pingConnectionTimeOut, TimeUnit.MILLISECONDS);

        if (!result){
            return false;
        }

        return proc.exitValue() == 0;
    }
}
