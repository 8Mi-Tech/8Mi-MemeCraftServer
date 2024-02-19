## 如何搭建一个BMCLAPI节点 - OSS模式

0. 准备材料
- 搜集Github反代地址
可以看看这里 [Greasy Fork](https://update.greasyfork.org/scripts/412245/Github%20%E5%A2%9E%E5%BC%BA%20-%20%E9%AB%98%E9%80%9F%E4%B8%8B%E8%BD%BD.user.js)
打开这个地址后，从里面找寻地址，随便选一个
- 本项目内 外部访问的HTTPS端口 均为8443 在Nginx内
如域名已备案，且不是部署在家用宽带网络环境下，可按默认端口443的方法来
- crontab 编辑指令为 
```bash
crontab -e
```
| 白名单|黑名单|
| - | - |
|天翼云盘（家庭盘）|百度网盘 ★★★★★★|
|移动网盘|lanzou ★★★★☆|
|联通网盘|OneDrive ★★☆☆☆|
||阿里网盘 ★★★★☆|
||夸克网盘 ★★★☆☆|
||QQ 微云 ★★★★☆|
||123云盘 ★★★★★|
||城通网盘 ★★★★★★|
||UC云盘 ★★★★☆|
||迅雷网盘 ★★★☆☆|
||曲奇云盘 ★★★★★|
黑名单6颗星 第六个代表群众的愤怒
---

1. 宝塔篇
- 确认你的发行版 `cat /etc/os-release`
- 去官网 bt.cn 复制安装指令
- 安装过程中，不要操作服务器任何东西，且等待他安装完毕
- 安装完毕后，先禁用面板的SSL，因为没域名，且自签证书，浏览器会提示不信任
```bash
##禁用方式是
sudo -i #进入root用户
bt 26 #关闭宝塔SSL
```
- 安装完毕后，要先在面板内登录宝塔账户
- 向导内，选择 编译安装 LNMP内的 Nginx* + MySQL (星号为必须) 
(MySQL是给AList用的，如果觉得AList内置的SQLite够用，就不需要)
(若是安装MariaDB 版本为 10.11)

- 设置反代
在网站内添加两个网站 给 BMCLAPI 和 AList
`这里的例子是基于我的域名，在宝塔批量创建网站`
```
alist.8mi.free.hr|/www/wwwroot/default|0|0|0
bmclapi.8mi.free.hr|/www/wwwroot/default|0|0|0
```
- 这里的例子是
BMCPAPI: `bmclapi.8mi.free.hr` 反代目标 `http://127.0.0.1:9393` 
对应配置 `ts`的`CLUSTER_PORT` 或 `go`的`port`
AList: `alist.8mi.free.hr` 反代目标 `http://127.0.0.1:5244` 反代缓存 60分钟 反代配置文件 移除`proxy_cache_valid`的`200`这个数字

- 设置SSL证书
通过DNS获取证书
域名: 8mi.free.hr 的 DNS服务器 在 CloudFlare
所以我们要SSL获取配置内 设置DNS 类型是CloudFlare 证书类型是 Let's Encrypt
Cloudflare API Key (Global) https://dash.cloudflare.com/profile/api-tokens
`像我这种域名由于有三段，不支持宝塔的通配符生成域名`

- （可选）高级选项
在每个域名内的配置，添加8443端口，配置如下(参考)
除非你是内网映射到路由器的 那就路由器上设置 内网端口443 外网端口8443 就能工作
`在教程中，我们选择了使用 内网端口443 外网端口8443 映射端口方法`
```conf
server
{
    listen 8443 ssl http2;
}
```
---

2. AList篇章
- 官网的一键安装脚本指令 
(如果你的网络环境能直连 Github.com 移除GH_PROXY的地址)
```bash
GH_PROXY=''
```
```bash
curl -fsSL "https://alist.nn.ci/v3.sh" | env GH_PROXY='https://hub.gitmirror.com/' bash -s install
```
- （若有安装MySQL且已启动）安装完成后 输入如下指令
打开配置文件
```bash
cd /opt/alist
vim data/config.json
```
- （若有安装MySQL且已启动）编辑config.json内的database内容
里面的内容根据你在宝塔设置的MySQL为准，我这里是个例子
```json
{
    "database":{
        "type": "mysql",
        "host": "localhost",
        "port": "3306",
        "user": "alist",
        "password": "alist",
        "name": "alist",
        "db_file": "",
        "table_prefix": "x_",
        "ssl_mode": ""
    }
}
```
- （若有安装MySQL且已启动） 文件保存后，输入如下指令以重新加载
重载前请先确认你已经在宝塔内设置好了数据库以及用户名和密码
```bash
systemctl restart alist
systemctl status alist
```
- 然后设置管理员密码(这里以密码123456为举例)
```bash
cd /opt/alist
./alist admin set '123456'
```
- 打开AList网站
`在教程中，我们以 https://alist.8mi.free.hr:8443 来访问`
- 登录管理员账户
- 注意事项:
`管理` -> `全局` -> `签名所有：关闭` -> `保存`

- 添加存储 (如想添加其他盘，请参阅以下地址)
https://alist.nn.ci/zh/guide/drivers/common.html
注意:`路径请按实际要求来，这里是移动网盘的配置`
`存储` -> `添加`
`本教程内使用的是一个好哥们的网盘配置 我给他分配到 storage/139Cloud`
缓存过期时间：建议1天以内 也就是1440分钟以内
在你的网盘内添加个文件夹 `BMCLAPI-Mirrors`
- 添加别名
然后添加与之相关联的别名路径 Go端设置的时候要用到这个
`存储` -> `添加` -> `驱动：别名` -> `挂载路径: /bmclapi/139` -> `路径: storage/139Cloud/BMCLAPI-Mirrors` -> `保存`
- 如果都能显示工作中,那么你的第一个盘添加成功，再添加第二个盘就简单了，多看看文档
---
3. RClone篇 （挂载篇）
- 首先安装RClone
以下是Debian系列发行版的安装指令，其他的要自行百度
`以勒只用 Deepin 和 UnionTechOS`
```bash
apt install rclone fuse3
```
- 然后根据如下操作步骤来(配置步骤)
```bash
> rclone config
n/s/q> n
name> alist
storage> webdav
url> http://127.0.0.1:5244/dav
vendor> other
user> admin
y/g/n> y
password: #输入你在AList那边设置的管理员密码
bearer_token> #直接按回车 这项不需要设置
advance config?
y/n> n

#最后按下y且回车以保存
#然后按下q且回车即可退出
```
- 如何挂载
```bash
mkdir ~/alist #仅在初始化的时候需要，后续不需要这个指令
rclone mount alist: ~/alist --daemon
```
- crontab配置
挂载完毕后，以防重启的时候消失，我们需要在crontab内，添加一行如下内容
```bash
@reboot rclone mount alist: ~/alist --daemon
@reboot rclone ls alist:
```
---
4. 重量级项目 Go-OpenBMCLAPI
- 先想个办法把群里的 `go-openbmclapi.sh` 放在服务器路径 `/root/bmclapi/` 内 
(文件名要正确，不正确的要记得改，方能跟随教程，我没记错的话，我给文件命名的是`openbmclapi-go.sh`)
- 输入以下指令，创建内存盘
```bash
mkdir ~/bmclapi/cache/.tmp #仅在初始化的时候需要，后续不需要这个指令
mount -t tmpfs -o size=1G bmclapi-ramdisk ~/bmclapi/cache/.tmp
```
- crontab配置
挂载完毕后，以防重启的时候消失，我们需要在crontab内，添加一行如下内容
```bash
@reboot mount -t tmpfs -o size=1G bmclapi-ramdisk ~/bmclapi/cache
```
- 创建软链接(注意 不是软链接别名路径，而是这个，直接写到盘的)
`注意：请根据你的情况来，这里是根据我刚才添加的盘来做的软链接，软链接要写在配置内，参考以下`
```bash
cd ~/bmclapi
ln -s ~/alist/storage/139Cloud/BMCLAPI-Mirrors 139Cloud
```

- 塞进去后，终端内输入如下内容
```bash
cd ~/bmclapi
chmod +x go-openbmclapi.sh
./go-openbmclapi.sh
```
- 当他提示配置文件生成完毕后，就要开始编辑了
链接内`/d`这个很重要
配置文件名是 config.yaml
这里就挑选一些重点的
```yaml
byoc: true
noopen: true
trusted-x-forwarded-for: true
keepalive_timeout: 60
download_max_conn: 16

storages: #list原则上能放多个平台的，在AList篇章有提到
  - type: mount
    weight: 0
    data:
      path: 139Cloud
      redirect_base: https://alist.8mi.free.hr:8443/d/bmclapi/139
  - type: mount
    weight: 0
    data:
      path: 189Cloud
      redirect_base: https://alist.8mi.free.hr:8443/d/bmclapi/189
```

新版支持对接WebDav，可以直接对接到WebDav，同时就不需要Alist里面的别名（但需要额外测试确认）
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
- 至于配置文件的配置项 cluster_id 和 cluster_secret , 联系 bangbang93 以获得信息
- 如果配置好了，那就输入如下指令吧
```bash
./go-openbmclapi.sh
```

5. 结尾
- 这个教程可能不完整，有些地方(例如缓存方面)偏太主观，
- 如果觉得缺少配图，我可以补充
- 这个项目，是在`2023-11-21`这天开始了测试，那个时候，mcbbs还没出事，我的节点犹如喝茶的老大爷
- 当时的方法是靠nginx劫持路径发送302信号，且仅对接了`天翼云盘`，
- 直到我遇到了一个我都想给他打钱的哥们，他就是go端的作者，他几乎都能按照我算是明确的需求来开发
- 在这里，我也非常感谢这个哥们
- 你们可以看看他的git仓库就知道我们这个方案不是一时兴起，而是已经打磨了好久，
- 在mcbbs出事后的第三天，我力挽狂澜，抗下了所有的连接，在这之后，我的节点遥遥领先
- 关于tmpfs的，如果你对Linux有充足的了解，可以自己搜索方法，且尝试把他放在fstab内，fstab的优先级比crontab还高（前提是对Linux要有充足的了解）
- rclone也可以尝试用systemctl来实现后台，效果比他自身的后台还要好（可能）
