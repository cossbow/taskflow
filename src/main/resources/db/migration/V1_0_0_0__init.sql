--
-- 工作流定义
--
-- 任务
create table node
(
    id                bigserial primary key,
    name              varchar not null unique,
    type              varchar not null,
    config            json    not null,
    default_arguments json,

    async             bool    not null default false,
    submit_config     json,

    max_retries       int     not null default 0,
    remark            varchar,
    created_at        timestamptz      default now(),
    updated_at        timestamptz
);
create index on node (name);
-- 工作流定义
create table workflow
(
    id                bigserial primary key,
    name              varchar not null unique,

    output_parameters json,
    notifier          varchar,
    notifier_config   json,
    timeout           bigint,

    remark            varchar,
    created_at        timestamptz default now(),
    updated_at        timestamptz
);
create index on workflow (name);
-- 工作流输入输出配置
create table workflow_node_io
(
    workflow_id      bigint references workflow (id) on delete cascade on update cascade,
    node_id          bigint references node (id) on delete cascade on update cascade,
    primary key (workflow_id, node_id),

    input_parameters json,
    ignore_error     bool not null,

    created_at       timestamptz default now(),
    updated_at       timestamptz
);
-- 工作流的边
create table workflow_node_edge
(
    workflow_id  bigint references workflow (id) on delete cascade on update cascade,
    from_node_id bigint references node (id) on delete cascade on update cascade,
    to_node_id   bigint references node (id) on delete cascade on update cascade,
    primary key (workflow_id, from_node_id, to_node_id),

    created_at   timestamptz default now()
);

--
-- 任务数据源
--
create table task_source
(
    id         bigserial primary key,
    name       varchar unique not null,
    workflow   varchar        not null,
    type       varchar        not null,
    config     json,
    batch      int            not null,
    timeout    bigint,
    retries    int         default 0,

    autostart  bool        default false,

    remark     varchar,
    created_at timestamptz default now(),
    updated_at timestamptz
);
create index on task_source (name);
