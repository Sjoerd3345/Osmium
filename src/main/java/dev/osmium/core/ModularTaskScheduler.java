package dev.osmium.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ModularTaskScheduler — a work queue that respects the frame budget.
 *
 * Instead of doing all work immediately (which causes spikes),
 * systems submit tasks here. Each tick, we run as many tasks as
 * the frame budget allows, and defer the rest to the next tick.
 *
 * How to use:
 *   // Submit a task from anywhere:
 *   OsmiumClient.SCHEDULER.submit("chunk-update", 0.5, () -> updateChunk(pos));
 *
 *   // The scheduler runs it when there's budget available.
 */
public class ModularTaskScheduler {

    private final FrameBudgetController budget;

    // The queue of pending tasks. ArrayDeque is fast for add/remove at both ends.
    private final Deque<ScheduledTask> taskQueue = new ArrayDeque<>();

    // How many tasks we ran last tick — useful for debugging.
    private int lastTickRanCount = 0;
    private int lastTickDeferredCount = 0;

    public ModularTaskScheduler(FrameBudgetController budget) {
        this.budget = budget;
    }

    /**
     * Submit a task to be run when there's budget available.
     *
     * @param name           human-readable name (for debugging)
     * @param estimatedCostMs how expensive this task is, in milliseconds
     * @param task           the actual code to run (a lambda)
     */
    public void submit(String name, double estimatedCostMs, Runnable task) {
        taskQueue.addLast(new ScheduledTask(name, estimatedCostMs, task));
    }

    /**
     * Called every game tick. Runs as many queued tasks as the budget allows.
     * Tasks that don't fit are left in the queue for next tick.
     */
    public void tick() {
        lastTickRanCount = 0;
        lastTickDeferredCount = 0;

        // We'll run tasks until we run out of budget or tasks.
        while (!taskQueue.isEmpty()) {
            ScheduledTask next = taskQueue.peekFirst();

            if (budget.hasBudget(next.estimatedCostMs)) {
                // We have budget — dequeue and run the task.
                taskQueue.pollFirst();
                long start = System.nanoTime();

                try {
                    next.task.run();
                } catch (Exception e) {
                    // If a task crashes, log it but don't crash the whole mod.
                    OsmiumClient.LOGGER.warn("Task '{}' threw an exception: {}", next.name, e.getMessage());
                }

                // Measure actual cost and report it to the budget controller.
                double actualCostMs = (System.nanoTime() - start) / 1_000_000.0;
                budget.consume(actualCostMs);
                lastTickRanCount++;
            } else {
                // No more budget this tick — stop and leave rest for next tick.
                lastTickDeferredCount = taskQueue.size();
                break;
            }
        }
    }

    /**
     * How many tasks are waiting to run.
     * If this keeps growing, tasks are being submitted faster than they run.
     */
    public int getPendingTaskCount() { return taskQueue.size(); }
    public int getLastTickRanCount() { return lastTickRanCount; }
    public int getLastTickDeferredCount() { return lastTickDeferredCount; }

    /**
     * Internal class representing a single piece of deferred work.
     *
     * In Java, a "record" is a simple data container — it automatically
     * creates a constructor and getter methods for you.
     */
    private record ScheduledTask(String name, double estimatedCostMs, Runnable task) {}
}
