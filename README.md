Android App内截屏监控及涂鸦功能实现
=================================

### 原理

Android系统并没有提供截屏通知相关的API，需要我们自己利用系统能提供的相关特性变通实现。一般有三种方法：

    利用FileObserver监听某个目录中资源变化情况

    利用ContentObserver监听全部资源的变化

    监听截屏快捷按键  ( 由于厂商自定义Android系统的多样性，再加上快捷键的不同以及第三方应用，监听截屏快捷键这事基本不靠谱，可以直接忽略 )

### FileObserver 与 ContentObserver 比较

** 方案一： 通过FileObserver监听截屏文件夹，当有新的截屏文件产生时，调用设定的回调函数执行相关操作。

    优点：
        1. 实现简单
    
    缺点：
        1. 不同手机默认的截屏路径可能不同，需要做适配处理
        2. 不同手机截屏触发的事件名称可能不同，需要测试适配
        3. 监听到截屏事件后马上获取图片获取不到，需要延迟一段时间

** 方案二：通过ContentObserver监听多媒体图片库资源的变化。当手机上有新的图片文件产生时都会通过MediaProvider类向图片数据库插入一条记录，
以方便系统的图片库进行图片查询，可以通过ContentObserver接收图片插入事件，并获取插入图片的URI。

    优点：
        1. 不同手机触发的事件是一样的
    
    缺点：
        1. 不同手机截屏文件的前缀可能不同，需要做适配
        2. 监听到截屏事件后马上获取图片获取不到，需要延迟一段时间
    
    综上，不同的手机，默认截屏图片储存的文件夹可能不同，FileObserver只能监听文件夹中子文件和子文件夹的变化情况，不能监听子文件夹内部的资源变化
    基于不同的手机，默认截屏图片储存的文件夹可能不同和可能收不到事件，这种方法并不能适用于所有的机型。

### ContentObserver 截屏监听

Android系统有一个媒体数据库，每拍一张照片，或使用系统截屏截取一张图片，都会把这张图片的详细信息加入到这个媒体数据库，并发出内容改变通知，
我们可以利用内容观察者（ContentObserver）监听媒体数据库的变化，当数据库有变化时，获取最后插入的一条图片数据，如果该图片符合特定的规则，则认为被截屏了。

需要ContentObserver监听的资源URI:

    MediaStore.Images.Media.INTERNAL_CONTENT_URI
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI

读取外部存储器资源，需要添加权限:

    android.permission.READ_EXTERNAL_STORAGE
    
当ContentObserver监听到媒体数据库的数据改变, 在有数据改变时 获取最后插入数据库的一条图片数据, 如果符合以下规则, 则认为截屏了:

    1、时间判断，图片的生成时间在开始监听之后, 并与当前时间相隔10秒内：开始监听后生成的图片才有意义，相隔10秒内说明是刚刚生成的,不同手机10秒可能不对；
    2、尺寸判断，图片的尺寸没有超过屏幕的尺寸：图片尺寸超过屏幕尺寸，一般都不是截屏图片（最近发现某些手机ROM支持滑动截屏，如果需要监听的APP界面支持滑动，需要做适当的调整和处理）；
    3、路径判断，图片路径符合包含特定的关键词：这一点是关键，截屏图片的保存路径通常包含“screenshot”。

这些判断是为了增加截屏检测结果的可靠性，防止误报，防止遗漏。其中截屏图片的路径正常Android系统保存的路径格式为：“外部存储器/Pictures/Screenshots/Screenshot_20161001-164643.png”，
但Android系统碎片化严重，加上其他第三方截屏APP等，所以路径关键字除了检查是否包含“screenshot”外，还可以适当增加其他关键字，详见最后的监听器完整代码。

这种监听截屏的方法也不是100%准确，例如某些被root的机器使用第三方截屏APP自定义保存路径，还比如通过ADB命令在电脑上获取手机屏幕快照均不能监听到，但这也是目前可行性最高的方法，对于绝大多数用户都比较靠谱。

### 实现效果如下：

![监听截屏，展示截屏并涂鸦](https://github.com/452896915/SnapShotMonitor/blob/master/2839011-04795289ae00d6c4.gif)

### 参考文档

1、[Android 截屏监听（截图分享功能实现）](https://www.jianshu.com/p/d7aba5a03b0f)

2、[Android系统 截屏监听 的 原理与实现](https://blog.csdn.net/xietansheng/article/details/52692163)

3、[Android App内截屏监控及涂鸦功能实现](https://www.jianshu.com/p/2e6d52abf115)