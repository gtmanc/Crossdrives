package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;

import java.util.ArrayList;
import java.util.List;

public class AllocChecker {
    List<Rule> rules = new ArrayList<>();

    interface Rule{
        boolean check(AllocationItem item);
    }

    public AllocChecker() {
        rules.add(new RuleCheckSeqNum());
    }

    boolean checkAllocationFile(AllocContainer ac){
        ac.getAllocItem().forEach((item)->{
            rules.forEach((rule)->{
                rule.check(item);
            });
        });

    }

    boolean checkItemJoin(List<AllocationItem> items){

    }

    class RuleCheckSeqNum implements Rule {

        @Override
        public boolean check(AllocationItem item) {
            int seq = item.getSequence();
            int totalSeg = item.getTotalSeg();
            boolean result = true;

            if (seq >= totalSeg) {
                result = false;
            }
            return result;
        }
    }
}
