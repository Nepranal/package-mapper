package com.mizookie.packagemapper;

import java.util.concurrent.Semaphore;

public abstract class Task extends Thread {
    static int N;
    static Semaphore readerSemaphore = new Semaphore(0);
    static Semaphore producerSemaphore = new Semaphore(N);
}
