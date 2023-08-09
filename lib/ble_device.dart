class BleDevice {
  String name;
  String address;
  bool isBonded;
  bool connected;
  int battery = 0;

  BleDevice({
    required this.name,
    required this.address,
    required this.isBonded,
    this.connected = false,
  });

  factory BleDevice.fromJson(Map<String, dynamic> json) {
    return BleDevice(
      name: json["name"],
      address: json["address"],
      isBonded: json["isBonded"],
    );
  }

  Map<String, dynamic> toMap() => {
        "name": name,
        "address": address,
        "isBonded": isBonded,
        "connected": connected,
        "battery": battery,
      };

  @override
  String toString() {
    return 'BleDevice{name: $name, address: $address, isBonded: $isBonded, '
        'connected: $connected, battery: $battery}';
  }
}
