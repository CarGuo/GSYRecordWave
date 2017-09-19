### MP3音频录制，支持类似IOS原生的单边或者双边波形显示，低版本音频权限兼容，本地或者在线音频播放。可以单纯使用录制功能，也可以单纯使用图形，播放也支持波形显示，录制波形和播放波形会根据声音频率变色的功能，边播边缓存功能。

---------------------------------


* 录制音频为MP3保存本地。
* 音频权限提示。
* 显示音频的波形，支持单边与双边，自动根据声音大小和控件高度调整波形高度。
* 支持获取声音大小。
* 本地/网络音频播放，音频时长与播放时长支持。
* 播放MP3显示波形数据。
* 根据录制和播放的波形根据特征变颜色。
* 自定义线大小、方向和绘制偏移。


[![](https://jitpack.io/v/CarGuo/RecordWave.svg)](https://jitpack.io/#CarGuo/RecordWave)
[![Build Status](https://travis-ci.org/CarGuo/RecordWave.svg?branch=master)](https://travis-ci.org/CarGuo/RecordWave)


#### 在你的项目project下的build.gradle添加
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
#### 在module下的build.gradle添加依赖
```
dependencies {
     compile 'com.github.CarGuo:RecordWave:v1.1.4'
}

```
　

### [简书入口 这里有基础介绍](http://www.jianshu.com/p/2448e2903b07)


## 效果显示
<img src="https://github.com/CarGuo/RecordWave/blob/master/01.jpg" width="240px" height="426px"/>
<img src="https://github.com/CarGuo/RecordWave/blob/master/03.jpg" width="240px" height="426px"/>

## 动态图效果

<img src="https://github.com/CarGuo/RecordWave/blob/master/01.gif" width="240px" height="426px"/>
　


### QQ群，有兴趣的可以进来，群里视频项目的人居多，平时多吹水吐槽：174815284 。

----------------------------------------------------

### 1.1.4 (2017-09-19)
* 增加速度、方向、开始偏移、自定义paint接口


### [历史版本](https://github.com/CarGuo/RecordWave/blob/master/OLD_VERSION.md)

### 使用方法请参考demo




