package dev.osmium.core;

import dev.osmium.OsmiumClient;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ModularTaskScheduler — a work queue that respects the frame budget.
 *
 * Instead of doing all work immediately (which causes spikes),
 * systems submit tasks here. Each tick, we run as many tasks as
 * the frame budget allows, and defer the rest to the next tick.
 */
public class ModularTaskScheduler {

    private final FrameBudgetController budget;
    private final Deque<ScheduledTask> taskQueue = new ArrayDeque<>();
    private int lastTickRanCount = 0;
    private int lastTickDeferredCount = 0;

    public ModularTaskScheduler(FrameBudgetController budget) {
        this.budget = budget;
    }

    public void submit(String name, double estimatedCostMs, Runnable task) {
        taskQueue.addLast(new ScheduledTask(name, estimatedCostMs, task));
    }

    public void tick() {
        lastTickRanCount = 0;
        lastTickDeferredCount = 0;

        while (!taskQueue.isEmpty()) {
            ScheduledTask next = taskQueue.peekFirst();

            if (budget.hasBudget(next.estimatedCostMs)) {
                taskQueue.pollFirst();
                long start = System.nanoTime();

                try {
                    next.task.run();
                } catch (Exception e) {
                    OsmiumClient.LOGGER.warn("Task '{}' threw an exception: {}", next.name, e.getMessage());
                }

                double actualCostMs = (System.nanoTime() - start) / 1_000_000.0;
                budget.consume(actualCostMs);
                lastTickRanCount++;
            } else {
                lastTickDeferredCount = taskQueue.size();
                break;
            }
        }
    }

    public int getPendingTaskCount()      { return taskQueue.size(); }
    public int getLastTickRanCount()      { return lastTickRanCount; }
    public int getLastTickDeferredCount() { return lastTickDeferredCount; }

    private record ScheduledTask(String name, double estimatedCostMs, Runnable task) {}
}
