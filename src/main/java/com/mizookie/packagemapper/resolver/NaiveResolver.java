package com.mizookie.packagemapper.resolver;

import com.mizookie.packagemapper.utils.FileService;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NaiveResolver {
    static private int N;
    private final ArrayList<Task> threads = new ArrayList<>();

    public NaiveResolver(int numberOfThreads) {
        N = numberOfThreads;
        for (int i = 0; i < N; ++i) {
            Task t = new Task(i);
            threads.add(t);
            t.start();
        }
    }

    public List<String> solve(List<String> filePaths, String filePath) throws FileNotFoundException {
        // setup
        Task.filePaths = filePaths;
        Task.results = new ArrayList<>();
        int numberOfFiles = Task.filePaths.size();
        int division = (int) Math.ceil(numberOfFiles / (1.0 * N));
        for (int i = 0; i < N; ++i) {
            Task t = threads.get(i);
            t.setStartPoint(i * division);
            t.setEndPoint((division == 1) ?
                    Math.min(numberOfFiles + 1, i + 1) % (numberOfFiles + 1) - 1
                    : Math.min(numberOfFiles, (i + 1) * division) - 1);
        }

        // Producer stuff here
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Task.producerSemaphore.acquire(N);
                Task.currentFilePath = filePath;
                Task.line = line;
                Task.readerSemaphore.release(N);
            }
            Task.producerSemaphore.acquire(N);
            Task.line = null;
            Task.currentFilePath = null;
            Task.readerSemaphore.release(N);
            return Task.results;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Reader - see analyse()
    public class Task extends Thread {
        static List<String> results = new ArrayList<>();
        static List<String> filePaths;
        static String currentFilePath;
        static Semaphore readerSemaphore = new Semaphore(0);
        static Semaphore producerSemaphore = new Semaphore(N);
        static Semaphore resultLock = new Semaphore(1);
        static Lock doneProcessingLock = new ReentrantLock();
        static String line;
        static int[] doneProcessing = {0, 0};
        static Condition[] doneProcessingConditions = {doneProcessingLock.newCondition(), doneProcessingLock.newCondition()};
        ArrayList<Integer> range = new ArrayList<>();
        private int id;
        private int startPoint, endPoint, found = 0, round = 1;

        Task(int id) {
            this.id = id;
        }

        public void run() {
            System.out.println("Naive resolver threads active");
            String line, currentFileName = null;
            while (true) {
                try {
                    readerSemaphore.acquire();
                    // New line available
                    if (currentFileName != null && !currentFileName.equals(Task.currentFilePath)) {
                        initState();
                    }
                    currentFileName = Task.currentFilePath; // Copy current file name
                    line = Task.line; // Copy line
                    producerSemaphore.release();
                    if (line != null) {
                        processLine(line, currentFileName);
                    }

                    doneProcessingLock.lock();
                    doneProcessing[round] += 1;
                    while (doneProcessing[round] < N) {
                        doneProcessingConditions[round].await();
                        doneProcessing[round] = N;
                    }
                    doneProcessingConditions[round].signalAll();
                    doneProcessing[round] = 0;
                    doneProcessingLock.unlock();
                    round = (round + 1) % 2;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void initState() {
            found = 0;
            range = new ArrayList<>();
            for (int i = startPoint; i <= endPoint; ++i) {
                range.add(i);
            }
        }

        private void processLine(String line, String currentFilePath) throws InterruptedException {
            ArrayList<String> temp = new ArrayList<>();
            for (int i = found; i < range.size(); ++i) {
                String filePath = filePaths.get(range.get(i));
                if (filePath.equals(currentFilePath)
                        && line.matches(String.format(".*\\b(%s)\\b.*", FileService.getFileNameWithoutExtension(filePath)))) {
                    temp.add(filePaths.get(range.get(i)));
                    Collections.swap(range, found, i);
                    found += 1;
                }
            }
            resultLock.acquire();
            results.addAll(temp);
            resultLock.release();
        }

        public void setStartPoint(int startPoint) {
            this.startPoint = startPoint;
        }

        public void setEndPoint(int endPoint) {
            this.endPoint = endPoint;
        }
    }
}
