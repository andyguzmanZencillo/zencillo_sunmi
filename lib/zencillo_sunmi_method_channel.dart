import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'zencillo_sunmi_platform_interface.dart';

class MethodChannelZencilloSunmi extends ZencilloSunmiPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('zencillo_sunmi');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  @override
  Future<bool> bindPrinter() async {
    final result = await methodChannel.invokeMethod<bool>('bindPrinter');
    return result ?? false;
  }

  @override
  Future<bool> isConnected() async {
    final result = await methodChannel.invokeMethod<bool>('isConnected');
    return result ?? false;
  }

  @override
  Future<bool> initPrinter() async {
    final result = await methodChannel.invokeMethod<bool>('initPrinter');
    return result ?? false;
  }

  @override
  Future<bool> printText(
    String text, {
    double size = 24,
    int align = 0,
    bool bold = false,
  }) async {
    final result = await methodChannel.invokeMethod<bool>('printText', {
      'text': text,
      'size': size,
      'align': align,
      'bold': bold,
    });

    return result ?? false;
  }

  @override
  Future<bool> feedPaper({int lines = 8}) async {
    final result = await methodChannel.invokeMethod<bool>('feedPaper', {
      'lines': lines,
    });

    return result ?? false;
  }

  @override
  Future<bool> printLine() async {
    final result = await methodChannel.invokeMethod<bool>('printLine');
    return result ?? false;
  }

  @override
  Future<bool> lineWrap({int lines = 3}) async {
    final result = await methodChannel.invokeMethod<bool>('lineWrap', {
      'lines': lines,
    });

    return result ?? false;
  }

  @override
  Future<bool> cutPaper() async {
    final result = await methodChannel.invokeMethod<bool>('cutPaper');
    return result ?? false;
  }

  @override
  Future<bool> printQr(
    String data, {
    int size = 6,
  }) async {
    final result = await methodChannel.invokeMethod<bool>('printQr', {
      'data': data,
      'size': size,
    });

    return result ?? false;
  }

  @override
  Future<bool> printImageBytes(Uint8List bytes) async {
    final result = await methodChannel.invokeMethod<bool>('printImageBytes', {
      'bytes': bytes,
    });

    return result ?? false;
  }
}
