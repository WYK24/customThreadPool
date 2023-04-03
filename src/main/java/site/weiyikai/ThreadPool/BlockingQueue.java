package site.weiyikai.ThreadPool;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 程序员小魏
 * @create 2023-03-29 22:19
 */
//自定义任务队列

class BlockingQueue<T> {
    Logger log = LoggerFactory.getLogger(BlockingQueue.class);
    // 1.任务队列
    private Deque<T> queue = new ArrayDeque<>();
    // 2.锁 (防止多个线程获取同一个任务)
    private ReentrantLock lock = new ReentrantLock();
    // 3.生产者条件变量 (当阻塞队列满了以后，生产者线程等待)
    private Condition fullWaitSet = lock.newCondition();
    // 4.消费者条件变量 (当阻塞队列为空以后，消费者线程等待)
    private Condition emptyWaitSet = lock.newCondition();
    // 5.容量
    private int capcity;

    public BlockingQueue(int capcity) {
        this.capcity = capcity;
    }

    // 带超时阻塞获取
    public T poll(long timeout, TimeUnit unit){
        lock.lock();

        try {
            // 将timeout统一转换为 纳秒
            long nanos = unit.toNanos(timeout);
            while (queue.isEmpty()) {
                try {
                    // 返回值是剩余时间
                    if (nanos <= 0){
                        return null;
                    }
                    //返回值是:等待时间-经过的时间
                    nanos = emptyWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //移除队列中的第一个元素
            T t = queue.removeFirst();
            //唤醒生产者线程继续生产
            fullWaitSet.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

    // 阻塞获取
    public T task() {
        lock.lock();

        try {
            while (queue.isEmpty()) {
                try {
                    //当任务队列为空，消费者就没有任务可以消费，那么就进入等待的状态
                    emptyWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //此时任务队列不为空，取出任务队列当中队头的任务返回
            T t = queue.removeFirst();
            //当从任务队列当中取出一个任务的时候，任务队列就有空位了，就可以唤醒因为队列满了而等待的生产者
            fullWaitSet.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

    // 阻塞添加
    public void put(T task) {
        lock.lock();

        try {
            // 队列已满
            while (queue.size() == capcity) {
                try {
                    log.debug("等待加入任务队列{}...",task);
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("加入任务队列{}",task);
            //当有空位的时候，将新的任务放到队列的尾部
            queue.addLast(task);
            //添加完新的元素之后，需要唤醒等待当中的消费者队列，因为有新的任务进队列
            emptyWaitSet.signal();
        } finally {
            lock.unlock();
        }
    }

    // 带超时时间阻塞添加
    public boolean offer(T task,long timeout, TimeUnit timeUnit) {
        lock.lock();

        try {
            long nanos = timeUnit.toNanos(timeout);
            while (queue.size() == capcity) {
                try {
                    if (nanos <= 0) {
                        return false;
                    }
                    log.debug("等待加入任务队列 {} ...", task);
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.debug("加入任务队列{}",task);
            // 向队列中添加元素
            queue.addLast(task);
            // 唤醒消费者线程
            emptyWaitSet.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    // 获取阻塞队列的大小
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    // 带自定义拒绝策略的的添加
    public void tryPut(RejectPolicy<T> rejectPolicy,T task) {
        lock.lock();

        try {
            // 判断队列是否满
            if(queue.size() == capcity) {
                // 执行拒绝策略的方法
                rejectPolicy.reject(this,task);
            } else { // 有空闲
                log.debug("加入任务队列{}",task);
                queue.addLast(task);
                emptyWaitSet.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}