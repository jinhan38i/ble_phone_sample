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
                bleController.init();
              },
              child: const Text("초기화")),
          ElevatedButton(
              onPressed: () {
                bleController.startScan();
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
