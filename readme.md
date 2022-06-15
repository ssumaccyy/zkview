# ZkView
一款用于修改zookeeper中数据的可视化软件

## 组件
- Swing
- Curator Framework
- Logback
- Slf4j

## jdk 版本
jdk11
> 仅在 gradle 打包过程中用到了 kotlin, 软件自身并未引用 kotlin

## 运行
执行命令 `java -jar zkview-1.0.0.1.jar` 或者 `java -cp ./zkview-1.0.0.1.jar manager.Application`
> 由于绑定了可视化界面，仅支持桌面系统使用
>

## 安全
禁止在节点 `/` 上执行删除操作  
禁止在节点 `/zookeeper` 下新增、删除、修改节点  