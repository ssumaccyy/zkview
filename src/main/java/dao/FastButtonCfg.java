package dao;

import java.util.Objects;

public class FastButtonCfg {
  public final String host;
  public final Integer port;

  public FastButtonCfg(String host, Integer port) {
    this.host = Objects.requireNonNull(host);
    this.port = Objects.requireNonNull(port);
  }

  public FastButtonCfg() {
    this("127.0.0.1", 2181);
  }

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }
}
