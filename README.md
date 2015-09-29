# Firebats

`Firebats` 服务器基础结构结构库包含以下功能：
* 异步反应性编程(利用rxjava)
* firebats.bus项目：基于事件总线的分布式微服务工具，包括：服务发现，事件总线，bus目前基于websocket json通信，在未经优化的状态下，单点性能在10万次/秒左右。
* firebats.http项目：基于rxnetty的http高级封装，利用责任链类型增强的方式提供全插拔组合式编程
* firebats.app项目：提供一致的启动终止cli应用工具
* firebats.config项目:基于强类型的属性配置

TODO                                                 
* cqrs模式读写分离实验                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             
* 有状态微服务(消息hash、分区负载均衡等)
* 微服务应以docker打包发布

### 发布

* 20141107 firebats 1.1 发布 ，野生救援项目上线后升级版本号为1.2-SNAPSHOT
* 20150702 firebats 1.6 发布 ，当前版本号为1.6.1-SNAPSHOT

### Documentation
* [Guides](http://xxxxx:8090/pages/viewpage.action?pageId=15043202)
