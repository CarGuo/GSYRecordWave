### MP3音频录制，支持类似IOS原生的单边或者双边波形显示，低版本音频权限兼容，本地或者在线音频播放。可以单纯使用录制功能，也可以单纯使用图形，播放也支持波形显示，录制波形和播放波形会根据声音频率变色的功能，边播边缓存功能。

---------------------------------


* 录制音频为MP3保存本地。
* 音频权限提示。
* 显示音频的波形，支持单边与双边，自动根据声音大小和控件高度调整波形高度。
* 支持获取声音大小。
* 本地/网络音频播放，音频时长与播放时长支持。
* 播放MP3显示波形数据。
* 根据录制和播放的波形根据特征变颜色。


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
     compile 'com.github.CarGuo:RecordWave:v1.1.2'
}

```
　

### [简书入口 这里有详细介绍](http://www.jianshu.com/p/2448e2903b07)


## 效果显示
<img src="https://github.com/CarGuo/RecordWave/blob/master/01.jpg" width="240px" height="426px"/>
<img src="https://github.com/CarGuo/RecordWave/blob/master/03.jpg" width="240px" height="426px"/>

###动态图效果
<img src="https://github.com/CarGuo/RecordWave/blob/master/01.gif" width="240px" height="426px"/>
　


### QQ群，有兴趣的可以进来，无底线欢迎：174815284 。

----------------------------------------------------
### 1.1.2 (2017-03-20)
增加新接口，修复已知问题
```
/**
 * 设置开始绘制波形的启始时间
 * @param startWaveTime 毫秒
 * */
public void setStartWaveTime(long startWaveTime)


/**
 * 是否画出基线
 *
 * @param drawBase
 */
public void setDrawBase(boolean drawBase)
```
### 1.1.0
* fix bug.

### 1.0.9
* fix some bug（like 'AudioTrack retrieve' and 'Short == null'）

### 1.0.8
* 最低API16
* 增加了波形播放边播边缓存（cache）功能
* 修复了潜在bug
```
/**
 * add cache url when play
 */
public void setUrlString(Context context, boolean cache, String urlString)
```

### 1.0.7 最低API调整到15

### 1.0.6

* **更新了对部分6.0的支持**

### 1.0.5
* **增加了播放对seekTo和获取时长与播放进度的支持**

### 1.0.4
* **增加了录制和播放时的暂停功能**

```
/**
 * 是否暂停
 */
public void setPause(boolean pause)

```

### 1.0.3
* **增加了录制波形和播放波形会根据声音频率变色的功能**

###效果 - GIF上颜色和帧数有些失真混在一起了

<img src="https://github.com/CarGuo/RecordWave/blob/master/02.gif" width="240px" height="426px"/>

```
//将播放器或者录制器设置进去即可生效
audioWave.setBaseRecorder(player);

····

/**
 * 三种颜色,不设置用默认的
 */
public void setChangeColor(int color1, int color2, int color3)

/**
 * 是否更具声音大小显示清晰度
 */
public void setAlphaByVolume(boolean alphaByVolume)


```

### 1.0.1
* 增加了录制的播放MP3时也可以显示波形 主要是通过<a href="https://github.com/piterwilson/MP3StreamPlayer">MP3RadioStreamPlayer</a>修改之后实现。
* 原理是使用AudioTrack播放流，通过系统的MediaCodec解码MP3,目前只支持本库录制下来的MP3:
   AudioFormat.CHANNEL_OUT_MONO 单声道
   AudioFormat.ENCODING_PCM_16BIT 16位

```
if (player != null) {
    player.stop();
    player.release();
    player = null;
}
player = new MP3RadioStreamPlayer();
player.setUrlString(uri);//可以是本地uri或者网络URL
player.setDelegate(this);

int size = getScreenWidth(this) / dip2px(this, 1);//控件默认的间隔是1
player.setDataList(audioWave.getRecList(), size);
audioWave.startView();
//可以设置循环播放

```

## AudioWaveView 声音波形显示，可单可双，自动调整波形高度适应高度

```
<declare-styleable name="waveView">
    <attr name="waveColor" format="color" />
    <attr name="waveOffset" format="dimension" />
    <attr name="waveCount" format="dimension" />
</declare-styleable>

audioWave.startView(); //开始绘制
audioWave.stopView(); //停止绘制

```

## MP3Recorder

来至<a href="https://github.com/GavinCT/AndroidMP3Recorder">AndroidMP3Recorder</a>,不过目前该作者已经停止维护。
该项目在此项目基础上增加了音频录制的权限判断和数据提取

```
···
mRecorder = new MP3Recorder(new File(filePath));
//控件默认的间隔是1dp
int size = getScreenWidth(this) / dip2px(this, 1);
//设置数据提取的list和最大数据存储数（一般就是控件的大小处于间隔）
这个list直接用AudioWaveView的lsit
mRecorder.setDataList(audioWave.getRecList(), size);
//错误回调
mRecorder.setErrorHandler(new Handler() {

···

/**
  * 获取真实的音量。
  *
  * @return 真实音量
  */
 public int getRealVolume()

 /**
  * 获取相对音量。 超过最大值时取最大值。
  *
  * @return 音量
  */
 public int getVolume()

```

## AudioPlayer 音频播放
```
···
audioPlayer = new AudioPlayer(this, new Handler() {
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case AudioPlayer.HANDLER_CUR_TIME://更新当前的时间
                break;
            case AudioPlayer.HANDLER_COMPLETE://播放结束
                break;
            case AudioPlayer.HANDLER_PREPARED://播放开始
                break;
            case AudioPlayer.HANDLER_ERROR://播放错误
                break;
        }

    }
});

audioPlayer.playUrl(filePath);

```




