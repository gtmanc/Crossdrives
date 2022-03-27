package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AllocChecker {
    List<RuleSingle<Result>> rulesSingle = new ArrayList<>();
    List<RuleJoined<Result>> rulesJoined = new ArrayList<>();

    interface RuleSingle<Result>{
        Result check(AllocationItem item);
    }

    interface RuleJoined<Result>{
        Result check(List<AllocationItem> items);
    }

    public AllocChecker() {

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

    public List<Result> checkItems(List<AllocationItem> items){
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

    class RuleCheckSizeCrossly implements RuleJoined<Result>{

        @Override
        public Result check(List<AllocationItem> items) {
            Result result = new Result(ResultCode.ERR_CDFSSIZE_NOT_IDENTICAL, "");
            final long size = items.get(0).getCDFSItemSize();
            if(items.stream().allMatch((item)->{
                return item.getCDFSItemSize() == size;
            }) == false){
                result.setErr(ResultCode.SUCCESS);
            }

            return result;
        }
    }

    class RuleCheckTotalSegCrossly implements RuleJoined<Result>{

        @Override
        public Result check(List<AllocationItem> items) {
            Result result = new Result(ResultCode.ERR_TOTALSEG_NOT_IDENTICAL, "");

            final int totalSeg = items.get(0).getTotalSeg();
            /*
                The CDFS size and Total seg must be identical
             */
            if(items.stream().allMatch((item)-> {
                return item.getTotalSeg() == totalSeg;
            }) == true) {result.setErr(ResultCode.SUCCESS);}
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

    class RuleCheckMissingItem implements RuleJoined<Result>{

        @Override
        public Result check(final List<AllocationItem> items) {
            Result result = new Result(ResultCode.SUCCESS, "");
            final int totalSeg = items.get(0).getTotalSeg();

            if(items.stream().map((item)-> item.getSequence()).
                    sorted().skip(1).reduce(items.get(0).getSequence(),(prev, seq)-> {
                int newSeq = 0;
                if(prev == seq-1) {
                    newSeq = seq;}
                return newSeq; }) != totalSeg){
                result.setErr(ResultCode.ERR_MISSING_ITEM);
            }

            return result;
        }
    }
}
