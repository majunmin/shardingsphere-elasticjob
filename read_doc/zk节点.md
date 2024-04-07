# elasticJob 很依赖 zookeeper 监听机制

选主/配置变更/failover/misfire/...

## 1.config节点

| path              | value                      | 是否是临时节点 | Description                                       |
|:------------------|:---------------------------|:--------|:--------------------------------------------------|
| /{jobName}/config | yaml, JobConfigurationPOJO | 否       | 作业配置节点, 是一个yaml格式字符串,可以解析为 `JobConfigurationPOJO` |

## 2.servers 节点

| path                    | value           | 是否是临时节点 | Description                                    |
|:------------------------|:----------------|:--------|:-----------------------------------------------|
| /{jobName}/servers/{ip} | DISABLE, ENABLE | 否       | 曾经注册了可以执行job的机器, 数据节点的值为空字符串表示正常，disabled表示不可用 |

## 3.instances 节点

| path                               | value             | 是否是临时节点 | Description                                                                         |
|:-----------------------------------|:------------------|:--------|:------------------------------------------------------------------------------------|
| /{jobName}/instances/{instance_id} | yaml, JobInstance | 否       | instanceid 格式` {ip}@-@{pid}`, 表示存活的实例 <br/>作业配置节点, 是一个yaml格式字符串,可以解析为 `JobInstance` |

## 4.sharding 节点

| path                                             | value | 是否是临时节点 | Description                                   |
|:-------------------------------------------------|:------|:--------|:----------------------------------------------|
| /{jobName}/sharding/*                            |       | 否       | job分片节点目录, 可以获取对应 job 下所有的分片                  |
| /{jobName}/sharding/{item_id}/instance           |       | 否       | 节点的值是instanceId eg. 10.3.9.7@-@4256           |
| /{jobName}/sharding/{item_id}/running            |       | 否       | 表示分片运行状态                                      |
| /{jobName}/sharding/{item_id}/disabled           |       | 否       | 分片被禁用                                         |
| /{jobName}/sharding/{item_id}/misfire            |       | 否       | 表示任务被错过执行标志,第二次调度时,第一次的任务还没有执行完成.             |
| /{jobName}/leader/sharding/{item_id}/processing  |       | 否       | 表示Job正在进行分片,分片完成后被删除                          |
| /{jobName}/leader/sharding/{item_id}/necessary   |       | 否       | 表示Job是否需要分片,分片完成后被删除                          |
| /{jobName}/leader/sharding/{item_id}/failover    |       | 否       | 表示执行分片failover, 节点的值是执行失败分片的实例id,即 instanceId |
| /{jobName}/leader/sharding/{item_id}/failovering |       | 否       |                                               |

## 5.leader 节点

| path                                       | value | 是否是临时节点 | Description                             |
|:-------------------------------------------|:------|:--------|:----------------------------------------|
| /{jobName}/leader/sharding/necessary       |       | 否       | 表示Job是否需要分片,分片完成后被删除                    |
| /{jobName}/leader/sharding/processing      |       | 否       | 表示Job正在执行分片,分片完成后被删除                    |
| /{jobName}/leader/election/latch           |       | 否       | 分布式锁节点, 表示正在选主的根节点                      |
| /{jobName}/leader/election/instance        |       | 否       | 表示被选为master的实例，节点的值是master实例的instanceId |
| /{jobName}/leader/failover/items/{item_id} |       | 否       | 表示需要failover的分片                         |




