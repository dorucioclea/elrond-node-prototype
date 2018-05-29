package network.elrond.data;

import network.elrond.blockchain.Blockchain;
import network.elrond.blockchain.BlockchainUnitType;
import network.elrond.core.Util;
import network.elrond.crypto.PrivateKey;
import network.elrond.crypto.PublicKey;
import network.elrond.crypto.Signature;
import network.elrond.crypto.SignatureService;
import network.elrond.service.AppServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * The TransactionServiceImpl class implements TransactionService and is used to maintain Transaction objects
 *
 * @author Elrond Team - JLS
 * @version 1.0
 * @since 2018-05-16
 */
public class TransactionServiceImpl implements TransactionService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private SerializationService serializationService = AppServiceProvider.getSerializationService();


    /**
     * Computes the hash of the complete tx info
     * Used as a mean of tx identification
     *
     * @param tx      transaction
     * @param withSig whether or not to include the signature parts in hash
     * @return hash as byte array
     */
//    public byte[] getHash(Transaction tx, boolean withSig) {
//        String json = AppServiceProvider.getSerializationService().encodeJSON(tx);
//        return (Util.SHA3.digest(json.getBytes()));
//    }

    /**
     * Signs the transaction using private keys
     *
     * @param tx               transaction
     * @param privateKeysBytes private key as byte array
     */
    public void signTransaction(Transaction tx, byte[] privateKeysBytes) {
        PrivateKey pvkey = new PrivateKey(privateKeysBytes);
        byte[] hashNoSigLocal = serializationService.getHash(tx, false);
        PublicKey pbkey = new PublicKey(pvkey);
        Signature sig;

        SignatureService schnorr = AppServiceProvider.getSignatureService();
        sig = schnorr.signMessage(hashNoSigLocal, privateKeysBytes, pbkey.getValue());

        tx.setSig1(sig.getSignature());
        tx.setSig2(sig.getChallenge());
    }

    /**
     * Verify the data stored in tx
     *
     * @param tx to be verified
     * @return true if tx passes all consistency tests
     */
    public boolean verifyTransaction(Transaction tx) {
        //test 1. consistency checks
        if ((tx.getNonce().compareTo(BigInteger.ZERO) < 0) ||
                (tx.getValue().compareTo(BigInteger.ZERO) < 0) ||
                (tx.getSig1() == null) ||
                (tx.getSig2() == null) ||
                (tx.getSig1().length == 0) ||
                (tx.getSig2().length == 0) ||
                (tx.getSendAddress().length() != Util.MAX_LEN_ADDR) ||
                (tx.getReceiverAddress().length() != Util.MAX_LEN_ADDR) ||
                (tx.getPubKey().length() != Util.MAX_LEN_PUB_KEY * 2)
                ) {
            return (false);
        }

        //test 2. verify if sender address is generated from public key used to sign tx
        if (!tx.getSendAddress().equals(Util.getAddressFromPublicKey(Util.hexStringToByteArray(tx.getPubKey())))) {
            return (false);
        }

        //test 3. verify the signature
        byte[] message = serializationService.getHash(tx, false);
        SignatureService schnorr = AppServiceProvider.getSignatureService();

        return schnorr.verifySignature(tx.getSig1(), tx.getSig2(), message, Util.hexStringToByteArray(tx.getPubKey()));
    }

    @Override
    public List<Transaction> getTransactions(Blockchain blockchain, Block block) throws IOException, ClassNotFoundException {

        List<Transaction> transactions = new ArrayList<>();

        List<byte[]> hashes = block.getListTXHashes();
        for (byte[] hash : hashes) {
            String hashString = Util.getHashEncoded64(hash);
            Transaction transaction = AppServiceProvider.getBlockchainService().get(hashString, blockchain, BlockchainUnitType.TRANSACTION);
            transactions.add(transaction);
        }

        return transactions;
    }

    @Override
    public Transaction generateTransaction(PublicKey sender, PublicKey receiver, long value, long nonce) {
        return new Transaction(Util.getAddressFromPublicKey(sender.getValue()),
                Util.getAddressFromPublicKey(receiver.getValue()),
                BigInteger.valueOf(10).pow(8).multiply(BigInteger.valueOf(value)),
                BigInteger.valueOf(nonce));
    }


}