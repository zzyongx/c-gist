## 云图推介
### 郑志勇
### zhengzhiyong@sogou-inc.com

## 云图是什么
1. 抓取其它网站的图片
2. 存储用户自己产生的图片
3. 对外展示这两类图片时，可以略做加工

## 为什么选择云图
1. 诸位的核心业务
2. 竞争对手在使用SaaS
3. 云图是一个SaaS

## 核心业务
1. 核心业务不是摆霍图片
2. 时间紧迫/时间用在刀刃上
3. 非核心代码/架构是负担，不是财富
4. 程序员的核心竞争力也不在此

## SaaS 的广泛使用
1. SaaS的数量，质量，功能都在提升
2. 使用IaaS/SaaS 的公司在增多
3. SaaS 使得IT基础组件触手可得
4. 优先考虑SaaS，如果没有，这是个机会

## BaiDu apistore
![](images/baidu-apistore.png)

## Sae Paas
![](images/sae-paas.png)

## AWS Iaas/Paas
![](images/aws-product.jpg)

## instagram
11年的时候，7个人的团队，用户数1400万，图片超过1.5一张


![](images/instagram.png)

## dropbox
![](images/dropbox.jpg)

## 云图是SaaS
1. 支持部门向服务部门转变
2. 诸位是甲方
3. SaaS 意味着无限可能

## 云图的功能
1. 抓图，突破防盗链，效率，CDN
2. 存图，防盗链，数据安全
3. 裁剪，缩略，水印，格式转化等
4. 易用和灵活兼具的 API

## 裁剪演示
* [原图](http://www.shaimn.com/uploads/allimg/141201/1-141201230436.jpg)
* [取图片的上半部分-1 /crop/h/0.5/](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/crop/h/0.5)
* [取图片的上半部分-2 /crop/y/-0.5/](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/crop/y/-0.5)
* [以人脸为中心 /crop/w/0.8/h/0.5/xy/face](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/crop/w/0.8/h/0.5/xy/face)

## 缩略演示
* [原图](http://www.shaimn.com/uploads/allimg/141201/1-141201230436.jpg)
* [缩率 /resize/w/440/t/2](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/resize/w/400/t/2/)
* [返回原图 /resize/w/1000/t/2](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/resize/w/1000/t/2)
* [安全模式 /resize/w/1000/t/2/s/1](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/resize/w/1000/t/2/s/1)

## 水印演示
* [原图](http://www.shaimn.com/uploads/allimg/141201/1-141201230436.jpg)
* [添加水印 /watermark/t/sogou](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/watermark/t/sogou)
* [水印样式 /watermark/t/sogou/c/red/s/24](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/watermark/t/sogou/s/24/c/red)

## 其他
* [修改图片格式 /retype/ext/png](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/retype/ext/png)
* [图片质量 /retype/q/30](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/retype/q/30)
* [级联操作 /crop/h/0.5/xy/face/resize/w/400/t/2/watermark/t/sogou/c/red/retype/q/30](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/crop/h/0.5/xy/face/resize/w/400/t/2/watermark/t/sogou/c/red/retype/q/30)
* [调试选项 /debug/1](http://10.136.110.179:8080/v2/thumb/debug/true/appid/101490007/url/http%3A%2F%2Fwww.shaimn.com%2Fuploads%2Fallimg%2F141201%2F1-141201230436.jpg/cls/gd/retype/qi/30)

## 未来
1. 云图不止于此
2. 更多*aaS值得期待
3. 我们是一伙儿的
4. 未来仰仗诸位的支持和理解

