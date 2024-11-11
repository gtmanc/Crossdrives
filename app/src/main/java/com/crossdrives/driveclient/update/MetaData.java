package com.crossdrives.driveclient.update;

import com.google.api.services.drive.model.ContentRestriction;

import java.util.List;

public class MetaData {
    private Parents parents = new Parents();
    private List<ContentRestriction> restrictions;

    //Google doesn't allow to add multiple parents to an item staring API v3.
    //Nevertheless, we still keep the list in case that there is change in the future.
    public class Parents{
        public List<String> toRemove;   //parents will be removed
        public List<String> toAdd;   //parents will be added
    }

    public Parents getParents() {
        return parents;
    }

    public List<ContentRestriction> getRestrictions() {
        return restrictions;
    }

    public void setParentsToRemoved(List<String> parents) {
        this.parents.toRemove = parents;
    }

    public void setParentsToAdded(List<String> parents) {
        this.parents.toAdd = parents;
    }
    public void setRestrictions(List<ContentRestriction> restrictions) {
        this.restrictions = restrictions;
    }
}
