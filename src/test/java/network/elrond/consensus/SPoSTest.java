package network.elrond.consensus;

import junit.framework.TestCase;
import network.elrond.UtilTest;
import network.elrond.core.Util;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class SPoSTest {
    @Test
    public void testSPoSCleanedUpList() {
        List<Validator> vList = new ArrayList<Validator>();

        vList.add(new Validator("0xA01", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(0), 0));
        vList.add(new Validator("0xA01", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(0), 0));
        vList.add(new Validator("0xA02", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(1), 0));
        vList.add(new Validator("0xA03", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(0), 1));
        vList.add(new Validator("0xA04", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE, 0));
        vList.add(new Validator("0xA05", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE, 1));
        vList.add(new Validator("0xA06", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(2)), 0));
        vList.add(new Validator("0xA07", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(2)), 1));
        vList.add(new Validator("0xA08", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(2)), 2));
        vList.add(new Validator("0xA09", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(10)), 2));
        vList.add(new Validator("0xA10", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(10)), 10));
        vList.add(new Validator("0xA11", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(10)), -1));
        vList.add(new Validator("0xA12", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.subtract(BigInteger.valueOf(1)), 0));

        EligibleListValidators cleanedUpList = SPoS.generateCleanupList(vList);

        System.out.printf("Original list [%d]:",vList.size());
        System.out.println();
        System.out.println("==========================================================");
        UtilTest.displayListValidators(vList);

        System.out.printf("Cleaned up list [%d]:", cleanedUpList.list.size());
        System.out.println();
        System.out.println("==========================================================");
        UtilTest.displayListValidators(cleanedUpList.list);

        //plain copy test
        TestCase.assertEquals(7, cleanedUpList.list.size());

    }

    @Test
    public void testSPoSWeightedEligibleListSmall() {

        List<Validator> vList = new ArrayList<Validator>();

        //plain copy (not enough nodes so do not bother generating more records into temp)

        vList.add(new Validator("0xA01", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(0), 0));
        vList.add(new Validator("0xA01", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(0), 0));
        vList.add(new Validator("0xA02", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(1), 0));
        vList.add(new Validator("0xA03", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(0), 1));
        vList.add(new Validator("0xA04", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE, 0));
        vList.add(new Validator("0xA05", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE, 1));
        vList.add(new Validator("0xA06", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(2)), 0));
        vList.add(new Validator("0xA07", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(2)), 1));
        vList.add(new Validator("0xA08", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(2)), 2));
        vList.add(new Validator("0xA09", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(10)), 2));
        vList.add(new Validator("0xA10", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(10)), 10));
        vList.add(new Validator("0xA11", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(10)), -1));
        vList.add(new Validator("0xA12", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.subtract(BigInteger.valueOf(1)), 0));

        EligibleListValidators cleanedUpList = SPoS.generateCleanupList(vList);
        List<Validator> lResult = SPoS.generateWeightedEligibleList(cleanedUpList);

        System.out.printf("[small] Original list [%d]:", vList.size());
        System.out.println();
        System.out.println("==========================================================");
        UtilTest.displayListValidators(vList);

        System.out.printf("[small] Weighted eligible [%d]:", lResult.size());
        System.out.println();
        System.out.println("==========================================================");
        UtilTest.displayListValidators(lResult);

        //plain copy test
        TestCase.assertEquals(26, lResult.size());
    }

    @Test
    public void testSPoSWeightedEligibleListMedium() {

        List<Validator> vList = new ArrayList<Validator>();

        //plain copy (not enough nodes so do not bother generating more records into temp)

        vList.add(new Validator("0xA01", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(0), 0));
        vList.add(new Validator("0xA01", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(0), 0));
        vList.add(new Validator("0xA02", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(1), 0));
        vList.add(new Validator("0xA03", "", ConsensusAnswerType.NOT_ANSWERED, BigInteger.valueOf(0), 1));
        vList.add(new Validator("0xA04", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE, 0));
        vList.add(new Validator("0xA05", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE, 1));
        vList.add(new Validator("0xA06", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(2)), 0));
        vList.add(new Validator("0xA07", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(2)), 1));
        vList.add(new Validator("0xA08", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(2)), 2));
        vList.add(new Validator("0xA09", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(10)), 2));
        vList.add(new Validator("0xA10", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(10)), 10));
        vList.add(new Validator("0xA11", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(10)), -1));
        vList.add(new Validator("0xA12", "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.subtract(BigInteger.valueOf(1)), 0));

        for (int i = 20; i < 100; i++) {
            vList.add(new Validator("0xA" + Integer.toString(i), "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE, 0));
        }

        EligibleListValidators cleanedUpList = SPoS.generateCleanupList(vList);
        List<Validator> lResult = SPoS.generateWeightedEligibleList(cleanedUpList);

        System.out.printf("[small] Original list [%d]:", vList.size());
        System.out.println();
        System.out.println("==========================================================");
        UtilTest.displayListValidators(vList);

        System.out.printf("[small] Weighted eligible [%d]:", lResult.size());
        System.out.println();
        System.out.println("==========================================================");
        UtilTest.displayListValidators(lResult);

        //plain copy test
        TestCase.assertEquals(106, lResult.size());
    }

    @Test
    public void DoBenchMarkNTimes()
    {
        for (int i = 0; i < 10; i++)
        {
            BenchMarkLotsOfValidators();
        }
    }

    @Test
    public void BenchMarkLotsOfValidators() {
        List<Validator> vList = new ArrayList<Validator>();

        Date dStart = new Date();

        for (int i = 0; i < 10000; i++) {
            vList.add(new Validator("0xA" + Integer.toString(i), "", ConsensusAnswerType.NOT_ANSWERED, Util.MIN_STAKE.multiply(BigInteger.valueOf(1)), 0));
        }

        Date dInterim = new Date();

        long ms1 = dInterim.getTime() - dStart.getTime();



        Util.SHA3.digest();

        dInterim = new Date();

        //CalcEligibleListValidators cleanedUpList = SPoS.generateCleanupList(vList);
        List<Validator> lResult = SPoS.generateValidatorsList("TEST", vList, BigInteger.ONE);

        Date dEnd = new Date();

        long ms2 = dEnd.getTime() - dInterim.getTime();

        System.out.printf("Generation took: %d ms, computation took: %d ms", ms1, ms2);
        System.out.println();
    }
}
