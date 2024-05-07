package com.tharaka.ds.cw.sycnhronization;

public interface DistributedTxListener {
    void onGlobalCommit();
    void onGlobalAbort();
}
