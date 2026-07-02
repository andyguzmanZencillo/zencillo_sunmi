import 'package:oxidized/oxidized.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'zencillo_sunmi_method_channel.dart';

abstract class ZencilloSunmiPlatform extends PlatformInterface {
  ZencilloSunmiPlatform() : super(token: _token);

  static final Object _token = Object();

  static ZencilloSunmiPlatform _instance = MethodChannelZencilloSunmi();

  static ZencilloSunmiPlatform get instance => _instance;

  static set instance(ZencilloSunmiPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<Result<Unit, String>> init();

  Future<Result<bool, String>> isConnected();

  Future<Result<Unit, String>> sunmiPrint(
    List<String> text, {
    String? code,
    int? tamanioLetra,
    bool? isQr,
  });

  Future<Result<Unit, String>> printText(
    String text, {
    int? tamanioLetra,
    bool? bold,
    bool? underline,
    int? align,
    int? feedLines,
  });

  Future<Result<Unit, String>> printQr(
    String code, {
    int? size,
  });

  Future<Result<Unit, String>> feed({
    int? lines,
  });

  Future<Result<Unit, String>> cut();

  Future<Result<Map<String, dynamic>, String>> getStatus();

  Future<Result<Map<String, dynamic>, String>> getPrinterInfo();
}
