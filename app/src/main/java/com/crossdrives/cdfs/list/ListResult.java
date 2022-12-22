package com.crossdrives.cdfs.list;

import com.crossdrives.cdfs.common.ResultCodes;
import com.crossdrives.cdfs.model.CdfsItem;

import java.util.Collection;
import java.util.List;

public class ListResult {

    java.util.List<CdfsItem> items;
    Collection<ListResultCodes> codes;

    class ListResultCodes extends ResultCodes{

    }

    public List<CdfsItem> getItems() {
        return items;
    }

    public Collection<ListResultCodes> getCodes() {
        return codes;
    }

    public void setItems(List<CdfsItem> items) {
        this.items = items;
    }

    public void setCodes(Collection<ListResultCodes> codes) {
        this.codes = codes;
    }
}
