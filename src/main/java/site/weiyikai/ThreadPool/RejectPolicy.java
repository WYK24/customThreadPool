package site.weiyikai.ThreadPool;

/**
 * @author 程序员小魏
 * @create 2023-03-29 22:20
 */
@FunctionalInterface // 拒绝策略
interface RejectPolicy<T> {
    void reject(BlockingQueue<T> queue, T task);
}