# 如何搭建一个BMCLAPI节点 - OSS模式

## 准备
1. 一台公网可访问装有 [NGINX](https://nginx.com/) v1.25+ 的计算机
2. 如域名已备案, 且 `443` 端口可正常访问,请使用默认HTTPs端口部署
3. 不建议使用以下网盘作为后端
    - 百度网盘 ★★★★★★
    - lanzou ★★★★☆
    - OneDrive ★★☆☆☆
    - 阿里网盘 ★★★★☆
    - 夸克网盘 ★★★☆☆
    - QQ 微云 ★★★★☆
    - 123云盘 ★★★★★
    - 城通网盘 ★★★★★★
    - UC云盘 ★★★★☆
    - 迅雷网盘 ★★★☆☆
    - 曲奇云盘 ★★★★★

## 反向代理([NGINX](https://nginx.com/))
> /etc/nginx/nginx.conf

在一般情况下 nginx 软件包会安装此文件到 `/etc/nginx/` 因此无需过多配置
```nginx.conf
user  nginx;
worker_processes  auto;

error_log  /var/log/nginx/error.log notice;
pid        /var/run/nginx.pid;


events {
    worker_connections  1024;
}


http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;

    sendfile        on;
    #tcp_nopush     on;

    keepalive_timeout  65;

    gzip  on; #如果需要则修改

    include /etc/nginx/conf.d/*.conf;
}
```
> /etc/nginx/conf.d/openbmclapi.conf

根据实际情况修改配置
```nginx.conf
upstream openbmclapi-default {
  zone openbmclapi-default 64k;
  server 127.0.0.1:4000;
  keepalive 2;
}

map $http_upgrade $connection_upgrade {
  default upgrade;
  ''      "";
}

server {
  listen 443 ssl;
  listen [::]:443 ssl;
  http2 on;
  server_name  openbmclapi.example.com;

  ## 如果自备ssl证书,请正确配置以下内容
  #ssl_certificate /path/to/certificate/openbmclapi.example.com/fullchain.pem;
  #ssl_certificate_key /path/to/certificate/openbmclapi.example.com/privkey.pem;
  #ssl_trusted_certificate /path/to/certificate/openbmclapi.example.com/fullchain.pem;

  location / {
    proxy_buffers 16 4k;
    proxy_buffer_size 2k;
    #proxy_buffering off;

    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_pass http://openbmclapi-default;
    }
  }
```
### 注意: 
如果遇到 `443` 端口不可用/被您的网络服务提供商阻止, 请修改默认的端口

## [AList](https://alist.nn.ci/zh/)
[安装AList](https://alist.nn.ci/zh/guide/install/)

(若有安装MySQL)编辑 `/opt/alist/data/config.json` 内的 `database` 内容,根据实际情况修改配置
```json
{
    "database":{
        "type": "mysql",
        "host": "localhost",
        "port": "3306",
        "user": "alist",
        "password": "THE_EXTREME_SERCET_PASSWORD",
        "name": "alist",
        "db_file": "",
        "table_prefix": "x_",
        "ssl_mode": ""
    }
}
```
### 注意:
  - 需关闭签名功能, 否则会导致openbmclapi无法拉取
  - [如何添加存储](https://alist.nn.ci/zh/guide/drivers/common.html)
  - 路径请按实际要求配置
  - 缓存过期时间：建议1天以内 也就是1440分钟以内在你的网盘内添加文件夹 `BMCLAPI-Mirrors`

## [Go-OpenBMCLAPI](https://github.com/LiterMC/go-openbmclapi)
[安装Go-OpenBMCLAPI](https://github.com/LiterMC/go-openbmclapi)

新版支持对接WebDav，可以直接对接到WebDav别名, 因此在你的 `/opt/openbmclapi/config.yaml` 中修改匹配字段为实际值
```yaml
stroages:
  - type: webdav
    weight: 0
    data:
      alias: example-user
      endpoint: http://127.0.0.1:5244/dav/139Cloud/BMCLAPI-Mirrors
  - type: webdav
    weight: 0
    data:
      alias: example-user
      endpoint: http://127.0.0.1:5244/dav/189Cloud/BMCLAPI-Mirrors

webdav-users:
    example-user:
        # Webdav 入口 URL
        endpoint: http://127.0.0.1:5244/dav
        username: example-username
        password: example-password
```
至于配置文件的配置项 cluster_id 和 cluster_secret , 联系 [bangbang93](https://github.com/bangbang93/openbmclapi/discussions) 以获得信息

## 结尾
1. 这个教程可能不完整，有些地方(例如缓存方面)偏太主观，如果觉得缺少配图，我可以补充
2. 这个项目，是在 2023/11/21 这天开始了测试，那个时候，mcbbs还没出事，我的节点犹如喝茶的老大爷
3. 当时的方法是靠nginx劫持路径发送302信号，且仅对接了天翼云盘，
4. 直到我遇到了一个我都想给他打钱的哥们，他就是go端的作者，他几乎都能按照我算是明确的需求来开发，在这里，我也非常感谢这个哥们
5. 你们可以看看他的git仓库就知道我们这个方案不是一时兴起，而是已经打磨了好久，
6. 在mcbbs出事后的第三天，我力挽狂澜，抗下了所有的连接，在这之后，我的节点遥遥领先
9. bang15bianshi

*我是后来的共献者, 几乎改完了全篇, 写的有够无语*
![There is no alt because im speechless](%E5%9B%BE%E7%89%87.png)
