## Automute


![GitHub top language](https://img.shields.io/github/languages/top/asforest/Automute)
![Release](https://img.shields.io/github/v/release/asforest/Automute)
![GitHub all releases](https://img.shields.io/github/downloads/asforest/Automute/total)

Automute插件是适用于Mirai机器人的群聊自动禁言插件。可以使用正则表达式识别违规内容，自动撤回并禁言或者踢出（插件工作时需要群主或者管理员权限，如果没有权限只会进行通知，没有实际撤回消息和禁言的动作）

### 工作原理

当群成员发言的时候会把消息内容和预先设定好的违规关键字进行对比。如果与其中任意一个匹配，则判定为消息违规。如果违规者是首犯，或者违规次数少于配置文件设定的谅解次数，就撤回消息+禁言。如果是惯犯超过了谅解次数，则撤回消息+踢出。同时会向预先设定好的管理员QQ发送通知信息

违规关键字可以设置多个档位，不同的档位有不同的严格程度。可以对频繁发言的人降低违规检测标准以避免误伤，同时对进群之后发的头几次消息进行着重检查，提高检测标准。档位可以存在任意多个，且每个档位都能设定不同的违规关键字

插件对群主和管理员不生效，即使他们发送了违规消息也是如此

### 使用指南

1. 首先将插件放到`plugins`目录下并重新启动mirai-console
2. 打开插件配置文件目录`config/com.github.asforest.automute`
3. 编辑**关键字**文件`keywords.yml`，可以参考[这里](#关键字文件)

3. 编辑**主配置**文件`config.yml`，可以参考[这里](#主配置文件)

4. 修改完成后使用指令`/automute reload`来重新加载配置文件，并且可以使用`/automute info`来确认配置文件是否加载成功

### 关键字文件

关键字文件的模板长这样：

其中有2个档位：`2`和`4`。2代表进群后前2条消息会检测这个档位下的所有关键字。超过2条消息之后，就不再检测2下面的关键字了。4代表会检测前4条消息。当群成员第一次发言时，会同时检测档位2和档位4。当发言次数超过4次以后，即使是发送了违规消息，也不会触发禁言。

档位只能是整数：比如`2`或者`4`或者`10`，否则不会生效。如果是负数或者是0，则该档位不会受到发言次数的影响，永远会生效，适合做一些常驻关键字检测

档位下方是一个列表，每一个列表元素都是一个关键字模板，模板使用正则表达式编写，需要注意加引号，否则过不了yaml文件格式检查会导致文件解析失败

```yaml
'2': 
  - '.*微信.*'
  - '.*成年.*'
  - '.*[群裙].*'
  - '.*[费废].{0,3}用.*'
  - '.*好.{0,3}评.*'
'4': 
  - '.*刷.*'
```

### 主配置文件

主配置文件用来控制插件的行为，可以按需修改

```yaml
# 开启只报告管理员有人发送违规消息，但插件不会有实际动作（指撤回消息+禁言）（主要用来调试）
dry-run: false

# 生效的群聊列表
groups-activated: 
  - 123456789

# 谅解次数，用户违规达到这个次数之前仅禁言，达到之后会踢出群聊。（设为0会直接踢出，没有禁言过程）
toleration: 1

# 禁言时长，单位秒。最长30天（2592000秒）
mute-duration: 2592000

# 踢出时的消息
kick-message: 广告给爷死！

# 踢出时是否拉黑（不再接受此人的加群请求）
block-when-kick: false

# 管理员QQ列表，违规通知会发给所有管理员（管理员需要和机器人是QQ好友）
admins: 
  - 123456789

# 开启后发送给管理员的原始消息样本会使用base64编码
# 关闭后发送原文
# 发送原文会增加腾讯误判发布不良信息的风险，如果担心请启用base64编码
report-with-base64: false

# 报告消息的模板
report-template: |
  检测到 $SENDER_NAME($SENDER_QQ)
  在QQ群聊 $GROUP_NAME($GROUP_NUMBER) 的发言
  违反了关键字【$KEYWORD】
  这是第$CURRENT_TIME次，当达到第$MAX_TIMES时会被请出群聊
  当前动作：$ACTIONS
  以下是消息样本：
  $SAMPLE
```

### 插件指令

插件只有三个指令（指令简写：`am`）：

+ `/automute reload`：重新加载所有配置文件
+ `/automute info`：显示目前的配置文件内容和违规关键字
+ `/automute decode <base64>`：一个很方便的工具，用来解码base64字符串

### 其它配置文件

除了关键字文件（`keywords.yml`）和主配置文件（`config.yml`）以外，还有一些其它的yml格式文件。这些文件不需要手动编辑，插件会自动维护文件内容。如果手动进行了修改，请使用指令重新加载一次

+ `config.yml`：主配置文件
+ `keywords.yml`：关键字文件
+ `mute-data.yml`：禁言数据（用来记录用户是首犯还是惯犯，以判断是禁言还是踢出）
+ `speakings.yml`：发言数据（用来记录用户的发言次数，以应用不同档位的关键字检测规则）
