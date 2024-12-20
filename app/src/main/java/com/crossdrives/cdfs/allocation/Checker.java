package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.list.ListResult;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Checker {
    final String TAG = "CD.Checker";
    List<RuleIndividual<AllocResultCodes>> rulesSingle = new ArrayList<>();
    List<RuleJoined<AllocResultCodes>> rulesJoined = new ArrayList<>();

    interface RuleIndividual<Result>{
        Result check(AllocationItem item);
    }

    interface RuleJoined<Result>{
        Result check(List<AllocationItem> items);
    }

    public Checker() {

        rulesSingle.add(new RuleCheckSeqNum());
        rulesSingle.add(new RuleCheckSize());

        rulesJoined.add(new RuleCheckSizeCrossly());
        rulesJoined.add(new RuleCheckTotalSegCrossly());
        rulesJoined.add(new RuleCheckMissingItem());
    }

    /*
        Stream API tutorial: https://www.baeldung.com/java-8-streams
     */
    public List<ListResult> checkAllocationFile(AllocContainer ac){
        List<ListResult> results = new ArrayList<>();


        return results;
    }

    public List<AllocResultCodes> checkAllocItem(final AllocationItem item){
        List<AllocResultCodes> results = new ArrayList<>();
        rulesSingle.forEach((rule)->{
            results.add(rule.check(item));
            });

        return results;
    }

    public List<AllocResultCodes> checkItemsCrossly(List<AllocationItem> items){
        List<AllocResultCodes> results = new ArrayList<>();
        rulesJoined.forEach((rule)->{
            results.add(rule.check(items));
        });

        return results;
    }

    /*
        Single item checks
     */
    /*
        Seq starts with 1 and the max equals to totalSeg.
     */
    class RuleCheckSeqNum implements RuleIndividual<AllocResultCodes> {

        @Override
        public AllocResultCodes check(AllocationItem item) {
            int seq = item.getSequence();
            int totalSeg = item.getTotalSeg();
            AllocResultCodes result = new AllocResultCodes(AllocResultCodes.SUCCESS, "");

            if( seq == 0){
                result.setErr(ResultCode.ERR_SEQ_OVER_SEG);
                Log.w(TAG, "Sequence number(SEQ) = " + seq + "An invalid Seq.");
                result.setReason("Sequence number(SEQ) = " + seq + "An invalid Seq.");
            }
            if (seq > totalSeg) {
                result.setErr(ResultCode.ERR_SEQ_OVER_SEG);
                Log.w(TAG, "Sequence number(SEQ) = " + seq + ". However total segment(totalSeg) is " + totalSeg);
                result.setReason("Sequence number(SEQ) = " + seq + ". However total segment(totalSeg) is " + totalSeg );
            }
            return result;
        }
    }

    class RuleCheckSize implements RuleIndividual<AllocResultCodes> {

        @Override
        public AllocResultCodes check(AllocationItem item) {
            long size = item.getSize();
            long maxSize = item.getCDFSItemSize();
            AllocResultCodes result = new AllocResultCodes(AllocResultCodes.SUCCESS, "");

            if (size > maxSize) {
                result.setErr(ResultCode.ERR_SIZE_OVER_MAX);
                Log.w(TAG, "size of item (SIZE) = " + size + ". However maximum size(CDFSitemSize) is " +  maxSize);
                result.setReason("size of item (SIZE) = " + size + ". However maximum size(CDFSitemSize) is " +  maxSize);
            }
            return result;
        }
    }
    /*
        Cross item checks
    */
    class RuleCheckSizeCrossly implements RuleJoined<AllocResultCodes>{

        @Override
        public AllocResultCodes check(List<AllocationItem> items) {
            AllocResultCodes result = new AllocResultCodes(AllocResultCodes.SUCCESS, "SUCCESS");
            final long size = items.get(0).getCDFSItemSize();
            if(!items.stream().allMatch((item) -> {
                return item.getCDFSItemSize() == size;
            })){
                Log.w(TAG, "CDFS Sizes are not identical.");
                result.setErr(AllocResultCodes.ERR_CDFSSIZE_NOT_IDENTICAL);
            }

            return result;
        }
    }

    class RuleCheckTotalSegCrossly implements RuleJoined<AllocResultCodes>{

        @Override
        public AllocResultCodes check(List<AllocationItem> items) {
            AllocResultCodes result = new AllocResultCodes(AllocResultCodes.SUCCESS, "");

            final int totalSeg = items.get(0).getTotalSeg();

            if(!items.stream().allMatch((item) -> {
                return item.getTotalSeg() == totalSeg;
            })) {
                Log.w(TAG, "totalSeq are not identical.");
                result.setErr(AllocResultCodes.ERR_TOTALSEG_NOT_IDENTICAL);
            }

            return result;
        }
    }

    class RuleCheckMissingItem implements RuleJoined<AllocResultCodes>{

        @Override
        public AllocResultCodes check(final List<AllocationItem> items) {
            AllocResultCodes result = new AllocResultCodes(AllocResultCodes.SUCCESS, "");
            final int totalSeg = items.get(0).getTotalSeg();
            Stream<Integer> sorted;

            /*
                Here we dont check whether all of the items have the same totSegment or not because it is
                checked in another rule checker.
                Check whether the number of items equals to totSegment
             */
            if(items.size() != totalSeg) {
                Log.w(TAG, "Number of items check unsuccessful. Item may be missing. " +
                        "Number of item: " + items.size() + " TotalSeg: " + totalSeg);
                result.setErr(AllocResultCodes.ERR_MISSING_ITEM);
            }

//            Log.d(TAG, "Sorted elements:");
            sorted = items.stream().map((item)-> item.getSequence()).sorted();
//            sorted.forEach((seq)->
//                    Log.d(TAG, "seq:" + seq));
            Integer identity = sorted.findFirst().get();
            sorted = items.stream().map((item)-> item.getSequence()).sorted();
            if(sorted.skip(1).reduce(identity,(prev, seq)-> {
                int newSeq = 0;
                Log.d(TAG, "Prev seq:" + prev + " current:" + seq);
                if(prev == seq-1) {
                    newSeq = seq;}
                return newSeq; }) != totalSeg){
                Log.w(TAG, "Seq number check unsuccessful. item may be missing");
                result.setErr(AllocResultCodes.ERR_MISSING_ITEM);
            }

            return result;
        }
    }
//    class RuleCheckSeqNumCrossly implements RuleJoined<Result>{
//
//        @Override
//        public Result check(final List<AllocationItem> items) {
//            Result result = new Result(ResultCode.SUCCESS, "");
//            final int totalSeg = items.get(0).getTotalSeg();
////            final int[] i = {0};
////            IntStream ints;
////            ints = IntStream.generate(()->{
////                int seq = items.get(i[0]).getSequence();
////                i[0]++;
////                return seq;
////            }).limit(items.size());
//
//            if(items.stream().
//                    map((item)-> item.getSequence()).
//                    max(Integer::compareTo).get() != totalSeg){
//                result.setErr(ResultCode.ERR_MAXSEQ_NOTEQUAL_TOTALSEG);
//            }
//
//            return result;
//        }
//    }


}
