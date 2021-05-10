#include <sys/mman.h>
#include <linux/android/binder.h>
#include "data_transact.h"

int open_driver() {
    int fd = open("/dev/binder", O_RDWR | O_CLOEXEC);
    if (fd >= 0) {
        int vers = 0;
        status_t result = ioctl(fd, BINDER_VERSION, &vers);
        if (result == -1) {
            LOGE("Binder ioctl to obtain version failed: %s", strerror(errno));
            close(fd);
            fd = -1;
        }
        if (result != 0 || vers != BINDER_CURRENT_PROTOCOL_VERSION) {
            LOGE("Binder driver protocol(%d) does not match user space protocol(%d)! ioctl() return value: %d",
                 vers, BINDER_CURRENT_PROTOCOL_VERSION, result);
            close(fd);
            fd = -1;
        }
        size_t maxThreads = DEFAULT_MAX_BINDER_THREADS;
        result = ioctl(fd, BINDER_SET_MAX_THREADS, &maxThreads);
        if (result == -1) {
            LOGE("Binder ioctl to set max threads failed: %s", strerror(errno));
        }
    } else {
        LOGE("Opening '%s' failed: %s\n", "/dev/binder", strerror(errno));
    }
    return fd;
}


void initProcessState(int mDriverFD, void *mVMStart) {
    if (mDriverFD >= 0) {
        // mmap the binder, providing a chunk of virtual address space to receive transactions.
        mVMStart = mmap(0, BINDER_VM_SIZE, PROT_READ, MAP_PRIVATE | MAP_NORESERVE, mDriverFD, 0);
        if (mVMStart == MAP_FAILED) {
            // *sigh*
            LOGE("Using /dev/binder failed: unable to mmap transaction memory.\n");
            close(mDriverFD);
            mDriverFD = -1;
        }
    }
}

void unInitProcessState(int mDriverFD, void *mVMStart) {
    if (mDriverFD >= 0) {
        if (mVMStart != MAP_FAILED) {
            munmap(mVMStart, BINDER_VM_SIZE);
        }
        close(mDriverFD);
    }
    mDriverFD = -1;
}

status_t talkWithDriver(bool doReceive, int mDriverFD, Parcel &mOut, Parcel &mIn) {
    if (mDriverFD <= 0) {
        return -EBADF;
    }
    if (mOut.dataSize() > 0) {
        LOGD("talkWithDriver1 %lu %lu", mOut.dataSize(), mIn.dataSize());
    }

    binder_write_read bwr;

    // Is the read buffer empty?
    const bool needRead = mIn.dataPosition() >= mIn.dataSize();

    // We don't want to write anything if we are still reading
    // from data left in the input buffer and the caller
    // has requested to read the next data.
    const size_t outAvail = (!doReceive || needRead) ? mOut.dataSize() : 0;
    if (outAvail > 0) {
        LOGD("talkWithDriver1 outAvail %lu %lu", mOut.dataSize(), mIn.dataSize());
    }
    if (mIn.dataSize() > 0) {
        LOGD("talkWithDriver1 inAvail %lu %lu", mOut.dataSize(), mIn.dataSize());
    }
//    LOGD("outAvail=%d,%d",outAvail,mOut.dataSize());
    bwr.write_size = outAvail;
    bwr.write_buffer = (long unsigned int) mOut.data();

    // This is what we'll read.
    if (doReceive && needRead) {
        bwr.read_size = mIn.dataCapacity();
        bwr.read_buffer = (long unsigned int) mIn.data();
    } else {
        bwr.read_size = 0;
        bwr.read_buffer = 0;
    }

//    LOGD("Sending commands to driver len=%lld", bwr.write_size);
//    LOGD("Size of receive buffer: %lld, needRead: %d, doReceive: %d", bwr.read_size, needRead,
//         doReceive);

    // Return immediately if there is nothing to do.
    if ((bwr.write_size == 0) && (bwr.read_size == 0)) return NO_ERROR;

    bwr.write_consumed = 0;
    bwr.read_consumed = 0;
    status_t err;
    do {
//        LOGD("About to read/write, write size = %lu", mOut.dataSize());
        int ret = 0;
        if ((ret = ioctl(mDriverFD, BINDER_WRITE_READ, &bwr)) >= 0)
            err = NO_ERROR;
        else
            err = -errno;

        if (mDriverFD <= 0) {
            err = -EBADF;
        }
        LOGD("\"Finished read/write, write size = %lu ret=%d", mOut.dataSize(), ret);
    } while (err == -EINTR);

    LOGD("Our err: %d, write consumed: %lld (of %lu), read consumed: %lld", err, bwr.write_consumed,
         mOut.dataSize(), bwr.read_consumed);

    if (err >= NO_ERROR) {
        if (bwr.write_consumed > 0) {
            if (bwr.write_consumed < mOut.dataSize())
                mOut.remove(0, bwr.write_consumed);
            else
                mOut.setDataSize(0);
        }
        if (bwr.read_consumed > 0) {
            mIn.setDataSize(bwr.read_consumed);
            mIn.setDataPosition(0);
        }

        LOGD("Remaining data size: %lu", mOut.dataSize());
        return NO_ERROR;
    }

    return err;
}

status_t
waitForResponse(Parcel *reply, status_t *acquireResult, int mDriverFD, Parcel &mOut, Parcel &mIn) {
    uint32_t cmd;
    int32_t err;
    LOGD("waitForResponse %lu %lu", mOut.dataSize(), mIn.dataSize());

    LOGD("BR_TRANSACTION_COMPLETE %d", BR_TRANSACTION_COMPLETE);
    LOGD("BR_DEAD_REPLY %d", BR_DEAD_REPLY);
    LOGD("BR_FAILED_REPLY %d", BR_FAILED_REPLY);
    LOGD("BR_ACQUIRE_RESULT %d", BR_ACQUIRE_RESULT);
    LOGD("BR_REPLY %d", BR_REPLY);
    LOGD("BR_ERROR %d", BR_ERROR);
    LOGD("BR_OK %d", BR_OK);
    LOGD("BR_TRANSACTION %d", BR_TRANSACTION);
    LOGD("BR_INCREFS %d", BR_INCREFS);
    LOGD("BR_NOOP %d", BR_NOOP);
    LOGD("BR_SPAWN_LOOPER %d", BR_SPAWN_LOOPER);
    LOGD("BR_FINISHED %d", BR_FINISHED);
    LOGD("BR_DEAD_BINDER %d", BR_DEAD_BINDER);
    LOGD("BR_CLEAR_DEATH_NOTIFICATION_DONE %d", BR_CLEAR_DEATH_NOTIFICATION_DONE);
    LOGD("BR_FAILED_REPLY %d", BR_FAILED_REPLY);

//    err = talkWithDriver(false, mDriverFD, mOut, mIn);
//    LOGD("talkWithDriver %d", err);
    while (1) {
        if ((err = talkWithDriver(true, mDriverFD, mOut, mIn)) < NO_ERROR) break;
//        err = mIn.errorCheck();
//        if (err < NO_ERROR) break;
        if (mIn.dataAvail() == 0) continue;

        cmd = mIn.readInt32();
        LOGD("Processing waitForResponse Command: %d %lu", cmd, mIn.dataSize());

        switch (cmd) {
            case BR_TRANSACTION_COMPLETE:
                LOGD("BR_TRANSACTION_COMPLETE");
                if (!reply && !acquireResult) goto finish;
                LOGD("bingo!");
                break;

            case BR_DEAD_REPLY:
                LOGD("BR_DEAD_REPLY");
                err = DEAD_OBJECT;
                goto finish;

            case BR_FAILED_REPLY:
                LOGD("BR_FAILED_REPLY");
                err = FAILED_TRANSACTION;
                goto finish;

            case BR_ACQUIRE_RESULT: {
                LOGD("BR_ACQUIRE_RESULT");
//                ALOG_ASSERT(acquireResult != NULL, "Unexpected brACQUIRE_RESULT");
                const int32_t result = mIn.readInt32();
                if (!acquireResult) continue;
                *acquireResult = result ? NO_ERROR : INVALID_OPERATION;
            }
                goto finish;

            case BR_REPLY: {
                LOGD("BR_REPLY");
                binder_transaction_data tr;
                err = mIn.read(&tr, sizeof(tr));
                LOGD("BR_REPLY handle = %d", tr.target.handle);
//                ALOG_ASSERT(err == NO_ERROR, "Not enough command data for brREPLY");
                if (err != NO_ERROR) goto finish;

                if (reply) {
                    LOGD("ipcSetDataReference data size=%lld", tr.data_size);
                    if ((tr.flags & TF_STATUS_CODE) == 0) {
                        reply->ipcSetDataReference(
                                reinterpret_cast<const uint8_t *>(tr.data.ptr.buffer),
                                tr.data_size,
                                reinterpret_cast<const binder_size_t *>(tr.data.ptr.offsets),
                                tr.offsets_size / sizeof(size_t),
                                freeBuffer, NULL);
                    } else {
                        err = *(const status_t *) (tr.data.ptr.buffer);
                        freeBuffer(NULL,
                                   reinterpret_cast<const uint8_t *>(tr.data.ptr.buffer),
                                   tr.data_size,
                                   reinterpret_cast<const binder_size_t *>(tr.data.ptr.offsets),
                                   tr.offsets_size / sizeof(size_t), NULL);
                    }
                } else {
                    freeBuffer(NULL,
                               reinterpret_cast<const uint8_t *>(tr.data.ptr.buffer),
                               tr.data_size,
                               reinterpret_cast<const binder_size_t *>(tr.data.ptr.offsets),
                               tr.offsets_size / sizeof(size_t), NULL);
                    continue;
                }
            }
                goto finish;

            default:
                err = executeCommand(cmd, mIn, mOut);
                LOGE("executeCommand err=%d", err);
                if (err != NO_ERROR) goto finish;
                break;
        }
    }

    finish:
    if (err != NO_ERROR) {
        if (acquireResult) *acquireResult = err;
        if (reply) reply->setError(err);
//        mLastError = err;
    }
    return err;
}

status_t writeTransactionData(int32_t cmd, uint32_t binderFlags,
                              int32_t handle, uint32_t code, const Parcel &data, Parcel &mOut,
                              status_t *statusBuffer) {
    binder_transaction_data tr;

    tr.target.handle = handle;
    tr.code = code;
    tr.flags = binderFlags;
    tr.cookie = 0;
    tr.sender_pid = 0;
    tr.sender_euid = 0;

    const status_t err = data.errorCheck();
    LOGD("errorCheck %d", err);
    if (err == NO_ERROR) {
        tr.data_size = data.ipcDataSize();
        tr.data.ptr.buffer = (uintptr_t) data.ipcData();
        tr.offsets_size = data.ipcObjectsCount() * sizeof(binder_size_t);
        tr.data.ptr.offsets = (uintptr_t) data.ipcObjects();
    } else if (statusBuffer) {
        tr.flags |= TF_STATUS_CODE;
        *statusBuffer = err;
        tr.data_size = sizeof(status_t);
        tr.data.ptr.buffer = (binder_uintptr_t) statusBuffer;
        tr.offsets_size = 0;
        tr.data.ptr.offsets = 0;
    } else {
        return err;
    }

    mOut.writeInt32(cmd);
    mOut.write(&tr, sizeof(tr));

    return NO_ERROR;
}

status_t
write_transact(int32_t handle, uint32_t code, const Parcel &data, Parcel *reply, uint32_t flags,
               int driverFD) {
    flags |= TF_ACCEPT_FDS;
    status_t err = data.errorCheck();
    Parcel *mOut = new Parcel;
    mOut->setDataCapacity(256);
    err = writeTransactionData(BC_TRANSACTION, flags, handle, code, data, *mOut, NULL);
    LOGD("writeTransactionData %lu %lu", data.dataSize(), mOut->dataSize());
    if (err != NO_ERROR) {
        LOGE("writeTransactionData error occurred: %s, %d,%d", strerror(errno), errno, err);
        delete mOut;
        return err;
    }

    Parcel *mIn = new Parcel;
    mIn->setDataCapacity(256);
//    uint8_t data_in[128];
//    mIn.setData(data_in, 128);
//    mIn.setDataCapacity(128);
//    mIn.setDataPosition(0);
    if ((flags & TF_ONE_WAY) == 0) {
        if (reply) { // Parcel *reply, status_t *acquireResult, int mDriverFD, Parcel &mOut, Parcel &mIn
            err = waitForResponse(reply, NULL, driverFD, *mOut, *mIn);
        } else {
            Parcel fakeReply;
            err = waitForResponse(&fakeReply, NULL, driverFD, *mOut, *mIn);
        }
    } else {
        err = waitForResponse(NULL, NULL, driverFD, *mOut, *mIn);
    }
    delete mIn;
    delete mOut;
    return err;
}


status_t executeCommand(uint32_t cmd, Parcel &mIn, Parcel &mOut) {
//    BBinder* obj;
    int32_t obj;
//    RefBase::weakref_type* refs;
    int32_t refs;
    status_t result = NO_ERROR;

    switch (cmd) {
        case BR_ERROR:
            LOGD("BR_ERROR");
            result = mIn.readInt32();
            break;

        case BR_OK:
            LOGD("BR_OK");
            break;

        case BR_ACQUIRE:
            LOGD("BR_ACQUIRE");
//            refs = (RefBase::weakref_type*)mIn.readPointer();
            refs = mIn.readInt32();
//            obj = (BBinder*)mIn.readPointer();
            obj = (int32_t) (mIn.readInt32());
//            ALOG_ASSERT(refs->refBase() == obj,
//                        "BR_ACQUIRE: object %p does not match cookie %p (expected %p)",
//                        refs, obj, refs->refBase());
//            obj->incStrong(mProcess.get());
//            IF_LOG_REMOTEREFS() {
//        LOG_REMOTEREFS("BR_ACQUIRE from driver on %p", obj);
//        obj->printRefs();
//    }
            mOut.writeInt32(BC_ACQUIRE_DONE);
            mOut.writeInt32((int32_t) refs);
            mOut.writeInt32((int32_t) obj);
            break;

        case BR_RELEASE:
            LOGD("BR_RELEASE");
            refs = (int32_t) mIn.readInt32();
            obj = (int32_t) mIn.readInt32();
//            ALOG_ASSERT(refs->refBase() == obj,
//                        "BR_RELEASE: object %p does not match cookie %p (expected %p)",
//                        refs, obj, refs->refBase());
//            IF_LOG_REMOTEREFS() {
//        LOG_REMOTEREFS("BR_RELEASE from driver on %p", obj);
//        obj->printRefs();
//    }
//            mPendingStrongDerefs.push(obj);
            break;

        case BR_INCREFS:
            LOGD("BR_INCREFS");
            refs = (int32_t) mIn.readInt32();
            obj = (int32_t) mIn.readInt32();
//            refs->incWeak(mProcess.get());
            mOut.writeInt32(BC_INCREFS_DONE);
            mOut.writeInt32((int32_t) refs);
            mOut.writeInt32((int32_t) obj);
            break;

        case BR_DECREFS:
            LOGD("BR_DECREFS");
            refs = (int32_t) mIn.readInt32();
            obj = (int32_t) mIn.readInt32();
            // NOTE: This assertion is not valid, because the object may no
            // longer exist (thus the (BBinder*)cast above resulting in a different
            // memory address).
            //ALOG_ASSERT(refs->refBase() == obj,
            //           "BR_DECREFS: object %p does not match cookie %p (expected %p)",
            //           refs, obj, refs->refBase());
//            mPendingWeakDerefs.push(refs);
            break;

        case BR_ATTEMPT_ACQUIRE:
            LOGD("BR_ATTEMPT_ACQUIRE");
            refs = (int32_t) mIn.readInt32();
            obj = (int32_t) mIn.readInt32();

            {
//                const bool success = refs->attemptIncStrong(mProcess.get());
//                ALOG_ASSERT(success && refs->refBase() == obj,
//                            "BR_ATTEMPT_ACQUIRE: object %p does not match cookie %p (expected %p)",
//                            refs, obj, refs->refBase());

                mOut.writeInt32(BC_ACQUIRE_RESULT);
                mOut.writeInt32((int32_t) true);
//                mOut.writeInt32((int32_t)success);
            }
            break;

        case BR_TRANSACTION: {
            LOGD("BR_TRANSACTION");
            binder_transaction_data tr;
            result = mIn.read(&tr, sizeof(tr));
//            ALOG_ASSERT(result == NO_ERROR,
//                        "Not enough command data for brTRANSACTION");
            if (result != NO_ERROR) break;

            Parcel buffer;
//            buffer.ipcSetDataReference(
//                    reinterpret_cast<const uint8_t*>(tr.data.ptr.buffer),
//                    tr.data_size,
//                    reinterpret_cast<const binder_size_t*>(tr.data.ptr.offsets),
//                    tr.offsets_size/sizeof(binder_size_t), freeBuffer, this);

//            const pid_t origPid = mCallingPid;
//            const uid_t origUid = mCallingUid;
//            const int32_t origStrictModePolicy = mStrictModePolicy;
//            const int32_t origTransactionBinderFlags = mLastTransactionBinderFlags;

//            mCallingPid = tr.sender_pid;
//            mCallingUid = tr.sender_euid;
//            mLastTransactionBinderFlags = tr.flags;

//            int curPrio = getpriority(PRIO_PROCESS, mMyThreadId);

//            if (gDisableBackgroundScheduling) {
//                if (curPrio > ANDROID_PRIORITY_NORMAL) {
//                    // We have inherited a reduced priority from the caller, but do not
//                    // want to run in that state in this process.  The driver set our
//                    // priority already (though not our scheduling class), so bounce
//                    // it back to the default before invoking the transaction.
//                    setpriority(PRIO_PROCESS, mMyThreadId, ANDROID_PRIORITY_NORMAL);
//                }
//            } else {
//                if (curPrio >= ANDROID_PRIORITY_BACKGROUND) {
//                    // We want to use the inherited priority from the caller.
//                    // Ensure this thread is in the background scheduling class,
//                    // since the driver won't modify scheduling classes for us.
//                    // The scheduling group is reset to default by the caller
//                    // once this method returns after the transaction is complete.
//                    set_sched_policy(mMyThreadId, SP_BACKGROUND);
//                }
//            }

            //ALOGI(">>>> TRANSACT from pid %d uid %d\n", mCallingPid, mCallingUid);

            Parcel reply;
            status_t error;
//            IF_LOG_TRANSACTIONS() {
//                TextOutput::Bundle _b(alog);
//                alog << "BR_TRANSACTION thr " << (void*)pthread_self()
//                     << " / obj " << tr.target.ptr << " / code "
//                     << TypeCode(tr.code) << ": " << indent << buffer
//                     << dedent << endl
//                     << "Data addr = "
//                     << reinterpret_cast<const uint8_t*>(tr.data.ptr.buffer)
//                     << ", offsets addr="
//                     << reinterpret_cast<const size_t*>(tr.data.ptr.offsets) << endl;
//            }
//            if (tr.target.ptr) {
//                // We only have a weak reference on the target object, so we must first try to
//                // safely acquire a strong reference before doing anything else with it.
//                if (reinterpret_cast<RefBase::weakref_type*>(
//                        tr.target.ptr)->attemptIncStrong(this)) {
//                    error = reinterpret_cast<BBinder*>(tr.cookie)->transact(tr.code, buffer,
//                                                                            &reply, tr.flags);
//                    reinterpret_cast<BBinder*>(tr.cookie)->decStrong(this);
//                } else {
//                    error = UNKNOWN_TRANSACTION;
//                }
//
//            } else {
//                error = the_context_object->transact(tr.code, buffer, &reply, tr.flags);
//            }

            //ALOGI("<<<< TRANSACT from pid %d restore pid %d uid %d\n",
            //     mCallingPid, origPid, origUid);

//            if ((tr.flags & TF_ONE_WAY) == 0) {
//                LOG_ONEWAY("Sending reply to %d!", mCallingPid);
//                if (error < NO_ERROR) reply.setError(error);
//                sendReply(reply, 0); // TODO
//            } else {
//                LOG_ONEWAY("NOT sending reply to %d!", mCallingPid);
//            }
//
//            mCallingPid = origPid;
//            mCallingUid = origUid;
//            mStrictModePolicy = origStrictModePolicy;
//            mLastTransactionBinderFlags = origTransactionBinderFlags;
//
//            IF_LOG_TRANSACTIONS() {
//                TextOutput::Bundle _b(alog);
//                alog << "BC_REPLY thr " << (void*)pthread_self() << " / obj "
//                     << tr.target.ptr << ": " << indent << reply << dedent << endl;
//            }

        }
            break;

        case BR_DEAD_BINDER: {
            LOGD("BR_DEAD_BINDER");
//            BpBinder *proxy = (BpBinder*)mIn.readInt32();
            int32_t proxy = mIn.readInt32();
//            proxy->sendObituary();
            mOut.writeInt32(BC_DEAD_BINDER_DONE);
            mOut.writeInt32((int32_t) proxy);
        }
            break;

        case BR_CLEAR_DEATH_NOTIFICATION_DONE: {
            LOGD("BR_CLEAR_DEATH_NOTIFICATION_DONE");
//            BpBinder *proxy = (BpBinder*)mIn.readInt32();
            int32_t proxy = mIn.readInt32();
//            proxy->getWeakRefs()->decWeak(proxy);
        }
            break;

        case BR_FINISHED:
            LOGD("BR_FINISHED");
            result = TIMED_OUT;
            break;

        case BR_NOOP:
            LOGD("BR_NOOP");
            break;

        case BR_SPAWN_LOOPER:
            LOGD("BR_SPAWN_LOOPER");
//            mProcess->spawnPooledThread(false);
            break;

        default:
            printf("*** BAD COMMAND %d received from Binder driver\n", cmd);
            result = UNKNOWN_ERROR;
            break;
    }

    if (result != NO_ERROR) {
//        mLastError = result;
    }

    return result;
}

void freeBuffer(Parcel *parcel,
                const uint8_t *data, size_t /*dataSize*/,
                const binder_size_t * /*objects*/, size_t /*objectsSize*/,
                void * /*cookie*/) {
    //ALOGI("Freeing parcel %p", &parcel);
    LOGD("Writing BC_FREE_BUFFER for %p", data);
    if (parcel != NULL) parcel->closeFileDescriptors();
//    IPCThreadState* state = self();
//    state->mOut.writeInt32(BC_FREE_BUFFER);
//    state->mOut.writePointer((uintptr_t)data);
}
