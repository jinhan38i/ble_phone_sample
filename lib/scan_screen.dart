import 'package:ble_phone_sample/android_method_channel.dart';
import 'package:ble_phone_sample/ble_controller.dart';
import 'package:flutter/material.dart';

class ScanScreen extends StatefulWidget {
  const ScanScreen({Key? key}) : super(key: key);

  @override
  State<ScanScreen> createState() => _ScanScreenState();
}

class _ScanScreenState extends State<ScanScreen> {
  var bleController = BleController();

  VoidCallback? listener;

  @override
  void initState() {
    // AndroidMethodChannel().cpuOn();
    bleController.scanDevice.clear();
    listener = () {
      if (mounted) {
        setState(() {});
      }
    };
    bleController.addListener(listener!);
    super.initState();
  }

  @override
  void dispose() {
    bleController.removeListener(listener!);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("ScanScreen"),
      ),
      body: Column(
        children: [
          ElevatedButton(
              onPressed: () {
                setState(() {});
              },
              child: Text("화면 갱신")),
          ElevatedButton(
              onPressed: () {
                bleController.init();
              },
              child: const Text("BLE 초기화")),
          ElevatedButton(
              onPressed: () {
                print('스캔 클릭');
                Future.delayed(
                  Duration(seconds: 2),
                  () {
                    print('스캔 시작');
                    bleController.startScan();
                  },
                );
              },
              child: const Text("스캔")),
          ElevatedButton(
              onPressed: () {
                bleController.disconnect();
              },
              child: const Text("연결 해제 ")),
          Expanded(
              child: SingleChildScrollView(
            child: Column(
              children: [
                ...List.generate(bleController.scanDevice.length, (index) {
                  var device = bleController.scanDevice[index];
                  return ListTile(
                    onTap: () {
                      bleController.connect(device);
                    },
                    title: Text("${device.name}, ${device.address}"),
                  );
                })
              ],
            ),
          )),
        ],
      ),
    );
  }
}
