import 'package:flutter/services.dart';
import 'package:oxidized/oxidized.dart';

import 'zencillo_sunmi_platform_interface.dart';

class MethodChannelZencilloSunmi extends ZencilloSunmiPlatform {
  static const MethodChannel _channel = MethodChannel('zencillo_sunmi');

  MethodChannelZencilloSunmi();

  @override
  Future<Result<Unit, String>> init() async {
    try {
      await _channel.invokeMethod<bool>('init');
      return const Ok(unit);
    } on PlatformException catch (e) {
      return Err(e.message ?? e.code);
    } catch (e) {
      return Err(e.toString());
    }
  }

  @override
  Future<Result<bool, String>> isConnected() async {
    try {
      final response = await _channel.invokeMethod<bool>('isConnected');
      return Ok(response ?? false);
    } on PlatformException catch (e) {
      return Err(e.message ?? e.code);
    } catch (e) {
      return Err(e.toString());
    }
  }

  @override
  Future<Result<Unit, String>> sunmiPrint(
    List<String> text, {
    String? code,
    int? tamanioLetra,
    bool? isQr,
  }) async {
    try {
      final buffer = StringBuffer();

      for (final line in text) {
        buffer.writeln(line);
      }

      if (code != null && code.trim().isNotEmpty) {
        if (isQr == true) {
          buffer.write('!-QR-!');
          buffer.write(code.trim());
          buffer.write('!-QR-!');
        } else {
          buffer.writeln(code.trim());
        }
      }

      await _channel.invokeMethod<void>('sunmiPrint', <String, dynamic>{
        'text': buffer.toString(),
        'size': tamanioLetra ?? 22,
      });

      return const Ok(unit);
    } on PlatformException catch (e) {
      return Err(e.message ?? e.code);
    } catch (e) {
      return Err(e.toString());
    }
  }

  @override
  Future<Result<Unit, String>> printText(
    String text, {
    int? tamanioLetra,
    bool? bold,
    bool? underline,
    int? align,
    int? feedLines,
  }) async {
    try {
      await _channel.invokeMethod<void>('printText', <String, dynamic>{
        'text': text,
        'size': tamanioLetra ?? 22,
        'bold': bold ?? true,
        'underline': underline ?? false,
        'align': align ?? 1,
        'feedLines': feedLines ?? 6,
      });

      return const Ok(unit);
    } on PlatformException catch (e) {
      return Err(e.message ?? e.code);
    } catch (e) {
      return Err(e.toString());
    }
  }

  @override
  Future<Result<Unit, String>> printQr(
    String code, {
    int? size,
  }) async {
    try {
      await _channel.invokeMethod<void>('printQr', <String, dynamic>{
        'data': code,
        'size': size ?? 5,
        'errorLevel': 0,
      });

      return const Ok(unit);
    } on PlatformException catch (e) {
      return Err(e.message ?? e.code);
    } catch (e) {
      return Err(e.toString());
    }
  }

  @override
  Future<Result<Unit, String>> feed({
    int? lines,
  }) async {
    try {
      await _channel.invokeMethod<void>('feed', <String, dynamic>{
        'lines': lines ?? 3,
      });

      return const Ok(unit);
    } on PlatformException catch (e) {
      return Err(e.message ?? e.code);
    } catch (e) {
      return Err(e.toString());
    }
  }

  @override
  Future<Result<Unit, String>> cut() async {
    try {
      await _channel.invokeMethod<void>('cut');
      return const Ok(unit);
    } on PlatformException catch (e) {
      return Err(e.message ?? e.code);
    } catch (e) {
      return Err(e.toString());
    }
  }

  @override
  Future<Result<Map<String, dynamic>, String>> getStatus() async {
    try {
      final response = await _channel.invokeMapMethod<String, dynamic>(
        'getStatus',
      );

      return Ok(response ?? <String, dynamic>{});
    } on PlatformException catch (e) {
      return Err(e.message ?? e.code);
    } catch (e) {
      return Err(e.toString());
    }
  }

  @override
  Future<Result<Map<String, dynamic>, String>> getPrinterInfo() async {
    try {
      final response = await _channel.invokeMapMethod<String, dynamic>(
        'getPrinterInfo',
      );

      return Ok(response ?? <String, dynamic>{});
    } on PlatformException catch (e) {
      return Err(e.message ?? e.code);
    } catch (e) {
      return Err(e.toString());
    }
  }
}
