## Java实现LFTP
使用Java实现用UDP协议传输文件. 其中UDP传输100%可靠.

使用流控制, 阻塞控制, 多线程.

即使用UDP来模拟TCP传输.

参考了一些大佬的意见, 加入了传输速度和传输时间的信息.

## 文件结构
(default package)
- LFTP 客户端程序入口

client
- Client 客户端

server
- Server 服务端程序入口

service
- Functions 通用方法
- ReceiveThread 接收线程
- SendThread 发送线程
- TCPPackage TCP数据包
## 使用方法
LFTP和Server均有main方法, 分别对应客户端和服务端.

服务方启动Server, 以本机的9090端口建立服务端.

用户方启用LFTP, 命令格式为
```
LFTP lsend/lget [host] [filename]
```
我是在Eclipse中运行的, 对于带有参数的LFTP, 在Run Configurations, Arguments, Program arguments中设置参数, 例如`lsend localhost file`.

文件发送接收均通过`folder`文件夹.

若host为本机, 则需在另一个目录拷贝一份项目启动客户端. 即一份项目作为服务端启动, 另一份项目作为客户端启动. 因为在同一项目下传输文件会出现冲突.

## 测试

我使用了两台电脑进行传输测试.

传输文件大小为908MB.

服务端ip为172.18.32.30.

传输文件做3份拷贝, 分别为file1, file2, file3.

file1和file2在服务端文件夹内, file3在客户端文件夹内.

同时发送file1和file2, 以及接受file3.

客户端每次分别设置如下参数启动LFTP.

1. `lsend 172.18.32.30 file1`
2. `lsend 172.18.32.30 file2`
3. `lget 172.18.32.30 file3`

客户端输出如图. 可以看到在同时接受或发送多个文件, 这里显示是两个.

在客户端和服务端分别能看到传输结果.

检查文件是否一致. 对比file1的哈希值, 发现一致, 说明lsend操作100%可靠.

对比file3的哈希值, 发现一致, 说明lget操作100%可靠.