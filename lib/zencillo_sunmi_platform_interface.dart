import 'dart:typed_data';

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

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('getPlatformVersion() no implementado.');
  }

  Future<bool> bindPrinter() {
    throw UnimplementedError('bindPrinter() no implementado.');
  }

  Future<bool> isConnected() {
    throw UnimplementedError('isConnected() no implementado.');
  }

  Future<bool> initPrinter() {
    throw UnimplementedError('initPrinter() no implementado.');
  }

  /// Igual al método original Java:
  ///
  /// PrintSunmy.initPrint(String sContent, int nSize)
  Future<bool> initPrint(
    String sContent, {
    int nSize = 24,
  }) {
    throw UnimplementedError('initPrint() no implementado.');
  }

  Future<bool> printText(
    String text, {
    double size = 24,
    int align = 0,
    bool bold = false,
  }) {
    throw UnimplementedError('printText() no implementado.');
  }

  Future<bool> printLine() {
    throw UnimplementedError('printLine() no implementado.');
  }

  Future<bool> lineWrap({int lines = 3}) {
    throw UnimplementedError('lineWrap() no implementado.');
  }

  Future<bool> cutPaper() {
    throw UnimplementedError('cutPaper() no implementado.');
  }

  Future<bool> feedPaper({int lines = 8}) {
    throw UnimplementedError('feedPaper() no implementado.');
  }

  Future<bool> printQr(
    String data, {
    int size = 6,
    int errorLevel = 2,
  }) {
    throw UnimplementedError('printQr() no implementado.');
  }

  Future<bool> printImageBytes(Uint8List bytes) {
    throw UnimplementedError('printImageBytes() no implementado.');
  }
}
