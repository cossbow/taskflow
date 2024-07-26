任务编排
===================

一个可以进行任务编排的工具，类似https://github.com/taskflow/taskflow。

### 简介

编排是基于【DAG】的任务流程管理系统，可灵活的多个任务接口组织成【DAG】工作流来执行， 并提供基于JsonPath的参数传递和状态确认机制，便于零入侵式组织已有的任务编排。

- JsonPath的引用标识是${}，不包括引号，比如${a.b}的jsonpath为

```jsonpath
$.a.b
```

- 实际使用JsonPath的相似语法JMESPath[详情参考](https://jmespath.org/)
- 项目使用了[JMESPath Java库](https://github.com/burtcorp/jmespath-java)

### 基本元素

1. 工作节点【Node】
    - 每个工作节点对应一个任务接口或任务功能，包含名称、类型和配置，其中类型用来判断 具体的执行过程，每个节点执行都是先接收输入【input】，完成后在输出【output】 并伴随执行状态的变化。
2. 工作流【Workflow】
    - 按照DAG的方式将各个工作节点用单向依赖的连接起来，每个连接（DAG的边）可能带有 参数的传递，参数传递通过配置来决定，通过JsonPath从上游节点的输出中获取数据然 后注入到本节点中。工作流包括本身的配置、边及输入输出配置。
3. 任务【Task】
    - 由外界发起的一次工作流的执行叫任务，接收外界参数输入，并提供给每个工作节点运行 使用。任务完成（子任务全部完成）后根据配置决定发出通知到外界。
4. 子任务【Subtask】
    1. 任务执行时，工作流里的每个工作节点执行的状态，必须等依赖的上游子任务全部完成后 才能执行，无依赖的头节点在任务启动是默认开始执行。
    2. 通过输入配置注入任务的输入和上游子任务的输出结果，执行完成（可以是异步）后保存 输出结果。
    3. 当前子任务完成后将试图发起下游子任务的执行，因为每个子任务是独立执行的，每完成 一个就会检查下游节点的每个子任务的上游节点是否全部完成才继续……

### 管理接口[MetaController](src/main/java/com/hikvision/hbfa/sf/controller/MetaController.java)

1. 添加工作节点

```json5
{
  // 节点名称，唯一 
  "name": "nlp",
  // 节点类型，这里是个REST调用
  "type": "REST",
  // 基本配置具体结构与"type"相关，下面是"REST"的配置
  "config": {
    // type=REST
    "method": "POST",
    "uri": "http://localhost:8080/calculate",
    "headers": {},
    // type=KAFKA
    "topic": "xxx-nlp-calculate"
  },
  // 默认参数：不包含JsonPath直接透传，会被io的inputParameters覆盖
  "defaultArguments": {},
  // 这是个异步处理
  "async": true,
  // 节点对应的子任务完成的提交配置
  "submitConfig": {
    // 提交形参
    "parameters": {
      // 通过这个获取subtaskId
      "id": "${subtaskId}",
      // 执行结果的状态码
      "code": "${code}",
      // 错误信息     
      "msg": "${msg}"
    },
    // parameters.code的状态码映射。设置null就以内置的为准。
    "codeMap": {}
  },
  "remark": "图片搜索"
}
```

2. 编辑工作流

```json5
{
  // 工作流名称，唯一
  "name": "test-work-1",
  // 输出配置，一个任务完成时，将配置的数据输出到指定地方，参考JsonPath文档
  "outputParameters": {
    "parseId": "${parse.output.id}",
    "imgId": "${imgSearch.output.id}",
    "nlpId": "${nlp.output.id}",
    "taskInputId": "${task.input.id}",
    "taskId": "${task.id}",
    "finallyName": "${task.input.name}"
  },
  // 任务完成时的通知方式，和节点调用的类型一样
  "notifier": "REST",
  // 通知配置，和节点的调用配置一样
  "notifierConfig": {
    "method": "POST",
    "uri": "http://localhost:25600/notify",
    "headers": {}
  },
  // 工作流中要加入的节点
  "nodes": [
    {
      // 节点名称
      "nodeName": "parse",
      // 输入形参，具体可引用实参下面说明
      "inputParameters": {
        "topic": "${submit.kafka.topic}",
        "value": "${task.input.value}",
        "fid": "${task.input.fid}",
        "submitId": "${id}"
      }
    },
    {
      "nodeName": "ocr",
      "inputParameters": {
        "topic": "${submit.kafka.topic}",
        "value": "${parse.output.value}",
        "submitId": "${id}"
      }
    },
    {
      "nodeName": "nlp",
      "inputParameters": {
        "topic": "${submit.kafka.topic}",
        "value": "${nlp.output.value}",
        "submitId": "${id}"
      }
    }
  ],
  // 图的边
  "edges": [
    {
      "fromNode": "parse",
      "toNode": "ocr"
    },
    {
      "fromNode": "ocr",
      "toNode": "nlp"
    }
  ],
  "remark": "search documents",
}
```

具体可引用实参为：

```json5
{
  // 当前子任务id
  "id": 338,
  // 当前任务属性
  "task": {
    "id": 5,
    // 任务的输入实参
    "input": {}
  },
  // 异步处理的话会有这个，主要传递service-flow任务的参数
  "submit": {
    // REST提交时：
    "host": "",
    "service": "",
    "path": "",
    // Kafka提交时
    "servers": "",

    "topic": ""
  },
  // 下面是上游子任务的输出，依赖m个就会有m个key
  // parse节点对应的子任务的输出
  "parse": {},
  "nlp": {},
  // ...
}
```

3. 子流程：将一个workflow作为一个调用封装成一个节点

```json5
{
  // 节点名称，唯一 
  "name": "subflow-1",
  // 节点类型，这里是个REST调用
  "type": "SUBFLOW",
  // 不需要填配置
  "config": {
    "workflow": "test-workflow-1"
  }
}
```

### 运行接口[TaskController](src/main/java/com/hikvision/hbfa/sf/controller/TaskController.java)

1. 启动任务：/task/start

```json5
{
  // 已经创建的工作流名称
  "name": "test-work-1",
  // 任务输入参数
  "input": {
    "value": 2000,
    "name": "jjj",
    "fid": "jfksajdkg"
  }
}
```

2. 任务和任务完成通知：根据创建工作流配置的'notifier'和'notifierConfig'通知外面系统

### 任务源

- 通过配置数据源主动获取数据来启动任务

```json5
{
  // 数据源名称，唯一
  "name": "testSearch1",
  // 要启动的工作流名称
  "workflow": "SearchImg",
  // 类型：目前仅实现了KAFKA
  "type": "KAFKA",
  // 配置
  "config": {
    // kafka类型对应属性topic，表示从这里拉取数据来启动任务
    "topic": "test-search-1"
  },
  // 批处理数量
  "batch": 1000,
  // 任务超时，毫秒
  "timeout": 600000,
  // 重试次数
  "retries": 5,
  // 备注说明
  "remark": "测试任务数据源"
}
```

