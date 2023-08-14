import 'dart:async';
import 'dart:convert';

import 'package:ble_phone_sample/android_method_channel.dart';
import 'package:ble_phone_sample/ble_device.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

class BleController with ChangeNotifier {
  static final BleController _notifierViewModel = BleController._singleton();

  factory BleController() => _notifierViewModel;

  BleController._singleton();

  final EventChannel bleChannel = const EventChannel('bleEventChannel');
  Stream<dynamic>? _stream;
  StreamSubscription? _streamSubscription;

  /// 스캔한 디바이스 목록
  List<BleDevice> scanDevice = [];

  BleDevice? connectedDevice;

  /// 블루투스 활성화 여부
  bool enableBluetooth = false;

  List<String> dataList = [];

  Future<void> init() async {
    scanDevice.clear();

    print('_stream : $_stream');

    if (_stream != null) return;

    await AndroidMethodChannel().bleInit();

    /// 블루투스 초기화
    _stream =
        bleChannel.receiveBroadcastStream().map<dynamic>((event) => event);

    _streamSubscription = _stream?.listen(parse);
  }

  /// 이벤트 채널 캔슬
  void cancel() {
    _streamSubscription?.cancel();
    _streamSubscription = null;
  }

  /// 스캔 시작
  void startScan() {
    AndroidMethodChannel().bleScan();
  }

  /// 스캔 시작
  void startBackgroundScan() {
    AndroidMethodChannel().bleBackgroundScan();
  }

  /// 스캔 중지
  void stopScan() => AndroidMethodChannel().bleScanStop();

  /// BLE 연결
  void connect(BleDevice bleDevice) {
    AndroidMethodChannel().bleConnect(bleDevice: bleDevice);
  }

  /// BLE 연결 해제
  void disconnect() {
    if (connectedDevice != null) {
      AndroidMethodChannel().bleDisConnect(bleDevice: connectedDevice!);
    }
  }

  void writeData(String value) {
    AndroidMethodChannel().bleWriteData(value);
    dataList.add(value);
    notifyListeners();
  }

  /// Bluetooth EventChannel 에서 받는 데이터 처리
  void parse(dynamic json) {
    print('jjjj : $json');
    try {
      switch (json["type"]) {
        case "scan":
          _parseScanCallback(json);

          break;
        case "connect":
          print('connect : $json');
          String address = json["data"].toString();
          connectedDevice =
              BleDevice(name: "", address: address, isBonded: true);
          notifyListeners();
          break;
        case "disconnect":
          print('disconnect');
          connectedDevice = null;
          notifyListeners();
          break;
        case "notification":
          String data = json["data"].toString();

          dataList.add(data);

          notifyListeners();
          break;
      }
    } catch (e) {
      print(e);
    }
  }

  /// 스캔 디바이스 정보 콜백
  void _parseScanCallback(dynamic json) {
    List<BleDevice> list = (json["data"] as Iterable).map<BleDevice>((e) {
      var json = jsonDecode(e);
      return BleDevice.fromJson(json);
    }).where((element) {
      return true;
    }).toList();

    scanDevice.clear();
    scanDevice.addAll(list);
    if (scanDevice.isNotEmpty) {
      for (var device in scanDevice) {
        if (device.name.toLowerCase() == "ihp-w") {
          connect(list[0]);
          stopScan();
          break;
        }
      }
    }
    notifyListeners();
  }
}
