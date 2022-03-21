package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;

import java.util.ArrayList;
import java.util.List;

public class AllocChecker {
    List<Rule<Result>> rules = new ArrayList<>();

    interface Rule<Result>{
        Result check(AllocationItem item);
    }

    public AllocChecker() {

        rules.add(new RuleCheckSeqNum());
        rules.add(new RuleCheckSize());
    }

    List<Result> checkAllocationFile(AllocContainer ac){
        List<Result> results = new ArrayList<>();

        ac.getAllocItem().forEach((item)->{
            rules.forEach((rule)->{
                results.add(rule.check(item));
            });
        });

        return results;
    }

    List<Result> checkItemJoin(List<AllocationItem> items){
        List<Result> results = new ArrayList<>();

        return results;
    }

    /*
        Single item checks
     */
    class RuleCheckSeqNum implements Rule<Result> {

        @Override
        public Result check(AllocationItem item) {
            int seq = item.getSequence();
            int totalSeg = item.getTotalSeg();
            Result result = new Result(ResultCode.SUCCESS, "");

            if (seq >= totalSeg) {
                result.setErr(ResultCode.ERR_SEQ_OVER_SEG);
                result.setReason("Sequence number(SEQ) = " + seq + ". However total segment(totalSeg) is " + totalSeg );
            }
            return result;
        }
    }

    class RuleCheckSize implements Rule<Result>{

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
}
