import 'package:permission_handler/permission_handler.dart';

class PermissionUtil {
  static Future<PermissionStatus> requestPermission(
      Permission permission) async {
    var status = await permission.status;

    switch (status) {
      case PermissionStatus.denied:
        var permissionStatus = await permission.request();
        status = permissionStatus;
        break;
      case PermissionStatus.granted:
        break;
      case PermissionStatus.limited:
        break;
      default:
        break;
    }
    return status;
  }
}
