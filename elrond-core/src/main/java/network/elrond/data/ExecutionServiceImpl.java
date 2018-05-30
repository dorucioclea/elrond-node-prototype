package network.elrond.data;

import com.sun.xml.internal.ws.model.ExternalMetadataReader;
import network.elrond.account.AccountAddress;
import network.elrond.account.AccountState;
import network.elrond.account.Accounts;
import network.elrond.blockchain.Blockchain;
import network.elrond.blockchain.BlockchainService;
import network.elrond.blockchain.BlockchainUnitType;
import network.elrond.chronology.Epoch;
import network.elrond.consensus.SPoSService;
import network.elrond.core.Util;
import network.elrond.crypto.MultiSignatureService;
import network.elrond.crypto.Signature;
import network.elrond.service.AppServiceProvider;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExecutionServiceImpl implements ExecutionService {


    private SerializationService serializationService = AppServiceProvider.getSerializationService();

    @Override
    public ExecutionReport processBlock(Block block, Accounts accounts, Blockchain blockchain) {

        try {
            return _processBlock(accounts, blockchain, block);
        } catch (IOException | ClassNotFoundException e) {
            return ExecutionReport.create().ko(e);
        }
    }

    private boolean validateBlockSigners(Accounts accounts, Blockchain blockchain, Block block) {
        // TODO: need to check that signers are the right ones for that specific epoch & round
        // Signers part of the eligible list in the epoch
        // Signers are selected by the previous block signature
        return true;
    }

    private boolean validateBlockSignature(byte[] signature, byte[] commitment, ArrayList<String> signers, Block block){
        // TODO: validate the multi-signature for the participating signers
        MultiSignatureService signatureService = AppServiceProvider.getMultiSignatureService();
        ArrayList<byte[]> signersPublicKeys = new ArrayList<>();
        block.getSig1();
        block.getSig2();
        long bitmap = (1 << signers.size()) - 1;
        byte[] message = AppServiceProvider.getSerializationService().getHash(block, false);

        for(String signer : signers){
            signersPublicKeys.add(Util.hexStringToByteArray(signer));
        }

        return signatureService.verifyAggregatedSignature(signersPublicKeys, signature, commitment, message, bitmap);
    }

    private ExecutionReport _processBlock(Accounts accounts, Blockchain blockchain, Block block) throws IOException, ClassNotFoundException {
        ExecutionReport blockExecutionReport = ExecutionReport.create();
        BlockchainService blockchainService = AppServiceProvider.getBlockchainService();
        String blockHash = new String(Base64.encode(serializationService.getHash(block, true)));
        ArrayList<String> signers;
        byte[] signature;
        byte[] commitment;

        // check that block is not already processed
        if(blockchainService.contains(blockHash, blockchain, BlockchainUnitType.BLOCK) ){
            blockExecutionReport.ko("Block already in blockchain");
            return blockExecutionReport;
        }

        // check if previous block hash is in blockchain, otherwise can't add it yet
        if(!blockchainService.contains(block.getPrevBlockHash(), blockchain, BlockchainUnitType.BLOCK)) {
            blockExecutionReport.ko("Previous block not in blockchain");
            return blockExecutionReport;
        }

        // check block signers are valid for the round
        if(!validateBlockSigners(accounts, blockchain, block)) {
            blockExecutionReport.ko("Signers not ok for epoch/round");
            return blockExecutionReport;
        }

        // get signature parts from block
        signers = (ArrayList<String>)block.getListPublicKeys();
        signature = block.getSig1();
        commitment = block.getSig2();

        // check multi-signature is valid
        if(!validateBlockSignature(signature, commitment, signers, block)){
            blockExecutionReport.ko("Signature not valid");
        }

        // TODO: split the block processing for the two usecases
        // there are two usecases for processing blocks
        // 1. when part of the consensus group, the node needs to validate and sign the block and add it to it's blockchain if pBFT OK otherwise rollback
        // 2. when not part of the consensus validate block and add it to it's blockchain if valid

        // Process transactions
        List<Transaction> transactions = AppServiceProvider.getTransactionService().getTransactions(blockchain, block);
        for (Transaction transaction : transactions) {

            ExecutionReport transactionExecutionReport = processTransaction(transaction, accounts);
            if (!transactionExecutionReport.isOk()) {
                blockExecutionReport.combine(transactionExecutionReport);
                break;
            }
        }

        if (blockExecutionReport.isOk()) {
            // check state merkle patricia trie root is the same with what was stored in block
            if(!Arrays.equals(block.getAppStateHash(),accounts.getAccountsPersistenceUnit().getRootHash())) {
                blockExecutionReport.ko("Application state root hash does not match");
                return blockExecutionReport;
            }

            AppServiceProvider.getAccountStateService().commitAccountStates(accounts);
            blockExecutionReport.ok("Commit account state changes");
        } else {
            AppServiceProvider.getAccountStateService().rollbackAccountStates(accounts);
            blockExecutionReport.ko("Rollback account state changes");
        }

        return blockExecutionReport;
    }

    @Override
    public ExecutionReport processTransaction(Transaction transaction, Accounts accounts) {

        try {
            return _processTransaction(accounts, transaction);
        } catch (Exception e) {
            return ExecutionReport.create().ko(e);
        }
    }

    private ExecutionReport _processTransaction(Accounts accounts, Transaction transaction) throws IOException, ClassNotFoundException {

        if (transaction == null) {
            return ExecutionReport.create().ko("Null transaction");
        }

        String strHash = new String(Base64.encode(serializationService.getHash(transaction, true)));

        if (!AppServiceProvider.getTransactionService().verifyTransaction(transaction)) {
            return ExecutionReport.create().ko("Invalid transaction! tx hash: " + strHash);
        }

        //We have to copy-construct the objects for sandbox mode
        AccountAddress receiverAddress = transaction.getReceiverAccountAddress();
        AccountState receiverAccountState = AppServiceProvider.getAccountStateService().getOrCreateAccountState(receiverAddress, accounts);

        AccountAddress sendAddress = transaction.getSendAccountAddress();
        AccountState senderAccountState = AppServiceProvider.getAccountStateService().getOrCreateAccountState(sendAddress, accounts);

        if (senderAccountState.getBalance().compareTo(transaction.getValue()) < 0) {
            return ExecutionReport.create().ko("Invalid transaction! Will result in negative balance! tx hash: " + strHash);
        }

        if (!senderAccountState.getNonce().equals(transaction.getNonce())) {
            return ExecutionReport.create().ko("Invalid transaction! Nonce mismatch! tx hash: " + strHash);
        }

        //transfer asset
        receiverAccountState.setBalance(receiverAccountState.getBalance().add(transaction.getValue()));
        AppServiceProvider.getAccountStateService().setAccountState(receiverAddress, receiverAccountState, accounts); // PMS

        senderAccountState.setBalance(senderAccountState.getBalance().subtract(transaction.getValue()));
        //increase sender nonce
        senderAccountState.setNonce(senderAccountState.getNonce().add(BigInteger.ONE));
        AppServiceProvider.getAccountStateService().setAccountState(sendAddress, senderAccountState, accounts); // PMS

        return ExecutionReport.create().ok();
    }
}
