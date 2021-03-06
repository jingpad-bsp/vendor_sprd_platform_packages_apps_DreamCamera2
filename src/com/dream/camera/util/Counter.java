package com.dream.camera.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Counter {

    //private AtomicInteger mCurrentValue;
    private CountDownLatch mCurrentValue ;

    public Counter(int maxValue) {
        mCurrentValue = new CountDownLatch(maxValue);
    }

    public void count() {
        mCurrentValue.countDown();
    }

    public void waitCount() {
        try{
            mCurrentValue.await();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    //Sprd:Fix bug904189
    public void waitCount(long timeout) {
        try {
            mCurrentValue.await(timeout,TimeUnit.MILLISECONDS);
            if (mCurrentValue.getCount() > 0) {
                android.util.Log.i(this.toString(),"wait timeout");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
