package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Checker {
    final String TAG = "CD.Checker";
    List<RuleSingle<Result>> rulesSingle = new ArrayList<>();
    List<RuleJoined<Result>> rulesJoined = new ArrayList<>();

    interface RuleSingle<Result>{
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
    public List<Result> checkAllocationFile(AllocContainer ac){
        List<Result> results = new ArrayList<>();


        return results;
    }

    public List<Result> checkAllocItem(final AllocationItem item){
        List<Result> results = new ArrayList<>();
        rulesSingle.forEach((rule)->{
            results.add(rule.check(item));
            });

        return results;
    }

    public List<Result> checkItemsCrossly(List<AllocationItem> items){
        List<Result> results = new ArrayList<>();
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
    class RuleCheckSeqNum implements RuleSingle<Result> {

        @Override
        public Result check(AllocationItem item) {
            int seq = item.getSequence();
            int totalSeg = item.getTotalSeg();
            Result result = new Result(ResultCode.SUCCESS, "");

            if( seq == 0){
                result.setErr(ResultCode.ERR_SEQ_OVER_SEG);
                result.setReason("Sequence number(SEQ) = " + seq + "An invalid Seq.");
            }
            if (seq > totalSeg) {
                result.setErr(ResultCode.ERR_SEQ_OVER_SEG);
                result.setReason("Sequence number(SEQ) = " + seq + ". However total segment(totalSeg) is " + totalSeg );
            }
            return result;
        }
    }

    class RuleCheckSize implements RuleSingle<Result> {

        @Override
        public Result check(AllocationItem item) {
            long size = item.getSize();
            long maxSize = item.getCDFSItemSize();
            Result result = new Result(ResultCode.SUCCESS, "");

            if (size >= maxSize) {
                result.setErr(ResultCode.ERR_SIZE_OVER_MAX);
                result.setReason("size of item (SIZE) = " + size + ". However maximum size(CDFSitemSize) is " +  maxSize);
            }
            return result;
        }
    }
    /*
        Cross item checks
    */
    class RuleCheckSizeCrossly implements RuleJoined<Result>{

        @Override
        public Result check(List<AllocationItem> items) {
            Result result = new Result(ResultCode.SUCCESS, "SUCCESS");
            final long size = items.get(0).getCDFSItemSize();
            if(!items.stream().allMatch((item) -> {
                return item.getCDFSItemSize() == size;
            })){
                Log.w(TAG, "CDFS Sizes are not identical.");
                result.setErr(ResultCode.ERR_CDFSSIZE_NOT_IDENTICAL);
            }

            return result;
        }
    }

    class RuleCheckTotalSegCrossly implements RuleJoined<Result>{

        @Override
        public Result check(List<AllocationItem> items) {
            Result result = new Result(ResultCode.SUCCESS, "");

            final int totalSeg = items.get(0).getTotalSeg();

            if(!items.stream().allMatch((item) -> {
                return item.getTotalSeg() == totalSeg;
            })) {
                Log.w(TAG, "totalSeq are not identical.");
                result.setErr(ResultCode.ERR_TOTALSEG_NOT_IDENTICAL);
            }

            return result;
        }
    }

    class RuleCheckMissingItem implements RuleJoined<Result>{

        @Override
        public Result check(final List<AllocationItem> items) {
            Result result = new Result(ResultCode.SUCCESS, "");
            final int totalSeg = items.get(0).getTotalSeg();

            /*
                Here we dont check whether all of the items have the same totSegment or not because it is
                checked in another rule checker.
                Check whether the number of items equals to totSegment
             */
            if(items.size() != totalSeg) {
                Log.w(TAG, " Number of items check unsuccessful. Item may be missing. " +
                        "Number of item:" + items.size() + "TotalSeg: " + totalSeg);
                result.setErr(ResultCode.ERR_MISSING_ITEM);
            }

            if(items.stream().map((item)-> item.getSequence()).
                    sorted().skip(1).reduce(items.get(0).getSequence(),(prev, seq)-> {
                int newSeq = 0;
                Log.d(TAG, "Reduce");
                if(prev == seq-1) {
                    newSeq = seq;}
                return newSeq; }) != totalSeg){
                Log.w(TAG, "Seq number check unsuccessful. item may be missing");
                result.setErr(ResultCode.ERR_MISSING_ITEM);
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
