package network.elrond.api;

import net.tomp2p.peers.PeerAddress;
import network.elrond.Application;
import network.elrond.ContextCreator;
import network.elrond.account.AccountAddress;
import network.elrond.api.manager.ElrondWebSocketManager;
import network.elrond.application.AppContext;
import network.elrond.core.ResponseObject;
import network.elrond.core.Util;
import network.elrond.data.BootstrapType;
import network.elrond.service.AppServiceProvider;
import network.elrond.sharding.ShardingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;

@Controller
public class ElrondNodeController {
    private static final Logger logger = LogManager.getLogger(ElrondNodeController.class);

    @Autowired
    ElrondApiNode elrondApiNode;

    @Autowired
    ElrondWebSocketManager elrondWebSocketManager;

    @RequestMapping(path = "/node/stop", method = RequestMethod.GET)
    public @ResponseBody
    boolean stopNode(HttpServletResponse response) {
        logger.traceEntry();
        Application application = elrondApiNode.getApplication();
        if (application != null) {
            logger.trace("application is null");
            application.stop();
        }

        return logger.traceExit(true);
    }


    @RequestMapping(path = "/node/appstatus", method = RequestMethod.GET)
    public @ResponseBody
    boolean nodeAppStatus(
            HttpServletResponse response) {
        return true;
    }


    @RequestMapping(path = "/node/status", method = RequestMethod.GET)
    public @ResponseBody
    boolean nodeStatus(HttpServletResponse response) {
        logger.traceEntry();
        Application application = elrondApiNode.getApplication();

        return logger.traceExit(application != null && application.getState().isStillRunning());
    }


    @RequestMapping(path = "/node/start", method = RequestMethod.GET)
    public @ResponseBody
    boolean startNode(
            HttpServletResponse response,
            @RequestParam(defaultValue = "elrond-node-1") String nodeName,
            @RequestParam(defaultValue = "4001") Integer port,
            @RequestParam(defaultValue = "4000", required = false) Integer masterPeerPort,
            @RequestParam(defaultValue = "127.0.0.1", required = false) String masterPeerIpAddress,
            @RequestParam(defaultValue = "00e15fc71adc4832c56c4e6a8b50a9503a4ede9485c4efbc585def0c657d93066a", required = true) String privateKey,
            @RequestParam(defaultValue = "21000000", required = false) String mintValue,
            @RequestParam(defaultValue = "START_FROM_SCRATCH", required = true) BootstrapType bootstrapType//,
            //@RequestParam(defaultValue = "elrond-node-1", required = false) String blockchainPath,
            //@RequestParam(defaultValue = "elrond-node-1", required = false) String blockchainRestorePath

    ) throws IOException {
        logger.traceEntry("params: {} {} {} {} {} {} {}", nodeName, port, masterPeerPort, masterPeerIpAddress,
                privateKey, mintValue, bootstrapType);
        //Reuploaded
        AppContext context = ContextCreator.createAppContext(nodeName, privateKey, masterPeerIpAddress,
                masterPeerPort, port, bootstrapType, nodeName);

        logger.info("Node name: {}", nodeName);

        return logger.traceExit(elrondApiNode.start(context, nodeName, nodeName));
    }


    @RequestMapping(path = "/node/send", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject send(
            HttpServletResponse response,
            @RequestParam  String address,
            @RequestParam(defaultValue = "1") BigInteger value) {
        logger.traceEntry("params: {} {}", address, value);

        try {
            AccountAddress _add = AccountAddress.fromHexString(address);
            return logger.traceExit(elrondApiNode.send(_add, value));
        } catch (Exception ex){
            logger.catching(ex);
            return logger.traceExit(new ResponseObject(false, ex.getMessage(), null));
        }
    }


    @RequestMapping(path = "/node/receipt", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject getReceipt(
            HttpServletResponse response,
            @RequestParam() String transactionHash) {

        logger.traceEntry("params: {}", transactionHash);
        return logger.traceExit(elrondApiNode.getReceipt(transactionHash));

    }


    @RequestMapping(path = "/node/balance", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject getBalance(
            HttpServletResponse response,
            @RequestParam() String address) {

        logger.traceEntry("params: {}", address);
        try {
            AccountAddress _add = AccountAddress.fromHexString(address);
            return logger.traceExit(elrondApiNode.getBalance(_add));
        } catch (Exception ex){
            logger.catching(ex);
            return logger.traceExit(new ResponseObject(false, ex.getMessage(), null));
        }
    }

    @RequestMapping(path = "/node/sendMultipleTransactions", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject sendMultipleTransactions(
            HttpServletResponse response,
            @RequestParam String address,
            @RequestParam(defaultValue = "1") BigInteger value,
            @RequestParam(defaultValue = "1") Integer nrTransactions) {
        logger.traceEntry("params: {} {} {}", address, value, nrTransactions);

        try {
            AccountAddress _add = AccountAddress.fromHexString(address);
            return logger.traceExit(elrondApiNode.sendMultipleTransactions(_add, value, nrTransactions));
        } catch (Exception ex){
            logger.catching(ex);
            return logger.traceExit(new ResponseObject(false, ex.getMessage(), null));
        }
    }

    @RequestMapping(path = "/node/sendMultipleTransactionsToAllShards", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject sendMultipleTransactionsToAllShards(
            HttpServletResponse response,
            @RequestParam(defaultValue = "1") BigInteger value,
            @RequestParam(defaultValue = "1") Integer nrTransactions) {
        logger.traceEntry("params: {} {}", value, nrTransactions);

        try {
            return logger.traceExit(elrondApiNode.sendMultipleTransactionsToAllShards(value, nrTransactions));
        } catch (Exception ex){
            logger.catching(ex);
            return logger.traceExit(new ResponseObject(false, ex.getMessage(), ex.getMessage()));
        }
    }

    @RequestMapping(path = "/node/getStats", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject getStats(
            HttpServletResponse response) {
        return logger.traceExit(elrondApiNode.getBenchmarkResult());

    }

    @RequestMapping(path = "/node/ping", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject ping(
            HttpServletResponse response,
            @RequestParam() String ipAddress,
            @RequestParam() int port
    ) {
        logger.traceEntry("params: {} {}", ipAddress, port);
        return logger.traceExit(elrondApiNode.ping(ipAddress, port));
    }

    @RequestMapping(path = "/node/checkfreeport", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject checkFreePort(
            HttpServletResponse response,
            @RequestParam() String ipAddress,
            @RequestParam() int port
    ) {
        logger.traceEntry("params: {} {}", ipAddress, port);
        return logger.traceExit(elrondApiNode.checkFreePort(ipAddress, port));
    }

    @RequestMapping(path = "/node/generatepublickeyandprivateKey", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject generatePublicKeyAndPrivateKey(
            HttpServletResponse response,
            @RequestParam() String privateKey) {
        logger.traceEntry("params: {}", privateKey);
        return logger.traceExit(elrondApiNode.generatePublicKeyAndPrivateKey(privateKey));
    }

    @RequestMapping(path = "/node/shardofaddress", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject shardOfAddress(
            HttpServletResponse response,
            @RequestParam() String address) {

        logger.traceEntry("params: {}", address);

        try {

            ShardingService shardingService = AppServiceProvider.getShardingService();
            byte[] publicKeyBytes = Util.hexStringToByteArray(address);
            return logger.traceExit(new ResponseObject(true, "", shardingService.getShard(publicKeyBytes).getIndex()));
        } catch (Exception ex){
            logger.throwing(ex);
            return logger.traceExit(new ResponseObject(false, ex.getMessage(), null));
        }
    }

    @RequestMapping(path = "/node/gettransactionfromhash", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject getTransactionFromHash(
            HttpServletResponse response,
            @RequestParam() String transactionHash) {

        logger.traceEntry("params: {}", transactionHash);
        return logger.traceExit(elrondApiNode.getTransactionFromHash(transactionHash));
    }

    @RequestMapping(path = "/node/getblockfromhash", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject getBlockFromHash(
            HttpServletResponse response,
            @RequestParam() String blockHash) {

        logger.traceEntry("params: {}", blockHash);
        return logger.traceExit(elrondApiNode.getBlockFromHash(blockHash));
    }

    @RequestMapping(path = "/node/getNextPrivateKey", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject getNextPrivateKey(HttpServletRequest request, HttpServletResponse response) {
        logger.traceEntry();
        return logger.traceExit(elrondApiNode.getNextPrivateKey(request.getRemoteAddr()));
    }

    @RequestMapping(path = "/node/getprivatepublickeyshard", method = RequestMethod.GET)
    public @ResponseBody
    ResponseObject getPrivatePublicKeyShard(
            HttpServletResponse response) {

        logger.traceEntry();
        return logger.traceExit(elrondApiNode.getPrivatePublicKeyShard());
    }

    @RequestMapping(path = "/node/exit", method = RequestMethod.GET)
    public @ResponseBody
    void nodeExit(
            HttpServletResponse response) {
        logger.traceEntry();
        System.exit(0);
        logger.traceExit();
    }

    @RequestMapping(path = {"/node/getNodeLogs", "/node/getNodeLogs/{shard}"}, method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<Resource> getNodeLogs(@PathVariable Optional<Integer> shard) throws IOException {
        logger.traceEntry();

        // Get log file path
        org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) logger;
        String logPath = loggerImpl.getContext().getConfiguration().getStrSubstitutor()
                .getVariableResolver().lookup("LOG_FOLDER");
        File logFolder = new File(logPath);

        if (!shard.isPresent()) {
            // Zip current node logs
            elrondApiNode.zipDirectory(logFolder, "logs.zip");
        } else {
            // Download logs for all shards
            HashSet<PeerAddress> peers = elrondApiNode.getPeersOnSelectedShard(shard.get());
            for (PeerAddress peer : peers) {
                String peerHostAddress = peer.inetAddress().getHostAddress();
                if ( !elrondApiNode.copyRemoteFile("http://" + peerHostAddress + ":8080/node/getNodeLogs",
                        "downloads/" + peerHostAddress + ".zip") ) {
                    logger.warn("Could not copy remote peer log files from " + peerHostAddress);
                    continue;
                }
            }
            elrondApiNode.zipDirectory(new File("downloads"), "logs.zip");
        }

        File zipArchive = new File("logs.zip");
        Path path = Paths.get(zipArchive.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=logs.zip");
        logger.traceExit();
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(zipArchive.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }
}
