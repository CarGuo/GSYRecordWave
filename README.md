<h4>MP3音频录制，支持类似IOS原生的单边或者双边波形显示，低版本音频权限兼容，本地或者在线音频播放。可以单纯使用录制功能，也可以单纯使用图形。</h4>
======================
```
<dependency>
  <groupId>com.shuyu</groupId>
  <artifactId>aduioRecord</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```

<h4> compile 'com.shuyu:aduioRecord:1.0.0'</h4>

<h4><a href="http://www.jianshu.com/p/2448e2903b07">简书入口 这里有详细介绍</a></h4>

* <h4>录制音频为MP3保存本地</h4>
* <h4>音频权限提示</h4>
* <h4>显示音频的波形，支持单边与双边，自动根据声音大小和控件高度调整波形高度</h4>
* <h4>支持获取声音大小</h4>
* <h4>本地/网络音频播放，音频时长与播放时长支持</h4>

## 效果显示
<img src="https://github.com/CarGuo/RecordWave/blob/master/01.jpg" width="240px" height="426px"/>
<p></p>
<img src="https://github.com/CarGuo/RecordWave/blob/master/03.jpg" width="240px" height="426px"/>
<p></p>
###动态图效果
<img src="https://github.com/CarGuo/RecordWave/blob/master/01.gif" width="240px" height="426px"/>
<p></p>

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




