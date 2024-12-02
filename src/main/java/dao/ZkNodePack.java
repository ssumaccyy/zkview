package dao;

import org.apache.zookeeper.data.Stat;

public class ZkNodePack {

  public final Stat stat;
  public final byte[] data;

  public ZkNodePack(Stat stat, byte[] data) {
    this.stat = stat;
    this.data = data;
  }
}
