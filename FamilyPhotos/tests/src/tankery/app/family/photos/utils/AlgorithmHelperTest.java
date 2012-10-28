package tankery.app.family.photos.utils;

import junit.framework.TestCase;

public class AlgorithmHelperTest extends TestCase {

    private final AlgorithmHelper.Comparetor<Integer> testComparetor = new AlgorithmHelper.Comparetor<Integer>() {

        @Override
        public int compareTarget(Integer obj1, Integer obj2) {
            return obj1 - obj2;
        }

        @Override
        public int itemRelatedToTarget(int index, Integer topLine) {
            return index - topLine;
        }
    };

    private static final int ITEM_LIST_LENGTH = 20;

    private static final int[][] indexAndTarget = {
        {0, ITEM_LIST_LENGTH, 0, ITEM_LIST_LENGTH},
        {0, ITEM_LIST_LENGTH, 0, ITEM_LIST_LENGTH/2},
        {0, ITEM_LIST_LENGTH, ITEM_LIST_LENGTH/2, ITEM_LIST_LENGTH},
        {0, ITEM_LIST_LENGTH, 0, 1},
        {0, ITEM_LIST_LENGTH, 0, 2},
        {0, ITEM_LIST_LENGTH, ITEM_LIST_LENGTH - 2, ITEM_LIST_LENGTH},
        {0, ITEM_LIST_LENGTH, ITEM_LIST_LENGTH - 1, ITEM_LIST_LENGTH},
        {0, ITEM_LIST_LENGTH, ITEM_LIST_LENGTH, ITEM_LIST_LENGTH/2}
    };

    public void testBinaryFindBetween() {
        int count = indexAndTarget.length / 4;
        for (int i = 0; i < count; i++) {
            int[] params = indexAndTarget[i];
            int[] result = AlgorithmHelper.binaryFindBetween(params[0],
                                                             params[1],
                                                             params[2],
                                                             params[3],
                                                             testComparetor);
            if (result == null) {
                assertTrue(params[2] >= params[3]);
                continue;
            }
            int[] expect = { Math.max(params[0], params[2]),
                            Math.min(params[1], params[3]) };
            assertEquals(expect[0], result[0]);
            assertEquals(expect[1], result[1]);
        }
    }

}
