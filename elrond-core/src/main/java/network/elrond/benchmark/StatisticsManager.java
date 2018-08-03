package network.elrond.benchmark;

import network.elrond.application.AppState;
import network.elrond.service.AppServiceProvider;
import network.elrond.sharding.AppShardingManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StatisticsManager implements Serializable {
    private static final Logger logger = LogManager.getLogger(StatisticsManager.class);
    private ElrondSystemTimer timer;
    private static final int maxStatistics = 100;
    private List<Statistic> statistics = new ArrayList<>();

    private Integer currentShardNumber = 0;
    private Integer numberNodesInShard = 0;

    private long currentIndex = 0;
    private long currentMillis = 0;
    private long startMillis = 0;

    private Double averageTps = 0.0;
    private Double maxTps = 0.0;
    private Double minTps = Double.MAX_VALUE;
    private Double liveTps = 0.0;

    private long averageNrTransactionsInBlock = 0;
    private long maxNrTransactionsInBlock = 0;
    private long minNrTransactionsInBlock = Integer.MAX_VALUE;
    private long liveNrTransactionsInBlock = 0;
    private long currentBlockNonce = 0;

    private long averageRoundTime = 0;
    private long liveRoundTime = 0;

    private long totalProcessedTransactions = 0;

    public StatisticsManager(ElrondSystemTimer timer, int currentShardNumber) {
        this.timer = timer;
        this.startMillis = timer.getCurrentTime();
        currentMillis = startMillis;
        this.currentShardNumber = currentShardNumber;
    }

    public void updateNetworkStats(AppState state) {
        numberNodesInShard = AppShardingManager.instance().getNumberNodesInShard(state);
    }

    Statistic currentStatistic = null;

    public void addStatistic(Statistic statistic){
        currentStatistic = statistic;
    }

    public void processStatistic() {
        logger.traceEntry("params: {}", currentStatistic);

        if(currentStatistic == null){
            return;
        }
        long roundTimeDuration = AppServiceProvider.getChronologyService().getRoundTimeDuration();

        long newTime  = timer.getCurrentTime();
        long ellapsedMillis = newTime - currentMillis;
        liveRoundTime = ellapsedMillis;
        currentMillis = newTime;

        liveRoundTime = roundTimeDuration;

        statistics.add(currentStatistic);
        if (statistics.size() > maxStatistics) {
            statistics.remove(0);
        }


        totalProcessedTransactions += currentStatistic.getNrTransactionsInBlock();
        liveTps = currentStatistic.getNrTransactionsInBlock() * 1000.0 / liveRoundTime;

        logger.info("Live Round Time is: " + liveRoundTime + " and processed " + currentStatistic.getNrTransactionsInBlock() + " transactions at a TPS of: " + liveTps);

        logger.trace("currentTps is " + liveTps);
        ComputeTps(liveTps);

        liveNrTransactionsInBlock = currentStatistic.getNrTransactionsInBlock();
        computeNrTransactionsInBlock(liveNrTransactionsInBlock);

        computeAverageRoundTime(ellapsedMillis);

        if(currentBlockNonce < currentStatistic.getCurrentBlockNonce()){
            currentBlockNonce = currentStatistic.getCurrentBlockNonce();
        }

        currentIndex++;
        logger.traceExit();
    }

    private void computeAverageRoundTime(long timeDifference) {
        averageRoundTime = (averageRoundTime * currentIndex + timeDifference) / (currentIndex + 1);
        logger.trace("averageNrTransactionsInBlock is " + averageNrTransactionsInBlock);
    }

    private void ComputeTps(Double currentTps) {
        if (maxTps < currentTps) {
            maxTps = currentTps;
        }

        if (minTps > currentTps) {
            minTps = currentTps;
        }

        averageTps = (totalProcessedTransactions * 1000.0) / (timer.getCurrentTime() - startMillis);
        logger.trace("averageTps is " + averageTps);
    }

    private void computeNrTransactionsInBlock(long currentNrTransactionsInBlock) {
        if (maxNrTransactionsInBlock < currentNrTransactionsInBlock) {
            maxNrTransactionsInBlock = currentNrTransactionsInBlock;
        }

        if (minNrTransactionsInBlock > currentNrTransactionsInBlock) {
            minNrTransactionsInBlock = currentNrTransactionsInBlock;
        }
        if(currentNrTransactionsInBlock>0) { //only in proposed blocks, this cannot have zero trasactions in them
            averageNrTransactionsInBlock = (averageNrTransactionsInBlock * currentIndex + currentNrTransactionsInBlock) / (currentIndex + 1);
        }
        logger.trace("averageNrTransactionsInProposedBlock is " + averageNrTransactionsInBlock);
    }

    public Double getAverageTps() {
        return averageTps;
    }

    public Double getMaxTps() {
        return maxTps;
    }

    public Double getMinTps() {
        return minTps;
    }

    public Double getLiveTps() {
        return liveTps;
    }

    public long getAverageNrTransactionsInBlock() {
        return averageNrTransactionsInBlock;
    }

    public long getMaxNrTransactionsInBlock() {
        return maxNrTransactionsInBlock;
    }

    public long getMinNrTransactionsInBlock() {
        return minNrTransactionsInBlock;
    }

    public long getLiveNrTransactionsInBlock() {
        return liveNrTransactionsInBlock;
    }

    public Integer getNumberNodesInShard() {
        return numberNodesInShard;
    }

    public long getAverageRoundTime() {
        return averageRoundTime;
    }

    public long getLiveRoundTime() {
        return liveRoundTime;
    }

    public long getTotalNrProcessedTransactions() {
        return totalProcessedTransactions;
    }

    public Integer getCurrentShardNumber() {
        return currentShardNumber;
    }

    public long getCurrentBlockNonce() { return currentBlockNonce;     }
}
