/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Android Open Source Project WakeLock implementation (2007), Apache2.0.
 * Extracted verbatim from frameworks/base android-2.1_r2 PowerManager.java.
 * Only outer Binder/service/handler collaborators below are test stubs. */
package cl.balocchi.nookpanel;
import java.util.*;
public class WakeLockApi7Test {
 static final int LOCK_MASK=0xffff,PARTIAL_WAKE_LOCK=1,SCREEN_DIM_WAKE_LOCK=6,SCREEN_BRIGHT_WAKE_LOCK=10,FULL_WAKE_LOCK=26,PROXIMITY_SCREEN_OFF_WAKE_LOCK=32;
 static final String TAG="Test";
 interface IBinder {} static class Binder implements IBinder {}
 static class RemoteException extends Exception {}
 static class RuntimeInit {static void crash(String tag,Exception e){throw new AssertionError(e);}}
 static class FakeService {Set<IBinder> held=new HashSet<IBinder>();void acquireWakeLock(int flags,IBinder token,String tag)throws RemoteException{held.add(token);}void releaseWakeLock(IBinder token,int flags)throws RemoteException{held.remove(token);}}
 static class FakeHandler {List<Runnable> queued=new ArrayList<Runnable>();void postDelayed(Runnable r,long delay){queued.add(r);}void runAll(){List<Runnable> copy=new ArrayList<Runnable>(queued);queued.clear();for(Runnable r:copy)r.run();}}
 FakeService mService=new FakeService();FakeHandler mHandler=new FakeHandler();
    public class WakeLock
    {
        static final int RELEASE_WAKE_LOCK = 1;

        Runnable mReleaser = new Runnable() {
            public void run() {
                release();
            }
        };
	
        int mFlags;
        String mTag;
        IBinder mToken;
        int mCount = 0;
        boolean mRefCounted = true;
        boolean mHeld = false;

        WakeLock(int flags, String tag)
        {
            switch (flags & LOCK_MASK) {
            case PARTIAL_WAKE_LOCK:
            case SCREEN_DIM_WAKE_LOCK:
            case SCREEN_BRIGHT_WAKE_LOCK:
            case FULL_WAKE_LOCK:
            case PROXIMITY_SCREEN_OFF_WAKE_LOCK:
                break;
            default:
                throw new IllegalArgumentException();
            }

            mFlags = flags;
            mTag = tag;
            mToken = new Binder();
        }

        /**
         * Sets whether this WakeLock is ref counted.
         *
         * @param value true for ref counted, false for not ref counted.
         */
        public void setReferenceCounted(boolean value)
        {
            mRefCounted = value;
        }

        /**
         * Makes sure the device is on at the level you asked when you created
         * the wake lock.
         */
        public void acquire()
        {
            synchronized (mToken) {
                if (!mRefCounted || mCount++ == 0) {
                    try {
                        mService.acquireWakeLock(mFlags, mToken, mTag);
                    } catch (RemoteException e) {
                    }
                    mHeld = true;
                }
            }
        }
        
        /**
         * Makes sure the device is on at the level you asked when you created
         * the wake lock. The lock will be released after the given timeout.
         * 
         * @param timeout Release the lock after the give timeout in milliseconds.
         */
        public void acquire(long timeout) {
            acquire();
            mHandler.postDelayed(mReleaser, timeout);
        }
        

        /**
         * Release your claim to the CPU or screen being on.
         *
         * <p>
         * It may turn off shortly after you release it, or it may not if there
         * are other wake locks held.
         */
        public void release()
        {
            release(0);
        }

        /**
         * Release your claim to the CPU or screen being on.
         * @param flags Combination of flag values to modify the release behavior.
         *              Currently only {@link #WAIT_FOR_PROXIMITY_NEGATIVE} is supported.
         *
         * <p>
         * It may turn off shortly after you release it, or it may not if there
         * are other wake locks held.
         *
         * {@hide}
         */
        public void release(int flags)
        {
            synchronized (mToken) {
                if (!mRefCounted || --mCount == 0) {
                    try {
                        mService.releaseWakeLock(mToken, flags);
                    } catch (RemoteException e) {
                    }
                    mHeld = false;
                }
                if (mCount < 0) {
                    throw new RuntimeException("WakeLock under-locked " + mTag);
                }
            }
        }

        public boolean isHeld()
        {
            synchronized (mToken) {
                return mHeld;
            }
        }

        public String toString() {
            synchronized (mToken) {
                return "WakeLock{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " held=" + mHeld + ", refCount=" + mCount + "}";
            }
        }

        @Override
        protected void finalize() throws Throwable
        {
            synchronized (mToken) {
                if (mHeld) {
                    try {
                        mService.releaseWakeLock(mToken, 0);
                    } catch (RemoteException e) {
                    }
                    RuntimeInit.crash(TAG, new Exception(
                                "WakeLock finalized while still held: "+mTag));
                }
            }
        }
    }
 public static void main(String[] args){
  WakeLockApi7Test old=new WakeLockApi7Test();WakeLock oldLock=old.new WakeLock(PARTIAL_WAKE_LOCK,"NookPanel:Update");oldLock.acquire(180000L);oldLock.release();boolean reproduced=false;
  try{old.mHandler.runAll();}catch(RuntimeException e){reproduced=e.getMessage().startsWith("WakeLock under-locked");}
  if(!reproduced)throw new AssertionError("Original device crash was not reproduced");
  int checks=0;
  for(int mode=0;mode<3;mode++)for(int repeat=0;repeat<50;repeat++){
   WakeLockApi7Test fixed=new WakeLockApi7Test();WakeLock lock=fixed.new WakeLock(PARTIAL_WAKE_LOCK,"NookPanel:Update");lock.setReferenceCounted(false);lock.acquire(180000L);
   if(mode==0){if(lock.isHeld())lock.release();fixed.mHandler.runAll();}
   else if(mode==1){fixed.mHandler.runAll();if(lock.isHeld())lock.release();}
   else{boolean held=lock.isHeld();fixed.mHandler.runAll();if(held)lock.release();}
   if(lock.isHeld()||!fixed.mService.held.isEmpty())throw new AssertionError("Lock leak");checks++;
  }
  WakeLockApi7Test overlap=new WakeLockApi7Test();WakeLock first=overlap.new WakeLock(PARTIAL_WAKE_LOCK,"NookPanel:Update"),second=overlap.new WakeLock(PARTIAL_WAKE_LOCK,"NookPanel:Update");first.setReferenceCounted(false);second.setReferenceCounted(false);first.acquire(180000L);first.release();second.acquire(180000L);overlap.mHandler.queued.remove(0).run();
  if(!second.isHeld()||!overlap.mService.held.contains(second.mToken))throw new AssertionError("An old callback released the next operation's lock");second.release();overlap.mHandler.runAll();if(!overlap.mService.held.isEmpty())throw new AssertionError("Overlap leak");checks++;
  System.out.println("Exact API7 WakeLock: original delayed-release crash reproduced; non-ref-counted release permutations passed "+checks+" times, no held tokens");
 }
}
