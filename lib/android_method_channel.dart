import 'dart:convert';

import 'package:flutter/services.dart';

import 'ble_device.dart';

class AndroidMethodChannel {
  AndroidMethodChannel._privateConstructor();

  static final AndroidMethodChannel _instance =
      AndroidMethodChannel._privateConstructor();

  factory AndroidMethodChannel() {
    return _instance;
  }

  static const _channelName = "android";
  static const _bleInit = "bleInit";
  static const _bleScan = "bleScan";
  static const _bleScanStop = "bleScanStop";
  static const _bleConnect = "bleConnect";
  static const _bleDisconnect = "bleDisconnect";
  static const _bleWriteData = "bleWriteData";

  /// 블루투스 초기화
  Future<void> bleInit() async {
    await const MethodChannel(_channelName).invokeMethod(_bleInit);
  }

  /// 블루투스 스캔 시작
  Future<void> bleScan() async {
    await const MethodChannel(_channelName).invokeMethod(_bleScan);
  }

  /// 블루투스 스캔 정지
  Future<void> bleScanStop() async {
    await const MethodChannel(_channelName).invokeMethod(_bleScanStop);
  }

  /// Ble 연결
  Future<void> bleConnect({required BleDevice bleDevice}) async {
    await const MethodChannel(_channelName).invokeMethod(
      _bleConnect,
      bleDevice.toMap(),
    );
  }

  /// Ble 연결 끊기
  Future<void> bleDisConnect({required BleDevice bleDevice}) async {
    await const MethodChannel(_channelName).invokeMethod(
      _bleDisconnect,
      bleDevice.toMap(),
    );
  }

  /// Ble 데이터 쓰기
  Future<void> bleWriteData(String value) async {
    await const MethodChannel(_channelName).invokeMethod(_bleWriteData, value);
  }
}
