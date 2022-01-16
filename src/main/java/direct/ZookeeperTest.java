package direct;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZookeeperTest {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperTest.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        final long start = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Watcher watcherImpl = event -> countDownLatch.countDown();
        ZooKeeper zookeeper = new ZooKeeper("10.106.1.1:2181", 30_000, watcherImpl);
        countDownLatch.await();
        logger.trace("connected");
        ZooKeeper.States states = zookeeper.getState();
        if (!states.isConnected()) {
            logger.trace("zookeeper not connected");
        }
        final long end = System.currentTimeMillis();
        final long connectCostTime = end - start;
        final long cost = end - start;
        logger.trace("连接耗时{}秒,{}毫秒",cost / 1_000 ,cost % 1_000);
        zookeeper.close();
    }
}
