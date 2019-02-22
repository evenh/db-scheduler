/**
 * Copyright (C) Gustav Karlsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.kagkarlsson.scheduler;

import java.util.concurrent.atomic.AtomicInteger;

class DueExecutionsBatch {

    private final int generationNumber;
    private final AtomicInteger executionsLeftInBatch;
    private int threadpoolSize;
    private boolean possiblyMoreExecutionsInDb;
    private boolean stale = false;
    private boolean triggeredExecuteDue;

    public DueExecutionsBatch(int threadpoolSize, int generationNumber, int executionsAdded, boolean possiblyMoreExecutionsInDb) {
        this.threadpoolSize = threadpoolSize;
        this.generationNumber = generationNumber;
        this.possiblyMoreExecutionsInDb = possiblyMoreExecutionsInDb;
        this.executionsLeftInBatch = new AtomicInteger(executionsAdded);
    }

    public void markBatchAsStale() {
        this.stale = true;
    }

    /**
     *
     * @param triggerCheckForNewBatch may be triggered more than one in racy conditions
     */
    public void oneExecutionDone(Runnable triggerCheckForNewBatch) {
        executionsLeftInBatch.decrementAndGet();

        if (!stale
                && !triggeredExecuteDue
                && possiblyMoreExecutionsInDb
                && executionsLeftInBatch.get() <= (threadpoolSize * Scheduler.TRIGGER_NEXT_BATCH_WHEN_AVAILABLE_THREADS_RATIO)) {
            triggeredExecuteDue = true;
            triggerCheckForNewBatch.run();
        }
    }

    public boolean isOlderGenerationThan(int compareTo) {
        return generationNumber < compareTo;
    }
}
