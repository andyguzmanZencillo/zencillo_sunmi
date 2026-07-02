import 'dart:developer';

import 'package:flutter/services.dart';
import 'package:oxidized/oxidized.dart';

import 'zencillo_sunmi_platform_interface.dart';

class ZencilloSunmi {
  static Future<String?> getPlatformVersion() {
    return ZencilloSunmiPlatform.instance.getPlatformVersion();
  }

  static Future<bool> bindPrinter() {
    return ZencilloSunmiPlatform.instance.bindPrinter();
  }

  static Future<bool> isConnected() {
    return ZencilloSunmiPlatform.instance.isConnected();
  }

  static Future<bool> initPrinter() {
    return ZencilloSunmiPlatform.instance.initPrinter();
  }

  static Future<bool> printText(
    String text, {
    double size = 24,
    SunmiAlign align = SunmiAlign.left,
    bool bold = false,
  }) {
    return ZencilloSunmiPlatform.instance.printText(
      text,
      size: size,
      align: align.value,
      bold: bold,
    );
  }

  static Future<bool> printLine() {
    return ZencilloSunmiPlatform.instance.printLine();
  }

  static Future<bool> lineWrap({int lines = 3}) {
    return ZencilloSunmiPlatform.instance.lineWrap(lines: lines);
  }

  static Future<bool> cutPaper() {
    return ZencilloSunmiPlatform.instance.cutPaper();
  }

  static Future<bool> printQr(
    String data, {
    int size = 6,
  }) {
    return ZencilloSunmiPlatform.instance.printQr(
      data,
      size: size,
    );
  }

  static Future<bool> printImageBytes(Uint8List bytes) {
    return ZencilloSunmiPlatform.instance.printImageBytes(bytes);
  }

  static Future<Result<Unit, String>> sunmiPrint(
    List<String> text, {
    String? code,
    int? tamanioLetra,
    bool? isQr,
  }) async {
    try {
      await bindPrinter();

      await initPrinter();

      for (final element in text) {
        await printText(
          element.trimRight(),
          align: SunmiAlign.center,
          size: (tamanioLetra ?? 20).toDouble(),
        );
      }

      if (code != null && code.trim().isNotEmpty && (isQr ?? false)) {
        await printText(
          '  ',
          align: SunmiAlign.center,
          size: (tamanioLetra ?? 20).toDouble(),
        );

        await printQr(code);
      }

      for (int x = 1; x <= 6; x++) {
        await printText(
          '  ',
          align: SunmiAlign.center,
          size: (tamanioLetra ?? 20).toDouble(),
        );
      }
      return const Ok(unit);
    } on PlatformException catch (e, stacktrace) {
      log('Sunmi PlatformException code ===> ${e.code}');
      log('Sunmi PlatformException message ===> ${e.message}');
      log('Sunmi PlatformException stacktrace ===> $stacktrace');

      return Err(e.message ?? 'Error de plataforma al imprimir en Sunmi.');
    } catch (e, stacktrace) {
      log('Sunmi Printer FAILED ===> $e');
      log('Sunmi Printer FAILED stacktrace ===> $stacktrace');

      if (e is ZencilloSunmiException) {
        return Err(e.message);
      }

      return const Err('Algo falló al imprimir en Sunmi.');
    }
  }

  static Future<bool> feedPaper({int lines = 8}) {
    return ZencilloSunmiPlatform.instance.feedPaper(lines: lines);
  }
}

enum SunmiAlign {
  left(0),
  center(1),
  right(2);

  final int value;

  const SunmiAlign(this.value);
}

class ZencilloSunmiException implements Exception {
  final String message;

  const ZencilloSunmiException(this.message);

  @override
  String toString() => message;
}
