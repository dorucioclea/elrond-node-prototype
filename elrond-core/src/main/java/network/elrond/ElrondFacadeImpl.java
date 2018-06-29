package network.elrond;

import network.elrond.account.AccountAddress;
import network.elrond.account.AccountState;
import network.elrond.account.Accounts;
import network.elrond.application.AppContext;
import network.elrond.application.AppState;
import network.elrond.benchmark.BenchmarkResult;
import network.elrond.benchmark.MultipleTransactionResult;
import network.elrond.benchmark.StatisticsManager;
import network.elrond.blockchain.Blockchain;
import network.elrond.blockchain.BlockchainUnitType;
import network.elrond.core.FutureUtil;
import network.elrond.core.ObjectUtil;
import network.elrond.core.ThreadUtil;
import network.elrond.core.Util;
import network.elrond.crypto.PKSKPair;
import network.elrond.crypto.PrivateKey;
import network.elrond.crypto.PublicKey;
import network.elrond.data.Block;
import network.elrond.data.Receipt;
import network.elrond.data.SecureObject;
import network.elrond.data.Transaction;
import network.elrond.p2p.*;
import network.elrond.service.AppServiceProvider;
import network.elrond.sharding.Shard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ElrondFacadeImpl implements ElrondFacade {
    private static final Logger logger = LogManager.getLogger(ElrondFacadeImpl.class);

    @Override
    public Application start(AppContext context) {
        logger.traceEntry("params: {}", context);

        try {
            Application application = new Application(context);
            application.start();
            logger.trace("Started!");
            return logger.traceExit(application);
        } catch (Exception e) {
            logger.catching(e);
            return logger.traceExit((Application) null);
        }
    }

    @Override
    public boolean stop(Application application) {
        logger.traceEntry("params: {}", application);

        try {
            application.stop();
            logger.trace("Stopped!");
            return logger.traceExit(true);
        } catch (Exception e) {
            logger.catching(e);
            return logger.traceExit(false);
        }
    }

    @Override
    public BigInteger getBalance(AccountAddress address, Application application) {
        logger.traceEntry("params: {} {}", address, application);

        if (application == null) {
            logger.warn("application is null");
            return BigInteger.ZERO;
        }


        AppState state = application.getState();
        Shard currentShard = application.getState().getShard();
        Shard addressShard = AppServiceProvider.getShardingService().getShard(address.getBytes());

        if (ObjectUtil.isEqual(addressShard, currentShard)) {

            try {

                Accounts accounts = state.getAccounts();
                AccountState account = AppServiceProvider.getAccountStateService().getAccountState(address, accounts);

                return logger.traceExit((account == null) ? BigInteger.ZERO : account.getBalance());

            } catch (Exception ex) {
                logger.throwing(ex);
                return logger.traceExit((BigInteger) null);
            }
        }

        try {

            P2PRequestChannel channel = state.getChanel(P2PRequestChannelName.ACCOUNT);
            AccountState account = AppServiceProvider.getP2PRequestService().get(channel, addressShard, P2PRequestChannelName.ACCOUNT, address);

            return logger.traceExit((account == null) ? BigInteger.ZERO : account.getBalance());

        } catch (Exception ex) {
            logger.throwing(ex);
            return logger.traceExit((BigInteger) null);
        }


    }

    @Override
    public Receipt getReceipt(String transactionHash, Application application) {
        logger.traceEntry("params: {} {}", transactionHash, application);
        Blockchain blockchain = application.getState().getBlockchain();

        try {

            Receipt receipt = FutureUtil.get(() -> {

                String receiptHash = null;
                SecureObject<Receipt> secureReceipt = null;

                do {
                    receiptHash = AppServiceProvider.getBlockchainService().get(transactionHash, blockchain, BlockchainUnitType.TRANSACTION_RECEIPT);
                    if (receiptHash != null) {
                        secureReceipt = AppServiceProvider.getBlockchainService().get(receiptHash, blockchain, BlockchainUnitType.RECEIPT);
                    }
                    ThreadUtil.sleep(200);

                } while (receiptHash == null || secureReceipt == null);

                return secureReceipt.getObject();

            }, 60L);

            return logger.traceExit(receipt);


        } catch (Exception e) {
            logger.catching(e);
        }
        return logger.traceExit((Receipt) null);
    }

    @Override
    public MultipleTransactionResult sendMultipleTransactions(AccountAddress receiver, BigInteger value, Integer nrTransactions, Application application) {
        logger.traceEntry("params: {} {} {}", receiver, value, application);
        if (application == null) {
            logger.warn("Invalid application state, application is null");
            return logger.traceExit((MultipleTransactionResult) null);
        }
        MultipleTransactionResult result = new MultipleTransactionResult();
        List<Transaction> transactions = new ArrayList<>();
        try {
        for (int i = 0; i < nrTransactions; i++) {
            Transaction transaction = generateTransaction(receiver, value, application.getState());
            transactions.add(transaction);
//            if (transaction == null) {
//                result.setFailedTransactionsNumber(result.getFailedTransactionsNumber() + 1);
//            } else {
//                result.setSuccessfulTransactionsNumber(result.getSuccessfulTransactionsNumber() + 1);
//            }
        }



            transactions.stream().parallel().forEach((tr)-> {
                try {
                    sendTransaction(application.getState(), tr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Transaction send(AccountAddress receiver, BigInteger value, Application application) {
        logger.traceEntry("params: {} {} {}", receiver, value, application);
        if (application == null) {
            logger.warn("Invalid application state, application is null");
            return logger.traceExit((Transaction) null);
        }

        try {

            Transaction transaction = generateTransaction(receiver, value, application.getState());

            sendTransaction(application.getState(), transaction);

            return logger.traceExit(transaction);


        } catch (Exception ex) {
            logger.catching(ex);
        }

        return logger.traceExit((Transaction) null);
    }

    private Transaction generateTransaction(AccountAddress receiver, BigInteger value, AppState state) throws IOException, ClassNotFoundException {
        Accounts accounts = state.getAccounts();

        PublicKey senderPublicKey = state.getPublicKey();
        PrivateKey senderPrivateKey = state.getPrivateKey();
        AccountAddress senderAddress = AccountAddress.fromBytes(senderPublicKey.getValue());
        AccountState senderAccount = AppServiceProvider.getAccountStateService().getAccountState(senderAddress, accounts);


        if (senderAccount == null) {
            // sender account is new, can't send
            logger.warn("Sender account is new, can't send");
            return logger.traceExit((Transaction) null);
        }

        PublicKey receiverPublicKey = new PublicKey(receiver.getBytes());

        BigInteger nonce = senderAccount.getNonce();
        Transaction transaction = AppServiceProvider.getTransactionService().generateTransaction(senderPublicKey, receiverPublicKey, value, nonce);
        AppServiceProvider.getTransactionService().signTransaction(transaction, senderPrivateKey.getValue(), senderPublicKey.getValue());
        return transaction;

    }

    private void sendTransaction(AppState state, Transaction transaction) throws java.io.IOException {
        String hash = AppServiceProvider.getSerializationService().getHashString(transaction);
        P2PConnection connection = state.getConnection();
        AppServiceProvider.getP2PObjectService().put(connection, hash, transaction);

        P2PBroadcastChanel channel = state.getChanel(P2PBroadcastChannelName.TRANSACTION);
        AppServiceProvider.getP2PBroadcastService().publishToChannel(channel, hash);
    }

    @Override
    public PingResponse ping(String ipAddress, int port) {
        logger.traceEntry("params: {} {}", ipAddress, port);
        try {
            return logger.traceExit(AppServiceProvider.getP2PCommunicationService().getPingResponse(ipAddress, port));
        } catch (Exception ex) {
            logger.catching(ex);
            PingResponse pingResponse = new PingResponse();
            pingResponse.setErrorMessage(ex.getLocalizedMessage());
            return logger.traceExit(pingResponse);
        }
    }

    @Override
    public PKSKPair generatePublicKeyAndPrivateKey(String strPrivateKey) {
        logger.traceEntry();
        try {
            PrivateKey privateKey = null;

            if ((strPrivateKey == null) || strPrivateKey.isEmpty()) {
                privateKey = new PrivateKey();
            } else {
                privateKey = new PrivateKey(Util.hexStringToByteArray(strPrivateKey));
            }

            PublicKey publicKey = new PublicKey(privateKey);

            return logger.traceExit(new PKSKPair(Util.byteArrayToHexString(publicKey.getValue()), Util.byteArrayToHexString(privateKey.getValue())));
        } catch (Exception ex) {
            logger.catching(ex);
            return logger.traceExit(new PKSKPair("Error", "Error"));
        }
    }

    @Override
    public ArrayList<BenchmarkResult> getBenchmarkResult(String benchmarkId, Application application) {
        ArrayList<BenchmarkResult> benchmarkResults = new ArrayList<>();

        P2PRequestChannel channel = application.getState().getChanel(P2PRequestChannelName.STATISTICS);
        Integer numberOfShards = AppServiceProvider.getShardingService().getNumberOfShards();

        ArrayList<StatisticsManager> shardsStatistics = new ArrayList<>();

        for (int i = 0; i < numberOfShards; i++) {
            Shard addressShard = new Shard(i);
            StatisticsManager statisticsManager = AppServiceProvider.getP2PRequestService().get(channel, addressShard, P2PRequestChannelName.STATISTICS, null);
            if (statisticsManager != null) {
                shardsStatistics.add(statisticsManager);
            }
        }

        for (StatisticsManager statisticsManager : shardsStatistics) {
            BenchmarkResult benchmarkResult = new BenchmarkResult();
            benchmarkResult.setCurrentShardNumber(statisticsManager.getCurrentShardNumber());
            benchmarkResult.setNetworkActiveNodes(statisticsManager.getNumberNodesInNetwork());
            benchmarkResult.setShardActiveNodes(statisticsManager.getNumberNodesInShard());
            benchmarkResult.setAverageRoundTime(statisticsManager.getAverageRoundTime());
            benchmarkResult.setLiveNrTransactionsPerBlock(statisticsManager.getLiveNrTransactionsInBlock());
            benchmarkResult.setAverageNrTxPerBlock(statisticsManager.getAverageNrTransactionsInBlock());
            benchmarkResult.setAverageTps(statisticsManager.getAverageTps());
            benchmarkResult.setLiveTps(statisticsManager.getLiveTps());
            benchmarkResult.setPeakTps(statisticsManager.getMaxTps());
            benchmarkResult.setLiveRoundTime(statisticsManager.getLiveRoundTime());
            benchmarkResult.setTotalNrProcessedTransactions(statisticsManager.getTotalNrProcessedTransactions());
            benchmarkResult.setNrShards(statisticsManager.getNumberOfShards());
            benchmarkResults.add(benchmarkResult);
        }
        return benchmarkResults;
    }

    @Override
    public Transaction getTransactionFromHash(String transactionHash, Blockchain blockchain){
        try {
            return (AppServiceProvider.getBlockchainService().get(transactionHash, blockchain, BlockchainUnitType.TRANSACTION));
        } catch (Exception ex){
            logger.throwing(ex);
            return (null);
        }
    }

    @Override
    public Block getBlockFromHash(String blockHash, Blockchain blockchain){
        try {
            return (AppServiceProvider.getBlockchainService().get(blockHash, blockchain, BlockchainUnitType.BLOCK));
        } catch (Exception ex){
            logger.throwing(ex);
            return (null);
        }
    }

}
