library zencillo_sunmi;

import 'package:oxidized/oxidized.dart';

import 'zencillo_sunmi_platform_interface.dart';

class ZencilloSunmi {
  static Future<Result<Unit, String>> init() {
    return ZencilloSunmiPlatform.instance.init();
  }

  static Future<Result<bool, String>> isConnected() {
    return ZencilloSunmiPlatform.instance.isConnected();
  }

  static Future<Result<Unit, String>> sunmiPrint(
    List<String> text, {
    String? code,
    int? tamanioLetra,
    bool? isQr,
  }) {
    return ZencilloSunmiPlatform.instance.sunmiPrint(
      text,
      code: code,
      tamanioLetra: tamanioLetra,
      isQr: isQr,
    );
  }

  static Future<Result<Unit, String>> printText(
    String text, {
    int? tamanioLetra,
    bool? bold,
    bool? underline,
    int? align,
    int? feedLines,
  }) {
    return ZencilloSunmiPlatform.instance.printText(
      text,
      tamanioLetra: tamanioLetra,
      bold: bold,
      underline: underline,
      align: align,
      feedLines: feedLines,
    );
  }

  static Future<Result<Unit, String>> printQr(
    String code, {
    int? size,
  }) {
    return ZencilloSunmiPlatform.instance.printQr(
      code,
      size: size,
    );
  }

  static Future<Result<Unit, String>> feed({
    int? lines,
  }) {
    return ZencilloSunmiPlatform.instance.feed(
      lines: lines,
    );
  }

  static Future<Result<Unit, String>> cut() {
    return ZencilloSunmiPlatform.instance.cut();
  }

  static Future<Result<Map<String, dynamic>, String>> getStatus() {
    return ZencilloSunmiPlatform.instance.getStatus();
  }

  static Future<Result<Map<String, dynamic>, String>> getPrinterInfo() {
    return ZencilloSunmiPlatform.instance.getPrinterInfo();
  }

  Future<Result<Unit, String>> imprimirSunmi(
    List<String> text, {
    String? code,
    int? tamanioLetra,
    bool? isQr,
  }) async {
    final initResult = await ZencilloSunmi.init();

    if (initResult.isErr()) {
      return Err(initResult.unwrapErr());
    }

    final result = await ZencilloSunmi.sunmiPrint(
      text,
      code: code,
      tamanioLetra: tamanioLetra,
      isQr: isQr,
    );

    return result;
  }
}
