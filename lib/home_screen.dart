import 'package:ble_phone_sample/ble_controller.dart';
import 'package:ble_phone_sample/permission_util.dart';
import 'package:ble_phone_sample/scan_screen.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  var bleController = BleController();

  @override
  void initState() {
    requestPermission();
    bleController.addListener(() {
      if (mounted) {
        setState(() {});
      }
    });
    super.initState();
  }

  void requestPermission() async {
    var connectStatus =
    await PermissionUtil.requestPermission(Permission.bluetoothConnect);
    if (connectStatus != PermissionStatus.granted) {
      requestPermission();
    }
    await PermissionUtil.requestPermission(Permission.bluetoothScan);
    await PermissionUtil.requestPermission(Permission.bluetoothAdvertise);
    await PermissionUtil.requestPermission(Permission.location);

    if (mounted) {
      setState(() {});
    }
  }

  @override
  void dispose() {

    super.dispose();
  }
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Column(
        children: [
          ElevatedButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) {
                      return const ScanScreen();
                    },
                  ),
                );
              },
              child: Text("스캔 화면 이동")),
          if (bleController.connectedDevice != null) ...[
            Text("연결된 디바이스 : ${bleController.connectedDevice}"),
            ElevatedButton(
              onPressed: () {
                String value = "writeData aaa";
                bleController.writeData(value);
              },
              child: Text("Write data"),
            ),
          ],
          if (bleController.dataList.isNotEmpty) ...[
            Expanded(
              child: ListView.builder(
                itemBuilder: (context, index) {
                  return ListTile(title: Text(bleController.dataList[index]));
                },
                itemCount: bleController.dataList.length,
              ),
            ),
          ],
        ],
      ),
    );
  }
}
