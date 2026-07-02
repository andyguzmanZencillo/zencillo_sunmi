import 'dart:developer';
import 'dart:typed_data';

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

  /// Igual al método original Java:
  ///
  /// PrintSunmy.initPrint(String sContent, int nSize)
  static Future<bool> initPrint(
    String sContent, {
    int nSize = 24,
  }) {
    return ZencilloSunmiPlatform.instance.initPrint(
      sContent,
      nSize: nSize,
    );
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

  static Future<bool> feedPaper({int lines = 8}) {
    return ZencilloSunmiPlatform.instance.feedPaper(lines: lines);
  }

  static Future<bool> printQr(
    String data, {
    int size = 6,
    int errorLevel = 2,
  }) {
    return ZencilloSunmiPlatform.instance.printQr(
      data,
      size: size,
      errorLevel: errorLevel,
    );
  }

  static Future<bool> printImageBytes(Uint8List bytes) {
    return ZencilloSunmiPlatform.instance.printImageBytes(bytes);
  }

  /// Método que usa tu app.
  ///
  /// Arma el String completo y llama a:
  ///
  /// initPrint(sContent, nSize)
  ///
  /// igual que la librería Java original.
  static Future<Result<Unit, String>> sunmiPrint(
    List<String> text, {
    String? code,
    int? tamanioLetra,
    bool? isQr,
  }) async {
    try {
      log('SUNMI_DART ===> sunmiPrint START');

      final fontSize = tamanioLetra ?? 20;
      final buffer = StringBuffer();

      for (final element in text) {
        buffer.writeln(element.trimRight());
      }

      if (code != null && code.trim().isNotEmpty && (isQr ?? false)) {
        buffer.writeln();
        buffer.writeln('!-QR-!${code.trim()}!-QR-!');
        buffer.writeln();
      }

      final sContent = buffer.toString();

      log('SUNMI_DART ===> calling initPrint');
      log('SUNMI_DART ===> nSize: $fontSize');
      log('SUNMI_DART ===> sContent: $sContent');

      final ok = await initPrint(
        sContent,
        nSize: fontSize,
      );

      log('SUNMI_DART ===> initPrint response: $ok');

      if (!ok) {
        return const Err('No se pudo imprimir en Sunmi.');
      }

      return const Ok(unit);
    } on PlatformException catch (e, stacktrace) {
      log('SUNMI_DART PlatformException code ===> ${e.code}');
      log('SUNMI_DART PlatformException message ===> ${e.message}');
      log('SUNMI_DART PlatformException stacktrace ===> $stacktrace');

      return Err(e.message ?? 'Error de plataforma al imprimir en Sunmi.');
    } catch (e, stacktrace) {
      log('SUNMI_DART FAILED ===> $e');
      log('SUNMI_DART stacktrace ===> $stacktrace');

      if (e is ZencilloSunmiException) {
        return Err(e.message);
      }

      return const Err('Algo falló al imprimir en Sunmi.');
    }
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
