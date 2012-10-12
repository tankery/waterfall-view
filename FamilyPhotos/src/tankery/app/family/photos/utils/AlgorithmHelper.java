package tankery.app.family.photos.utils;

public final class AlgorithmHelper {
    /**
     * interface for search compare.
     * 
     * @author tankery
     * 
     */
    public interface Comparetor<T> {
        /**
         * algorithm will pass the obj1 to compare with the obj2.
         * 
         * @param index
         *            the index of object in list.
         * @param target
         * @return the compare result.
         * the meaning of result is here:
         * return < 0: obj1 is in lower index of obj2;
         * return = 0: obj1 is in same index of obj2;
         * return > 0: obj1 is in higher index of obj2.
         */
        abstract int compareTarget(T obj1, T obj2);

        abstract int itemRelatedToTarget(int index, T obj);
    }

    static public <I, T> int[] binaryFindBetween(int begin, int end, T first,
            T last, Comparetor<T> comparetor) {
        if (comparetor.compareTarget(first, last) >= 0)
            return null;

        int firstItemIndex = -1;
        int lastItemIndex = -1;

        // using binary search to find the items at first and last.
        int midPt = 0;
        // first, find the item between the search area.
        while (begin <= end) {
            midPt = (begin + end) / 2;
            int itemToFirst = comparetor.itemRelatedToTarget(midPt, first);
            int itemToLast = comparetor.itemRelatedToTarget(midPt, last);
            if (itemToFirst == 0) {
                firstItemIndex = midPt;
                break;
            } else if (itemToLast == 0) {
                lastItemIndex = midPt;
                break;
            } else if (itemToFirst > 0 && itemToLast < 0) {
                // find the center item, just break;
                break;
            } else if (itemToFirst < 0) {
                begin = midPt + 1;
            } else if (itemToLast > 0) {
                end = midPt - 1;
            }
        }

        if (begin > end)
            return null; // not found.

        // if item on first not found, find it
        if (firstItemIndex == -1) {
            int s, e, m;
            s = begin;
            e = midPt - 1;
            while (s <= e) {
                m = (s + e) / 2;
                int itemToFirst = comparetor.itemRelatedToTarget(m, first);
                if (itemToFirst == 0) {
                    firstItemIndex = m;
                    break;
                } else if (itemToFirst < 0) {
                    s = m + 1;
                } else if (itemToFirst > 0) {
                    e = m - 1;
                }
            }
            if (s > e)
                return null; // not found.
        }

        // if item on last not found, find it
        if (lastItemIndex == -1) {
            int s, e, m;
            s = midPt + 1;
            e = end;
            while (s <= e) {
                m = (s + e) / 2;
                int itemToLast = comparetor.itemRelatedToTarget(m, last);
                if (itemToLast == 0) {
                    lastItemIndex = m;
                    break;
                } else if (itemToLast < 0) {
                    s = m + 1;
                } else if (itemToLast > 0) {
                    e = m - 1;
                }
            }
            if (s > e)
                return null; // not found.
        }

        // one item on first and last at the same time, treat as not found.
        if (firstItemIndex == lastItemIndex)
            return null;

        int[] ret = { firstItemIndex, lastItemIndex };

        return ret;
    }
}
