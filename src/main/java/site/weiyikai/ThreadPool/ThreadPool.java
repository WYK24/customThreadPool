package site.weiyikai.ThreadPool;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * @author 程序员小魏
 * @create 2023-03-29 22:53
 */

class ThreadPool {
    Logger log = LoggerFactory.getLogger(BlockingQueue.class);
    // 任务队列
    private BlockingQueue<Runnable> taskQueue;
    // 线程集合:存放工作线程
    private HashSet<Worker> workers = new HashSet<>();
    // 核心线程数
    private int coreSize;
    // 获取任务时的超时时间
    private long timeout;
    private TimeUnit timeUnit;
    private RejectPolicy<Runnable> rejectPolicy;

    // 执行任务.线程池向外提供的执行方法
    public void execute(Runnable task) {
        // 当任务数没有超过 coreSize 时，直接交给 worker 对象执行
        // 如果任务数超过 coreSize 时，加入任务队列暂存
        synchronized (workers) {
            if(workers.size() < coreSize) {
                Worker worker = new Worker(task); // 交给工作线程
                log.debug("新增 worker{}, {}", worker, task);
                workers.add(worker); // 将工作线程交给工作线程队列
                worker.start(); // 启动
            } else {
                // taskQueue.put(task);
                // 1) 死等
                // 2) 带超时等待
                // 3) 让调用者放弃任务执行
                // 4) 让调用者抛出异常
                // 5) 让调用者自己执行任务
                //拒绝策略,即到底如何处理多余的任务，交由创建线程池的创建者选择
                taskQueue.tryPut(rejectPolicy, task);
            }
        }
    }
    public ThreadPool(int coreSize, long timeout, TimeUnit timeUnit, int queueCapcity,
                      RejectPolicy<Runnable> rejectPolicy) {
        this.coreSize = coreSize; //核心线程数
        this.timeout = timeout; //获取任务的超时时间
        this.timeUnit = timeUnit; //转换时间器
        this.taskQueue = new BlockingQueue<>(queueCapcity); //阻塞队列
        this.rejectPolicy = rejectPolicy; //拒绝策略，在构建线程池的时候定义
    }

    // 工作线程:它的逻辑是先执行当前任务，如果当前任务执行结束后从任务队列中取任务执行。
    class Worker extends Thread{
        // 任务
        private Runnable task;
        public Worker(Runnable task) {
            this.task = task;
        }
        @Override
        public void run() {
            // 执行任务
            // 1) 当 task 不为空，执行任务
            // 2) 当 task 执行完毕，再接着从任务队列获取任务并执行
            // while(task != null || (task = taskQueue.take()) != null) {
            while(task != null || (task = taskQueue.poll(timeout, timeUnit)) != null) {
                try {
                    log.debug("正在执行...{}", task);
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task = null;
                }
            }
            //如果都没有任务了，那么该工作线程被移除
            synchronized (workers) {
                log.debug("worker 被移除{}", this);
                workers.remove(this);
            }
        }
    }
}