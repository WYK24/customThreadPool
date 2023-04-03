package site.weiyikai.ThreadPool;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author 程序员小魏
 * @create 2023-03-29 23:04
 */

public class CustomThreadPoolDemo {
    static Logger log = LoggerFactory.getLogger(BlockingQueue.class);
    public static void main(String[] args) {

        ThreadPool threadPool = new ThreadPool(1,
                1000, TimeUnit.MILLISECONDS, 1, (queue,task) ->{
            // 1. 死等
            //queue.put(task);
            // 2. 带超时等待
            //queue.offer(task, 500, TimeUnit.MILLISECONDS);
            // 3. 让调用者放弃任务执行
            //log.debug("放弃{}", task);
            // 4. 让调用者抛出异常
            throw new RuntimeException("任务执行失败 " + task);

        });
        for (int i = 0; i < 3; i++) {
            int j = i;
            threadPool.execute(() -> {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("{}", j);
            });
        }
    }
}